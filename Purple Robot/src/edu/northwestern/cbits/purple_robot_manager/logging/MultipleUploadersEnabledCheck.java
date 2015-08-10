package edu.northwestern.cbits.purple_robot_manager.logging;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.plugins.DataUploadPlugin;

public class MultipleUploadersEnabledCheck extends SanityCheck
{
    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_multiple_uploaders_enabled);
    }

    public void runCheck(Context context)
    {
        PurpleRobotApplication.fixPreferences(context, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (DataUploadPlugin.multipleUploadersEnabled(context) == false)
        {
            this._errorLevel = SanityCheck.OK;
            return;
        }

        this._errorLevel = SanityCheck.WARNING;
        this._errorMessage = context.getString(R.string.name_sanity_multiple_uploaders_enabled_warning);
    }
}
