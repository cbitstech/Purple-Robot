package edu.northwestern.cbits.purple_robot_manager.logging;

import edu.northwestern.cbits.purple_robot_manager.R;
import android.content.Context;

public class TestWarningCheck extends SanityCheck
{
    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_warning);
    }

    public void runCheck(Context context)
    {
        this._errorLevel = SanityCheck.WARNING;
        this._errorMessage = context.getString(R.string.check_warning_message);
    }
}
