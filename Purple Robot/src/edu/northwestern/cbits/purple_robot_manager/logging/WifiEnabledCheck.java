package edu.northwestern.cbits.purple_robot_manager.logging;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.WiFiHelper;
import edu.northwestern.cbits.purple_robot_manager.plugins.DataUploadPlugin;

public class WifiEnabledCheck extends SanityCheck
{
    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_wifi_enabled);
    }

    public void runCheck(Context context)
    {
        PurpleRobotApplication.fixPreferences(context, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (DataUploadPlugin.uploadEnabled(context) == false || DataUploadPlugin.restrictToWifi(prefs) == false || WiFiHelper.wifiAvailable(context))
        {
            this._errorLevel = SanityCheck.OK;
            return;
        }

        int count = DataUploadPlugin.pendingFileCount(context);

        if (count > 100) {
            this._errorLevel = SanityCheck.ERROR;
            this._errorMessage = context.getString(R.string.name_sanity_wifi_enabled_error);
        }
        else {
            this._errorLevel = SanityCheck.WARNING;
            this._errorMessage = context.getString(R.string.name_sanity_wifi_enabled_warning);
        }
    }
}
