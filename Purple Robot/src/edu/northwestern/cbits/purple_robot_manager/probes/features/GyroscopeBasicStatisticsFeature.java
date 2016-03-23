package edu.northwestern.cbits.purple_robot_manager.probes.features;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.GyroscopeProbe;

public class GyroscopeBasicStatisticsFeature extends XYZBasicStatisticsFeature
{
    protected String featureKey()
    {
        return "gyroscope_statistics";
    }

    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_sensor_category);
    }

    public String summary(Context context)
    {
        return context.getString(R.string.summary_gyroscope_statistics_feature_desc);
    }

    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.features.GyroscopeBasicStatisticsFeature";
    }

    public String source(Context context)
    {
        return GyroscopeProbe.NAME;
    }

    public String title(Context context)
    {
        return context.getString(R.string.title_gyroscope_statistics_feature);
    }

    @Override
    public String getPreferenceKey() {
        return "features_gyroscope_statistics";
    }
}
