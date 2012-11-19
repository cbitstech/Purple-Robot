package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;

public class LightProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.LightSensorProbe";
	}

	public String key()
	{
		return "light";
	}

	protected int funfTitle()
	{
		return R.string.title_light_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_light_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		float lux = bundle.getFloatArray("LUX")[0];

		return String.format(context.getResources().getString(R.string.summary_light_probe), lux);
	}

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		long[] eventTimes = bundle.getLongArray("EVENT_TIMESTAMP");
		float[] lux = bundle.getFloatArray("LUX");

		SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.display_date_format));

		if (eventTimes.length > 1)
		{
			ArrayList<String> keys = new ArrayList<String>();

			Bundle readings = new Bundle();

			for (int i = 0; i < eventTimes.length; i++)
			{
				String formatString = String.format(context.getString(R.string.display_light_reading), lux[i]);
				long time = eventTimes[i];

				Date d = new Date(time / 1000000);

				String key = sdf.format(d);

				readings.putString(key, formatString);

				keys.add(key);
			}

			if (keys.size() > 0)
				readings.putStringArrayList("KEY_ORDER", keys);

			formatted.putBundle(context.getString(R.string.display_light_readings), readings);
		}
		else if (eventTimes.length > 0)
		{
			String formatString = String.format(context.getString(R.string.display_light_reading), lux[0]);

			long time = eventTimes[0];

			Date d = new Date(time / 1000000);

			formatted.putString(sdf.format(d), formatString);
		}

		return formatted;
	};
}
