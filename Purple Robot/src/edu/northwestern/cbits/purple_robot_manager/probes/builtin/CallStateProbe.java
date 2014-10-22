package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class CallStateProbe extends Probe {
    public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.builtin.CallStateProbe";

    public static final String STATE_IDLE = "Idle";
    public static final String STATE_OFF_HOOK = "Off-Hook";
    public static final String STATE_RINGING = "Ringing";

    public static final String CALL_STATE = "CALL_STATE";

    private static final boolean DEFAULT_ENABLED = true;

    private long _lastXmit = 0;
    private BroadcastReceiver _receiver = null;

    public String name(Context context) {
        return CallStateProbe.NAME;
    }

    public String title(Context context) {
        return context.getString(R.string.title_call_state_probe);
    }

    public String probeCategory(Context context) {
        return context.getResources().getString(
                R.string.probe_device_info_category);
    }

    public boolean isEnabled(Context context) {
        if (super.isEnabled(context)) {
            SharedPreferences prefs = Probe.getPreferences(context);

            if (prefs.getBoolean("config_probe_call_state_enabled",
                    CallStateProbe.DEFAULT_ENABLED)) {
                if (this._receiver == null) {
                    final CallStateProbe me = this;

                    this._receiver = new BroadcastReceiver() {
                        public void onReceive(Context context, Intent intent) {
                            SharedPreferences prefs = Probe
                                    .getPreferences(context);

                            if (prefs.getBoolean(
                                    "config_probe_call_state_enabled",
                                    CallStateProbe.DEFAULT_ENABLED)) {
                                TelephonyManager tm = (TelephonyManager) context
                                        .getSystemService(Context.TELEPHONY_SERVICE);

                                Bundle bundle = new Bundle();
                                bundle.putString("PROBE", me.name(context));
                                bundle.putLong("TIMESTAMP",
                                        System.currentTimeMillis() / 1000);

                                bundle.putString(CallStateProbe.CALL_STATE,
                                        me.getCallState(tm.getCallState()));

                                me.transmitData(context, bundle);
                            }
                        }
                    };

                    IntentFilter filter = new IntentFilter(
                            TelephonyManager.ACTION_PHONE_STATE_CHANGED);
                    context.registerReceiver(this._receiver, filter);
                }

                long now = System.currentTimeMillis();

                if (now - this._lastXmit > 60000) {
                    TelephonyManager tm = (TelephonyManager) context
                            .getSystemService(Context.TELEPHONY_SERVICE);

                    Bundle bundle = new Bundle();
                    bundle.putString("PROBE", this.name(context));
                    bundle.putLong("TIMESTAMP", now / 1000);

                    bundle.putString(CallStateProbe.CALL_STATE,
                            this.getCallState(tm.getCallState()));

                    this.transmitData(context, bundle);

                    this._lastXmit = now;
                }

                return true;
            }
        }

        if (this._receiver != null) {
            try {
                context.unregisterReceiver(this._receiver);
            } catch (RuntimeException e) {

            }

            this._receiver = null;
        }

        return false;
    }

    public Bundle formattedBundle(Context context, Bundle bundle) {
        Bundle formatted = super.formattedBundle(context, bundle);

        formatted.putString(context.getString(R.string.call_state_label),
                bundle.getString(CallStateProbe.CALL_STATE));

        return formatted;
    };

    protected String getCallState(int callState) {
        String state = "Unknown";

        switch (callState) {
        case TelephonyManager.CALL_STATE_IDLE:
            state = CallStateProbe.STATE_IDLE;
            break;
        case TelephonyManager.CALL_STATE_OFFHOOK:
            state = CallStateProbe.STATE_OFF_HOOK;
            break;
        case TelephonyManager.CALL_STATE_RINGING:
            state = CallStateProbe.STATE_RINGING;
            break;
        }

        return state;
    }

    public String summarizeValue(Context context, Bundle bundle) {
        String state = bundle.getString(CallStateProbe.CALL_STATE);

        return String.format(
                context.getResources().getString(
                        R.string.summary_call_state_probe), state);
    }

    public void enable(Context context) {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean("config_probe_call_state_enabled", true);
        e.commit();
    }

    public void disable(Context context) {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean("config_probe_call_state_enabled", false);
        e.commit();
    }

    public String summary(Context context) {
        return context.getString(R.string.summary_call_state_probe_desc);
    }

    @SuppressWarnings("deprecation")
    public PreferenceScreen preferenceScreen(PreferenceActivity activity) {
        PreferenceManager manager = activity.getPreferenceManager();

        PreferenceScreen screen = manager.createPreferenceScreen(activity);
        screen.setTitle(this.title(activity));
        screen.setSummary(R.string.summary_call_state_probe_desc);

        CheckBoxPreference enabled = new CheckBoxPreference(activity);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey("config_probe_call_state_enabled");
        enabled.setDefaultValue(CallStateProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        return screen;
    }
}
