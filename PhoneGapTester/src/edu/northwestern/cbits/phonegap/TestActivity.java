package edu.northwestern.cbits.phonegap;

import org.apache.cordova.DroidGap;

import android.os.Bundle;
import android.util.Log;

public class TestActivity extends DroidGap 
{
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		super.loadUrl("file:///android_asset/www/index.html");
	}

    public void onConsoleMessage(String message, int lineNumber, String sourceID)
    {       
    	Log.e("JS Console", sourceID + ": Line " + Integer.toString(lineNumber) + " : " + message);
    }
}
