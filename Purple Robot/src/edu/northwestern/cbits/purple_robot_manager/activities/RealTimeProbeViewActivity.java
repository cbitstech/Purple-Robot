package edu.northwestern.cbits.purple_robot_manager.activities;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

@SuppressLint("SimpleDateFormat")
public class RealTimeProbeViewActivity extends WebkitActivity
{
    public static final String PROBE_ID = "probe_id";

    private static RealTimeProbeViewActivity _currentActivity = null;

    private static long _lastUpdate = 0;

    private boolean _inited = false;

    private int _probeId = -1;

    @Override
    protected String contentString()
    {
        try
        {
            String name = this.getIntent().getStringExtra("probe_name");

            if (name != null)
            {
                Probe p = ProbeManager.probeForName(name, this);

                String content = p.getDisplayContent(this);

                if (content != null)
                    return content;
            }

            return RealTimeProbeViewActivity.stringForAsset(this, "webkit/webview_epoch.html");
        }
        catch (IOException e)
        {
            LogManager.getInstance(this).logException(e);
        }

        return null;
    }

    @Override
    protected String contentSubtitle()
    {
        return this.getString(R.string.subtitle_streaming_live);
    }

    @Override
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    protected void onResume()
    {
        super.onResume();

        this._probeId = this.getIntent().getIntExtra(RealTimeProbeViewActivity.PROBE_ID, -1);

        WebView webview = (WebView) this.findViewById(R.id.webview);

        webview.setWebChromeClient(new WebChromeClient()
        {
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm)
            {
                Log.e("PR", cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
                return true;
            }
        });

        if (this._inited == false)
        {
            webview.addJavascriptInterface(this, "PurpleRobot");

            this._inited = true;
        }

        String contentString = this.contentString();

        if (contentString != null)
        {
            webview.getSettings().setJavaScriptEnabled(true);
            webview.loadDataWithBaseURL("file:///android_asset/webkit/", contentString, "text/html", "UTF-8", null);

            String title = this.contentTitle();
            String subtitle = this.contentSubtitle();

            ActionBar actionBar = this.getSupportActionBar();

            actionBar.setTitle(title);
            actionBar.setSubtitle(subtitle);
        }
        else
        {
            Intent dataIntent = new Intent(this, LegacyProbeViewerActivity.class);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
                dataIntent = new Intent(this, ProbeViewerActivity.class);

            dataIntent.putExtra("probe_name", this.getIntent().getStringExtra("probe_name"));
            dataIntent.putExtra("probe_bundle", this.getIntent().getParcelableExtra("probe_bundle"));

            this.startActivity(dataIntent);

            this.finish();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Probe.PROBE_READING);

        RealTimeProbeViewActivity._currentActivity = this;
    }

    @Override
    protected void onPause()
    {
        RealTimeProbeViewActivity._currentActivity = null;

        super.onPause();
    }

    public static void plotIfVisible(final int probeId, final double[] values)
    {
        if (RealTimeProbeViewActivity._currentActivity != null)
        {
            long now = System.currentTimeMillis();

            if (now - RealTimeProbeViewActivity._lastUpdate < 1000)
                return;

            final RealTimeProbeViewActivity me = RealTimeProbeViewActivity._currentActivity;

            if (probeId != me._probeId)
                return;

            RealTimeProbeViewActivity._lastUpdate = now;

            me.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    final WebView webview = (WebView) me.findViewById(R.id.webview);

                    double t = values[0];
                    double x = values[1];
                    double y = 0;
                    double z = 0;

                    if (values.length >= 4)
                    {
                        y = values[2];
                        z = values[3];
                    }

                    webview.loadUrl("javascript:newData(" + x + ", " + y + ", " + z + ", " + t + ");");
                }
            });
        }
    }
}
