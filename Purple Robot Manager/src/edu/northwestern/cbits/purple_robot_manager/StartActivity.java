package edu.northwestern.cbits.purple_robot_manager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class StartActivity extends Activity
{
	private void launchPreferences()
	{
		Intent intent = new Intent();
		intent.setClass(this, SettingsActivity.class);

		this.startActivity(intent);

		this.finish();
	}

	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_startup_activity);

        ManagerService.setupPeriodicCheck(this);
    }

	private void setJsonUri(Uri jsonConfigUri)
	{
		final Activity me = this;

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

        final StartActivity me = this;

        final String savedPassword = PreferenceManager.getDefaultSharedPreferences(this).getString("config_password", null);

        final EditText passwordField = (EditText) this.findViewById(R.id.text_startup_password);

        Button exitButton = (Button) this.findViewById(R.id.button_exit);

        exitButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
//				Intent intent = new Intent(ManagerService.PERIODIC_CHECK_INTENT);
//				me.startService(intent);

//				me.finish();

		        JavaScriptEngine js = new JavaScriptEngine(me);
		        js.runScript("PurpleRobot.showNativeDialog('TITLE', 'MESSAGE', 'CONFIRM', 'CANCEL', function() { PurpleRobot.emitToast('CONFIRM!!!', false); }, function() { PurpleRobot.emitToast('CANCEL!!!', false); });");
			}
        });

        Button submitButton = (Button) this.findViewById(R.id.button_submit_password);

        submitButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				Editable password = passwordField.getText();

				if (savedPassword == null || savedPassword.equals("") || savedPassword.equals(password.toString()))
				{
					me.launchPreferences();
				}
				else
				{
					Toast.makeText(me, R.string.toast_incorrect_password, Toast.LENGTH_LONG).show();
				}
			}
        });

        if (jsonConfigUri != null)
        {
        	if (savedPassword == null || savedPassword.equals(""))
        		this.setJsonUri(jsonConfigUri);
        	else
        		Toast.makeText(this, R.string.error_json_set_uri_password, Toast.LENGTH_LONG).show();
        }
    }
}
