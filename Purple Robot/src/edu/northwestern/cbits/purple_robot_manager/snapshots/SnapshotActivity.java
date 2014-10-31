package edu.northwestern.cbits.purple_robot_manager.snapshots;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.RobotContentProvider;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.features.Feature;

public class SnapshotActivity extends ActionBarActivity
{
    private String _nextPayload = null;

    private String _audioPath = null;

    private boolean _isPlaying = false;

    @SuppressLint("SimpleDateFormat")
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.getSupportActionBar().setSubtitle("TODO: PROBE COUNT HERE");
        this.setContentView(R.layout.layout_snapshot_activity);

        final SnapshotActivity me = this;

        final SimpleDateFormat sdf = new SimpleDateFormat("MMM d, H:mm:ss");

        ListView listView = (ListView) this.findViewById(R.id.list_probes);

        final ArrayList<JSONObject> jsonObjects = new ArrayList<JSONObject>();

        Uri queryUri = ContentUris.withAppendedId(RobotContentProvider.SNAPSHOTS, this.getIntent()
                .getLongExtra("id", 0));

        Cursor c = this.getContentResolver().query(queryUri, null, null, null, null);

        if (c.moveToNext())
        {
            String source = c.getString(c.getColumnIndex("source"));
            long recorded = c.getLong(c.getColumnIndex("recorded"));
            String jsonString = c.getString(c.getColumnIndex("value"));

            this._audioPath = c.getString(c.getColumnIndex("audio_file"));

            if (this._audioPath != null)
            {
                File f = new File(this._audioPath);

                if (f.exists() == false)
                    this._audioPath = null;
            }

            this.getSupportActionBar().setTitle(source + ": " + sdf.format(new Date(recorded)));

            try
            {
                JSONArray items = new JSONArray(jsonString);

                this.getSupportActionBar().setSubtitle(this.getString(R.string.snapshot_probe_count, items.length()));

                for (int i = 0; i < items.length(); i++)
                {
                    JSONObject item = items.getJSONObject(i);

                    jsonObjects.add(item);
                }
            }
            catch (JSONException e)
            {
                LogManager.getInstance(this).logException(e);
            }
        }

        c.close();

        ArrayAdapter<JSONObject> adapter = new ArrayAdapter<JSONObject>(this, R.layout.layout_probe_row, jsonObjects)
        {
            public View getView(int position, View convertView, ViewGroup parent)
            {
                if (convertView == null)
                {
                    LayoutInflater inflater = LayoutInflater.from(me);
                    convertView = inflater.inflate(R.layout.layout_probe_row, parent, false);
                }

                JSONObject json = this.getItem(position);

                try
                {
                    Bundle value = OutputPlugin.bundleForJson(me, json.getString("value").toString());
                    String sensorName = json.getString("probe");

                    final Probe probe = ProbeManager.probeForName(sensorName, me);

                    Date sensorDate = new Date(json.getLong("recorded") * 1000);

                    String formattedValue = sensorName;

                    String displayName = formattedValue;

                    if (probe != null && value != null)
                    {
                        try
                        {
                            displayName = probe.title(me);
                            formattedValue = probe.summarizeValue(me, value);
                        }
                        catch (Exception e)
                        {
                            LogManager.getInstance(me).logException(e);
                        }

                        Bundle sensor = value.getBundle("SENSOR");

                        if (sensor != null && sensor.containsKey("POWER"))
                        {
                            DecimalFormat df = new DecimalFormat("#.##");

                            formattedValue += " (" + df.format(sensor.getDouble("POWER")) + " mA)";
                        }
                    }
                    else if (value.containsKey(Feature.FEATURE_VALUE))
                        formattedValue = value.get(Feature.FEATURE_VALUE).toString();
                    else if (value.containsKey("PREDICTION") && value.containsKey("MODEL_NAME"))
                    {
                        formattedValue = value.get("PREDICTION").toString();
                        displayName = value.getString("MODEL_NAME");
                    }

                    String name = displayName + " (" + sdf.format(sensorDate) + ")";
                    String display = formattedValue;

                    TextView nameField = (TextView) convertView.findViewById(R.id.text_sensor_name);
                    TextView valueField = (TextView) convertView.findViewById(R.id.text_sensor_value);

                    nameField.setText(name);
                    valueField.setText(display);
                }
                catch (JSONException e)
                {
                    LogManager.getInstance(me).logException(e);
                }

                return convertView;
            }
        };

        listView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
            {
                try
                {
                    me.displayProbe(jsonObjects.get(position));
                }
                catch (JSONException e)
                {
                    LogManager.getInstance(me).logException(e);
                }
            }
        });

        listView.setAdapter(adapter);

        MapsInitializer.initialize(this);

        final WebView webView = (WebView) this.findViewById(R.id.json_view);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(false);

        webView.setWebViewClient(new WebViewClient()
        {
            public void onPageFinished(WebView view, String url)
            {
                if (me._nextPayload != null)
                {
                    webView.loadData(me._nextPayload, "text/html", null);

                    me._nextPayload = null;
                }
            }
        });

        webView.setVisibility(View.VISIBLE);
    }

    protected void onResume()
    {
        super.onResume();

        ListView listView = (ListView) this.findViewById(R.id.list_probes);

        try
        {
            JSONObject item = (JSONObject) listView.getItemAtPosition(0);

            try
            {
                this.displayProbe(item);
            }
            catch (JSONException e)
            {
                LogManager.getInstance(this).logException(e);
            }
        }
        catch (IndexOutOfBoundsException e)
        {
            this.finish();
        }
    }

    private void displayProbe(JSONObject object) throws JSONException
    {
        final WebView webView = (WebView) this.findViewById(R.id.json_view);

        String probeName = object.getString("probe");

        if (LocationProbe.NAME.equals(probeName))
        {
            webView.setVisibility(View.GONE);

            JSONObject value = object.getJSONObject("value");

            SupportMapFragment fragment = (SupportMapFragment) this.getSupportFragmentManager().findFragmentById(
                    R.id.map_fragment);

            final GoogleMap map = fragment.getMap();

            if (map != null)
            {
                double latitude = value.getDouble("LATITUDE");
                double longitude = value.getDouble("LONGITUDE");

                LatLng location = new LatLng(latitude, longitude);

                MarkerOptions marker = new MarkerOptions();
                marker.position(new LatLng(latitude, longitude));
                marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

                map.addMarker(marker);
                map.setMapType(GoogleMap.MAP_TYPE_HYBRID);

                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(location, 15);

                map.moveCamera(update);
            }
            else
                Log.e("PR", "NULL MAP");
        }
        else
        {
            webView.setVisibility(View.VISIBLE);

            String code = object.toString(2);

            try
            {
                code = URLEncoder.encode(code, "utf-8").replaceAll("\\+", "%20");
            }
            catch (UnsupportedEncodingException e)
            {
                LogManager.getInstance(this).logException(e);
            }

            code = "<!DOCTYPE html><html><body style=\"background-color: #000; color: #00c000;\"><pre>" + code
                    + "</pre></body></html>";

            this._nextPayload = code;

            webView.loadData(this._nextPayload, "text/html", null);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {

        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_snapshot, menu);

        if (this._audioPath == null)
        {
            MenuItem menuItem = menu.findItem(R.id.menu_play_snapshot_item);
            menuItem.setVisible(false);
        }

        return true;
    }

    public boolean onOptionsItemSelected(final MenuItem item)
    {
        final SnapshotActivity me = this;

        if (item.getItemId() == R.id.menu_play_snapshot_item)
        {
            if (this._isPlaying == false)
            {
                this._isPlaying = true;
                item.setVisible(false);

                MediaPlayer player = new MediaPlayer();

                try
                {
                    player.setDataSource(this._audioPath);

                    player.setOnCompletionListener(new OnCompletionListener()
                    {
                        public void onCompletion(MediaPlayer player)
                        {
                            player.release();

                            me._isPlaying = false;
                            item.setVisible(true);

                        }
                    });

                    player.prepare();
                    player.start();
                }
                catch (IOException e)
                {
                    this._isPlaying = false;
                }
            }

        }

        return true;
    }

}
