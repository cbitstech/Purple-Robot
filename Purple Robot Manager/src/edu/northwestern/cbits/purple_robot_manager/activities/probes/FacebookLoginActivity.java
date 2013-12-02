package edu.northwestern.cbits.purple_robot_manager.activities.probes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;

import com.facebook.Session;
import com.facebook.SessionState;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.FacebookProbe;

public class FacebookLoginActivity extends ActionBarActivity 
{
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_facebook_activity);
    }

	protected void onResume()
	{
		super.onResume();

		final FacebookLoginActivity me = this;
		
		Session.openActiveSession(this, true, new Session.StatusCallback() 
		{
	        public void call(Session session, SessionState state, Exception exception) 
	        {
	        	me.onSessionStateChange(session, state, exception);
	        }
		});
	}

	private void onSessionStateChange(Session session, SessionState state, Exception exception) 
	{
		if (state.isOpened())
			this.go(session);

		final FacebookLoginActivity me = this;
		
		if (Session.getActiveSession() == null || Session.getActiveSession().isClosed()) 
		{
			Session.openActiveSession(this, true, new Session.StatusCallback() 
			{
		        public void call(Session session, SessionState state, Exception exception) 
		        {
		        	me.onSessionStateChange(session, state, exception);
		        }
			});
		}
		else
			this.go(session);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
          super.onActivityResult(requestCode, resultCode, data);

          Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}

	private void go(Session session) 
	{
		String token = session.getAccessToken();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);	
		Editor e = prefs.edit();
		e.putString(FacebookProbe.TOKEN, token);
		e.commit();

		SanityManager sanity = SanityManager.getInstance(this);
		
		sanity.clearAlert(this.getString(R.string.title_facebook_check));

		sanity.refreshState();

		this.finish();
    }
}
