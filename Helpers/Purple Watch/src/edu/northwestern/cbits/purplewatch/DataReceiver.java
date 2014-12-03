package edu.northwestern.cbits.purplewatch;

import java.util.UUID;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleDataLogReceiver;

public class DataReceiver extends PebbleDataLogReceiver
{
    private static boolean _inited = false;

    protected DataReceiver(UUID uuid)
    {
        super(uuid);
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (DataReceiver._inited)
            return;

        Log.e("PW", "INITING RECEIVER");
        DataReceiver receiver = new DataReceiver(UUID.fromString("3cab0453-ff04-4594-8223-fa357112c305"));

        PebbleKit.registerDataLogReceiver(context, receiver);

        super.onReceive(context, intent);
    }

    @Override
    public void receiveData(final Context context, UUID logUuid, final Long timestamp, final Long tag, final byte[] data)
    {
        Log.e("PR", "GOT DATA: " + data.length);
    }

    @Override
    public void onFinishSession(Context context, UUID logUuid, Long timestamp, Long tag)
    {
        super.onFinishSession(context, logUuid, timestamp, tag);

        Log.e("PW", "FINISHED");

        // Session is finished, use the data!
    }
}
