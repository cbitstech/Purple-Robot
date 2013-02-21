package edu.northwestern.cbits.purple.notifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockListActivity 
{
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

        this.getSupportActionBar().setTitle(R.string.title_widget_identifiers);
        this.setContentView(R.layout.layout_main_list);
    }
	
	protected void onResume()
	{
		super.onResume();
		
		this.refreshList();
	}
	
	private void refreshList()
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		Set<String> idSet = prefs.getStringSet("saved_identifiers", new HashSet<String>());
		
		final ArrayList<String> identifiers = new ArrayList<String>();
		final MainActivity me = this;
		
		for (String identifier : idSet)
		{
			identifiers.add(identifier);
		}
		
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.layout_identifier_row, identifiers.toArray(new String[0]))
        {
        	public View getView(final int position, View convertView, ViewGroup parent)
        	{
        		if (convertView == null)
        		{
        			LayoutInflater inflater = (LayoutInflater) me.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        			convertView = inflater.inflate(R.layout.layout_identifier_row, null);
        		}
        		
        		String identifier = identifiers.get(position);

        		TextView idField = (TextView) convertView.findViewById(R.id.identifier_value);
        		idField.setText(identifier);

        		return convertView;
        	}
        };
        
        this.getListView().setOnItemLongClickListener(new OnItemLongClickListener()
        {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id)
			{
				final String value = identifiers.get(position);

				AlertDialog.Builder alert = new AlertDialog.Builder(me);

    			alert.setTitle(R.string.prompt_remove_identifier);
    			alert.setMessage(String.format(me.getString(R.string.message_remove_identifier), value));

    			alert.setPositiveButton(R.string.button_remove, new DialogInterface.OnClickListener() 
    			{
    				public void onClick(DialogInterface dialog, int whichButton) 
    				{
    					HashSet<String> values = new HashSet<String>(prefs.getStringSet("saved_identifiers", new HashSet<String>()));
    					
    					values.remove(value);
    					
    					Editor e = prefs.edit();
    					e.putStringSet("saved_identifiers", values);
    					e.commit();
    					
    					me.refreshList();
    				}
    			});

    			alert.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() 
    			{
    				public void onClick(DialogInterface dialog, int whichButton) 
    				{

    				}
    			});

    			alert.show();
				
				return true;
			}
        });

        this.setListAdapter(adapter);
 	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		Log.e("PN", "CREATE CONTEXT MENU FOR " + menuInfo + " " + v.getClass());
	}
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
        MenuInflater inflater = this.getSupportMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        return true;
	}

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
    	{
    		case R.id.menu_add_item:
    			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    			final MainActivity me = this;

    			AlertDialog.Builder alert = new AlertDialog.Builder(this);

    			alert.setTitle(R.string.prompt_new_identifier);
    			alert.setMessage(R.string.message_new_identifier);

    			final EditText input = new EditText(this);
    			alert.setView(input);

    			alert.setPositiveButton(R.string.button_add, new DialogInterface.OnClickListener() 
    			{
    				public void onClick(DialogInterface dialog, int whichButton) 
    				{
    					String value = input.getText().toString();
    					
    					HashSet<String> values = new HashSet<String>(prefs.getStringSet("saved_identifiers", new HashSet<String>()));
    					
    					values.add(value);
    					
    					Editor e = prefs.edit();
    					e.putStringSet("saved_identifiers", values);
    					e.commit();
    					
    					me.refreshList();
    				}
    			});

    			alert.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() 
    			{
    				public void onClick(DialogInterface dialog, int whichButton) 
    				{

    				}
    			});

    			alert.show();
    			
    			break;
    		case R.id.menu_download_item:
    			Log.e("PN", "DOWNLOAD ITEMS");

    			break;
		}

    	return true;
    }
	
}
