package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.ArrayList;

import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.LinearLayout;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class TouchEventsProbe extends Probe
{
    private static final String ENABLED = "config_probe_touch_events_enabled";
    private static final boolean DEFAULT_ENABLED = false;

    private Context _context = null;
    private View _overlay = null;
    private final ArrayList<Long> _timestamps = new ArrayList<>();
    private long _lastTouch = 0;

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.TouchEventsProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_touch_events_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_misc_category);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(TouchEventsProbe.ENABLED, true);

        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(TouchEventsProbe.ENABLED, false);

        e.commit();
    }

    @Override
    public boolean isEnabled(Context context)
    {
        final SharedPreferences prefs = Probe.getPreferences(context);

        boolean enabled = super.isEnabled(context);

        if (this._context == null)
            this._context = context.getApplicationContext();

        if (enabled)
            enabled = prefs.getBoolean(TouchEventsProbe.ENABLED, TouchEventsProbe.DEFAULT_ENABLED);

        if (enabled)
        {
            WindowManager wm = (WindowManager) this._context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);

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
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

                    final TouchEventsProbe me = this;

                    this._overlay = new LinearLayout(this._context.getApplicationContext());
                    this._overlay.setBackgroundColor(android.graphics.Color.argb(0, 255, 255, 255));
                    this._overlay.setHapticFeedbackEnabled(true);
                    this._overlay.setOnTouchListener(new OnTouchListener()
                    {
                        @Override
                        public boolean onTouch(View arg0, MotionEvent event)
                        {
                            me._lastTouch = System.currentTimeMillis();
                            me._timestamps.add(me._lastTouch);

                            return false;
                        }
                    });

                    if (Looper.myLooper() == null)
                        Looper.prepare();

                    try
                    {
                        wm.addView(this._overlay, params);
                    }
                    catch (IllegalStateException e)
                    {
                        LogManager.getInstance(context).logException(e);
                    }
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            {
                if (this._overlay.isAttachedToWindow())
                    wm.removeView(this._overlay);
            }

            this._overlay = null;
        }

        return false;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        int count = (int) bundle.getDouble("TOUCH_COUNT");
        long delay = (long) bundle.getDouble("LAST_TOUCH_DELAY");

        if (count == 1)
            return context.getResources().getString(R.string.summary_touch_events_probe_single, delay);

        return context.getResources().getString(R.string.summary_touch_events_probe, count, delay);
    }

    @Override
    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(R.string.summary_touch_events_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(TouchEventsProbe.ENABLED);
        enabled.setDefaultValue(TouchEventsProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        return screen;
    }

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_touch_events_probe_desc);
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        return null;

        // JSONObject settings = new JSONObject();
        //
        // try
        // {
        // JSONObject enabled = new JSONObject();
        // enabled.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
        // JSONArray values = new JSONArray();
        // values.put(true);
        // values.put(false);
        // enabled.put(Probe.PROBE_VALUES, values);
        // settings.put(Probe.PROBE_ENABLED, enabled);
        // }
        // catch (JSONException e)
        // {
        // LogManager.getInstance(context).logException(e);
        // }
        //
        // return settings;
    }
}
