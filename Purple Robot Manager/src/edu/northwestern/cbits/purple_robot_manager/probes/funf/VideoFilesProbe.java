package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class VideoFilesProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.VideosProbe";
	}

	public String key()
	{
		return "videos";
	}

	protected int funfTitle()
	{
		return R.string.title_videos_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_videos_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_interaction_category);
	}
}
