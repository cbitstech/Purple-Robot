package edu.northwestern.cbits.purple.notifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class IdentifiersManager 
{
	public static String[] fetchIdentifiers(Context context)
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		String idList = prefs.getString("saved_identifiers", "");
		
		String[] identifiers = new String[0];
		
		if (idList.trim().length() > 0)
			identifiers = idList.trim().split(";");
		
		return identifiers;
	}

	public static void putIndentifiers(Context context, String[] identifiers) 
	{   
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < identifiers.length; i++)
		{
			if (sb.length() > 0)
				sb.append(";");
			
			sb.append(identifiers[i]);
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putString("saved_identifiers", sb.toString());
		e.commit();
	}
}

