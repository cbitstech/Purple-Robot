package edu.northwestern.cbits.purple_robot_manager.activities;

import java.util.Enumeration;

import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.tests.RobotTestCase;
import edu.northwestern.cbits.purple_robot_manager.tests.RobotTestRunner;

public class TestActivity extends ActionBarActivity
{
	public static final String PROGRESS_MESSAGE = "test_activity_progress_message";
	public static final String PROGRESS_DELAY = "test_activity_progress_delay";
	public static final String INTENT_PROGRESS_MESSAGE = "intent_test_activiity_progress_message";

	private static RobotTestRunner _testRunner = null;
	private BroadcastReceiver _receiver = null;
	
	private static ProgressDialog _progress = null;

	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);
		
		this.getSupportActionBar().setTitle(R.string.title_run_tests);
		
		if (TestActivity._testRunner == null)
			TestActivity._testRunner = new RobotTestRunner(this);

		Log.e("PR", "RUNNER: " + TestActivity._testRunner);
		Log.e("PR", "CASES: " + TestActivity._testRunner.getTestCases());
		
		int count = TestActivity._testRunner.getTestCases().size();
		
		if (count != 1)
			this.getSupportActionBar().setSubtitle(this.getString(R.string.subtitle_run_tests, count));
		else
			this.getSupportActionBar().setSubtitle(R.string.subtitle_run_tests_single);
		
		this.setContentView(R.layout.layout_test_activity);
    }
	
	protected void onResume()
	{
		super.onResume();
		
		final TestActivity me = this;
		
		ListView listView = (ListView) this.findViewById(R.id.list_tests);
		
		ArrayAdapter<TestCase> adapter = new ArrayAdapter<TestCase>(this, R.layout.layout_test_row, TestActivity._testRunner.getTestCases())
		{
        	@SuppressLint("InflateParams")
			public View getView(final int position, View convertView, ViewGroup parent)
        	{
        		if (convertView == null)
        		{
        			LayoutInflater inflater = (LayoutInflater) me.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        			convertView = inflater.inflate(R.layout.layout_test_row, null);
        		}
        		
        		TestCase test = this.getItem(position);
        		
        		if (test instanceof RobotTestCase)
        		{
        			RobotTestCase robot = (RobotTestCase) test;
        			
	        		TextView nameField = (TextView) convertView.findViewById(R.id.text_test_name);
	        		nameField.setText(robot.name(me));
	
//	        		TextView detailsField = (TextView) convertView.findViewById(R.id.text_test_details);
//	        		detailsField.setText(robot.description(me));
        		}
        		
        		return convertView;
        	}
		};
		
		listView.setAdapter(adapter);
		
		this._receiver = new BroadcastReceiver()
		{
			public void onReceive(final Context context, final Intent intent) 
			{
				if (TestActivity._progress != null)
				{
					me.runOnUiThread(new Runnable()
					{
						public void run() 
						{
							TestActivity._progress.setMessage(intent.getStringExtra(TestActivity.PROGRESS_MESSAGE));
						}
					});
					
					try 
					{
						Thread.sleep(intent.getLongExtra(TestActivity.PROGRESS_DELAY, 1000));
					}
					catch (InterruptedException e) 
					{

					}
				}
			}
		};
		
		IntentFilter filter = new IntentFilter(TestActivity.INTENT_PROGRESS_MESSAGE);
		
		LocalBroadcastManager.getInstance(this).registerReceiver(this._receiver, filter);
	}
	
	protected void onPause()
	{
		LocalBroadcastManager.getInstance(this).unregisterReceiver(this._receiver);
		
		this._receiver = null;
		
		super.onPause();
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_test, menu);

        return true;
	}
	
    public boolean onOptionsItemSelected(final MenuItem item)
    {
    	final TestActivity me = this;
    	
        switch (item.getItemId())
    	{
			case R.id.menu_test_item:
				if (TestActivity._testRunner.isRunning())
				{
					item.setIcon(R.drawable.action_play);
					
					TestActivity._testRunner.stopTests();
				}
				else
				{
					item.setIcon(R.drawable.action_pause);
					
					TestActivity._progress = new ProgressDialog(this);
					TestActivity._progress.setIndeterminate(true);
					
					TestActivity._progress.setTitle(R.string.title_running_tests);
					TestActivity._progress.setCancelable(false);
					TestActivity._progress.show();
					
					final TestResult result = new TestResult();
					
					TestActivity._testRunner.startTests(result, new Runnable()
					{
						public void run() 
						{
							me.runOnUiThread(new Runnable()
							{
								public void run() 
								{
									TestActivity._progress.dismiss();
									TestActivity._progress = null;
									
									Log.e("PR", "RESULTS: " + result.errorCount() + " errors; " + result.failureCount() + " failures");
									
									int count = result.errorCount() + result.failureCount();

									AlertDialog.Builder builder = new AlertDialog.Builder(me);

									if (count == 0)
									{
										builder.setTitle(R.string.title_tests_successful);
										builder.setMessage(R.string.message_tests_successful);
									}
									else
									{
										if (count > 1)
											builder.setTitle(me.getString(R.string.title_tests_failed, count));
										else
											builder.setTitle(R.string.title_test_failed);
										
										StringBuffer sb = new StringBuffer();

										Enumeration<TestFailure> errors = result.errors();
										
										while (errors.hasMoreElements())
										{
											if (sb.length() > 0)
												sb.append("\n\n");
											
											TestFailure fail = errors.nextElement();
											
											sb.append(fail.toString());
										}

										Enumeration<TestFailure> fails = result.failures();
										
										while (fails.hasMoreElements())
										{
											if (sb.length() > 0)
												sb.append("\n\n");

											TestFailure fail = fails.nextElement();
											
											sb.append(fail.toString());
										}
										
										builder.setMessage(sb.toString().replace("edu.northwestern.cbits.purple_robot_manager.tests.", ""));
									}

									builder.setPositiveButton(R.string.action_close, null);
									builder.create().show();
									
									item.setIcon(R.drawable.action_play);
								}
							});
						}
					});
				}
				
				break;
    	}
        
        return true;
    }
}
