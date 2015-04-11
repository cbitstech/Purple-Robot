package edu.northwestern.cbits.purple_robot_manager.logging;

import android.content.Context;

import edu.northwestern.cbits.purple_robot_manager.R;

public class LogEventCheck extends SanityCheck
{
    private static long WARNING_SIZE = 256;
    private static long ERROR_SIZE = 512;

    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_log_count);
    }

    @SuppressWarnings("deprecation")
    public void runCheck(Context context)
    {
        this._errorMessage = null;
        this._errorLevel = SanityCheck.OK;

        int logCount = LogManager.getInstance(context).pendingEventsCount();

        if (logCount > LogEventCheck.ERROR_SIZE)
        {
            this._errorLevel = SanityCheck.ERROR;
            this._errorMessage = context.getString(R.string.name_sanity_log_events_error);
        }
        else if (logCount > LogEventCheck.WARNING_SIZE)
        {
            this._errorLevel = SanityCheck.WARNING;
            this._errorMessage = context.getString(R.string.name_sanity_log_events_warning);
        }
    }
}
