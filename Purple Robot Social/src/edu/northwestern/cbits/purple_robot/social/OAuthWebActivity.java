package edu.northwestern.cbits.purple_robot.social;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.R;

public class OAuthWebActivity extends ActionBarActivity
{
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.getWindow().requestFeature(Window.FEATURE_PROGRESS);

        this.setContentView(R.layout.layout_web_activity);
    }

	@SuppressLint({ "SetJavaScriptEnabled", "DefaultLocale" })
	protected void onResume()
	{
		super.onResume();
		
        WebView webView = (WebView) this.findViewById(R.id.webview);
        
        Uri uri = this.getIntent().getData();
        
        if (uri != null && uri.getScheme() != null && uri.getScheme().toLowerCase(Locale.ENGLISH).startsWith("http"))
        {
        	final OAuthWebActivity me = this;
        	
        	WebSettings settings = webView.getSettings();
        	
        	settings.setJavaScriptEnabled(true);
        	settings.setBuiltInZoomControls(true);

        	webView.setWebChromeClient(new WebChromeClient() 
        	{
        		public void onProgressChanged(WebView view, int progress) 
        		{
        			me.setProgress(progress * 1000);
        		}
        		
        		public void onCloseWindow (WebView window)
        		{
        			me.finish();
        		}
        		
        		public void onReceivedTitle (WebView view, String title)
        		{
        			me.getSupportActionBar().setTitle(title);
        		}
        	});
        	
        	webView.setWebViewClient(new WebViewClient() 
        	{
        		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) 
        		{
        			Toast.makeText(me, description, Toast.LENGTH_LONG).show();
        		}
        		
    			public boolean shouldOverrideUrlLoading (WebView view, String url)
        		{
        			boolean oauth = false;
        			
        			if (url.toLowerCase(Locale.getDefault()).startsWith("http://purple.robot.com/oauth"))
        				oauth = true;
        			else if (url.toLowerCase(Locale.getDefault()).startsWith("http://tech.cbits.northwestern.edu/oauth"))
        				oauth = true;
        			else if (url.toLowerCase(Locale.getDefault()).startsWith("http://pr-oauth/oauth"))
        				oauth = true;
        			
        			if (oauth)
        			{
        				Intent intent = new Intent(me, OAuthActivity.class);
        				intent.setData(Uri.parse(url));
        				intent.putExtras(new Bundle());
        				
        				me.startActivity(intent);
        				
        				me.finish();
        			}
        			
        			return oauth;
        		}
        	});

            webView.loadUrl(uri.toString());
        }
        else
			Toast.makeText(this, R.string.error_missing_uri, Toast.LENGTH_LONG).show();
	}
	
	public void onBackPressed ()
	{
        WebView webView = (WebView) this.findViewById(R.id.webview);
        
        if (webView.canGoBack())
        	webView.goBack();
        else
        	super.onBackPressed();
	}
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu_oauth_web_activity, menu);

        return true;
	}

	public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
    	{
    		case R.id.menu_close:
    			this.finish();

    			break;
		}

    	return true;
    }
}
