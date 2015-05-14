package edu.northwestern.cbits.purple_robot_manager.activities;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;

public class WebActivity extends AppCompatActivity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        this.getWindow().requestFeature(Window.FEATURE_PROGRESS);

        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_web_activity);
    }

    @SuppressLint(
    { "SetJavaScriptEnabled", "DefaultLocale" })
    protected void onResume()
    {
        super.onResume();

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        WebView webView = (WebView) this.findViewById(R.id.webview);

        final Uri uri = this.getIntent().getData();

        if (uri != null && uri.getScheme() != null && (uri.getScheme().toLowerCase(Locale.ENGLISH).startsWith("http") || uri.getScheme().toLowerCase(Locale.ENGLISH).startsWith("file")))
        {
            final WebActivity me = this;

            WebSettings settings = webView.getSettings();

            settings.setJavaScriptEnabled(true);
            settings.setBuiltInZoomControls(true);

            webView.setWebChromeClient(new WebChromeClient() {
                public void onProgressChanged(WebView view, int progress) {
                    me.setProgress(progress * 1000);
                }

                public void onCloseWindow(WebView window) {
                    me.finish();
                }

                public void onReceivedTitle(WebView view, String title) {
                    me.getSupportActionBar().setTitle(title);
                }
            });

            webView.setWebViewClient(new WebViewClient() {
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Toast.makeText(me, description, Toast.LENGTH_LONG).show();
                }

                public boolean shouldOverrideUrlLoading(WebView view, String url)
                {
                    if (uri.toString().startsWith("file:///android_asset/") && url.toLowerCase().startsWith("http")) {
                        me.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

                        return true;
                    }

                    return false;
                }
            });

            webView.loadUrl(uri.toString());
        }
        else
            Toast.makeText(this, R.string.error_missing_uri, Toast.LENGTH_LONG).show();
    }

    public void onBackPressed()
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
        inflater.inflate(R.menu.menu_web_activity, menu);

        Uri uri = this.getIntent().getData();

        if (uri == null || uri.toString().startsWith("file:///android_asset/"))
            menu.removeItem(R.id.menu_open);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        final int itemId = item.getItemId();

        if (itemId == android.R.id.home)
        {
            this.onUpPressed();
        }
        if (itemId == R.id.menu_close)
        {
            this.finish();
        }
        if (itemId == R.id.menu_open)
        {
            JavaScriptEngine engine = new JavaScriptEngine(this);

            Uri data = this.getIntent().getData();

            if (data != null)
                engine.launchUrl(data.toString());
        }

        return true;
    }

    private void onUpPressed()
    {
        if (this.isTaskRoot())
        {
            Intent intent = new Intent(this, StartActivity.class);
            this.startActivity(intent);
        }

        this.finish();
    }
}
