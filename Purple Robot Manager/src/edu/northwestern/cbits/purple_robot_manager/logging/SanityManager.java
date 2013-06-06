package edu.northwestern.cbits.purple_robot_manager.logging;

import java.util.HashMap;
import java.util.Map;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import edu.northwestern.cbits.purple_robot_manager.ManagerService;
import edu.northwestern.cbits.purple_robot_manager.R;

public class SanityManager 
{
	private static SanityManager _sharedInstance = null;
	
	private Context _context = null;
	
	private HashMap<String, String> _errors = new HashMap<String, String>();
	private HashMap<String, String> _warnings = new HashMap<String, String>();
	
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
	
	@SuppressWarnings("rawtypes")
	public void refreshState() 
	{
		this._errors.clear();
		this._warnings.clear();
		
		String packageName = this.getClass().getPackage().getName();
		
		String[] checkClasses = this._context.getResources().getStringArray(R.array.sanity_check_classes);

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
						this._errors.put(check.name(this._context), check.getErrorMessage());
					else if (error == SanityCheck.WARNING)
						this._warnings.put(check.name(this._context), check.getErrorMessage());
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
