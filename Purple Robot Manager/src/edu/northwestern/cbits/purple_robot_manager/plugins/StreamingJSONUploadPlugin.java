package edu.northwestern.cbits.purple_robot_manager.plugins;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.commons.io.FileUtils;
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
import android.app.ActivityManager.RunningTaskInfo;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.location.Location;
import android.net.http.AndroidHttpClient;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.JsonWriter;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.WiFiHelper;
import edu.northwestern.cbits.purple_robot_manager.activities.StartActivity;
import edu.northwestern.cbits.purple_robot_manager.logging.LiberalSSLSocketFactory;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

@SuppressLint("NewApi")
public class StreamingJSONUploadPlugin extends OutputPlugin
{
	// TODO: Move to superclass...
	private final static String USER_HASH_KEY = "UserHash";
	private final static String OPERATION_KEY = "Operation";
	private final static String PAYLOAD_KEY = "Payload";
	private final static String CHECKSUM_KEY = "Checksum";
	private final static String CONTENT_LENGTH_KEY = "ContentLength";
	private final static String STATUS_KEY = "Status";

	private final static String FILE_EXTENSION = ".streaming";
	
	private final static String CACHE_DIR = "streaming_pending_uploads";
	protected static final String UPLOAD_URI = "streaming_upload_uri";
	private static final String ENABLED = null;
	private int _arrayCount = 0;

	private JsonWriter _writer = null;
	
	public String[] respondsTo() 
	{
		String[] activeActions = { Probe.PROBE_READING, OutputPlugin.FORCE_UPLOAD };
		return activeActions;
	}
	
	// TODO: Pull into superclass for this and HttpUploadPlugin
	public File getPendingFolder()
	{
		SharedPreferences prefs = HttpUploadPlugin.getPreferences(this.getContext());

		File internalStorage = this.getContext().getFilesDir();

		if (prefs.getBoolean(OutputPlugin.USE_EXTERNAL_STORAGE, false))
			internalStorage = this.getContext().getExternalFilesDir(null);

		if (internalStorage != null && !internalStorage.exists())
			internalStorage.mkdirs();

		File pendingFolder = new File(internalStorage, StreamingJSONUploadPlugin.CACHE_DIR);

		if (pendingFolder != null && !pendingFolder.exists())
			pendingFolder.mkdirs();

		return pendingFolder;
	}

	public void processIntent(Intent intent) 
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
		
		if (prefs.getBoolean(StreamingJSONUploadPlugin.ENABLED, false) == false)
			return;

		String action = intent.getAction();
		
		if (OutputPlugin.FORCE_UPLOAD.equals(action))
		{
			final StreamingJSONUploadPlugin me = this;

			Runnable r = new Runnable()
			{
				@SuppressWarnings("deprecation")
				public void run() 
				{
					Context context = me.getContext();

					AndroidHttpClient androidClient = AndroidHttpClient.newInstance("Purple Robot", context);

					try
					{
						while (true)
						{
							if (me.restrictToWifi(prefs))
							{
								if (WiFiHelper.wifiAvailable(context) == false)
								{
	//								this._throughput = 0.0;
	
									me.broadcastMessage(context.getString(R.string.message_wifi_pending));
	
	//								this._lastUpload = now;
	//								this._uploading = false;
	
									return;
								}
							}
	
							JSONObject jsonMessage = new JSONObject();
	
							jsonMessage.put(OPERATION_KEY, "SubmitProbes");
							
							File pendingFolder = me.getPendingFolder();
	
							String[] filenames = pendingFolder.list(new FilenameFilter()
							{
								public boolean accept(File dir, String filename)
								{
									return filename.endsWith(StreamingJSONUploadPlugin.FILE_EXTENSION);
								}
							});
	
							if (filenames == null)
								filenames = new String[0];
							
							if (filenames.length == 0)
								return;
							
							SecureRandom random = new SecureRandom();
							
							int index = random.nextInt(filenames.length);
							
							String payload = FileUtils.readFileToString(new File(pendingFolder, filenames[index]));
							
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
							
							if (prefs.getBoolean("config_http_liberal_ssl", true))
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
							
							String uriString = prefs.getString(StreamingJSONUploadPlugin.UPLOAD_URI, me.getContext().getString(R.string.sensor_upload_url));

							URI siteUri = new URI(uriString);
							
							HttpPost httpPost = new HttpPost(siteUri);

							String jsonString = jsonMessage.toString();

							List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
							nameValuePairs.add(new BasicNameValuePair("json", jsonString));
							HttpEntity entity = new UrlEncodedFormEntity(nameValuePairs, HTTP.US_ASCII);

							httpPost.setEntity(entity);

							String uploadMessage = String.format(context.getString(R.string.message_transmit_bytes),
									(httpPost.getEntity().getContentLength() / 1024));
							me.broadcastMessage(uploadMessage);

//							noteManager.notify(12345, note);
							
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
								{
									out.write(buffer, 0, read);
								}

								in.close();

								body = out.toString("UTF-8");
							}
							else
								body = EntityUtils.toString(httpEntity);

							JSONObject json = new JSONObject(body);

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
									
									// TODO: Delete file...

									me.broadcastMessage(uploadedMessage);
								}
								else
									me.broadcastMessage(context.getString(R.string.message_checksum_failed));
							}
							else
							{
								String errorMessage = String.format(context.getString(R.string.message_server_error), status);
								me.broadcastMessage(errorMessage);
								
								return;
							}
						}
					}
					catch (HttpHostConnectException e)
					{
						me.broadcastMessage(context.getString(R.string.message_http_connection_error));
						LogManager.getInstance(context).logException(e);

						return;
					}
					catch (SocketTimeoutException e)
					{
						me.broadcastMessage(context.getString(R.string.message_socket_timeout_error));
						LogManager.getInstance(me.getContext()).logException(e);

						return;
					}
					catch (SocketException e)
					{
						String errorMessage = String.format(context.getString(R.string.message_socket_error), e.getMessage());
						me.broadcastMessage(errorMessage);
						LogManager.getInstance(me.getContext()).logException(e);

						return;
					}
					catch (UnknownHostException e)
					{
						me.broadcastMessage(context.getString(R.string.message_unreachable_error));
						LogManager.getInstance(me.getContext()).logException(e);
					
						return;
					}
					catch (JSONException e)
					{
						me.broadcastMessage(context.getString(R.string.message_response_error));
						LogManager.getInstance(me.getContext()).logException(e);

						return;
					}
					catch (SSLPeerUnverifiedException e)
					{
						LogManager.getInstance(me.getContext()).logException(e);
						me.broadcastMessage(context.getString(R.string.message_unverified_server));

						return;
					}
					catch (Exception e)
					{
						LogManager.getInstance(me.getContext()).logException(e);
						me.broadcastMessage(context.getString(R.string.message_general_error));

						return;
					}
					finally
					{
						androidClient.close();
					}
				}
			};
			
			Thread t = new Thread(r);
			t.start();
		}
		else if (Probe.PROBE_READING.equals(action))
		{
			Bundle extras = intent.getExtras();
			
			// TODO: Get rid of verbatim string...
			if (extras.containsKey("TRANSMIT") && extras.getBoolean("TRANSMIT") == false)
				return;

			long now = System.currentTimeMillis();

			try 
			{
				if (this._writer != null && this._arrayCount > 16)
				{
					this._writer.endArray();
					this._writer.flush();
					this._writer.close();
					
					this._writer = null;
					this._arrayCount = 0;
				}
				
				Intent i = new Intent(OutputPlugin.FORCE_UPLOAD);
				this.process(i);

				if (this._writer == null)
				{
					File f = new File(this.getPendingFolder(), now + ".streaming");
					
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
					this._writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
					
					this._writer.beginArray();
					
					this._arrayCount = 0;
				}
				
				StreamingJSONUploadPlugin.writeBundle(this.getContext(), this._writer, extras);
				this._arrayCount += 1;
			} 
			catch (IOException e) 
			{
				LogManager.getInstance(this.getContext()).logException(e);
			}
		}
	}

	// TODO: Cleanup & move to superclass...
	
	protected boolean restrictToWifi(SharedPreferences prefs) 
	{
		try
		{
			return prefs.getBoolean("config_restrict_data_wifi", true);
		}
		catch (ClassCastException e)
		{
			String enabled = prefs.getString("config_restrict_data_wifi", "true").toLowerCase(Locale.ENGLISH);
			
			boolean isRestricted = ("false".equals(enabled) == false); 
			
			Editor edit = prefs.edit();
			edit.putBoolean("config_restrict_data_wifi", isRestricted);
			edit.commit();
			
			return isRestricted;
		}
	}

	@SuppressWarnings("unchecked")
	private static void writeBundle(Context context, JsonWriter writer, Bundle bundle) 
	{
		try 
		{
			writer.beginObject();

			Map<String, Object> values = OutputPlugin.getValues(bundle);

			for (String key : values.keySet())
			{
				Object value = values.get(key);
				
				if (value == null || key == null)
				{
					// Skip
				}
				else
				{
					writer.name(key);
					
					if (value instanceof String)
					{
						writer.value((String) value);
					}
					else if (value instanceof float[])
					{
						float[] floats = (float[]) value;
						
						writer.beginArray();
						
						for (float f : floats)
							writer.value(f);
						
						writer.endArray();
					}
					else if (value instanceof int[])
					{
						int[] ints = (int[]) value;

						writer.beginArray();
						
						for (int i : ints)
							writer.value(i);
						
						writer.endArray();
					}
					else if (value instanceof long[])
					{
						long[] longs = (long[]) value;

						writer.beginArray();
						
						for (long l : longs)
							writer.value(l);
						
						writer.endArray();
					}
					else if (value instanceof double[])
					{
						double[] doubles = (double[]) value;
	
						writer.beginArray();
						
						for (double d : doubles)
							writer.value(d);
						
						writer.endArray();
					}
					else if (value instanceof Float)
					{
						Float f = (Float) value;
	
						writer.value(f);
					}
					else if (value instanceof Integer)
					{
						Integer i = (Integer) value;
	
						writer.value(i);
					}
					else if (value instanceof Long)
					{
						Long l = (Long) value;
	
						writer.value(l);
					}
					else if (value instanceof Boolean)
					{
						Boolean b = (Boolean) value;
	
						writer.value(b);
					}
					else if (value instanceof Short)
					{
						Short s = (Short) value;
	
						writer.value(s);
					}
					else if (value instanceof Double)
					{
						Double d = (Double) value;
						
						if (d.isInfinite())
							writer.value(Double.MAX_VALUE);
						else
							writer.value(d);
					}
					else if (value instanceof List)
					{
						List<Object> list = (List<Object>) value;
	
						writer.beginArray();
	
						for (Object o : list)
						{
							if (o instanceof String)
								writer.value(o.toString());
							else if (o instanceof Bundle)
								StreamingJSONUploadPlugin.writeBundle(context, writer, (Bundle) o);								
							else if (o instanceof ScanResult)
							{
								ScanResult s = (ScanResult) o;
								
								writer.beginObject();
	
								if (s.BSSID != null)
									writer.name("BSSID").value(s.BSSID);
	
								if (s.SSID != null)
									writer.name("SSID").value(s.SSID);
	
								if (s.capabilities != null)
									writer.name("Capabilities").value(s.capabilities);

								writer.name("Frequency").value(s.frequency);
								writer.name("Level dBm").value(s.level);
								
								writer.endObject();
							}
							else if (o instanceof RunningTaskInfo)
							{
								RunningTaskInfo r = (RunningTaskInfo) o;
	
								writer.beginObject();
	
								if (r.baseActivity != null)
									writer.name("Base Activity").value(r.baseActivity.getPackageName());
	
								if (r.description != null)
									writer.name("Description").value(r.description.toString());
	
								writer.name("Activity Count").value(r.numActivities);
								writer.name("Running Activity Count").value(r.numRunning);
	
								writer.endObject();
							}
							else if (o instanceof ApplicationInfo)
							{
								ApplicationInfo a = (ApplicationInfo) o;
								
								writer.value(a.packageName);
							}
							else if (o instanceof Location)
							{
								Location l = (Location) o;
	
								writer.beginObject();
								
								writer.name("Accuracy").value(l.getAccuracy());
								writer.name("Altitude").value(l.getAltitude());
								writer.name("Bearing").value(l.getBearing());
								writer.name("Latitude").value(l.getLatitude());
								writer.name("Longitude").value(l.getLongitude());
	
								if (l.getProvider() != null)
									writer.name("Provider").value(l.getProvider());
								else
									writer.name("Provider").value("Unknown");
	
								writer.name("Speed").value(l.getSpeed());
								writer.name("Timestamp").value(l.getTime());
								
								writer.endObject();
							}
							else
								Log.e("PRM", "LIST OBJ: " + o.getClass().getCanonicalName() + " IN " + key);
						}
	
						writer.endArray();
					}
					else if (value instanceof Location)
					{
						Location l = (Location) value;
	
						writer.beginObject();
						
						writer.name("Accuracy").value(l.getAccuracy());
						writer.name("Altitude").value(l.getAltitude());
						writer.name("Bearing").value(l.getBearing());
						writer.name("Latitude").value(l.getLatitude());
						writer.name("Longitude").value(l.getLongitude());

						if (l.getProvider() != null)
							writer.name("Provider").value(l.getProvider());
						else
							writer.name("Provider").value("Unknown");

						writer.name("Speed").value(l.getSpeed());
						writer.name("Timestamp").value(l.getTime());
						
						writer.endObject();
					}
					else if (value instanceof BluetoothClass)
					{
						BluetoothClass btClass = (BluetoothClass) value;
	
						writer.value(btClass.toString());
					}
					else if (value instanceof BluetoothDevice)
					{
						BluetoothDevice device = (BluetoothDevice) value;
	
						writer.beginObject();
	
						if (device.getBondState() == BluetoothDevice.BOND_BONDED)
							writer.name("Bond State").value("Bonded");
						else if (device.getBondState() == BluetoothDevice.BOND_BONDING)
							writer.name("Bond State").value("Bonding");
						else
							writer.name("Bond State").value("None");
	
						writer.name("Device Address").value(device.getAddress());
						writer.name("Device Class").value(device.getBluetoothClass().toString());

						writer.endObject();
					}
					else if (value instanceof Bundle)
						StreamingJSONUploadPlugin.writeBundle(context, writer, (Bundle) value);								
					else
						Log.e("PRM", "GOT TYPE " + value.getClass().getCanonicalName() + " FOR " + key);
				}
			}
			
			writer.endObject();
		} 
		catch (IOException e) 
		{
			LogManager.getInstance(context).logException(e);
		}
	}
}
