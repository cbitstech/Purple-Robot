package edu.northwestern.cbits.purple_robot_manager.probes.features;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;

public abstract class XYZBasicStatisticsFeature extends XYZContinuousProbeFeature 
{
	protected void analyzeBuffers(Context context) 
	{
		long now = System.currentTimeMillis();
		
		Bundle data = new Bundle();

		data.putDouble("TIMESTAMP", now / 1000);
		data.putString("PROBE", this.name(context));
		
		double maxTime = Double.MIN_VALUE;
		double minTime = Double.MAX_VALUE;
		
		DescriptiveStatistics xStats = new DescriptiveStatistics(XYZContinuousProbeFeature.BUFFER_SIZE);
		DescriptiveStatistics yStats = new DescriptiveStatistics(XYZContinuousProbeFeature.BUFFER_SIZE);
		DescriptiveStatistics zStats = new DescriptiveStatistics(XYZContinuousProbeFeature.BUFFER_SIZE);
		
		for (int i = 0; i < XYZContinuousProbeFeature.BUFFER_SIZE; i++)
		{
			xStats.addValue(this.x[i]);
			yStats.addValue(this.y[i]);
			zStats.addValue(this.z[i]);

			if (this.timestamp[i] > maxTime)
				maxTime = this.timestamp[i];

			if (this.timestamp[i] < minTime)
				minTime = this.timestamp[i];
		}
		
		data.putDouble("X_MIN", xStats.getMin());
		data.putDouble("X_MAX", xStats.getMax());
		data.putDouble("X_MEAN", xStats.getMean());
		data.putDouble("X_STD_DEV", xStats.getStandardDeviation());
		data.putDouble("X_RMS", Math.sqrt(xStats.getSumsq() / XYZContinuousProbeFeature.BUFFER_SIZE));

		data.putDouble("Y_MIN", yStats.getMin());
		data.putDouble("Y_MAX", yStats.getMax());
		data.putDouble("Y_MEAN", yStats.getMean());
		data.putDouble("Y_STD_DEV", yStats.getStandardDeviation());
		data.putDouble("Y_RMS", Math.sqrt(yStats.getSumsq() / XYZContinuousProbeFeature.BUFFER_SIZE));

		data.putDouble("Z_MIN", zStats.getMin());
		data.putDouble("Z_MAX", zStats.getMax());
		data.putDouble("Z_MEAN", zStats.getMean());
		data.putDouble("Z_STD_DEV", zStats.getStandardDeviation());
		data.putDouble("Z_RMS", Math.sqrt(zStats.getSumsq() / XYZContinuousProbeFeature.BUFFER_SIZE));

		data.putInt("BUFFER_SIZE", XYZContinuousProbeFeature.BUFFER_SIZE);
		data.putDouble("FREQUENCY", ((double) XYZContinuousProbeFeature.BUFFER_SIZE) / ((maxTime - minTime) * 1000));
		data.putDouble("DURATION", ((double) ((maxTime - minTime) * 1000)));

		this.transmitData(context, data);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		double x = bundle.getDouble("X_STD_DEV");
		double y = bundle.getDouble("Y_STD_DEV");
		double z = bundle.getDouble("Z_STD_DEV");

		return String.format(context.getResources().getString(R.string.summary_accelerator_statistics_feature), x, y, z);
	}

	protected abstract String featureKey();
	protected abstract String summary(Context context); 
	public abstract String name(Context context);
	public abstract String source(Context context); 
	public abstract String title(Context context); 
}
