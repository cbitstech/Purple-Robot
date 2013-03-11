package edu.northwestern.cbits.purple_robot_manager.scripting;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.SettingsActivity;
import edu.northwestern.cbits.purple_robot_manager.R.array;
import edu.northwestern.cbits.purple_robot_manager.config.LegacyJSONConfigFile;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public abstract class BaseScriptEngine 
{
	protected static String SCRIPT_ENGINE_PERSISTENCE_PREFIX = "purple_robot_script_persist_prefix_";

	protected Context _context = null;
	private static Map<String, String> packageMap = null;

	public BaseScriptEngine(Context context)
	{
		this._context = context;
	}

	protected abstract String language();
	
	public void log(String message)
	{
		Log.e("PRM." + this.language(), message);
	}
	
	public void playDefaultTone()
	{
		this.playTone(null);
	}
	
	public void playTone(String tone)
	{
		Intent intent = new Intent(ManagerService.RINGTONE_INTENT);

		if (tone != null)
			intent.putExtra(ManagerService.RINGTONE_NAME, tone);

		this._context.startService(intent);
	}

	public boolean persistEncryptedString(String key, String value)
	{
		key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;

		return EncryptionManager.getInstance().persistEncryptedString(this._context, key, value);
	}

	public String fetchEncryptedString(String key)
	{
		key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;
		
		return EncryptionManager.getInstance().fetchEncryptedString(this._context, key);
	}
	
	public void vibrate(String pattern)
	{
		Intent intent = new Intent(ManagerService.HAPTIC_PATTERN_INTENT);
		intent.putExtra(ManagerService.HAPTIC_PATTERN_NAME, pattern);

		this._context.startService(intent);
	}

	public String readUrl(String urlString)
	{
		try 
		{
			URL url = new URL(urlString);

	        URLConnection connection = url.openConnection();

	        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

	        StringBuilder response = new StringBuilder();

	        String inputLine;

	        while ((inputLine = in.readLine()) != null) 
	            response.append(inputLine);

	        in.close();

	        return response.toString();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		} 
			
		return null;
	}
	
	
	public boolean emitToast(final String message, final boolean longDuration)
	{
		if (this._context instanceof Activity)
		{
			final Activity activity = (Activity) this._context;

			activity.runOnUiThread(new Runnable()
			{
				public void run()
				{
					if (longDuration)
						Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
					else
						Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
				}
			});

			return true;
		}

		return false;
	}

	public boolean launchUrl(String urlString)
	{
		try
		{
			Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
			launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			this._context.startActivity(launchIntent);

			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}

	public String packageForApplicationName(String applicationName)
	{
		if (applicationName == null)
			return null;

		if (BaseScriptEngine.packageMap == null)
		{
			BaseScriptEngine.packageMap = new HashMap<String, String>();

			String[] keyArray = this._context.getResources().getStringArray(R.array.app_package_keys);
			String[] valueArray = this._context.getResources().getStringArray(R.array.app_package_values);

			if (keyArray.length == valueArray.length)
			{
				for (int i = 0; i < keyArray.length; i++)
				{
					BaseScriptEngine.packageMap.put(keyArray[i].toLowerCase(), valueArray[i]);
				}
			}
		}

		String packageName = BaseScriptEngine.packageMap.get(applicationName.toLowerCase());

		if (packageName == null)
			packageName = applicationName; // Allows us to launch by package name as well.

		PackageManager pkgManager = this._context.getPackageManager();

		Intent launchIntent = pkgManager.getLaunchIntentForPackage(packageName);

		if (launchIntent == null) // No matching package found on system...

			packageName = null;

		return packageName;
	}
	
	public String version()
	{
		try
		{
			PackageInfo info = this._context.getPackageManager().getPackageInfo(this._context.getPackageName(), 0);

			return info.versionName;
		}
		catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}

		return null;
	}

	public int versionCode()
	{
		try
		{
			PackageInfo info = this._context.getPackageManager().getPackageInfo(this._context.getPackageName(), 0);

			return info.versionCode;
		}
		catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}

		return -1;
	}

	public boolean persistString(String key, String value)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		Editor editor = prefs.edit();

		key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;

		if (value != null)
			editor.putString(key,  value.toString());
		else
			editor.remove(key);

		return editor.commit();
	}

	public String fetchString(String key)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

		key = SCRIPT_ENGINE_PERSISTENCE_PREFIX + key;

		return prefs.getString(key, null);
	}

	public void resetTrigger(String triggerId)
	{
		for (Trigger trigger : TriggerManager.getInstance().triggersForId(triggerId))
		{
			trigger.reset(this._context);
		}
	}

	public void enableTrigger(String triggerId)
	{
		for (Trigger trigger : TriggerManager.getInstance().triggersForId(triggerId))
		{
			trigger.setEnabled(true);
		}
	}

	public void disableTrigger(String triggerId)
	{
		for (Trigger trigger : TriggerManager.getInstance().triggersForId(triggerId))
		{
			trigger.setEnabled(false);
		}
	}
	
	public void disableAutoConfigUpdates(String triggerId)
	{
		// TODO
	}

	public void enableAutoConfigUpdates(String triggerId)
	{
		// TODO
	}

	public void enableProbes()
	{
		ProbeManager.enableProbes(this._context);
	}

	public void disableProbes()
	{
		ProbeManager.disableProbes(this._context);
	}
	
	public boolean probesState()
	{
		return ProbeManager.probesState(this._context);
	}
	
	protected void transmitData(Bundle data)
	{
		UUID uuid = UUID.randomUUID();
		data.putString("GUID", uuid.toString());

		LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this._context);
		Intent intent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
		intent.putExtras(data);

		localManager.sendBroadcast(intent);
	}
	
	public void disableProbe(String probeName)
	{
		ProbeManager.disableProbe(this._context, probeName);
	}
	
	public void updateConfigUrl(String newUrl)
	{
		if (newUrl.trim().length() == 0)
			newUrl = null;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);

		Editor e = prefs.edit();
		
		e.putString(LegacyJSONConfigFile.JSON_LAST_HASH, "");
		e.putLong(LegacyJSONConfigFile.JSON_LAST_UPDATE, 0);
		
		if (newUrl != null)
			e.putString(LegacyJSONConfigFile.JSON_CONFIGURATION_URL, newUrl);
		else
			e.remove(LegacyJSONConfigFile.JSON_CONFIGURATION_URL);
		
		e.commit();
		
		LegacyJSONConfigFile.update(this._context);
	}
	
	public void setPassword(String password)
	{
		if (password == null || password.trim().length() == 0)
			this.clearPassword();
		else
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
			
			Editor e = prefs.edit();
			e.putString("config_password", password);
			e.commit();
		}
	}
	
	public void clearPassword()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		Editor e = prefs.edit();
		e.remove("config_password");
		e.commit();
	}

	public void enableBackgroundImage()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_show_background", true);
		e.commit();
	}

	public void disableBackgroundImage()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_show_background", false);
		e.commit();
	}
	
	private void refreshConfigUrl()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		Editor editor = prefs.edit();

		editor.putLong(LegacyJSONConfigFile.JSON_LAST_UPDATE, 0);
		editor.commit();
		LegacyJSONConfigFile.update(this._context);

		ProbeManager.nudgeProbes(this._context);
	}

	public void setUserId(String userId)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		Editor e = prefs.edit();
		e.putString("config_user_id", userId);
		
		e.commit();
		
		this.refreshConfigUrl();
	}
	
	public void restoreDefaultId()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		Editor e = prefs.edit();
		e.remove("config_user_id");
		
		e.commit();

		this.refreshConfigUrl();
	}

	public void enableUpdateChecks()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		Editor e = prefs.edit();
		e.putBoolean(SettingsActivity.CHECK_UPDATES_KEY, true);
		e.commit();
	}

	public void disableUpdateChecks()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		Editor e = prefs.edit();
		e.putBoolean(SettingsActivity.CHECK_UPDATES_KEY, false);
		e.commit();
	}
}
