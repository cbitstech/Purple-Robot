package edu.northwestern.cbits.purple_robot_manager.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import android.content.Context;
import android.net.Uri;

public abstract class TrainedModel extends Model 
{
	protected Uri _source = null;
	protected String _sourceHash = null;
	protected boolean _inited = false;
	protected String _name = null;
	protected double _accuracy = 0.0;
	
	public TrainedModel(final Context context, Uri uri) 
	{
		this._source = uri;
		this._sourceHash = EncryptionManager.getInstance().createHash(context, uri.toString());
		
		final TrainedModel me = this;
		
		Runnable r = new Runnable()
		{
			public void run() 
			{
				try 
				{
					URL u = new URL(me._source.toString());

			        BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
			        
			        StringBuffer sb = new StringBuffer();
			        
			        String inputLine = null;
			        
			        while ((inputLine = in.readLine()) != null)
			        	sb.append(inputLine);

			        in.close();
			        
			        JSONObject json = new JSONObject(sb.toString());
			        
			        me._name = json.getString("class");
			        me._accuracy = json.getDouble("accuracy");
			        
			        me.generateModel(context, json.getString("model"));
			        
			        me._inited = true;
				} 
				catch (MalformedURLException e) 
				{
					LogManager.getInstance(context).logException(e);
				} 
				catch (IOException e) 
				{
					LogManager.getInstance(context).logException(e);
				} 
				catch (JSONException e) 
				{
					LogManager.getInstance(context).logException(e);
				}
			}
		};
		
		Thread t = new Thread(r);
		t.start();
	}
	
	public String title(Context context) 
	{
		return this._name;
	}
	
	public String summary(Context context)
	{
		return context.getString(R.string.summary_model_unknown);
	}

	public String getPreferenceKey() 
	{
		return this._sourceHash;
	}
	
	protected String name(Context context) 
	{
		return this._source.toString();
	}
	
	public void predict(final Context context, final HashMap<String, Object> snapshot) 
	{
		if (this._inited == false)
			return;
		
		final TrainedModel me = this;
		
		Runnable r = new Runnable()
		{
			public void run() 
			{
				Object value = me.evaluateModel(context, snapshot);
				
				if (value == null)
				{
					
				}
				else if (value instanceof Double)
				{
					Double doubleValue = (Double) value;

					me.transmitPrediction(context, doubleValue.doubleValue());
				}
				else
					me.transmitPrediction(context, value.toString());
			}
		};
		
		Thread t = new Thread(r);
		t.start();
	}

	protected abstract void generateModel(Context context, String modelString); 
	protected abstract Object evaluateModel(Context context, HashMap<String, Object> snapshot);
}
