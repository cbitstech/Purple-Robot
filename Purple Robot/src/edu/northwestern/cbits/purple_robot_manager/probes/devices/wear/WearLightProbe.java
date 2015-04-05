package edu.northwestern.cbits.purple_robot_manager.probes.devices.wear;

import android.content.Context;
import android.os.Bundle;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.devices.AndroidWearProbe;

public class WearLightProbe extends WearSensorProbe
{
    private static final String LIGHT_KEY = "LUX";

    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.WearLightProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_wear_light_probe);
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_wear_light_probe_desc);
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        double lux = bundle.getDoubleArray(LIGHT_KEY)[0];

        return String.format(context.getResources().getString(R.string.summary_light_probe), lux);
    }


    @Override
    protected String getPreferenceKey()
    {
        return AndroidWearProbe.LIGHT_METER_ENABLED;
    }

}
