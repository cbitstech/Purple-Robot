package edu.northwestern.cbits.purple_robot_manager.probes.features;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.provider.CallLog;
import android.telephony.PhoneNumberUtils;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class CallHistoryFeature extends Feature
{
	private static final String TOTAL = "TOTAL";
	private static final String OUTCOMING_COUNT = "OUTGOING_COUNT";
	private static final String STRANGER_COUNT = "STRANGER_COUNT";
	private static final String ACQUAINTANCE_RATIO = "ACQUAINTANCE_RATIO";
	private static final String AVG_DURATION = "AVG_DURATION";
	private static final String INCOMING_COUNT = "INCOMING_COUNT";
	private static final String ACQUAINTANCE_COUNT = "ACQUIANTANCE_COUNT";
	private static final String INCOMING_RATIO = "INCOMING_RATIO";
	private static final String TOTAL_DURATION = "TOTAL_DURATION";
	private static final String MAX_DURATION = "MAX_DURATION";
	private static final String STD_DEVIATION = "STD_DEVIATION";
	private static final String MIN_DURATION = "MIN_DURATION";
	private static final String WINDOW_SIZE = "WINDOW_SIZE";
	private static final String WINDOWS = "WINDOWS";
	private static final String ACK_COUNT = "ACK_COUNT";
	private static final String NEW_COUNT = "NEW_COUNT";
	private static final String ACK_RATIO = "ACK_RATIO";
	private static final String CONTACT_ANALYSES = "CONTACT_ANALYSES";
	private static final String IS_ACQUAINTANCE = "IS_ACQUAINTANCE";
	private static final String IDENTIFIER = "IDENTIFIER";

	private long _lastCheck = 0;

	protected String featureKey() 
	{
		return "call_history";
	}

	public String name(Context context) 
	{
		return "edu.northwestern.cbits.purple_robot_manager.probes.features.CallHistoryFeature";
	}

	public String title(Context context) 
	{
		return context.getString(R.string.title_call_history_feature);
	}

	public String summary(Context context)
	{
		return context.getString(R.string.summary_call_history_feature_desc);
	}

	public String probeCategory(Context context)
	{
		return context.getResources().getString(R.string.probe_personal_info_category);
	}

	public void enable(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_feature_call_history_enabled", true);
		
		e.commit();
	}		

	public boolean isEnabled(final Context context)
	{
		final SharedPreferences prefs = Probe.getPreferences(context);

		final long now = System.currentTimeMillis();

		if (super.isEnabled(context))
		{
			if (prefs.getBoolean("config_feature_call_history_enabled", true))
			{
				synchronized(this)
				{
					if (now - this._lastCheck > 15 * 60 * 1000)
					{
						this._lastCheck = now;

						try
						{
							boolean doHash = prefs.getBoolean("config_probe_call_hash_data", true);
	
							Bundle bundle = new Bundle();
							bundle.putString("PROBE", this.name(context));
							bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);
	
							ArrayList<Bundle> analyses = new ArrayList<Bundle>();
	
							double[] periods = { 0.25, 0.5, 1.0, 4.0, 12.0, 24.0, 168.0 };
								
							for (double period : periods)
							{
								Bundle analysis = new Bundle();
								
								HashMap<String, ArrayList<ContentValues>> contacts = new HashMap<String, ArrayList<ContentValues>>();
	
								long start = now - ((long) Math.floor(period * 1000 * 60 * 60));
	
								double total = 0;
								double incomingCount = 0;
								double acquaintanceCount = 0;
								double ackCount = 0;
								
								ArrayList<Double> durations = new ArrayList<Double>();
	
								String selection = "date > ?";
								String[] selectionArgs = { "" + start };
							
								Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, selection, selectionArgs, null);
	
								while (cursor.moveToNext())
								{
									total += 1;
									
									for (int i = 0; i < cursor.getColumnCount(); i++)
									{
										String name = cursor.getColumnName(i);
										
										Object value = cursor.getString(i);
										
										if (name.equals(CallLog.Calls.TYPE))
										{
											int type = cursor.getInt(i);
											
											if (type == CallLog.Calls.INCOMING_TYPE)
												incomingCount += 1;
										}
										else if (name.equals(CallLog.Calls.DURATION))
											durations.add((double) cursor.getInt(i));
										else if (name.equals(CallLog.Calls.CACHED_NAME))
										{
											if (value == null)
											{
												
											}
											else
												acquaintanceCount += 1;
										}
										else if (name.equals(CallLog.Calls.NEW))
										{
											if (cursor.getInt(i) != 0)
												ackCount += 1;
										}
									}
									
									String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
									number = PhoneNumberUtils.formatNumber(number);
	
									if (doHash)
										number = EncryptionManager.getInstance().createHash(context, number);
									
									ContentValues phoneCall = new ContentValues();
									
									DatabaseUtils.cursorRowToContentValues(cursor, phoneCall);
									
									ArrayList<ContentValues> calls = contacts.get(number);
									
									if (calls == null)
									{
										calls = new ArrayList<ContentValues>();
										contacts.put(number, calls);
									}
									
									calls.add(phoneCall);
								}
	
								cursor.close();
								
								if (total > 0)
								{
									analysis.putDouble(CallHistoryFeature.TOTAL, total);
		
									analysis.putDouble(CallHistoryFeature.INCOMING_COUNT, incomingCount);
									analysis.putDouble(CallHistoryFeature.OUTCOMING_COUNT, total - incomingCount);
									analysis.putDouble(CallHistoryFeature.INCOMING_RATIO, incomingCount / total);
		
									analysis.putDouble(CallHistoryFeature.ACQUAINTANCE_COUNT, acquaintanceCount);
									analysis.putDouble(CallHistoryFeature.STRANGER_COUNT, total - acquaintanceCount);
									analysis.putDouble(CallHistoryFeature.ACQUAINTANCE_RATIO, acquaintanceCount / total);
		
									analysis.putDouble(CallHistoryFeature.ACK_COUNT, ackCount);
									analysis.putDouble(CallHistoryFeature.NEW_COUNT, total - ackCount);
									analysis.putDouble(CallHistoryFeature.ACK_RATIO, ackCount / total);
	
									double maxDuration = Double.MIN_VALUE;
									double minDuration = Double.MAX_VALUE;
									
									double totalDuration = 0;
									
									double[] primitives = new double[durations.size()];
									
									for (int k = 0; k < durations.size(); k++)
									{
										Double duration = durations.get(k);
										
										primitives[k] = duration.doubleValue();
										
										totalDuration += duration;
										
										if (maxDuration < duration)
											maxDuration = duration;
										
										if (minDuration > duration)
											minDuration = duration;
									}
									
									StandardDeviation sd = new StandardDeviation();
									double stdDev = sd.evaluate(primitives);
									
									analysis.putDouble(CallHistoryFeature.TOTAL_DURATION, totalDuration);
									analysis.putDouble(CallHistoryFeature.AVG_DURATION, totalDuration / total);
									analysis.putDouble(CallHistoryFeature.MIN_DURATION, minDuration);
									analysis.putDouble(CallHistoryFeature.MAX_DURATION, maxDuration);
									analysis.putDouble(CallHistoryFeature.STD_DEVIATION, stdDev);
									
									ArrayList<Bundle> contactBundles = new ArrayList<Bundle>();
									
									for (String key : contacts.keySet())
									{
										Bundle contactInfo = new Bundle();
										
										ArrayList<ContentValues> calls = contacts.get(key);
	
										contactInfo.putString(CallHistoryFeature.IDENTIFIER, key);
										contactInfo.putDouble(CallHistoryFeature.TOTAL, calls.size());
										contactInfo.putBoolean(CallHistoryFeature.IS_ACQUAINTANCE, calls.get(0).containsKey(CallLog.Calls.CACHED_NAME));
										
										total = 0.0;
										totalDuration = 0;
										double incoming = 0.0;
										double acked = 0.0;
										durations = new ArrayList<Double>();
										
										for (ContentValues call : calls)
										{
											total += 1;
													
											if (call.getAsInteger(CallLog.Calls.TYPE) == CallLog.Calls.INCOMING_TYPE)
												incoming += 1;
											
											if (call.getAsInteger(CallLog.Calls.NEW) == 0)
												acked += 1;
											
											double duration = call.getAsDouble(CallLog.Calls.DURATION);
											
											durations.add(duration);
											totalDuration += duration;
										}
										
										contactInfo.putDouble(CallHistoryFeature.INCOMING_COUNT, incoming);
										contactInfo.putDouble(CallHistoryFeature.OUTCOMING_COUNT, total - incoming);
										contactInfo.putDouble(CallHistoryFeature.INCOMING_RATIO, incoming / total);
	
										contactInfo.putDouble(CallHistoryFeature.ACK_COUNT, acked);
										contactInfo.putDouble(CallHistoryFeature.NEW_COUNT, total - acked);
										contactInfo.putDouble(CallHistoryFeature.ACK_RATIO, acked / total);
	
										totalDuration = 0;
										maxDuration = Double.MIN_VALUE;
										minDuration = Double.MAX_VALUE;
										
										primitives = new double[durations.size()];
										
										for (int k = 0; k < durations.size(); k++)
										{
											Double duration = durations.get(k);
											
											primitives[k] = duration.doubleValue();
											
											totalDuration += duration;
											
											if (maxDuration < duration)
												maxDuration = duration;
											
											if (minDuration > duration)
												minDuration = duration;
										}
										
										sd = new StandardDeviation();
										stdDev = sd.evaluate(primitives);
										
										contactInfo.putDouble(CallHistoryFeature.TOTAL_DURATION, totalDuration);
										contactInfo.putDouble(CallHistoryFeature.AVG_DURATION, totalDuration / total);
										contactInfo.putDouble(CallHistoryFeature.MIN_DURATION, minDuration);
										contactInfo.putDouble(CallHistoryFeature.MAX_DURATION, maxDuration);
	
										contactInfo.putDouble(CallHistoryFeature.STD_DEVIATION, stdDev);
										
										contactBundles.add(contactInfo);
									}
									
									analysis.putParcelableArrayList(CallHistoryFeature.CONTACT_ANALYSES, contactBundles);
								}
								else
								{
									analysis.putDouble(CallHistoryFeature.TOTAL, 0);
									
									analysis.putDouble(CallHistoryFeature.INCOMING_COUNT, 0);
									analysis.putDouble(CallHistoryFeature.OUTCOMING_COUNT, 0);
									analysis.putDouble(CallHistoryFeature.INCOMING_RATIO, 0);
		
									analysis.putDouble(CallHistoryFeature.ACQUAINTANCE_COUNT, 0);
									analysis.putDouble(CallHistoryFeature.STRANGER_COUNT, 0);
									analysis.putDouble(CallHistoryFeature.ACQUAINTANCE_RATIO, 0);
		
									analysis.putDouble(CallHistoryFeature.ACK_COUNT, 0);
									analysis.putDouble(CallHistoryFeature.NEW_COUNT, 0);
									analysis.putDouble(CallHistoryFeature.ACK_RATIO, 0);
	
									analysis.putDouble(CallHistoryFeature.TOTAL_DURATION, 0);
									analysis.putDouble(CallHistoryFeature.AVG_DURATION, 0);
									analysis.putDouble(CallHistoryFeature.MIN_DURATION, 0);
									analysis.putDouble(CallHistoryFeature.MAX_DURATION, 0);
									analysis.putDouble(CallHistoryFeature.STD_DEVIATION, 0);
								}
								
								analysis.putLong(CallHistoryFeature.WINDOW_SIZE, now - start);
								
								analyses.add(analysis);
							}
							
							bundle.putParcelableArrayList(CallHistoryFeature.WINDOWS, analyses);
	
							this.transmitData(context, bundle);
						}
						catch (SecurityException e)
						{
							LogManager.getInstance(context).logException(e);
						}

						return true;
					}
				}
			}
		}
		
		return false;
	}

/*	private static ContentValues parseCursor(Cursor cursor) 
	{
		ContentValues values = new ContentValues();
		
		for (int i = 0; i < cursor.getColumnCount(); i++)
		{
			String name = cursor.getColumnName(i);
			
			switch(cursor.getType(i))
			{
				case Cursor.FIELD_TYPE_FLOAT:
					values.put(name, cursor.getFloat(i));
					break;
				case Cursor.FIELD_TYPE_INTEGER:
					values.put(name, cursor.getInt(i));
					break;
				case Cursor.FIELD_TYPE_STRING:
					values.put(name, cursor.getString(i));
					break;
			}
		}
		
		return values;
	} */

	public String summarizeValue(Context context, Bundle bundle)
	{
		ArrayList<Bundle> windows = bundle.getParcelableArrayList(CallHistoryFeature.WINDOWS);
		
		Bundle window = windows.get(windows.size() - 1);

		double avgDuration = window.getDouble(CallHistoryFeature.AVG_DURATION);
		double total = window.getDouble(CallHistoryFeature.TOTAL);
		double stdDev = window.getDouble(CallHistoryFeature.STD_DEVIATION);

		return String.format(context.getResources().getString(R.string.summary_call_history_probe), total, avgDuration, stdDev);
	}
	
	public void disable(Context context) 
	{
		SharedPreferences prefs = Probe.getPreferences(context);
		
		Editor e = prefs.edit();
		e.putBoolean("config_feature_call_history_enabled", false);
		
		e.commit();
	}
}
