package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;

public class AudioFilesProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.AudioFilesProbe";
	}

	public String key()
	{
		return "audio_files";
	}

	protected int funfTitle()
	{
		return R.string.title_audio_files_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_audio_files_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_interaction_category);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		ArrayList<Object> files = (ArrayList<Object>) bundle.get("AUDIO_FILES");

		return String.format(context.getResources().getString(R.string.summary_files_probe), files.size());
	}
}
