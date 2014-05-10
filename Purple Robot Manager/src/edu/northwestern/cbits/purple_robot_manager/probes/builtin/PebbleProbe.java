package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.Map;
import java.util.UUID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class PebbleProbe extends Probe
{
	private static final boolean DEFAULT_ENABLED = false;

	public static final String INTENT_DL_ACK_DATA = "com.getpebble.action.dl.ACK_DATA";
	public static final String INTENT_DL_REQUEST_DATA = "com.getpebble.action.dl.REQUEST_DATA";
    public static final String INTENT_DL_FINISH_SESSION = "com.getpebble.action.dl.FINISH_SESSION";
    public static final String INTENT_DL_RECEIVE_DATA = "com.getpebble.action.dl.RECEIVE_DATA";
    public static final String APP_UUID = "uuid";
    public static final String PBL_DATA_ID = "pbl_data_id";
    public static final String PBL_DATA_TYPE = "pbl_data_type";
    public static final String PBL_DATA_OBJECT = "pbl_data_object";
    public static final String DATA_LOG_UUID = "data_log_uuid";
    public static final String DATA_LOG_TIMESTAMP = "data_log_timestamp";
    public static final String DATA_LOG_TAG = "data_log_tag";
    
    public static enum PebbleDataType {
        /**
         * The byte[].
         */
        BYTES(0x00),

        /**
         * The UnsignedInteger.
         */
        UINT(0x02),

        /**
         * The Integer.
         */
        INT(0x03),

        /**
         * The Invalid.
         */
        INVALID(0xff);

        /**
         * The ord.
         */
        public final byte ord;

        /**
         * Instantiates a new pebble data type.
         */
        private PebbleDataType(int ord) {
            this.ord = (byte) ord;
        }

        /**
         * Instantiates a new pebble data type from a byte.
         */
        public static PebbleDataType fromByte(byte b) {
            for (PebbleDataType type : values()) {
                if (type.ord == b) {
                    return type;
                }
            }
            return null;
        }
    }

	private BroadcastReceiver _receiver = null;
	
	private long _lastCheck = 0;
	
	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.PebbleProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_pebble_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_misc_category);
	}
	
	public boolean isEnabled(final Context context)
	{
		if (super.isEnabled(context))
		{
			SharedPreferences prefs = Probe.getPreferences(context);

			if (prefs.getBoolean("config_probe_pebble_enabled", PebbleProbe.DEFAULT_ENABLED))
			{
				if (this._receiver == null)
				{
					final PebbleProbe me = this;
					
					/* this._receiver = new BroadcastReceiver()
					{
						public void onReceive(Context context, Intent intent) 
						{
							Bundle bundle = new Bundle();

							bundle.putString("PROBE", me.name(context));
							bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
							bundle.putInt("DEVICE_COUNT", bundleDevices.size());
							bundle.putParcelableArrayList("DEVICES", bundleDevices);
							
							if (controller != null)
								bundle.putBundle("CONTROLLER", controller);

							me.transmitData(context, bundle);
						}
					};
					*/

			        IntentFilter filter = new IntentFilter();
			        filter.addAction(INTENT_DL_RECEIVE_DATA);
			        filter.addAction(INTENT_DL_FINISH_SESSION);

			        this._receiver = new BroadcastReceiver()
			        {
			            public void onReceive(final Context context, final Intent intent) 
			            {
//			                final UUID receivedUuid = (UUID) intent.getSerializableExtra(APP_UUID);
			                
			                Log.e("PR", "DATA FROM " + intent.getAction());

			                try 
			                {
//			                    final UnsignedInteger timestamp;
//			                    final UnsignedInteger tag;

			                    final UUID logUuid = (UUID) intent.getSerializableExtra(DATA_LOG_UUID);

			                    if (logUuid == null) throw new IllegalArgumentException();

//			                    timestamp = (UnsignedInteger) intent.getSerializableExtra(DATA_LOG_TIMESTAMP);

//			                    if (timestamp == null) throw new IllegalArgumentException();

//			                    tag = (UnsignedInteger) intent.getSerializableExtra(DATA_LOG_TAG);
			                    
//			                    if (tag == null) throw new IllegalArgumentException();

			                    if (intent.getAction() == INTENT_DL_RECEIVE_DATA) 
			                    {
						           final int dataId = intent.getIntExtra(PBL_DATA_ID, -1);
			                    	/*
   						           if (dataId < 0) throw new IllegalArgumentException();
						
						           final PebbleDataType type = PebbleDataType.fromByte(intent.getByteExtra(PBL_DATA_TYPE, PebbleDataType.INVALID.ord));
						           if (type == null) throw new IllegalArgumentException();
						
						           switch (type) {
						               case BYTES:
//						                   byte[] bytes = Base64.decode(intent.getStringExtra(PBL_DATA_OBJECT), Base64.NO_WRAP);
//						                   if (bytes == null) {
//						                       throw new IllegalArgumentException();
//						                   }
//						
//						                   receiveData(context, logUuid, timestamp, tag, bytes);

						            	   Log.e("PR", "RECV BYTES: " + intent.getStringExtra(PBL_DATA_OBJECT));
						            	   break;
						               case UINT:
//						                   UnsignedInteger uint = (UnsignedInteger) intent.getSerializableExtra(PBL_DATA_OBJECT);
//						                   if (uint == null) {
//						                       throw new IllegalArgumentException();
//						                   }
//						
//						                   receiveData(context, logUuid, timestamp, tag, uint);

						            	   Log.e("PR", "RECV UINT");

						                   break;
						               case INT:
						                   Integer i = (Integer) intent.getSerializableExtra(PBL_DATA_OBJECT);
//						                   if (i == null) {
//						                       throw new IllegalArgumentException();
//						                   }
						
//						                   receiveData(context, logUuid, timestamp, tag, i.intValue());

						            	   Log.e("PR", "RECV INT: " + i);

						                   break;
						               default:
						                   throw new IllegalArgumentException("Invalid type:" + type.toString());
						           }
						*/
						           final Intent ackIntent = new Intent(INTENT_DL_ACK_DATA);
						           ackIntent.putExtra(DATA_LOG_UUID, logUuid);
						           ackIntent.putExtra(PBL_DATA_ID, dataId);
						           context.sendBroadcast(ackIntent);
			                    }
			                    else if (intent.getAction() == INTENT_DL_FINISH_SESSION) 
			                    {
//			                        handleFinishSessionIntent(context, intent, logUuid, timestamp, tag);
			                    }
			                } catch (IllegalArgumentException e) {
			                    e.printStackTrace();
			                    return;
			                }
			            }
			        };
			        
			        Log.e("PR", "REGISTERED PEBBLE RECEIVER");
					
					context.registerReceiver(this._receiver, filter);
				}
				
				final long now = System.currentTimeMillis();
				
				synchronized(this)
				{
					long freq = Long.parseLong(prefs.getString("config_probe_pebble_frequency", Probe.DEFAULT_FREQUENCY));
					
					if (now - this._lastCheck  > freq)
					{
						this._lastCheck = now;
					}
				}
				
				return true;
			}
		}
		
		if (this._receiver != null)
		{
			context.unregisterReceiver(this._receiver);
			this._receiver = null;
		}

        final Intent requestIntent = new Intent(INTENT_DL_REQUEST_DATA);
        requestIntent.putExtra(APP_UUID, UUID.fromString("3cab0453-ff04-4594-8223-fa357112c305"));
        context.sendBroadcast(requestIntent);

		return false;
	}
	
	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_pebble_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_pebble_enabled", false);
		
		e.commit();
	}

/*	public String summarizeValue(Context context, Bundle bundle)
	{
		int count = (int) bundle.getDouble("DEVICE_COUNT");
		
		if (bundle.containsKey("CONTROLLER"))
		{
			Bundle controller = bundle.getBundle("CONTROLLER");
			
			double level = controller.getDouble(PebbleProbe.LEVEL_VALUE) / 2.55;
			
			return context.getString(R.string.summary_pebble_probe_controller, level, count);
		}

		return context.getString(R.string.summary_pebble_probe, count);
	}
*/
	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);

		long freq = Long.parseLong(prefs.getString("config_probe_pebble_frequency", Probe.DEFAULT_FREQUENCY));
		
		map.put(Probe.PROBE_FREQUENCY, freq);
		
		return map;
	}
	
	public void updateFromMap(Context context, Map<String, Object> params) 
	{
		super.updateFromMap(context, params);
		
		if (params.containsKey(Probe.PROBE_FREQUENCY))
		{
			Object frequency = params.get(Probe.PROBE_FREQUENCY);
			
			if (frequency instanceof Long)
			{
				SharedPreferences prefs = Probe.getPreferences(context);
				Editor e = prefs.edit();
				
				e.putString("config_probe_pebble_frequency", frequency.toString());
				e.commit();
			}
		}
	}

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_pebble_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_pebble_enabled");
		enabled.setDefaultValue(PebbleProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_pebble_frequency");
		duration.setEntryValues(R.array.probe_satellite_frequency_values);
		duration.setEntries(R.array.probe_satellite_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);
		duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);
		
		// TODO: Add username, password, server, site fields...

		screen.addPreference(duration);

		return screen;
	}

	public String summary(Context context) 
	{
		return context.getString(R.string.summary_pebble_probe_desc);
	}
}
