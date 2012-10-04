package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class CellTowerProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.CellProbe";
	}

	public String key()
	{
		return "cell_tower";
	}

	protected int funfTitle()
	{
		return R.string.title_cell_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_cell_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_positioning_category);
	}

	public String period()
	{
		return "1200";
	}
}
