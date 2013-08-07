package edu.northwestern.cbits.purple_robot_manager.models;

import java.security.SecureRandom;
import java.util.HashMap;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class NoiseModel extends Model 
{
	protected static final String NOISE_VALUE = "NOISE_VALUE";

	public String getPreferenceKey() 
	{
		return "noise";
	}

	public String title(Context context) 
	{
		return context.getString(R.string.title_noise_model);
	}

	public String summary(Context context) 
	{
		return context.getString(R.string.summary_noise_model_desc);
	}

	public void predict(final Context context, HashMap<String, Object> snapshot) 
	{
		final NoiseModel me = this;
		
		Runnable r = new Runnable()
		{
			public void run() 
			{
				SecureRandom random = new SecureRandom();
				me.transmitPrediction(context, random.nextDouble(), random.nextDouble());
			}
		};
		
		Thread t = new Thread(r);
		t.start();
	}

	public String name(Context context) 
	{
		return "edu.northwestern.cbits.purple_robot_manager.models.NoiseModel";
	}
	
	public String modelType() 
	{
		return "noise";
	}
}
