package edu.northwestern.cbits.purple_robot_manager.probes.devices.wear;

import android.content.Context;
import android.os.Bundle;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.devices.AndroidWearProbe;

public class WearBatteryProbe extends WearSensorProbe
{
    public static String NAME = "edu.northwestern.cbits.purple_robot_manager.WearBatteryProbe";

    public String name(Context context)
    {
        return WearBatteryProbe.NAME;
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
        int level = (int) bundle.getDouble("BATTERY_LEVEL", -1);

        String status = context.getString(R.string.label_battery_discharging);

        if (charging)
            status = context.getString(R.string.label_battery_charging);

        return String.format(context.getResources().getString(R.string.summary_battery_probe), level, status);
    }

    @Override
    protected String getPreferenceKey()
    {
        return AndroidWearProbe.ENABLED;
    }
}
