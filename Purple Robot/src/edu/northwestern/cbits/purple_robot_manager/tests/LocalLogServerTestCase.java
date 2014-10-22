package edu.northwestern.cbits.purple_robot_manager.tests;

import junit.framework.Assert;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.http.LogServerEmulatorRequestHandler;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

public class LocalLogServerTestCase extends RobotTestCase {
    public LocalLogServerTestCase(Context context, int priority) {
        super(context, priority);
    }

    public void test() {
        if (this.isSelected(this._context) == false)
            return;

        try {
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(this._context);

            Editor e = prefs.edit();
            e.remove(LogServerEmulatorRequestHandler.LOG_COUNT);
            e.commit();

            String oldEndpoint = LogManager.getInstance(this._context)
                    .getEndpoint();
            boolean oldEnabled = LogManager.getInstance(this._context)
                    .getEnabled();

            LogManager.getInstance(this._context).setEndpoint(
                    "http://127.0.0.1:12345/log");

            Assert.assertEquals("LLST001", "http://127.0.0.1:12345/log",
                    LogManager.getInstance(this._context).getEndpoint());

            LogManager.getInstance(this._context).setEnabled(true);

            Assert.assertEquals("LLST002", true,
                    LogManager.getInstance(this._context).getEnabled());

            LogManager.getInstance(this._context).log("test_event", null);

            LogManager.getInstance(this._context).upload();

            Thread.sleep(3000);

            prefs = PreferenceManager
                    .getDefaultSharedPreferences(this._context);

            Assert.assertTrue(
                    "LLST003",
                    prefs.getInt(LogServerEmulatorRequestHandler.LOG_COUNT, 0) > 0);

            LogManager.getInstance(this._context).setEndpoint(oldEndpoint);

            Assert.assertEquals("LLST004", "" + oldEndpoint, ""
                    + LogManager.getInstance(this._context).getEndpoint());

            LogManager.getInstance(this._context).setEnabled(oldEnabled);

            Assert.assertEquals("LLST005", oldEnabled,
                    LogManager.getInstance(this._context).getEnabled());
        } catch (InterruptedException e) {
            LogManager.getInstance(this._context).logException(e);
        }
    }

    public int estimatedMinutes() {
        return 1;
    }

    public String name(Context context) {
        return context.getString(R.string.name_local_log_server_test);
    }
}
