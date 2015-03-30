package edu.northwestern.cbits.purple_robot_manager.probes.devices;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.northwestern.cbits.purple_robot_manager.AndroidWearService;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class AndroidWearProbe extends Probe implements DataApi.DataListener
{
    private static final String ENABLED = "config_probe_android_wear_enabled";
    private static final boolean DEFAULT_ENABLED = false;
    public static final String URI_READING_PREFIX = "/purple-robot-reading";

    private GoogleApiClient _apiClient = null;
    private Context _context = null;
    private long _lastRequest = 0;

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.devices.AndroidWearProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_android_wear_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_other_devices_category);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_android_wear_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(AndroidWearProbe.ENABLED);
        enabled.setDefaultValue(AndroidWearProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        return screen;
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_android_wear_probe_desc);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(AndroidWearProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(AndroidWearProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = new JSONObject();

        try
        {
            JSONObject enabled = new JSONObject();
            enabled.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);

            JSONArray values = new JSONArray();
            values.put(true);
            values.put(false);
            enabled.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_ENABLED, enabled);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    @Override
    public boolean isEnabled(Context context)
    {
        this._context = context.getApplicationContext();
        SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(AndroidWearProbe.ENABLED, AndroidWearProbe.DEFAULT_ENABLED))
            {
                long now = System.currentTimeMillis();

                if (now - this._lastRequest > 1000 * 60 * 5)
                {
                    this._lastRequest = now;

                    AndroidWearService.requestDataFromDevices(context);
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents)
    {
        for (DataEvent event : dataEvents)
        {
            if (event.getType() == DataEvent.TYPE_CHANGED)
            {
                DataItem item = event.getDataItem();

                if (item.getUri().getPath().compareTo(AndroidWearProbe.URI_READING_PREFIX) == 0)
                {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                    this.transmitData(this._context, dataMap.toBundle());

                    Wearable.DataApi.deleteDataItems(this._apiClient, item.getUri());
                }
            }
            else if (event.getType() == DataEvent.TYPE_DELETED)
            {

            }
        }
    }
}
