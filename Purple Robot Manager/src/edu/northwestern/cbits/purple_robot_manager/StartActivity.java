package edu.northwestern.cbits.purple_robot_manager;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.WazaBe.HoloEverywhere.app.AlertDialog;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.graphics.Shader;

public class StartActivity extends SherlockActivity
{
	private void launchPreferences()
	{
		Intent intent = new Intent();
		intent.setClass(this, SettingsActivity.class);

		this.startActivity(intent);
	}

	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_startup_activity);

        Bitmap bmp = BitmapFactory.decodeResource(this.getResources(), R.drawable.tile_background);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(bmp);
        bitmapDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

        LinearLayout layout = (LinearLayout) this.findViewById(R.id.layout_start_root);
        layout.setBackgroundDrawable(bitmapDrawable);

        ManagerService.setupPeriodicCheck(this);
    }

	private void setJsonUri(Uri jsonConfigUri)
	{
		final StartActivity me = this;

		if (jsonConfigUri.getScheme().equals("cbits-prm"))
		{
			Uri.Builder b = jsonConfigUri.buildUpon();

			b.scheme("http");

			jsonConfigUri = b.build();
		}

		// TODO: Add support for HTTPS?

		final Uri finalUri = jsonConfigUri;

		JSONConfigFile.validateUri(this, jsonConfigUri, true, new Runnable()
		{
			public void run()
			{
				// Success

				me.runOnUiThread(new Runnable()
				{
					public void run()
					{
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(me);
						Editor editor = prefs.edit();

						editor.putString("config_json_url", finalUri.toString());
						editor.commit();

						Toast.makeText(me, R.string.success_json_set_uri, Toast.LENGTH_LONG).show();
					}
				});
			}
		}, new Runnable()
		{
			public void run()
			{
				me.runOnUiThread(new Runnable()
				{
					public void run()
					{
						// Failure

						Toast.makeText(me, R.string.error_json_set_uri, Toast.LENGTH_LONG).show();
					}
				});
			}
		});
	}

	protected void onResume()
	{
		super.onResume();

		Uri jsonConfigUri = this.getIntent().getData();

        final String savedPassword = PreferenceManager.getDefaultSharedPreferences(this).getString("config_password", null);

        if (jsonConfigUri != null)
        {
        	if (savedPassword == null || savedPassword.equals(""))
        		this.setJsonUri(jsonConfigUri);
        	else
        		Toast.makeText(this, R.string.error_json_set_uri_password, Toast.LENGTH_LONG).show();
        }
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
    		case R.id.menu_settings_item:
    	        final String savedPassword = PreferenceManager.getDefaultSharedPreferences(this).getString("config_password", null);

    	        final StartActivity me = this;

				if (savedPassword == null || savedPassword.equals(""))
					this.launchPreferences();
				else
				{
	    	        AlertDialog.Builder builder = new AlertDialog.Builder(this);

	    	        builder.setMessage(R.string.dialog_password_prompt);
	    	        builder.setPositiveButton(R.string.dialog_password_submit, new DialogInterface.OnClickListener()
	    	        {
						public void onClick(DialogInterface dialog, int which)
						{
							AlertDialog alertDialog = (AlertDialog) dialog;

			    	        final EditText passwordField = (EditText) alertDialog.findViewById(R.id.text_dialog_password);

							Editable password = passwordField.getText();

							if (password.toString().equals(savedPassword))
								me.launchPreferences();
							else
								Toast.makeText(me, R.string.toast_incorrect_password, Toast.LENGTH_LONG).show();

							alertDialog.dismiss();
						}
					});
	    	        builder.setView(me.getLayoutInflater().inflate(R.layout.dialog_password, null));

	    	        AlertDialog alert = builder.create();

	    	        alert.show();
				}

    	        break;
		}

    	return true;
    }
}
