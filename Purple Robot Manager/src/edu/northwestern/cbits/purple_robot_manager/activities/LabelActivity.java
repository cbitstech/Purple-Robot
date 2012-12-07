package edu.northwestern.cbits.purple_robot_manager.activities;

import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import edu.northwestern.cbits.purple_robot_manager.R;

public class LabelActivity extends Activity
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

        getWindow().setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

	protected void onResume()
	{
		super.onResume();

        final TextView messageText = (TextView) this.findViewById(R.id.text_label_message);

        Date d = new Date((long) this._timestamp);

        this.setTitle(d.toString());

        messageText.setText(R.string.text_label_instruction);

        Button confirmButton = (Button) this.findViewById(R.id.button_dialog_confirm);
        Button cancelButton = (Button) this.findViewById(R.id.button_dialog_cancel);

        final LabelActivity me = this;

        confirmButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				Log.e("PRM", "LABEL THIS");

				me.finish();
			}
        });

        cancelButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				me.finish();
			}
        });
    }
}
