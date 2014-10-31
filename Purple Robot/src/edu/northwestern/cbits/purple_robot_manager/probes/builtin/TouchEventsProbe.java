package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.LinearLayout;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class TouchEventsProbe extends Probe
{
    private static final boolean DEFAULT_ENABLED = false;

    private static final String ENABLED_KEY = "config_probe_activity_detection_enabled";

    private Context _context = null;
    private View _overlay = null;
    private ArrayList<Long> _timestamps = new ArrayList<Long>();
    private long _lastTouch = 0;

    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.TouchEventsProbe";
    }

    public String title(Context context)
    {
        return context.getString(R.string.title_touch_events_probe);
    }

    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_misc_category);
    }

    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(TouchEventsProbe.ENABLED_KEY, true);

        e.commit();
    }

    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(TouchEventsProbe.ENABLED_KEY, false);

        e.commit();
    }

    public boolean isEnabled(Context context)
    {
        final SharedPreferences prefs = Probe.getPreferences(context);

        boolean enabled = super.isEnabled(context);

        if (this._context == null)
            this._context = context.getApplicationContext();

        if (enabled)
            enabled = prefs.getBoolean(TouchEventsProbe.ENABLED_KEY, TouchEventsProbe.DEFAULT_ENABLED);

        if (enabled)
        {
            WindowManager wm = (WindowManager) this._context.getApplicationContext().getSystemService(
                    Context.WINDOW_SERVICE);

            synchronized (wm)
            {
                if (this._overlay == null)
                {

                    WindowManager.LayoutParams params = new WindowManager.LayoutParams();

                    params.format = PixelFormat.TRANSLUCENT;
                    params.height = 1; // WindowManager.LayoutParams.MATCH_PARENT;
                    params.width = 1; // WindowManager.LayoutParams.MATCH_PARENT;
                    params.gravity = Gravity.RIGHT | Gravity.BOTTOM;
                    params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

                    final TouchEventsProbe me = this;

                    this._overlay = new LinearLayout(this._context.getApplicationContext());
                    this._overlay.setBackgroundColor(android.graphics.Color.argb(0, 255, 255, 255));
                    this._overlay.setHapticFeedbackEnabled(true);
                    this._overlay.setOnTouchListener(new OnTouchListener()
                    {
                        public boolean onTouch(View arg0, MotionEvent event)
                        {
                            me._lastTouch = System.currentTimeMillis();
                            me._timestamps.add(me._lastTouch);

                            return false;
                        }
                    });

                    wm.addView(this._overlay, params);
                }
            }

            if (this._lastTouch != 0)
            {
                long now = System.currentTimeMillis();

                Bundle bundle = new Bundle();

                bundle.putString("PROBE", this.name(context));
                bundle.putLong("TIMESTAMP", now / 1000);

                bundle.putLong("LAST_TOUCH_DELAY", now - this._lastTouch);
                bundle.putInt("TOUCH_COUNT", this._timestamps.size());

                this.transmitData(context, bundle);

                this._timestamps.clear();
            }

            return true;
        }
        else if (this._overlay != null)
        {
            WindowManager wm = (WindowManager) this._context.getSystemService(Context.WINDOW_SERVICE);

            if (this._overlay.isAttachedToWindow())
                wm.removeView(this._overlay);

            this._overlay = null;
        }

        return false;
    }

    public String summarizeValue(Context context, Bundle bundle)
    {
        int count = (int) bundle.getDouble("TOUCH_COUNT");
        long delay = (long) bundle.getDouble("LAST_TOUCH_DELAY");

        if (count == 1)
            return context.getResources().getString(R.string.summary_touch_events_probe_single, delay);

        return context.getResources().getString(R.string.summary_touch_events_probe, count, delay);
    }

    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(PreferenceActivity activity)
    {
        PreferenceManager manager = activity.getPreferenceManager();

        PreferenceScreen screen = manager.createPreferenceScreen(activity);
        screen.setTitle(this.title(activity));
        screen.setSummary(R.string.summary_touch_events_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(TouchEventsProbe.ENABLED_KEY);
        enabled.setDefaultValue(TouchEventsProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        return screen;
    }

    public String summary(Context context)
    {
        return context.getString(R.string.summary_touch_events_probe_desc);
    }
}
