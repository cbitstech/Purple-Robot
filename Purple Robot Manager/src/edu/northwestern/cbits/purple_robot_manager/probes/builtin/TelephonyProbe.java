package edu.northwestern.cbits.purple_robot_manager.probes.builtin;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import edu.northwestern.cbits.purple_robot_manager.R;
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

	private long _lastCheck = 0;

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

	public boolean isEnabled(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		if (super.isEnabled(context))
		{
			long now = System.currentTimeMillis();

			if (prefs.getBoolean("config_probe_telephony_enabled", true))
			{
				synchronized(this)
				{
					long freq = Long.parseLong(prefs.getString("config_probe_telephony_frequency", "300000"));

					if (now - this._lastCheck  > freq)
					{
						Bundle bundle = new Bundle();
						bundle.putString("PROBE", this.name(context));
						bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

						TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

						bundle.putInt(TelephonyProbe.CALL_STATE, manager.getCallState());
						bundle.putString(TelephonyProbe.NETWORK_COUNTRY_ISO, manager.getNetworkCountryIso());
						bundle.putString(TelephonyProbe.NETWORK_OPERATOR, manager.getNetworkOperator());
						bundle.putString(TelephonyProbe.NETWORK_OPERATOR_NAME, manager.getNetworkOperatorName());
						bundle.putString(TelephonyProbe.SIM_COUNTRY_ISO, manager.getSimCountryIso());
						bundle.putString(TelephonyProbe.SIM_OPERATOR, manager.getSimOperator());
						bundle.putString(TelephonyProbe.SIM_OPERATOR_NAME, manager.getSimOperatorName());
						bundle.putBoolean(TelephonyProbe.HAS_ICC_CARD, manager.hasIccCard());
						bundle.putString(TelephonyProbe.NETWORK_TYPE, this.networkType(manager.getNetworkType()));
						bundle.putString(TelephonyProbe.PHONE_TYPE, this.phoneType(manager.getPhoneType()));
						bundle.putString(TelephonyProbe.SIM_STATE, this.simState(manager.getSimState()));
						bundle.putString(TelephonyProbe.DEVICE_SOFTWARE_VERSION, manager.getDeviceSoftwareVersion());

						CellLocation location = manager.getCellLocation();

						if (location instanceof GsmCellLocation)
						{
							GsmCellLocation gsmLocation = (GsmCellLocation) location;
							gsmLocation.fillInNotifierBundle(bundle);
							bundle.putString(TelephonyProbe.NETWORK_TYPE, "GSM");
						}
						else if (location instanceof CdmaCellLocation)
						{
							CdmaCellLocation cdmaLocation = (CdmaCellLocation) location;
							cdmaLocation.fillInNotifierBundle(bundle);
							bundle.putString(TelephonyProbe.NETWORK_TYPE, "CDMA");
						}
						else
							bundle.putString(TelephonyProbe.NETWORK_TYPE, "None");

						this.transmitData(context, bundle);

						this._lastCheck = now;
					}
				}

				return true;
			}
		}

		return false;
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		String operator = bundle.getString(TelephonyProbe.SIM_OPERATOR_NAME);
		String network = bundle.getString(TelephonyProbe.NETWORK_TYPE);

		return String.format(context.getResources().getString(R.string.summary_telephony_probe), operator, network);
	}

/*
	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		@SuppressWarnings("unchecked")
		ArrayList<Bundle> array = (ArrayList<Bundle>) bundle.get(HardwareInformationProbe.DEVICES);
		int count = bundle.getInt(HardwareInformationProbe.DEVICES_COUNT);

		Bundle devicesBundle = this.bundleForDevicesArray(context, array);

		formatted.putBundle(String.format(context.getString(R.string.display_bluetooth_devices_title), count), devicesBundle);

		return formatted;
	};
*/

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

	@SuppressWarnings("deprecation")
	public PreferenceScreen preferenceScreen(PreferenceActivity activity)
	{
		PreferenceManager manager = activity.getPreferenceManager();

		PreferenceScreen screen = manager.createPreferenceScreen(activity);
		screen.setTitle(this.title(activity));
		screen.setSummary(R.string.summary_telephony_probe_desc);

		CheckBoxPreference enabled = new CheckBoxPreference(activity);
		enabled.setTitle(R.string.title_enable_probe);
		enabled.setKey("config_probe_telephony_enabled");
		enabled.setDefaultValue(true);

		screen.addPreference(enabled);

		ListPreference duration = new ListPreference(activity);
		duration.setKey("config_probe_telephony_frequency");
		duration.setDefaultValue("300000");
		duration.setEntryValues(R.array.probe_satellite_frequency_values);
		duration.setEntries(R.array.probe_satellite_frequency_labels);
		duration.setTitle(R.string.probe_frequency_label);

		screen.addPreference(duration);

		return screen;
	}

	public void updateFromJSON(Context context, JSONObject json) throws JSONException
	{
		// TODO Auto-generated method stub
	}
}
