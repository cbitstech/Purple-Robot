package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;

public class BrowserSearchesProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.BrowserSearchesProbe";
	}

	public String key()
	{
		return "browser_searches";
	}

	protected int funfTitle()
	{
		return R.string.title_browser_searches_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_browser_searches_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_interaction_category);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		@SuppressWarnings("unchecked")
		ArrayList<Object> searches = (ArrayList<Object>) bundle.get("SEARCHES");

		return String.format(context.getResources().getString(R.string.summary_searches_probe), searches.size());
	}
}
