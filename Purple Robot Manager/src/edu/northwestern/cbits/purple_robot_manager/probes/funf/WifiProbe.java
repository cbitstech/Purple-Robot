package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.StartActivity;

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
		ArrayList<Object> scans = (ArrayList<Object>) bundle.get("SCAN_RESULTS");

		return String.format(context.getResources().getString(R.string.summary_wifi_probe), scans.size());
	}
}
