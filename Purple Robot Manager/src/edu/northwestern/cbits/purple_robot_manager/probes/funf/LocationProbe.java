package edu.northwestern.cbits.purple_robot_manager.probes.funf;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.StartActivity;

public class LocationProbe extends BasicFunfProbe
{
	public String funfName()
	{
		return "edu.mit.media.funf.probe.builtin.LocationProbe";
	}

	public String key()
	{
		return "location";
	}

	protected int funfTitle()
	{
		return R.string.title_location_probe;
	}

	protected int funfSummary()
	{
		return R.string.summary_location_probe_desc;
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_positioning_category);
	}

	public String period()
	{
		return "1800";
	}

	public String duration()
	{
		return "120";
	}

	public String summarizeValue(Context context, Bundle bundle)
	{
		Location location = (Location) bundle.get("LOCATION");
		double longitude = location.getLongitude();
		double latitude = location.getLatitude();

		return String.format(context.getResources().getString(R.string.summary_location_probe), latitude, longitude);
	}
}
