package edu.northwestern.cbits.purple_robot_manager.calibration;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.services.WeatherUndergroundProbe;

public class WeatherUndergroundCalibrationHelper
{
    public static void check(final Context context, boolean isEnabled)
    {
        final SanityManager sanity = SanityManager.getInstance(context);
        final String title = context.getString(R.string.title_weather_underground_check);

        sanity.clearAlert(title);

        if (isEnabled) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String apiKey = prefs.getString(WeatherUndergroundProbe.API_KEY, WeatherUndergroundProbe.DEFAULT_API_KEY);

            if (apiKey == null || apiKey.trim().length() == 0)
            {
                SharedPreferences.Editor e = prefs.edit();
                e.putLong(WeatherUndergroundProbe.LAST_CHECK, 0);
                e.commit();

                String message = context.getString(R.string.message_weather_underground_check);

                Runnable action = new Runnable() {
                    @Override
                    public void run() {

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(context.getString(R.string.url_weather_api_key)));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        context.startActivity(intent);
                    }
                };

                sanity.addAlert(SanityCheck.WARNING, title, message, action);
            }
        }
    }
}
