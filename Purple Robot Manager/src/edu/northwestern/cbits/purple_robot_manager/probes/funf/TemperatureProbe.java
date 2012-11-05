package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import edu.northwestern.cbits.purple_robot_manager.R;
import android.content.Context;
import android.os.Bundle;

public class TemperatureProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.TemperatureSensorProbe";
	}

	public String key()
	{
		return "temperature";
	}

	protected int funfTitle()
	{
		return R.string.title_temperature_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_temperature_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_environment_category);
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		float pressure = bundle.getFloatArray("TEMPERATURE")[0];

		return String.format(context.getResources().getString(R.string.summary_pressure_probe), pressure);
	}

}
