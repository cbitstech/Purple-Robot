package edu.northwestern.cbits.purple_robot_manager.activities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;

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
import edu.emory.mathcs.backport.java.util.Collections;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;

public class DialogActivity extends Activity 
{
	public static final String DIALOG_TAG = "dialog_tag";
	public static final String DIALOG_PRIORITY = "dialog_priority";
	public static final String DIALOG_ADDED = "dialog_added";
	public static String DIALOG_MESSAGE = "dialog_message";
	public static String DIALOG_TITLE = "dialog_title";
	public static String DIALOG_CONFIRM_BUTTON = "dialog_confirm";
	public static String DIALOG_CANCEL_BUTTON= "dialog_cancel";
	public static String DIALOG_CONFIRM_SCRIPT = "dialog_confirm_script";
	public static String DIALOG_CANCEL_SCRIPT = "dialog_cancel_script";
	
	private static ArrayList<HashMap<String, Object>> _pendingDialogs = new ArrayList<HashMap<String, Object>>();
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
	
	public static void showNativeDialog(Context context, String title, String message, String confirmTitle, String cancelTitle, String confirmScript, String cancelScript, String tag, long priority)
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
		
		if (tag == null || "".equals(tag))
			tag = UUID.randomUUID().toString();
		
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

			intent.putExtra(DialogActivity.DIALOG_TAG, tag);
			intent.putExtra(DialogActivity.DIALOG_ADDED, System.currentTimeMillis());
			intent.putExtra(DialogActivity.DIALOG_PRIORITY, priority);

			context.startActivity(intent);
		}
		else
		{
			Intent intent = DialogActivity._currentActivity.getIntent();
		
			if (intent.getLongExtra(DialogActivity.DIALOG_PRIORITY, 0) < priority)
			{
				HashMap<String, Object> dialog = new HashMap<String, Object>();
				dialog.put(DialogActivity.DIALOG_TITLE, intent.getStringExtra(DialogActivity.DIALOG_TITLE));
				dialog.put(DialogActivity.DIALOG_MESSAGE, intent.getStringExtra(DialogActivity.DIALOG_MESSAGE));
				dialog.put(DialogActivity.DIALOG_CONFIRM_BUTTON, intent.getStringExtra(DialogActivity.DIALOG_CONFIRM_BUTTON));
				dialog.put(DialogActivity.DIALOG_CONFIRM_SCRIPT, intent.getStringExtra(DialogActivity.DIALOG_CONFIRM_SCRIPT));
				dialog.put(DialogActivity.DIALOG_CANCEL_BUTTON, intent.getStringExtra(DialogActivity.DIALOG_CANCEL_BUTTON));
				dialog.put(DialogActivity.DIALOG_CANCEL_SCRIPT, intent.getStringExtra(DialogActivity.DIALOG_CANCEL_SCRIPT));

				dialog.put(DialogActivity.DIALOG_TAG, intent.getStringExtra(DialogActivity.DIALOG_CANCEL_SCRIPT));
				dialog.put(DialogActivity.DIALOG_ADDED, intent.getLongExtra(DialogActivity.DIALOG_ADDED, System.currentTimeMillis()));
				dialog.put(DialogActivity.DIALOG_PRIORITY, intent.getLongExtra(DialogActivity.DIALOG_PRIORITY, 0));

				DialogActivity._pendingDialogs.add(dialog);
			}

			HashMap<String, Object> dialog = new HashMap<String, Object>();
			dialog.put(DialogActivity.DIALOG_TITLE, title);
			dialog.put(DialogActivity.DIALOG_MESSAGE, message);
			dialog.put(DialogActivity.DIALOG_CONFIRM_BUTTON, confirmTitle);
			dialog.put(DialogActivity.DIALOG_CONFIRM_SCRIPT, confirmScript);
			dialog.put(DialogActivity.DIALOG_CANCEL_BUTTON, cancelTitle);
			dialog.put(DialogActivity.DIALOG_CANCEL_SCRIPT, cancelScript);
			dialog.put(DialogActivity.DIALOG_TAG, tag);
			dialog.put(DialogActivity.DIALOG_ADDED, System.currentTimeMillis());
			dialog.put(DialogActivity.DIALOG_PRIORITY, priority);

			DialogActivity.clearNativeDialogs(tag, dialog);
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
		}
		
		String cancelTitle = intent.getStringExtra(DialogActivity.DIALOG_CANCEL_BUTTON);
		final String cancelScript = intent.getStringExtra(DialogActivity.DIALOG_CANCEL_SCRIPT);

		if (cancelTitle.trim().length() > 0)
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
						DialogActivity.sortPending();
						
						HashMap<String, Object> dialog = DialogActivity._pendingDialogs.remove(0);
						
						Intent intent = new Intent();
						intent.putExtra(DialogActivity.DIALOG_TITLE, dialog.get(DialogActivity.DIALOG_TITLE).toString());
						intent.putExtra(DialogActivity.DIALOG_MESSAGE, dialog.get(DialogActivity.DIALOG_MESSAGE).toString());
						intent.putExtra(DialogActivity.DIALOG_CONFIRM_BUTTON, dialog.get(DialogActivity.DIALOG_CONFIRM_BUTTON).toString());
						intent.putExtra(DialogActivity.DIALOG_CONFIRM_SCRIPT, dialog.get(DialogActivity.DIALOG_CONFIRM_SCRIPT).toString());
						intent.putExtra(DialogActivity.DIALOG_CANCEL_BUTTON, dialog.get(DialogActivity.DIALOG_CANCEL_BUTTON).toString());
						intent.putExtra(DialogActivity.DIALOG_CANCEL_SCRIPT, dialog.get(DialogActivity.DIALOG_CANCEL_SCRIPT).toString());
						intent.putExtra(DialogActivity.DIALOG_TAG, dialog.get(DialogActivity.DIALOG_TAG).toString());
						intent.putExtra(DialogActivity.DIALOG_PRIORITY, (Long) dialog.get(DialogActivity.DIALOG_PRIORITY));
						
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

	private static void sortPending() 
	{
		Collections.sort(DialogActivity._pendingDialogs, new Comparator<HashMap<String, Object>>()
		{
			public int compare(HashMap<String, Object> one, HashMap<String, Object> two) 
			{
				Long priorityOne = (Long) one.get(DialogActivity.DIALOG_TITLE);
				Long priorityTwo = (Long) two.get(DialogActivity.DIALOG_TITLE);
				
				if (priorityOne.longValue() < priorityTwo.longValue())
					return 1;
				else if (priorityOne.longValue() > priorityTwo.longValue())
					return -1;

				Long addedOne = (Long) one.get(DialogActivity.DIALOG_ADDED);
				Long addedTwo = (Long) two.get(DialogActivity.DIALOG_ADDED);
				
				if (addedOne.longValue() < addedTwo.longValue())
					return -1;
				else if (addedOne.longValue() > addedTwo.longValue())
					return 1;

				return 0;
			}
		});
	}

	public static void clearNativeDialogs() 
	{
		DialogActivity._pendingDialogs.clear();
		
		if (DialogActivity._currentDialog != null && DialogActivity._currentActivity != null)
		{
			DialogActivity._currentActivity.runOnUiThread(new Runnable()
			{
				public void run() 
				{
					DialogActivity._currentDialog.dismiss();
				}
			});
		}
	}

	public static void clearNativeDialogs(String tag, HashMap<String, Object> replacement) 
	{
		ArrayList<HashMap<String, Object>> toRemove = new ArrayList<HashMap<String, Object>>();
		
		for (HashMap<String, Object> dialog : DialogActivity._pendingDialogs)
		{
			if (tag.equals(dialog.get(DialogActivity.DIALOG_TAG)))
				toRemove.add(dialog);
		}
		
		DialogActivity._pendingDialogs.removeAll(toRemove);
		
		if (replacement != null)
			DialogActivity._pendingDialogs.add(replacement);
		
		DialogActivity.sortPending();
		
		if (DialogActivity._currentDialog != null && DialogActivity._currentActivity != null)
		{
			Intent intent = DialogActivity._currentActivity.getIntent();
			
			if (tag.equals(intent.getStringExtra(DialogActivity.DIALOG_TAG)))
			{
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
}
