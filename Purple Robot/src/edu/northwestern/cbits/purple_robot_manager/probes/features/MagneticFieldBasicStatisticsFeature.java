package edu.northwestern.cbits.purple_robot_manager.probes.features;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.MagneticFieldProbe;

public class MagneticFieldBasicStatisticsFeature extends XYZBasicStatisticsFeature
{
    protected String featureKey()
    {
        return "magnetic_statistics";
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
        return "edu.northwestern.cbits.purple_robot_manager.probes.features.MagneticFieldBasicStatisticsFeature";
    }

    public String source(Context context)
    {
        return MagneticFieldProbe.NAME;
    }

    public String title(Context context)
    {
        return context.getString(R.string.title_magnetic_statistics_feature);
    }

    @Override
    public String getPreferenceKey() {
        return "features_magnetometer_statistics";
    }
}
