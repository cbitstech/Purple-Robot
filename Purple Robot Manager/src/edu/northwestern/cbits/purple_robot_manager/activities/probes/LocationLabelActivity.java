package edu.northwestern.cbits.purple_robot_manager.activities.probes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe;
import edu.northwestern.cbits.purple_robot_manager.util.DBSCAN;
import edu.northwestern.cbits.purple_robot_manager.util.DBSCAN.Cluster;
import edu.northwestern.cbits.purple_robot_manager.util.DBSCAN.Point;

public class LocationLabelActivity extends MapActivity 
{
	ArrayList<Cluster> _clusters = new ArrayList<Cluster>();
	
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_location_label_activity);
        
        DBSCAN dbscan = new DBSCAN(0.0005, 5);

		Cursor cursor = ProbeValuesProvider.getProvider(this).retrieveValues(this, LocationProbe.DB_TABLE, LocationProbe.databaseSchema());

		while (cursor.moveToNext())
		{
			dbscan.addPoint(dbscan.new Point(cursor.getDouble(cursor.getColumnIndex(LocationProbe.LATITUDE_KEY)), cursor.getDouble(cursor.getColumnIndex(LocationProbe.LONGITUDE_KEY))));
		}

		cursor.close();
		
		this._clusters.addAll(dbscan.calculate());
		
		Collections.sort(this._clusters, new Comparator<Cluster>()
		{
			public int compare(Cluster one, Cluster two) 
			{
				if (one.population() > two.population())
					return -1;
				else if (one.population() < two.population())
					return 1;
							
				return 0;
			}
		});

		Log.e("PR", "FOUND " + this._clusters.size() + " CLUSTERS");
		
		MapView map = (MapView) this.findViewById(R.id.map_view);
		map.setBuiltInZoomControls(true);

		List<Overlay> overlays = map.getOverlays();
		
		for (int i = 0; i < this._clusters.size(); i++)
		{
			Cluster cluster = this._clusters.get(i);
			
			int color = Color.BLACK;
			
			switch (i % 5)
			{
				case 0:
					color = Color.parseColor("#33B5E5");
					break;
				case 1:
					color = Color.parseColor("#AA66CC");
					break;
				case 2:
					color = Color.parseColor("#99CC00");
					break;
				case 3:
					color = Color.parseColor("#FFBB33");
					break;
				case 4:
					color = Color.parseColor("#FF4444");
					break;
			}
			
			for (Point p : cluster.getPoints())
			{
				CircleOverlay circle = new CircleOverlay(this, p.x(), p.y(), color);
				
				overlays.add(circle);
			}
		}
    }
	
	private class CircleOverlay extends Overlay 
	{
	    private Context _context = null;
	    private double _latitude = 0;
	    private double _longitude = 0;
	    private int _color = Color.BLUE;

	    public CircleOverlay(Context context, double latitude, double longitude, int color) 
	    {
	    	this._context = context;
	    	this._latitude = latitude;
	    	this._longitude = longitude;
	    	this._color = color;
	    }

	    public void draw(Canvas canvas, MapView mapView, boolean shadow) 
	    {
	        super.draw(canvas, mapView, shadow); 

	        if (shadow) 
	        	return; // Ignore the shadow layer

	        Projection projection = mapView.getProjection();

	        android.graphics.Point pt = new android.graphics.Point();

	        GeoPoint geo = new GeoPoint((int) (this._latitude * 1e6), (int)(this._longitude * 1e6));

	        projection.toPixels(geo, pt);

	        Paint innerCirclePaint = new Paint();
	        innerCirclePaint.setColor(this._color);
	        innerCirclePaint.setAlpha(255);
	        innerCirclePaint.setAntiAlias(true);

	        innerCirclePaint.setStyle(Paint.Style.FILL);

	        canvas.drawCircle((float)pt.x, (float)pt.y, 15, innerCirclePaint);
	    }
	}

	protected boolean isRouteDisplayed() 
	{
		return false;
	}
}
