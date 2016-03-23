package edu.northwestern.cbits.purple_robot_manager.probes;

import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;

public class LabelProbe extends Probe
{
    public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.Label";

    @Override
    public String name(Context context)
    {
        return LabelProbe.NAME;
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_label_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_misc_category);
    }

    @Override
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        return null;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        return bundle.getString("KEY") + ": " + bundle.getString("VALUE");
    }

    @Override
    public void enable(Context context)
    {

    }

    @Override
    public void disable(Context context)
    {

    }

    @Override
    public String summary(Context context)
    {
        return "";
    }

    @Override
    public JSONObject fetchSettings(Context _context)
    {
        return null;
    }

    @Override
    public String getPreferenceKey() {
        return "features_label";
    }
}
