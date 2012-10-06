package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.StartActivity;

public class BrowserBookmarksProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.BrowserBookmarksProbe";
	}

	public String key()
	{
		return "browser_bookmarks";
	}

	protected int funfTitle()
	{
		return R.string.title_browser_bookmarks_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_browser_bookmarks_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_interaction_category);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		ArrayList<Object> bookmarks = (ArrayList<Object>) bundle.get("BOOKMARKS");

		return String.format(context.getResources().getString(R.string.summary_bookmarks_probe), bookmarks.size());
	}
}
