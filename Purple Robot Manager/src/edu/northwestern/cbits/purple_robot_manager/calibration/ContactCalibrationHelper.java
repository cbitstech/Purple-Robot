package edu.northwestern.cbits.purple_robot_manager.calibration;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberUtils;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.probes.AddressBookLabelActivity;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;

public class ContactCalibrationHelper 
{
	public static void check(final Context context) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

		if (prefs.contains("last_address_book_calibration") == false)
		{
			final SanityManager sanity = SanityManager.getInstance(context);

			final String title = context.getString(R.string.title_address_book_label_check);
			String message = context.getString(R.string.message_address_book_label_check);
				
			Runnable action = new Runnable()
			{
				public void run() 
				{
					Intent intent = new Intent(context, AddressBookLabelActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					
					context.startActivity(intent);
				}
			};
				
			sanity.addAlert(SanityCheck.WARNING, title, message, action);
		}
	}

	public static String getGroup(Context context, String key, boolean isPhone) 
	{
		if (key == null)
			return null;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		String group = prefs.getString("contact_calibration_" + key + "_group", null);
		
		if (group != null)
			return group;

		if (isPhone)
		{
			String numbersOnly = key.replaceAll("[^\\d]", "");
			
			if (numbersOnly.length() == 10)
				numbersOnly = "1" + numbersOnly;
			else if (numbersOnly.length() == 11)
				numbersOnly = numbersOnly.substring(1);
			
			key = PhoneNumberUtils.formatNumber(numbersOnly);
		}
		
		return prefs.getString("contact_calibration_" + key + "_group", null);
	}

	public static void setGroup(Context context, String key, String group)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putString("contact_calibration_" + key + "_group", group);
		e.commit();
	}

    public static List<ContactRecord> fetchContactRecords(Context context)
    {
    	ArrayList<ContactRecord> contacts = new ArrayList<ContactRecord>();
    	
		Cursor c = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, null);

		while (c.moveToNext())
		{
			String numberName = c.getString(c.getColumnIndex(Calls.CACHED_NAME));
			String phoneNumber = PhoneNumberUtils.formatNumber(c.getString(c.getColumnIndex(Calls.NUMBER)));

			boolean found = false;
			
			if (numberName == null)
				numberName = "";
			
			for (ContactRecord contact : contacts)
			{
				if (contact.number.endsWith(phoneNumber) || phoneNumber.endsWith(contact.number))
				{
					String largerNumber = contact.number;
					
					if (phoneNumber.length() > largerNumber.length())
						largerNumber = phoneNumber;
					
					contact.number = largerNumber;
					
					found = true;
					contact.count += 1;
					
					if ("".equals(numberName) == false && "".equals(contact.name))
						contact.name = numberName;
				}
			}
			
			if (found == false)
			{
				ContactRecord contact = new ContactRecord();
				contact.name = numberName;
				contact.number = phoneNumber;
				
				String key = contact.name;
				
				boolean isPhone = false;
				
				if ("".equals(key))
				{
					key = contact.number;
					isPhone = true;
				}
				
				String group = ContactCalibrationHelper.getGroup(context, key, isPhone);
				
				if (group != null)
					contact.group = group;
				
				contacts.add(contact);
			}
		}
		
		Collections.sort(contacts);

		ArrayList<ContactRecord> normalizedContacts = new ArrayList<ContactRecord>();
		
		for (ContactRecord contact : contacts)
		{
			if ("".equals(contact.name) == false)
			{
				boolean found = false;
				
				for (ContactRecord normalized : normalizedContacts)
				{
					if (contact.name.equals(normalized.name))
					{
						found = true;
						
						normalized.count += contact.count;
					}
				}
				
				if (found == false)
					normalizedContacts.add(contact);
			}
			else
				normalizedContacts.add(contact);
		}
		
		Collections.sort(normalizedContacts);
		
    	return normalizedContacts;
    }
}
