package edu.northwestern.cbits.purple_robot_manager.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.SettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.util.Slugify;

public class ModelManager extends BroadcastReceiver {
    private static final String PERSISTED_MODELS = "model_manager_peristed_models";

    private static ModelManager _instance = null;

    private Context _context = null;
    private List<Model> _models = new ArrayList<Model>();
    private HashMap<String, Object> _milieu = new HashMap<String, Object>();
    private HashMap<String, String> _keyCache = new HashMap<String, String>();

    private HashSet<String> _modelUrls = new HashSet<String>();

    private ModelManager(Context context) {
        if (ModelManager._instance != null)
            throw new IllegalStateException("Already instantiated");

        this._context = context.getApplicationContext();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Probe.PROBE_READING);

        LocalBroadcastManager localManager = LocalBroadcastManager
                .getInstance(context);
        localManager.registerReceiver(this, intentFilter);

        this.loadPersistedModels();
    }

    public static ModelManager getInstance(Context context) {
        if (ModelManager._instance == null) {
            ModelManager._instance = new ModelManager(
                    context.getApplicationContext());
        }

        return ModelManager._instance;
    }

    @SuppressWarnings("deprecation")
    public PreferenceScreen buildPreferenceScreen(
            PreferenceActivity settingsActivity) {
        PreferenceManager manager = settingsActivity.getPreferenceManager();

        PreferenceScreen screen = manager
                .createPreferenceScreen(settingsActivity);
        screen.setOrder(0);
        screen.setTitle(R.string.title_preference_models_screen);
        screen.setKey(SettingsActivity.MODELS_SCREEN_KEY);

        PreferenceCategory globalCategory = new PreferenceCategory(
                settingsActivity);
        globalCategory
                .setTitle(R.string.title_preference_models_global_category);
        globalCategory.setKey("key_available_models");

        screen.addPreference(globalCategory);

        CheckBoxPreference enabled = new CheckBoxPreference(settingsActivity);
        enabled.setTitle(R.string.title_preference_models_enable_models);
        enabled.setKey("config_models_enabled");
        enabled.setDefaultValue(Model.DEFAULT_ENABLED);

        globalCategory.addPreference(enabled);

        PreferenceCategory probesCategory = new PreferenceCategory(
                settingsActivity);
        probesCategory
                .setTitle(R.string.title_preference_models_available_category);
        probesCategory.setKey("key_available_models");

        screen.addPreference(probesCategory);

        for (Model model : this.allModels(settingsActivity)) {
            PreferenceScreen modelScreen = model
                    .preferenceScreen(settingsActivity);

            if (modelScreen != null)
                screen.addPreference(modelScreen);
        }

        return screen;
    }

    private List<Model> allModels(Context context) {
        ArrayList<Model> models = new ArrayList<Model>();
        models.addAll(this._models);

        return models;
    }

    @SuppressLint("DefaultLocale")
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();

        if (extras.containsKey("FROM_MODEL"))
            return;

        String[] nameComponents = extras.getString("PROBE").split("\\.");

        String probeName = nameComponents[nameComponents.length - 1];

        for (String key : extras.keySet()) {
            if ("PROBE".equals(key) || "GUID".equals(key)
                    || "TIMESTAMP".equals(key)) {
                // Do nothing...
            } else {
                String probeKey = probeName + " " + key;

                String slug = this._keyCache.get(probeKey);

                if (slug == null) {
                    slug = probeKey.toLowerCase(Locale.getDefault())
                            .replaceAll("_", " ");
                    slug = Slugify.slugify(slug).replaceAll("-", "_");

                    this._keyCache.put(probeKey, slug);
                }

                this._milieu.put(slug, extras.get(key));
            }
        }

        for (Model model : this.allModels(context)) {
            HashMap<String, Object> snapshot = new HashMap<String, Object>();

            synchronized (this._milieu) {
                snapshot.putAll(this._milieu);
            }

            if (model != null)
                model.predict(context, snapshot);
        }
    }

    public Model fetchModelByName(Context context, String name) {
        for (Model model : this._models) {
            if (name.equals(model.name(context)))
                return model;
        }

        return null;
    }

    public void addModel(String jsonUrl) {
        this.deleteModel(jsonUrl);

        this._modelUrls.add(jsonUrl);

        Model m = Model.modelForUrl(this._context, jsonUrl);

        if (m != null)
            this._models.add(m);

        this.persistModels();
    }

    private void persistModels() {
        JSONArray urls = new JSONArray();

        for (String url : this._modelUrls) {
            urls.put(url);
        }

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this._context);
        Editor e = prefs.edit();
        e.putString(ModelManager.PERSISTED_MODELS, urls.toString());
        e.commit();
    }

    private void loadPersistedModels() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this._context);

        try {
            JSONArray urls = new JSONArray(prefs.getString(
                    ModelManager.PERSISTED_MODELS, "[]"));

            for (int i = 0; i < urls.length(); i++) {
                String url = urls.getString(i);

                this.addModel(url);
            }
        } catch (JSONException e) {
            LogManager.getInstance(this._context).logException(e);
        }
    }

    public void deleteModel(String jsonUrl) {
        synchronized (this._models) {
            List<Model> toRemove = new ArrayList<Model>();

            for (Model model : this._models) {
                Uri uri = model.uri();

                if (uri != null && uri.toString().equals(jsonUrl))
                    toRemove.add(model);
            }

            this._models.removeAll(toRemove);
        }

        this._modelUrls.remove(jsonUrl);

        this.persistModels();
    }

    public void enableModel(String jsonUrl) {
        for (Model model : this._models) {
            Uri uri = model.uri();

            if (uri != null && uri.toString().equals(jsonUrl))
                model.enable(this._context);
        }
    }

    public void disableModel(String jsonUrl) {
        for (Model model : this._models) {
            Uri uri = model.uri();

            if (uri != null && uri.toString().equals(jsonUrl))
                model.disable(this._context);
        }
    }

    public boolean enabled(Context context) {
        SharedPreferences prefs = Probe.getPreferences(context);

        return prefs.getBoolean("config_models_enabled", Model.DEFAULT_ENABLED);
    }

    public Map<String, Object> models(Context context) {
        HashMap<String, Object> modelMap = new HashMap<String, Object>();

        for (Model m : this._models) {
            modelMap.put(m.title(context), "" + m.uri());
        }

        return modelMap;
    }

    public Map<String, Object> readings(Context context) {
        return this._milieu;
    }

    public Model fetchModelByTitle(Context context, String title) {
        if (title == null)
            return null;

        for (Model m : this._models) {
            if (title.equals(m.title(context)))
                return m;
        }

        return null;
    }

    public Map<String, Object> predictions(Context context) {
        HashMap<String, Object> predictions = new HashMap<String, Object>();

        for (Model m : this._models) {
            predictions.put(m.title(context), m.latestPrediction(context));
        }

        return predictions;
    }
}
