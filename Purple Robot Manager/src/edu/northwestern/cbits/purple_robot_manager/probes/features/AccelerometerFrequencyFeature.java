package edu.northwestern.cbits.purple_robot_manager.probes.features;

import edu.northwestern.cbits.purple_robot_manager.probes.builtin.AccelerometerProbe;
import android.content.Context;

public class AccelerometerFrequencyFeature extends XYZBasicFrequencyFeature 
{
	protected String featureKey() 
	{
		return "accelerometer_frequency";
	}

	protected String summary(Context context) 
	{
		return "tODO";
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
		return "jjg Goodness";
	}
}
