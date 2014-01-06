package edu.northwestern.cbits.purple_robot_manager.probes.features;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public abstract class Feature extends Probe
{
	public static final String FEATURE_VALUE = "FEATURE_VALUE";

	protected abstract String featureKey();

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(this.summary(activity));

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_feature_" + this.featureKey() + "_enabled");
		enabled.setDefaultValue(this.defaultEnabled());

		screen.addPreference(enabled);

		return screen;
	}

	protected boolean defaultEnabled() 
	{
		return true;
	}

	protected abstract String summary(Context context);


	{

	}
}
