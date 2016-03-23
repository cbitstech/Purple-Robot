package edu.northwestern.cbits.purple_robot_manager.probes.features;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.AccelerometerProbe;

public class AccelerometerFrequencyFeature extends XYZBasicFrequencyFeature
{
    @Override
    protected String featureKey()
    {
        return "accelerometer_frequency";
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_sensor_category);
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_accelerator_frequencies_feature_desc);
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.features.AccelerometerFrequencyFeature";
    }

    @Override
    public String source(Context context)
    {
        return AccelerometerProbe.NAME;
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_accelerator_frequencies_feature);
    }

    @Override
    public String getPreferenceKey() {
        return "features_accelerometer_frequency";
    }
}
