package edu.northwestern.cbits.purple_robot_manager.activities;

import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;

public class DialogActivity extends Activity
{
	public static String DIALOG_MESSAGE = "dialog_message";
	public static String DIALOG_TITLE = "dialog_title";
	public static String DIALOG_CONFIRM_BUTTON = "dialog_confirm";
	public static String DIALOG_CANCEL_BUTTON= "dialog_cancel";

	public static String DIALOG_CONFIRM_SCRIPT = "dialog_confirm_script";
	public static String DIALOG_CANCEL_SCRIPT = "dialog_cancel_script";
	
	private static ArrayList<DialogActivity> _dialogStack = new ArrayList<DialogActivity>();

	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_dialog_activity);

        this.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        int stackSize = Integer.parseInt(prefs.getString("config_dialog_count", "4"));
        
        DialogActivity._dialogStack.add(this);

        while (stackSize > 0 && DialogActivity._dialogStack.size() > stackSize)
        {
        	DialogActivity activity = DialogActivity._dialogStack.remove(0);
        	
        	activity.finish();
        }
    }
	
	protected void onDestroy ()
	{
		super.onDestroy();
		
		DialogActivity._dialogStack.remove(this);
	}
	
	protected void onPause()
	{
		super.onPause();

        Intent intent = this.getIntent();

        final String title = intent.getStringExtra(DialogActivity.DIALOG_TITLE);
        final String message = intent.getStringExtra(DialogActivity.DIALOG_MESSAGE);

        HashMap <String, Object> payload = new HashMap<String, Object>();
		payload.put("title", title);
		payload.put("message", message);
		
		LogManager.getInstance(this).log("dialog_dismissed", payload);
	}

	@SuppressLint("NewApi")
	protected void onResume()
	{
		super.onResume();

		final DialogActivity me = this;

        final TextView messageText = (TextView) this.findViewById(R.id.text_dialog_message);

        Intent intent = this.getIntent();

        final String title = intent.getStringExtra(DialogActivity.DIALOG_TITLE);
        final String message = intent.getStringExtra(DialogActivity.DIALOG_MESSAGE);

        final String confirmScript = intent.getStringExtra(DialogActivity.DIALOG_CONFIRM_SCRIPT);

        this.setTitle(title);

        messageText.setText(message);

        Button confirmButton = (Button) this.findViewById(R.id.button_dialog_confirm);
        confirmButton.setText(intent.getStringExtra(DialogActivity.DIALOG_CONFIRM_BUTTON));

		HashMap <String, Object> payload = new HashMap<String, Object>();
		payload.put("title", title);
		payload.put("message", message);
		
		LogManager.getInstance(me).log("dialog_shown", payload);

        confirmButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				HashMap <String, Object> payload = new HashMap<String, Object>();
				payload.put("title", title);
				payload.put("message", message);
				
				LogManager.getInstance(me).log("dialog_confirm", payload);
				
				if (confirmScript != null && confirmScript.length() >= 0)
				{
					try
					{
						BaseScriptEngine.runScript(me, confirmScript);
					}
					catch (Exception e)
					{
						LogManager.getInstance(me).logException(e);
					}
				}

				me.finish();
			}
        });

        Button cancelButton = (Button) this.findViewById(R.id.button_dialog_cancel);

        if (intent.hasExtra(DialogActivity.DIALOG_CANCEL_BUTTON))
        {
			cancelButton.setText(intent.getStringExtra(DialogActivity.DIALOG_CANCEL_BUTTON));

            if (intent.hasExtra(DialogActivity.DIALOG_CANCEL_SCRIPT))
            {
	            final String cancelScript = intent.getStringExtra(DialogActivity.DIALOG_CANCEL_SCRIPT);
	
	            cancelButton.setOnClickListener(new OnClickListener()
	            {
	    			public void onClick(View v)
	    			{
	    				HashMap <String, Object> payload = new HashMap<String, Object>();
	    				payload.put("title", title);
	    				payload.put("message", message);
	    				
	    				LogManager.getInstance(me).log("dialog_cancel", payload);

	    				if (cancelScript != null && cancelScript.length() >= 0)
	    				{
	    					try
	    					{
	    						BaseScriptEngine.runScript(me, cancelScript);
	    					}
	    					catch (Exception e)
	    					{
	    						LogManager.getInstance(me).logException(e);
	    					}
	    				}
	    				
	    				me.finish();
	    			}
	            });
            }

            cancelButton.setVisibility(View.VISIBLE);
        }
        else
            cancelButton.setVisibility(View.GONE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        	this.setFinishOnTouchOutside(false);
    }
}
