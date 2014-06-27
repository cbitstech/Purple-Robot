package edu.northwestern.cbits.purple_robot_manager.activities;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
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
	public static final String PROBE_NAME = "probe_name";
	
	private boolean _inited = false;

	private BroadcastReceiver _receiver = null;
	

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

	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        final String probeName = this.getIntent().getStringExtra(RealTimeProbeViewActivity.PROBE_NAME);
		
        final WebView webview = (WebView) this.findViewById(R.id.webview);
        
        this._receiver = new BroadcastReceiver()
        {
			public void onReceive(Context context, Intent intent) 
			{
				String bcastName = intent.getStringExtra("PROBE");
				
				if (bcastName != null && bcastName.equals(probeName))
				{
					Bundle extras = intent.getExtras();
					
					float[] xs = extras.getFloatArray("X");
					float[] ys = extras.getFloatArray("Y");
					float[] zs = extras.getFloatArray("Z");
					double[] ts = extras.getDoubleArray("EVENT_TIMESTAMP");

					Log.e("PR", "GOT MATCH " + xs + " - " + ys + " - " + zs + " - " + ts);
					
					webview.loadUrl("javascript:newData(" + xs[0] + ", " + ys[0] + ", " + zs[0] + ", " + ts[0] + ");");
				}
			}
        };
    }

	@SuppressLint("SetJavaScriptEnabled")
	protected void onResume()
	{
		super.onResume();

		WebView webview = (WebView) this.findViewById(R.id.webview);

		webview.setWebChromeClient(new WebChromeClient()
		{
			  public boolean onConsoleMessage(ConsoleMessage cm)
			  {
				    Log.e("PRM", cm.message() + " -- From line "
				                         + cm.lineNumber() + " of "
				                         + cm.sourceId() );
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
			Intent dataIntent = new Intent(this, ProbeViewerActivity.class);

			dataIntent.putExtra("probe_name", this.getIntent().getStringExtra("probe_name"));
			dataIntent.putExtra("probe_bundle", this.getIntent().getParcelableExtra("probe_bundle"));

			this.startActivity(dataIntent);

			this.finish();
		}
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Probe.PROBE_READING);
		
		LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this);
		localManager.registerReceiver(this._receiver , intentFilter);
	}
	
	protected void onPause()
	{
		LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this);
		localManager.unregisterReceiver(this._receiver);
	}
}
