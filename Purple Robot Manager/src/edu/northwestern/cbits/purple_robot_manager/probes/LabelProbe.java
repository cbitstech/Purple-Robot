package edu.northwestern.cbits.purple_robot_manager.probes;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;

public class LabelProbe extends Probe 
{
	public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.Label";

	public String name(Context context) 
	{
		return LabelProbe.NAME;
	}

	@Override
	public String title(Context context) 
	{
		return context.getString(R.string.title_label_probe);
	}

	public String probeCategory(Context context) 
	{
		return context.getResources().getString(R.string.probe_misc_category);
	}

	public PreferenceScreen preferenceScreen(PreferenceActivity settingsActivity) 
	{
		return null;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		return bundle.getString("KEY") + ": " + bundle.getString("VALUE");	
	}

	public void enable(Context context) 
	{

	}

	public void disable(Context context) 
	{

	}
}
