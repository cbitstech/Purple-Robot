package edu.northwestern.cbits.purple_robot_manager.activities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;

@SuppressLint("SimpleDateFormat")
public class LabelActivity extends SherlockFragmentActivity
{
	public static final String TIMESTAMP = "LABEL TIMESTAMP";
	public static final String LABEL_CONTEXT = "LABEL_CONTEXT";
	public static final String LABEL_KEY = "LABEL_KEY";
	public static final String LABEL_DEFINITIONS = "LABEL_DEFINITIONS";

	private double _timestamp = 0;
	private String _labelContext = null;
	
	private HashMap<String, Object> _values = new HashMap<String, Object>();

	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_label_activity);

        getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }
	
	private String[] savedLabels()
	{
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String[] labels = new String[0];
        
        try 
        {
			JSONArray jsonLabels = new JSONArray(prefs.getString("list_label_values", "['Activity', 'Location', 'Social Context']"));
			
			labels = new String[jsonLabels.length()];
			
			for (int i = 0; i < jsonLabels.length(); i++)
			{
				labels[i] = jsonLabels.getString(i);
			}
		}
        catch (JSONException e) 
        {
        	LogManager.getInstance(this).logException(e);
		}
        
        return labels;
	}

	private String[] savedValues()
	{
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String[] values = new String[0];
        
        try 
        {
			JSONArray jsonValues = new JSONArray(prefs.getString("list_value_values", "[]"));
			
			values = new String[jsonValues.length()];
			
			for (int i = 0; i < jsonValues.length(); i++)
			{
				values[i] = jsonValues.getString(i);
			}
		}
        catch (JSONException e) 
        {
        	LogManager.getInstance(this).logException(e);
		}
        
        return values;
	}
	
	private void saveLabels(String[] labels) 
	{
		JSONArray array = new JSONArray();
		
		for (String label : labels)
			array.put(label);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor e = prefs.edit();
		
		e.putString("list_label_values", array.toString());
		
		e.commit();
	}

	private void saveValues(String[] values) 
	{
		JSONArray array = new JSONArray();
		
		for (String value : values)
			array.put(value);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor e = prefs.edit();
		
		e.putString("list_value_values", array.toString());
		
		e.commit();
	}
	
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		
		this.setIntent(intent);
	}

	protected void onResume()
	{
		super.onResume();
		
        Bundle extras = this.getIntent().getExtras();
        
        if (extras.containsKey(LabelActivity.TIMESTAMP))
        	this._timestamp = extras.getDouble(LabelActivity.TIMESTAMP);
        else
        	this._timestamp = System.currentTimeMillis();
        
        if (extras.containsKey(LabelActivity.LABEL_CONTEXT))
        	this._labelContext = extras.getString(LabelActivity.LABEL_CONTEXT);
        else
        	this._labelContext = this.getString(R.string.label_unknown_context);

		ActionBar actionBar = this.getSupportActionBar();

        final TextView contextText = (TextView) this.findViewById(R.id.text_label_context);
        contextText.setText(String.format(this.getString(R.string.label_context), this._labelContext));

        Date d = new Date((long) this._timestamp);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss, MMM d");

		HashMap <String, Object> payload = new HashMap<String, Object>();

        final AutoCompleteTextView value = (AutoCompleteTextView) this.findViewById(R.id.text_value_text);
        final AutoCompleteTextView label = (AutoCompleteTextView) this.findViewById(R.id.text_label_text);

		if (extras.containsKey(LabelActivity.LABEL_DEFINITIONS))
		{
			label.setVisibility(View.GONE);
			value.setVisibility(View.GONE);

			LinearLayout labelLayout = (LinearLayout) this.findViewById(R.id.layout_label);
			
			labelLayout.removeAllViews();

			Bundle definitions = extras.getBundle(LabelActivity.LABEL_DEFINITIONS);
			
			HashMap<String, String> sortedMap = new HashMap<String, String>();			
			
			for (String key : definitions.keySet())
			{
				Bundle field = definitions.getBundle(key);

				String fieldName = field.getString("name");
				double weight = field.getDouble("weight", 0.0);
				
				sortedMap.put(weight + "_" + fieldName, fieldName);
			}

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			
			ArrayList<String> keyList = new ArrayList<String>();
			
			for (String key : sortedMap.keySet())
			{
				if (key != null)
					keyList.add(key);
			}
			
			Collections.sort(keyList);
			
			for (String field : keyList)
			{
				field = sortedMap.get(field);
				
				Bundle fieldDef = definitions.getBundle(field);

				final TextView fieldName = new TextView(this);
				fieldName.setText(field);
				
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
				params.setMargins(8, 24, 8, 8);
				fieldName.setLayoutParams(params);
				
				labelLayout.addView(fieldName);
				
				String fieldType = fieldDef.getString("type");
				
				final LabelActivity me = this;
				final String fieldLabel = field;
				
				if (fieldType.equalsIgnoreCase("real"))
				{
					SeekBar seekBar = new SeekBar(this);
					
					params = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
					params.setMargins(8, 8, 8, 8);
					seekBar.setLayoutParams(params);
					
					final double min = fieldDef.getDouble("min", 1.0);
					final double max = fieldDef.getDouble("max", 10.0);
					double step = fieldDef.getDouble("step", 1.0);
					
					float lastValue = prefs.getFloat("label_field_" + field, (float) (((min + max) / 2) - min));
					
					seekBar.setMax((int) (max - min));
					seekBar.setKeyProgressIncrement((int) step);
					
					seekBar.setProgress((int) lastValue);
					
					me._values.put(fieldLabel, Float.valueOf(lastValue));
					
					seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
					{
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
						{
							fieldName.setText(fieldLabel + ": " + ((int) (min + progress)));
							
							me._values.put(fieldLabel, Float.valueOf((float) (min + progress)));
						}

						public void onStartTrackingTouch(SeekBar arg0) 
						{

						}

						public void onStopTrackingTouch(SeekBar seekBar) 
						{
							
						}
					});

					labelLayout.addView(seekBar);
				}
				else if (fieldType.equalsIgnoreCase("nominal"))
				{
					if (fieldDef.containsKey("values") && fieldDef.getStringArrayList("values").size() > 0)
					{
						ScrollView scroller = new ScrollView(this);
						params = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 0, 1);
						scroller.setLayoutParams(params);
						
						String lastValue = prefs.getString("label_field_" + field, null);
						
						RadioGroup radios = new RadioGroup(this);
						
						for (String nominalValue : fieldDef.getStringArrayList("values"))
						{
							RadioButton radio = new RadioButton(this);
							radio.setText(nominalValue);
							
							radios.addView(radio);
							
							if (lastValue != null && nominalValue.equalsIgnoreCase(lastValue))
							{
								if (radio.isChecked() == false)
									radio.toggle();
							}
						}
						
						if (lastValue != null)
							me._values.put(fieldLabel, lastValue);
						
						radios.setOnCheckedChangeListener(new OnCheckedChangeListener()
						{
							public void onCheckedChanged(RadioGroup radios, int checkId) 
							{
								if (checkId != -1)
								{
									RadioButton button = (RadioButton) radios.findViewById(checkId);

									me._values.put(fieldLabel, button.getText());
								}
								else
									me._values.remove(fieldLabel);
							}
						});
						
						scroller.addView(radios);
						labelLayout.addView(scroller);
					}
				}
				else if (fieldType.equalsIgnoreCase("text"))
				{
					AutoCompleteTextView textField = new AutoCompleteTextView(this);

					if (fieldDef.containsKey("placeholder"))
						textField.setHint(fieldDef.getString("placeholder"));

					String valuesList = prefs.getString("label_field_" + field + "_saved_values", "");
					
					ArrayList<String> savedValues = new ArrayList<String>();
					
					for (String saved : valuesList.split(";"))
						savedValues.add(saved);
					
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, savedValues);
					textField.setAdapter(adapter);
					textField.setThreshold(1);
					
					params = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
					params.setMargins(8, 8, 8, 8);
					textField.setLayoutParams(params);
					
					textField.addTextChangedListener(new TextWatcher()
					{
						public void afterTextChanged(Editable s) 
						{
							me._values.put(fieldLabel, s.toString());
						}

						public void beforeTextChanged(CharSequence s, int start, int count, int after) 
						{

						}

						public void onTextChanged(CharSequence s, int start, int before, int count) 
						{

						}
					});

					labelLayout.addView(textField);
				}
			}
		}
		else
		{
			label.setVisibility(View.VISIBLE);
			value.setVisibility(View.VISIBLE);
			
	        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, this.savedLabels());

	        label.setAdapter(adapter);
	        
			if (extras.containsKey(LabelActivity.LABEL_KEY))
	        {
	        	label.setText(extras.getString(LabelActivity.LABEL_KEY));
	    		payload.put("label", extras.getString(LabelActivity.LABEL_KEY));
	        }
	        
	        ArrayAdapter<String> valueAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, this.savedValues());
	        value.setAdapter(valueAdapter);
	        
	        value.requestFocus();
		}

        actionBar.setSubtitle(sdf.format(d));
        actionBar.setTitle(R.string.title_confirm_label);
        
		payload.put("label_time", this._timestamp);
		payload.put("label_context", this._labelContext);
		
		LogManager.getInstance(this).log("label_prompt", payload);
    }

	protected void onPause()
	{
		super.onPause();

        Bundle extras = this.getIntent().getExtras();

        HashMap <String, Object> payload = new HashMap<String, Object>();

        if (extras.containsKey(LabelActivity.LABEL_KEY))
    		payload.put("label", extras.getString(LabelActivity.LABEL_KEY));

        payload.put("label_time", this._timestamp);
		payload.put("label_context", this._labelContext);
		
		LogManager.getInstance(this).log("label_dismissed", payload);
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
    	        Bundle extras = this.getIntent().getExtras();
    	        
    			if (extras.containsKey(LabelActivity.LABEL_DEFINITIONS))
    			{
        	        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        	        Editor e = prefs.edit();

        	        for (String key : this._values.keySet())
    				{
    					Object value = this._values.get(key);
    					
	    				JavaScriptEngine js = new JavaScriptEngine(this);
	    				js.emitReading(key, value.toString());
	
	    				HashMap <String, Object> payload = new HashMap<String, Object>();
	
	    				payload.put("label_time", this._timestamp);
	    				payload.put("label", key);
	    				payload.put("label_value", value.toString());
	    				
	    				LogManager.getInstance(this).log("label_submit", payload);
	    				
	    				if (value instanceof Float)
	    					e.putFloat("label_field_" + key, ((Float) value).floatValue());
	    				else
	    				{
	    					e.putString("label_field_" + key, value.toString());
	    					
	    					String valuesList = prefs.getString("label_field_" + key + "_saved_values", "");
	    					
	    					ArrayList<String> savedValues = new ArrayList<String>();
	    					
	    					for (String saved : valuesList.split(";"))
	    						savedValues.add(saved);
	    					
	    					savedValues.remove(value.toString());
	    					savedValues.add(0, value.toString());
	    					
	    					StringBuffer sb = new StringBuffer();
	    					
	    					for (String savedValue : savedValues)
	    					{
	    						if (sb.length() > 0)
	    							sb.append(";");
	    						
	    						sb.append(savedValue);
	    					}
	    					
	    					e.putString("label_field_" + key + "_saved_values", sb.toString());
	    				}
    				}
    				
        	        e.commit();
        	        
    				this.finish();
    			}
    			else
    			{
	    			EditText keyText = (EditText) this.findViewById(R.id.text_label_text);
	    			EditText valueText = (EditText) this.findViewById(R.id.text_value_text);
	
	    			String key = keyText.getText().toString();
	    			String value = valueText.getText().toString();
	
	    			if (key != null && value != null && key.length() > 0 && value.length() > 0)
	    			{
	    				JavaScriptEngine js = new JavaScriptEngine(this);
	    				js.emitReading(key, value);
	    				
	    				List<String> labels = new ArrayList<String>(Arrays.asList(this.savedLabels()));
	    				labels.remove(key);
	    				labels.add(0, key);
	    				String[] labelsArray = labels.toArray(new String[0]);
	    				this.saveLabels(labelsArray);
	
	    				List<String> values = new ArrayList<String>(Arrays.asList(this.savedValues()));
	    				values.remove(value);
	    				values.add(0, value);
	    				String[] valuesArray = values.toArray(new String[0]);
	    				this.saveValues(valuesArray);
	
	    				HashMap <String, Object> payload = new HashMap<String, Object>();
	
	    				payload.put("label_time", this._timestamp);
	    				payload.put("label", key);
	    				payload.put("label_value", value);
	    				
	    				LogManager.getInstance(this).log("label_submit", payload);
	
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
    			}
    			
    			break;

    		case R.id.menu_cancel:
    			this.finish();

    			break;
		}

    	return true;
    }
}
