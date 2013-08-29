package edu.northwestern.cbits.purple_robot_manager.activities.probes;

import java.util.ArrayList;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.support.v7.app.ActionBarActivity;
import android.telephony.PhoneNumberUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class AddressBookLabelActivity extends ActionBarActivity 
{
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_address_label_activity);
        this.getSupportActionBar().setTitle(R.string.title_address_book_label);
    }

	public static void check(final Context context) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
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
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_address_book_label_activity, menu);

        return true;
	}

    @SuppressLint("ValidFragment")
	public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
    	{
    		case R.id.menu_accept_label:
    			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    			Editor e = prefs.edit();
    			
    			e.putLong("last_address_book_calibration", System.currentTimeMillis());
    			e.commit();

    			this.finish();

    			final SanityManager sanity = SanityManager.getInstance(this);
    			final String title = this.getString(R.string.title_address_book_label_check);
    			sanity.clearAlert(title);

    			break;
    		case R.id.menu_cancel:
    			this.finish();

    			break;
    	}
        
        return true;
    }
    
    private class ContactCount implements Comparable<ContactCount>
    {
    	public int count = 0;
    	public String number = null;
    	public String name = null;

		public int compareTo(ContactCount other) 
		{
			if (this.count > other.count)
				return 1;
			else if (this.count < other.count)
				return -1;
					
			if (this.name != null)
				return this.name.compareTo(other.name);
			
			return this.number.compareTo(other.number);
		}
    }
    
    private List<ContactCount> fetchContactCounts()
    {
    	ArrayList<ContactCount> contacts = new ArrayList<ContactCount>();
    	
		Cursor c = this.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, null);

		while (c.moveToNext())
		{
			String numberName = c.getString(c.getColumnIndex(Calls.CACHED_NAME));
			String phoneNumber = PhoneNumberUtils.formatNumber(c.getString(c.getColumnIndex(Calls.NUMBER)));

			boolean found = false;
			
			for (ContactCount contact : contacts)
			{
				if (contact.number.equals(phoneNumber))
				{
					found = true;
					contact.count += 1;
					
					if (numberName != null && contact.name == null)
						contact.name = numberName;
				}
			}
			
			if (found == false)
			{
				ContactCount contact = new ContactCount();
				
				contacts.add(contact);
			}
		}
		
		Collections.sort(contacts);
		
    	return contacts;
    }
}
