package edu.northwestern.cbits.purple_robot_manager.activities.probes;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;

import com.facebook.Session;
import com.facebook.SessionDefaultAudience;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.FacebookProbe;

public class FacebookLoginActivity extends ActionBarActivity 
{
	private boolean _inited = false;
	
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_facebook_activity);
    }

	protected void onResume()
	{
		super.onResume();
		
		if (this._inited)
			return;
		
		this._inited = true;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor e = prefs.edit();
		e.remove(FacebookProbe.TOKEN);
		e.commit();

		final FacebookLoginActivity me = this;
        
		Session.Builder builder = new Session.Builder(this);
		
		Session session = builder.setApplicationId("266981220119291").build();
		
		session.addCallback(new Session.StatusCallback() 
		{
	        public void call(Session session, SessionState state, Exception exception) 
	        {
	        	if (SessionState.OPENED == state || SessionState.OPENED_TOKEN_UPDATED == state)
		        	me.onSessionStateChange(session, state, exception);
	        	else
	        		session.addCallback(this);
	        }
		});
				
		Session.OpenRequest request = new Session.OpenRequest(this);
		request.setPermissions("read_stream");
		request.setLoginBehavior(SessionLoginBehavior.SSO_WITH_FALLBACK);
		request.setDefaultAudience(SessionDefaultAudience.ONLY_ME);
		
		Session.setActiveSession(session);
		
		session.openForRead(request);
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
		        	session = Session.getActiveSession();

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
		
		if (Session.getActiveSession() != null)
	  		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}

	private void go(Session session) 
	{
		final FacebookLoginActivity me = this;

		String token = session.getAccessToken();
		
		if (token != null && token.trim().length() > 0)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);	
			Editor e = prefs.edit();
			e.putString(FacebookProbe.TOKEN, token);
			e.commit();

			SanityManager sanity = SanityManager.getInstance(this);
			
			sanity.clearAlert(this.getString(R.string.title_facebook_check));

			sanity.refreshState();
			
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			
			builder = builder.setTitle(R.string.title_facebook_success);
			builder = builder.setMessage(R.string.message_facebook_success);
			builder = builder.setPositiveButton(R.string.confirm_facebook_success, new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int which) 
				{
					me.finish();
				}
			});
			
			builder.create().show();
		}
		else
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			
			builder = builder.setTitle(R.string.title_facebook_failure);
			builder = builder.setMessage(R.string.message_facebook_failure);
			builder = builder.setPositiveButton(R.string.confirm_facebook_success, new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int which) 
				{
					me.finish();
				}
			});

			builder = builder.setNegativeButton(R.string.confirm_facebook_try_again, new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int which) 
				{
					me.finish();
					
					Intent intent = new Intent(me, FacebookLoginActivity.class);
					me.startActivity(intent);
				}
			});
			
			builder.create().show();
		}
    }
}
