package edu.northwestern.cbits.purple_robot_manager;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;
import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;

public class JSONConfigFile
{
	public static String JSON_CONFIGURATION = "json_configuration_contents";
	public static String JSON_LAST_UPDATE = "json_configuration_last_update";

	private JSONObject parameters = null;
	private List<Trigger> _triggerList = null;

	private static JSONConfigFile _sharedFile = null;
	private static Uri _configUri = null;

	public static void updateFromOnline(Context context, Uri uri)
	{
		if (!uri.equals(JSONConfigFile._configUri))
			JSONConfigFile._sharedFile = null;

		if (JSONConfigFile._sharedFile == null)
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

				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
				Editor edit = prefs.edit();

				edit.putString(JSONConfigFile.JSON_CONFIGURATION, json.toString());

				edit.commit();
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

	public static JSONConfigFile getSharedFile(Context context)
	{
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

	private JSONConfigFile(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		try
		{
			this.parameters = new JSONObject(prefs.getString(JSONConfigFile.JSON_CONFIGURATION, "{}"));
		}
		catch (JSONException e)
		{
			this.parameters = new JSONObject();
		}
	}

	public static void update(Context context)
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		long lastUpdate = prefs.getLong(JSONConfigFile.JSON_LAST_UPDATE, 0);

		long now = System.currentTimeMillis();

		int interval = Integer.parseInt(prefs.getString("config_json_refresh_interval", "60"));

		if (now - lastUpdate > 1000 * 60 * interval)
		{
			Editor edit = prefs.edit();

			String uriString = prefs.getString("config_json_url", null);

			if (uriString != null)
				JSONConfigFile.updateFromOnline(context, Uri.parse(uriString));

			edit.putLong(JSONConfigFile.JSON_LAST_UPDATE, 0);
			edit.commit();
		}
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

	public List<Trigger> getTriggers(Context context)
	{
		if (this._triggerList == null)
		{
			this._triggerList = new ArrayList<Trigger>();

			try
			{
				JSONArray triggers = this.parameters.getJSONArray("triggers");

				for (int i = 0; triggers != null && i < triggers.length(); i++)
				{
					Trigger t = Trigger.parse(context, triggers.getJSONObject(i));

					if (t != null)
						this._triggerList.add(t);
				}
			}
			catch (JSONException e)
			{

			}
		}

		return this._triggerList;
	}
}
