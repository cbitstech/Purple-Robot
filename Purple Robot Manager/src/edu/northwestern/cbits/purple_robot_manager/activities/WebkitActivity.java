package edu.northwestern.cbits.purple_robot_manager.activities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import edu.northwestern.cbits.purple_robot_manager.ProbeViewerActivity;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

public class WebkitActivity extends SherlockFragmentActivity
{
	private double _selectedTimestamp = 0;
	private boolean _inited = false;

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

	public void setValue(String key, String value)
	{
		if (key.equals("timestamp"))
		{
			this._selectedTimestamp = Double.parseDouble(value);

			final Date d = new Date((long) this._selectedTimestamp);

			final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss, MMM d");

			final WebkitActivity me = this;

			this.runOnUiThread(new Runnable()
			{
				public void run()
				{
					ActionBar actionBar = me.getSupportActionBar();

					actionBar.setSubtitle(String.format(me.getString(R.string.display_date_item_count), sdf.format(d), me.contentSubtitle()));
				}
			});
		}
	}

	protected void onResume()
	{
		super.onResume();

		this.refresh();
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void refresh()
	{
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

    		case R.id.menu_new_label:
    			if (this._selectedTimestamp == 0)
    			{
    				FragmentManager manager = this.getSupportFragmentManager();

    				final WebkitActivity me = this;

    				DialogFragment dialog = new DialogFragment()
    				{
    					public Dialog onCreateDialog(Bundle savedInstanceState)
    					{
    	    				AlertDialog.Builder builder = new AlertDialog.Builder(me);
    	    				builder.setTitle(R.string.title_missing_timestamp);
    	    				builder.setMessage(R.string.message_missing_timestamp);
    	    				builder.setPositiveButton(R.string.button_ok, new OnClickListener()
    	    				{
    							public void onClick(DialogInterface dialog, int arg)
    							{

    							}
    	    				});

    	    				return builder.create();
    					}
    				};

    				dialog.show(manager, "label_missing");
    			}
    			else
    			{

        			Intent labelIntent = new Intent(this, LabelActivity.class);
        			labelIntent.putExtra(LabelActivity.TIMESTAMP, this._selectedTimestamp);
        			labelIntent.putExtra(LabelActivity.LABEL_CONTEXT, this.getIntent().getStringExtra("probe_name"));

        			this.startActivity(labelIntent);
    			}

    			break;

    		case R.id.menu_refresh:
    			this.refresh();

    			break;
    	}

    	return true;
    }
}
