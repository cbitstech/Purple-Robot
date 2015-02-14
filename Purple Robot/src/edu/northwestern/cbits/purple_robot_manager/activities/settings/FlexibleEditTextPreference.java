package edu.northwestern.cbits.purple_robot_manager.activities.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class FlexibleEditTextPreference extends EditTextPreference
{
    private Context _context = null;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FlexibleEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FlexibleEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public FlexibleEditTextPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public FlexibleEditTextPreference(Context context)
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
