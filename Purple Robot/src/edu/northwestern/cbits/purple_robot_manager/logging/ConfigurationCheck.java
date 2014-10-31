package edu.northwestern.cbits.purple_robot_manager.logging;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;

public class ConfigurationCheck extends SanityCheck
{
    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_configuration);
    }

    public void runCheck(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // config_http_upload_interval
        // config_log_upload_interval
        // config_json_refresh_interval

        long refresh = Long.parseLong(prefs.getString("config_json_refresh_interval", "0"));

        if (refresh > 0 && refresh < 600)
        {
            this._errorLevel = SanityCheck.WARNING;
            this._errorMessage = context.getString(R.string.name_sanity_configuration_refresh_warning);

            return;
        }

        PurpleRobotApplication.fixPreferences(context, false);

        if (prefs.getBoolean("config_enable_data_server", false))
        {
            long upload = Long.parseLong(prefs.getString("config_http_upload_interval", "0"));

            if (upload > 0 && upload < 300000)
            {
                this._errorLevel = SanityCheck.WARNING;
                this._errorMessage = context.getString(R.string.name_sanity_configuration_upload_warning);

                return;
            }
        }

        this._errorLevel = SanityCheck.OK;
    }
}
