package edu.northwestern.cbits.purple_robot_manager.logging;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.WebActivity;

public class Android50MemoryCheck extends SanityCheck
{
    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_android_five_memory);
    }

    @SuppressWarnings("deprecation")
    public void runCheck(Context context)
    {
        this._errorMessage = null;
        this._errorLevel = SanityCheck.OK;

        if ((Build.VERSION.RELEASE.startsWith("5.0") && SystemClock.elapsedRealtime() > (18 * 60 * 60 * 1000)))
        {
            this._errorMessage = context.getString(R.string.name_sanity_android_five_memory_warning);
            this._errorLevel = SanityCheck.WARNING;
        }
    }

    public Runnable getAction(final Context context)
    {
        return new Runnable()
        {
            public void run()
            {
                Intent intent = new Intent(context, WebActivity.class);
                intent.setData(Uri.parse("file:///android_asset/embedded_website/warnings/android50.html"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        };
    }
}
