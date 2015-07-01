package edu.northwestern.cbits.purple_robot_manager.activities.probes;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.LegacyProbeViewerActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.ProbeViewerActivity;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe;

@SuppressLint("SimpleDateFormat")
public class LocationProbeActivity extends AppCompatActivity
{
    public static final String DB_TABLE_NAME = "DB_TABLE_NAME";
    private String _tableName = null;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_location_activity);

        this.getSupportActionBar().setTitle(R.string.title_location_history);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;

        this._tableName = this.getIntent().getStringExtra(LocationProbeActivity.DB_TABLE_NAME);

        if (this._tableName == null)
            this._tableName = LocationProbe.DB_TABLE;

        Cursor cursor = ProbeValuesProvider.getProvider(this).retrieveValues(this, this._tableName, LocationProbe.databaseSchema());

        while (cursor.moveToNext())
        {
            long time = ((long) cursor.getDouble(cursor.getColumnIndex(ProbeValuesProvider.TIMESTAMP))) * 1000;

            if (time < minTime)
                minTime = time;

            if (time > maxTime)
                maxTime = time;
        }

        String startDate = sdf.format(new Date(minTime));
        String endDate = sdf.format(new Date(maxTime));

        String subtitle = startDate + " (" + cursor.getCount() + ")";

        if (!startDate.equals(endDate))
            subtitle = startDate + " - " + endDate + " (" + cursor.getCount() + ")";

        this.getSupportActionBar().setSubtitle(subtitle);

        cursor.close();

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_probe_activity, menu);

        // MenuItem refresh = menu.findItem(R.id.menu_refresh);
        // MenuItem label = menu.findItem(R.id.menu_new_label);

        // refresh.setVisible(false);
        // label.setVisible(false);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        final int itemId = item.getItemId();

        if (itemId == R.id.menu_data_item)
        {
            Intent dataIntent = new Intent(this, LegacyProbeViewerActivity.class);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
                dataIntent = new Intent(this, ProbeViewerActivity.class);

            dataIntent.putExtra("probe_name", this.getIntent().getStringExtra("probe_name"));
            dataIntent.putExtra("probe_bundle", this.getIntent().getParcelableExtra("probe_bundle"));

            this.startActivity(dataIntent);
        }
        else if (itemId == android.R.id.home)
        {
            this.finish();
        }

        return true;
    }
}
