package edu.northwestern.cbits.purple_robot_manager.activities;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.config.LegacyJSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;

public class MarkTimeActivity extends AppCompatActivity
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
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

                if (nameValue.trim().length() == 0)
                    nameValue = me.getString(R.string.value_anonymous_timestamp);

                me._timestamps.append(now, nameValue);

                JavaScriptEngine js = new JavaScriptEngine(me);
                js.emitReading(LegacyJSONConfigFile.toSlug(nameValue), nameValue);

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
        final ArrayList<String> names = new ArrayList<String>();

        for (int i = 0; i < this._timestamps.size(); i++)
        {
            indices.add(this._timestamps.keyAt(i));

            String name = this._timestamps.valueAt(i);

            if (names.contains(name) == false)
                names.add(name);
        }

        Collections.sort(indices);
        Collections.sort(names);

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
        list.setEmptyView(this.findViewById(R.id.placeholder_timestamps));

        final AutoCompleteTextView nameField = (AutoCompleteTextView) this.findViewById(R.id.text_label_text);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                String name = me._timestamps.valueAt(position);

                nameField.setText(name);
            }
        });

        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id)
            {
                Date date = new Date(me._timestamps.keyAt(position));

                String dateString = timeFormat.format(date) + " @ " + dateFormat.format(date);

                AlertDialog.Builder builder = new AlertDialog.Builder(me);
                builder.setTitle(R.string.title_clear_timestamp);
                builder.setMessage(me.getString(R.string.message_clear_timestamp, me._timestamps.valueAt(position), dateString));

                builder.setPositiveButton(R.string.action_clear, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        me._timestamps.removeAt(position);
                        me.saveTimestamps();

                        me.refreshList();
                    }
                });

                builder.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // Do nothing...
                    }
                });

                builder.create().show();

                return true;
            }
        });

        final AutoCompleteTextView name = (AutoCompleteTextView) this.findViewById(R.id.text_label_text);

        ArrayAdapter<String> namesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, names);
        name.setAdapter(namesAdapter);
        name.setThreshold(1);
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
        final MarkTimeActivity me = this;

        final int itemId = item.getItemId();

        if (itemId == android.R.id.home)
        {
            this.finish();
        }
        else if (itemId == R.id.menu_mail)
        {
            StringBuffer message = new StringBuffer();
            String newline = System.getProperty("line.separator");
            message.append("Time Point\tTimestamp");
            message.append(newline);

            for (int i = 0; i < this._timestamps.size(); i++)
            {
                message.append(this._timestamps.valueAt(i) + "\t" + this._timestamps.keyAt(i));
                message.append(newline);
            }

            try
            {
                Intent intent = new Intent(Intent.ACTION_SEND);

                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_SUBJECT, this.getString(R.string.email_timetamp_subject));
                intent.putExtra(Intent.EXTRA_TEXT, this.getString(R.string.email_timetamp_message));

                File cacheDir = this.getExternalCacheDir();
                File configFile = new File(cacheDir, "time-points.txt");

                FileOutputStream fout = new FileOutputStream(configFile);

                fout.write(message.toString().getBytes(Charset.defaultCharset().name()));

                fout.flush();
                fout.close();

                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(configFile));

                this.startActivity(intent);
            }
            catch (ActivityNotFoundException e)
            {
                Toast.makeText(this, R.string.toast_mail_not_found, Toast.LENGTH_LONG).show();
            }
            catch (FileNotFoundException e)
            {
                LogManager.getInstance(this).logException(e);
            }
            catch (IOException e)
            {
                LogManager.getInstance(this).logException(e);
            }
        }
        else if (itemId == R.id.menu_clear)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_clear_timestamps);
            builder.setMessage(R.string.title_clear_timestamps);

            builder.setPositiveButton(R.string.action_clear, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    me._timestamps.clear();
                    me.saveTimestamps();

                    me.refreshList();
                }
            });

            builder.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    // Do nothing...
                }
            });

            builder.create().show();
        }

        return true;
    }
}
