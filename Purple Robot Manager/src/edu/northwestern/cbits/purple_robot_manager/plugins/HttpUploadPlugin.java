package edu.northwestern.cbits.purple_robot_manager.plugins;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.util.Log;

import com.WazaBe.HoloEverywhere.preference.SharedPreferences.Editor;

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

	private final static long MAX_UPLOAD_PERIOD = 3600000;
	private final static long MIN_UPLOAD_PERIOD = 300000;

	private final static long MAX_UPLOAD_SIZE = 1048576; // 1MB
	private final static long MIN_UPLOAD_SIZE = 16384; // 16KB

	private List<String> _pendingSaves = new ArrayList<String>();
	private long _lastSave = 0;
	private long _lastUpload = 0;

	private long _uploadSize = MIN_UPLOAD_SIZE;
	private long _uploadPeriod = MIN_UPLOAD_SIZE;

	private Thread _uploadThread = null;

	private void logSuccess(boolean success)
	{
		if (success)
		{
			this._uploadSize *=2;
			this._uploadPeriod /=2;
		}
		else
		{
			this._uploadSize /=2;
			this._uploadPeriod *=2;
		}

		if (this._uploadSize > MAX_UPLOAD_SIZE)
			this._uploadSize = MAX_UPLOAD_SIZE;
		else if (this._uploadSize < MIN_UPLOAD_SIZE)
			this._uploadSize = MIN_UPLOAD_SIZE;

		if (this._uploadPeriod > MAX_UPLOAD_PERIOD)
			this._uploadPeriod = MAX_UPLOAD_PERIOD;
		else if (this._uploadPeriod < MIN_UPLOAD_PERIOD)
			this._uploadPeriod = MIN_UPLOAD_PERIOD;
	}

	private long savePeriod()
	{
		return 30000;
	}

	private long uploadPeriod()
	{
		return this._uploadPeriod;
	}

	private long maxUploadSize()
	{
		return this._uploadSize; // 128KB
	}

	public String[] respondsTo()
	{
		String[] activeActions = { Probe.PROBE_READING, OutputPlugin.FORCE_UPLOAD };

		return activeActions;
	}

	@Override
	public void processIntent(Intent intent)
	{
		if (OutputPlugin.FORCE_UPLOAD.equals(intent.getAction()))
		{
			this._lastUpload = 0;

			this.uploadPendingObjects();
		}
		else
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
	}

	private String getUserHash()
	{
		String userHash = this.preferences.getString("config_user_hash", null);

		if (userHash == null)
		{
			String userId = "unknown-user";

			Context context = this.getContext();

			AccountManager manager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
			Account[] list = manager.getAccountsByType("com.google");

			if (list.length == 0)
				list = manager.getAccounts();

			if (list.length > 0)
				userId = list[0].name;

			try
			{
				MessageDigest md = MessageDigest.getInstance("MD5");
				byte[] digest = md.digest(userId.getBytes("UTF-8"));

				userHash = (new BigInteger(1, digest)).toString(16);

				while(userHash.length() < 32 )
				{
					userHash = "0" + userHash;
				}
			}
			catch (NoSuchAlgorithmException e)
			{
				e.printStackTrace();
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}

			Editor e = this.preferences.edit();

			if (userId != null)
				e.putString("config_user_id", userId);

			if (userHash != null)
				e.putString("config_user_hash", userHash);

			e.commit();
		}

		return userHash;
	}

	private void uploadPendingObjects()
	{
		if (this._uploadThread != null)
			return;

		final long now = System.currentTimeMillis();

		final HttpUploadPlugin me = this;

		if (now - me._lastUpload > me.uploadPeriod())
		{
			final List<String> uploadCount = new ArrayList<String>();
			final Resources resources = this.getContext().getResources();

			final Runnable r = new Runnable()
			{
				@SuppressWarnings("deprecation")
				public void run()
				{
					boolean continueUpload = false;
					boolean wasSuccessful = false;

					Cipher encrypt = null;
					Cipher decrypt = null;

					try
					{
						String keyString = "PRM" + (new StringBuffer(me.getUserHash())).reverse().toString();
						SecretKeySpec secretKey;
						secretKey = new SecretKeySpec(keyString.getBytes("utf-8"),"AES");

				        encrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
				        encrypt.init(Cipher.ENCRYPT_MODE, secretKey);

				        decrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
				        decrypt.init(Cipher.ENCRYPT_MODE, secretKey);
					}
					catch (UnsupportedEncodingException e)
					{
						throw new RuntimeException(e);
					}
					catch (NoSuchAlgorithmException e)
					{
						throw new RuntimeException(e);
					}
					catch (NoSuchPaddingException e)
					{
						throw new RuntimeException(e);
					}
					catch (InvalidKeyException e)
					{
						throw new RuntimeException(e);
					}

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
						        CipherInputStream cin = new CipherInputStream(new FileInputStream(f), decrypt);

						        ByteArrayOutputStream baos = new ByteArrayOutputStream();

						        byte[] buffer = new byte[1024];
						        int read = 0;

						        while ((read = cin.read(buffer, 0, buffer.length)) != -1)
						        {
						        	baos.write(buffer, 0, read);
						        }

						        cin.close();

								JSONArray jsonArray = new JSONArray(baos.toString("utf-8"));

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
						me.broadcastMessage(R.string.message_package_upload);

						long maxUploadSize = me.maxUploadSize();
						long tally = 0;

						List<JSONObject> toUpload = new ArrayList<JSONObject>();

						for (int i = 0; i < pendingObjects.size() && tally < maxUploadSize; i++)
						{
							try
							{
								JSONObject json = pendingObjects.get(i);

								String jsonString = json.toString();

								int jsonSize = jsonString.toString().getBytes("UTF-8").length;

								if (jsonSize > maxUploadSize)
								{
									// Skip until connection is better...
								}
								else if (jsonSize + tally < maxUploadSize)
								{
									tally += jsonSize;

									toUpload.add(json);
								}
							}
							catch (UnsupportedEncodingException e)
							{
								e.printStackTrace();
							}
						}

						Log.e("PRM", "UPLOADING " + toUpload.size() + " ITEMS...");

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

							String userHash = me.getUserHash();

							jsonMessage.put(USER_HASH_KEY, userHash);

							MessageDigest md = MessageDigest.getInstance("MD5");

							byte[] digest = md.digest((jsonMessage.get(USER_HASH_KEY).toString() + jsonMessage.get(OPERATION_KEY).toString() + jsonMessage.get(PAYLOAD_KEY).toString()).getBytes("UTF-8"));

							String checksum = (new BigInteger(1, digest)).toString(16);

							while(checksum.length() < 32 )
							{
								checksum = "0" + checksum;
							}

							jsonMessage.put(CHECKSUM_KEY, checksum);

							NotificationManager noteManager = (NotificationManager) me.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

							AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Purple Robot", me.getContext());

							String title = me.getContext().getString(R.string.notify_upload_data);

							Notification note = new Notification(R.drawable.ic_notify_foreground, title, System.currentTimeMillis());
							PendingIntent contentIntent = PendingIntent.getActivity(me.getContext(), 0, new Intent(me.getContext(), StartActivity.class), Notification.FLAG_ONGOING_EVENT);

							note.setLatestEventInfo(me.getContext(), title, title, contentIntent);

							note.flags = Notification.FLAG_ONGOING_EVENT;

							String body = null;

							try
							{
								URI siteUri = new URI(me.getContext().getResources().getString(R.string.sensor_upload_url));

								HttpPost httpPost = new HttpPost(siteUri);

								List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
								nameValuePairs.add(new BasicNameValuePair("json", jsonMessage.toString()));
								HttpEntity entity = new UrlEncodedFormEntity(nameValuePairs);

//								entity = AndroidHttpClient.getCompressedEntity(EntityUtils.toByteArray(entity), me.getContext().getContentResolver());
//								httpPost.setHeader("Content-Encoding", "gzip");

								httpPost.setEntity(entity);

								String uploadMessage = String.format(resources.getString(R.string.message_transmit_bytes), httpPost.getEntity().getContentLength());
								me.broadcastMessage(uploadMessage);

								noteManager.notify(999, note);

								AndroidHttpClient.modifyRequestToAcceptGzipResponse(httpPost);

								HttpResponse response = httpClient.execute(httpPost);

								HttpEntity httpEntity = response.getEntity();

								String contentHeader = response.getFirstHeader("Content-Encoding").getValue();

								if (contentHeader != null && contentHeader.endsWith("gzip"))
								{
									BufferedInputStream in = new BufferedInputStream(AndroidHttpClient.getUngzippedContent(httpEntity));

									ByteArrayOutputStream out = new ByteArrayOutputStream();

									int read = 0;
									byte[] buffer = new byte[1024];

									while((read = in.read(buffer, 0, buffer.length)) != -1)
									{
										out.write(buffer, 0, read);
									}

									in.close();

									body = out.toString("utf-8");
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

									while(responseChecksum.length() < 32 )
									{
										responseChecksum = "0" + responseChecksum;
									}

									if (responseChecksum.equals(json.getString(CHECKSUM_KEY)))
									{
										pendingObjects.removeAll(toUpload);

										continueUpload = true;

										wasSuccessful = true;

										me.broadcastMessage(R.string.message_upload_successful);

									}
									else
										me.broadcastMessage(R.string.message_checksum_failed);
								}
								else
								{
									String errorMessage = String.format(resources.getString(R.string.message_server_error), status);
									me.broadcastMessage(errorMessage);
								}
							}
							catch (HttpHostConnectException e)
							{
								me.broadcastMessage(R.string.message_http_connection_error);
							}
							catch (SocketTimeoutException e)
							{
								me.broadcastMessage(R.string.message_socket_timeout_error);
							}
							catch (SocketException e)
							{
								String errorMessage = String.format(resources.getString(R.string.message_socket_error), e.getMessage());
								me.broadcastMessage(errorMessage);
							}
							catch (UnknownHostException e)
							{
								me.broadcastMessage(R.string.message_unreachable_error);
							}
							catch (JSONException e)
							{
								me.broadcastMessage(R.string.message_response_error);
							}
							catch (Exception e)
							{
								e.printStackTrace();

								e.printStackTrace();
								String errorMessage = String.format(resources.getString(R.string.message_general_error), e.toString());
								me.broadcastMessage(errorMessage);

								Log.e("PRM", "GENERAL ERROR BODY: " + body);
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

						        CipherOutputStream cout = new CipherOutputStream(new FileOutputStream(f), encrypt);

								byte[] jsonBytes = toSave.toString().getBytes("UTF-8");

								cout.write(jsonBytes);

								cout.flush();
								cout.close();

								pendingObjects.removeAll(toRemove);
							}

							me.logSuccess(wasSuccessful);
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
						catch (NoSuchAlgorithmException e)
						{
							throw new RuntimeException(e);
						}
						catch (UnsupportedEncodingException e)
						{
							throw new RuntimeException(e);
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
	}

	protected void broadcastMessage(int stringId)
	{
		this.broadcastMessage(this.getContext().getResources().getString(stringId));
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

		if (jsonObject != null)
			this._pendingSaves.add(jsonObject.toString());

		if (now - this._lastSave > this.savePeriod() || this._pendingSaves.size() > 128) // Save files every 15 seconds or 128 samples...
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

					if (saveArray.length() < 256)
						saveArray.put(json);

					toRemove.add(jsonString);
				}
				catch (JSONException e)
				{
					invalidRemove.add(jsonString);
				}
				catch (OutOfMemoryError e)
				{
					invalidRemove.add(jsonString);
				}
			}

			this._pendingSaves.removeAll(invalidRemove);

			try
			{
		        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

		        String keyString = "PRM" + (new StringBuffer(this.getUserHash())).reverse().toString();

		        keyString = keyString.substring(0, 16);

		        SecretKeySpec secretKey = new SecretKeySpec(keyString.getBytes("utf-8"),"AES");

		        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

		        CipherOutputStream cout = new CipherOutputStream(new FileOutputStream(f), cipher);

				byte[] jsonBytes = saveArray.toString().getBytes("UTF-8");

				cout.write(jsonBytes);

				cout.flush();
				cout.close();

				this._pendingSaves.removeAll(toRemove);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (UnsupportedEncodingException e)
			{
				throw new RuntimeException(e);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (InvalidKeyException e)
			{
				throw new RuntimeException(e);
			}
			catch (NoSuchAlgorithmException e)
			{
				throw new RuntimeException(e);
			}
			catch (NoSuchPaddingException e)
			{
				throw new RuntimeException(e);
			}

			if (this._pendingSaves.size() > 0)
			{
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException e)
				{

				}

				this._lastSave = 0;
				this.persistJSONObject(null);
			}
		}
	}
}
