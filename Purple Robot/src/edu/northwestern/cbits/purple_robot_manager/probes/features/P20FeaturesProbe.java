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

import android.media.ToneGenerator;
import android.media.AudioManager;

public class P20FeaturesProbe extends Probe implements SensorEventListener 
{
	
	private final int freq_interp = 50;
	private final long window_size = (long)4e9; // sensor timestamps are in nanoseconds
	private final long window_shift = (long)3e9; // sensor timestamps are in nanoseconds
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
	private long deltat;
	//private long t_trans = 0;

    //private String[] feature_list = {"accx_hist2","accx_max","acc_cross_zx"};

    private String[] feature_list = {"acc_mean","accx_mean","accy_mean","accz_mean","acc_mean_abs","accx_mean_abs","accy_mean_abs","accz_mean_abs",
    								"accx_std","accy_std","accz_std","accx_skew","accy_skew","accz_skew","accx_kurt","accy_kurt","accz_kurt",
    								"accx_diff_mean","accy_diff_mean","accz_diff_mean","accx_diff_std","accy_diff_std","accz_diff_std",
    								"accx_diff_skew","accy_diff_skew","accz_diff_skew","accx_diff_kurt","accy_diff_kurt","accz_diff_kurt",
    								"accx_max","accy_max","accz_max","accx_min","accy_min","accz_min","accx_max_abs","accy_max_abs","accz_max_abs","accx_min_abs","accy_min_abs","accz_min_abs",
    								"accx_rms","accy_rms","accz_rms",
    								"acc_cross_xy","acc_cross_yz","acc_cross_zx","acc_cross_xy_abs","acc_cross_yz_abs","acc_cross_zx_abs",
    								"acc_cross_xy_norm","acc_cross_yz_norm","acc_cross_zx_norm","acc_cross_xy_norm_abs","acc_cross_yz_norm_abs","acc_cross_zx_norm_abs",
    								"accx_fft_1","accx_fft_2","accx_fft_3","accx_fft_4","accx_fft_5","accx_fft_6","accx_fft_7","accx_fft_8","accx_fft_9","accx_fft_10",
    								"accy_fft_1","accy_fft_2","accy_fft_3","accy_fft_4","accy_fft_5","accy_fft_6","accy_fft_7","accy_fft_8","accy_fft_9","accy_fft_10",
    								"accz_fft_1","accz_fft_2","accz_fft_3","accz_fft_4","accz_fft_5","accz_fft_6","accz_fft_7","accz_fft_8","accz_fft_9","accz_fft_10",
    								"accx_hist1","accx_hist2","accx_hist3","accx_hist4","accx_hist5","accx_hist6",
    								"accy_hist1","accy_hist2","accy_hist3","accy_hist4","accy_hist5","accy_hist6",
    								"accz_hist1","accz_hist2","accz_hist3","accz_hist4","accz_hist5","accz_hist6", ///
    								"gyr_mean","gyrx_mean","gyry_mean","gyrz_mean","gyr_mean_abs","gyrx_mean_abs","gyry_mean_abs","gyrz_mean_abs",
    								"gyrx_std","gyry_std","gyrz_std","gyrx_skew","gyry_skew","gyrz_skew","gyrx_kurt","gyry_kurt","gyrz_kurt",
    								"gyrx_diff_mean","gyry_diff_mean","gyrz_diff_mean","gyrx_diff_std","gyry_diff_std","gyrz_diff_std",
    								"gyrx_diff_skew","gyry_diff_skew","gyrz_diff_skew","gyrx_diff_kurt","gyry_diff_kurt","gyrz_diff_kurt",
    								"gyrx_max","gyry_max","gyrz_max","gyrx_min","gyry_min","gyrz_min","gyrx_max_abs","gyry_max_abs","gyrz_max_abs","gyrx_min_abs","gyry_min_abs","gyrz_min_abs",
    								"gyrx_rms","gyry_rms","gyrz_rms",
    								"gyr_cross_xy","gyr_cross_yz","gyr_cross_zx","gyr_cross_xy_abs","gyr_cross_yz_abs","gyr_cross_zx_abs",
    								"gyr_cross_xy_norm","gyr_cross_yz_norm","gyr_cross_zx_norm","gyr_cross_xy_norm_abs","gyr_cross_yz_norm_abs","gyr_cross_zx_norm_abs",
    								"gyrx_fft_1","gyrx_fft_2","gyrx_fft_3","gyrx_fft_4","gyrx_fft_5","gyrx_fft_6","gyrx_fft_7","gyrx_fft_8","gyrx_fft_9","gyrx_fft_10",
    								"gyry_fft_1","gyry_fft_2","gyry_fft_3","gyry_fft_4","gyry_fft_5","gyry_fft_6","gyry_fft_7","gyry_fft_8","gyry_fft_9","gyry_fft_10",
    								"gyrz_fft_1","gyrz_fft_2","gyrz_fft_3","gyrz_fft_4","gyrz_fft_5","gyrz_fft_6","gyrz_fft_7","gyrz_fft_8","gyrz_fft_9","gyrz_fft_10",
    								"gyrx_hist1","gyrx_hist2","gyrx_hist3","gyrx_hist4","gyrx_hist5","gyrx_hist6",
    								"gyry_hist1","gyry_hist2","gyry_hist3","gyry_hist4","gyry_hist5","gyry_hist6",
    								"gyrz_hist1","gyrz_hist2","gyrz_hist3","gyrz_hist4","gyrz_hist5","gyrz_hist6"};

    
    //sorted feature list
    private List<String> feature_list_sorted;

    private boolean has_acc = false;
    private boolean has_gyr = false;
    private boolean has_bar = false;

    //feature lists to be passed to FeatureExtractor (without probe name)
    private List<String> acc_feature_list,  gyr_feature_list, bar_feature_list;

    private ToneGenerator toneG;
    private boolean generate_tone;
    private long last_timestamp;

    boolean acc_writing = false;
    boolean gyr_writing = false;
    boolean bar_writing = false;

	private Thread myThread;

	//private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	Runnable myRunnable = new Runnable() {
		public void run() {
			while (extract_features) {
				//if (acc_clip.value.size()>0) {
					long t1 = System.currentTimeMillis();
					features = new ArrayList<Double>();
					
					generate_tone = false;
					
					//checking if the clip has moved since last time
					if (acc_clip.timestamp.size()>0) {
						if (acc_clip.timestamp.get(acc_clip.timestamp.size()-1)==last_timestamp) {
							Log.e("WARNING","Clip hasn't moved since last feature extraction!");
							generate_tone = true;
						} 
						else {
							last_timestamp = acc_clip.timestamp.get(acc_clip.timestamp.size()-1);
							Log.e("INFO", "n_samp = "+acc_clip.timestamp.size());
						}
					}
					
					if (has_acc) {
						while (acc_writing);
						features.addAll(f_acc.ExtractFeatures(acc_clip));
						if (acc_clip.value.size()<100) generate_tone = true;
					}
					if (has_gyr) {
						while (gyr_writing);
						features.addAll(f_gyr.ExtractFeatures(gyr_clip));
					}
					if (has_bar) {
						while (bar_writing);
						features.addAll(f_bar.ExtractFeatures(bar_clip));
					}

					//transmit feature values and time stamps
					transmitAnalysis();

					try {				
						if (generate_tone)
							toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
						//measuring the processing time
						deltat = System.currentTimeMillis() - t1;
						Log.e("INFO", "Processing Time : "+deltat+" ms");
						//accounting for the processing time / also converting from ns to ms
						long time_to_sleep = window_shift/(long)1e6 - deltat;
						//in the rare case that processing time is greater than window shift interval
						if (time_to_sleep<0) time_to_sleep = 0;
						Thread.sleep(time_to_sleep);
					} 
					catch (Exception e) {
						extract_features = false;
						e.printStackTrace();
					}

					
				//}
			}
		}
	};


	public P20FeaturesProbe() {
				
		acc_clip = new Clip(3, window_size);
		gyr_clip = new Clip(3, window_size);
		bar_clip = new Clip(1, window_size);

	    //lists to be passed to FeatureExtractor
	    acc_feature_list = new ArrayList<String>();
	    gyr_feature_list = new ArrayList<String>();
	    bar_feature_list = new ArrayList<String>();
	    //lists to sort the final feature list
	    List<String> feature_list_sorted_acc = new ArrayList<String>();
	    List<String> feature_list_sorted_gyr = new ArrayList<String>();
	    List<String> feature_list_sorted_bar = new ArrayList<String>();
	    for (String s : feature_list) {
	    	if (s.startsWith("acc")) {
	    		if (!has_acc) has_acc = true;
	    		acc_feature_list.add(s.substring(3));
	    		feature_list_sorted_acc.add(s);
	    	}
	    	if (s.startsWith("gyr")) {
	    		if (!has_gyr) has_gyr = true;
	    		gyr_feature_list.add(s.substring(3));
	    		feature_list_sorted_gyr.add(s);
	    	}
	    	if (s.startsWith("bar")) {
	    		if (!has_bar) has_bar = true;
	    		bar_feature_list.add(s.substring(3));
	    		feature_list_sorted_bar.add(s);
	    	}
	    }

	    //initializing feature extractors
	    f_acc = new FeatureExtractor(window_size, acc_feature_list, 3);
	    f_gyr = new FeatureExtractor(window_size, gyr_feature_list, 3);
	    f_bar = new FeatureExtractor(window_size, bar_feature_list, 1);

	    //creating the sorted feature list
	    feature_list_sorted = new ArrayList<String>();
	    feature_list_sorted.addAll(feature_list_sorted_acc);
	    feature_list_sorted.addAll(feature_list_sorted_gyr);
	    feature_list_sorted.addAll(feature_list_sorted_bar);

	    // creating a tone generator
	    // the second argument is the volume (0-100)
	    toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 50);
	    
	    //creating and staring the thread

		myThread = new Thread(myRunnable);

		// Just to create a dead thread for isAlive() not to complain later on. But not running the 
		// thread since the probe might be disabled.
		extract_features = false;

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
				if (has_acc) {
					acc_writing = true;
					acc_clip.add(values, timestamp);
					acc_writing = false;
				}
				break;
			case Sensor.TYPE_GYROSCOPE:
				if (has_gyr) {
					gyr_writing = true;
					gyr_clip.add(values, timestamp);
					gyr_writing = false;
				}
				break;
			case Sensor.TYPE_PRESSURE:
				if (has_bar) {
					bar_writing = true;
					bar_clip.add(values, timestamp);
					bar_writing = false;
				}
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
			bundle.putDouble(feature_list_sorted.get(i), features.get(i));
		
		bundle.putLong("PT", deltat);	// processing time
				

		//bundle.putInt("CNT", counter);
		//bundle.putLong("T", deltat);
		//bundle.putLong("Trans", t_trans);
		//bundle.putLong("Time", (System.currentTimeMillis() - (long)1.410811849e12));

		this.transmitData(this._context, bundle);
	}

}
