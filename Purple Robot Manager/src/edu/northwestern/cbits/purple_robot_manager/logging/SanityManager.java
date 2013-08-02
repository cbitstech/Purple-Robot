package edu.northwestern.cbits.purple_robot_manager.logging;

import java.util.HashMap;
import java.util.Map;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.StartActivity;

public class SanityManager 
{
	private static SanityManager _sharedInstance = null;
	
	private Context _context = null;
	
	private HashMap<String, String> _errors = new HashMap<String, String>();
	private HashMap<String, String> _warnings = new HashMap<String, String>();
	
	private int _lastStatus = -1;
	
	public SanityManager(Context context) 
	{
		this._context = context;
		
		AlarmManager alarms = (AlarmManager) this._context.getSystemService(Context.ALARM_SERVICE);
		
		Intent intent = new Intent(ManagerService.REFRESH_ERROR_STATE_INTENT);
		PendingIntent pending = PendingIntent.getService(this._context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarms.setInexactRepeating(AlarmManager.RTC, 0, 60000, pending);
	}

	public static SanityManager getInstance(Context context)
	{
		if (SanityManager._sharedInstance != null)
			return SanityManager._sharedInstance;
		
		if (context != null)
			SanityManager._sharedInstance = new SanityManager(context.getApplicationContext());
		
		return SanityManager._sharedInstance;
	}
	
	@SuppressWarnings({ "rawtypes", "deprecation" })
	public void refreshState() 
	{
		this._errors.clear();
		this._warnings.clear();
		
		String packageName = this.getClass().getPackage().getName();
		
		String[] checkClasses = this._context.getResources().getStringArray(R.array.sanity_check_classes);
		String title = null;

		for (String className : checkClasses)
		{
			Class checkClass = null;
			
			try
			{
				checkClass = Class.forName(packageName + "." + className);
			}
			catch (ClassNotFoundException e)
			{
				try 
				{
					checkClass = Class.forName(className);
				} 
				catch (ClassNotFoundException ee) 
				{
					LogManager.getInstance(this._context).logException(ee);
				}
			}
			
			if (checkClass != null)
			{
				try 
				{
					SanityCheck check = (SanityCheck) checkClass.newInstance();
					
					check.runCheck(this._context);
					
					int error = check.getErrorLevel();
					
					if (error == SanityCheck.ERROR)
					{
						title = check.getErrorMessage();
						this._errors.put(check.name(this._context), title);
					}
					else if (error == SanityCheck.WARNING)
					{
						if (title == null)
							title = check.getErrorMessage();

						this._warnings.put(check.name(this._context), check.getErrorMessage());
					}
				}
				catch (InstantiationException e) 
				{
					LogManager.getInstance(this._context).logException(e);
				} 
				catch (IllegalAccessException e) 
				{
					LogManager.getInstance(this._context).logException(e);
				}
				catch (ClassCastException e) 
				{
					LogManager.getInstance(this._context).logException(e);
				}
			}
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this._context);
		
		if (prefs.getBoolean("config_mute_warnings", false) == false && this.getErrorLevel() != this._lastStatus)
		{
			this._lastStatus = this.getErrorLevel();

			NotificationManager noteManager = (NotificationManager) this._context.getSystemService(Context.NOTIFICATION_SERVICE);

			int icon = R.drawable.ic_note_normal;
			
			if (this.getErrorLevel() == SanityCheck.ERROR)
				icon = R.drawable.ic_note_error;
			else if (this.getErrorLevel() == SanityCheck.WARNING)
				icon = R.drawable.ic_note_warning;
			
			if (title == null)
				title = this._context.getString(R.string.pr_errors_none_label);

			Notification note = new Notification(icon, title, System.currentTimeMillis());

			PendingIntent contentIntent = PendingIntent.getActivity(this._context, 0, new Intent(this._context, StartActivity.class), Notification.FLAG_ONGOING_EVENT);
			note.setLatestEventInfo(this._context, title, title, contentIntent);
			note.flags = Notification.FLAG_ONGOING_EVENT;

			noteManager.notify(12345, note);
		}
	}

	public int getErrorLevel() 
	{
		if (this._errors.size() > 0)
			return SanityCheck.ERROR;
		else if (this._warnings.size() > 0)
			return SanityCheck.WARNING;
		
		return SanityCheck.OK;
	}

	public int getErrorIconResource() 
	{
    	switch (this.getErrorLevel())
    	{
    		case SanityCheck.ERROR:
    			return R.drawable.action_error;
    		case SanityCheck.WARNING:
    			return R.drawable.action_warning;
    	}
    	
		return R.drawable.action_about;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, String> errors()
	{
		return (Map<String, String>) this._errors.clone();
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> warnings()
	{
		return (Map<String, String>) this._warnings.clone();
	}

}
