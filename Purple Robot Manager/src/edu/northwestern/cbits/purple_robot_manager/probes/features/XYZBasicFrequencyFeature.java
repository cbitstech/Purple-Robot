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

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public abstract class XYZBasicFrequencyFeature extends ContinuousProbeFeature 
{
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
	
	private void appendValues(float xs[], float ys[], float zs[], double[] ts)
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

			Log.e("PR", "SHIFT: " + shift + " -- " + offset);
			
			for (int i = 0; i < ts.length; i++)
			{
				_xValues[offset + i] = (double) xs[i];
				_yValues[offset + i] = (double) ys[i];
				_zValues[offset + i] = (double) zs[i];
				_timestamps[offset + i] = ts[i] / 1000;
			}
			
			this._currentIndex = offset + ts.length; // Should be BUFFER_SIZE
		}
		else
		{
			for (int i = 0; i < ts.length; i++)
			{
				_xValues[this._currentIndex + i] = (double) xs[i];
				_yValues[this._currentIndex + i] = (double) ys[i];
				_zValues[this._currentIndex + i] = (double) zs[i];
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

	protected void processData(Context context, Bundle dataBundle) 
	{
//		if (true)
//			return;
		
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
			
			if (true) // add last updated check for config
			{
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
				
				//Log.e("PR", "RAW TIME[0]: " + ts[0]);
				//Log.e("PR", "RAW TIME[1]: " + ts[1]);
				
				PolynomialSplineFunction fX = interpolator.interpolate(ts, xs);
				PolynomialSplineFunction fY = interpolator.interpolate(ts, ys);
				PolynomialSplineFunction fZ = interpolator.interpolate(ts, zs);
				
//				double lowFreq = 0.6;
//				double highFreq = 7.0;
			
				double durationOffset = ts[0];
				double bufferDuration = ts[ts.length-1] - durationOffset;

				double interval = 1.0 / 120.0; // (((double) ts.length) / bufferDuration); // Working in milliseconds...

				//Log.e("PR", "TS/0: " + ts[0] + " -- TS/-1: " + ts[ts.length - 1] + " -- LEN TS: " + ts.length);
				Log.e("PR", "BD: " + bufferDuration + " INT: " + interval);
				
				int twoPow = ts.length == 0 ? 0 : (32 - Integer.numberOfLeadingZeros(ts.length - 1));
				int bufferSize = (int) Math.pow(2, twoPow);
				
				Log.e("PR", "BUFF SIZE: " + bufferSize);
				
				double[] interX = new double[bufferSize];
				double[] interY = new double[bufferSize];
				double[] interZ = new double[bufferSize];
				
				Arrays.fill(interX, 0.0);
				Arrays.fill(interY, 0.0);
				Arrays.fill(interZ, 0.0);
				
//				double oldTime = 0.0;
				for (int i = 0; i < bufferSize; i++)
				{
					double time = durationOffset + (i * interval);
					
					//Log.e("PR", "TIME REQUEST: " + time);
					//Log.e("PR", "TIME DIFFERENCE: " + (oldTime - time));
					
					if (time > ts[ts.length - 1])
						time = ts[ts.length - 1];
					
					interX[i] = fX.value(time);
					interY[i] = fY.value(time);
					interZ[i] = fZ.value(time);
					
//					oldTime = time;
				}
				
				Log.e("PR", "INTERP SAMPLE: " + interX[bufferSize - 1] + " - " +  interY[bufferSize - 1] + " - " +  interZ[bufferSize - 1] + "INTERP BUFFER SIZE: " + bufferSize);

				double dynamicX[] = new double[interX.length];
				double dynamicY[] = new double[interY.length];
				double dynamicZ[] = new double[interZ.length];
				
				double staticX[] = new double[interX.length];
				double staticY[] = new double[interY.length];
				double staticZ[] = new double[interZ.length];

				for (int i = 0; i < interX.length; i++)
				{
					if (i < 2)
					{
						dynamicX[i] = 0;
						dynamicY[i] = 0;
						dynamicZ[i] = 0;
						
						staticX[i] = 0;
						staticY[i] = 0;
						staticZ[i] = 0;
					}
					else
					{
						if (i == dynamicX.length - 1)
						{
							dynamicX[i] = XYZBasicFrequencyFeature.bpFilter(interX, this._xBPHistory, i, "X");
							dynamicY[i] = XYZBasicFrequencyFeature.bpFilter(interY, this._yBPHistory, i, "Y");
							dynamicZ[i] = XYZBasicFrequencyFeature.bpFilter(interZ, this._zBPHistory, i, "Z");
							
							staticX[i] = XYZBasicFrequencyFeature.lpFilter(interX, this._xLPHistory, i, "X");
							staticY[i] = XYZBasicFrequencyFeature.lpFilter(interY, this._yLPHistory, i, "Y");
							staticZ[i] = XYZBasicFrequencyFeature.lpFilter(interZ, this._zLPHistory, i, "Z");
						}						
						else
						{
							dynamicX[i] = XYZBasicFrequencyFeature.bpFilter(interX, this._xBPHistory, i, null);
							dynamicY[i] = XYZBasicFrequencyFeature.bpFilter(interY, this._yBPHistory, i, null);
							dynamicZ[i] = XYZBasicFrequencyFeature.bpFilter(interZ, this._zBPHistory, i, null);
							
							staticX[i] = XYZBasicFrequencyFeature.lpFilter(interX, this._xLPHistory, i, null);
							staticY[i] = XYZBasicFrequencyFeature.lpFilter(interY, this._yLPHistory, i, null);
							staticZ[i] = XYZBasicFrequencyFeature.lpFilter(interZ, this._zLPHistory, i, null);
						}
						
						this._xBPHistory[1] = this._xBPHistory[0];
						this._xBPHistory[0] = dynamicX[i];
						
						this._yBPHistory[1] = this._yBPHistory[0];
						this._yBPHistory[0] = dynamicY[i];
						
						this._zBPHistory[1] = this._zBPHistory[0];
						this._zBPHistory[0] = dynamicZ[i];
						
						this._xLPHistory[1] = this._xLPHistory[0];
						this._xLPHistory[0] = staticX[i];
						
						this._yLPHistory[1] = this._yLPHistory[0];
						this._yLPHistory[0] = staticY[i];
						
						this._zLPHistory[1] = this._zLPHistory[0];
						this._zLPHistory[0] = staticZ[i];
					}
				}
							
				Log.e("PR", "Inter Sample: " + interX[interX.length - 1] + " - " +  interY[interX.length - 1] + " - " +  interZ[interX.length - 1]);
				Log.e("PR", "DY Sample: " + dynamicX[dynamicX.length - 1] + " - " +  dynamicY[dynamicX.length - 1] + " - " +  dynamicZ[interX.length - 1]);
				Log.e("PR", "GR Sample: " + staticX[staticX.length - 1] + " - " +  staticY[staticY.length - 1] + " - " +  staticZ[staticZ.length - 1]);

				double observedFreq = interX.length / bufferDuration; // (((double) this._currentIndex) / bufferDuration);

				Log.e("PR", "IL: + " + interX.length + " / BD: " + bufferDuration);

				Log.e("PR", "OBS HZ: " + observedFreq);

				FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
				
				Complex[] xFFT = fft.transform(dynamicX, TransformType.FORWARD);
				Complex[] yFFT = fft.transform(dynamicY, TransformType.FORWARD);
				Complex[] zFFT = fft.transform(dynamicZ, TransformType.FORWARD);
				
				double[] frequencies = XYZBasicFrequencyFeature.calculateFreqArray(interX.length, observedFreq);
				
				double xPeakFreq = XYZBasicFrequencyFeature.findPeakFrequency(xFFT, frequencies);
				double yPeakFreq = XYZBasicFrequencyFeature.findPeakFrequency(yFFT, frequencies);
				double zPeakFreq = XYZBasicFrequencyFeature.findPeakFrequency(zFFT, frequencies);
				
				Log.e("PR", "FREQS & GEEKS: x:" + xPeakFreq + " y:" + yPeakFreq + " z:" + zPeakFreq);
			}
		}
	}

	private static double findPeakFrequency(Complex[] samples, double[] frequencies)
	{
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
			return frequencies[index];
		
		return Double.NaN;
	}

	private static double[] calculateFreqArray(int numSamples, double maxFrequency)
	{
		double[] freqs = new double[numSamples];
		double stepSize = 1.0 / (double) (numSamples - 1);
		
		for (int i = 0; i < freqs.length; i++)
		{
			freqs[i] = (maxFrequency / 2) * (stepSize * i);
		}
		
		Log.e("PR", "STATED: " + maxFrequency + " -- OBS: " + freqs[freqs.length - 1]);
		
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

    	if (label != null)
    	{
    		Log.e("PR", "BP LABEL " + label);

    		Log.e("PR", "BETA[" + betaComponent + "/" + alphaComponent +"] = " + presentOutput);
    	}

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

    	if (label != null)
    	{
    		Log.e("PR", "LP LABEL " + label);

    		Log.e("PR", "BETA[" + betaComponent + "/" + alphaComponent +"] = " + presentOutput);
    	}

        
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
}
