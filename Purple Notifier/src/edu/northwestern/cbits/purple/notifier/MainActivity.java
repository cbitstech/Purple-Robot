package edu.northwestern.cbits.purple.notifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
	private String _contextLabel = null;
	
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

        this.getSupportActionBar().setTitle(R.string.title_widget_identifiers);
        this.setContentView(R.layout.layout_main_list);
    }
	
	protected void onResume()
	{
		super.onResume();

        Intent intent = new Intent(WidgetIntentService.ACTION_BOOT);
		this.startService(intent);

		this.refreshList();
	}
	
	public boolean onContextItemSelected(android.view.MenuItem item)
	{
		final MainActivity me = this;

		switch (item.getItemId())
    	{
    		case R.id.menu_update_item:
    			Intent intent = new Intent();
    			intent.setClass(this, UpdateWidgetActivity.class);
    			intent.putExtra("identifier", this._contextLabel);

    			this.startActivity(intent);

    			break;

    		case R.id.menu_remove_item:
				AlertDialog.Builder alert = new AlertDialog.Builder(this);

    			alert.setTitle(R.string.prompt_remove_identifier);
    			alert.setMessage(String.format(this.getString(R.string.message_remove_identifier), this._contextLabel));

    			alert.setPositiveButton(R.string.button_remove, new DialogInterface.OnClickListener() 
    			{
    				public void onClick(DialogInterface dialog, int whichButton) 
    				{
    					String[] savedValues = IdentifiersManager.fetchIdentifiers(me);
    					
    					ArrayList<String> values = new ArrayList<String>();
    					
    					for (int i = 0; i < savedValues.length; i++)
    						values.add(savedValues[i]);

    					values.remove(me._contextLabel);

    					IdentifiersManager.putIndentifiers(me, values.toArray(new String[0]));
    					
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
    	}
		
		return true;
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		
		TextView labelView = (TextView) v.findViewById(R.id.identifier_value);
		
		this._contextLabel = labelView.getText().toString();

		android.view.MenuInflater inflater = this.getMenuInflater();
		
		inflater.inflate(R.menu.menu_label_item, menu);
	}

	private void refreshList()
	{
		final MainActivity me = this;
		
		final String[] finalList = IdentifiersManager.fetchIdentifiers(this);
		
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.layout_identifier_row, finalList)
        {
        	public View getView(final int position, View convertView, ViewGroup parent)
        	{
        		if (convertView == null)
        		{
        			LayoutInflater inflater = (LayoutInflater) me.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        			convertView = inflater.inflate(R.layout.layout_identifier_row, null);
        		}
        		
        		String identifier = finalList[position];

        		TextView idField = (TextView) convertView.findViewById(R.id.identifier_value);
        		idField.setText(identifier);
        		
        		me.unregisterForContextMenu(convertView);

        		me.registerForContextMenu(convertView);
        		convertView.setOnCreateContextMenuListener(me);

        		return convertView;
        	}
        };
        
        this.setListAdapter(adapter);
 	}
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
        MenuInflater inflater = this.getSupportMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        return true;
	}

    public boolean onOptionsItemSelected(MenuItem item)
    {
		final MainActivity me = this;

		switch (item.getItemId())
    	{
    		case R.id.menu_add_item:

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
    					
    					String[] savedValues = IdentifiersManager.fetchIdentifiers(me);
    					
    					ArrayList<String> values = new ArrayList<String>();
    					
    					for (int i = 0; i < savedValues.length; i++)
    						values.add(savedValues[i]);
    					
    					values.add(value);
    					
    					IdentifiersManager.putIndentifiers(me, values.toArray(new String[0]));
    					
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
    			AlertDialog.Builder downloadAlert = new AlertDialog.Builder(this);

    			downloadAlert.setTitle(R.string.prompt_id_resource);
    			downloadAlert.setMessage(R.string.message_id_resource);

    			final EditText urlField = new EditText(this);
    			
    			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    			String lastUrl = prefs.getString("last_url", this.getString(R.string.default_url));
    			
    			urlField.setText(lastUrl);

    			downloadAlert.setView(urlField);

    			downloadAlert.setPositiveButton(R.string.button_fetch, new DialogInterface.OnClickListener() 
    			{
    				public void onClick(DialogInterface dialog, int whichButton) 
    				{
    					Runnable r = new Runnable()
    					{
							public void run() 
							{
								
		    					try 
		    					{
			    					HttpClient client = new DefaultHttpClient();
			    					HttpGet request = new HttpGet();
		
			    					request.setURI(new URI(urlField.getEditableText().toString()));
			    					
			    					HttpResponse response = client.execute(request);
			    					
			    					InputStream in = response.getEntity().getContent();
			    					
			    					BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			    					
			    					final ArrayList<String> values = new ArrayList<String>();
			    					
			    					String line = null;
			    					while ((line = br.readLine()) != null)
			    					{
			    						line = line.trim();
			    						
			    						if (line.length() > 0 && values.contains(line) == false)
			    							values.add(line);
			    					}
			    					
			    					Editor e = prefs.edit();
			    					e.putString("last_url", urlField.getEditableText().toString());
			    					e.commit();
			    					
			    					me.runOnUiThread(new Runnable()
			    					{
										public void run() 
										{
											IdentifiersManager.putIndentifiers(me, values.toArray(new String[0]));
					    					
					    					me.refreshList();
										}
			    					});
								} 
		    					catch (URISyntaxException e) 
		    					{
			    					me.runOnUiThread(new Runnable()
			    					{
										public void run() 
										{
											Toast.makeText(me, R.string.error_fetch_exception, Toast.LENGTH_LONG).show();
										}
			    					});
								} 
		    					catch (ClientProtocolException e) 
		    					{
			    					me.runOnUiThread(new Runnable()
			    					{
										public void run() 
										{
											Toast.makeText(me, R.string.error_fetch_exception, Toast.LENGTH_LONG).show();
										}
			    					});
								}
		    					catch (IOException e) 
		    					{
			    					me.runOnUiThread(new Runnable()
			    					{
										public void run() 
										{
											Toast.makeText(me, R.string.error_unknown_exception, Toast.LENGTH_LONG).show();
										}
			    					});
								}
							}
    					};
    					
    					Thread t = new Thread(r);
    					t.start();
    				}
    			});

    			downloadAlert.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() 
    			{
    				public void onClick(DialogInterface dialog, int whichButton) 
    				{

    				}
    			});

    			downloadAlert.show();

    			break;
		}

    	return true;
    }
}
