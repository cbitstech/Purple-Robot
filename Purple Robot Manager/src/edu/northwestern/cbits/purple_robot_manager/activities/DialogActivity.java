package edu.northwestern.cbits.purple_robot_manager.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;

public class DialogActivity extends Activity 
{
	public static String DIALOG_MESSAGE = "dialog_message";
	public static String DIALOG_TITLE = "dialog_title";
	public static String DIALOG_CONFIRM_BUTTON = "dialog_confirm";
	public static String DIALOG_CANCEL_BUTTON= "dialog_cancel";

	public static String DIALOG_CONFIRM_SCRIPT = "dialog_confirm_script";
	public static String DIALOG_CANCEL_SCRIPT = "dialog_cancel_script";
	
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_dialog_background_activity);
    }
	
	protected void onResume()
	{
		super.onResume();
		
		Intent intent = this.getIntent();
		
		String title = intent.getStringExtra(DialogActivity.DIALOG_TITLE);
		String message = intent.getStringExtra(DialogActivity.DIALOG_MESSAGE);

		String confirmTitle = intent.getStringExtra(DialogActivity.DIALOG_CONFIRM_BUTTON);
		final String confirmScript = intent.getStringExtra(DialogActivity.DIALOG_CONFIRM_SCRIPT);
		
		final Activity me = this;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder = builder.setTitle(title);
		builder = builder.setMessage(message);
		builder = builder.setCancelable(false);
		builder = builder.setPositiveButton(confirmTitle, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int which) 
			{
				if (confirmScript != null && confirmScript.trim().length() > 0)
				{
					Runnable r = new Runnable()
					{
						public void run() 
						{
							try 
							{
								Thread.sleep(500);
							} 
							catch (InterruptedException e) 
							{
								e.printStackTrace();
							}

							BaseScriptEngine.runScript(me, confirmScript);
						}
					};
					
					Thread t = new Thread(r);
					t.start();
				}
			}
		});

		String cancelTitle = intent.getStringExtra(DialogActivity.DIALOG_CANCEL_BUTTON);
		final String cancelScript = intent.getStringExtra(DialogActivity.DIALOG_CANCEL_SCRIPT);

		if (cancelTitle != null && cancelTitle.trim().length() > 0)
		{
			builder = builder.setNegativeButton(cancelTitle, new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int which) 
				{
					if (cancelScript != null && cancelScript.trim().length() > 0)
					{
						Runnable r = new Runnable()
						{
							public void run() 
							{
								try 
								{
									Thread.sleep(500);
								} 
								catch (InterruptedException e) 
								{
									e.printStackTrace();
								}
	
								BaseScriptEngine.runScript(me, cancelScript);
							}
						};
						
						Thread t = new Thread(r);
						t.start();
					}
				}
			});
		}
		
		builder = builder.setOnDismissListener(new OnDismissListener()
		{
			public void onDismiss(DialogInterface arg0) 
			{
				me.finish();
			}
		});
		
		builder.create().show();
	}
}
