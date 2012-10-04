package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class BluetoothProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.BluetoothProbe";
	}

	public String key()
	{
		return "bluetooth";
	}

	protected int funfTitle()
	{
		return R.string.title_bluetooth_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_bluetooth_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_positioning_category);
	}

	public String period()
	{
		return "300";
	}
}
