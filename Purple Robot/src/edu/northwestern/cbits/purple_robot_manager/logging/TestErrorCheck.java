package edu.northwestern.cbits.purple_robot_manager.logging;

import edu.northwestern.cbits.purple_robot_manager.R;
import android.content.Context;

public class TestErrorCheck extends SanityCheck
{
    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_error);
    }

    public void runCheck(Context context)
    {
        this._errorLevel = SanityCheck.ERROR;
        this._errorMessage = context.getString(R.string.check_error_message);
    }
}
