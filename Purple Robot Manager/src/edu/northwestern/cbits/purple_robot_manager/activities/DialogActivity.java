package edu.northwestern.cbits.purple_robot_manager.activities;

import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
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
	
	private static ArrayList<HashMap<String, String>> _pendingDialogs = new ArrayList<HashMap<String, String>>();
	private static boolean _visible = false;
	private static AlertDialog _currentDialog = null;
	private static DialogActivity _currentActivity = null;
	
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_dialog_background_activity);
        
        DialogActivity._visible = true;
    }
	
	protected void onDestroy()
	{
		DialogActivity._visible = false;
		
		super.onDestroy();
	}
	
	public static void showNativeDialog(Context context, String title, String message, String confirmTitle, String cancelTitle, String confirmScript, String cancelScript)
	{
		if (title == null)
			title = "";

		if (message == null)
			message = "";
		
		if (confirmTitle == null)
			confirmTitle = "";

		if (confirmScript == null)
			confirmScript = "";

		if (cancelTitle == null)
			cancelTitle = "";

		if (cancelScript == null)
			cancelScript = "";

		HashMap<String, Object> payload = new HashMap<String, Object>();
		payload.put("title", title);
		payload.put("message", message);
		payload.put("confirmTitle", confirmTitle);
		payload.put("cancelTitle", cancelTitle);
		payload.put("cancelScript", cancelScript);
		payload.put("confirmTitle", confirmTitle);
		
		LogManager.getInstance(context).log("static_show_dialog", payload);

		if (DialogActivity._visible == false)
		{
			DialogActivity._visible = true;
			
			Intent intent = new Intent(context, DialogActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			
			intent.putExtra(DialogActivity.DIALOG_TITLE, title);
			intent.putExtra(DialogActivity.DIALOG_MESSAGE, message);
			intent.putExtra(DialogActivity.DIALOG_CONFIRM_BUTTON, confirmTitle);
			intent.putExtra(DialogActivity.DIALOG_CONFIRM_SCRIPT, confirmScript);
			intent.putExtra(DialogActivity.DIALOG_CANCEL_BUTTON, cancelTitle);
			intent.putExtra(DialogActivity.DIALOG_CANCEL_SCRIPT, cancelScript);

			context.startActivity(intent);
		}
		else
		{
			HashMap<String, String> dialog = new HashMap<String, String>();
			dialog.put(DialogActivity.DIALOG_TITLE, title);
			dialog.put(DialogActivity.DIALOG_MESSAGE, message);
			dialog.put(DialogActivity.DIALOG_CONFIRM_BUTTON, confirmTitle);
			dialog.put(DialogActivity.DIALOG_CONFIRM_SCRIPT, confirmScript);
			dialog.put(DialogActivity.DIALOG_CANCEL_BUTTON, cancelTitle);
			dialog.put(DialogActivity.DIALOG_CANCEL_SCRIPT, cancelScript);
			
			DialogActivity._pendingDialogs.add(dialog);
		}
	}
	
	protected void onResume()
	{
		super.onResume();
		
		this.showNativeDialog();
	}
		
	@SuppressLint("NewApi")
	private void showNativeDialog() 
	{
		Intent intent = this.getIntent();
		
		String title = intent.getStringExtra(DialogActivity.DIALOG_TITLE);
		String message = intent.getStringExtra(DialogActivity.DIALOG_MESSAGE);

		String confirmTitle = intent.getStringExtra(DialogActivity.DIALOG_CONFIRM_BUTTON);
		final String confirmScript = intent.getStringExtra(DialogActivity.DIALOG_CONFIRM_SCRIPT);
		
		final DialogActivity me = this;
		
		ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.Theme_AppCompat);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(wrapper);
		builder = builder.setTitle(title);
		builder = builder.setMessage(message);
		builder = builder.setCancelable(false);
		
		if (confirmTitle.trim().length() > 0)
		{
			builder = builder.setPositiveButton(confirmTitle, new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int which) 
				{
					HashMap<String, Object> payload = new HashMap<String, Object>();

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

						payload.put("confirmScript", confirmScript);
					}
					
					LogManager.getInstance(me).log("dialog_clicked_positive", payload);
				}
			});
		}
		
		String cancelTitle = intent.getStringExtra(DialogActivity.DIALOG_CANCEL_BUTTON);
		final String cancelScript = intent.getStringExtra(DialogActivity.DIALOG_CANCEL_SCRIPT);

		if (cancelTitle.trim().length() > 0)
		{
			builder = builder.setNegativeButton(cancelTitle, new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int which) 
				{
					HashMap<String, Object> payload = new HashMap<String, Object>();

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

						payload.put("cancelScript", cancelScript);
					}
					
					LogManager.getInstance(me).log("dialog_clicked_negative", payload);
				}
			});
		}
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
		{
			builder = builder.setOnDismissListener(new OnDismissListener()
			{
				public void onDismiss(DialogInterface arg0) 
				{
					DialogActivity._currentDialog = null;
					DialogActivity._currentActivity = null;
					
					if (DialogActivity._pendingDialogs.size() > 0)
					{
						HashMap<String, String> dialog = DialogActivity._pendingDialogs.remove(0);
						
						Intent intent = new Intent();
						intent.putExtra(DialogActivity.DIALOG_TITLE, dialog.get(DialogActivity.DIALOG_TITLE));
						intent.putExtra(DialogActivity.DIALOG_MESSAGE, dialog.get(DialogActivity.DIALOG_MESSAGE));
						intent.putExtra(DialogActivity.DIALOG_CONFIRM_BUTTON, dialog.get(DialogActivity.DIALOG_CONFIRM_BUTTON));
						intent.putExtra(DialogActivity.DIALOG_CONFIRM_SCRIPT, dialog.get(DialogActivity.DIALOG_CONFIRM_SCRIPT));
						intent.putExtra(DialogActivity.DIALOG_CANCEL_BUTTON, dialog.get(DialogActivity.DIALOG_CANCEL_BUTTON));
						intent.putExtra(DialogActivity.DIALOG_CANCEL_SCRIPT, dialog.get(DialogActivity.DIALOG_CANCEL_SCRIPT));
						
						me.setIntent(intent);
						
						me.showNativeDialog();
					}
					else
						me.finish();
				}
			});
		}
		
		DialogActivity._currentDialog = builder.create();
		DialogActivity._currentActivity  = this;
				
		DialogActivity._currentDialog.show();
	}

	public static void clearNativeDialogs() 
	{
		DialogActivity._pendingDialogs.clear();
		
		if (DialogActivity._currentDialog != null && DialogActivity._currentActivity != null)
		{
			LogManager.getInstance(DialogActivity._currentActivity).log("dialogs_cleared", null);

			DialogActivity._currentActivity.runOnUiThread(new Runnable()
			{
				public void run() 
				{
					DialogActivity._currentDialog.dismiss();
				}
			});
		}
	}
}
