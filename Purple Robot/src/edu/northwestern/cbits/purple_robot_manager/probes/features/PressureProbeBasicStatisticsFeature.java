package edu.northwestern.cbits.purple_robot_manager.probes.features;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.PressureProbe;

public class PressureProbeBasicStatisticsFeature extends UnivariateContinuousProbeFeature
{
    protected String valueKey()
    {
        return "PRESSURE";
    }

    protected String featureKey()
    {
        return "pressure_statistics";
    }

    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_sensor_category);
    }

    public String summary(Context context)
    {
        return context.getString(R.string.summary_pressure_statistics_feature_desc);
    }

    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.features.PressureProbeBasicStatisticsFeature";
    }

    public String source(Context context)
    {
        return PressureProbe.NAME;
    }

    public String title(Context context)
    {
        return context.getString(R.string.title_pressure_statistics_feature);
    }

    @Override
    public String getPreferenceKey() {
        return "features_pressure_statistics";
    }

}
