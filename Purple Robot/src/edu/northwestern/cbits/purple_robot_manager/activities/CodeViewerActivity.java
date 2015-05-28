package edu.northwestern.cbits.purple_robot_manager.activities;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Scanner;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebView;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

public class CodeViewerActivity extends AppCompatActivity
{
    public static final String SOURCE_CODE = "SOURCE_CODE";
    public static final String TITLE = "TITLE";

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_code_activity);

        getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @SuppressWarnings("resource")
    protected void onResume()
    {
        super.onResume();

        this.getSupportActionBar().setTitle(this.getIntent().getStringExtra(CodeViewerActivity.TITLE));

        String code = this.getIntent().getStringExtra(CodeViewerActivity.SOURCE_CODE);

        String language = "javascript";

        if (code.startsWith("(") && code.endsWith(")"))
            language = "scheme";

        if (language.equals("javascript"))
        {
            Context js = Context.enter();
            js.setOptimizationLevel(-1);

            Scriptable scope = js.initStandardObjects();
            ScriptableObject.putProperty(scope, "sourceCode", code);

            AssetManager am = this.getAssets();

            try
            {
                InputStream jsStream = am.open("js/beautify.js");

                Scanner s = new Scanner(jsStream).useDelimiter("\\A");

                String script = "";

                if (s.hasNext())
                {
                    script = s.next();

                    code = js.evaluateString(scope, "var exports = {};" + script + " exports.js_beautify(sourceCode);",
                            "<engine>", 1, null).toString();
                }
            }
            catch (IOException | StackOverflowError e)
            {
                LogManager.getInstance(this).logException(e);
            }
        }

        code = "<!DOCTYPE html><html><body style=\"background-color: #000; color: #00c000;\"><pre>" + code
                + "</pre></body></html>";

        try
        {
            code = URLEncoder.encode(code, "utf-8").replaceAll("\\+", "%20");
        }
        catch (UnsupportedEncodingException e)
        {
            LogManager.getInstance(this).logException(e);
        }

        WebView webView = (WebView) this.findViewById(R.id.code_web_view);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(false);
        webView.loadData(code, "text/html", null);
    }
}
