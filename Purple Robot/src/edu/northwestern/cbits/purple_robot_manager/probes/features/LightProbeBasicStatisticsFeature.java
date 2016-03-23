package edu.northwestern.cbits.purple_robot_manager.probes.features;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LightProbe;

public class LightProbeBasicStatisticsFeature extends UnivariateContinuousProbeFeature
{
    protected String valueKey()
    {
        return "LUX";
    }

    protected String featureKey()
    {
        return "light_statistics";
    }

    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_sensor_category);
    }

    public String summary(Context context)
    {
        return context.getString(R.string.summary_light_statistics_feature_desc);
    }

    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.features.LightProbeBasicStatisticsFeature";
    }

    public String source(Context context)
    {
        return LightProbe.NAME;
    }

    public String title(Context context)
    {
        return context.getString(R.string.title_light_statistics_feature);
    }

    @Override
    public String getPreferenceKey() {
        return "features_light_statistics";
    }
}
