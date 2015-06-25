package edu.northwestern.cbits.purple_robot_manager.logging;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import edu.northwestern.cbits.purple_robot_manager.PowerHelper;
import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.plugins.DataUploadPlugin;

public class ChargingRequiredCheck extends SanityCheck
{
    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_charging_required);
    }

    public void runCheck(Context context)
    {
        PurpleRobotApplication.fixPreferences(context, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (DataUploadPlugin.uploadEnabled(context) == false || DataUploadPlugin.restrictToCharging(prefs) == false || PowerHelper.isPluggedIn(context))
        {
            this._errorLevel = SanityCheck.OK;
            return;
        }

        int count = DataUploadPlugin.pendingFileCount(context);

        if (count > 1000) {
            this._errorLevel = SanityCheck.ERROR;
            this._errorMessage = context.getString(R.string.name_sanity_charging_required_error);
        }
        else {
            this._errorLevel = SanityCheck.OK;
        }
    }
}
