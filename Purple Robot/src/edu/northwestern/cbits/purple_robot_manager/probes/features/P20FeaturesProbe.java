package edu.northwestern.cbits.purple_robot_manager.probes.features;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.ContinuousProbe;
import edu.northwestern.cbits.purple_robot_manager.probes.sample.SampleProbe;

import java.util.List;
import java.util.ArrayList;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.Executors;
import java.lang.Thread;
import android.util.Log;

public class P20FeaturesProbe extends Probe implements SensorEventListener 
{
	
	private final int freq_interp = 50;
	private final long window_size = (long)4e9; // sensor timestamps are in ns
	private final long window_shift = (long)1e9; // sensor timestamps are in ns
	private long lastprocessingtime = 0;
	
	private FeatureExtractor f_acc, f_gyr, f_bar;

	private List<Double> features;

	Clip acc_clip, gyr_clip, bar_clip;

	public static final String NAME = "edu.northwestern.cbits.purple_robot_manager.probes.features.P20FeaturesProbe";

	private static final String ENABLE = "config_probe_p20_enabled";
	private static final boolean DEFAULT_ENABLED = false;

	private int _lastFrequency = -1;
	private Context _context = null;

	private boolean extract_features = false;

	//private int counter = 0;
	//private long deltat;
	//private long t_trans = 0;

    private String[] feature_list = {"accx_mean","accx_std","gyrx_std","accx_kurt"};

    private boolean has_acc = false;
    private boolean has_gyr = false;
    private boolean has_bar = false;

    private List<String> acc_feature_list;
    private List<String> gyr_feature_list;
    private List<String> bar_feature_list;

	private Thread myThread;

	//private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	Runnable myRunnable = new Runnable() {
		public void run() {
			while (extract_features) {
				if (acc_clip.value.size()>0) {
					//long t1 = System.currentTimeMillis();
					features = new ArrayList<Double>();
					if (has_acc)
						features.addAll(f_acc.ExtractFeatures(acc_clip));
					if (has_gyr)
						features.addAll(f_gyr.ExtractFeatures(gyr_clip));
					if (has_bar)
						features.addAll(f_bar.ExtractFeatures(bar_clip));

					transmitAnalysis();
					//counter++;
					try {
						Thread.sleep(1000);
					} 
					catch (InterruptedException e) {
						extract_features = false;
						e.printStackTrace();
					}
					//deltat = System.currentTimeMillis() - t1;
				}
			}
		}
	};


	public P20FeaturesProbe() {
				
		acc_clip = new Clip(3, window_size);
		gyr_clip = new Clip(3, window_size);
		bar_clip = new Clip(1, window_size);

	    //fining out which sensors are needed
	    acc_feature_list = new ArrayList<String>();
	    gyr_feature_list = new ArrayList<String>();
	    bar_feature_list = new ArrayList<String>();
	    for (String s : feature_list) {
	    	if (s.startsWith("acc")) {
	    		if (!has_acc) has_acc = true;
	    		acc_feature_list.add(s.substring(3));
	    	}
	    	if (s.startsWith("gyr")) {
	    		if (!has_gyr) has_gyr = true;
	    		gyr_feature_list.add(s.substring(3));
	    	}
	    	if (s.startsWith("bar")) {
	    		if (!has_bar) has_bar = true;
	    		bar_feature_list.add(s.substring(3));
	    	}
	    }

	    //initializing feature extractors
	    f_acc = new FeatureExtractor(window_size, acc_feature_list, 3);
	    f_gyr = new FeatureExtractor(window_size, gyr_feature_list, 3);
	    f_bar = new FeatureExtractor(window_size, bar_feature_list, 1);

	    //creating and staring the thread

		myThread = new Thread(myRunnable);

		extract_features = true;

	    myThread.start();

	    Log.e("TEST", "Thread Started");


	}

	public String name(Context context) 
	{
		return P20FeaturesProbe.NAME;
	}

	public String title(Context context) 
	{
		// TODO: Pull out into strings.xml
		
		return "P20 Features Probe";
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
		// TODO
		return "TODO";
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

			extract_features = true;

			// checking if the current thread is still running
			if (!myThread.isAlive()) {
				// A dead thread cannot be restarted. A new thread has to be created.
				myThread = new Thread(myRunnable);
			    myThread.start();
	    	    Log.e("TEST", "Thread Started");

			}

			return true;
        }
    	else
    	{
    		sensors.unregisterListener(this, accelerometer);
    		sensors.unregisterListener(this, gyroscope);

    		this._lastFrequency = -1;

			extract_features = false;

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
		for (int i=0; i<event.values.length; i++)
			values[i] = (double)event.values[i]; // values.length = 3: X, Y, Z
		final long timestamp = event.timestamp;

		switch(sensor.getType())
		{
			case Sensor.TYPE_ACCELEROMETER:
				if (has_acc) acc_clip.add(values, timestamp);
				break;
			case Sensor.TYPE_GYROSCOPE:
				if (has_gyr) gyr_clip.add(values, timestamp);
				break;
			case Sensor.TYPE_PRESSURE:
				if (has_bar) bar_clip.add(values, timestamp);
				break;
		}
/*
		if (timestamp-lastprocessingtime > window_shift) {

			features_acc = f0.ExtractFeatures(acc_clip);

			lastprocessingtime = timestamp;

			this.transmitAnalysis();

		}
*/				
		
	}
	
	public void transmitAnalysis()
	{
		Bundle bundle = new Bundle();
		bundle.putString("PROBE", this.name(this._context)); // Required
		bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000); // Required
		
		//bundle.putDouble("MY_FEATURE_A", 1.23456); 		// define your own keys and values 
		//bundle.putLong("MY_FEATURE_B", Long.MAX_VALUE); // for the models & data collection

		for (int i=0; i<features.size(); i++)
			bundle.putDouble(feature_list[i], features.get(i));
		//bundle.putDouble("F2", features_acc[1]);
		//bundle.putDouble("F3", features_acc[2]);
		

		//bundle.putInt("CNT", counter);
		//bundle.putLong("T", deltat);
		//bundle.putLong("Trans", t_trans);
		//bundle.putLong("Time", (System.currentTimeMillis() - (long)1.410811849e12));

		this.transmitData(this._context, bundle);
	}

}
