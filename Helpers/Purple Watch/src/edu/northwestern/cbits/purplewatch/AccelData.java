package edu.northwestern.cbits.purplewatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

// TODO: Credit https://github.com/kramimus/pebble-accel-analyzer

public class AccelData
{
    private static final String TAG = AccelData.class.getSimpleName();

    final private int x;
    final private int y;
    final private int z;

    private long timestamp = 0;
    final private boolean didVibrate;

    public AccelData(byte[] data)
    {
        x = (data[0] & 0xff) | (data[1] << 8);
        y = (data[2] & 0xff) | (data[3] << 8);
        z = (data[4] & 0xff) | (data[5] << 8);
        didVibrate = data[6] != 0;

        for (int i = 0; i < 8; i++)
        {
            timestamp |= ((long) (data[i + 7] & 0xff)) << (i * 8);
        }
    }

    public JSONObject toJson()
    {
        JSONObject json = new JSONObject();
        try
        {
            json.put("x", x);
            json.put("y", y);
            json.put("z", z);
            json.put("ts", timestamp);
            json.put("v", didVibrate);
            return json;
        }
        catch (JSONException e)
        {
            Log.w(TAG, "problem constructing accel data, skipping " + e);
        }
        return null;
    }

    public static List<AccelData> fromDataArray(byte[] data)
    {
        List<AccelData> accels = new ArrayList<AccelData>();
        for (int i = 0; i < data.length; i += 15)
        {
            accels.add(new AccelData(Arrays.copyOfRange(data, i, i + 15)));
        }
        return accels;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public void applyTimezone(TimeZone tz)
    {
        timestamp -= tz.getOffset(timestamp);
    }
}
