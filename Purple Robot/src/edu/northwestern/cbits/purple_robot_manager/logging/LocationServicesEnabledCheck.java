package edu.northwestern.cbits.purple_robot_manager.logging;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;

public class LocationServicesEnabledCheck extends SanityCheck
{
    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_location_services_enabled);
    }

    public Runnable getAction(final Context context)
    {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);
            }
        };

        return r;
    }

    public void runCheck(Context context)
    {
        PurpleRobotApplication.fixPreferences(context, false);

        boolean locationOff = false;

        int locationMode = Settings.Secure.LOCATION_MODE_OFF;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            try
            {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            }
            catch (Settings.SettingNotFoundException e)
            {
                LogManager.getInstance(context).logException(e);
            }

            locationOff = (locationMode == Settings.Secure.LOCATION_MODE_OFF);
        }
        else
        {
            String locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

            locationOff = TextUtils.isEmpty(locationProviders);
        }

        if (locationOff)
        {
            this._errorLevel = SanityCheck.WARNING;
            this._errorMessage = context.getString(R.string.name_sanity_location_services_enabled_warning);
        }
        else
            this._errorLevel = SanityCheck.OK;
    }
}
