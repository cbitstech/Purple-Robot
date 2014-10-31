package edu.northwestern.cbits.purple_robot_manager.logging;

import edu.northwestern.cbits.purple_robot_manager.R;
import android.content.Context;

public abstract class SanityCheck
{
    public static final int OK = 0;
    public static final int ERROR = 1;
    public static final int WARNING = 2;

    protected int _errorLevel = SanityCheck.WARNING;
    protected String _errorMessage = "Check is unimplemented.";

    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_unknown);
    }

    public abstract void runCheck(Context context);

    public int getErrorLevel()
    {
        return this._errorLevel;
    }

    public String getErrorMessage()
    {
        return this._errorMessage;
    }

    public Runnable getAction(Context context)
    {
        return null;
    }
}
