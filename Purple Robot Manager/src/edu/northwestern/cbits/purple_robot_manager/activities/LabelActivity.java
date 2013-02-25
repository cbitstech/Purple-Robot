package edu.northwestern.cbits.purple_robot_manager.activities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import edu.northwestern.cbits.purple_robot_manager.R;

@SuppressLint("SimpleDateFormat")
public class LabelActivity extends SherlockFragmentActivity
{
	public static final String TIMESTAMP = "LABEL TIMESTAMP";
	public static final String LABEL_CONTEXT = "LABEL_CONTEXT";
	private double _timestamp = 0;
	private String _labelContext = null;

	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle extras = this.getIntent().getExtras();

        this._timestamp = extras.getDouble(LabelActivity.TIMESTAMP);
        this._labelContext = extras.getString(LabelActivity.LABEL_CONTEXT);

        this.setContentView(R.layout.layout_label_activity);

        getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

	protected void onResume()
	{
		super.onResume();

		ActionBar actionBar = this.getSupportActionBar();

        final TextView contextText = (TextView) this.findViewById(R.id.text_label_context);
        contextText.setText(String.format(this.getString(R.string.label_context), this._labelContext));

        Date d = new Date((long) this._timestamp);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss, MMM d");

        actionBar.setSubtitle(sdf.format(d));
        actionBar.setTitle(R.string.title_confirm_label);
    }

	public boolean onCreateOptionsMenu(Menu menu)
	{
        MenuInflater inflater = this.getSupportMenuInflater();
        inflater.inflate(R.menu.menu_label_activity, menu);

        return true;
	}

    @SuppressLint("ValidFragment")
	public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
    	{
    		case R.id.menu_accept_label:
    			EditText keyText = (EditText) this.findViewById(R.id.text_label_text);
    			EditText valueText = (EditText) this.findViewById(R.id.text_value_text);

    			String key = keyText.getText().toString();
    			String value = valueText.getText().toString();

    			if (key != null && value != null && key.length() > 0 && value.length() > 0)
    			{
    				Bundle bundle = new Bundle();
    				bundle.putString("PROBE", "edu.northwestern.cbits.purple_robot_manager.Label");
    				bundle.putDouble("TIMESTAMP", this._timestamp / 1000);

    				bundle.putString("KEY", key);
    				bundle.putString("VALUE", value);

    				UUID uuid = UUID.randomUUID();
    				bundle.putString("GUID", uuid.toString());

    				LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this);
    				Intent intent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
    				intent.putExtras(bundle);

    				localManager.sendBroadcast(intent);

        			this.finish();
    			}
    			else
    			{
    				FragmentManager manager = this.getSupportFragmentManager();

    				final LabelActivity me = this;

    				DialogFragment dialog = new DialogFragment()
    				{
    					public Dialog onCreateDialog(Bundle savedInstanceState)
    					{
    	    				AlertDialog.Builder builder = new AlertDialog.Builder(me);
    	    				builder.setTitle(R.string.title_missing_label);
    	    				builder.setMessage(R.string.message_missing_label);
    	    				builder.setPositiveButton(R.string.button_ok, new OnClickListener()
    	    				{
								public void onClick(DialogInterface dialog, int arg)
								{

								}
    	    				});

    	    				return builder.create();
    					}
    				};

    				dialog.show(manager, "label_error");
    			}

    			break;

    		case R.id.menu_cancel:
    			this.finish();

    			break;
		}

    	return true;
    }


}
