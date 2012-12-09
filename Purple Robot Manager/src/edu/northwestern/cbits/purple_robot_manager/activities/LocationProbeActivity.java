package edu.northwestern.cbits.purple_robot_manager.activities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockMapActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

import edu.northwestern.cbits.purple_robot_manager.ProbeViewerActivity;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe;

public class LocationProbeActivity extends SherlockMapActivity
{
	private String _subtitle = null;
	private double _selectedTimestamp = 0;

	private class PointOverlay extends ItemizedOverlay<OverlayItem>
	{
		private List<Location> _locations = null;
		private LocationProbeActivity _activity = null;

		public PointOverlay(Drawable drawable)
		{
			super(drawable);
		}

		public PointOverlay(Drawable drawable, List<Location> locations, LocationProbeActivity activity)
	    {
			super(drawable);

	    	this._locations = new ArrayList<Location>(locations);
	    	this._activity = activity;

	    	this.populate();
	    }

		protected boolean onTap(int index)
		{
			Log.e("PRM", "TAPPED: " + index);

			Location l = this._locations.get(index);

			final long timestamp = l.getTime();

			Log.e("PRM", "TIMESTAMP: " + timestamp);

			final Date d = new Date((long) timestamp);

			final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss, MMM d");

			final PointOverlay me = this;

			this._activity.runOnUiThread(new Runnable()
			{
				public void run()
				{
					ActionBar actionBar = me._activity.getSupportActionBar();

					actionBar.setSubtitle(String.format(me._activity.getString(R.string.display_date_item_count), sdf.format(d), me._activity._subtitle));

					me._activity._selectedTimestamp = (double) timestamp;
				}
			});

			return true;
		}

		protected OverlayItem createItem(int i)
		{
			Location l = this._locations.get(i);

			int latitude = (int) (l.getLatitude() * 1000000);
			int longitude = (int) (l.getLongitude() * 1000000);

	        GeoPoint origin = new GeoPoint(latitude, longitude);

			return new OverlayItem(origin, "", "");
		}

		public int size()
		{
			return this._locations.size();
		}
	}

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

		this.refresh();
	}

	public void refresh()
	{
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

		final DisplayMetrics metrics = new DisplayMetrics();

		this.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		PathOverlay pathOverlay = new PathOverlay(locations, metrics);
		overlays.add(pathOverlay);

		Drawable d = new Drawable()
		{
			public void draw(Canvas canvas)
			{
		    	Paint paint = new Paint();

		        paint.setDither(true);
		        paint.setColor(Color.DKGRAY);
		        paint.setStyle(Paint.Style.STROKE);
		        paint.setStrokeJoin(Paint.Join.ROUND);
		        paint.setStrokeCap(Paint.Cap.ROUND);
		        paint.setStrokeWidth(metrics.density * 15);

		        Path path = new Path();

		        path.moveTo(0, 0);
		        path.lineTo(1.0f, 0.0f);
		        path.lineTo(1.0f, 1.0f);
		        path.lineTo(0.0f, 1.0f);

		        canvas.drawPath(path, paint);
			}

			public int getOpacity()
			{
				return PixelFormat.OPAQUE;
			}

			public void setAlpha(int alpha)
			{

			}

			public void setColorFilter(ColorFilter cf)
			{

			}
		};

		PointOverlay pointOverlay = new PointOverlay(d, locations, this);
		overlays.add(pointOverlay);

		this.getSupportActionBar().setTitle(R.string.title_location_history);

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

		String startDate = sdf.format(new Date(minTime));
		String endDate = sdf.format(new Date(maxTime));

		if (!startDate.equals(endDate))
			this._subtitle = startDate + " - " + endDate + " (" + locations.size() + ")";
		else
			this._subtitle = startDate + " (" + locations.size() + ")";

		this.getSupportActionBar().setSubtitle(this._subtitle);
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

    		case R.id.menu_new_label:
    			if (this._selectedTimestamp == 0)
    				Toast.makeText(this, R.string.message_missing_timestamp, Toast.LENGTH_LONG).show();
    			else
    			{

        			Intent labelIntent = new Intent(this, LabelActivity.class);
        			labelIntent.putExtra(LabelActivity.TIMESTAMP, this._selectedTimestamp);
        			labelIntent.putExtra(LabelActivity.LABEL_CONTEXT, this.getIntent().getStringExtra("probe_name"));

        			this.startActivity(labelIntent);
    			}

    			break;

    		case R.id.menu_refresh:
    			this.refresh();

    			break;
    	}

    	return true;
    }
}
