package edu.northwestern.cbits.purple_robot_manager.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationManager;
import android.net.http.AndroidHttpClient;
import android.preference.PreferenceManager;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.WiFiHelper;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;

public class LogManager 
{
	private static final String EVENT_TYPE = "event_type";
	private static final String TIMESTAMP = "timestamp";
	private static final String NAMESPACE = "event_log_params";
	private static final String LATITUDE = "latitude";
	private static final String LONGITUDE = "longitude";
	private static final String ALTITUDE = "altitude";
	private static final String TIME_DRIFT = "time_drift";
	
	private static final String LOG_QUEUE = "pending_log_queue";
	private static final String CONTENT_OBJECT = "content_object";
	private static final String USER_ID = "user_id";
	private static final String STACKTRACE = "stacktrace";

	private static LogManager _sharedInstance = null;
	
	private boolean _uploading = false;
	
	private Context _context = null;
	
	public LogManager(Context context) 
	{
		this._context = context;
		
		AlarmManager alarms = (AlarmManager) this._context.getSystemService(Context.ALARM_SERVICE);
		
		Intent intent = new Intent(ManagerService.UPLOAD_LOGS_INTENT);
		PendingIntent pending = PendingIntent.getService(this._context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarms.setInexactRepeating(AlarmManager.RTC, 0, 300000, pending);
	}

	public static LogManager getInstance(Context context)
	{
		if (LogManager._sharedInstance != null)
			return LogManager._sharedInstance;
		
		if (context != null)
			LogManager._sharedInstance = new LogManager(context.getApplicationContext());
		
		LogManager._sharedInstance.log("pr_log_manager_initialized", null);
		
		return LogManager._sharedInstance;
	}
	
	public boolean log(String event, Map<String, Object> payload)
	{
		long now = System.currentTimeMillis();

		if (payload == null)
			payload = new HashMap<String, Object>();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		boolean enabled = false;
		
		try
		{
			enabled = prefs.getBoolean("config_enable_log_server", false);
		}
		catch (ClassCastException e)
		{
			enabled = prefs.getString("config_enable_log_server", "false").equalsIgnoreCase("true");
		}
			
		if (enabled)
		{
			String endpointUri = prefs.getString("config_log_server_uri", null);
			
			if (endpointUri != null)
			{
				JavaScriptEngine engine = new JavaScriptEngine(this._context);
		
				Map<String, Object> namespace = engine.fetchNamespaceMap(LogManager.NAMESPACE);
				
				if (namespace != null)
				{
					for (String key : namespace.keySet())
					{
						if (payload.containsKey(key) == false)
							payload.put(key, namespace.get(key));
					}
				}
				
				boolean logLocation = false;
						
				try
				{
					logLocation = prefs.getBoolean("config_log_location", false);
				}
				catch (ClassCastException e)
				{
					logLocation = prefs.getString("config_log_location", "false").equalsIgnoreCase("true");
				}

				if (logLocation)
				{
					LocationManager lm = (LocationManager) this._context.getSystemService(Context.LOCATION_SERVICE);
				
					Location lastLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					
					Location backupLocation = null;
				
					if (lastLocation != null && now - lastLocation.getTime() > (1000 * 60 * 60))
					{
						backupLocation = lastLocation;
					
						lastLocation = null;
					}
					
					if (lastLocation == null)
						lastLocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					
					if (lastLocation == null)
						lastLocation = backupLocation;
					
					if (lastLocation != null)
					{
						payload.put(LogManager.LATITUDE, lastLocation.getLatitude());
						payload.put(LogManager.LONGITUDE, lastLocation.getLongitude());
						payload.put(LogManager.ALTITUDE, lastLocation.getAltitude());
						payload.put(LogManager.TIME_DRIFT, now - lastLocation.getTime());
					}
				}

				payload.put(LogManager.EVENT_TYPE, event);
				payload.put(LogManager.TIMESTAMP, now / 1000);
				
				if (payload.containsKey(LogManager.USER_ID) == false)
					payload.put(LogManager.USER_ID, EncryptionManager.getInstance().getUserHash(this._context));

				try 
				{
					JSONArray pendingEvents = new JSONArray(prefs.getString(LogManager.LOG_QUEUE, "[]"));
					JSONObject jsonEvent = new JSONObject();
					
					for (String key : payload.keySet())
					{
						jsonEvent.put(key, payload.get(key));
					}

					jsonEvent.put(LogManager.CONTENT_OBJECT, new JSONObject(jsonEvent.toString()));

					pendingEvents.put(jsonEvent);
					
					if (pendingEvents.length() > 128)
					{
						JSONArray newEvents = new JSONArray();
						
						for (int i = pendingEvents.length() - 128; i < pendingEvents.length(); i++)
						{
							newEvents.put(pendingEvents.get(i));
						}

						pendingEvents = newEvents;
					}
					
					Editor e = prefs.edit();
					e.putString(LogManager.LOG_QUEUE, pendingEvents.toString());
					e.commit();

					pendingEvents = new JSONArray(prefs.getString(LogManager.LOG_QUEUE, "[]"));

					return true;
				}
				catch (JSONException e) 
				{
					e.printStackTrace();
				}
			}
		}

		return false;
	}
	
	public boolean queueContains(String event, Map<String, Object> payload)
	{
		return false;
	}

	public void attemptUploads() 
	{
		if (this._uploading)
			return;
		
		this._uploading = true;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

		boolean restrictWifi = true;
		
		try
		{
			restrictWifi = prefs.getBoolean("config_restrict_log_wifi", true);
		}
		catch (ClassCastException e)
		{
			restrictWifi = prefs.getString("config_restrict_log_wifi", "true").equalsIgnoreCase("true");
		}
		
		if (restrictWifi && WiFiHelper.wifiAvailable(this._context) == false)
			return;
		
		String endpointUri = prefs.getString("config_log_server_uri", null);
		
		if (endpointUri != null)
		{
			try 
			{
				JSONArray pendingEvents = new JSONArray(prefs.getString(LogManager.LOG_QUEUE, "[]"));
				
				try 
				{
					URI siteUri = new URI(endpointUri);

					SchemeRegistry registry = new SchemeRegistry();
					registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
					
					SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();

					boolean liberalSsl = true;
					
					try
					{
						liberalSsl = prefs.getBoolean("config_http_liberal_ssl", true);
					}
					catch (ClassCastException e)
					{
						liberalSsl = prefs.getString("config_http_liberal_ssl", "true").equalsIgnoreCase("true");
					}
					
					if (liberalSsl)
					{
				        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				        trustStore.load(null, null);

				        socketFactory = new LiberalSSLSocketFactory(trustStore);								
					}

					registry.register(new Scheme("https", socketFactory, 443));

					for (int i = 0; i < pendingEvents.length(); i++)
					{
						AndroidHttpClient androidClient = AndroidHttpClient.newInstance("Purple Robot", this._context);

						ThreadSafeClientConnManager mgr = new ThreadSafeClientConnManager(androidClient.getParams(), registry);
						HttpClient httpClient = new DefaultHttpClient(mgr, androidClient.getParams());

						androidClient.close();

						JSONObject event = pendingEvents.getJSONObject(i);
						
						HttpPost httpPost = new HttpPost(siteUri);
						
						List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
						nameValuePairs.add(new BasicNameValuePair("logJSON", event.toString()));
						nameValuePairs.add(new BasicNameValuePair("json", event.toString()));
						HttpEntity entity = new UrlEncodedFormEntity(nameValuePairs, HTTP.US_ASCII);

						httpPost.setEntity(entity);
						
						httpClient.execute(httpPost);
						HttpResponse response = httpClient.execute(httpPost);

						HttpEntity httpEntity = response.getEntity();
						
						Log.e("PR-LOGGING", "Log upload result: " + EntityUtils.toString(httpEntity));
						
						httpEntity.consumeContent();

						mgr.shutdown();
					}
				}
				catch (URISyntaxException e) 
				{
					e.printStackTrace();
				} 
				catch (KeyStoreException e) 
				{
					e.printStackTrace();
				} 
				catch (NoSuchAlgorithmException e) 
				{
					e.printStackTrace();
				}
				catch (CertificateException e) 
				{
					e.printStackTrace();
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				} 
				catch (KeyManagementException e) 
				{
					e.printStackTrace();
				}
				catch (UnrecoverableKeyException e) 
				{
					e.printStackTrace();
				}
				
				Editor e = prefs.edit();
				e.putString(LogManager.LOG_QUEUE, "[]");
				e.commit();
			}
			catch (JSONException e) 
			{
				e.printStackTrace();
			}
		}
		
		this._uploading = false;
	}

	public void logException(Throwable e) 
	{
		e.printStackTrace();

		Map<String, Object> payload = new HashMap<String, Object>();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		
		e.printStackTrace(out);
		
		out.close();
		
		String stacktrace = baos.toString();
		
		payload.put(LogManager.STACKTRACE, stacktrace);
		
		this.log("java_exception", payload);
	}
}
