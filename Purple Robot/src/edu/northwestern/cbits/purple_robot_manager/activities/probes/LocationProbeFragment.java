package edu.northwestern.cbits.purple_robot_manager.activities.probes;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import edu.northwestern.cbits.purple_robot_manager.db.ProbeValuesProvider;
import edu.northwestern.cbits.purple_robot_manager.probes.builtin.LocationProbe;

public class LocationProbeFragment extends SupportMapFragment
{
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        MapsInitializer.initialize(this.getActivity());

        List<Location> locations = new ArrayList<>();

        String tableName = this.getActivity().getIntent().getStringExtra(LocationProbeActivity.DB_TABLE_NAME);

        if (tableName == null)
            tableName = LocationProbe.DB_TABLE;

        Cursor cursor = ProbeValuesProvider.getProvider(this.getActivity()).retrieveValues(this.getActivity(),
                tableName, LocationProbe.databaseSchema());

        while (cursor.moveToNext())
        {
            Location l = new Location(this.getClass().getCanonicalName());

            l.setLatitude(cursor.getDouble(cursor.getColumnIndex(LocationProbe.LATITUDE_KEY)));
            l.setLongitude(cursor.getDouble(cursor.getColumnIndex(LocationProbe.LONGITUDE_KEY)));

            l.setTime(((long) cursor.getDouble(cursor.getColumnIndex(ProbeValuesProvider.TIMESTAMP))) * 1000);

            locations.add(l);
        }

        cursor.close();

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

            if (longitude < minLon)
                ;
            minLon = longitude;

            if (longitude > maxLon)
                maxLon = longitude;

            if (time < minTime)
                minTime = time;

            if (time > maxTime)
                maxTime = time;
        }

        GoogleMap map = this.getMap();
        map.setIndoorEnabled(true);
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        LatLng lookAt = new LatLng((minLat + ((maxLat - minLat) / 2)), (minLon + ((maxLon - minLon) / 2)));

        CameraPosition camera = CameraPosition.fromLatLngZoom(lookAt, 12);
        map.moveCamera(CameraUpdateFactory.newCameraPosition(camera));

        for (Location location : locations)
        {
            CircleOptions options = new CircleOptions();
            options.center(new LatLng(location.getLatitude(), location.getLongitude()));
            options.fillColor(Color.parseColor("#AA66CC"));
            options.strokeColor(Color.parseColor("#AA66CC"));
            options.strokeWidth(20.0f);
            options.radius(5);

            map.addCircle(options);
        }
    }

    protected boolean isRouteDisplayed()
    {
        return false;
    }
}
