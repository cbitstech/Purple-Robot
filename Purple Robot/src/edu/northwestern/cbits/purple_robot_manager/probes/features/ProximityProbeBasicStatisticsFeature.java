package edu.northwestern.cbits.purple_robot_manager.probes.features;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ProximityProbe;

public class ProximityProbeBasicStatisticsFeature extends UnivariateContinuousProbeFeature
{
    protected String valueKey()
    {
        return "DISTANCE";
    }

    protected String featureKey()
    {
        return "proximity_statistics";
    }

    public String probeCategory(Context context)
    {
        return context.getString(R.string.probe_sensor_category);
    }

    public String summary(Context context)
    {
        return context.getString(R.string.summary_proximity_statistics_feature_desc);
    }

    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.features.ProximityProbeBasicStatisticsFeature";
    }

    public String source(Context context)
    {
        return ProximityProbe.NAME;
    }

    public String title(Context context)
    {
        return context.getString(R.string.title_proximity_statistics_feature);
    }

    @Override
    public String getPreferenceKey() {
        return "features_proximity_statistics";
    }
}
