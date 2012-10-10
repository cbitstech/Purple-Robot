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

import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class HttpUploadPlugin extends OutputPlugin
{
	private final static String CACHE_DIR = "http_pending_uploads";
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
			jsonObject.put("CBITSUserName", "TODO: CBITSUserName");
			jsonObject.put("CBITSEventID", "TODO: SOME-GUID-1234");

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

		final Runnable r = new Runnable()
		{
			public void run()
			{
				boolean continueUpload = true;

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

							jsonMessage.put("operation", "submit_probes");

							JSONObject payload = new JSONObject();
							payload.put("name", "Submit Probe Values");
							payload.put("value", uploadArray);

							jsonMessage.put("payload", payload.toString());

							MessageDigest md = MessageDigest.getInstance("MD5");
							byte[] digest = md.digest(("submit_probes" + jsonMessage.get("payload").toString()).getBytes("UTF-8"));

							String checksum = (new BigInteger(1, digest)).toString(16);

							while(checksum.length() < 32 )
							{
								checksum = "0" + checksum;
							}

							jsonMessage.put("checksum", checksum);

							AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Purple Robot", me.getContext());

							try
							{
								URI siteUri = new URI(me.getContext().getResources().getString(R.string.sensor_upload_url));

								HttpPost httpPost = new HttpPost(siteUri);

								List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
								nameValuePairs.add(new BasicNameValuePair("json", jsonMessage.toString()));
								httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

								HttpResponse response = httpClient.execute(httpPost);

					            HttpEntity httpEntity = response.getEntity();
					            String body = EntityUtils.toString(httpEntity);

					            JSONObject json = new JSONObject(body);

					            String status = json.getString("status");
					            String message = json.getString("message");
					            String operation = json.getString("operation");
					            String responsePayload = json.getString("payload");

					            if (message.equals("error") == false)
					            {
									byte[] responseDigest = md.digest((status + message + operation + responsePayload).getBytes("UTF-8"));
									String responseChecksum = (new BigInteger(1, responseDigest)).toString(16);

									while(responseChecksum.length() < 32 )
									{
										responseChecksum = "0" + responseChecksum;
									}

//									JSONObject responseJson = new JSONObject(responsePayload);

//									Log.e("PRM", "CHECKSUM " + json.getString("checksum") + " =? " + responseChecksum);

//									Log.e("PRM", "GOT SUCCESSFUL UPLOAD RESPONSE: " + responseJson);

							    	pendingObjects.removeAll(toUpload);
					            }
							}
							catch (NotFoundException e)
							{
								e.printStackTrace();
								continueUpload = false;
							}
							catch (URISyntaxException e)
							{
								e.printStackTrace();
								continueUpload = false;
							}
							catch (ClientProtocolException e)
							{
								e.printStackTrace();
								continueUpload = false;
							}
							catch (IOException e)
							{
								e.printStackTrace();
								continueUpload = false;
							}

				            httpClient.close();

							JSONArray toSave = new JSONArray();

							if (pendingObjects.size() > 0)
							{
								for (int i = 0; i < pendingObjects.size(); i++)
								{
									toSave.put(pendingObjects.get(i));
								}

								File f = new File(pendingFolder, "pending.json");

								OutputStream out = new BufferedOutputStream(new FileOutputStream(f));

								byte[] jsonBytes = toSave.toString().getBytes("UTF-8");

								out.write(jsonBytes);

								out.flush();
								out.close();
							}
							else
								continueUpload = false;
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
				else
					continueUpload = false;

				if (continueUpload)
				{
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
