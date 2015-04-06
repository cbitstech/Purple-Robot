package edu.northwestern.cbits.purple_robot_manager.probes.devices.wear;

import android.content.Context;
import android.os.Bundle;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.devices.AndroidWearProbe;

public class WearHeartRateProbe extends WearSensorProbe
{
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.WearHeartProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_wear_battery_probe);
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_wear_battery_probe_desc);
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        boolean charging = bundle.getBoolean("BATTERY_CHARGING", false);
        int beats = (int) bundle.getDoubleArray("BPM")[0];
        return String.format(context.getResources().getString(R.string.summary_wear_heart_probe), beats);
    }

    @Override
    protected String getPreferenceKey()
    {
        return AndroidWearProbe.HEART_METER_ENABLED;
    }

}
