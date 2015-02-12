package edu.northwestern.cbits.purple_robot_manager.calibration;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.xsi.facebook.FacebookApi;
import edu.northwestern.cbits.xsi.facebook.FacebookLoginActivity;

public class FacebookCalibrationHelper
{
    public static void check(final Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        final SanityManager sanity = SanityManager.getInstance(context);
        final String title = context.getString(R.string.title_facebook_check);

        if (prefs.contains(FacebookApi.TOKEN) == false)
        {
            String message = context.getString(R.string.message_facebook_check);

            Runnable action = new Runnable()
            {
                public void run()
                {
                    Intent intent = new Intent(context, FacebookLoginActivity.class);
                    intent.putExtra(FacebookLoginActivity.APP_ID, context.getString(R.string.facebook_app_id));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    context.startActivity(intent);
                }
            };

            sanity.addAlert(SanityCheck.WARNING, title, message, action);
        }
        else
            sanity.clearAlert(title);
    }
}
