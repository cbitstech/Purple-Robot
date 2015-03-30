package edu.northwestern.cbits.purple_robot_manager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import edu.northwestern.cbits.purple_robot_manager.probes.devices.AndroidWearProbe;

public class AndroidWearService extends WearableListenerService
{
    private static final String PATH_REQUEST_DATA = "/purple-robot/request-data";

    private static GoogleApiClient _apiClient = null;

    private static void initClient(final Context context)
    {
        if (AndroidWearService._apiClient == null)
        {
            Runnable r = new Runnable()
            {
                public void run()
                {
                    GoogleApiClient.Builder builder = new GoogleApiClient.Builder(context);
                    builder.addApi(Wearable.API);
                    AndroidWearService._apiClient = builder.build();

                    ConnectionResult connectionResult = AndroidWearService._apiClient.blockingConnect(30, TimeUnit.SECONDS);

                    if (!connectionResult.isSuccess())
                    {
                        // Log.e("PR", "Failed to connect to GoogleApiClient.");
                    }
                    else
                        AndroidWearService.requestDataFromDevices(context);
                }
            };

            Thread t = new Thread(r);
            t.start();
        }
    }

    public static void requestDataFromDevices(final Context context)
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                if (AndroidWearService._apiClient == null)
                    AndroidWearService.initClient(context);
//                else
//                    Log.e("PR", "CONNECTED CLIENT: " + AndroidWearService._apiClient.isConnected());

                if (AndroidWearService._apiClient != null && AndroidWearService._apiClient.isConnected())
                {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(AndroidWearService._apiClient).await();

                    for (Node node : nodes.getNodes())
                    {
                        PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(AndroidWearService._apiClient, node.getId(), PATH_REQUEST_DATA, new byte[0]);

                        result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>()
                        {
                            @Override
                            public void onResult(MessageApi.SendMessageResult sendMessageResult)
                            {
                                if (!sendMessageResult.getStatus().isSuccess())
                                {
//                                    Log.e("PR", "Failed to send message with status code: " + sendMessageResult.getStatus().getStatusCode());
                                }
                            }
                        });
                    }
                }
            }
        };

        Thread t = new Thread(r);
        t.start();
    }

    public void onDataChanged(DataEventBuffer buffer)
    {
        if (AndroidWearService._apiClient.isConnected())
        {
            final List<DataEvent> events = FreezableUtils.freezeIterable(buffer);
            final AndroidWearService me = this;

            Runnable r = new Runnable()
            {
                public void run()
                {
                    long sleepInterval = 2000 / events.size();

                    for (DataEvent event : events)
                    {
                        if (event.getType() == DataEvent.TYPE_CHANGED)
                        {
                            DataItem item = event.getDataItem();

                            if (item.getUri().getPath().startsWith(AndroidWearProbe.URI_READING_PREFIX))
                            {
                                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                                Bundle data = dataMap.toBundle();

                                UUID uuid = UUID.randomUUID();
                                data.putString("GUID", uuid.toString());

                                LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(me);
                                Intent intent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
                                intent.putExtras(data);

                                localManager.sendBroadcast(intent);

                                Wearable.DataApi.deleteDataItems(me._apiClient, item.getUri());
                            }

                            try
                            {
                                Thread.sleep(sleepInterval);
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        else if (event.getType() == DataEvent.TYPE_DELETED)
                        {

                        }
                    }
                }
            };

            Thread t = new Thread(r);
            t.start();
        }
    }
}
