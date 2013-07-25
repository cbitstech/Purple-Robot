package edu.northwestern.cbits.purple_robot_manager.probes.features;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.AccelerometerProbe;
import android.content.Context;

public class AccelerometerBasicStatisticsFeature extends XYZBasicStatisticsFeature 
{
	protected String featureKey() 
	{
		return "accelerometer_frequencies";
	}

	protected String summary(Context context) 
	{
		return context.getString(R.string.title_accelerator_frequencies_feature);
	}

	public String name(Context context) 
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.features.AccelerometerBasicStatisticsFeature";
	}

	public String source(Context context) 
	{
		return AccelerometerProbe.NAME;
	}

	public String title(Context context) 
	{
		return context.getString(R.string.summary_accelerator_frequencies_feature_desc);
	}
}
