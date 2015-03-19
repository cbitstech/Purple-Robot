package edu.northwestern.cbits.purple_robot_manager.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import edu.northwestern.cbits.purple_robot_manager.R;

public class MarkTimeActivity extends ActionBarActivity
{
    private static final String SAVED_TIMESTAMPS = "edu.northwestern.cbits.purple_robot_manager.activitiesMarkTimeActivity.SAVED_TIMESTAMPS";

    private LongSparseArray<String> _timestamps = new LongSparseArray<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_mark_time_activity);

        getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        this.getSupportActionBar().setTitle(R.string.title_mark_time);

        this.loadTimestamps();
    }

    private void loadTimestamps()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        try
        {
            JSONArray saved = new JSONArray(prefs.getString(MarkTimeActivity.SAVED_TIMESTAMPS, "[]"));

            for (int i = 0; i < saved.length(); i++)
            {
                try
                {
                    JSONObject ts = saved.getJSONObject(i);

                    _timestamps.append(ts.getLong("t"), ts.getString("name"));
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        this.setIntent(intent);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onResume()
    {
        super.onResume();

        final MarkTimeActivity me = this;

        ImageView createButton = (ImageView) this.findViewById(R.id.create_button);
        final AutoCompleteTextView name = (AutoCompleteTextView) this.findViewById(R.id.text_label_text);

        createButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                long now = System.currentTimeMillis();
                String nameValue = name.getText().toString();
                long[] spec = { 0L, 205L };

                Vibrator vibrator = (Vibrator) me.getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(spec, -1);

                me._timestamps.append(now, nameValue);
                
                // TODO: XMIT                
                
                me.saveTimestamps();
                me.refreshList();
            }
        });

        this.refreshList();
    }

    private void refreshList()
    {
        final MarkTimeActivity me = this;

        ListView list = (ListView) this.findViewById(R.id.list_timestamps);
        list.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        list.setStackFromBottom(true);

        final ArrayList<Long> indices = new ArrayList<Long>();

        for (int i = 0; i < this._timestamps.size(); i++)
        {
            indices.add(this._timestamps.keyAt(i));
        }

        Collections.sort(indices);

        final java.text.DateFormat dateFormat = DateFormat.getMediumDateFormat(this);
        final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(this);

        ArrayAdapter<Long> adapter = new ArrayAdapter<Long>(this, R.layout.layout_timestamp_row, R.id.name_label, indices)
        {
            public View getView(int position, View convertView, ViewGroup parent)
            {
                convertView = super.getView(position, convertView, parent);

                TextView dateLabel = (TextView) convertView.findViewById(R.id.date_label);
                TextView nameLabel = (TextView) convertView.findViewById(R.id.name_label);

                long timestamp = indices.get(position);
                String name = me._timestamps.valueAt(position);

                Date date = new Date(timestamp);

                dateLabel.setText(timeFormat.format(date) + " @ " + dateFormat.format(date));
                nameLabel.setText(name);

                return convertView;
            }
        };

        list.setAdapter(adapter);
    }

    private void saveTimestamps()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        try
        {
            JSONArray saved = new JSONArray();

            for (int i = 0; i < this._timestamps.size(); i++)
            {
                JSONObject ts = new JSONObject();
                ts.put("t", this._timestamps.keyAt(i));
                ts.put("name", this._timestamps.valueAt(i));

                saved.put(ts);
            }

            SharedPreferences.Editor e = prefs.edit();
            e.putString(MarkTimeActivity.SAVED_TIMESTAMPS, saved.toString());
            e.commit();
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_mark_time_activity, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        final int itemId = item.getItemId();

        if (itemId == R.id.menu_cancel)
        {
            this.finish();
        }

        return true;
    }
}
