package edu.northwestern.cbits.purple_robot_manager.probes.features;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;

public abstract class XYZBasicFrequencyFeature extends XYZContinuousProbeFeature 
{
	private long _lastCheck = 0;
	private boolean _running = false;
	
	protected void analyzeBuffers(final Context context) 
	{
		final long now = System.currentTimeMillis();
		
		if (now - this._lastCheck > 10000 && this._running == false)
		{
			this._lastCheck = now;
			
			final XYZBasicFrequencyFeature me = this;
			
			Runnable r = new Runnable()
			{
				public void run() 
				{
					me._running = true;

					Bundle data = new Bundle();

					data.putDouble("TIMESTAMP", now / 1000);
					data.putString("PROBE", me.name(context));
					
					double maxTime = Double.MIN_VALUE;
					double minTime = Double.MAX_VALUE;
					
					for (int i = 0; i < XYZContinuousProbeFeature.BUFFER_SIZE; i++)
					{
						if (me.timestamp[i] > maxTime)
							maxTime = me.timestamp[i];

						if (me.timestamp[i] < minTime)
							minTime = me.timestamp[i];
					}

					data.putInt("BUFFER_SIZE", XYZContinuousProbeFeature.BUFFER_SIZE);
					data.putDouble("FREQUENCY", ((double) XYZContinuousProbeFeature.BUFFER_SIZE) / ((maxTime - minTime) / 1000));
					data.putDouble("DURATION", ((double) ((maxTime - minTime) / 1000.0)));

					me.transmitData(context, data);

					me._running = false;
				}
			};
			
			Thread t = new Thread(r);
			
			t.start();
		}
	}

/*	public String summarizeValue(Context context, Bundle bundle)
	{
		double x = bundle.getDouble("X_FREQUENCY");
		double y = bundle.getDouble("Y_FREQUENCY");
		double z = bundle.getDouble("Z_FREQUENCY");

		return String.format(context.getResources().getString(R.string.summary_accelerator_frequency_feature), x, y, z);
	}
*/
	
	protected abstract String featureKey();
	protected abstract String summary(Context context); 
	public abstract String name(Context context);
	public abstract String source(Context context); 
	public abstract String title(Context context); 
}
