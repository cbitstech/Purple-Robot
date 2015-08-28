package edu.northwestern.cbits.purple_robot_manager.plugins;

import java.io.File;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.PowerHelper;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.WiFiHelper;
import edu.northwestern.cbits.purple_robot_manager.activities.StartActivity;
import edu.northwestern.cbits.purple_robot_manager.logging.LiberalSSLSocketFactory;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

@SuppressLint("NewApi")
public abstract class DataUploadPlugin extends OutputPlugin
{
    public final static String USER_HASH_KEY = "UserHash";
    public final static String OPERATION_KEY = "Operation";
    public final static String PAYLOAD_KEY = "Payload";
    public final static String CHECKSUM_KEY = "Checksum";
    public final static String CONTENT_LENGTH_KEY = "ContentLength";
    public final static String STATUS_KEY = "Status";

    private final static String CACHE_DIR = "pending_uploads";
    public static final String TRANSMIT_KEY = "TRANSMIT";

    public static final String RESTRICT_TO_WIFI = "config_restrict_data_wifi";
    private static final boolean RESTRICT_TO_WIFI_DEFAULT = true;
    public static final String UPLOAD_URI = "config_data_server_uri";

    private static final String RESTRICT_TO_CHARGING = "config_restrict_data_charging";
    private static final boolean RESTRICT_TO_CHARGING_DEFAULT = false;

    public static final String ALLOW_ALL_SSL_CERTIFICATES = "config_http_liberal_ssl";
    public static final boolean ALLOW_ALL_SSL_CERTIFICATES_DEFAULT = true;

    protected static final int RESULT_SUCCESS = 0;
    protected static final int RESULT_NO_CONNECTION = 1;
    protected static final int RESULT_ERROR = 2;
    protected static final int RESULT_NO_POWER = 3;

    public File getPendingFolder()
    {
        SharedPreferences prefs = HttpUploadPlugin.getPreferences(this.getContext());

        File internalStorage = this.getContext().getFilesDir();

        if (prefs.getBoolean(OutputPlugin.USE_EXTERNAL_STORAGE, false))
            internalStorage = this.getContext().getExternalFilesDir(null);

        if (internalStorage != null && !internalStorage.exists())
            internalStorage.mkdirs();

        File pendingFolder = new File(internalStorage, DataUploadPlugin.CACHE_DIR);

        if (pendingFolder != null && !pendingFolder.exists())
            pendingFolder.mkdirs();

        return pendingFolder;
    }

    public static boolean restrictToWifi(SharedPreferences prefs)
    {
        try
        {
            return prefs.getBoolean(DataUploadPlugin.RESTRICT_TO_WIFI, DataUploadPlugin.RESTRICT_TO_WIFI_DEFAULT);
        }
        catch (ClassCastException e)
        {
            String enabled = prefs.getString(DataUploadPlugin.RESTRICT_TO_WIFI,
                    "" + DataUploadPlugin.RESTRICT_TO_WIFI_DEFAULT).toLowerCase(Locale.ENGLISH);

            boolean isRestricted = ("false".equals(enabled) == false);

            Editor edit = prefs.edit();
            edit.putBoolean(DataUploadPlugin.RESTRICT_TO_WIFI, isRestricted);
            edit.commit();

            return isRestricted;
        }
    }

    public static boolean restrictToCharging(SharedPreferences prefs)
    {
        try
        {
            return prefs.getBoolean(DataUploadPlugin.RESTRICT_TO_CHARGING, DataUploadPlugin.RESTRICT_TO_CHARGING_DEFAULT);
        }
        catch (ClassCastException e)
        {
            String enabled = prefs.getString(DataUploadPlugin.RESTRICT_TO_CHARGING,
                    "" + DataUploadPlugin.RESTRICT_TO_CHARGING_DEFAULT).toLowerCase(Locale.ENGLISH);

            boolean isRestricted = ("false".equals(enabled) == false);

            Editor edit = prefs.edit();
            edit.putBoolean(DataUploadPlugin.RESTRICT_TO_CHARGING, isRestricted);
            edit.commit();

            return isRestricted;
        }
    }

    public boolean shouldAttemptUpload(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final DataUploadPlugin me = this;

        if (DataUploadPlugin.restrictToWifi(prefs))
        {
            if (WiFiHelper.wifiAvailable(context) == false)
            {
                me.broadcastMessage(context.getString(R.string.message_wifi_pending), false);

                return false;
            }
        }

        if (DataUploadPlugin.restrictToCharging(prefs))
        {
            if (PowerHelper.isPluggedIn(context) == false)
            {
                me.broadcastMessage(context.getString(R.string.message_charging_pending), false);

                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    protected int transmitPayload(SharedPreferences prefs, String payload)
    {
        Context context = this.getContext();

        if (prefs == null)
            prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (payload == null || payload.trim().length() == 0)
        {
            LogManager.getInstance(context).log("null_or_empty_payload", null);
            return DataUploadPlugin.RESULT_SUCCESS;
        }

        final DataUploadPlugin me = this;

        try
        {
            try
            {
                if (DataUploadPlugin.restrictToWifi(prefs))
                {
                    if (WiFiHelper.wifiAvailable(context) == false)
                    {
                        me.broadcastMessage(context.getString(R.string.message_wifi_pending), false);

                        return DataUploadPlugin.RESULT_NO_CONNECTION;
                    }
                }

                if (DataUploadPlugin.restrictToCharging(prefs))
                {
                    if (PowerHelper.isPluggedIn(context) == false)
                    {
                        me.broadcastMessage(context.getString(R.string.message_charging_pending), false);

                        return DataUploadPlugin.RESULT_NO_POWER;
                    }
                }

                JSONObject jsonMessage = new JSONObject();

                jsonMessage.put(OPERATION_KEY, "SubmitProbes");

                payload = payload.replaceAll("\r", "");
                payload = payload.replaceAll("\n", "");

                jsonMessage.put(PAYLOAD_KEY, payload);

                String userHash = EncryptionManager.getInstance().getUserHash(me.getContext());

                jsonMessage.put(USER_HASH_KEY, userHash);

                MessageDigest md = MessageDigest.getInstance("MD5");

                byte[] checksummed = (jsonMessage.get(USER_HASH_KEY).toString()
                        + jsonMessage.get(OPERATION_KEY).toString() + jsonMessage.get(PAYLOAD_KEY).toString())
                        .getBytes("UTF-8");

                byte[] digest = md.digest(checksummed);

                String checksum = (new BigInteger(1, digest)).toString(16);

                while (checksum.length() < 32)
                    checksum = "0" + checksum;

                jsonMessage.put(CHECKSUM_KEY, checksum);
                jsonMessage.put(CONTENT_LENGTH_KEY, checksummed.length);

                // Liberal HTTPS setup:
                // http://stackoverflow.com/questions/2012497/accepting-a-certificate-for-https-on-android

                HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

                SchemeRegistry registry = new SchemeRegistry();
                registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

                SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();

                if (prefs.getBoolean(DataUploadPlugin.ALLOW_ALL_SSL_CERTIFICATES,
                        DataUploadPlugin.ALLOW_ALL_SSL_CERTIFICATES_DEFAULT))
                {
                    KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    trustStore.load(null, null);

                    socketFactory = new LiberalSSLSocketFactory(trustStore);
                }

                registry.register(new Scheme("https", socketFactory, 443));

                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(3, TimeUnit.MINUTES);
                client.setReadTimeout(3, TimeUnit.MINUTES);

                String title = me.getContext().getString(R.string.notify_upload_data);

                PendingIntent contentIntent = PendingIntent.getActivity(me.getContext(), 0, new Intent(me.getContext(), StartActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

                Notification.Builder noteBuilder = new Notification.Builder(me.getContext());
                noteBuilder.setContentTitle(title);
                noteBuilder.setContentText(title);
                noteBuilder.setContentIntent(contentIntent);
                noteBuilder.setSmallIcon(R.drawable.ic_note_normal);
                noteBuilder.setWhen(System.currentTimeMillis());
                noteBuilder.setColor(0xff4e015c);

                Notification note = noteBuilder.build();

                note.flags = Notification.FLAG_ONGOING_EVENT;

                String uriString = prefs.getString(DataUploadPlugin.UPLOAD_URI, context.getString(R.string.sensor_upload_url));

                String jsonString = jsonMessage.toString();

                String uploadMessage = String.format(context.getString(R.string.message_transmit_bytes), (jsonString.length() / 1024));
                me.broadcastMessage(uploadMessage, false);

                MultipartBuilder builder = new MultipartBuilder();
                builder = builder.type(MultipartBuilder.FORM);

                ArrayList<File> toDelete = new ArrayList<>();

                if (payload.contains("\"" + Probe.PROBE_MEDIA_URL + "\":"))
                {
                    JSONArray payloadJson = new JSONArray(payload);

                    for (int i = 0; i < payloadJson.length(); i++)
                    {
                        JSONObject reading = payloadJson.getJSONObject(i);

                        if (reading.has(Probe.PROBE_MEDIA_URL))
                        {
                            Uri u = Uri.parse(reading.getString(Probe.PROBE_MEDIA_URL));

                            String mimeType = "application/octet-stream";

                            if (reading.has(Probe.PROBE_MEDIA_CONTENT_TYPE))
                                mimeType = reading.getString(Probe.PROBE_MEDIA_CONTENT_TYPE);

                            String guid = reading.getString(Probe.PROBE_GUID);
                            File file = new File(u.getPath());

                            if (file.exists()) {
                                builder = builder.addFormDataPart(guid, file.getName(), RequestBody.create(MediaType.parse(mimeType), file));
                                toDelete.add(file);
                            }
                        }
                    }
                }

                builder = builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"json\""), RequestBody.create(null, jsonString));

                String version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;

                RequestBody requestBody = builder.build();

                Request request = new Request.Builder()
                        .removeHeader("User-Agent")
                        .addHeader("User-Agent", "Purple Robot " + version)
                        .url(uriString)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();

                JSONObject json = new JSONObject(response.body().string());

                String status = json.getString(STATUS_KEY);

                String responsePayload = "";

                if (json.has(PAYLOAD_KEY))
                    responsePayload = json.getString(PAYLOAD_KEY);

                if (status.equals("error") == false)
                {
                    byte[] responseDigest = md.digest((status + responsePayload).getBytes("UTF-8"));
                    String responseChecksum = (new BigInteger(1, responseDigest)).toString(16);

                    while (responseChecksum.length() < 32)
                        responseChecksum = "0" + responseChecksum;

                    if (responseChecksum.equals(json.getString(CHECKSUM_KEY)))
                    {
                        String uploadedMessage = String.format(context.getString(R.string.message_upload_successful),
                                (jsonString.length() / 1024));

                        me.broadcastMessage(uploadedMessage, false);

                        for (File f : toDelete)
                            f.delete();

                        return DataUploadPlugin.RESULT_SUCCESS;
                    }
                    else
                    {
                        HashMap<String, Object> logPayload = new HashMap<>();
                        logPayload.put("remote_checksum", json.getString(CHECKSUM_KEY));
                        logPayload.put("local_checksum", responseChecksum);

                        LogManager.getInstance(context).log("null_or_empty_payload", logPayload);

                        me.broadcastMessage(context.getString(R.string.message_checksum_failed), true);
                    }

                    return DataUploadPlugin.RESULT_ERROR;
                }
                else
                {
                    String errorMessage = String.format(context.getString(R.string.message_server_error), status);
                    me.broadcastMessage(errorMessage, true);
                }
            }
            catch (HttpHostConnectException | ConnectTimeoutException e)
            {
                e.printStackTrace();

                me.broadcastMessage(context.getString(R.string.message_http_connection_error), true);
                LogManager.getInstance(context).logException(e);
            } catch (SocketTimeoutException e)
            {
                e.printStackTrace();

                me.broadcastMessage(context.getString(R.string.message_socket_timeout_error), true);
                LogManager.getInstance(me.getContext()).logException(e);
            }
            catch (SocketException e)
            {
                e.printStackTrace();

                String errorMessage = String.format(context.getString(R.string.message_socket_error), e.getMessage());
                me.broadcastMessage(errorMessage, true);
                LogManager.getInstance(me.getContext()).logException(e);
            }
            catch (UnknownHostException e)
            {
                e.printStackTrace();

                me.broadcastMessage(context.getString(R.string.message_unreachable_error), true);
                LogManager.getInstance(me.getContext()).logException(e);
            }
            catch (JSONException e)
            {
                e.printStackTrace();

                me.broadcastMessage(context.getString(R.string.message_response_error), true);
                LogManager.getInstance(me.getContext()).logException(e);
            }
            catch (SSLPeerUnverifiedException e)
            {
                LogManager.getInstance(me.getContext()).logException(e);
                me.broadcastMessage(context.getString(R.string.message_unverified_server), true);

                LogManager.getInstance(context).logException(e);
            }
            catch (Exception e)
            {
                e.printStackTrace();

                LogManager.getInstance(me.getContext()).logException(e);
                me.broadcastMessage(context.getString(R.string.message_general_error, e.getMessage()), true);

                LogManager.getInstance(context).logException(e);
            }
        }
        catch (OutOfMemoryError e)
        {
            LogManager.getInstance(me.getContext()).logException(e);
            me.broadcastMessage(context.getString(R.string.message_general_error, e.getMessage()), true);

            LogManager.getInstance(context).logException(e);
        }

        return DataUploadPlugin.RESULT_ERROR;
    }

    public abstract boolean isEnabled(Context context);

    public static boolean uploadEnabled(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean(HttpUploadPlugin.ENABLED, HttpUploadPlugin.ENABLED_DEFAULT))
            return true;
        else if (prefs.getBoolean(StreamingJacksonUploadPlugin.ENABLED, StreamingJacksonUploadPlugin.ENABLED_DEFAULT))
            return true;

        return false;
    }

    public static long lastUploadTime(SharedPreferences prefs)
    {
        long lastUpload = -1;

        if (prefs.contains(HttpUploadPlugin.LAST_UPLOAD_TIME))
            lastUpload = prefs.getLong(HttpUploadPlugin.LAST_UPLOAD_TIME, -1);

        if (prefs.contains(StreamingJacksonUploadPlugin.LAST_UPLOAD_TIME))
        {
            long streamingUpload = prefs.getLong(StreamingJacksonUploadPlugin.LAST_UPLOAD_TIME, -1);

            if (streamingUpload > lastUpload)
                lastUpload = streamingUpload;
        }

        return lastUpload;
    }

    public static long lastUploadSize(SharedPreferences prefs)
    {
        long lastSize = -1;

        if (prefs.contains(HttpUploadPlugin.LAST_UPLOAD_SIZE))
            lastSize = prefs.getLong(HttpUploadPlugin.LAST_UPLOAD_SIZE, -1);

        if (prefs.contains(StreamingJacksonUploadPlugin.LAST_UPLOAD_SIZE))
        {
            long lastUpload = -1;

            if (prefs.contains(HttpUploadPlugin.LAST_UPLOAD_TIME))
                lastUpload = prefs.getLong(HttpUploadPlugin.LAST_UPLOAD_TIME, -1);

            if (prefs.contains(StreamingJacksonUploadPlugin.LAST_UPLOAD_TIME))
            {
                long streamingUpload = prefs.getLong(StreamingJacksonUploadPlugin.LAST_UPLOAD_TIME, -1);

                if (streamingUpload > lastUpload)
                    lastSize = prefs.getLong(StreamingJacksonUploadPlugin.LAST_UPLOAD_SIZE, -1);
            }
        }

        return lastSize;
    }

    public static int pendingFileCount(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean(HttpUploadPlugin.ENABLED, HttpUploadPlugin.ENABLED_DEFAULT))
        {
            OutputPlugin plugin = OutputPluginManager.sharedInstance.pluginForClass(context, HttpUploadPlugin.class);

            if (plugin instanceof HttpUploadPlugin)
            {
                HttpUploadPlugin http = (HttpUploadPlugin) plugin;

                return http.pendingFilesCount();
            }
        }
        else
        {
            OutputPlugin plugin = OutputPluginManager.sharedInstance.pluginForClass(context, StreamingJacksonUploadPlugin.class);

            if (plugin instanceof StreamingJacksonUploadPlugin)
            {
                StreamingJacksonUploadPlugin http = (StreamingJacksonUploadPlugin) plugin;

                return http.pendingFilesCount();
            }
        }

        return 0;
    }

    public static boolean multipleUploadersEnabled(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean(HttpUploadPlugin.ENABLED, HttpUploadPlugin.ENABLED_DEFAULT) &&
            prefs.getBoolean(StreamingJacksonUploadPlugin.ENABLED, StreamingJacksonUploadPlugin.ENABLED_DEFAULT))
        {
            return true;
        }

        return false;
    }
}
