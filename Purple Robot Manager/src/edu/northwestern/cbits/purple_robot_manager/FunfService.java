package edu.northwestern.cbits.purple_robot_manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import edu.mit.media.funf.CustomizedIntentService;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.storage.BundleSerializer;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;

public class FunfService extends CustomizedIntentService
{
	public static final String TAG = "PurpleRobotPipeline";
	public static final String DEFAULT_PIPELINE_NAME = TAG;

	private static final String PREFIX = "edu.mit.media.funf.";

	public static final String ACTION_RELOAD = PREFIX + "reload";
	public static final String ACTION_ENABLE = PREFIX + "enable";
//	public static final String ACTION_DISABLE = PREFIX + "disable";
	public static final String EXTRA_FORCE_UPLOAD = "FORCE";
	public static final String ACTION_DISABLE = "PROBE_INTERNAL_DISABLE";


	private final IBinder mBinder = new LocalBinder();

	private boolean _isEnabled = true;

	public FunfService()
	{
		super("PurpleRobotPipeline");
	}

	public void onCreate()
	{
		super.onCreate();

		this.ensureServicesAreRunning();
	}

	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// HACK: Send a fake start id to prevent this service from being stopped
		// This is so we could use all of the other features of Intent service without rewriting them

		int FAKE_START_ID = 98723546;

		return super.onStartCommand(intent, flags, FAKE_START_ID);
	}

	protected void onHandleIntent(Intent intent)
	{
		String action = intent.getAction();

		if (ACTION_RELOAD.equals(action))
			reload();
		else if(ACTION_ENABLE.equals(action))
			setEnabled(true);
		else if(ACTION_DISABLE.equals(action))
			setEnabled(false);
		else if (Probe.ACTION_DATA.equals(action))
			this.onDataReceived(intent.getExtras());
		else if (Probe.ACTION_STATUS.equals(action))
			this.onStatusReceived(new Probe.Status(intent.getExtras()));
		else if (Probe.ACTION_DETAILS.equals(action))
			this.onDetailsReceived(new Probe.Details(intent.getExtras()));
	}

	public void reload()
	{
		this.removeProbeRequests();

		if (isEnabled())
			ensureServicesAreRunning();
	}

	public void ensureServicesAreRunning()
	{
		if (this.isEnabled())
			this.sendProbeRequests();
	}

	protected PendingIntent getCallback()
	{
		return PendingIntent.getService(this, 0, new Intent(this, this.getClass()), PendingIntent.FLAG_UPDATE_CURRENT);
	}

	public void sendProbeRequests()
	{
		Map<String,Bundle[]> dataRequests = ProbeManager.getDataRequests(this);

		// ACTION_DISABLE
		for (String probeName : dataRequests.keySet())
		{
			this.sendProbeRequest(probeName, dataRequests.get(probeName));
		}
	}

	public void sendProbeRequest(String probeName, Bundle[] requests)
	{
		if (requests == null)
			requests = new Bundle[]{};

		ArrayList<Bundle> dataRequest = new ArrayList<Bundle>(Arrays.asList(requests));

		Intent request = new Intent(Probe.ACTION_REQUEST);
		request.setClassName(this, probeName);
		request.putExtra(Probe.CALLBACK_KEY, this.getCallback());
		request.putExtra(Probe.REQUESTS_KEY, dataRequest);

		this.startService(request);
	}

	private void removeProbeRequests()
	{
		this.getCallback().cancel();
	}

	public String getPipelineName()
	{
		return DEFAULT_PIPELINE_NAME;
	}

	public boolean isEnabled()
	{
		// TODO: Move to preferences?

		return this._isEnabled;
	}

	public boolean setEnabled(boolean enabled)
	{
		// TODO: Move to preferences?

		this._isEnabled = enabled;

		return true;
	}

	public static class BundleToJson implements BundleSerializer
	{
		public String serialize(Bundle bundle)
		{
			return JsonUtils.getGson().toJson(Utils.getValues(bundle));
		}
	}

	public BundleSerializer getBundleSerializer()
	{
		return new BundleToJson();
	}

	public void onDataReceived(Bundle data)
	{
		LocalBroadcastManager localManager = LocalBroadcastManager.getInstance(this);
		Intent intent = new Intent(edu.northwestern.cbits.purple_robot_manager.probes.Probe.PROBE_READING);
		intent.putExtras(data);

		localManager.sendBroadcast(intent);
	}

	public void onStatusReceived(Probe.Status status)
	{
		// TODO:
	}

	public void onDetailsReceived(Probe.Details details)
	{
		// TODO:
	}

	protected void onEndOfQueue()
	{
		// nothing
	}

	public class LocalBinder extends Binder
	{
		public FunfService getService()
		{
            return FunfService.this;
        }
    }

	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}
}
