package edu.northwestern.cbits.purple_robot_manager;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import edu.northwestern.cbits.purple_robot_manager.probes.devices.AndroidWearProbe;

public class AndroidWearService extends WearableListenerService
{
    private static GoogleApiClient _apiClient = null;


    public void onDataChanged(DataEventBuffer buffer)
    {
        Log.e("PR", "DATA CHANGED VIA SERVICE");

        if (AndroidWearService._apiClient == null)
        {
            GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this);
            builder.addApi(Wearable.API);
            AndroidWearService._apiClient = builder.build();

            ConnectionResult connectionResult = AndroidWearService._apiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess())
            {
                Log.e("PR", "Failed to connect to GoogleApiClient.");
                return;
            }
        }

        Log.e("PR", "HAVE API CLIENT - CONTINUE...");

        final List<DataEvent> events = FreezableUtils.freezeIterable(buffer);

        for (DataEvent event : events)
        {
            if (event.getType() == DataEvent.TYPE_CHANGED)
            {
                DataItem item = event.getDataItem();

                Log.e("PR", "GOT ITEM: " + item.getUri());

                if (item.getUri().getPath().startsWith(AndroidWearProbe.URI_READING_PREFIX))
                {
                    Log.e("PR", "LOGGING WEAR PAYLOAD...");

                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                    Bundle data = dataMap.toBundle();

                    UUID uuid = UUID.randomUUID();
                    data.putString("GUID", uuid.toString());

                    LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this);
                    Intent intent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
                    intent.putExtras(data);

                    localManager.sendBroadcast(intent);

                    Wearable.DataApi.deleteDataItems(this._apiClient, item.getUri());
                }
            }
            else if (event.getType() == DataEvent.TYPE_DELETED)
            {

            }
        }
    }
}
