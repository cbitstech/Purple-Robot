package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
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
		return R.string.summary_browser_searches_probe;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_interaction_category);
	}

	public String summarizeValue(Context context, Object object)
	{
		if (object instanceof String)
		{
			try
			{
				String jsonString = (String) object;

				JSONObject json = new JSONObject(jsonString);

				JSONArray bookmarks = json.getJSONObject("extras").getJSONObject("VALUE").getJSONArray("SEARCHES");

				return String.format(context.getResources().getString(R.string.summary_searches_probe), bookmarks.length());
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return super.summarizeValue(context, object);
	}
}
