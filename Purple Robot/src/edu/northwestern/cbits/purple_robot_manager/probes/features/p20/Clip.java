package edu.northwestern.cbits.purple_robot_manager.probes.features.p20;

import java.util.List;
import java.util.ArrayList;

import edu.emory.mathcs.backport.java.util.Arrays;

import android.util.Log;

public class Clip
{
    public static final int ACCELEROMETER = 1;
    public static final int GYROSCOPE = 2;
    public static final int BAROMETER = 3;

    private List<double[]> _values;
    private List<Long> _timestamps;
    private int _dimensions = -1;
    private long _windowSize = -1;
    private int _clipType = -1;

    public Clip(int dimension, long windowSize, int clipType)
    {
        this._values = new ArrayList<>();
        this._timestamps = new ArrayList<>();
        this._dimensions = dimension;
        this._windowSize = windowSize;
        this._clipType = clipType;
    }

    // Copy Constructor
    public Clip(Clip another)
    {
        this._values = new ArrayList<>(another._values);
        this._timestamps = new ArrayList<>(another._timestamps);
        this._dimensions = another._dimensions;
        this._windowSize = another._windowSize;
        this._clipType = another._clipType;
    }

    public static class ClipException extends Exception
    {
        private static final long serialVersionUID = 2813224970919166162L;

        public ClipException(String message)
        {
            super(message);
        }
    }

    public void appendValues(double[] values, long timestamp) throws ClipException
    {
        if (values.length != this._dimensions)
        {
            throw new ClipException("Dimensions do not match. Received " + values.length + ", expected "
                    + this._dimensions + ".");
        }
        else
        {
            synchronized (this)
            {
                if (this._timestamps.size() == 0)
                {
                    this.pushValues(values, timestamp);
                }
                else
                {
                    Long first = this._timestamps.get(0);

                    while ((timestamp - first > this._windowSize) && (this._timestamps.size() > 1))
                    {
                        this._timestamps.remove(0);
                        this._values.remove(0);

                        first = this._timestamps.get(0);
                    }

                    if (timestamp - first > this._windowSize)
                    {
                        // Clear values if gap larger than _windowSize...

                        this._values.clear();
                        this._timestamps.clear();

                        Log.e("PR", "Clip: There has been a gap longer than " + this._windowSize + "ms.");
                    }

                    this.pushValues(values, timestamp);
                }
            }
        }
    }

    private void pushValues(double[] values, long timestamp)
    {
        synchronized (this)
        {
            this._timestamps.add(timestamp);

            this._values.add(Arrays.copyOf(values, values.length));
        }
    }

    public List<double[]> getValues()
    {
        List<double[]> values = new ArrayList<>();

        synchronized (this)
        {
            values.addAll(this._values);
        }

        return values;
    }

    public List<Long> getTimestamps()
    {
        List<Long> timestamps = new ArrayList<>();

        synchronized (this)
        {
            timestamps.addAll(this._timestamps);
        }

        return timestamps;
    }

    public long getLastTimestamp()
    {
        long timestamp = 0;

        synchronized (this)
        {
            timestamp = this._timestamps.get(this._timestamps.size() - 1);
        }

        return timestamp;
    }

    public int getType()
    {
        return this._clipType;
    }
}