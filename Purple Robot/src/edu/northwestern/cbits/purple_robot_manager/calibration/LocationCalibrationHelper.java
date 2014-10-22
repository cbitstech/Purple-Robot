package edu.northwestern.cbits.purple_robot_manager.calibration;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.probes.LocationLabelActivity;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe;

public class LocationCalibrationHelper {
    public static void check(final Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());

        long lastCalibration = prefs.getLong("last_location_calibration", 0);
        long now = System.currentTimeMillis();

        Cursor cursor = ProbeValuesProvider.getProvider(context)
                .retrieveValues(context, LocationProbe.DB_TABLE,
                        LocationProbe.databaseSchema());

        int count = cursor.getCount();

        cursor.close();

        if (now - lastCalibration > (1000L * 60 * 60 * 24 * 30) && count > 500) {
            final SanityManager sanity = SanityManager.getInstance(context);

            final String title = context
                    .getString(R.string.title_location_label_check);
            String message = context
                    .getString(R.string.message_location_label_check);

            Runnable action = new Runnable() {
                public void run() {
                    Intent intent = new Intent(context,
                            LocationLabelActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    context.startActivity(intent);
                }
            };

            sanity.addAlert(SanityCheck.WARNING, title, message, action);
        }
    }
}
