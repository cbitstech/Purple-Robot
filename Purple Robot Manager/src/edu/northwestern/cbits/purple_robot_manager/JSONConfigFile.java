package edu.northwestern.cbits.purple_robot_manager;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

public class JSONConfigFile
{
	public static String JSON_CONFIGURATION = "json_configuration_contents";
	public static String JSON_LAST_UPDATE = "json_configuration_last_update";

	private JSONObject parameters = null;

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

	public JSONConfigFile(Uri uri) throws IOException, JSONException
	{
		Log.e("PRM", "JSON CONFIG URI: " + uri.toString());

		URL u = new URL(uri.toString());

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

		this.parameters = new JSONObject(jsonString);
	}

	public JSONConfigFile(Context context) throws JSONException
	{
		final JSONConfigFile me = this;

		this.parameters = new JSONObject();

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		String jsonString = prefs.getString(JSONConfigFile.JSON_CONFIGURATION, "{}");
		long lastUpdate = prefs.getLong(JSONConfigFile.JSON_LAST_UPDATE, 0);

		long now = System.currentTimeMillis();

		int interval = Integer.parseInt(prefs.getString("config_json_refresh_interval", "60"));

		if (now - lastUpdate > 1000 * 60 * interval)
		{
			String uriString = prefs.getString("config_json_url", null);

			if (uriString != null)
			{
				JSONConfigFile.validateUri(context, Uri.parse(uriString), true, new Runnable()
				{
					public void run()
					{
						try
						{
							String jsonString = prefs.getString(JSONConfigFile.JSON_CONFIGURATION, "{}");

							me.parameters = new JSONObject(jsonString);
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
					}
				}, null);
			}
		}
		else
			this.parameters = new JSONObject(jsonString);
	}

	public static void validateUri(final Context context, final Uri jsonConfigUri, final boolean saveIfValid, final Runnable success, final Runnable failure)
	{
		Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					JSONConfigFile file = new JSONConfigFile(jsonConfigUri);

					if (file.isValid())
					{
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
						Editor editor = prefs.edit();

						editor.putString(JSONConfigFile.JSON_CONFIGURATION, file.content());
						editor.putLong(JSONConfigFile.JSON_LAST_UPDATE, System.currentTimeMillis());
						editor.commit();

						if (success != null)
							success.run();

						return;
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				if (failure != null)
					failure.run();
			}
		});

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

	public List<Trigger> getTriggers()
	{
		List<Trigger> triggerList = new ArrayList<Trigger>();

		try
		{
			JSONArray triggers = this.parameters.getJSONArray("triggers");

			for (int i = 0; triggers != null && i < triggers.length(); i++)
			{
				Trigger t = Trigger.parse(triggers.getJSONObject(i));

				if (t != null)
					triggerList.add(t);
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		return triggerList;
	}
}
