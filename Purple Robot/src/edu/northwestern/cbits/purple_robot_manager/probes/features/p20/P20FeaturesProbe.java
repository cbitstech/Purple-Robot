package edu.northwestern.cbits.purple_robot_manager.probes.features.p20;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.features.p20.Clip.ClipException;
import edu.northwestern.cbits.purple_robot_manager.probes.features.p20.FeatureExtractor.Feature;

public class P20FeaturesProbe extends Probe implements SensorEventListener 
{
	public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.features.P20FeaturesProbe";
	private static final String ENABLE = "config_probe_p20_enabled";
	private static final boolean DEFAULT_ENABLED = false;
	
	private static final long WINDOW_SIZE = (long) 4e9; // sensor timestamps are in nanoseconds
	private static final long WINDOW_SHIFT = (long) 3e9; // sensor timestamps are in nanoseconds

	private final int f_interp = 50;	// (Hz) sampling frequency of interpolation prior to feature extraction

	private FeatureExtractor _accelerometerExtractor = null;
	private FeatureExtractor _gyroscopeExtractor = null;
	private FeatureExtractor _barometerExtractor = null;

	private HashMap<Feature, Double> _featureValues = new HashMap<Feature, Double>();

	private Clip _accelerometerClip = null;
	private Clip _gyroscopeClip = null;
	private Clip _barometerClip = null;

	private int _lastFrequency = -1;
	
	private Context _context = null;

	private boolean _extractFeatures = false;

    private FeatureExtractor.Feature[] _featureList = 
    {
    	Feature.ACC_MEAN, Feature.ACCX_MEAN, Feature.ACCY_MEAN, Feature.ACCZ_MEAN, 
    	Feature.ACC_MEAN_ABS, Feature.ACCX_MEAN_ABS, Feature.ACCY_MEAN_ABS, Feature.ACCZ_MEAN_ABS,
    	Feature.ACCX_STD, Feature.ACCY_STD, Feature.ACCZ_STD,
    	Feature.ACCX_SKEW, Feature.ACCY_SKEW, Feature.ACCZ_SKEW,
    	Feature.ACCX_KURT, Feature.ACCY_KURT, Feature.ACCZ_KURT,
    	Feature.ACCX_DIFF_MEAN, Feature.ACCY_DIFF_MEAN, Feature.ACCZ_DIFF_MEAN,
    	Feature.ACCX_DIFF_STD, Feature.ACCY_DIFF_STD, Feature.ACCZ_DIFF_STD,
    	Feature.ACCX_DIFF_SKEW, Feature.ACCY_DIFF_SKEW, Feature.ACCZ_DIFF_SKEW,
    	Feature.ACCX_DIFF_KURT, Feature.ACCY_DIFF_KURT, Feature.ACCZ_DIFF_KURT,
    	Feature.ACCX_MAX, Feature.ACCY_MAX, Feature.ACCZ_MAX,
    	Feature.ACCX_MIN, Feature.ACCY_MIN, Feature.ACCZ_MIN,
    	Feature.ACCX_MAX_ABS, Feature.ACCY_MAX_ABS, Feature.ACCZ_MAX_ABS,
    	Feature.ACCX_MIN_ABS, Feature.ACCY_MIN_ABS, Feature.ACCZ_MIN_ABS,
    	Feature.ACCX_RMS, Feature.ACCY_RMS, Feature.ACCZ_RMS,
    	Feature.ACC_CROSS_XY, Feature.ACC_CROSS_YZ, Feature.ACC_CROSS_ZX,
    	Feature.ACC_CROSS_XY_ABS, Feature.ACC_CROSS_YZ_ABS, Feature.ACC_CROSS_ZX_ABS,
    	Feature.ACC_CROSS_XY_NORM, Feature.ACC_CROSS_YZ_NORM, Feature.ACC_CROSS_ZX_NORM,
    	Feature.ACC_CROSS_XY_NORM_ABS, Feature.ACC_CROSS_YZ_NORM_ABS, Feature.ACC_CROSS_ZX_NORM_ABS,
    	Feature.ACCX_FFT1, Feature.ACCX_FFT2, Feature.ACCX_FFT3, Feature.ACCX_FFT4, Feature.ACCX_FFT5, Feature.ACCX_FFT6, Feature.ACCX_FFT7, Feature.ACCX_FFT8, Feature.ACCX_FFT9, Feature.ACCX_FFT10,
    	Feature.ACCY_FFT1, Feature.ACCY_FFT2, Feature.ACCY_FFT3, Feature.ACCY_FFT4, Feature.ACCY_FFT5, Feature.ACCY_FFT6, Feature.ACCY_FFT7, Feature.ACCY_FFT8, Feature.ACCY_FFT9, Feature.ACCY_FFT10,
    	Feature.ACCZ_FFT1, Feature.ACCZ_FFT2, Feature.ACCZ_FFT3, Feature.ACCZ_FFT4, Feature.ACCZ_FFT5, Feature.ACCZ_FFT6, Feature.ACCZ_FFT7, Feature.ACCZ_FFT8, Feature.ACCZ_FFT9, Feature.ACCZ_FFT10,
    	Feature.ACCX_HIST1, Feature.ACCX_HIST2, Feature.ACCX_HIST3, Feature.ACCX_HIST4, Feature.ACCX_HIST5, Feature.ACCX_HIST6,
    	Feature.ACCY_HIST1, Feature.ACCY_HIST2, Feature.ACCY_HIST3, Feature.ACCY_HIST4, Feature.ACCY_HIST5, Feature.ACCY_HIST6,
    	Feature.ACCZ_HIST1, Feature.ACCZ_HIST2, Feature.ACCZ_HIST3, Feature.ACCZ_HIST4, Feature.ACCZ_HIST5, Feature.ACCZ_HIST6,
    	Feature.GYR_MEAN, Feature.GYRX_MEAN, Feature.GYRY_MEAN, Feature.GYRZ_MEAN,
    	Feature.GYR_MEAN_ABS, Feature.GYRX_MEAN_ABS, Feature.GYRY_MEAN_ABS, Feature.GYRZ_MEAN_ABS,
    	Feature.GYRX_STD, Feature.GYRY_STD, Feature.GYRZ_STD,
    	Feature.GYRX_SKEW, Feature.GYRY_SKEW, Feature.GYRZ_SKEW,
    	Feature.GYRX_KURT, Feature.GYRY_KURT, Feature.GYRZ_KURT,
    	Feature.GYRX_DIFF_MEAN, Feature.GYRY_DIFF_MEAN, Feature.GYRZ_DIFF_MEAN,
    	Feature.GYRX_DIFF_STD, Feature.GYRY_DIFF_STD, Feature.GYRZ_DIFF_STD,
    	Feature.GYRX_DIFF_SKEW, Feature.GYRY_DIFF_SKEW, Feature.GYRZ_DIFF_SKEW,
    	Feature.GYRX_DIFF_KURT, Feature.GYRY_DIFF_KURT, Feature.GYRZ_DIFF_KURT,
    	Feature.GYRX_MAX, Feature.GYRY_MAX, Feature.GYRZ_MAX, Feature.GYRX_MIN, Feature.GYRY_MIN, Feature.GYRZ_MIN,
    	Feature.GYRX_MAX_ABS, Feature.GYRY_MAX_ABS, Feature.GYRZ_MAX_ABS, Feature.GYRX_MIN_ABS, Feature.GYRY_MIN_ABS, Feature.GYRZ_MIN_ABS,
    	Feature.GYRX_RMS, Feature.GYRY_RMS, Feature.GYRZ_RMS,
    	Feature.GYR_CROSS_XY, Feature.GYR_CROSS_YZ, Feature.GYR_CROSS_ZX,
    	Feature.GYR_CROSS_XY_ABS, Feature.GYR_CROSS_YZ_ABS, Feature.GYR_CROSS_ZX_ABS,
    	Feature.GYR_CROSS_XY_NORM, Feature.GYR_CROSS_YZ_NORM, Feature.GYR_CROSS_ZX_NORM,
    	Feature.GYR_CROSS_XY_NORM_ABS, Feature.GYR_CROSS_YZ_NORM_ABS, Feature.GYR_CROSS_ZX_NORM_ABS,
    	Feature.GYRX_FFT1, Feature.GYRX_FFT2, Feature.GYRX_FFT3, Feature.GYRX_FFT4, Feature.GYRX_FFT5, Feature.GYRX_FFT6, Feature.GYRX_FFT7, Feature.GYRX_FFT8, Feature.GYRX_FFT9, Feature.GYRX_FFT10,
    	Feature.GYRY_FFT1, Feature.GYRY_FFT2, Feature.GYRY_FFT3, Feature.GYRY_FFT4, Feature.GYRY_FFT5, Feature.GYRY_FFT6, Feature.GYRY_FFT7, Feature.GYRY_FFT8, Feature.GYRY_FFT9, Feature.GYRY_FFT10,
    	Feature.GYRZ_FFT1, Feature.GYRZ_FFT2, Feature.GYRZ_FFT3, Feature.GYRZ_FFT4, Feature.GYRZ_FFT5, Feature.GYRZ_FFT6, Feature.GYRZ_FFT7, Feature.GYRZ_FFT8, Feature.GYRZ_FFT9, Feature.GYRZ_FFT10,
    	Feature.GYRX_HIST1, Feature.GYRX_HIST2, Feature.GYRX_HIST3, Feature.GYRX_HIST4, Feature.GYRX_HIST5, Feature.GYRX_HIST6,
    	Feature.GYRY_HIST1, Feature.GYRY_HIST2, Feature.GYRY_HIST3, Feature.GYRY_HIST4, Feature.GYRY_HIST5, Feature.GYRY_HIST6,
    	Feature.GYRZ_HIST1, Feature.GYRZ_HIST2, Feature.GYRZ_HIST3, Feature.GYRZ_HIST4, Feature.GYRZ_HIST5, Feature.GYRZ_HIST6    
    };

    private boolean _hasAccelerometer = false;
    private boolean _hasGyroscope = false;
    private boolean _hasBarometer = false;

    private long last_timestamp;

    boolean acc_writing = false;
    boolean gyr_writing = false;
    boolean bar_writing = false;

	private Thread _featureThread = null;
	
	private Runnable _featureRunnable = null;

	public P20FeaturesProbe() 
	{
		this._accelerometerClip = new Clip(3, P20FeaturesProbe.WINDOW_SIZE, Clip.ACCELEROMETER);
		this._gyroscopeClip = new Clip(3, P20FeaturesProbe.WINDOW_SIZE, Clip.GYROSCOPE);
		this._barometerClip = new Clip(1, P20FeaturesProbe.WINDOW_SIZE, Clip.BAROMETER);

	    //lists to be passed to FeatureExtractor
		ArrayList<Feature> accelerometerFeatures = new ArrayList<Feature>();
		ArrayList<Feature> gyroscopeFeatures = new ArrayList<Feature>();
		ArrayList<Feature> barometerFeatures = new ArrayList<Feature>();

	    for (Feature f : this._featureList) 
	    {
	    	String featureName = f.toString().toLowerCase();
	    	
	    	if (featureName.startsWith("acc")) 
	    	{
	    		this._hasAccelerometer = true;
	    		
	    		accelerometerFeatures.add(f);
	    	}
	    	else if (featureName.startsWith("gyr")) 
	    	{
	    		this._hasGyroscope = true;
	    		
	    		gyroscopeFeatures.add(f);
	    	}
	    	else if (featureName.startsWith("bar")) 
	    	{
	    		this._hasBarometer = true;
	    		barometerFeatures.add(f);
	    	}
	    }

	    //initializing feature extractors
	    this._accelerometerExtractor = new FeatureExtractor(P20FeaturesProbe.WINDOW_SIZE, accelerometerFeatures, 3);
	    this._gyroscopeExtractor = new FeatureExtractor(P20FeaturesProbe.WINDOW_SIZE, gyroscopeFeatures, 3);
	    this._barometerExtractor = new FeatureExtractor(P20FeaturesProbe.WINDOW_SIZE, barometerFeatures, 1);

	    final P20FeaturesProbe me = this;
	    
		this._featureRunnable = new Runnable() 
		{
			public void run() 
			{
				while (me._extractFeatures) 
				{
					long now = System.currentTimeMillis();
					
					boolean generateTone = false;
						
					//checking if the clip has moved since last time
					if (me._accelerometerClip.getTimestamps().size() > 0) 
					{
						if (me._accelerometerClip.getLastTimestamp() == last_timestamp) 
						{
							Log.e("PR", "P20FeaturesProbe: Clip hasn't moved since last feature extraction!");
							generateTone = true;
						} 
						else {
							last_timestamp = me._accelerometerClip.getLastTimestamp();
							Log.e("PR", "P20FeaturesProbe: n_samp = "+ me._accelerometerClip.getTimestamps().size());
						}
					}
					
					if (me._hasAccelerometer) 
					{
						synchronized(me._accelerometerClip)
						{
							me._featureValues.putAll(me._accelerometerExtractor.extractFeatures(me._accelerometerClip, f_interp));

							if (me._accelerometerClip.getValues().size() < 100) 
								generateTone = true;
						}
					}

					if (me._hasGyroscope) 
					{
						synchronized(me._gyroscopeClip)
						{
							me._featureValues.putAll(me._gyroscopeExtractor.extractFeatures(me._gyroscopeClip, f_interp));

							if (me._gyroscopeClip.getValues().size() < 100) 
								generateTone = true;
						}
					}

					if (me._hasBarometer) 
					{
						synchronized(me._barometerClip)
						{
							me._featureValues.putAll(me._barometerExtractor.extractFeatures(me._barometerClip, f_interp));

							if (me._barometerClip.getValues().size() < 100) 
								generateTone = true;
						}
					}

					//transmit feature values and time stamps
					me.transmitAnalysis();

					try 
					{				
						if (generateTone)
						{
							// creating a tone generator
						    // the second argument is the volume (0-100)
							ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 50);

							toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
						}
						
						//measuring the processing time
						long deltaT = System.currentTimeMillis() - now;
						
						me._featureValues.put(Feature.PROCESSING_TIME, (double) deltaT);

						//accounting for the processing time / also converting from ns to ms
						
						long sleepTime = P20FeaturesProbe.WINDOW_SHIFT / (long) 1e6 - deltaT;

						//in the rare case that processing time is greater than window shift interval
						
						if (sleepTime < 0) 
							sleepTime = 0;
						
						Thread.sleep(sleepTime);
					} 
					catch (Exception e) 
					{
						me._extractFeatures = false;
						e.printStackTrace();
					}
				}
			}
		};
	}

	public String name(Context context) 
	{
		return P20FeaturesProbe.NAME;
	}

	public String title(Context context) 
	{
		return context.getString(R.string.title_probe_p20_features);
	}

	public String probeCategory(Context context) 
	{
		return context.getString(R.string.probe_misc_category);
	}

	public PreferenceScreen preferenceScreen(PreferenceActivity activity) 
	{
		@SuppressWarnings("deprecation")
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(this.summary(activity));

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey(P20FeaturesProbe.ENABLE);
		enabled.setDefaultValue(P20FeaturesProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		return screen;
	}
	
	@Override
	public String summary(Context context) 
	{
		return context.getString(R.string.summary_probe_p20_features);
	}

	public void enable(Context context) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean(P20FeaturesProbe.ENABLE, true);
		e.commit();
	}

	public void disable(Context context) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean(P20FeaturesProbe.ENABLE, false);
		e.commit();
	}

	public boolean isEnabled(final Context context)
	{
		if (this._context == null)
			this._context = context.getApplicationContext();
		
		boolean enabled = super.isEnabled(context);

		SharedPreferences prefs = Probe.getPreferences(context);

		if (enabled)
			enabled = prefs.getBoolean(P20FeaturesProbe.ENABLE, P20FeaturesProbe.DEFAULT_ENABLED);
		
    	SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

    	Sensor accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Sensor gyroscope = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

		if (enabled)
		{
			int frequency = SensorManager.SENSOR_DELAY_GAME; // TODO: Set in preferences!
	
			if (this._lastFrequency != frequency)
			{
				sensors.unregisterListener(this, accelerometer);
				sensors.unregisterListener(this, gyroscope);
	            
	            switch (frequency)
	            {
	            	case SensorManager.SENSOR_DELAY_FASTEST:
	                	sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST, null);
	                	sensors.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST, null);
	            		break;
	            	case SensorManager.SENSOR_DELAY_GAME:
	                	sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, null);
	                	sensors.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME, null);
	            		break;
	            	case SensorManager.SENSOR_DELAY_UI:
	                	sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI, null);
	                	sensors.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI, null);
	            		break;
	            	case SensorManager.SENSOR_DELAY_NORMAL:
	                	sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL, null);
	                	sensors.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL, null);
	            		break;
	            	default:
	                	sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, null);
	                	sensors.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME, null);
	            		break;
	            }
	            
	            this._lastFrequency = frequency;
			}

			this._extractFeatures = true;

			// checking if the current thread is still running
			if (this._featureThread == null || this._featureThread.isAlive() == false) 
			{
				// A dead thread cannot be restarted. A new thread has to be created.

				this._featureThread = new Thread(this._featureRunnable);
				this._featureThread.start();
			}

			return true;
        }
    	else
    	{
    		sensors.unregisterListener(this, accelerometer);
    		sensors.unregisterListener(this, gyroscope);

    		this._lastFrequency = -1;

			this._extractFeatures = false;
    	}
		
		return false;
	}

	public void onAccuracyChanged(Sensor arg0, int arg1) 
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void onSensorChanged(SensorEvent event) 
	{
		// Sohrob - configure this!

		Sensor sensor = event.sensor;

		final double[] values = new double[event.values.length];
		
		for (int i = 0; i < event.values.length; i++)
			values[i] = (double)event.values[i]; // values.length = 3: X, Y, Z
		
		final long timestamp = event.timestamp;

		try 
		{
			switch(sensor.getType())
			{
				case Sensor.TYPE_ACCELEROMETER:
					if (this._hasAccelerometer) 
					{
						synchronized(this._accelerometerClip)
						{
							this._accelerometerClip.appendValues(values, timestamp);
						}
					}
	
					break;
				case Sensor.TYPE_GYROSCOPE:
					if (this._hasGyroscope) 
					{
						synchronized(this._gyroscopeClip)
						{
							this._gyroscopeClip.appendValues(values, timestamp);
						}
					}
					
					break;
				case Sensor.TYPE_PRESSURE:
					if (this._hasBarometer) 
					{
						synchronized(this._barometerClip)
						{
							this._barometerClip.appendValues(values, timestamp);
						}
					}
					break;
			}
		} 
		catch (ClipException e) 
		{
			LogManager.getInstance(this._context).logException(e);
		}
	}
	
	public void transmitAnalysis()
	{
		Bundle bundle = new Bundle();
		bundle.putString("PROBE", this.name(this._context)); // Required
		bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000); // Required
		
		for (Feature f : this._featureValues.keySet())
		{
			bundle.putDouble(f.toString(), this._featureValues.get(f));
		}

		//bundle.putInt("CNT", counter);
		//bundle.putLong("T", deltat);
		//bundle.putLong("Trans", t_trans);
		//bundle.putLong("Time", (System.currentTimeMillis() - (long)1.410811849e12));

		this.transmitData(this._context, bundle);
	}
	
	public String summarizeValue(Context context, Bundle bundle)
	{
		return String.format(context.getResources().getString(R.string.summary_p20_features_probe), (int) bundle.getDouble("PROCESSING_TIME"));
	}
}
