package edu.northwestern.cbits.purple_robot_manager.probes.features;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import edu.northwestern.cbits.purple_robot_manager.R;
import android.content.Context;
import android.os.Bundle;

public abstract class UnivariateContinuousProbeFeature extends
        ContinuousProbeFeature {
    protected static int BUFFER_SIZE = 1024;

    protected float[] value = new float[BUFFER_SIZE];
    protected double[] timestamp = new double[BUFFER_SIZE];

    protected int index = 0;
    private boolean _filled = false;

    private long _lastCheck = 0;
    private boolean _running = false;

    protected abstract String valueKey();

    protected void analyzeBuffers(final Context context) {
        final long now = System.currentTimeMillis();

        if (now - this._lastCheck > 10000 && this._running == false) {
            this._lastCheck = now;

            final UnivariateContinuousProbeFeature me = this;

            Runnable r = new Runnable() {
                public void run() {
                    me._running = true;

                    Bundle data = new Bundle();

                    data.putDouble("TIMESTAMP", now / 1000);
                    data.putString("PROBE", me.name(context));

                    double maxTime = Double.MIN_VALUE;
                    double minTime = Double.MAX_VALUE;

                    DescriptiveStatistics stats = new DescriptiveStatistics(
                            XYZContinuousProbeFeature.BUFFER_SIZE);

                    for (int i = 0; i < UnivariateContinuousProbeFeature.BUFFER_SIZE; i++) {
                        stats.addValue(me.value[i]);

                        if (me.timestamp[i] > maxTime)
                            maxTime = me.timestamp[i];

                        if (me.timestamp[i] < minTime)
                            minTime = me.timestamp[i];
                    }

                    data.putDouble("MIN", stats.getMin());
                    data.putDouble("MAX", stats.getMax());
                    data.putDouble("MEAN", stats.getMean());
                    data.putDouble("STD_DEV", stats.getStandardDeviation());
                    data.putDouble("RMS", Math.sqrt(stats.getSumsq()
                            / UnivariateContinuousProbeFeature.BUFFER_SIZE));

                    data.putInt("BUFFER_SIZE",
                            UnivariateContinuousProbeFeature.BUFFER_SIZE);
                    data.putDouble(
                            "FREQUENCY",
                            ((double) UnivariateContinuousProbeFeature.BUFFER_SIZE)
                                    / ((maxTime - minTime) / 1000));
                    data.putDouble("DURATION",
                            ((double) ((maxTime - minTime) / 1000.0)));

                    me.transmitData(context, data);

                    me._running = false;
                }
            };

            Thread t = new Thread(r);
            t.start();
        }
    }

    protected void processData(Context context, Bundle dataBundle) {
        String key = this.valueKey();

        if (dataBundle.containsKey(key)
                && dataBundle.containsKey("EVENT_TIMESTAMP")) {
            double[] incomingTimes = dataBundle
                    .getDoubleArray("EVENT_TIMESTAMP");
            float[] values = dataBundle.getFloatArray(key);

            if (values != null) {
                for (int i = 0; i < incomingTimes.length; i++) {
                    if (index + i > BUFFER_SIZE)
                        this._filled = true;

                    int bufferIndex = (index + i) % BUFFER_SIZE;

                    timestamp[bufferIndex] = incomingTimes[i];
                    value[bufferIndex] = values[i];
                }

                index += incomingTimes.length;

                if (this._filled)
                    this.analyzeBuffers(context);
            }
        }
    }

    public String summarizeValue(Context context, Bundle bundle) {
        double mean = bundle.getDouble("MEAN");
        double stdDev = bundle.getDouble("STD_DEV");
        double rootMeanSquare = bundle.getDouble("RMS");

        return String.format(
                context.getResources().getString(
                        R.string.summary_univariate_statistics_feature), mean,
                stdDev, rootMeanSquare);
    }

}
