package edu.northwestern.cbits.purple_robot_manager.probes.features;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.AccelerometerProbe;
import android.content.Context;

public class AccelerometerFrequencyFeature extends XYZBasicFrequencyFeature 
{
	protected String featureKey() 
	{
		return "accelerometer_frequency";
	}

	public String probeCategory(Context context)
	{
		return context.getString(R.string.probe_sensor_category);
	}

	protected String summary(Context context) 
	{
		return context.getString(R.string.summary_accelerator_frequencies_feature_desc);
	}

	public String name(Context context) 
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.features.AccelerometerFrequencyFeature";
	}

	public String source(Context context) 
	{
		return AccelerometerProbe.NAME;
	}

	public String title(Context context) 
	{
		return context.getString(R.string.title_accelerator_frequencies_feature);
	}
}
