package edu.northwestern.cbits.purple_robot_manager.plugins;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.NullCipher;
import javax.crypto.spec.IvParameterSpec;
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
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.util.Log;
import android.widget.Toast;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;
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

	private final static int WIFI_MULTIPLIER = 2;

	private final static long MAX_UPLOAD_PERIOD = 3600000;
	private final static long MIN_UPLOAD_PERIOD = 300000;

	private final static long MAX_RETRIES = 16;

	private final static long MAX_UPLOAD_SIZE = 262144; // 512KB
	private final static long MIN_UPLOAD_SIZE = 16384; // 16KB

	private static final String CRYPTO_ALGORITHM = "AES/CBC/PKCS5Padding";

	private static SharedPreferences prefs = null;

	private List<String> _pendingSaves = new ArrayList<String>();
	private long _lastSave = 0;
	private long _lastUpload = 0;

	private double _throughput = 0.0;

	private boolean _wifiAvailable = false;
	private long _lastWifiCheck = 0;

	private long _uploadSize = MIN_UPLOAD_SIZE;
	private long _uploadPeriod = MIN_UPLOAD_PERIOD;

	private boolean _uploading = false;

	private int _failCount = 0;

	protected static SharedPreferences getPreferences(Context context)
	{
		if (HttpUploadPlugin.prefs  == null)
			HttpUploadPlugin.prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

		return HttpUploadPlugin.prefs;
	}

	public double getRecentThroughput()
	{
		return this._throughput;
	}

	private void logSuccess(boolean success)
	{
		if (success)
		{
			this._uploadSize *= 2;
			this._uploadPeriod /= 2;
		}
		else
		{
			this._uploadSize /= 2;
			this._uploadPeriod *= 2;
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
		return 10000;
	}

	private long uploadPeriod()
	{
		return this._uploadPeriod;
	}

	private long maxUploadSize()
	{
		int multiplier = 1;

		if (this.wifiAvailable())
			multiplier = WIFI_MULTIPLIER;

		return this._uploadSize * multiplier; // 128KB
	}

	public String[] respondsTo()
	{
		String[] activeActions = { Probe.PROBE_READING, OutputPlugin.FORCE_UPLOAD };

		return activeActions;
	}

	public void processIntent(Intent intent)
	{
		if (OutputPlugin.FORCE_UPLOAD.equals(intent.getAction()))
		{
			this._lastUpload = 0;
			this._lastSave = 0;
			this._failCount = 0;

			final HttpUploadPlugin me = this;

			Runnable r = new Runnable()
			{
				public void run()
				{
					me.persistJSONObject(null);
					me.uploadPendingObjects();
				}
			};

			Thread t = new Thread(r);
			t.start();
		}
		else
		{
			try
			{
				Bundle extras = intent.getExtras();

				final JSONObject jsonObject = OutputPlugin.jsonForBundle(extras);

				final HttpUploadPlugin me = this;

				Runnable r = new Runnable()
				{
					public void run()
					{
						me.persistJSONObject(jsonObject);
						me.uploadPendingObjects();
					}
				};

				Thread t = new Thread(r);
				t.start();
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
	}

	private SecretKeySpec keyForCipher(String cipherName) throws UnsupportedEncodingException
	{
		String userHash = this.getUserHash();
		String keyString = (new StringBuffer(userHash)).reverse().toString();

		if (cipherName != null && cipherName.startsWith("AES"))
		{
			byte[] stringBytes = keyString.getBytes("UTF-8");

			byte[] keyBytes = new byte[32];
			Arrays.fill(keyBytes, (byte) 0x00);

			for (int i = 0; i < keyBytes.length && i < stringBytes.length; i++)
			{
				keyBytes[i] = stringBytes[i];
			}

			SecretKeySpec key = new SecretKeySpec(keyBytes, cipherName);

			return key;
		}

		return keyForCipher(HttpUploadPlugin.CRYPTO_ALGORITHM);
	}

	private String getUserHash()
	{
		Context context = this.getContext();

		SharedPreferences prefs = HttpUploadPlugin.getPreferences(context);

		String userHash = prefs.getString("config_user_hash", null);

		if (userHash == null)
		{
			String userId = "unknown-user";

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

				while (userHash.length() < 32)
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

			Editor e = prefs.edit();

			if (userId != null)
				e.putString("config_user_id", userId);

			if (userHash != null)
				e.putString("config_user_hash", userHash);

			e.commit();
		}

		return userHash;
	}

	private boolean wifiAvailable()
	{
		long now = System.currentTimeMillis();

		if (now - this._lastWifiCheck > 10000)
		{
			this._lastWifiCheck = now;

			WifiManager wifi = (WifiManager) this.getContext().getSystemService(Context.WIFI_SERVICE);

			if (wifi.isWifiEnabled())
			{
				this._wifiAvailable = true;

				ConnectivityManager connection = (ConnectivityManager) this.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

				NetworkInfo netInfo = connection.getActiveNetworkInfo();

				if (netInfo != null)
				{
					if (netInfo.getType() != ConnectivityManager.TYPE_WIFI)
						this._wifiAvailable = false;
					else if (netInfo.getState() != NetworkInfo.State.CONNECTED && netInfo.getState() != NetworkInfo.State.CONNECTING)
						this._wifiAvailable = false;
				}
				else
					this._wifiAvailable = false;
			}
			else
				this._wifiAvailable =  false;
		}

		return this._wifiAvailable;
	}

	private void uploadPendingObjects()
	{
		if (this._uploading)
			return;

		final HttpUploadPlugin me = this;

		final long now = System.currentTimeMillis();

		if (now - me._lastUpload > me.uploadPeriod())
		{
			this._uploading = true;

			final SharedPreferences prefs = HttpUploadPlugin.getPreferences(this.getContext());

			if (prefs.getBoolean("config_enable_data_server", false) == false)
			{
				this._uploading = false;
				return;
			}

			if (prefs.getBoolean("config_restrict_data_wifi", true))
			{
				if (this.wifiAvailable() == false)
				{
					this.broadcastMessage(R.string.message_wifi_pending);

					this._lastUpload = now;
					this._uploading = false;

					return;
				}
			}

			final Resources resources = this.getContext().getResources();
			final long maxUploadSize = me.maxUploadSize();

			final Runnable r = new Runnable()
			{
				@SuppressWarnings("deprecation")
				public void run()
				{
					long start = System.currentTimeMillis();

					boolean continueUpload = false;
					boolean wasSuccessful = false;

					Cipher encrypt = new NullCipher();
					Cipher decrypt = new NullCipher();

					if (prefs.getBoolean("config_http_encrypt", true))
					{
						try
						{
							SecretKeySpec secretKey = me.keyForCipher(HttpUploadPlugin.CRYPTO_ALGORITHM);

							IvParameterSpec ivParameterSpec = new IvParameterSpec(me.getIVBytes());

							encrypt = Cipher.getInstance(HttpUploadPlugin.CRYPTO_ALGORITHM);
							encrypt.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

							decrypt = Cipher.getInstance(HttpUploadPlugin.CRYPTO_ALGORITHM);
							decrypt.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
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
						catch (InvalidAlgorithmParameterException e)
						{
							throw new RuntimeException(e);
						}
					}

					me._lastUpload = now;

					File pendingFolder = me.getPendingFolder();

					File archiveFolder = me.getArchiveFolder();

					me.broadcastMessage(R.string.message_reading_files);

					String[] filenames = pendingFolder.list(new FilenameFilter()
					{
						public boolean accept(File dir, String filename)
						{
							return filename.endsWith(".json");
						}
					});

					if (filenames == null)
						filenames = new String[0];

					Collections.shuffle(Arrays.asList(filenames));

					ArrayList<JSONObject> pendingObjects = new ArrayList<JSONObject>();

					int totalRead = 0;

					for (String filename : filenames)
					{
						if (totalRead < maxUploadSize)
						{
							File f = new File(pendingFolder, filename);

							try
							{
								if (f.length() > 32)
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

									JSONArray jsonArray = new JSONArray(baos.toString("UTF-8"));

									totalRead += baos.size();

									for (int i = 0; i < jsonArray.length(); i++)
									{
										JSONObject jsonObject = jsonArray.getJSONObject(i);

										pendingObjects.add(jsonObject);
									}
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
								Log.e("PRM", "JSON ERROR: " + f.getAbsolutePath() + " " + f.length());

								e.printStackTrace();
							}

							if (prefs.getBoolean("config_http_archive", false))
							{
								long now = System.currentTimeMillis();

								File archive = new File(archiveFolder, now + ".archive");

								f.renameTo(archive);
							}
							else
								f.delete();
						}
					}

					me.broadcastMessage(R.string.message_reading_complete);

					if (pendingObjects.size() > 0)
					{
						me.broadcastMessage(R.string.message_package_upload);

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

							byte[] digest = md.digest((jsonMessage.get(USER_HASH_KEY).toString() +
									jsonMessage.get(OPERATION_KEY).toString() + jsonMessage.get(PAYLOAD_KEY).toString()).getBytes("UTF-8"));

							String checksum = (new BigInteger(1, digest)).toString(16);

							while (checksum.length() < 32)
							{
								checksum = "0" + checksum;
							}

							jsonMessage.put(CHECKSUM_KEY, checksum);

							NotificationManager noteManager = (NotificationManager) me.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

							AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Purple Robot", me.getContext());

							String title = me.getContext().getString(R.string.notify_upload_data);

							Notification note = new Notification(R.drawable.ic_notify_foreground, title, System.currentTimeMillis());
							PendingIntent contentIntent = PendingIntent.getActivity(me.getContext(), 0,
									new Intent(me.getContext(), StartActivity.class), Notification.FLAG_ONGOING_EVENT);

							note.setLatestEventInfo(me.getContext(), title, title, contentIntent);

							note.flags = Notification.FLAG_ONGOING_EVENT;

							String body = null;

							try
							{
								String uriString = prefs.getString("config_data_server_uri", me.getContext().getResources().getString(R.string.sensor_upload_url));

								URI siteUri = new URI(uriString);

								HttpPost httpPost = new HttpPost(siteUri);

								String jsonString = jsonMessage.toString();

								List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
								nameValuePairs.add(new BasicNameValuePair("json", jsonString));
								HttpEntity entity = new UrlEncodedFormEntity(nameValuePairs);

								httpPost.setEntity(entity);

								String uploadMessage = String.format(resources.getString(R.string.message_transmit_bytes),
										(httpPost.getEntity().getContentLength() / 1024));
								me.broadcastMessage(uploadMessage);

								noteManager.notify(12345, note);

								AndroidHttpClient.modifyRequestToAcceptGzipResponse(httpPost);

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

								Log.i("PRM", "GOT RESPONSE (" + json.getString("Status") + "): " + body);

								String status = json.getString(STATUS_KEY);

								String responsePayload = "";

								if (json.has(PAYLOAD_KEY))
									responsePayload = json.getString(PAYLOAD_KEY);

								if (status.equals("error") == false)
								{
									byte[] responseDigest = md.digest((status + responsePayload).getBytes("UTF-8"));
									String responseChecksum = (new BigInteger(1, responseDigest)).toString(16);

									while (responseChecksum.length() < 32)
									{
										responseChecksum = "0" + responseChecksum;
									}

									if (responseChecksum.equals(json.getString(CHECKSUM_KEY)))
									{
										pendingObjects.removeAll(toUpload);

										continueUpload = true;

										wasSuccessful = true;

										String uploadedMessage = String.format(resources.getString(R.string.message_upload_successful),
												(httpPost.getEntity().getContentLength() / 1024));

										me._failCount = 0;

										me.broadcastMessage(uploadedMessage);

										long elapsed = (System.currentTimeMillis() - start) / 1000;

										me._throughput = ((double) httpPost.getEntity().getContentLength()) / elapsed;
									}
									else
									{
										me.broadcastMessage(R.string.message_checksum_failed);
										me._failCount += 1;

										me._throughput = 0.0;
									}
								}
								else
								{
									String errorMessage = String.format(resources.getString(R.string.message_server_error),	status);
									me.broadcastMessage(errorMessage);

									Log.e("PRM", "XMITTED " + httpPost.getEntity().getContentLength() + " BYTES");

									me._failCount += 1;

									me._throughput = 0.0;
								}
							}
							catch (HttpHostConnectException e)
							{
								me.broadcastMessage(R.string.message_http_connection_error);
								e.printStackTrace();
							}
							catch (SocketTimeoutException e)
							{
								me.broadcastMessage(R.string.message_socket_timeout_error);
								e.printStackTrace();
							}
							catch (SocketException e)
							{
								String errorMessage = String.format(resources.getString(R.string.message_socket_error),	e.getMessage());
								me.broadcastMessage(errorMessage);
								e.printStackTrace();
							}
							catch (UnknownHostException e)
							{
								me.broadcastMessage(R.string.message_unreachable_error);
								e.printStackTrace();
							}
							catch (JSONException e)
							{
								me.broadcastMessage(R.string.message_response_error);
								e.printStackTrace();
							}
							catch (Exception e)
							{
								e.printStackTrace();
								String errorMessage = String.format(resources.getString(R.string.message_general_error), e.toString());
								me.broadcastMessage(errorMessage);

								Log.i("PRM", "GENERAL ERROR BODY: " + body);
							}
							finally
							{
								httpClient.close();

								String message = me.getContext().getString(R.string.notify_running);

								note.setLatestEventInfo(me.getContext(), message, message, contentIntent);

								noteManager.notify(12345, note);
							}

							int l = 0;
							Random r = new Random(System.currentTimeMillis());

							while(pendingObjects.size() > 0)
							{
								JSONArray toSave = new JSONArray();

								List<JSONObject> toRemove = new ArrayList<JSONObject>();

								for (int i = 0; i < pendingObjects.size() && i < 100; i++)
								{
									toSave.put(pendingObjects.get(i));
									toRemove.add(pendingObjects.get(i));
								}

								File f = new File(pendingFolder, "pending_" + l + ".json");

								while (f.exists())
								{
									l += r.nextInt(10);

									f = new File(pendingFolder, "pending_" + l + ".json");
								}

								CipherOutputStream cout = new CipherOutputStream(new FileOutputStream(f), encrypt);

								byte[] jsonBytes = toSave.toString().getBytes("UTF-8");

								cout.write(jsonBytes);

								cout.flush();
								cout.close();

								pendingObjects.removeAll(toRemove);
							}

							if (wasSuccessful == false && me._failCount < MAX_RETRIES)
							{

							}
							else
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

					me._uploading = false;

					Log.e("PRM", "FAIL COUNT: " + me._failCount);

					if (continueUpload || (me._failCount > 0 && me._failCount < MAX_RETRIES))
					{
						me._lastUpload = 0;

						try
						{
							Thread.sleep(1000);
							me.uploadPendingObjects();
						}
						catch (InterruptedException e)
						{

						}
					}
				}
			};

			Thread t = new Thread(r);
			t.start();
		}
	}

	protected byte[] getIVBytes()
	{
		byte[] bytes = {(byte) 0xff, 0x00, 0x11, (byte) 0xee, 0x22,
						(byte) 0xdd, 0x33, (byte) 0xcc, 0x44, (byte) 0xbb, 0x55,
						(byte) 0xaa, 0x66, (byte) 0x99, 0x77, (byte) 0x88 };

		return bytes;
	}

	protected void broadcastMessage(int stringId)
	{
		this.broadcastMessage(this.getContext().getResources().getString(stringId));
	}

	public File getPendingFolder()
	{
		SharedPreferences prefs = HttpUploadPlugin.getPreferences(this.getContext());

		File internalStorage = this.getContext().getFilesDir();

		if (prefs.getBoolean("config_external_storage", false))
			internalStorage = this.getContext().getExternalFilesDir(null);

		if (!internalStorage.exists())
			internalStorage.mkdirs();

		File pendingFolder = new File(internalStorage, CACHE_DIR);

		if (!pendingFolder.exists())
			pendingFolder.mkdirs();

		return pendingFolder;
	}

	public File getArchiveFolder()
	{
		File f = this.getPendingFolder();

		File archiveFolder = new File(f, "Archives");

		if (!archiveFolder.exists())
			archiveFolder.mkdirs();

		return archiveFolder;
	}

	private void persistJSONObject(final JSONObject jsonObject)
	{
		final SharedPreferences prefs = HttpUploadPlugin.getPreferences(this.getContext());

		long now = System.currentTimeMillis();

		if (jsonObject != null)
		{
			synchronized (this._pendingSaves)
			{
				this._pendingSaves.add(jsonObject.toString());
			}
		}

		if (now - this._lastSave > this.savePeriod() || this._pendingSaves.size() > 1024)
		{
			this._lastSave = now;

			File pendingFolder = this.getPendingFolder();

			String filename = now + ".json";

			File f = new File(pendingFolder, filename);

			HashSet<String> toRemove = new HashSet<String>();
			HashSet<String> invalidRemove = new HashSet<String>();

			JSONArray saveArray = new JSONArray();

			synchronized (this._pendingSaves)
			{
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
			}

			try
			{
				SecretKeySpec secretKey = this.keyForCipher(HttpUploadPlugin.CRYPTO_ALGORITHM);
				IvParameterSpec ivParameterSpec = new IvParameterSpec(this.getIVBytes());

				Cipher cipher = new NullCipher();

				if (prefs.getBoolean("config_http_encrypt", true))
				{
					cipher = Cipher.getInstance(HttpUploadPlugin.CRYPTO_ALGORITHM);
					cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
				}

				CipherOutputStream cout = new CipherOutputStream(new FileOutputStream(f), cipher);

				byte[] jsonBytes = saveArray.toString().getBytes("UTF-8");

				cout.write(jsonBytes);

				cout.flush();
				cout.close();

				synchronized (this._pendingSaves)
				{
					this._pendingSaves.removeAll(toRemove);
				}
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (UnsupportedEncodingException e)
			{
				throw new RuntimeException(e);
			}
			catch (OutOfMemoryError e)
			{
				e.printStackTrace();
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
			catch (InvalidAlgorithmParameterException e)
			{
				throw new RuntimeException(e);
			}

			if (this._pendingSaves.size() > 512)
			{
				this._lastSave = 0;
				this._failCount = 0;

				final HttpUploadPlugin me = this;

				Runnable r = new Runnable()
				{
					public void run()
					{
						me.persistJSONObject(null);
					}
				};

				Thread t = new Thread(r);
				t.start();
			}
		}
	}

	public void mailArchiveFiles(final Activity activity)
	{
		activity.runOnUiThread(new Runnable()
		{
			public void run()
			{
				Toast.makeText(activity, "Packaging archive files for mailing...", Toast.LENGTH_LONG).show();
			}
		});

		final HttpUploadPlugin me = this;

		Runnable r = new Runnable()
		{
			public void run()
			{
				File storage = activity.getExternalCacheDir();

				if (!storage.exists())
					storage.mkdirs();

				File pendingFolder = me.getArchiveFolder();

				final File[] pendingFiles = pendingFolder.listFiles(new FileFilter()
				{
					public boolean accept(File file)
					{
						if (file.getName().toLowerCase().endsWith(".archive"))
							return true;

						return false;
					}
				});

				final File zipfile = new File(storage, "archives.zip");

				final ArrayList<File> toDelete = new ArrayList<File>();

				try
				{
					ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipfile));

					int totalWritten = 0;

					for (int i = 0; i < pendingFiles.length && totalWritten < (MAX_UPLOAD_SIZE * 5 * 4); i++)
					{
						File f = pendingFiles[i];

						String filename = f.getName();
						FileInputStream fin = new FileInputStream(f);

						ZipEntry entry = new ZipEntry(filename);
						zout.putNextEntry(entry);

						byte[] buffer = new byte[2048];
						int read = 0;

						while ((read = fin.read(buffer, 0, buffer.length)) != -1)
						{
							zout.write(buffer, 0, read);
							totalWritten += read;
						}

						fin.close();

						zout.closeEntry();

						toDelete.add(f);
					}

					zout.close();
				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

				activity.runOnUiThread(new Runnable()
				{
					public void run()
					{
					    AccountManager accountManager = AccountManager.get(activity);

					    String email = null;

					    Account[] accounts = accountManager.getAccountsByType("com.google");

					    for (int i = 0; i < accounts.length && email == null; i++)
					    {
					    	Account account = accounts[i];

					    	email = account.name;
					    }

						Uri fileUri = Uri.fromFile(zipfile);

						Intent sendIntent = new Intent(Intent.ACTION_SEND);
				        sendIntent.setType("application/zip");
				        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Purple Robot Archives");
				        sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);

				        if (email != null)
				        {
				        	String[] emails = { email };
				        	sendIntent.putExtra(Intent.EXTRA_EMAIL, emails);
				        }

				        activity.startActivity(sendIntent);

				        int remaining = pendingFiles.length - toDelete.size();

				        Toast.makeText(activity, toDelete.size() + " archives packaged, " + remaining + " left in the device.", Toast.LENGTH_LONG).show();

				        for (File f : toDelete)
				        {
				        	f.delete();
				        }
					}
				});

			}
		};

		Thread t = new Thread(r);
		t.start();
	}

	public void deleteArchiveFiles(final Activity activity)
	{
		final HttpUploadPlugin me = this;

		Runnable r = new Runnable()
		{
			public void run()
			{
				File pendingFolder = me.getArchiveFolder();

				final File[] pendingFiles = pendingFolder.listFiles(new FileFilter()
				{
					public boolean accept(File file)
					{
						if (file.getName().toLowerCase().endsWith(".archive"))
							return true;

						return false;
					}
				});

				activity.runOnUiThread(new Runnable()
				{
					public void run()
					{
				        Toast.makeText(activity, "Deleting " + pendingFiles.length + " archive files...", Toast.LENGTH_LONG).show();
					}
				});

				for (File f : pendingFiles)
				{
					f.delete();
				}
			}
		};

		Thread t = new Thread(r);
		t.start();
	}
}
