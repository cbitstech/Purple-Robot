package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class ImageFilesProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.ImagesProbe";
	}

	public String key()
	{
		return "image_files";
	}

	protected int funfTitle()
	{
		return R.string.title_image_files_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_image_files_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_interaction_category);
	}
}
