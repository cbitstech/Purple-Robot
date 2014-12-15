package edu.northwestern.cbits.purple_robot_manager.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

/**
 * Provides the structure for Models. Models take input from probes, features, 
 * and other models and generate predictions from those inputs. Purple Robot 
 * supports both linear (e.g. regressions) and categorical models (e.g. decision
 * trees).
 * 
 * In Purple Robot, models are defined by providing a URL to the model, which 
 * contains a JSON representation of the model that can be deserialized into a 
 * functioning class instance that implements the given model. Purple Robot 
 * caches model definitions, allowing the system to access and construct model
 * instances even when the device is unable to access the original definition's 
 * URL due to a lack of reliable network connectivity.
 * 
 * Please see the various Model subclasses for specific implementation details.
 */

/**
 * @author Administrator
 * 
 */
public abstract class Model
{
    public static final boolean DEFAULT_ENABLED = true;
    private static long _lastEnabledCheck = 0;
    private static boolean _lastEnabled = false;

    protected HashMap<String, String> _featureMap = new HashMap<String, String>();

    // Cached values used to determine when the model state has changed.
    private Object _latestPrediction = null;
    private double _latestAccuracy = 0.0;

    /**
     * Provides a lookup key used to generate and configure a given model.
     * 
     * @return Value used internally to manipulate model settings via the
     *         SharedPreferences mechanism.
     */

    public abstract String getPreferenceKey();

    /**
     * Provides a human-readable name for the model used throughout the user
     * interface.
     * 
     * @param context
     *            Android Context object used to resolve values such as strings.
     * 
     * @return Human-readable model name.
     */

    public abstract String title(Context context);

    /**
     * Provides a human-readable description of the model used throughout the
     * user interface.
     * 
     * @param context
     *            Android Context object used to resolve values such as strings.
     * 
     * @return Human-readable model description.
     */

    public abstract String summary(Context context);

    /**
     * Reads the model definition from an online URL and attempts to construct
     * an appropriate dynamic model instance using the model subclasses. For
     * example, a decision tree may be constructed from the TreeModel class,
     * while a linear equation may be expanded using the {@link RegressionModel}
     * class.
     * 
     * @param context
     *            Android Context object used to lookup internal storage
     *            destinations.
     * @param jsonUrl
     *            HTTP URL pointing to a model definition.
     * 
     * @return Model instance implementing model specified, null if no
     *         appropriate {@link Model} class can be determined.
     */

    public final static Model modelForUrl(Context context, String jsonUrl)
    {
        String hash = EncryptionManager.getInstance().createHash(context, jsonUrl, "MD5");

        SharedPreferences prefs = Probe.getPreferences(context);

        // Determine where to cache the contents of jsonUrl...

        File internalStorage = context.getFilesDir();

        if (prefs.getBoolean("config_external_storage", false))
            internalStorage = context.getExternalFilesDir(null);

        if (internalStorage != null && !internalStorage.exists())
            internalStorage.mkdirs();

        File modelsFolder = new File(internalStorage, "persisted_models");

        if (modelsFolder != null && !modelsFolder.exists())
            modelsFolder.mkdirs();

        String contents = null;
        File cachedModel = new File(modelsFolder, hash);

        // Load any cached model definitions...

        try
        {
            contents = FileUtils.readFileToString(cachedModel);
        }
        catch (IOException e)
        {

        }

        // Fetch the contents of the URL & replace the cached version if
        // successful retrieving the definition online...

        try
        {
            BufferedReader in = null;

            if (jsonUrl.startsWith("file:///android_asset/"))
            {
                AssetManager assets = context.getAssets();

                in = new BufferedReader(new InputStreamReader(
                        assets.open(jsonUrl.replace("file:///android_asset/", ""))));
            }
            else
            {
                URL u = new URL(jsonUrl);

                in = new BufferedReader(new InputStreamReader(u.openStream()));
            }

            StringBuffer sb = new StringBuffer();

            String inputLine = null;

            while ((inputLine = in.readLine()) != null)
                sb.append(inputLine);

            in.close();

            contents = sb.toString();
        }
        catch (MalformedURLException e)
        {
            LogManager.getInstance(context).logException(e);
        }
        catch (IOException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        // Determine which subclass to use to instantiate an instance. Return
        // the new instance if successful...

        if (contents != null)
        {
            try
            {
                JSONObject json = new JSONObject(contents);

                String type = json.getString("model_type");

                if (RegressionModel.TYPE.equals(type))
                    return new RegressionModel(context, Uri.parse(jsonUrl));
                else if (WekaTreeModel.TYPE.equals(type))
                    return new WekaTreeModel(context, Uri.parse(jsonUrl));
                else if (MatlabForestModel.TYPE.equals(type))
                    return new MatlabForestModel(context, Uri.parse(jsonUrl));
                else if (MatlabTreeModel.TYPE.equals(type))
                    return new MatlabTreeModel(context, Uri.parse(jsonUrl));
                else if (MatlabForestModel.TYPE.equals(type))
                    return new MatlabForestModel(context, Uri.parse(jsonUrl));

                else if (NoiseModel.TYPE.equals(type))
                    return new NoiseModel(context, Uri.parse(jsonUrl));

            }
            catch (JSONException e)
            {
                LogManager.getInstance(context).logException(e);
            }
        }

        // ... and return null if something went wrong.

        return null;
    }

    /**
     * @return Uri referencing the model. May be the original URL of the model,
     *         but may also be an alternative Uri as the situation permits.
     *         Model implementation returns null by default - subclasses
     *         implement alternative behaviors.
     */

    public Uri uri()
    {
        return null;
    }

    /**
     * Enables the model. This method is used by other parts of the system such
     * as the scripting framework and user-facing settings.
     * 
     * @param context
     *            Android Context object used to access the SharedPreferences
     *            instances.
     */

    public void enable(Context context)
    {
        String key = this.getPreferenceKey();

        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean("config_model_" + key + "_enabled", true);

        e.commit();
    }

    /**
     * Disables the model. This method is used by other parts of the system such
     * as the scripting framework and user-facing settings.
     * 
     * @param context
     *            Android Context object used to access the SharedPreferences
     *            instances.
     */

    public void disable(Context context)
    {
        String key = this.getPreferenceKey();

        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean("config_model_" + key + "_enabled", false);

        e.commit();
    }

    /**
     * Reports whether a model instance is enabled. Note that models may be
     * enabled and disabled on an individual basis or all may be disabled from
     * the {@link ModelManager} singleton.
     * 
     * @param context
     *            Android Context object used to access the SharedPreferences
     *            instances
     * 
     * @return Status of the model.
     */

    public boolean enabled(Context context)
    {
        if (ModelManager.getInstance(context).enabled(context))
        {
            String key = this.getPreferenceKey();

            SharedPreferences prefs = Probe.getPreferences(context);

            return prefs.getBoolean("config_model_" + key + "_enabled", Model.DEFAULT_ENABLED);
        }

        return false;
    }

    /**
     * Constructs a preference screen dynamically for an infividual model
     * instance for used in the user-facing app settings.
     * 
     * @param activity
     *            Parent settings activity.
     * 
     * @return Screen containing all the relevant options for a model instance.
     */

    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(PreferenceActivity activity)
    {
        PreferenceManager manager = activity.getPreferenceManager();

        PreferenceScreen screen = manager.createPreferenceScreen(activity);
        screen.setTitle(this.title(activity));
        screen.setSummary(this.summary(activity));

        String key = this.getPreferenceKey();

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_model);
        enabled.setKey("config_model_" + key + "_enabled");
        enabled.setDefaultValue(Model.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        return screen;
    }

    /**
     * Called periodically by the rest of the system to determine if the model
     * is enabled. TODO: "isEnabled" is a convention adopted from the Probe
     * classes, and needs to be refactored, along with these instances as well.
     * ("nudge" might be a better name.
     * 
     * @param context
     *            Android Context object used to access the SharedPreferences
     *            instances
     * 
     * @return Status of the model.
     */

    public boolean isEnabled(Context context)
    {
        long now = System.currentTimeMillis();
        SharedPreferences prefs = Probe.getPreferences(context);

        if (now - Model._lastEnabledCheck > 10000)
        {
            Model._lastEnabledCheck = now;

            Model._lastEnabled = prefs.getBoolean("config_models_enabled", Model.DEFAULT_ENABLED);
        }

        if (Model._lastEnabled)
        {
            String key = this.getPreferenceKey();

            return prefs.getBoolean("config_model_" + key + "_enabled", true);
        }

        return Model._lastEnabled;
    }

    /**
     * Transmits a continuous prediction (real number) generated by the model to
     * the rest of the data processing pipeline.
     * 
     * @param context
     * @param prediction
     *            Value of the prediction.
     * @param accuracy
     *            Estimated accuracy of the prediction.
     */

    protected void transmitPrediction(Context context, double prediction, double accuracy)
    {
        Bundle bundle = new Bundle();
        bundle.putString("PROBE", this.title(context));
        bundle.putDouble("TIMESTAMP", ((double) System.currentTimeMillis()) / 1000);
        bundle.putDouble("PREDICTION", prediction);
        bundle.putBoolean("FROM_MODEL", true);

        this.transmitData(context, bundle);

        this._latestPrediction = Double.valueOf(prediction);
        this._latestAccuracy = accuracy;
    }

    /**
     * Provides the latest prediction made by the model.
     * 
     * @param context
     * @return Map containing the prediction and any relevant metadata.
     */

    public Map<String, Object> latestPrediction(Context context)
    {
        HashMap<String, Object> prediction = new HashMap<String, Object>();

        prediction.put("prediction", this._latestPrediction);
        prediction.put("accuracy", this._latestAccuracy);
        prediction.put("type", this.modelType());
        prediction.put("title", this.title(context));
        prediction.put("url", this.name(context).replace("\\/", "/"));

        return prediction;
    }

    /**
     * Transmits a classification (string label) generated by the model to the
     * rest of the data processing pipeline.
     * 
     * @param context
     * @param prediction
     *            Value of the prediction.
     * @param accuracy
     *            Estimated accuracy of the prediction.
     * @param map
     */

    protected void transmitPrediction(Context context, String prediction, double accuracy, Map<String, Object> map)
    {
        Bundle bundle = new Bundle();
        bundle.putString("PROBE", this.title(context));
        bundle.putDouble("TIMESTAMP", ((double) System.currentTimeMillis()) / 1000);
        bundle.putString("PREDICTION", prediction);
        bundle.putBoolean("FROM_MODEL", true);
        bundle.putDouble("ACCURACY", accuracy);

        if (map != null)
        {
            for (String key : map.keySet())
            {
                Object value = map.get(key);

                if (value instanceof Double)
                    bundle.putDouble(key, (Double) value);
                else if (value instanceof Integer)
                    bundle.putInt(key, (Integer) value);
                else
                    bundle.putString(key, value.toString());
            }
        }

        this.transmitData(context, bundle);

        this._latestPrediction = prediction;
        this._latestAccuracy = accuracy;
    }

    protected void transmitPrediction(Context context, String prediction, double accuracy)
    {
        this.transmitPrediction(context, prediction, accuracy, null);
    }

    /**
     * Utility function used by the transmitPrediction methods to broadcast a
     * model's predictions to the rest of the data processing pipeline.
     * 
     * @param context
     * @param data
     *            Bundle containing the prediction and any relevant metadata.
     */

    protected void transmitData(Context context, Bundle data)
    {
        if (context != null)
        {
            UUID uuid = UUID.randomUUID();
            data.putString("GUID", uuid.toString());
            data.putString("MODEL_NAME", this.title(context));

            LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(context);
            Intent intent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
            intent.putExtras(data);

            localManager.sendBroadcast(intent);
        }
    }

    /**
     * Called when a model prediction is requested. Note that this only requests
     * a prediction - models are not obligated to return a prediction
     * immediately, but instead will generate a prediction asynchronously and
     * share the prediction using the transmitPrediction methods.
     * 
     * @param context
     * @param snapshot
     *            A representation of the state of the world to be used to
     *            generate a prediction.
     */

    public abstract void predict(Context context, Map<String, Object> snapshot);

    /**
     * Returns the name of the model used internally (not human-readable). In
     * most cases, this will be the URL of the model's definition file.
     * 
     * @param context
     * 
     * @return Unique string identifying a model instance.
     */

    public abstract String name(Context context);

    /**
     * @return Identifier for the type of the model.
     */

    public abstract String modelType();

    public String mappedFeatureName(String key)
    {
        return this._featureMap.get(key);
    }
}
