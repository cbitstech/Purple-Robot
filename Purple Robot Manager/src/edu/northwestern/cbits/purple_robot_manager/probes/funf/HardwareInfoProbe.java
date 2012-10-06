package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.StartActivity;

public class HardwareInfoProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.HardwareInfoProbe";
	}

	public String key()
	{
		return "hardware_info";
	}

	protected int funfTitle()
	{
		return R.string.title_hardware_info_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_hardware_info_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_category);
	}

	public String period()
	{
		return "300";
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		String model = bundle.getString("MODEL");
		String mac = bundle.getString("WIFI_MAC");

		return String.format(context.getResources().getString(R.string.summary_hardware_info_probe), model, mac);
	}
}
