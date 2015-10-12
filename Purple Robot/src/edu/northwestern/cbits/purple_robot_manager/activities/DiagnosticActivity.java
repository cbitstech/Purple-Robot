package edu.northwestern.cbits.purple_robot_manager.activities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.GoogleApiAvailability;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.PurpleRobotApplication;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.config.SchemeConfigFile;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.plugins.DataUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.HttpUploadPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPlugin;
import edu.northwestern.cbits.purple_robot_manager.plugins.OutputPluginManager;
import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

@SuppressLint("SimpleDateFormat")
public class DiagnosticActivity extends AppCompatActivity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.getSupportActionBar().setTitle(R.string.activity_diagnostic_title);

        this.setContentView(R.layout.layout_diagnostic_activity);
    }

    @SuppressWarnings("deprecation")
    protected void onResume()
    {
        super.onResume();

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView userId = (TextView) this.findViewById(R.id.user_id_value);
        TextView probeStatus = (TextView) this.findViewById(R.id.probe_status_value);
        TextView uploadStatus = (TextView) this.findViewById(R.id.upload_status_value);
        TextView lastUpload = (TextView) this.findViewById(R.id.last_upload_value);
        TextView prVersion = (TextView) this.findViewById(R.id.pr_version_value);
        TextView gpsVersion = (TextView) this.findViewById(R.id.play_services_version_value);
        TextView permissionsStatus = (TextView) this.findViewById(R.id.pr_permissions_value);

        userId.setText("\"" + EncryptionManager.getInstance().getUserId(this) + "\"");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean probeEnabled = prefs.getBoolean("config_probes_enabled", false);

        if (probeEnabled)
            probeStatus.setText(R.string.probe_status_enabled);
        else
            probeStatus.setText(R.string.probe_status_disabled);

        PurpleRobotApplication.fixPreferences(this, true);

        boolean uploadEnabled = DataUploadPlugin.uploadEnabled(this);

        if (uploadEnabled)
        {
            boolean wifiOnly = DataUploadPlugin.restrictToWifi(prefs);
            boolean powerOnly = DataUploadPlugin.restrictToCharging(prefs);

            if (wifiOnly && powerOnly)
                uploadStatus.setText(R.string.upload_status_enabled_wifi_charging_only);
            else if (wifiOnly)
                uploadStatus.setText(R.string.upload_status_enabled_wifi_only);
            else if (powerOnly)
                uploadStatus.setText(R.string.upload_status_enabled_charging_only);
            else
                uploadStatus.setText(R.string.upload_status_enabled);
        }
        else
            uploadStatus.setText(R.string.upload_status_disabled);

        long lastUploadTime = DataUploadPlugin.lastUploadTime(prefs);
        long lastPayloadSize = DataUploadPlugin.lastUploadSize(prefs) / 1024;

        if (lastUploadTime != -1 && lastPayloadSize != -1)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d - HH:mm:ss");

            String dateString = sdf.format(new Date(lastUploadTime));

            lastUpload.setText(String.format(this.getString(R.string.last_upload_format), dateString, lastPayloadSize));
        }

        TextView uploadCount = (TextView) this.findViewById(R.id.pending_files_value);
        uploadCount.setText(this.getString(R.string.pending_files_file, DataUploadPlugin.pendingFileCount(this)));

        if (prefs.getBoolean("config_enable_log_server", false))
        {
            TextView pendingEventsCount = (TextView) this.findViewById(R.id.pending_log_events_value);
            pendingEventsCount.setText(this.getString(R.string.pending_log_events_value, LogManager.getInstance(this).pendingEventsCount()));
        }

        try
        {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            prVersion.setText(pInfo.versionName);
        }
        catch (NameNotFoundException e)
        {
            LogManager.getInstance(this).logException(e);
        }

        gpsVersion.setText("" + GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE);

        TextView okText = (TextView) this.findViewById(R.id.pr_error_none_value);
        LinearLayout errorList = (LinearLayout) this.findViewById(R.id.pr_error_list);
        errorList.removeAllViews();

        final SanityManager sanity = SanityManager.getInstance(this);

        if (sanity.getErrorLevel() != SanityCheck.OK)
        {
            errorList.setVisibility(View.VISIBLE);
            okText.setVisibility(View.GONE);

            Map<String, String> errors = sanity.errors();

            if (errors.size() > 0)
            {
                errorList.setVisibility(View.VISIBLE);

                for (String error : errors.keySet())
                {
                    TextView errorLine = new TextView(this);
                    errorLine.setText(errors.get(error));
                    errorLine.setTextColor(0xffff4444);
                    errorLine.setTextSize(18);

                    LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                            LayoutParams.WRAP_CONTENT);
                    layout.setMargins(0, 0, 0, 10);
                    errorLine.setLayoutParams(layout);

                    final String errorKey = error;

                    errorLine.setOnClickListener(new OnClickListener()
                    {
                        public void onClick(View view)
                        {
                            sanity.runActionForAlert(errorKey);
                        }

                    });

                    errorList.addView(errorLine);
                }
            }

            Map<String, String> warnings = sanity.warnings();

            if (warnings.size() > 0)
            {
                for (String error : warnings.keySet())
                {
                    TextView errorLine = new TextView(this);
                    errorLine.setText(warnings.get(error));
                    errorLine.setTextColor(0xffffbb33);
                    errorLine.setTextSize(18);

                    LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                            LayoutParams.WRAP_CONTENT);
                    layout.setMargins(0, 0, 0, 10);
                    errorLine.setLayoutParams(layout);

                    final String errorKey = error;

                    errorLine.setOnClickListener(new OnClickListener()
                    {
                        public void onClick(View view)
                        {
                            sanity.runActionForAlert(errorKey);
                        }

                    });

                    errorList.addView(errorLine);
                }
            }
        }
        else
        {
            errorList.setVisibility(View.GONE);
            okText.setVisibility(View.VISIBLE);
        }

        TextView triggerList = (TextView) this.findViewById(R.id.installed_triggers_value);

        StringBuilder triggerSb = new StringBuilder();

        List<Trigger> triggers = TriggerManager.getInstance(this).allTriggers();

        if (triggers.size() > 0)
        {
            for (Trigger trigger : triggers)
            {
                if (triggerSb.length() > 0)
                    triggerSb.append("\n");

                triggerSb.append(trigger.getDiagnosticString(this));
            }

            triggerList.setText(triggerSb.toString());
        }
        else
            triggerList.setText(R.string.no_installed_triggers_label);

        TextView sensorsList = (TextView) this.findViewById(R.id.available_sensors_value);
        StringBuilder sb = new StringBuilder();

        SensorManager sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

        for (Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL))
        {
            if (sb.length() > 0)
                sb.append("\n");

            sb.append(s.getName());
        }

        sensorsList.setText(sb.toString());

        String ipAddress = null;

        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (wifiInfo != null)
        {
            int ip = wifiInfo.getIpAddress();

            ipAddress = Formatter.formatIpAddress(ip);
        }
        else
        {
            try
            {
                Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();

                NetworkInterface iface = null;

                while (ifaces.hasMoreElements() && (iface = ifaces.nextElement()) != null && ipAddress == null)
                {
                    if (!iface.getName().equals("lo"))
                    {
                        Enumeration<InetAddress> ips = iface.getInetAddresses();
                        InetAddress ipAddr = null;

                        while (ips.hasMoreElements() && (ipAddr = ips.nextElement()) != null)
                        {
                            ipAddress = ipAddr.getHostAddress();
                        }
                    }
                }
            }
            catch (SocketException e)
            {
                LogManager.getInstance(this).logException(e);
            }
        }

        if (ipAddress != null)
        {
            TextView ipAddressView = (TextView) this.findViewById(R.id.ip_address_value);
            ipAddressView.setText(ipAddress);
        }

        final DiagnosticActivity me = this;

        permissionsStatus.setText(PermissionsActivity.status(this));

        switch(PermissionsActivity.statusCode(this))
        {
            case REQUIRED_MISSING:
                permissionsStatus.setTextColor(0xffff4444);
                break;
            case OPTIONAL_MISSING:
                permissionsStatus.setTextColor(0xffffbb33);
                break;
        }

        permissionsStatus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(me, PermissionsActivity.class);
                me.startActivity(intent);
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_diagnostics, menu);

        return true;
    }

    public void onBackPressed()
    {
        if (this.isTaskRoot())
        {
            Intent intent = new Intent(this, StartActivity.class);
            this.startActivity(intent);
        }

        this.finish();
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        final int itemId = item.getItemId();

        if (itemId == android.R.id.home)
        {
            this.onBackPressed();

        }
        if (itemId == R.id.menu_email_item)
        {
            StringBuilder message = new StringBuilder();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            String newline = System.getProperty("line.separator");

            message.append(this.getString(R.string.user_id_label));
            message.append(newline);
            message.append("\"" + EncryptionManager.getInstance().getUserId(this) + "\"");

            message.append(newline);
            message.append(newline);

            message.append(this.getString(R.string.probe_status_label));
            message.append(newline);

            boolean probeEnabled = prefs.getBoolean("config_probes_enabled", false);

            if (probeEnabled)
                message.append(this.getString(R.string.probe_status_enabled));
            else
                message.append(this.getString(R.string.probe_status_disabled));

            message.append(newline);
            message.append(newline);

            message.append(this.getString(R.string.upload_status_label));
            message.append(newline);

            PurpleRobotApplication.fixPreferences(this, true);

            boolean uploadEnabled = prefs.getBoolean("config_enable_data_server", false);

            if (uploadEnabled)
            {
                boolean wifiOnly = prefs.getBoolean("config_restrict_data_wifi", true);

                if (wifiOnly)
                    message.append(this.getString(R.string.upload_status_enabled_wifi_only));
                else
                    message.append(this.getString(R.string.upload_status_enabled));
            }
            else
                message.append(this.getString(R.string.upload_status_disabled));

            message.append(newline);
            message.append(newline);

            message.append(this.getString(R.string.last_upload_label));
            message.append(newline);

            if (prefs.contains("http_last_upload") && prefs.contains("http_last_payload_size"))
            {
                long lastUploadTime = prefs.getLong("http_last_upload", 0);
                long lastPayloadSize = prefs.getLong("http_last_payload_size", 0) / 1024;

                SimpleDateFormat sdf = new SimpleDateFormat("MMM d - HH:mm:ss");

                String dateString = sdf.format(new Date(lastUploadTime));

                message.append(String.format(this.getString(R.string.last_upload_format), dateString, lastPayloadSize));
            }
            else
                message.append(this.getString(R.string.last_upload_placeholder));

            message.append(newline);
            message.append(newline);

            OutputPlugin plugin = OutputPluginManager.sharedInstance.pluginForClass(this, HttpUploadPlugin.class);

            if (plugin instanceof HttpUploadPlugin)
            {
                HttpUploadPlugin http = (HttpUploadPlugin) plugin;

                message.append(this.getString(R.string.robot_pending_count_label));
                message.append(newline);
                message.append(this.getString(R.string.pending_files_file, http.pendingFilesCount()));

                message.append(newline);
                message.append(newline);
            }

            message.append(this.getString(R.string.pr_errors_label));
            message.append(newline);

            final SanityManager sanity = SanityManager.getInstance(this);

            if (sanity.getErrorLevel() != SanityCheck.OK)
            {
                Map<String, String> errors = sanity.errors();

                if (errors.size() > 0)
                {
                    for (String error : errors.keySet())
                    {
                        message.append(errors.get(error));
                        message.append(newline);
                    }

                    message.append(newline);
                }

                Map<String, String> warnings = sanity.warnings();

                if (warnings.size() > 0)
                {
                    for (String error : warnings.keySet())
                    {
                        message.append(warnings.get(error));
                        message.append(newline);
                    }

                    message.append(newline);
                }
            }
            else
            {
                message.append(this.getString(R.string.pr_errors_none_label));
                message.append(newline);
            }

            message.append(newline);

            message.append(this.getString(R.string.pr_version_label));
            message.append(newline);

            try
            {
                PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
                message.append(pInfo.versionName);
            }
            catch (NameNotFoundException e)
            {
                LogManager.getInstance(this).logException(e);
            }

            message.append(newline);
            message.append(newline);

            message.append(this.getString(R.string.play_services_version_label));
            message.append(newline);
            message.append("" + GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE);
            message.append(newline);
            message.append(newline);

            message.append(this.getString(R.string.available_sensors_label));
            message.append(newline);

            StringBuilder sb = new StringBuilder();

            SensorManager sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

            for (Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL))
            {
                if (sb.length() > 0)
                    sb.append("\n");

                sb.append(s.getName());
            }

            message.append(sb.toString());

            try
            {
                Intent intent = new Intent(Intent.ACTION_SEND);

                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_SUBJECT, this.getString(R.string.email_diagnostic_subject));
                intent.putExtra(Intent.EXTRA_TEXT, message.toString());

                SchemeConfigFile scheme = new SchemeConfigFile(this);

                File cacheDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

                if (cacheDir.exists() == false)
                    cacheDir.mkdirs();

                File configFile = new File(cacheDir, "config.scm");

                FileOutputStream fout = new FileOutputStream(configFile);

                fout.write(scheme.toString().getBytes(Charset.defaultCharset().name()));

                fout.flush();
                fout.close();

                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(configFile));

                this.startActivity(intent);
            }
            catch (ActivityNotFoundException e)
            {
                try
                {
                    Intent mailIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:c-karr@northwestern.edu"));
                    mailIntent.putExtra(Intent.EXTRA_SUBJECT, this.getString(R.string.email_diagnostic_subject));
                    mailIntent.putExtra(Intent.EXTRA_TEXT, message.toString());

                    this.startActivity(mailIntent);
                }
                catch (ActivityNotFoundException ex)
                {
                    Toast.makeText(this, R.string.toast_mail_not_found, Toast.LENGTH_LONG).show();
                }
            }
            catch (IOException e)
            {
                LogManager.getInstance(this).logException(e);
            }
        }

        return true;
    }
}
