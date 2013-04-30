package edu.northwestern.cbits.purple_robot_manager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class PurpleRobotApplication extends Application
{
    private static Context _context;

    public void onCreate()
    {
        super.onCreate();

        PurpleRobotApplication._context = this.getApplicationContext();
    }

    public static Context getAppContext()
    {
        return PurpleRobotApplication._context;
    }
    
    public static boolean updateFromMap(Context context, Map<String, Object> config)
    {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
		Editor e = prefs.edit();
		
		for (String key : config.keySet())
		{
			Object value = config.get(key);
			
			if (value instanceof String)
				e.putString(key, value.toString());
			else if (value instanceof Boolean)
				e.putBoolean(key, ((Boolean) value).booleanValue());
		}
		
		boolean success = e.commit();
		
		return success;
    }

	public static Map<String, Object> configuration(Context context) 
	{
		Map<String, Object> map = new HashMap<String, Object>();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		try 
		{
	        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();

	        factory.setNamespaceAware(false);

	        XmlPullParser xpp = context.getResources().getXml(R.layout.layout_settings_activity);
	        int eventType = xpp.getEventType();

	        while (eventType != XmlPullParser.END_DOCUMENT) 
	        {
	        	if (eventType == XmlPullParser.START_TAG) 
	        	{
	        		String name = xpp.getName();
	        		String key = xpp.getAttributeValue("http://schemas.android.com/apk/res/android", "key");
        			
        			if (prefs.contains(key))
        			{
        				if ("EditTextPreference".equals(name))
        					map.put(key, prefs.getString(key, null));
        				else if ("ListPreference".equals(name))
        					map.put(key, prefs.getString(key, null));
        				else if ("CheckBoxPreference".equals(name))
        					map.put(key, prefs.getBoolean(key, false));
        				else if ("Preference".equals(name))
        					map.put(key, prefs.getString(key, null));
        			}
	        	} 

	        	eventType = xpp.next();
	        }
		}
		catch (XmlPullParserException e) 
		{
			LogManager.getInstance(context).logException(e);
		} 
		catch (IOException e) 
		{
			LogManager.getInstance(context).logException(e);
		}
		
		map.put("config_probes_enabled", Boolean.valueOf(prefs.getBoolean("config_probes_enabled", false)));
		
		return map;
	}
}


