package edu.northwestern.cbits.purple_robot_manager.probes.features;

import android.content.Context;
import android.os.Bundle;

import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ContinuousProbe;

public abstract class XYZContinuousProbeFeature extends ContinuousProbeFeature
{
    protected static int BUFFER_SIZE = 512;

    protected float[] x = new float[BUFFER_SIZE];
    protected float[] y = new float[BUFFER_SIZE];
    protected float[] z = new float[BUFFER_SIZE];
    protected double[] timestamp = new double[BUFFER_SIZE];

    protected int index = 0;
    private boolean _filled = false;

    protected abstract void analyzeBuffers(Context context);

    @Override
    protected void processData(Context context, Bundle dataBundle)
    {
        if (this.isEnabled(context))
        {
            if (dataBundle.containsKey(ContinuousProbe.EVENT_TIMESTAMP) && dataBundle.containsKey("X") && dataBundle.containsKey("Y") && dataBundle.containsKey("Z"))
            {
                double[] incomingTimes = dataBundle.getDoubleArray(ContinuousProbe.EVENT_TIMESTAMP);
                float[] incomingX = dataBundle.getFloatArray("X");
                float[] incomingY = dataBundle.getFloatArray("Y");
                float[] incomingZ = dataBundle.getFloatArray("Z");

                for (int i = 0; i < incomingTimes.length; i++)
                {
                    if (index + i > BUFFER_SIZE)
                        this._filled = true;

                    int bufferIndex = (index + i) % BUFFER_SIZE;

                    timestamp[bufferIndex] = incomingTimes[i];
                    x[bufferIndex] = incomingX[i];
                    y[bufferIndex] = incomingY[i];
                    z[bufferIndex] = incomingZ[i];
                }

                index += incomingTimes.length;

                if (this._filled)
                    this.analyzeBuffers(context);
            }
        }
    }
}
