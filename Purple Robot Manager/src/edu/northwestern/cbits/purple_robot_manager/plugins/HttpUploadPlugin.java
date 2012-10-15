package edu.northwestern.cbits.purple_robot_manager.plugins;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.StartActivity;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class HttpUploadPlugin extends OutputPlugin
{
	private final static String CACHE_DIR = "http_pending_uploads";

	private final static String USER_HASH_KEY = "UserHash";
	private final static String OPERATION_KEY = "Operation";
	private final static String PAYLOAD_KEY = "Payload";
	private final static String CHECKSUM_KEY = "Checksum";
	private final static String STATUS_KEY = "Status";

	private List<String> _pendingSaves = new ArrayList<String>();
	private long _lastSave = 0;
	private long _lastUpload = 0;

	private Thread _uploadThread = null;

	private long savePeriod()
	{
		return 30000;
	}

	private long maxUploadSize()
	{
		return 131072; // 128KB
	}

	private long uploadPeriod()
	{
		// TODO: Dynamic uploading period here...
		return 60000;
	}

	public String[] respondsTo()
	{
		String[] activeActions = { Probe.PROBE_READING };

		return activeActions;
	}

	@Override
	public void processIntent(Intent intent)
	{
		try
		{
			Bundle extras = intent.getExtras();

			JSONObject jsonObject = OutputPlugin.jsonForBundle(extras);

			this.persistJSONObject(jsonObject);
			this.uploadPendingObjects();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private void uploadPendingObjects()
	{
		if (this._uploadThread != null)
			return;

		final HttpUploadPlugin me = this;

		final List<String> uploadCount = new ArrayList<String>();

		final Runnable r = new Runnable()
		{
			@SuppressWarnings("deprecation")
			public void run()
			{
				boolean continueUpload = false;

				long now = System.currentTimeMillis();

				if (now - me._lastUpload > me.uploadPeriod())
				{
					me._lastUpload = now;

					File pendingFolder = me.getPendingFolder();

					File[] pendingFiles = pendingFolder.listFiles(new FileFilter()
					{
						public boolean accept(File file)
						{
							if (file.getName().toLowerCase().endsWith(".json"))
								return true;

							return false;
						}
					});

					ArrayList<JSONObject> pendingObjects = new ArrayList<JSONObject>();

					for (File f : pendingFiles)
					{
						if (pendingObjects.size() < 1024)
						{
							try
							{
								BufferedReader reader = new BufferedReader(new FileReader(f));

							    StringBuffer sb = new StringBuffer();
							    String line = null;

							    while((line = reader.readLine()) != null)
							    {
							    	sb.append(line);
							    }

							    reader.close();

							    JSONArray jsonArray = new JSONArray(sb.toString());

							    for (int i = 0; i < jsonArray.length(); i++)
							    {
							    	JSONObject jsonObject = jsonArray.getJSONObject(i);

							    	pendingObjects.add(jsonObject);
							    }
							}
							catch (FileNotFoundException e)
							{
								e.printStackTrace();
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
							catch (JSONException e)
							{
								e.printStackTrace();
							}

							f.delete();
						}
					}

					if (pendingObjects.size() > 0)
					{
						long maxUploadSize = me.maxUploadSize();
						long tally = 2;

						List<JSONObject> toUpload = new ArrayList<JSONObject>();

						for (int i = 0; i < pendingObjects.size() && tally < maxUploadSize; i++)
						{
							try
							{
								JSONObject json = pendingObjects.get(i);

								String jsonString = json.toString();

								int jsonSize = jsonString.toString().getBytes("UTF-8").length;

								if (jsonSize + tally < maxUploadSize)
								{
									tally += jsonSize + 2;

									toUpload.add(json);
								}
							}
							catch (UnsupportedEncodingException e)
							{
								e.printStackTrace();
							}
						}

						JSONArray uploadArray = new JSONArray();

						for (int i = 0; i < toUpload.size(); i++)
						{
							uploadArray.put(toUpload.get(i));
						}

						try
						{
							JSONObject jsonMessage = new JSONObject();

							jsonMessage.put(OPERATION_KEY, "SubmitProbes");
							jsonMessage.put(PAYLOAD_KEY, uploadArray.toString());
							jsonMessage.put(USER_HASH_KEY, me.preferences.getString("config_user_hash", ""));

							MessageDigest md = MessageDigest.getInstance("MD5");

							byte[] digest = md.digest((jsonMessage.get(USER_HASH_KEY).toString() + jsonMessage.get(OPERATION_KEY).toString() + jsonMessage.get(PAYLOAD_KEY).toString()).getBytes("UTF-8"));

							String checksum = (new BigInteger(1, digest)).toString(16);

							Log.e("PRM", "SEND CHECKSUM " + checksum);

							while(checksum.length() < 32 )
							{
								checksum = "0" + checksum;
							}

							jsonMessage.put(CHECKSUM_KEY, checksum);

							NotificationManager noteManager = (NotificationManager) me.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

							AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Purple Robot", me.getContext());

							String title = me.getContext().getString(R.string.notify_upload_data);

						    Notification note = new Notification(R.drawable.ic_notify_sync, title, System.currentTimeMillis());
						    PendingIntent contentIntent = PendingIntent.getActivity(me.getContext(), 0, new Intent(me.getContext(), StartActivity.class), Notification.FLAG_ONGOING_EVENT);

						    note.setLatestEventInfo(me.getContext(), title, title, contentIntent);

						    note.flags = Notification.FLAG_ONGOING_EVENT;

							try
							{
								URI siteUri = new URI(me.getContext().getResources().getString(R.string.sensor_upload_url));

								HttpPost httpPost = new HttpPost(siteUri);

								List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
								nameValuePairs.add(new BasicNameValuePair("json", jsonMessage.toString()));
								httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

							    noteManager.notify(999, note);

								Log.e("PRM", "SENDING " + httpPost.getEntity().getContentLength() + " BYTES");
								HttpResponse response = httpClient.execute(httpPost);

								Log.e("PRM", "RECVED " + response.getEntity().getContentLength() + " BYTES");

					            HttpEntity httpEntity = response.getEntity();
					            String body = EntityUtils.toString(httpEntity);

					            JSONObject json = new JSONObject(body);

					            String status = json.getString(STATUS_KEY);

					            String responsePayload = "";

					            Log.e("PRM", "STATUS: " + status);

					            if (json.has(PAYLOAD_KEY))
					            	responsePayload = json.getString(PAYLOAD_KEY);

					            if (status.equals("error") == false)
					            {
									byte[] responseDigest = md.digest((status + responsePayload).getBytes("UTF-8"));
									String responseChecksum = (new BigInteger(1, responseDigest)).toString(16);

									while(responseChecksum.length() < 32 )
									{
										responseChecksum = "0" + responseChecksum;
									}

									if (responseChecksum.equals(json.getString(CHECKSUM_KEY)))
									{
										Log.e("PRM", "CHECKSUM VALID");

										pendingObjects.removeAll(toUpload);

										continueUpload = true;
									}
									else
									{
										Log.e("PRM", "CHECKSUM INVALID");
									}
					            }
					            else
					            	Log.e("PRM", "MESSAGE: " + responsePayload);
							}
							catch (NotFoundException e)
							{
								e.printStackTrace();
							}
							catch (URISyntaxException e)
							{
								e.printStackTrace();
							}
							catch (ClientProtocolException e)
							{
								e.printStackTrace();
							}
							catch (IOException e)
							{

							}
							finally
							{
					            httpClient.close();

					            noteManager.cancel(999);
							}

							for (int k = 0; pendingObjects.size() > 0; k++)
							{
								JSONArray toSave = new JSONArray();

								List<JSONObject> toRemove = new ArrayList<JSONObject>();

								for (int i = 0; i < pendingObjects.size() && i < 100; i++)
								{
									toSave.put(pendingObjects.get(i));
									toRemove.add(pendingObjects.get(i));
								}

								File f = new File(pendingFolder, "pending_" + k + ".json");

								OutputStream out = new BufferedOutputStream(new FileOutputStream(f));

								byte[] jsonBytes = toSave.toString().getBytes("UTF-8");

								out.write(jsonBytes);

								out.flush();
								out.close();

								pendingObjects.removeAll(toRemove);
							}
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
						catch (NoSuchAlgorithmException e)
						{
							e.printStackTrace();
						}
						catch (UnsupportedEncodingException e)
						{
							e.printStackTrace();
						}
						catch (FileNotFoundException e)
						{
							e.printStackTrace();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
				}

				if (continueUpload && uploadCount.size() < 16)
				{
					uploadCount.add("");

					me._lastUpload = 0;
					this.run();
				}

				me._uploadThread = null;
			}
		};

		this._uploadThread = new Thread(r);
		this._uploadThread.start();
	}

	private File getPendingFolder()
	{
		File internalStorage = this.getContext().getFilesDir();

		if (!internalStorage.exists())
			internalStorage.mkdirs();

		File pendingFolder = new File(internalStorage, CACHE_DIR);

		if (!pendingFolder.exists())
			pendingFolder.mkdirs();

		return pendingFolder;
	}

	private void persistJSONObject(JSONObject jsonObject)
	{
		long now = System.currentTimeMillis();

		this._pendingSaves.add(jsonObject.toString());

		if (now - this._lastSave > this.savePeriod()) // Save files every 15 seconds..
		{
			this._lastSave = now;

			File pendingFolder = this.getPendingFolder();

			String filename = now + ".json";

			File f = new File(pendingFolder, filename);

			HashSet<String> toRemove = new HashSet<String>();
			HashSet<String> invalidRemove = new HashSet<String>();

			JSONArray saveArray = new JSONArray();

			for (String jsonString : this._pendingSaves)
			{
				try
				{
					JSONObject json = new JSONObject(jsonString);

					saveArray.put(json);

					toRemove.add(jsonString);
				}
				catch (JSONException e)
				{
					e.printStackTrace();

					invalidRemove.add(jsonString);
				}
			}

			this._pendingSaves.removeAll(invalidRemove);

			try
			{
				OutputStream out = new BufferedOutputStream(new FileOutputStream(f));

				byte[] jsonBytes = saveArray.toString().getBytes("UTF-8");

				out.write(jsonBytes);

				out.flush();
				out.close();

				this._pendingSaves.removeAll(toRemove);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
