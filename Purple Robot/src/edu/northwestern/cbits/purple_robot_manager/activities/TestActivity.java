package edu.northwestern.cbits.purple_robot_manager.activities;

import java.util.Enumeration;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
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
    protected static String _progressMessage;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.getSupportActionBar().setTitle(R.string.title_run_tests);

        if (TestActivity._testRunner == null)
            TestActivity._testRunner = new RobotTestRunner(this);

        this.setContentView(R.layout.layout_test_activity);

        this.updateSubtitle();
    }

    protected void onResume()
    {
        super.onResume();

        final TestActivity me = this;

        ListView listView = (ListView) this.findViewById(R.id.list_tests);

        ArrayAdapter<TestCase> adapter = new ArrayAdapter<TestCase>(this, R.layout.layout_test_row,
                TestActivity._testRunner.getTestCases(this))
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
                    final RobotTestCase robot = (RobotTestCase) test;

                    CheckBox nameField = (CheckBox) convertView.findViewById(R.id.text_test_name);
                    nameField.setText(robot.name(me));

                    nameField.setOnCheckedChangeListener(new OnCheckedChangeListener()
                    {
                        public void onCheckedChanged(CompoundButton arg0, boolean arg1)
                        {
                            // Do nothing...
                        }
                    });

                    nameField.setChecked(robot.isSelected(me));

                    nameField.setOnCheckedChangeListener(new OnCheckedChangeListener()
                    {
                        public void onCheckedChanged(CompoundButton arg0, boolean isSelected)
                        {
                            robot.setSelected(me, isSelected);

                            me.updateSubtitle();
                        }
                    });

                    TextView detailsField = (TextView) convertView.findViewById(R.id.text_test_details);
                    detailsField.setText(robot.description(me));
                }

                final View view = convertView;

                convertView.setOnClickListener(new OnClickListener()
                {
                    public void onClick(View arg0)
                    {
                        CheckBox nameField = (CheckBox) view.findViewById(R.id.text_test_name);

                        nameField.setChecked(nameField.isChecked() == false);
                    }
                });

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
                            TestActivity._progressMessage = intent.getStringExtra(TestActivity.PROGRESS_MESSAGE);

                            me.showProgress(TestActivity._progressMessage);
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

        this.showProgress(TestActivity._progressMessage);
    }

    protected void updateSubtitle()
    {
        List<TestCase> cases = TestActivity._testRunner.getTestCases(this);

        int minutes = 0;
        int count = 0;

        for (TestCase test : cases)
        {
            if (test instanceof RobotTestCase)
            {
                RobotTestCase robot = (RobotTestCase) test;

                if (robot.isSelected(this))
                {
                    count += 1;
                    minutes += robot.estimatedMinutes();
                }
            }
        }

        if (count == 0)
            this.getSupportActionBar().setSubtitle(this.getString(R.string.subtitle_no_tests));
        else if (minutes < 1)
        {
            if (count < 2)
                this.getSupportActionBar().setSubtitle(this.getString(R.string.subtitle_minute_or_less_test_single));
            else
                this.getSupportActionBar().setSubtitle(this.getString(R.string.subtitle_minute_or_less_test, count));
        }
        else
        {
            if (count < 2)
                this.getSupportActionBar().setSubtitle(this.getString(R.string.subtitle_minutes_test_single, minutes));
            else
                this.getSupportActionBar().setSubtitle(this.getString(R.string.subtitle_minutes_test, count, minutes));
        }
    }

    protected void onPause()
    {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this._receiver);

        this._receiver = null;

        if (TestActivity._progress != null)
        {
            TestActivity._progress.dismiss();
            TestActivity._progress = null;
        }

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

        final int itemId = item.getItemId();

        if (itemId == R.id.menu_test_item)
        {
            if (TestActivity._testRunner.isRunning())
            {
                item.setIcon(R.drawable.action_play);

                TestActivity._testRunner.stopTests();
            }
            else
            {
                this.showProgress(this.getString(R.string.message_starting_test));

                final TestResult result = new TestResult();

                TestActivity._testRunner.startTests(this, result, new Runnable()
                {
                    public void run()
                    {
                        me.runOnUiThread(new Runnable()
                        {
                            public void run()
                            {
                                me.showProgress(null);

                                int count = result.errorCount() + result.failureCount();

                                String title = me.getString(R.string.title_tests_successful);
                                String message = me.getString(R.string.message_tests_successful);

                                if (count != 0)
                                {
                                    if (count > 1)
                                        title = me.getString(R.string.title_tests_failed, count);
                                    else
                                        title = me.getString(R.string.title_test_failed);

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

                                    message = sb.toString().replace(
                                            "edu.northwestern.cbits.purple_robot_manager.tests.", "");
                                }

                                DialogActivity.showNativeDialog(me, title, message,
                                        me.getString(R.string.action_close), null, null, null, null, Integer.MAX_VALUE);
                            }
                        });
                    }
                });
            }
        }

        return true;
    }

    private void showProgress(String message)
    {
        TestActivity._progressMessage = message;

        if (TestActivity._progressMessage != null)
        {
            if (TestActivity._progress == null)
            {
                TestActivity._progress = new ProgressDialog(this);
                TestActivity._progress.setIndeterminate(true);

                TestActivity._progress.setTitle(R.string.title_running_tests);
                TestActivity._progress.setCancelable(false);
                TestActivity._progress.show();
            }

            TestActivity._progress.setMessage(TestActivity._progressMessage);
        }
        else
        {
            if (TestActivity._progress != null)
            {
                TestActivity._progress.dismiss();
                TestActivity._progress = null;
            }
        }
    }
}
