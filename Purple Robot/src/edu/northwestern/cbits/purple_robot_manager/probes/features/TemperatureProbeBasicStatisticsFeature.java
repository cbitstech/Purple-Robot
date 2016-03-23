package edu.northwestern.cbits.purple_robot_manager.probes.features;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.TemperatureProbe;

public class TemperatureProbeBasicStatisticsFeature extends UnivariateContinuousProbeFeature
{
    protected String valueKey()
    {
        return "TEMPERATURE";
    }

    protected String featureKey()
    {
        return "temperature_statistics";
    }

    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_sensor_category);
    }

    public String summary(Context context)
    {
        return context.getString(R.string.summary_temperature_statistics_feature_desc);
    }

    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.features.TemperatureProbeBasicStatisticsFeature";
    }

    public String source(Context context)
    {
        return TemperatureProbe.NAME;
    }

    public String title(Context context)
    {
        return context.getString(R.string.title_temperature_statistics_feature);
    }

    @Override
    public String getPreferenceKey() {
        return "features_temperature_statistics";
    }
}
