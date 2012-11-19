package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import java.util.ArrayList;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;

public class WifiProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.WifiProbe";
	}

	public String key()
	{
		return "wifi";
	}

	protected int funfTitle()
	{
		return R.string.title_wifi_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_wifi_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_positioning_category);
	}

	public String period()
	{
		return "1200";
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		@SuppressWarnings("unchecked")
		ArrayList<Object> scans = (ArrayList<Object>) bundle.get("SCAN_RESULTS");

		return String.format(context.getResources().getString(R.string.summary_wifi_probe), scans.size());
	}

	private Bundle bundleForScanResultArray(Context context, ArrayList<ScanResult> objects)
	{
		Bundle bundle = new Bundle();

		for (ScanResult value : objects)
		{
			String key = String.format(context.getString(R.string.display_wifi_scan_result_title), value.BSSID, value.capabilities);
			String keyValue = String.format(context.getString(R.string.display_wifi_scan_result_summary), value.SSID, value.frequency, value.level);

			bundle.putString(key, keyValue);
		}

		return bundle;
	}

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		@SuppressWarnings("unchecked")
		ArrayList<ScanResult> array = (ArrayList<ScanResult>) bundle.get("SCAN_RESULTS");

		Bundle scanBundle = this.bundleForScanResultArray(context, array);

		formatted.putBundle(context.getString(R.string.display_wifi_scan_results_title), scanBundle);

		return formatted;
	};
}
