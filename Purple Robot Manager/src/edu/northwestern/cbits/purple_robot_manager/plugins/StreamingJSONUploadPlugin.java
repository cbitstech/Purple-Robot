package edu.northwestern.cbits.purple_robot_manager.plugins;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

import android.app.ActivityManager.RunningTaskInfo;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.util.JsonWriter;
import android.util.Log;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class StreamingJSONUploadPlugin extends OutputPlugin
{
	private final static String CACHE_DIR = "streaming_pending_uploads";
	private int _arrayCount = 0;

	private JsonWriter _writer = null;
	
	public String[] respondsTo() 
	{
		String[] activeActions = { Probe.PROBE_READING, OutputPlugin.FORCE_UPLOAD };
		return activeActions;
	}
	
	// TODO: Pull into superclass for this and HttpUploadPlugin
	public File getPendingFolder()
	{
		SharedPreferences prefs = HttpUploadPlugin.getPreferences(this.getContext());

		File internalStorage = this.getContext().getFilesDir();

		if (prefs.getBoolean(OutputPlugin.USE_EXTERNAL_STORAGE, false))
			internalStorage = this.getContext().getExternalFilesDir(null);

		if (internalStorage != null && !internalStorage.exists())
			internalStorage.mkdirs();

		File pendingFolder = new File(internalStorage, StreamingJSONUploadPlugin.CACHE_DIR);

		if (pendingFolder != null && !pendingFolder.exists())
			pendingFolder.mkdirs();

		return pendingFolder;
	}

	public void processIntent(Intent intent) 
	{
		String action = intent.getAction();

		if (OutputPlugin.FORCE_UPLOAD.equals(action))
		{
			// TODO: Upload...
		}
		else if (Probe.PROBE_READING.equals(action))
		{
			Bundle extras = intent.getExtras();
			
			// TODO: Get rid of verbatim string...
			if (extras.containsKey("TRANSMIT") && extras.getBoolean("TRANSMIT") == false)
				return;

			long now = System.currentTimeMillis();

			try 
			{
				if (this._writer != null && this._arrayCount > 16)
				{
					this._writer.endArray();
					this._writer.flush();
					this._writer.close();
					
					this._writer = null;
					this._arrayCount = 0;
				}

				if (this._writer == null)
				{
					File f = new File(this.getPendingFolder(), now + ".streaming");
					
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
					this._writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
					
					this._writer.beginArray();
					
					this._arrayCount = 0;
				}
				
				StreamingJSONUploadPlugin.writeBundle(this.getContext(), this._writer, extras);
				this._arrayCount += 1;
			} 
			catch (IOException e) 
			{
				LogManager.getInstance(this.getContext()).logException(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void writeBundle(Context context, JsonWriter writer, Bundle bundle) 
	{
		try 
		{
			writer.beginObject();

			Map<String, Object> values = OutputPlugin.getValues(bundle);

			for (String key : values.keySet())
			{
				Object value = values.get(key);
				
				if (value == null || key == null)
				{
					// Skip
				}
				else
				{
					writer.name(key);
					
					if (value instanceof String)
					{
						writer.value((String) value);
					}
					else if (value instanceof float[])
					{
						float[] floats = (float[]) value;
						
						writer.beginArray();
						
						for (float f : floats)
							writer.value(f);
						
						writer.endArray();
					}
					else if (value instanceof int[])
					{
						int[] ints = (int[]) value;

						writer.beginArray();
						
						for (int i : ints)
							writer.value(i);
						
						writer.endArray();
					}
					else if (value instanceof long[])
					{
						long[] longs = (long[]) value;

						writer.beginArray();
						
						for (long l : longs)
							writer.value(l);
						
						writer.endArray();
					}
					else if (value instanceof double[])
					{
						double[] doubles = (double[]) value;
	
						writer.beginArray();
						
						for (double d : doubles)
							writer.value(d);
						
						writer.endArray();
					}
					else if (value instanceof Float)
					{
						Float f = (Float) value;
	
						writer.value(f);
					}
					else if (value instanceof Integer)
					{
						Integer i = (Integer) value;
	
						writer.value(i);
					}
					else if (value instanceof Long)
					{
						Long l = (Long) value;
	
						writer.value(l);
					}
					else if (value instanceof Boolean)
					{
						Boolean b = (Boolean) value;
	
						writer.value(b);
					}
					else if (value instanceof Short)
					{
						Short s = (Short) value;
	
						writer.value(s);
					}
					else if (value instanceof Double)
					{
						Double d = (Double) value;
						
						if (d.isInfinite())
							writer.value(Double.MAX_VALUE);
						else
							writer.value(d);
					}
					else if (value instanceof List)
					{
						List<Object> list = (List<Object>) value;
	
						writer.beginArray();
	
						for (Object o : list)
						{
							if (o instanceof String)
								writer.value(o.toString());
							else if (o instanceof Bundle)
								StreamingJSONUploadPlugin.writeBundle(context, writer, (Bundle) o);								
							else if (o instanceof ScanResult)
							{
								ScanResult s = (ScanResult) o;
								
								writer.beginObject();
	
								if (s.BSSID != null)
									writer.name("BSSID").value(s.BSSID);
	
								if (s.SSID != null)
									writer.name("SSID").value(s.SSID);
	
								if (s.capabilities != null)
									writer.name("Capabilities").value(s.capabilities);

								writer.name("Frequency").value(s.frequency);
								writer.name("Level dBm").value(s.level);
								
								writer.endObject();
							}
							else if (o instanceof RunningTaskInfo)
							{
								RunningTaskInfo r = (RunningTaskInfo) o;
	
								writer.beginObject();
	
								if (r.baseActivity != null)
									writer.name("Base Activity").value(r.baseActivity.getPackageName());
	
								if (r.description != null)
									writer.name("Description").value(r.description.toString());
	
								writer.name("Activity Count").value(r.numActivities);
								writer.name("Running Activity Count").value(r.numRunning);
	
								writer.endObject();
							}
							else if (o instanceof ApplicationInfo)
							{
								ApplicationInfo a = (ApplicationInfo) o;
								
								writer.value(a.packageName);
							}
							else if (o instanceof Location)
							{
								Location l = (Location) o;
	
								writer.beginObject();
								
								writer.name("Accuracy").value(l.getAccuracy());
								writer.name("Altitude").value(l.getAltitude());
								writer.name("Bearing").value(l.getBearing());
								writer.name("Latitude").value(l.getLatitude());
								writer.name("Longitude").value(l.getLongitude());
	
								if (l.getProvider() != null)
									writer.name("Provider").value(l.getProvider());
								else
									writer.name("Provider").value("Unknown");
	
								writer.name("Speed").value(l.getSpeed());
								writer.name("Timestamp").value(l.getTime());
								
								writer.endObject();
							}
							else
								Log.e("PRM", "LIST OBJ: " + o.getClass().getCanonicalName() + " IN " + key);
						}
	
						writer.endArray();
					}
					else if (value instanceof Location)
					{
						Location l = (Location) value;
	
						writer.beginObject();
						
						writer.name("Accuracy").value(l.getAccuracy());
						writer.name("Altitude").value(l.getAltitude());
						writer.name("Bearing").value(l.getBearing());
						writer.name("Latitude").value(l.getLatitude());
						writer.name("Longitude").value(l.getLongitude());

						if (l.getProvider() != null)
							writer.name("Provider").value(l.getProvider());
						else
							writer.name("Provider").value("Unknown");

						writer.name("Speed").value(l.getSpeed());
						writer.name("Timestamp").value(l.getTime());
						
						writer.endObject();
					}
					else if (value instanceof BluetoothClass)
					{
						BluetoothClass btClass = (BluetoothClass) value;
	
						writer.value(btClass.toString());
					}
					else if (value instanceof BluetoothDevice)
					{
						BluetoothDevice device = (BluetoothDevice) value;
	
						writer.beginObject();
	
						if (device.getBondState() == BluetoothDevice.BOND_BONDED)
							writer.name("Bond State").value("Bonded");
						else if (device.getBondState() == BluetoothDevice.BOND_BONDING)
							writer.name("Bond State").value("Bonding");
						else
							writer.name("Bond State").value("None");
	
						writer.name("Device Address").value(device.getAddress());
						writer.name("Device Class").value(device.getBluetoothClass().toString());

						writer.endObject();
					}
					else if (value instanceof Bundle)
						StreamingJSONUploadPlugin.writeBundle(context, writer, (Bundle) value);								
					else
						Log.e("PRM", "GOT TYPE " + value.getClass().getCanonicalName() + " FOR " + key);
				}
			}
			
			writer.endObject();
		} 
		catch (IOException e) 
		{
			LogManager.getInstance(context).logException(e);
		}
	}
}
