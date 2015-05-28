package edu.northwestern.cbits.purple_robot_manager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

public class MainActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_main);

        final MainActivity me = this;

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener()
        {
            public void onLayoutInflated(WatchViewStub stub)
            {
                SensorManager sensors = (SensorManager) me.getSystemService(Context.SENSOR_SERVICE);

                TextView sensorsList = (TextView) stub.findViewById(R.id.sensors_list);

                StringBuilder buffer = new StringBuilder();

                for (Sensor s : sensors.getSensorList(Sensor.TYPE_ALL))
                {
                    if (buffer.length() != 0)
                        buffer.append("\n");

                    buffer.append(s.getName() + " (" + s.getPower() + ")");
                }

                sensorsList.setText(buffer.toString());
            }
        });

        Intent serviceIntent = new Intent(this, HeartbeatService.class);
        this.startService(serviceIntent);
    }
}
