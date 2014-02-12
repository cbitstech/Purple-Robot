package edu.northwestern.cbits.purple_robot_manager.plugins;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.WiFiHelper;
import edu.northwestern.cbits.purple_robot_manager.activities.StartActivity;
import edu.northwestern.cbits.purple_robot_manager.logging.LiberalSSLSocketFactory;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

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

	private static final String RESTRICT_TO_WIFI = "config_restrict_data_wifi";
	private static final boolean RESTRICT_TO_WIFI_DEFAULT = true;
	private static final String ALLOW_ALL_SSL_CERTIFICATES = "config_http_liberal_ssl";
	private static final boolean ALLOW_ALL_SSL_CERTIFICATES_DEFAULT = true;
	private static final String UPLOAD_URI = "config_data_server_uri";

	protected static final int RESULT_SUCCESS = 0;
	protected static final int RESULT_NO_CONNECTION = 1;
	protected static final int RESULT_ERROR = 2;
	
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

	private boolean restrictToWifi(SharedPreferences prefs) 
	{
		try
		{
			return prefs.getBoolean(DataUploadPlugin.RESTRICT_TO_WIFI, DataUploadPlugin.RESTRICT_TO_WIFI_DEFAULT);
		}
		catch (ClassCastException e)
		{
			String enabled = prefs.getString(DataUploadPlugin.RESTRICT_TO_WIFI, "" + DataUploadPlugin.RESTRICT_TO_WIFI_DEFAULT).toLowerCase(Locale.ENGLISH);
			
			boolean isRestricted = ("false".equals(enabled) == false); 
			
			Editor edit = prefs.edit();
			edit.putBoolean(DataUploadPlugin.RESTRICT_TO_WIFI, isRestricted);
			edit.commit();
			
			return isRestricted;
		}
	}

	@SuppressWarnings("deprecation")
	protected int transmitPayload(SharedPreferences prefs, String payload) 
	{
		Context context = this.getContext();

		if (prefs == null)
			prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		if (payload == null || payload.trim().length() == 0)
			return DataUploadPlugin.RESULT_SUCCESS;

		AndroidHttpClient androidClient = AndroidHttpClient.newInstance("Purple Robot", context);
		
		final DataUploadPlugin me = this;

		try
		{
			if (this.restrictToWifi(prefs))
			{
				if (WiFiHelper.wifiAvailable(context) == false)
				{
					me.broadcastMessage(context.getString(R.string.message_wifi_pending));

					return DataUploadPlugin.RESULT_NO_CONNECTION;
				}
			}

			JSONObject jsonMessage = new JSONObject();

			jsonMessage.put(OPERATION_KEY, "SubmitProbes");
				
				
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
				payload = Normalizer.normalize(payload, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
						
			payload = payload.replaceAll("\r", "");
			payload = payload.replaceAll("\n", "");
				
			jsonMessage.put(PAYLOAD_KEY, payload);

			String userHash = EncryptionManager.getInstance().getUserHash(me.getContext());

			jsonMessage.put(USER_HASH_KEY, userHash);

			MessageDigest md = MessageDigest.getInstance("MD5");
				
			byte[] checksummed = (jsonMessage.get(USER_HASH_KEY).toString() + jsonMessage.get(OPERATION_KEY).toString() + jsonMessage.get(PAYLOAD_KEY).toString()).getBytes("US-ASCII");

			byte[] digest = md.digest(checksummed);

			String checksum = (new BigInteger(1, digest)).toString(16);

			while (checksum.length() < 32)
				checksum = "0" + checksum;

			jsonMessage.put(CHECKSUM_KEY, checksum);
			jsonMessage.put(CONTENT_LENGTH_KEY, checksummed.length);

			// Liberal HTTPS setup: http://stackoverflow.com/questions/2012497/accepting-a-certificate-for-https-on-android

	        HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
				
			SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
				
			if (prefs.getBoolean(DataUploadPlugin.ALLOW_ALL_SSL_CERTIFICATES, DataUploadPlugin.ALLOW_ALL_SSL_CERTIFICATES_DEFAULT))
			{
		        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		        trustStore.load(null, null);

		        socketFactory = new LiberalSSLSocketFactory(trustStore);								
			}

			registry.register(new Scheme("https", socketFactory, 443));
			
			HttpParams params = androidClient.getParams();
			HttpConnectionParams.setConnectionTimeout(params, 180000);
			HttpConnectionParams.setSoTimeout(params, 180000);
			
			SingleClientConnManager mgr = new SingleClientConnManager(params, registry);
			HttpClient httpClient = new DefaultHttpClient(mgr, params);

			HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
				
			String title = me.getContext().getString(R.string.notify_upload_data);

			Notification note = new Notification(R.drawable.ic_note_normal, title, System.currentTimeMillis());
			PendingIntent contentIntent = PendingIntent.getActivity(me.getContext(), 0,
					new Intent(me.getContext(), StartActivity.class), Notification.FLAG_ONGOING_EVENT);

			note.setLatestEventInfo(me.getContext(), title, title, contentIntent);

			note.flags = Notification.FLAG_ONGOING_EVENT;

			String body = null;
				
			String uriString = prefs.getString(DataUploadPlugin.UPLOAD_URI, context.getString(R.string.sensor_upload_url));

			URI siteUri = new URI(uriString);
			
			HttpPost httpPost = new HttpPost(siteUri);

			String jsonString = jsonMessage.toString();
			
//			Log.e("PR", "SENDING : " + jsonString);

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("json", jsonString));
			HttpEntity entity = new UrlEncodedFormEntity(nameValuePairs, HTTP.US_ASCII);

			httpPost.setEntity(entity);

			String uploadMessage = String.format(context.getString(R.string.message_transmit_bytes),
					(httpPost.getEntity().getContentLength() / 1024));
			me.broadcastMessage(uploadMessage);

			HttpResponse response = httpClient.execute(httpPost);

			HttpEntity httpEntity = response.getEntity();

			String contentHeader = null;

			if (response.containsHeader("Content-Encoding"))
				contentHeader = response.getFirstHeader("Content-Encoding").getValue();

			if (contentHeader != null && contentHeader.endsWith("gzip"))
			{
				BufferedInputStream in = new BufferedInputStream(AndroidHttpClient.getUngzippedContent(httpEntity));

				ByteArrayOutputStream out = new ByteArrayOutputStream();

				int read = 0;
				byte[] buffer = new byte[1024];

				while ((read = in.read(buffer, 0, buffer.length)) != -1)
					out.write(buffer, 0, read);

				in.close();

				body = out.toString("UTF-8");
			}
			else
				body = EntityUtils.toString(httpEntity);
			
			JSONObject json = new JSONObject(body);

			int index = body.length() - 512;
			
			if (index < 0)
				index = 0;
			
			Log.e("PR", "RECV: " + body.substring(index));

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
							(httpPost.getEntity().getContentLength() / 1024));

					me.broadcastMessage(uploadedMessage);
				}
				else
					me.broadcastMessage(context.getString(R.string.message_checksum_failed));
				
				return DataUploadPlugin.RESULT_SUCCESS;
			}
			else
			{
				String errorMessage = String.format(context.getString(R.string.message_server_error), status);
				me.broadcastMessage(errorMessage);
			}
		}
		catch (HttpHostConnectException e)
		{
			me.broadcastMessage(context.getString(R.string.message_http_connection_error));
			LogManager.getInstance(context).logException(e);
		}
		catch (SocketTimeoutException e)
		{
			me.broadcastMessage(context.getString(R.string.message_socket_timeout_error));
			LogManager.getInstance(me.getContext()).logException(e);
		}
		catch (SocketException e)
		{
			String errorMessage = String.format(context.getString(R.string.message_socket_error), e.getMessage());
			me.broadcastMessage(errorMessage);
			LogManager.getInstance(me.getContext()).logException(e);
		}
		catch (UnknownHostException e)
		{
			me.broadcastMessage(context.getString(R.string.message_unreachable_error));
			LogManager.getInstance(me.getContext()).logException(e);
		}
		catch (JSONException e)
		{
			me.broadcastMessage(context.getString(R.string.message_response_error));
			LogManager.getInstance(me.getContext()).logException(e);
		}
		catch (SSLPeerUnverifiedException e)
		{
			LogManager.getInstance(me.getContext()).logException(e);
			me.broadcastMessage(context.getString(R.string.message_unverified_server));
		}
		catch (Exception e)
		{
			LogManager.getInstance(me.getContext()).logException(e);
			me.broadcastMessage(context.getString(R.string.message_general_error, e.getMessage()));
		}
		finally
		{
			androidClient.close();
		}
		
		return DataUploadPlugin.RESULT_ERROR;
	}
}
