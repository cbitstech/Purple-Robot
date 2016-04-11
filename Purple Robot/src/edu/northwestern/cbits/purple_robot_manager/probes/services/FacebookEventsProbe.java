package edu.northwestern.cbits.purple_robot_manager.probes.services;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import org.apache.commons.lang3.StringUtils;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class FacebookEventsProbe extends Probe
{
    public static final String PROBE_NAME = "edu.northwestern.cbits.purple_robot_manager.probes.services.FacebookEventsProbe";

    @Override
    public String getPreferenceKey() {
        return "services_facebook_events";
    }

    @Override
    public String name(Context context)
    {
        return FacebookEventsProbe.PROBE_NAME;
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_facebook_events_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_external_services_category);
    }

    @Override
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        return null;
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
        return context.getString(R.string.title_facebook_events_probe);
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        String message = bundle.getString("MESSAGE");
        String type = bundle.getString("TYPE");

        boolean obfuscated = bundle.getBoolean("IS_OBFUSCATED");

        if (message.length() > 512)
            message = StringUtils.abbreviate(message, 512);

        if (obfuscated)
            return String.format(context.getResources().getString(R.string.facebook_event_obfuscated_desc), message, type);

        return String.format(context.getResources().getString(R.string.facebook_event_clear_desc), message, type);
    }
}
