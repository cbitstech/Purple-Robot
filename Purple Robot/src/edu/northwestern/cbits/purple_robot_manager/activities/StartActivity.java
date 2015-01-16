package edu.northwestern.cbits.purple_robot_manager.activities;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import net.hockeyapp.android.UpdateManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.RobotContentProvider;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.BaseSettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.LegacySettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.SettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.config.LegacyJSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.models.Model;
import edu.northwestern.cbits.purple_robot_manager.models.ModelManager;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.probes.features.Feature;
import edu.northwestern.cbits.purple_robot_manager.snapshots.SnapshotsActivity;
import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class StartActivity extends ActionBarActivity
{
    public static final String UPDATE_MESSAGE = "UPDATE_LIST_MESSAGE";
    public static final String DISPLAY_MESSAGE = "DISPLAY_MESSAGE";

    public static String UPDATE_DISPLAY = "UPDATE_LIST_DISPLAY";
    public static String DISPLAY_PROBE_NAME = "DISPLAY_PROBE_NAME";
    public static String DISPLAY_PROBE_VALUE = "DISPLAY_PROBE_VALUE";

    private BroadcastReceiver _receiver = null;

    private static String _statusMessage = null;

    private SharedPreferences prefs = null;
    protected String _lastProbe = "";

    private Menu _menu = null;

    private ContentObserver _observer = null;
    protected HashMap<String, Boolean> _enabledCache = new HashMap<String, Boolean>();

    private static OnSharedPreferenceChangeListener _prefListener = new OnSharedPreferenceChangeListener()
    {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
        {
            if (BaseSettingsActivity.USER_ID_KEY.equals(key))
            {
                Editor e = prefs.edit();

                e.remove(BaseSettingsActivity.USER_HASH_KEY);
                e.commit();
            }
        }
    };

    private SharedPreferences getPreferences(Context context)
    {
        if (this.prefs == null)
            this.prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        return this.prefs;
    }

    private void launchPreferences()
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            Intent intent = new Intent(this, LegacySettingsActivity.class);
            this.startActivity(intent);
        }
        else
        {
            Intent intent = new Intent(this, SettingsActivity.class);
            this.startActivity(intent);
        }
    }

    @Override
    @SuppressLint("SimpleDateFormat")
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        LogManager.getInstance(this);
        ModelManager.getInstance(this);

        SharedPreferences sharedPrefs = this.getPreferences(this);

        if (this.getPackageManager().getInstallerPackageName(this.getPackageName()) == null)
        {
            if (sharedPrefs.getBoolean(BaseSettingsActivity.CHECK_UPDATES_KEY, false))
                UpdateManager.register(this, "7550093e020b1a4a6df90f1e9dde68b6");
        }

        this.getSupportActionBar().setTitle(R.string.title_probe_readings);
        this.setContentView(R.layout.layout_startup_activity);

        ManagerService.setupPeriodicCheck(this);

        sharedPrefs.registerOnSharedPreferenceChangeListener(StartActivity._prefListener);

        final StartActivity me = this;

        final ListView listView = (ListView) this.findViewById(R.id.list_probes);

        this._receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, final Intent intent)
            {
                me.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (StartActivity.UPDATE_MESSAGE.equals(intent.getAction()))
                        {
                            final String message = intent.getStringExtra(StartActivity.DISPLAY_MESSAGE);

                            me.runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if (me.getString(R.string.message_reading_complete).equals(message) &&
                                        prefs.getBoolean("config_probes_enabled", false) == false)
                                    {
                                        StartActivity._statusMessage = null;
                                    }
                                    else
                                    {
                                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                                        StartActivity._statusMessage = sdf.format(new Date()) + ": " + message;
                                    }

                                    ActionBar bar = me.getSupportActionBar();
                                    bar.setSubtitle(StartActivity._statusMessage);
                                }
                            });
                        }
                    }
                });
            }
        };

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(me);

        IntentFilter filter = new IntentFilter();

        filter.addAction(StartActivity.UPDATE_MESSAGE);

        broadcastManager.registerReceiver(this._receiver, filter);

        final SimpleDateFormat sdf = new SimpleDateFormat("MMM d, H:mm:ss");

        Cursor c = this.getContentResolver().query(RobotContentProvider.RECENT_PROBE_VALUES, null, null, null, "recorded DESC");

        final CursorAdapter adapter = new CursorAdapter(this, c, true)
        {
            @Override
            public void bindView(final View view, Context context, Cursor cursor)
            {
                final String sensorName = cursor.getString(cursor.getColumnIndex("source"));

                final Probe probe = ProbeManager.probeForName(sensorName, me);

                final Date sensorDate = new Date(cursor.getLong(cursor.getColumnIndex("recorded")) * 1000);

                final String jsonString = cursor.getString(cursor.getColumnIndex("value"));

                Runnable r = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Bundle value = OutputPlugin.bundleForJson(me, jsonString);

                        String formattedValue = sensorName;

                        String displayName = formattedValue;

                        boolean enabled = true;

                        if (probe == null)
                            enabled = true;
                        else
                        {
                            Boolean probeEnabled = me._enabledCache.get(sensorName);

                            if (probeEnabled == null)
                            {
                                probeEnabled = Boolean.valueOf(probe.isEnabled(me));

                                me._enabledCache.put(sensorName, probeEnabled);
                            }

                            enabled = probeEnabled.booleanValue();
                        }

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

                        final String name = displayName + " (" + sdf.format(sensorDate) + ")";
                        final String display = formattedValue;

                        final boolean tintProbe = (enabled == false);

                        me.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                TextView nameField = (TextView) view.findViewById(R.id.text_sensor_name);
                                TextView valueField = (TextView) view.findViewById(R.id.text_sensor_value);

                                if (tintProbe)
                                {
                                    nameField.setTextColor(0xff808080);
                                    valueField.setTextColor(0xff808080);
                                }
                                else
                                {
                                    nameField.setTextColor(0xfff3f3f3);
                                    valueField.setTextColor(0xfff3f3f3);
                                }

                                nameField.setText(name);
                                valueField.setText(display);
                            }
                        });
                    }
                };

                Thread t = new Thread(r);
                t.start();
            }

            @SuppressLint("InflateParams")
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent)
            {
                LayoutInflater inflater = (LayoutInflater) me.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                View view = inflater.inflate(R.layout.layout_probe_row, null);

                this.bindView(view, context, cursor);

                return view;
            }
        };

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, final long id)
            {
                Runnable r = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Uri uri = ContentUris.withAppendedId(RobotContentProvider.RECENT_PROBE_VALUES, id);

                        Cursor c = me.getContentResolver().query(uri, null, null, null, null);

                        if (c.moveToNext()) {
                            final String sensorName = c.getString(c.getColumnIndex("source"));
                            final String jsonString = c.getString(c.getColumnIndex("value"));

                            final Bundle value = OutputPlugin.bundleForJson(me, jsonString);

                            me.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    final Probe probe = ProbeManager.probeForName(sensorName, me);

                                    if (probe != null) {
                                        Intent intent = probe.viewIntent(me);

                                        if (intent == null) {
                                            Intent dataIntent = new Intent(me, ProbeViewerActivity.class);

                                            dataIntent.putExtra("probe_name", sensorName);
                                            dataIntent.putExtra("probe_bundle", value);

                                            me.startActivity(dataIntent);
                                        } else {
                                            intent.putExtra("probe_name", sensorName);
                                            intent.putExtra("probe_bundle", value);

                                            me.startActivity(intent);
                                        }
                                    } else {
                                        Model model = ModelManager.getInstance(me).fetchModelByName(me, sensorName);

                                        Intent dataIntent = new Intent(me, ProbeViewerActivity.class);

                                        if (model != null)
                                            dataIntent.putExtra("probe_name", model.title(me));
                                        else
                                            dataIntent.putExtra("probe_name", sensorName);

                                        dataIntent.putExtra("is_model", true);
                                        dataIntent.putExtra("probe_bundle", value);
                                        me.startActivity(dataIntent);
                                    }
                                }
                            });
                        }

                        c.close();
                    }
                };

                Thread t = new Thread(r);
                t.start();
            }
        });

        final String savedPassword = prefs.getString("config_password", null);

        listView.setOnItemLongClickListener(new OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                Uri uri = ContentUris.withAppendedId(RobotContentProvider.RECENT_PROBE_VALUES, id);

                Cursor c = me.getContentResolver().query(uri, null, null, null, null);

                if (c.moveToNext())
                {
                    String sensorName = c.getString(c.getColumnIndex("source"));

                    final Probe probe = ProbeManager.probeForName(sensorName, me);

                    AlertDialog.Builder builder = new AlertDialog.Builder(me);
                    boolean inited = false;

                    if (probe != null)
                    {
                        builder = builder.setTitle(probe.title(me));
                        builder = builder.setMessage(probe.summary(me));

                        if (savedPassword == null || savedPassword.trim().length() == 0)
                        {
                            if (probe.isEnabled(me))
                                builder.setPositiveButton(R.string.button_disable, new OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1)
                                    {
                                        probe.disable(me);

                                        me._enabledCache.clear();

                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            else
                                builder.setPositiveButton(R.string.button_enable, new OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1)
                                    {
                                        probe.enable(me);

                                        me._enabledCache.clear();

                                        adapter.notifyDataSetChanged();
                                    }
                                });

                            builder.setNegativeButton(R.string.button_close, null);

                        }
                        else
                            builder.setPositiveButton(R.string.button_close, null);

                        inited = true;
                    }
                    else
                    {
                        final Model model = ModelManager.getInstance(me).fetchModelByName(me, sensorName);

                        if (model != null)
                        {
                            builder = builder.setTitle(model.title(me));
                            builder = builder.setMessage(model.summary(me));

                            if (savedPassword == null || savedPassword.trim().length() == 0)
                            {
                                if (model.isEnabled(me))
                                    builder.setPositiveButton(R.string.button_disable, new OnClickListener()
                                    {
                                        @Override
                                        public void onClick(DialogInterface arg0, int arg1)
                                        {
                                            model.disable(me);

                                            me._enabledCache.clear();

                                            adapter.notifyDataSetChanged();
                                        }
                                    });
                                else
                                    builder.setPositiveButton(R.string.button_enable, new OnClickListener()
                                    {
                                        @Override
                                        public void onClick(DialogInterface arg0, int arg1)
                                        {
                                            model.enable(me);

                                            me._enabledCache.clear();

                                            adapter.notifyDataSetChanged();
                                        }
                                    });

                                builder.setNegativeButton(R.string.button_close, null);
                            }
                            else
                                builder.setPositiveButton(R.string.button_close, null);

                            inited = true;
                        }
                        else
                            Log.e("PR", "Looking for model named " + sensorName);
                    }

                    if (inited)
                        builder.create().show();
                }

                return true;
            }
        });

        LegacyJSONConfigFile.getSharedFile(this.getApplicationContext());
    }

    @Override
    protected void onDestroy()
    {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.unregisterReceiver(this._receiver);

        super.onDestroy();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        ListView listView = (ListView) this.findViewById(R.id.list_probes);

        boolean probesEnabled = (listView.getVisibility() == View.VISIBLE);

        HashMap<String, Object> payload = new HashMap<String, Object>();
        payload.put("probes_enabled", probesEnabled);
        LogManager.getInstance(this).log("pr_main_ui_dismissed", payload);

        if (this._observer != null)
        {
            this.getContentResolver().unregisterContentObserver(this._observer);
            this._observer = null;
        }
    }

    protected void onNewIntent (Intent intent)
    {
        super.onNewIntent(intent);

        this.setIntent(intent);
    }

    private void setJsonUri(Uri jsonConfigUri)
    {
        if (jsonConfigUri.getScheme().equals("http") || jsonConfigUri.getScheme().equals("https"))
            jsonConfigUri = Uri.parse(jsonConfigUri.toString().replace("//pr-config/", "//"));
        else if (jsonConfigUri.getScheme().equals("cbits-prm") || jsonConfigUri.getScheme().equals("cbits-pr"))
        {
            Uri.Builder b = jsonConfigUri.buildUpon();

            b.scheme("http");

            jsonConfigUri = b.build();
        }

        EncryptionManager.getInstance().setConfigUri(this, jsonConfigUri);

        final StartActivity me = this;

        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(500);
                }
                catch (InterruptedException e)
                {

                }

                me.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        LegacyJSONConfigFile.updateFromOnline(me);
                    }
                });
            }
        };

        Thread t = new Thread(r);
        t.start();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        CrashManager.register(this, "7550093e020b1a4a6df90f1e9dde68b6", new CrashManagerListener()
        {
            @Override
            public Boolean onCrashesFound()
            {
                return true;
            }
        });

        this._enabledCache.clear();

        if (StartActivity._statusMessage != null)
            this.getSupportActionBar().setSubtitle(StartActivity._statusMessage);

        Uri incomingUri = this.getIntent().getData();

        final SharedPreferences prefs = this.getPreferences(this);

        final String savedPassword = prefs.getString("config_password", null);

        if (incomingUri != null)
        {
            if (savedPassword == null || savedPassword.equals(""))
            {
                this.setJsonUri(incomingUri);

                this.getIntent().setData(null);
            }
            else
                Toast.makeText(this, R.string.error_json_set_uri_password, Toast.LENGTH_LONG).show();
        }

        final ListView listView = (ListView) this.findViewById(R.id.list_probes);
        View logoView = this.findViewById(R.id.logo_view);

        boolean probesEnabled = prefs.getBoolean("config_probes_enabled", false);

        this.getSupportActionBar().setTitle(R.string.app_name);

        if (probesEnabled)
        {
            listView.setVisibility(View.VISIBLE);
            logoView.setVisibility(View.GONE);
        }
        else
        {
            listView.setVisibility(View.GONE);
            logoView.setVisibility(View.VISIBLE);
        }

        boolean showBackground = prefs.getBoolean("config_show_background", true);

        if (showBackground == false)
            logoView.setVisibility(View.GONE);

        HashMap<String, Object> payload = new HashMap<String, Object>();
        payload.put("probes_enabled", probesEnabled);
        LogManager.getInstance(this).log("pr_main_ui_shown", payload);

        final StartActivity me = this;

        if (this._observer == null)
        {
            this._observer = new ContentObserver(new Handler())
            {
                @Override
                public void onChange(boolean selfChange, Uri uri)
                {
                    ListAdapter listAdapter = listView.getAdapter();

                    Cursor c = listView.getContext().getContentResolver()
                            .query(RobotContentProvider.RECENT_PROBE_VALUES, null, null, null, "recorded DESC");

                    if (listAdapter instanceof CursorAdapter)
                    {
                        CursorAdapter adapter = (CursorAdapter) listAdapter;

                        adapter.changeCursor(c);
                    }

                    if (prefs.getBoolean("config_probes_enabled", false))
                        me.getSupportActionBar().setTitle(
                                String.format(me.getResources().getString(R.string.title_probes_count), c.getCount()));
                    else
                        me.getSupportActionBar().setTitle(R.string.app_name);

                    me.updateAlertIcon();
                };

                @Override
                public void onChange(boolean selfChange)
                {
                    this.onChange(selfChange, null);
                }
            };
        }

        this.getContentResolver().registerContentObserver(RobotContentProvider.RECENT_PROBE_VALUES, true,
                this._observer);
    }

    private void updateAlertIcon()
    {
        if (this._menu != null)
        {
            MenuItem diagIcon = this._menu.findItem(R.id.menu_diagnostic_item);

            diagIcon.setIcon(SanityManager.getInstance(this).getErrorIconResource());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        if (NfcActivity.canScan(this) == false)
            menu.removeItem(R.id.menu_nfc_item);

        this._menu = menu;

        this.updateAlertIcon();

        return true;
    }

    @SuppressLint("InflateParams")
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        final StartActivity me = this;

        final String savedPassword = this.getPreferences(this).getString("config_password", null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final int itemId = item.getItemId();

        if (itemId == R.id.menu_snapshot_item)
        {
            Intent snapIntent = new Intent(this, SnapshotsActivity.class);

            this.startActivity(snapIntent);
        }
        else if (itemId == R.id.menu_trigger_item)
        {
            builder.setTitle(R.string.title_fire_triggers);

            if (savedPassword == null || savedPassword.equals(""))
            {
                final List<Trigger> triggers = TriggerManager.getInstance(this).allTriggers();

                if (triggers.size() > 0)
                {
                    ArrayAdapter<Trigger> adapter = new ArrayAdapter<Trigger>(this,
                            android.R.layout.simple_list_item_1, triggers);

                    builder.setAdapter(adapter, new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            Trigger target = triggers.get(which);

                            target.execute(me, true);
                        }
                    });
                }
                else
                {
                    builder.setMessage(R.string.message_no_triggers);

                    builder.setPositiveButton(R.string.button_close, new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1)
                        {

                        }
                    });
                }
            }
            else
            {
                builder.setMessage(R.string.message_no_user_triggers);

                builder.setPositiveButton(R.string.button_close, new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1)
                    {

                    }
                });
            }
            builder.create().show();
        }
        else if (itemId == R.id.menu_label_item)
        {
            Intent labelIntent = new Intent(this, LabelActivity.class);

            labelIntent.putExtra(LabelActivity.LABEL_CONTEXT, "Home Screen");
            labelIntent.putExtra(LabelActivity.TIMESTAMP, ((double) System.currentTimeMillis()));

            this.startActivity(labelIntent);
        }
        else if (itemId == R.id.menu_upload_item)
        {
            LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this);
            Intent intent = new Intent(OutputPlugin.FORCE_UPLOAD);

            localManager.sendBroadcast(intent);
        }
        else if (itemId == R.id.menu_diagnostic_item)
        {
            Intent diagIntent = new Intent(this, DiagnosticActivity.class);

            this.startActivity(diagIntent);
        }
        else if (itemId == R.id.menu_settings_item)
        {
            if (savedPassword == null || savedPassword.equals(""))
                this.launchPreferences();
            else
            {
                builder.setMessage(R.string.dialog_password_prompt);
                builder.setPositiveButton(R.string.dialog_password_submit, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        AlertDialog alertDialog = (AlertDialog) dialog;

                        final EditText passwordField = (EditText) alertDialog.findViewById(R.id.text_dialog_password);

                        Editable password = passwordField.getText();

                        if (password.toString().equals(savedPassword))
                            me.launchPreferences();
                        else
                            Toast.makeText(me, R.string.toast_incorrect_password, Toast.LENGTH_LONG).show();

                        alertDialog.dismiss();
                    }
                });
                builder.setView(me.getLayoutInflater().inflate(R.layout.dialog_password, null));

                AlertDialog alert = builder.create();

                alert.show();
            }
        }
        else if (itemId == R.id.menu_nfc_item)
        {
            Intent nfcIntent = new Intent(this, NfcActivity.class);
            this.startActivity(nfcIntent);
        }

        return true;
    }
}
