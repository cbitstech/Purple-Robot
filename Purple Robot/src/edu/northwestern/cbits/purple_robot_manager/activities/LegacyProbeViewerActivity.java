package edu.northwestern.cbits.purple_robot_manager.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

@SuppressWarnings("deprecation")
public class LegacyProbeViewerActivity extends PreferenceActivity
{
    private String _probeName = null;
    private Bundle _probeBundle = null;
    private Probe _probe = null;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle bundle = this.getIntent().getExtras();

        this._probeName = bundle.getString("probe_name");
        this._probeBundle = bundle.getBundle("probe_bundle");

        if (bundle.getBoolean("is_model", false))
        {
            PreferenceScreen screen = ProbeViewerActivity.screenForBundle(this, this.getPreferenceManager(), this._probeName, this._probeBundle);

            this.setPreferenceScreen(screen);
        }
        else
        {
            this._probe = ProbeManager.probeForName(this._probeName, this);

            if (this._probe != null)
            {
                Bundle formattedBundle = this._probe.formattedBundle(this, this._probeBundle);

                if (formattedBundle != null)
                {
                    PreferenceScreen screen = ProbeViewerActivity.screenForBundle(this, this.getPreferenceManager(), this._probe.title(this), formattedBundle);
                    screen.setTitle(this._probe.title(this));

                    screen.addPreference(ProbeViewerActivity.screenForBundle(this, this.getPreferenceManager(), this.getString(R.string.display_raw_data),
                            this._probeBundle));

                    this.setPreferenceScreen(screen);
                }
                else
                {
                    PreferenceScreen screen = ProbeViewerActivity.screenForBundle(this, this.getPreferenceManager(), this._probe.title(this), this._probeBundle);

                    this.setPreferenceScreen(screen);
                }
            }
        }
    }
}
