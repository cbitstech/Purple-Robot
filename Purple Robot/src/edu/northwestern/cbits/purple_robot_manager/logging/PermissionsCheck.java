package edu.northwestern.cbits.purple_robot_manager.logging;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.PermissionsActivity;

public class PermissionsCheck extends SanityCheck
{
    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_permissions);
    }

    public Runnable getAction(final Context context)
    {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(context, PermissionsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);
            }
        };

        return r;
    }

    public void runCheck(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            boolean missing = false;

            String[] required = context.getResources().getStringArray(R.array.required_permissions);

            for (String permission : required) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                    missing = true;
            }

            if (missing) {
                this._errorLevel = SanityCheck.ERROR;
                this._errorMessage = context.getString(R.string.name_sanity_permission_error);
            }
            else
                this._errorLevel = SanityCheck.OK;
        }
        else
            this._errorLevel = SanityCheck.OK;
    }
}
