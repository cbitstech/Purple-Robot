package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import org.json.JSONException;
import org.json.JSONObject;

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

	public String summarizeValue(Context context, Object object)
	{
		if (object instanceof String)
		{
			try
			{
				String jsonString = (String) object;

				JSONObject json = new JSONObject(jsonString);

				String cellId = json.getJSONObject("extras").getJSONObject("VALUE").getString("cid");

				if (cellId != null)
					cellId = "Unknown";

				return String.format(context.getResources().getString(R.string.summary_cell_probe), cellId);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return super.summarizeValue(context, object);
	}
}
