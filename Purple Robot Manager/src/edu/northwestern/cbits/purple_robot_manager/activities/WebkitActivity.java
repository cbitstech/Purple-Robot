package edu.northwestern.cbits.purple_robot_manager.activities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import edu.northwestern.cbits.purple_robot_manager.ProbeViewerActivity;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

public class WebkitActivity extends SherlockActivity
{
	public static String stringForAsset(Activity activity, String assetName) throws IOException
	{
		InputStream is = activity.getAssets().open(assetName);

		BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

		StringBuffer sb = new StringBuffer();
		String line = null;

		while ((line = br.readLine()) != null)
		{
			sb.append(line);
		}

		return sb.toString();
	}


	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_webkit_activity);
    }

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

			return WebkitActivity.stringForAsset(this, "webkit/webview.html");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return null;
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
	}


	private String contentSubtitle()
	{
		String name = this.getIntent().getStringExtra("probe_name");

		if (name != null)
		{
			Probe p = ProbeManager.probeForName(name, this);

			return p.contentSubtitle(this);
		}

		return null;
	}

	private String contentTitle()
	{
		String name = this.getIntent().getStringExtra("probe_name");

		if (name != null)
		{
			Probe p = ProbeManager.probeForName(name, this);

			return p.title(this);
		}

		return this.getString(R.string.app_name);
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
        MenuInflater inflater = this.getSupportMenuInflater();
        inflater.inflate(R.menu.menu_probe_activity, menu);

        return true;
	}

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
    	{
    		case R.id.menu_data_item:
				Intent dataIntent = new Intent(this, ProbeViewerActivity.class);

				dataIntent.putExtra("probe_name", this.getIntent().getStringExtra("probe_name"));
				dataIntent.putExtra("probe_bundle", this.getIntent().getParcelableExtra("probe_bundle"));

				this.startActivity(dataIntent);

    			break;
		}

    	return true;
    }
}
