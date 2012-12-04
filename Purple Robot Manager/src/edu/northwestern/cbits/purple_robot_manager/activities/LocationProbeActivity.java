package edu.northwestern.cbits.purple_robot_manager.activities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;

import com.actionbarsherlock.app.SherlockMapActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import edu.northwestern.cbits.purple_robot_manager.ProbeViewerActivity;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe;

public class LocationProbeActivity extends SherlockMapActivity
{
	private class PathOverlay extends Overlay
	{
		List<Location> _locations = null;
		DisplayMetrics _metrics = null;

		public PathOverlay(List<Location> locations, DisplayMetrics metrics)
	    {
	    	this._locations = new ArrayList<Location>(locations);
	    	this._metrics = metrics;
	    }

	    public void draw(Canvas canvas, MapView mapView, boolean shadow)
	    {
	        super.draw(canvas, mapView, shadow);

	    	if (shadow)
	    		return;

	    	Paint paint = new Paint();

	        paint.setDither(true);
	        paint.setColor(Color.RED);
	        paint.setStyle(Paint.Style.STROKE);
	        paint.setStrokeJoin(Paint.Join.ROUND);
	        paint.setStrokeCap(Paint.Cap.ROUND);
	        paint.setStrokeWidth(this._metrics.density * 5);

	        Projection projection = mapView.getProjection();

	        Path path = new Path();

	        Point p = new Point();

	        Location l = this._locations.get(0);

			int latitude = (int) (l.getLatitude() * 1000000);
			int longitude = (int) (l.getLongitude() * 1000000);

	        GeoPoint origin = new GeoPoint(latitude, longitude);

	        projection.toPixels(origin, p);

	        path.moveTo(p.x, p.y);
	        path.lineTo(p.x, p.y + 1);

	        for (int i = 1; i < this._locations.size(); i++)
	        {
		        l = this._locations.get(i);

				latitude = (int) (l.getLatitude() * 1000000);
				longitude = (int) (l.getLongitude() * 1000000);

				GeoPoint location = new GeoPoint(latitude, longitude);

		        projection.toPixels(location, p);

		        path.lineTo(p.x, p.y);
	        }

	        canvas.drawPath(path, paint);
	    }
	}

	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_location_activity);
    }

	protected void onResume()
	{
		super.onResume();

		MapView map = (MapView) this.findViewById(R.id.mapview);

		List<Overlay> overlays = map.getOverlays();

		overlays.clear();

		List<Location> locations = LocationProbe.cachedLocations();

		double minLat = 90;
		double maxLat = -90;
		double minLon = 180;
		double maxLon = -180;

		long minTime = Long.MAX_VALUE;
		long maxTime = Long.MIN_VALUE;

		for (Location l : locations)
		{
			double latitude = l.getLatitude();
			double longitude = l.getLongitude();

			long time = l.getTime();

			if (latitude < minLat)
				minLat = latitude;

			if (latitude > maxLat)
				maxLat = latitude;

			if (longitude < minLon);
				minLon = longitude;

			if (longitude > maxLon)
				maxLon = longitude;

			if (time < minTime)
				minTime = time;

			if (time > maxTime)
				maxTime = time;
		}

		MapController controller = map.getController();

		int centerLat = (int) ((minLat + ((maxLat - minLat) / 2)) * 1000000);
		int centerLon = (int) ((minLon + ((maxLon - minLon) / 2)) * 1000000);

		controller.setCenter(new GeoPoint(centerLat, centerLon));

		int latSpan = (int) ((maxLat - minLat) * 1000000);
		int lonSpan = (int) ((maxLon - minLon) * 1000000);

		controller.zoomToSpan(latSpan, lonSpan);

		DisplayMetrics metrics = new DisplayMetrics();

		this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		overlays.add(new PathOverlay(locations, metrics));

		this.getSupportActionBar().setTitle(R.string.title_location_history);

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

		String startDate = sdf.format(new Date(minTime));
		String endDate = sdf.format(new Date(maxTime));

		if (!startDate.equals(endDate))
			this.getSupportActionBar().setSubtitle(startDate + " - " + endDate + " (" + locations.size() + ")");
		else
			this.getSupportActionBar().setSubtitle(startDate + " (" + locations.size() + ")");
	}

	protected boolean isRouteDisplayed()
	{
		return false;
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
        MenuInflater inflater = this.getSupportMenuInflater();
        inflater.inflate(R.menu.menu_probe_activity, menu);

        return true;
	}

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
    	{
    		case R.id.menu_data_item:
				Intent dataIntent = new Intent(this, ProbeViewerActivity.class);

				dataIntent.putExtra("probe_name", this.getIntent().getStringExtra("probe_name"));
				dataIntent.putExtra("probe_bundle", this.getIntent().getParcelableExtra("probe_bundle"));

				this.startActivity(dataIntent);

    			break;
		}

    	return true;
    }
}
