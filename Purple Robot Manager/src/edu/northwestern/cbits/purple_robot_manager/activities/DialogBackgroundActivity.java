package edu.northwestern.cbits.purple_robot_manager.activities;

import edu.northwestern.cbits.purple_robot_manager.DialogActivity;
import edu.northwestern.cbits.purple_robot_manager.R;
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
			
			intent.putExtras(this.getIntent().getExtras());
		
			this.startActivity(intent);
			
			this._started = true;
		}
	}
}
