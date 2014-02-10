package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import org.apache.commons.lang.StringUtils;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class FacebookEventsProbe extends Probe 
{
	public static final String PROBE_NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.FacebookEventsProbe";

	public String name(Context context) 
	{
		return FacebookEventsProbe.PROBE_NAME;
	}

	@Override
	public String title(Context context) 
	{
		return context.getString(R.string.title_facebook_events_probe);
	}

	public String probeCategory(Context context) 
	{
		return context.getResources().getString(R.string.probe_external_services_category);
	}

	public PreferenceScreen preferenceScreen(PreferenceActivity settingsActivity) 
	{
		return null;
	}

 
	{

	}

	public void enable(Context context) 
	{

	}

	public void disable(Context context) 
	{

	}
	
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
