package edu.northwestern.cbits.purple_robot_manager.probes.features;

import java.util.ArrayList;
import java.util.Comparator;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.exception.NonMonotonicSequenceException;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.util.MathArrays;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.northwestern.cbits.purple_robot_manager.R;

public abstract class XYZBasicFrequencyFeature extends ContinuousProbeFeature 
{
	private static final boolean INTERPOLATED_ENABLED = false;
	private static final boolean BANDPASS_ENABLED = false;
	private static final boolean LOWPASS_ENABLED = false;
	private static final String DEFAULT_FREQUENCY = "60";

	protected static int BUFFER_SIZE = 4096;

	private double[] _xValues = new double[BUFFER_SIZE];
	private double[] _yValues = new double[BUFFER_SIZE];
	private double[] _zValues = new double[BUFFER_SIZE];
	private double[] _timestamps = new double[BUFFER_SIZE];
	
	private double[] _xBPHistory = { 0.0, 0.0, 0.0 };
	private double[] _yBPHistory = { 0.0, 0.0, 0.0 };
	private double[] _zBPHistory = { 0.0, 0.0, 0.0 };
	
	private double[] _xLPHistory = { 0.0, 0.0, 0.0 };
	private double[] _yLPHistory = { 0.0, 0.0, 0.0 };
	private double[] _zLPHistory = { 0.0, 0.0, 0.0 };
	
	private int _currentIndex = 0;
	private long _lastUpdate = 0;
	
	double interTimes[];
	
	private class Reading
	{
		public double t;
		public double x;
		public double y;
		public double z;

		private Reading(double t, double x, double y, double z)
		{
			this.t = t;
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
	
	private void appendValues(float[] incomingX, float[] incomingY, float[] incomingZ, double[] ts)
	{
		if (this._currentIndex + ts.length > BUFFER_SIZE)
		{
			int shift = this._currentIndex + ts.length - BUFFER_SIZE;
			
			for (int i = 0; i < BUFFER_SIZE - shift; i++)
			{
				_xValues[i] = _xValues[i + shift];
				_yValues[i] = _yValues[i + shift];
				_zValues[i] = _zValues[i + shift];
				_timestamps[i] = _timestamps[i + shift];
			}

			int offset = BUFFER_SIZE - shift;

			for (int i = 0; i < ts.length; i++)
			{
				_xValues[offset + i] = (double) incomingX[i];
				_yValues[offset + i] = (double) incomingY[i];
				_zValues[offset + i] = (double) incomingZ[i];
				_timestamps[offset + i] = ts[i] / 1000;
			}
			
			this._currentIndex = offset + ts.length; // Should be BUFFER_SIZE
		}
		else
		{
			for (int i = 0; i < ts.length; i++)
			{
				_xValues[this._currentIndex + i] = (double) incomingX[i];
				_yValues[this._currentIndex + i] = (double) incomingY[i];
				_zValues[this._currentIndex + i] = (double) incomingZ[i];
				_timestamps[this._currentIndex + i] = ts[i] / 1000;
			}

			this._currentIndex += ts.length;
		}

		//Log.e("PR", "Raw Timestamp: " + ts[ts.length-1]);

		ArrayList<Reading> readings = new ArrayList<Reading>();
		
		for (int i = 0; i < this._currentIndex; i++)
		{
			readings.add(new Reading(_timestamps[i], _xValues[i], _yValues[i], _zValues[i]));
		}
		
		boolean ordered = false;

		while (ordered == false)
		{
			try
			{
				Collections.sort(readings, new Comparator<Reading>()
				{
					public int compare(Reading one, Reading two) 
					{
						if ((two.t - one.t) > 0)
							return -1;
						else if ((two.t - one.t) < 0)
							return 1;
						
						return 0;
					}
				});
				
				double[] testTimestamps = new double[readings.size()];
						
				for (int i = 0; i < readings.size(); i++)
				{
					Reading r = readings.get(i);
					
					testTimestamps[i] = r.t;

					if (i > 1 && testTimestamps[i] == testTimestamps[i-1])
					{
						double newTime = (testTimestamps[i-2] + testTimestamps[i]) / 2;

						testTimestamps[i-1] = newTime;
						readings.get(i-1).t = newTime;
					}
				}

				MathArrays.checkOrder(testTimestamps);
				
				ordered = true;
				
				for (int i = 0; i < readings.size(); i++)
				{
					Reading r = readings.get(i);
					
					_timestamps[i] = r.t;
					_xValues[i] = r.x;
					_yValues[i] = r.y;
					_zValues[i] = r.z;
				}
			}
			catch (NonMonotonicSequenceException e)
			{
				e.printStackTrace();
			}
		}
	}

	protected void processData(final Context context, Bundle dataBundle) 
	{
		if (dataBundle.containsKey("EVENT_TIMESTAMP") &&
			dataBundle.containsKey("X") &&
			dataBundle.containsKey("Y") &&
			dataBundle.containsKey("Z"))
		{
			double[] incomingTimes = dataBundle.getDoubleArray("EVENT_TIMESTAMP");
			
			float[] incomingX = dataBundle.getFloatArray("X");
			float[] incomingY = dataBundle.getFloatArray("Y");
			float[] incomingZ = dataBundle.getFloatArray("Z");
			
			this.appendValues(incomingX, incomingY, incomingZ, incomingTimes);
			
			final long now = System.currentTimeMillis();
			
			final String key = this.featureKey();

			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			long updateInterval = Long.parseLong(prefs.getString("config_probe_" + key + "_frequency", XYZBasicFrequencyFeature.DEFAULT_FREQUENCY)) * 1000;
			
			if (now - this._lastUpdate > updateInterval) // add last updated check for config
			{
				this._lastUpdate = now;
				
				LinearInterpolator interpolator = new LinearInterpolator();

				double[] xs = _xValues;
				double[] ys = _yValues;
				double[] zs = _zValues;
				double[] ts = _timestamps;

				if (this._currentIndex < BUFFER_SIZE - 1)
				{
					xs = Arrays.copyOfRange(_xValues, 0, this._currentIndex);
					ys = Arrays.copyOfRange(_yValues, 0, this._currentIndex);
					zs = Arrays.copyOfRange(_zValues, 0, this._currentIndex);
					ts = Arrays.copyOfRange(_timestamps, 0, this._currentIndex);
				}
				
//				Log.e("PR", "FIRST RAW TIME: " + ts[0] + " LAST RAW TIME: " + ts[ts.length - 1]);
				
				//Log.e("PR", "RAW TIME[0]: " + ts[0]);
				//Log.e("PR", "RAW TIME[1]: " + ts[1]);
				
				PolynomialSplineFunction fX = interpolator.interpolate(ts, xs);
				PolynomialSplineFunction fY = interpolator.interpolate(ts, ys);
				PolynomialSplineFunction fZ = interpolator.interpolate(ts, zs);
				
//				double lowFreq = 0.6;
//				double highFreq = 7.0;
			
				double durationOffset = ts[0];
				double bufferDuration = ts[ts.length-1] - durationOffset;

				double interval = 1.0 / 120.0; 

				//Log.e("PR", "TS/0: " + ts[0] + " -- TS/-1: " + ts[ts.length - 1] + " -- LEN TS: " + ts.length);
//				Log.e("PR", "BD: " + bufferDuration + " INT: " + interval);
				
				int twoPow = ts.length == 0 ? 0 : (32 - Integer.numberOfLeadingZeros(ts.length - 1));
				int bufferSize = (int) Math.pow(2, twoPow);
				
//				Log.e("PR", "BUFF SIZE: " + bufferSize);
				
				final double[] _interX = new double[bufferSize];
				final double[] _interY = new double[bufferSize];
				final double[] _interZ = new double[bufferSize];
			
				Arrays.fill(_interX, 0.0);
				Arrays.fill(_interY, 0.0);
				Arrays.fill(_interZ, 0.0);
				
				interTimes = new double[bufferSize];
				
				for (int i = 0; i < bufferSize; i++)
				{
					interTimes[i] = durationOffset + (i * interval);
					
					//Log.e("PR", "TIME REQUEST: " + time);
					//Log.e("PR", "TIME DIFFERENCE: " + (oldTime - time));
					
					if (interTimes[i] > ts[ts.length - 1]) //If the current timestamp is greater than the last recorded timestamp, set it to the last timestamp
						interTimes[i] = ts[ts.length - 1];
					
					_interX[i] = fX.value(interTimes[i]);
					_interY[i] = fY.value(interTimes[i]);
					_interZ[i] = fZ.value(interTimes[i]);
							
				}
			
//				double timeDifference = interTimes[bufferSize - 1] - interTimes[0];
				
//				Log.e("PR", "INTERP TIME: " + timeDifference + " BUFFER SIZE: " + bufferSize);
//				Log.e("PR", "FIRST INTERP TIME: " + interTimes[0] + " LAST INTERP TIME: " + interTimes[interTimes.length - 1]);
				
				//Log.e("PR", "INTERP SAMPLE: " + interX[bufferSize - 1] + " - " +  interY[bufferSize - 1] + " - " +  interZ[bufferSize - 1]);

				final double[] _dynamicX = new double[_interX.length];
				final double[] _dynamicY = new double[_interY.length]; 
				final double[] _dynamicZ = new double[_interZ.length];
				
				final double[] _staticX = new double[_interX.length];
				final double[] _staticY = new double[_interY.length];
				final double[] _staticZ = new double[_interZ.length];

				for (int i = 0; i < _interX.length; i++)
				{
					if (i < 2)
					{
						_dynamicX[i] = 0;
						_dynamicY[i] = 0;
						_dynamicZ[i] = 0;
						
						_staticX[i] = 0;
						_staticY[i] = 0;
						_staticZ[i] = 0;
					}
					else
					{
						if (i == _dynamicX.length - 1)
						{
							_dynamicX[i] = XYZBasicFrequencyFeature.bpFilter(_interX, this._xBPHistory, i, "X");
							_dynamicY[i] = XYZBasicFrequencyFeature.bpFilter(_interY, this._yBPHistory, i, "Y");
							_dynamicZ[i] = XYZBasicFrequencyFeature.bpFilter(_interZ, this._zBPHistory, i, "Z");
							
							_staticX[i] = XYZBasicFrequencyFeature.lpFilter(_interX, this._xLPHistory, i, "X");
							_staticY[i] = XYZBasicFrequencyFeature.lpFilter(_interY, this._yLPHistory, i, "Y");
							_staticZ[i] = XYZBasicFrequencyFeature.lpFilter(_interZ, this._zLPHistory, i, "Z");
						}						
						else
						{
							_dynamicX[i] = XYZBasicFrequencyFeature.bpFilter(_interX, this._xBPHistory, i, null);
							_dynamicY[i] = XYZBasicFrequencyFeature.bpFilter(_interY, this._yBPHistory, i, null);
							_dynamicZ[i] = XYZBasicFrequencyFeature.bpFilter(_interZ, this._zBPHistory, i, null);
							
							_staticX[i] = XYZBasicFrequencyFeature.lpFilter(_interX, this._xLPHistory, i, null);
							_staticY[i] = XYZBasicFrequencyFeature.lpFilter(_interY, this._yLPHistory, i, null);
							_staticZ[i] = XYZBasicFrequencyFeature.lpFilter(_interZ, this._zLPHistory, i, null);
						}
						
						this._xBPHistory[1] = this._xBPHistory[0];
						this._xBPHistory[0] = _dynamicX[i];
						
						this._yBPHistory[1] = this._yBPHistory[0];
						this._yBPHistory[0] = _dynamicY[i];
						
						this._zBPHistory[1] = this._zBPHistory[0];
						this._zBPHistory[0] = _dynamicZ[i];
						
						this._xLPHistory[1] = this._xLPHistory[0];
						this._xLPHistory[0] = _staticX[i];
						
						this._yLPHistory[1] = this._yLPHistory[0];
						this._yLPHistory[0] = _staticY[i];
						
						this._zLPHistory[1] = this._zLPHistory[0];
						this._zLPHistory[0] = _staticZ[i];
					}
				}
						
//				Log.e("PR", "Inter Sample: " + _interX[_interX.length - 1] + " - " +  _interY[_interX.length - 1] + " - " +  _interZ[_interX.length - 1]);
//				Log.e("PR", "DY Sample: " + _dynamicX[_dynamicX.length - 1] + " - " +  _dynamicY[_dynamicX.length - 1] + " - " +  _dynamicZ[_interX.length - 1]);
//				Log.e("PR", "GR Sample: " + _staticX[_staticX.length - 1] + " - " +  _staticY[_staticY.length - 1] + " - " +  _staticZ[_staticZ.length - 1]);
	
				double observedFreq = _interX.length / bufferDuration; // (((double) this._currentIndex) / bufferDuration);
	
//				Log.e("PR", "IL: + " + _interX.length + " / BD: " + bufferDuration);
//				Log.e("PR", "OBS HZ: " + observedFreq);
	
				FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
				
				Complex[] xFFT = fft.transform(_dynamicX, TransformType.FORWARD);
				Complex[] yFFT = fft.transform(_dynamicY, TransformType.FORWARD);
				Complex[] zFFT = fft.transform(_dynamicZ, TransformType.FORWARD);
				
				double[] frequencies = XYZBasicFrequencyFeature.calculateFreqArray(_interX.length, observedFreq);
				
				final double[] _xMaxFreqPowPair = XYZBasicFrequencyFeature.findPeakFrequency(xFFT, frequencies); 
				final double[] _yMaxFreqPowPair = XYZBasicFrequencyFeature.findPeakFrequency(yFFT, frequencies);
				final double[] _zMaxFreqPowPair = XYZBasicFrequencyFeature.findPeakFrequency(zFFT, frequencies);
				
//				Log.e("PR", "FREQS & GEEKS: x:" + _xMaxFreqPowPair[0] + " - " + _xMaxFreqPowPair[1]  + " y:" + _yMaxFreqPowPair[0]  + " - " + _yMaxFreqPowPair[1] + " z:" + _zMaxFreqPowPair[0]  + " - " + _zMaxFreqPowPair[1] );
	
				final XYZBasicFrequencyFeature me = this;
				
				Runnable r = new Runnable()
				{
					public void run() 
					{
						Bundle data = new Bundle();
	
						data.putDouble("TIMESTAMP", now / 1000);
						data.putString("PROBE", me.name(context));
						
						boolean incInterpolated = prefs.getBoolean("config_probe_" + key + "_interpolated_enabled", XYZBasicFrequencyFeature.INTERPOLATED_ENABLED);
						boolean incBandpass = prefs.getBoolean("config_probe_" + key + "_bandpass_enabled", XYZBasicFrequencyFeature.BANDPASS_ENABLED);
						boolean incLowpass = prefs.getBoolean("config_probe_" + key + "_lowpass_enabled", XYZBasicFrequencyFeature.LOWPASS_ENABLED);
	
						if (incInterpolated || incBandpass || incLowpass)
						{
							Bundle sensorData = new Bundle();
							
							synchronized(me)
							{
								sensorData.putDoubleArray("INTERP_TIMESTAMPS", interTimes);
								
								if (incInterpolated)
								{
									sensorData.putDoubleArray("INTER_X", _interX);
									sensorData.putDoubleArray("INTER_Y", _interY);
									sensorData.putDoubleArray("INTER_Z", _interZ);
								}
								
								if (incBandpass)
								{
									sensorData.putDoubleArray("DYNAMIC_X", _dynamicX);
									sensorData.putDoubleArray("DYNAMIC_Y", _dynamicY);
									sensorData.putDoubleArray("DYNAMIC_Z", _dynamicZ);
								}
	
								if (incLowpass)
								{
									sensorData.putDoubleArray("STATIC_X", _staticX);
									sensorData.putDoubleArray("STATIC_Y", _staticY);
									sensorData.putDoubleArray("STATIC_Z", _staticZ);
								}
	
								data.putBundle("CALCULATIONS", sensorData);
							}
						}
	
						data.putDouble("WINDOW_TIMESTAMP", interTimes[0]);
						
						data.putDouble("POWER_X", _xMaxFreqPowPair[1]);
						data.putDouble("POWER_Y", _yMaxFreqPowPair[1]);
						data.putDouble("POWER_Z", _zMaxFreqPowPair[1]);
						
						data.putDouble("FREQ_X", _xMaxFreqPowPair[0]);
						data.putDouble("FREQ_Y", _yMaxFreqPowPair[0]);
						data.putDouble("FREQ_Z", _zMaxFreqPowPair[0]);
						
						me.transmitData(context, data);
					}
				};
				
				Thread t = new Thread(r);
				
				t.start();
			}
		}
	}

	private static double[] findPeakFrequency(Complex[] samples, double[] frequencies)
	{
		int FREQUENCY_INDEX = 0;
		int POWER_INDEX = 1;
		
		double[] returnFrequencyPowerPair = {-1, -1};
		double max = Double.MIN_NORMAL; 
		int index = -1;
		
		int singleSide = (samples.length / 2);
		
		for(int i = 0; i < singleSide; i++)
		{
			double value = 2 * Math.abs(samples[i].getReal());

			if (value > max)
			{
				max = value;
				index = i;
			}
		}
		
		if (index >= 0)
		{
			returnFrequencyPowerPair[FREQUENCY_INDEX] = frequencies[index];
			returnFrequencyPowerPair[POWER_INDEX] = 2 * Math.abs(samples[index].getReal());
		}
			//return frequencies[index];
		
		return returnFrequencyPowerPair;
	}

	private static double[] calculateFreqArray(int numSamples, double maxFrequency)
	{
		double[] freqs = new double[numSamples];
		double stepSize = 1.0 / (double) (numSamples - 1);
		 
		for (int i = 0; i < freqs.length; i++)
		{
			freqs[i] = (maxFrequency / 2) * (stepSize * i);
		}
		
//		Log.e("PR", "STATED: " + maxFrequency + " -- OBS: " + freqs[freqs.length - 1]);
		
		return freqs;
	}
	
	public static double bpFilter(double[] inputs, double[] outputs, int offset, String label)
    {
		// Magic numbers: M is for MatLab...
		
	    double[] beta = {0.15442873388844763, 0, -0.15442873388844763};
	    double[] alpha = { 1, -1.6806066079606878,  0.69114253222310462};
	    
	    double betaComponent = 0.0;
	    double alphaComponent = 0.0;

        double presentOutput = 0;
        
        for(int k = 0; k <= 2; k++)
        {
        	 betaComponent = betaComponent + (beta[k] * inputs[offset - k]);
        	 // = b(0)*inputs(n) + b(1)*inputs(n - 1) + b(2)*inputs(n - 2)
        }
        
        for(int j = 1; j <= 2; j++)
        {
        	alphaComponent = alphaComponent + (alpha[j] * outputs[j-1]);
        	// = a(1)*outputs(0) + a(2)*outputs(1)
        }
        
        presentOutput = betaComponent - alphaComponent;

//    	if (label != null)
//    	{
//    		Log.e("PR", "BP LABEL " + label);
//    		Log.e("PR", "BETA[" + betaComponent + "/" + alphaComponent +"] = " + presentOutput);
//    	}

        // presentOutput = presentOutput / FILTER_SCALING_FACTOR;
        // Not a huge advantage using doubles to begin with!
        
        return presentOutput;
    }
	
	public static double lpFilter(double[] inputs, double[] outputs, int offset, String label)
    {
		// Magic numbers: M is for MatLab...
		
	    double[] beta = {0.00024135904904198073, 0.0004827180980838, 0.00024135904904198073};
	    double[] alpha = {1, -1.9555782403150355, 0.95654367651120342};
	    
	    double betaComponent = 0.0;
	    double alphaComponent = 0.0;

        double presentOutput = 0;
        
        for(int k = 0; k <= 2; k++)
        {
        	 betaComponent = betaComponent + (beta[k] * inputs[offset - k]);
        	 // = b(0)*inputs(n) + b(1)*inputs(n - 1) + b(2)*inputs(n - 2)
        }
        
        for(int j = 1; j <= 2; j++)
        {
        	alphaComponent = alphaComponent + (alpha[j] * outputs[j-1]);
        	// = a(1)*outputs(0) + a(2)*outputs(1)
        }
        
        presentOutput = 0 - alphaComponent + betaComponent;

//    	if (label != null)
//    	{
//    		Log.e("PR", "LP LABEL " + label);
//    		Log.e("PR", "BETA[" + betaComponent + "/" + alphaComponent +"] = " + presentOutput);
//    	}
        
        return presentOutput;
    }

	@SuppressWarnings("unused")
	private static double[] lowPassFilter(double[] values, double interval, double lowFreq, double highFreq) 
	{
		double tau = 1 / interval; // Back to seconds...
		double dt = 1.0 / lowFreq;

		double a = tau / (tau + dt);
		
		double last = values[0];
		
		double[] newValues = new double[values.length];
		
		for (int i = 0; i < newValues.length; i++)
		{
			last = XYZBasicFrequencyFeature.weightedSmoothing(values[i], last, a);
			
			newValues[i] = last;
		}
		
		return newValues;
	}

	private static double weightedSmoothing(double value, double last, double a) 
	{
		return (last * (1.0 - a)) + (value * a);
	}
	
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceScreen screen = super.preferenceScreen(activity);

		String key = this.featureKey();

		// Include interpolated
		// Include bandpass (dynamic)
		// Include lowpass (static)

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_" + key + "_frequency");
		duration.setEntryValues(R.array.frequency_probe_duration_values);
		duration.setEntries(R.array.frequency_probe_duration_labels);
		duration.setTitle(R.string.probe_frequency_label);
		duration.setDefaultValue(XYZBasicFrequencyFeature.DEFAULT_FREQUENCY);

		screen.addPreference(duration);

		CheckBoxPreference interpolated = new CheckBoxPreference(activity);
		interpolated.setTitle(R.string.title_enable_interpolated_probe);
		interpolated.setKey("config_probe_" + key + "_interpolated_enabled");
		interpolated.setDefaultValue(XYZBasicFrequencyFeature.INTERPOLATED_ENABLED);
		screen.addPreference(interpolated);

		CheckBoxPreference bandpass = new CheckBoxPreference(activity);
		bandpass.setTitle(R.string.title_enable_bandpass_probe);
		bandpass.setKey("config_probe_" + key + "_bandpass_enabled");
		bandpass.setDefaultValue(XYZBasicFrequencyFeature.BANDPASS_ENABLED);
		screen.addPreference(bandpass);

		CheckBoxPreference lowpass = new CheckBoxPreference(activity);
		lowpass.setTitle(R.string.title_enable_lowpass_probe);
		lowpass.setKey("config_probe_" + key + "_lowpass_enabled");
		lowpass.setDefaultValue(XYZBasicFrequencyFeature.LOWPASS_ENABLED);
		screen.addPreference(lowpass);

		return screen;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		double powerX = bundle.getDouble("POWER_X");
		double powerY = bundle.getDouble("POWER_Y");
		double powerZ = bundle.getDouble("POWER_Z");

		double freqX = bundle.getDouble("FREQ_X");
		double freqY = bundle.getDouble("FREQ_Y");
		double freqZ = bundle.getDouble("FREQ_Z");

		return String.format(context.getResources().getString(R.string.summary_frequency_statistics_feature), freqX, freqY, freqZ, powerX, powerY, powerZ);
	}
}
