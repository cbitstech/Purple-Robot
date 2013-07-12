package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class TelephonyProbe extends Probe
{
	private static final String NETWORK_TYPE = "NETWORK_TYPE";
	private static final String CALL_STATE = "CALL_STATE";
	private static final String HAS_ICC_CARD = "HAS_ICC_CARD";
	private static final String NETWORK_COUNTRY_ISO = "NETWORK_COUNTRY_ISO";
	private static final String NETWORK_OPERATOR = "NETWORK_OPERATOR";
	private static final String SIM_COUNTRY_ISO = "SIM_COUNTRY_ISO";
	private static final String SIM_OPERATOR_NAME = "SIM_OPERATOR_NAME";
	private static final String NETWORK_OPERATOR_NAME = "SIM_OPERATOR_NAME";
	private static final String SIM_OPERATOR = "SIM_OPERATOR_NAME";
	private static final String PHONE_TYPE = "PHONE_TYPE";
	private static final String SIM_STATE = "SIM_STATE";
	private static final String DEVICE_SOFTWARE_VERSION = "DEVICE_SOFTWARE_VERSION";
	private static final String CDMA_STATION = "CDMA_STATION";
	private static final String CDMA_LONGITUDE = "CDMA_LONGITUDE";
	private static final String CDMA_SYSTEM_ID = "CDMA_SYSTEM_ID";
	private static final String CDMA_LATITUDE = "CDMA_LATITUDE";
	private static final String CDMA_NETWORK_ID = "CDMA_NETWORK_ID";
	private static final String GSM_ERROR_RATE = "GSM_ERROR_RATE";
	private static final String GSM_SIGNAL_STRENGTH = "GSM_SIGNAL_STRENGTH";
	private static final String CDMA_ECIO = "CDMA_ECIO";
	private static final String CDMA_EVDO_ECIO = "CDMA_EVDO_ECIO";
	private static final String CDMA_EVDO_DBM = "CDMA_EVDO_DBM";
	private static final String CDMA_DBM = "CDMA_DBM";
	private static final String CDMA_EVDO_SNR = "CDMA_EVDO_SNR";
	private static final String IS_FORWARDING = "IS_FORWARDING";
	private static final String SERVICE_STATE = "SERVICE_STATE";

	private static final boolean DEFAULT_ENABLED = true;

	private long _lastCheck = 0;
	private long _lastSignalCheck = 0;
	
	private boolean _isForwarding = false;

	private int _cdmaDbm = 0;
	private int _cdmaEcio = 0;
	private int _evdoDbm = 0;
	private int _evdoEcio = 0;
	private int _evdoSnr = 0;
	private int _gsmErrorRate = 0;
	private int _gsmSignalStrength = 0;
	
	private int _serviceState = 0;
	
	private PhoneStateListener _listener = null;

	public String name(Context context)
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.builtin.TelephonyProbe";
	}

	public String title(Context context)
	{
		return context.getString(R.string.title_telephony_probe);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public void enable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_telephony_enabled", true);
		
		e.commit();
	}

	public void disable(Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_probe_telephony_enabled", false);
		
		e.commit();
	}

	public boolean isEnabled(final Context context)
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		
		final TelephonyProbe me = this;
		
		if (super.isEnabled(context))
		{
			if (this._listener == null)
			{
				Looper looper = Looper.myLooper();
				
				if (looper == null)
					Looper.prepare();

				this._listener = new PhoneStateListener()
				{
					public void onCallForwardingIndicatorChanged(boolean forwarding)
					{
						if (me._isForwarding != forwarding)
						{
							me._isForwarding = forwarding;
							me._lastCheck = 0;
						}
					}
	
					public void onSignalStrengthsChanged(SignalStrength signal)
					{
						long now = System.currentTimeMillis();
						
						if (now - me._lastSignalCheck > 5000)
						{
							me._cdmaDbm = signal.getCdmaDbm();
							me._cdmaEcio = signal.getCdmaEcio();
							me._evdoDbm = signal.getEvdoDbm();
							me._evdoEcio = signal.getEvdoEcio();
							me._evdoSnr = signal.getEvdoSnr();
							
							me._gsmErrorRate = signal.getGsmBitErrorRate();
							me._gsmSignalStrength = signal.getGsmSignalStrength();
							
							me._lastCheck = 0;
							
							me._lastSignalCheck = now;
						}
					}
	
					public void onServiceStateChanged(ServiceState serviceState)
					{
						if (me._serviceState != serviceState.getState())
						{
							me._serviceState = serviceState.getState();
							me._lastCheck = 0;
						}
					}
				};
			}
			
			long now = System.currentTimeMillis();

			if (prefs.getBoolean("config_probe_telephony_enabled", TelephonyProbe.DEFAULT_ENABLED))
			{
				synchronized(this)
				{
					manager.listen(this._listener, 
							PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR |
							PhoneStateListener.LISTEN_SERVICE_STATE |
							PhoneStateListener.LISTEN_SIGNAL_STRENGTH);

					long freq = Long.parseLong(prefs.getString("config_probe_telephony_frequency", Probe.DEFAULT_FREQUENCY));

					if (now - this._lastCheck  > freq)
					{
						try
						{
							Bundle bundle = new Bundle();
							bundle.putString("PROBE", this.name(context));
							bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
	
							String operatorName = manager.getSimOperatorName();
							
							if (operatorName == null || operatorName.trim().length() == 0)
								operatorName = context.getString(R.string.label_no_operator);
							
							bundle.putInt(TelephonyProbe.CALL_STATE, manager.getCallState());
							bundle.putString(TelephonyProbe.NETWORK_COUNTRY_ISO, manager.getNetworkCountryIso());
							bundle.putString(TelephonyProbe.NETWORK_OPERATOR, manager.getNetworkOperator());
							bundle.putString(TelephonyProbe.NETWORK_OPERATOR_NAME, manager.getNetworkOperatorName());
							bundle.putString(TelephonyProbe.SIM_COUNTRY_ISO, manager.getSimCountryIso());
							bundle.putString(TelephonyProbe.SIM_OPERATOR, manager.getSimOperator());
							bundle.putString(TelephonyProbe.SIM_OPERATOR_NAME, operatorName);
							bundle.putBoolean(TelephonyProbe.HAS_ICC_CARD, manager.hasIccCard());
							bundle.putString(TelephonyProbe.NETWORK_TYPE, this.networkType(manager.getNetworkType()));
							bundle.putString(TelephonyProbe.PHONE_TYPE, this.phoneType(manager.getPhoneType()));
							bundle.putString(TelephonyProbe.SIM_STATE, this.simState(manager.getSimState()));
							bundle.putString(TelephonyProbe.DEVICE_SOFTWARE_VERSION, manager.getDeviceSoftwareVersion());
	
							bundle.putBoolean(TelephonyProbe.IS_FORWARDING, this._isForwarding);
							
							if (this._serviceState == ServiceState.STATE_EMERGENCY_ONLY)
								bundle.putString(TelephonyProbe.SERVICE_STATE, context.getString(R.string.service_state_emergency));
							else if (this._serviceState == ServiceState.STATE_IN_SERVICE)
								bundle.putString(TelephonyProbe.SERVICE_STATE, context.getString(R.string.service_state_in_service));
							if (this._serviceState == ServiceState.STATE_OUT_OF_SERVICE)
								bundle.putString(TelephonyProbe.SERVICE_STATE, context.getString(R.string.service_state_out_service));
							if (this._serviceState == ServiceState.STATE_POWER_OFF)
								bundle.putString(TelephonyProbe.SERVICE_STATE, context.getString(R.string.service_state_off));
						
	
							CellLocation location = manager.getCellLocation();
	
							if (location instanceof GsmCellLocation)
							{
								GsmCellLocation gsmLocation = (GsmCellLocation) location;
								gsmLocation.fillInNotifierBundle(bundle);
								bundle.putString(TelephonyProbe.NETWORK_TYPE, "GSM");
								
								bundle.putInt(TelephonyProbe.GSM_ERROR_RATE, this._gsmErrorRate);
								bundle.putInt(TelephonyProbe.GSM_SIGNAL_STRENGTH, this._gsmSignalStrength);
							}
							else if (location instanceof CdmaCellLocation)
							{
								CdmaCellLocation cdmaLocation = (CdmaCellLocation) location;
								cdmaLocation.fillInNotifierBundle(bundle);
								bundle.putString(TelephonyProbe.NETWORK_TYPE, "CDMA");
	
								bundle.putInt(TelephonyProbe.CDMA_STATION, cdmaLocation.getBaseStationId());
								bundle.putInt(TelephonyProbe.CDMA_LATITUDE, cdmaLocation.getBaseStationLatitude());
								bundle.putInt(TelephonyProbe.CDMA_LONGITUDE, cdmaLocation.getBaseStationLongitude());
								bundle.putInt(TelephonyProbe.CDMA_NETWORK_ID, cdmaLocation.getNetworkId());
								bundle.putInt(TelephonyProbe.CDMA_SYSTEM_ID, cdmaLocation.getSystemId());
	
								bundle.putInt(TelephonyProbe.CDMA_DBM, this._cdmaDbm);
								bundle.putInt(TelephonyProbe.CDMA_ECIO, this._cdmaEcio);
								bundle.putInt(TelephonyProbe.CDMA_EVDO_DBM, this._evdoDbm);
								bundle.putInt(TelephonyProbe.CDMA_EVDO_ECIO, this._evdoEcio);
								bundle.putInt(TelephonyProbe.CDMA_EVDO_SNR, this._evdoSnr);
							}
							else
								bundle.putString(TelephonyProbe.NETWORK_TYPE, "None");
	
							this.transmitData(context, bundle);
						}
						catch (SecurityException e)
						{
							LogManager.getInstance(context).logException(e);
						}

						this._lastCheck = now;
					}
				}

				return true;
			}
		}
		else
		{
			if (this._listener != null)
				manager.listen(this._listener, 0);
		}

		return false;
	}

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		formatted.putString(context.getString(R.string.display_telephony_operator_title), bundle.getString(TelephonyProbe.NETWORK_OPERATOR_NAME));
		formatted.putString(context.getString(R.string.display_telephony_network_title), bundle.getString(TelephonyProbe.PHONE_TYPE));

		return formatted;
	};
	
	public String summarizeValue(Context context, Bundle bundle)
	{
		String operator = bundle.getString(TelephonyProbe.SIM_OPERATOR_NAME);
		String network = bundle.getString(TelephonyProbe.NETWORK_TYPE);

		return String.format(context.getResources().getString(R.string.summary_telephony_probe), operator, network);
	}

	private String simState(int simState)
	{
		String type = "Unknown";

		switch (simState)
		{
			case TelephonyManager.SIM_STATE_ABSENT:
				type = "Absent";
				break;
			case TelephonyManager.SIM_STATE_PIN_REQUIRED:
				type = "PIN Required";
				break;
			case TelephonyManager.SIM_STATE_PUK_REQUIRED:
				type = "PUK Required";
				break;
			case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
				type = "Network Locked";
				break;
			case TelephonyManager.SIM_STATE_READY:
				type = "Ready";
				break;
		}

		return type;
	}

	private String phoneType(int phoneType)
	{
		String type = "Unknown";

		switch (phoneType)
		{
			case TelephonyManager.PHONE_TYPE_NONE:
				type = "None";
				break;
			case TelephonyManager.PHONE_TYPE_GSM:
				type = "GSM";
				break;
			case TelephonyManager.PHONE_TYPE_CDMA:
				type = "CDMA";
				break;
			case TelephonyManager.PHONE_TYPE_SIP:
				type = "SIP";
				break;
		}

		return type;
	}

	private String networkType(int networkType)
	{
		String type = "Unknown";

		switch (networkType)
		{
			case TelephonyManager.NETWORK_TYPE_GPRS:
				type = "GPRS";
				break;
			case TelephonyManager.NETWORK_TYPE_UMTS:
				type = "UTMS";
				break;
			case TelephonyManager.NETWORK_TYPE_EDGE:
				type = "EDGE";
				break;
			case TelephonyManager.NETWORK_TYPE_HSDPA:
				type = "HSDPA";
				break;
			case TelephonyManager.NETWORK_TYPE_HSUPA:
				type = "HSUPA";
				break;
			case TelephonyManager.NETWORK_TYPE_HSPA:
				type = "HSPA";
				break;
			case TelephonyManager.NETWORK_TYPE_CDMA:
				type = "CDMA";
				break;
			case TelephonyManager.NETWORK_TYPE_EVDO_0:
				type = "EVDOr0";
				break;
			case TelephonyManager.NETWORK_TYPE_EVDO_A:
				type = "EVDOrA";
				break;
			case TelephonyManager.NETWORK_TYPE_EVDO_B:
				type = "EVDOrB";
				break;
			case TelephonyManager.NETWORK_TYPE_1xRTT:
				type = "1x RTT";
				break;
			case TelephonyManager.NETWORK_TYPE_IDEN:
				type = "IDEN";
				break;
			case TelephonyManager.NETWORK_TYPE_LTE:
				type = "LTE";
				break;
			case TelephonyManager.NETWORK_TYPE_EHRPD:
				type = "EHRPD";
				break;
			case TelephonyManager.NETWORK_TYPE_HSPAP:
				type = "HSPA+";
				break;
		}

		return type;
	}

	public Map<String, Object> configuration(Context context)
	{
		Map<String, Object> map = super.configuration(context);
		
		SharedPreferences prefs = Probe.getPreferences(context);

		long freq = Long.parseLong(prefs.getString("config_probe_telephony_frequency", Probe.DEFAULT_FREQUENCY));
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
				
				e.putString("config_probe_telephony_frequency", frequency.toString());
				e.commit();
			}
		}
	}

	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_telephony_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_telephony_enabled");
		enabled.setDefaultValue(TelephonyProbe.DEFAULT_ENABLED);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_telephony_frequency");
		duration.setEntryValues(R.array.probe_satellite_frequency_values);
		duration.setEntries(R.array.probe_satellite_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);
		duration.setDefaultValue(Probe.DEFAULT_FREQUENCY);

		screen.addPreference(duration);

		return screen;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO Auto-generated method stub
	}
}
