package edu.northwestern.cbits.purple_robot_manager.probes;

import org.json.JSONException;
import org.json.JSONObject;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ScreenProbe;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

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
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public PreferenceScreen preferenceScreen(PreferenceActivity settingsActivity) 
	{
		return null;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		return bundle.getString("KEY") + ": " + bundle.getString("VALUE");	
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException 
	{
		// TODO Auto-generated method stub
	}
	
	public void enable(Context context) 
	{

	}

	public void disable(Context context) 
	{

	}
}
