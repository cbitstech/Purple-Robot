package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;

public class BatteryProbe extends PeriodFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.BatteryProbe";
	}

	public String key()
	{
		return "battery";
	}

	protected int funfTitle()
	{
		return R.string.title_battery_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_battery_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_device_category);
	}

	public String period()
	{
		return "300";
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		int level = bundle.getInt("level");

		return String.format(context.getResources().getString(R.string.summary_battery_probe), level);
	}

	public Bundle formattedBundle(Context context, Bundle bundle)
	{
		Bundle formatted = super.formattedBundle(context, bundle);

		switch(bundle.getInt("health"))
		{
			case BatteryManager.BATTERY_HEALTH_COLD:
				formatted.putString(context.getString(R.string.display_battery_health_label), context.getString(R.string.display_battery_health_cold));
				break;
			case BatteryManager.BATTERY_HEALTH_DEAD:
				formatted.putString(context.getString(R.string.display_battery_health_label), context.getString(R.string.display_battery_health_dead));
				break;
			case BatteryManager.BATTERY_HEALTH_GOOD:
				formatted.putString(context.getString(R.string.display_battery_health_label), context.getString(R.string.display_battery_health_good));
				break;
			case BatteryManager.BATTERY_HEALTH_OVERHEAT:
				formatted.putString(context.getString(R.string.display_battery_health_label), context.getString(R.string.display_battery_health_overheat));
				break;
			case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
				formatted.putString(context.getString(R.string.display_battery_health_label), context.getString(R.string.display_battery_health_over_voltage));
				break;
			case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
				formatted.putString(context.getString(R.string.display_battery_health_label), context.getString(R.string.display_battery_health_failure));
				break;
		}

		formatted.putString(context.getString(R.string.display_battery_health_tech_label), bundle.getString("technology"));

		formatted.putInt("sTATUS", bundle.getInt("status"));
		formatted.putInt("tEMPERATURE", bundle.getInt("temperature"));
		formatted.putInt("lEVEL", bundle.getInt("level"));
		formatted.putInt("vOLTAGE", bundle.getInt("voltage"));
		formatted.putInt("pLUGGED", bundle.getInt("plugged"));
		formatted.putString("tECHNOLOGY", bundle.getString("technology"));
		formatted.putString("tECHNOLOGY", bundle.getString("technology"));



		return formatted;
	};

}
