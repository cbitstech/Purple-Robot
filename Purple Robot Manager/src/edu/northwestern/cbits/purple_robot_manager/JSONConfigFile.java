package edu.northwestern.cbits.purple_robot_manager;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class JSONConfigFile
{
	public static final String USER_ID = "user_id";
	public static final String USER_HASH = "user_hash";
	public static final String JSON_CONFIGURATION = "json_configuration_contents";
	public static final String JSON_PROBE_SETTINGS = "probe_settings";
	public static final String JSON_CONFIGURATION_URL = "config_json_url";
	public static final String JSON_LAST_UPDATE = "json_configuration_last_update";
	public static final String FEATURES = "features";

	private static SharedPreferences prefs = null;

	private JSONObject parameters = null;

	private static JSONConfigFile _sharedFile = null;
	private static Uri _configUri = null;

	public static void updateFromOnline(final Context context, final Uri uri, boolean force)
	{
		if (!uri.equals(JSONConfigFile._configUri))
			JSONConfigFile._sharedFile = null;

		if (JSONConfigFile._sharedFile == null || force)
		{
			Runnable r = new Runnable()
			{
				public void run()
				{
					if (uri != null)
					{
						try
						{
							JSONConfigFile._configUri = uri;

							URL u = new URL(JSONConfigFile._configUri.toString());

							HttpURLConnection conn = (HttpURLConnection) u.openConnection();

							BufferedInputStream bin = new BufferedInputStream(conn.getInputStream());
							ByteArrayOutputStream bout = new ByteArrayOutputStream();

							byte[] buffer = new byte[4096];
							int read = 0;

							while ((read = bin.read(buffer, 0, buffer.length)) != -1)
							{
								bout.write(buffer, 0, read);
							}

							bin.close();

							String jsonString = new String(bout.toByteArray(), "UTF-8");

							JSONObject json = new JSONObject(jsonString);

							SharedPreferences prefs = JSONConfigFile.getPreferences(context);
							Editor edit = prefs.edit();

							edit.putString(JSONConfigFile.JSON_CONFIGURATION, json.toString());
							edit.putString(JSONConfigFile.JSON_CONFIGURATION_URL, u.toString());

							edit.commit();
							
							if (context instanceof Activity)
							{
								final Activity activity = (Activity) context;

								activity.runOnUiThread(new Runnable()
								{
									public void run()
									{
										Toast.makeText(activity, R.string.success_json_set_uri, Toast.LENGTH_LONG).show();
									}
								});
							}
						}
						catch (MalformedURLException e)
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

						JSONConfigFile._sharedFile = new JSONConfigFile(context);
					}
				}
			};

			Thread t = new Thread(r);
			t.start();
		}
	}

	protected void updateTriggers(Context context) 
	{
		List<Trigger> triggerList = new ArrayList<Trigger>();

		try
		{
			JSONArray triggers = this.parameters.getJSONArray("triggers");

			for (int i = 0; triggers != null && i < triggers.length(); i++)
			{
				Trigger t = Trigger.parse(context, triggers.getJSONObject(i));

				if (t != null)
					triggerList.add(t);
			}
		}
		catch (JSONException e)
		{

		}
		
		TriggerManager.getInstance().updateTriggers(triggerList);
	}

	public static JSONConfigFile getSharedFile(Context context)
	{
		ProbeManager.allProbes(context);

		if (JSONConfigFile._sharedFile == null)
			JSONConfigFile._sharedFile = new JSONConfigFile(context);

		JSONConfigFile.update(context);

		return JSONConfigFile._sharedFile;
	}

	public String getStringParameter(String key)
	{
		try
		{
			if (this.parameters != null)
				return this.parameters.getString(key);
		}
		catch (JSONException e)
		{

		}

		return null;
	}

	private static SharedPreferences getPreferences(Context context)
	{
		if (JSONConfigFile.prefs == null)
			JSONConfigFile.prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

		return JSONConfigFile.prefs;
	}

	private JSONConfigFile(Context context)
	{
		SharedPreferences prefs = JSONConfigFile.getPreferences(context);

		try
		{
			this.parameters = new JSONObject(prefs.getString(JSONConfigFile.JSON_CONFIGURATION, "{}"));

			this.updateSharedPreferences(context);
			
			this.updateTriggers(context);
		}
		catch (JSONException e)
		{
			this.parameters = new JSONObject();
		}
	}

	private void updateSharedPreferences(Context context)
	{
		String userId = null;
		String userHash = null;

		try
		{
			if (this.parameters.has(JSONConfigFile.USER_ID))
				userId = this.parameters.getString(JSONConfigFile.USER_ID);

			if (this.parameters.has(JSONConfigFile.USER_HASH))
				userHash = this.parameters.getString(JSONConfigFile.USER_HASH);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		SharedPreferences prefs = JSONConfigFile.getPreferences(context);
		Editor editor = prefs.edit();

		if (userId == null)
			userId = prefs.getString("config_user_id", null);

		if (userId == null)
		{
			AccountManager manager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
			Account[] list = manager.getAccountsByType("com.google");

			if (list.length == 0)
				list = manager.getAccounts();

			if (list.length > 0)
			{
				userId = list[0].name;
			}
		}

		if (userId != null && userHash == null)
		{
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
		}

		if (userId != null)
			editor.putString("config_user_id", userId);

		if (userHash != null)
			editor.putString("config_user_hash", userHash);

		if (this.parameters.has(JSONConfigFile.JSON_PROBE_SETTINGS))
		{
			try
			{
				JSONArray probeSettings = this.parameters.getJSONArray(JSONConfigFile.JSON_PROBE_SETTINGS);

				ProbeManager.updateProbesFromJSON(context, probeSettings);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		if (this.parameters.has(JSONConfigFile.FEATURES))
		{
			ProbeManager.clearFeatures();

			try
			{
				JSONArray features = this.parameters.getJSONArray(JSONConfigFile.FEATURES);

				for (int i = 0; i < features.length(); i++)
				{
					JSONObject feature = features.getJSONObject(i);

					String name = context.getString(R.string.label_unknown_feature);
					String script = "";
					String formatter = "";

					if (feature.has("name"))
						name = feature.getString("name");

					if (feature.has("feature"))
						script = feature.getString("feature");

					if (feature.has("formatter"))
						formatter = feature.getString("formatter");

					ArrayList<String> sources = new ArrayList<String>();

					if (feature.has("sources"))
					{
						JSONArray sourceArray = feature.getJSONArray("sources");

						for (int j = 0; j < sourceArray.length(); j++)
						{
							String source = sourceArray.getString(j);

							sources.add(source);
						}
					}

					ProbeManager.addFeature(name, name, script, formatter, sources, false);
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		editor.commit();
	}

	public static void update(final Context context)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				SharedPreferences prefs = JSONConfigFile.getPreferences(context);

				long lastUpdate = prefs.getLong(JSONConfigFile.JSON_LAST_UPDATE, 0);

				long now = System.currentTimeMillis();

				int interval = Integer.parseInt(prefs.getString("config_json_refresh_interval", "60"));

				if (now - lastUpdate > 1000 * 60 * interval)
				{
					Editor edit = prefs.edit();

					boolean force = false;

					force = (lastUpdate == 0);

					String uriString = prefs.getString(JSONConfigFile.JSON_CONFIGURATION_URL, null);

					if (uriString != null)
						JSONConfigFile.updateFromOnline(context, Uri.parse(uriString), force);

					edit.putLong(JSONConfigFile.JSON_LAST_UPDATE, now);
					edit.commit();
					
					if (JSONConfigFile._sharedFile != null)
						JSONConfigFile._sharedFile.updateTriggers(context);
				}
			}
		};

		Thread t = new Thread(r);
		t.start();
	}

	protected String content()
	{
		return this.parameters.toString();
	}

	protected boolean isValid()
	{
		if (this.content().length() > Preferences.MAX_VALUE_LENGTH)
			return false;

		return true;
	}
}
