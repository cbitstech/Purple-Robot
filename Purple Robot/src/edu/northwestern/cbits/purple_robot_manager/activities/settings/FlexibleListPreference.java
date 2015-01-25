package edu.northwestern.cbits.purple_robot_manager.activities.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class FlexibleListPreference extends ListPreference
{
    private Context _context = null;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FlexibleListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FlexibleListPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public FlexibleListPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public FlexibleListPreference(Context context)
    {
        super(context);
    }

    public void setContext(Context context)
    {
        this._context = context;
    }

    public Context getContext()
    {
        if (this._context == null)
            return super.getContext();

        return this._context;
    }
}
