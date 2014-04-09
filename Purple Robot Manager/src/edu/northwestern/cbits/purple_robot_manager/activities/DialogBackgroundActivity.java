package edu.northwestern.cbits.purple_robot_manager.activities;

import java.util.HashMap;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class DialogBackgroundActivity extends Activity 
{
	private boolean _started = false;
	
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_dialog_background_activity);
    }
	
	protected void onResume()
	{
		super.onResume();
		
		if (this._started)
			this.finish();
		else
		{
			Intent intent = new Intent(this, DialogActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			
			intent.putExtras(this.getIntent().getExtras());
		
			this.startActivity(intent);
			
			this._started = true;
		}
	}
	
	protected void onUserLeaveHint()
	{
        Intent intent = this.getIntent();

        final String title = intent.getStringExtra(DialogActivity.DIALOG_TITLE);
        final String message = intent.getStringExtra(DialogActivity.DIALOG_MESSAGE);

        HashMap <String, Object> payload = new HashMap<String, Object>();
		payload.put("title", title);
		payload.put("message", message);
		payload.put("home_button", Boolean.TRUE);
		
		LogManager.getInstance(this).log("dialog_dismissed", payload);

		this.finish();
	}
}
