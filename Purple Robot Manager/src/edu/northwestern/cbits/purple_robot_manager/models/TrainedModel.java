package edu.northwestern.cbits.purple_robot_manager.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public abstract class TrainedModel extends Model 
{
	protected Uri _source = null;
	protected String _sourceHash = null;
	protected boolean _inited = false;
	protected String _name = null;

	protected double _accuracy = 0.0;
	private long _lastCheck = 0;

	public Uri uri() 
	{
		return this._source;
	}

	public TrainedModel(final Context context, Uri uri) 
	{
		this._source = uri;
		this._sourceHash = EncryptionManager.getInstance().createHash(context, uri.toString());
		
		final TrainedModel me = this;
		
		Runnable r = new Runnable()
		{
			public void run() 
			{
				String hash = EncryptionManager.getInstance().createHash(context, me._source.toString());
				
				SharedPreferences prefs = Probe.getPreferences(context);

				File internalStorage = context.getFilesDir();

				if (prefs.getBoolean("config_external_storage", false))
					internalStorage = context.getExternalFilesDir(null);

				if (internalStorage != null && !internalStorage.exists())
					internalStorage.mkdirs();

				File modelsFolder = new File(internalStorage, "persisted_models");

				if (modelsFolder != null && !modelsFolder.exists())
					modelsFolder.mkdirs();
				
				String contents = null;
				File cachedModel = new File(modelsFolder, hash);
				
				try 
				{
					contents = FileUtils.readFileToString(cachedModel);
				}
				catch (IOException e) 
				{

				}
				
				try 
				{
					URL u = new URL(me._source.toString());

			        BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
			        
			        StringBuffer sb = new StringBuffer();
			        
			        String inputLine = null;
			        
			        while ((inputLine = in.readLine()) != null)
			        	sb.append(inputLine);

			        in.close();
			        
			        contents = sb.toString();
				} 
				catch (MalformedURLException e) 
				{
					LogManager.getInstance(context).logException(e);
				} 
				catch (IOException e) 
				{
					LogManager.getInstance(context).logException(e);
				} 
				
				if (contents != null)
				{
					try
					{
				        JSONObject json = new JSONObject(contents);
				        
				        me._name = json.getString("class");
				        me._accuracy = json.getDouble("accuracy");
				        
				        me.generateModel(context, json.get("model"));

				        FileUtils.writeStringToFile(cachedModel, contents);

				        me._inited = true;
					}
					catch (JSONException e) 
					{
						LogManager.getInstance(context).logException(e);
					} 
					catch (IOException e) 
					{
						LogManager.getInstance(context).logException(e);
					}
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
	
	public String name(Context context) 
	{
		return this._source.toString();
	}
	
	public void predict(final Context context, final HashMap<String, Object> snapshot) 
	{
		if (this._inited == false || this.enabled(context) == false)
			return;
		
		long now = System.currentTimeMillis();
		
		if (now - this._lastCheck < 1000)
		{
			this._lastCheck = now;
			
			return;
		}

		final TrainedModel me = this;
		
		Runnable r = new Runnable()
		{
			public void run() 
			{
				Object value = me.evaluateModel(context, snapshot);

				Log.e("PR", "GOT PREDICTION: " + value);
				
				if (value == null)
				{
					
				}
				else if (value instanceof Double)
				{
					Double doubleValue = (Double) value;

					me.transmitPrediction(context, doubleValue.doubleValue(), me._accuracy);
				}
				else
					me.transmitPrediction(context, value.toString(), me._accuracy);
			}
		};
		
		Thread t = new Thread(r);
		t.start();
	}

	protected abstract void generateModel(Context context, Object model); 
	protected abstract Object evaluateModel(Context context, HashMap<String, Object> snapshot);
}
