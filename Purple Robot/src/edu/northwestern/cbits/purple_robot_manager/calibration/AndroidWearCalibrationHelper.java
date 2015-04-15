package edu.northwestern.cbits.purple_robot_manager.calibration;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;

public class AndroidWearCalibrationHelper
{
    private static final String ANDROID_WEAR_PACKAGE = "com.google.android.wearable.app";

    public static void check(final Context context, boolean isEnabled)
    {
        final SanityManager sanity = SanityManager.getInstance(context);
        final String title = context.getString(R.string.title_android_wear_check);

        if (isEnabled == false)
        {
            sanity.clearAlert(title);

            return;
        }

        try
        {
            context.getPackageManager().getPackageInfo(AndroidWearCalibrationHelper.ANDROID_WEAR_PACKAGE, 0);

            sanity.clearAlert(title);
        }
        catch (NameNotFoundException e)
        {
            String message = context.getString(R.string.message_android_wear_check);

            Runnable action = new Runnable()
            {
                @Override
                public void run()
                {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + AndroidWearCalibrationHelper.ANDROID_WEAR_PACKAGE));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    context.startActivity(intent);
                }
            };

            sanity.addAlert(SanityCheck.WARNING, title, message, action);
        }
    }
}
