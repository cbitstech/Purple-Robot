package edu.northwestern.cbits.purple_robot_manager.logging;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.plugins.DataUploadPlugin;

public class LastUploadCheck extends SanityCheck
{
    private static long WARN_DURATION = (1000 * 60 * 60 * 12);
    private static long ERROR_DURATION = WARN_DURATION * 2;

    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_last_upload);
    }

    public void runCheck(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        PurpleRobotApplication.fixPreferences(context, false);

        if (DataUploadPlugin.uploadEnabled(context) == false)
        {
            this._errorLevel = SanityCheck.OK;
            return;
        }

        int pendingFiles = DataUploadPlugin.pendingFileCount(context);

        if (pendingFiles < 8)
        {
            this._errorLevel = SanityCheck.OK;
            return;
        }

        long now = System.currentTimeMillis();

        long lastUploadTime = DataUploadPlugin.lastUploadTime(prefs);

        this._errorLevel = SanityCheck.OK;

        if (lastUploadTime == 0)
        {
            if (pendingFiles > 0)
            {
                this._errorLevel = SanityCheck.ERROR;
                this._errorMessage = context.getString(R.string.name_sanity_last_upload_never);
            }
        }
        else if (now - lastUploadTime > LastUploadCheck.ERROR_DURATION)
        {
            this._errorLevel = SanityCheck.WARNING;
            this._errorMessage = context.getString(R.string.name_sanity_last_upload_error);
        }
        else if (now - lastUploadTime > LastUploadCheck.WARN_DURATION)
        {
            this._errorLevel = SanityCheck.WARNING;
            this._errorMessage = context.getString(R.string.name_sanity_last_upload_warning);
        }
    }
}
