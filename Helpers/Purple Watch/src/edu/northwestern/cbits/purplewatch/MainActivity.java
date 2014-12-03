package edu.northwestern.cbits.purplewatch;

import java.util.List;
import java.util.UUID;

import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleDataLogReceiver;

public class MainActivity extends ActionBarActivity
{
    private PebbleDataLogReceiver _receiver = null;

    private static UUID WATCHAPP_UUID = UUID.fromString("3cab0453-ff04-4594-8223-fa357112c305");

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        Log.e("PW", "ON RESUME " + PebbleKit.isDataLoggingSupported(this));

        PebbleKit.startAppOnPebble(this, WATCHAPP_UUID);

        if (this._receiver == null)
        {
            Log.e("PW", "CREATING RECV");

            this._receiver = new PebbleDataLogReceiver(WATCHAPP_UUID)
            {
                @Override
                public void onReceive(final Context context, final Intent intent)
                {
                    super.onReceive(context, intent);

                    Log.e("PW", "INTENT: " + intent);
                }

                @Override
                public void receiveData(final Context context, UUID logUuid, final Long timestamp, final Long tag,
                        final byte[] data)
                {
                    Log.e("PW", "GOT DATA: " + data.length);

                    List<AccelData> accels = AccelData.fromDataArray(data);

                    for (AccelData accel : accels)
                    {
                        try
                        {
                            Log.e("PW", "A: " + accel.toJson().toString(2));
                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFinishSession(Context context, UUID logUuid, Long timestamp, Long tag)
                {
                    super.onFinishSession(context, logUuid, timestamp, tag);

                    Log.e("PW", "FINISHED");
                }

                @Override
                public void receiveData(final Context context, UUID logUuid, final Long timestamp, final Long tag,
                        final int data)
                {
                    Log.e("PW", "TS: " + timestamp + " TAG: " + tag + " DATA: " + data);
                }

            };
        }

        PebbleKit.registerDataLogReceiver(this, this._receiver);
        PebbleKit.requestDataLogsForApp(this, WATCHAPP_UUID);
        // this.registerReceiver(this._receiver, filter);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        Log.e("PW", "UNREGISTER RECV");

        PebbleKit.closeAppOnPebble(this, WATCHAPP_UUID);

        this.unregisterReceiver(this._receiver);
    }
}
