package edu.northwestern.cbits.purple_robot_manager.probes.devices.wear;

import android.content.Context;
import android.os.Bundle;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.devices.AndroidWearProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.studies.LivewellActivityCountsProbe;

public class WearLivewellActivityCountProbe extends WearSensorProbe
{
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.WearLivewellActivityCountProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_livewell_wear_activity_counts_probe);
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_livewell_wear_activity_counts_probe_desc);
    }

    public String summarizeValue(Context context, Bundle bundle)
    {
        double count = bundle.getDouble(LivewellActivityCountsProbe.BUNDLE_ALL_DELTA);
        double numSamples = bundle.getDouble(LivewellActivityCountsProbe.BUNDLE_NUM_SAMPLES);
        double duration = bundle.getDouble(LivewellActivityCountsProbe.BUNDLE_DURATION);

        return String.format(context.getResources().getString(R.string.summary_livewell_pebble_probe), (duration / 1000), numSamples, count);
    }

    @Override
    public String getPreferenceKey() {
        return "devices_wear_livewell_counts";
    }
}
