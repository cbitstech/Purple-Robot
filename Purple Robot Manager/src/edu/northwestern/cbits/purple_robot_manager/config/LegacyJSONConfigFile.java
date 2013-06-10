package edu.northwestern.cbits.purple_robot_manager.config;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.RhinoException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.scripting.SchemeEngine;
import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class LegacyJSONConfigFile
{
	public static final String FIRST_RUN = "json_config_first_run";
	
	public static final String USER_ID = "user_id";
	public static final String USER_HASH = "user_hash";
	public static final String JSON_CONFIGURATION = "json_configuration_contents";
	public static final String JSON_PROBE_SETTINGS = "probe_settings";
	public static final String JSON_CONFIGURATION_URL = "config_json_url";
	public static final String JSON_LAST_UPDATE = "json_configuration_last_update";
	public static final String JSON_LAST_HASH = "json_configuration_last_update_hash";
	public static final String FEATURES = "features";
	private static final String JSON_INIT_SCRIPT = "init_script";

	private static SharedPreferences prefs = null;

	private JSONObject parameters = null;

	private static LegacyJSONConfigFile _sharedFile = null;

	public static void updateFromOnline(final Context context, final Uri uri)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				Runnable next = null;
				
				if (uri != null && uri.toString().trim().length() > 0)
				{
					final EncryptionManager encryption = EncryptionManager.getInstance();

					Uri newUri = uri;
					
					try
					{
						String userId = encryption.getUserId(context);
						String existingId = uri.getQueryParameter("user_id");
						
						if (existingId == null || existingId.equals(userId) == false)
						{
							Builder builder = new Builder();
							
							builder.scheme(uri.getScheme());
							builder.encodedAuthority(uri.getAuthority());

							if (uri.getPath() != null)
								builder.encodedPath(uri.getPath());
							
							if (uri.getFragment() != null)
								builder.encodedFragment(uri.getFragment());
							
							String query = uri.getQuery();
							
							ArrayList<String> keys = new ArrayList<String>();

							if (query != null)
							{
								
								String[] params = query.split("&");
								
								for (String param : params)
								{
									String[] components = param.split("=");
									
									keys.add(components[0]);
								}
							}
							
							for (String key : keys)
							{
								if ("user_id".equals(key))
								{
									// Don't keep existing User ID. Use value set in prefs...
								}
								else
									builder.appendQueryParameter(key, uri.getQueryParameter(key));
							}

							builder.appendQueryParameter("user_id", userId);

							newUri = builder.build();
						}
						
						Log.e("PR", "USING CONFIG URI " + newUri);
						
						URL u = new URL(newUri.toString());

						final SharedPreferences prefs = LegacyJSONConfigFile.getPreferences(context);
						Editor edit = prefs.edit();

						edit.putString(LegacyJSONConfigFile.JSON_CONFIGURATION_URL, u.toString());
						edit.commit();

						HttpURLConnection conn = (HttpURLConnection) u.openConnection();
						
						for (int z = 0; z < 16 && conn.getHeaderField("Location") != null; z++)
						{
							URL newUrl = new URL(conn.getHeaderField("Location"));
							
							conn.disconnect();
							
							conn = (HttpURLConnection) newUrl.openConnection();
						}
						
						BufferedInputStream bin = new BufferedInputStream(conn.getInputStream());
						ByteArrayOutputStream bout = new ByteArrayOutputStream();

						byte[] buffer = new byte[4096];
						int read = 0;

						while ((read = bin.read(buffer, 0, buffer.length)) != -1)
						{
							bout.write(buffer, 0, read);
						}

						bin.close();

						String scriptString = new String(bout.toByteArray(), "UTF-8");
						
						String oldHash = prefs.getString(LegacyJSONConfigFile.JSON_LAST_HASH, "");
						final String newHash = encryption.createHash(context, scriptString);

						if (conn.getContentType().toLowerCase().startsWith("text/x-scheme") || scriptString.toLowerCase().startsWith("(begin "))
						{
							// TODO: Temp code until we get a more flexible parsing system in place...

							if ("".equals(scriptString.trim()))
								scriptString = "(begin)";
							
							if (oldHash.equals(newHash) == false)
							{
								try
								{
									SchemeEngine scheme = new SchemeEngine(context, null);
									scheme.evaluateSource(scriptString);
								}
								catch (final Exception e)
								{
									LogManager.getInstance(context).logException(e);
									
									if (context instanceof Activity)
									{
										final Activity activity = (Activity) context;

										activity.runOnUiThread(new Runnable()
										{
											public void run()
											{
												Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
											}
										});
									}
								}

								edit.putString(LegacyJSONConfigFile.JSON_LAST_HASH, newHash);
								edit.commit();
								
								if (context instanceof Activity)
								{
									final Activity activity = (Activity) context;
		
									activity.runOnUiThread(new Runnable()
									{
										public void run()
										{
											Toast.makeText(activity, R.string.success_json_set_uri, Toast.LENGTH_LONG).show();
										}
									});
								}
							}
						}
						else
						{
							if ("".equals(scriptString.trim()))
								scriptString = "{}";
	
							final JSONObject json = new JSONObject(scriptString);
							
							if (oldHash.equals(newHash) == false)
							{
								edit.putString(LegacyJSONConfigFile.JSON_CONFIGURATION, scriptString);
								edit.commit();
	
//								TriggerManager.getInstance(context).removeAllTriggers();
								
								next = new Runnable()
								{
									public void run() 
									{
										try 
										{
											if (json.has(LegacyJSONConfigFile.JSON_INIT_SCRIPT))
											{
												String script = json.getString(LegacyJSONConfigFile.JSON_INIT_SCRIPT);
												
												JavaScriptEngine engine = new JavaScriptEngine(context);
												
												engine.runScript(script);
											}
											
											Editor edit = prefs.edit();
											edit.putString(LegacyJSONConfigFile.JSON_LAST_HASH, newHash);
											edit.commit();
										}
										catch (JSONException e) 
										{
											LogManager.getInstance(context).logException(e);
										}
										catch (final EcmaError e)
										{
											LogManager.getInstance(context).logException(e);
											
											if (context instanceof Activity)
											{
												final Activity activity = (Activity) context;
	
												activity.runOnUiThread(new Runnable()
												{
													public void run()
													{
														Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
													}
												});
											}
										}
										catch (final EvaluatorException e)
										{
											LogManager.getInstance(context).logException(e);
											
											if (context instanceof Activity)
											{
												final Activity activity = (Activity) context;
	
												activity.runOnUiThread(new Runnable()
												{
													public void run()
													{
														Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
													}
												});
											}
										}
									}
								};
							}
									
							if (context instanceof Activity)
							{
								final Activity activity = (Activity) context;
	
								activity.runOnUiThread(new Runnable()
								{
									public void run()
									{
										Toast.makeText(activity, R.string.success_json_set_uri, Toast.LENGTH_LONG).show();
									}
								});
							}
						}
					}
					catch (MalformedURLException e)
					{
						LogManager.getInstance(context).logException(e);
					}
					catch (IOException e)
					{
						LogManager.getInstance(context).logException(e);
					}
					catch (JSONException e)
					{
						LogManager.getInstance(context).logException(e);
					}

					LegacyJSONConfigFile._sharedFile = new LegacyJSONConfigFile(context, next);
				}
				else
				{
					final SharedPreferences prefs = LegacyJSONConfigFile.getPreferences(context);
					
					if (prefs.getString(LegacyJSONConfigFile.JSON_CONFIGURATION, "{}").length() > 4)
					{
						Editor edit = prefs.edit();

						edit.putString(LegacyJSONConfigFile.JSON_CONFIGURATION, "{}");
						edit.commit();
					}					
				}
			}
		};

		Thread t = new Thread(r);
		t.start();
	}

	protected void updateTriggers(Context context) 
	{
		List<Trigger> triggerList = new ArrayList<Trigger>();

		try
		{
			JSONArray triggers = this.parameters.getJSONArray("triggers");

			for (int i = 0; triggers != null && i < triggers.length(); i++)
			{
				JSONObject json = triggers.getJSONObject(i);
				
				Map<String, Object> map = new HashMap<String, Object>();
						
				@SuppressWarnings("unchecked")
				Iterator<String> keys = json.keys();
				
				while (keys.hasNext())
				{
					String key = keys.next();
					
					map.put(key, json.get(key));
				}

				Trigger t = Trigger.parse(context, map);

				if (t != null)
					triggerList.add(t);
			}
		}
		catch (JSONException e)
		{

		}
		
		TriggerManager.getInstance(context).updateTriggers(context, triggerList);
	}

	public static LegacyJSONConfigFile getSharedFile(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		if (prefs.getBoolean(LegacyJSONConfigFile.FIRST_RUN, true))
		{
			Editor e = prefs.edit();

			e.putString(LegacyJSONConfigFile.JSON_CONFIGURATION_URL, context.getString(R.string.json_config_url));
			e.putBoolean(LegacyJSONConfigFile.FIRST_RUN, false);

			e.commit();
		}

		ProbeManager.allProbes(context);
		
		if (LegacyJSONConfigFile._sharedFile == null)
		{
			LegacyJSONConfigFile._sharedFile = new LegacyJSONConfigFile(context, null);
			LegacyJSONConfigFile.update(context);
		}

		return LegacyJSONConfigFile._sharedFile;
	}

	public String getStringParameter(String key)
	{
		try
		{
			if (this.parameters != null)
				return this.parameters.getString(key);
		}
		catch (JSONException e)
		{

		}

		return null;
	}

	private static SharedPreferences getPreferences(Context context)
	{
		if (LegacyJSONConfigFile.prefs == null)
			LegacyJSONConfigFile.prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

		return LegacyJSONConfigFile.prefs;
	}

	private LegacyJSONConfigFile(Context context, Runnable next)
	{
		SharedPreferences prefs = LegacyJSONConfigFile.getPreferences(context);

		try
		{
			this.parameters = new JSONObject(prefs.getString(LegacyJSONConfigFile.JSON_CONFIGURATION, "{}"));

			this.updateSharedPreferences(context);
			this.updateTriggers(context);

			String script = this.parameters.getString(LegacyJSONConfigFile.JSON_INIT_SCRIPT);
			JavaScriptEngine engine = new JavaScriptEngine(context);
			engine.runScript(script);
			
			if (next != null)
				next.run();
		}
		catch (JSONException e)
		{
			this.parameters = new JSONObject();
		}
		catch (RhinoException e)
		{
			LogManager.getInstance(context).logException(e);

			this.parameters = new JSONObject();
		}
	}

	private void updateSharedPreferences(Context context)
	{
		String userId = null;
		String userHash = null;

		try
		{
			if (this.parameters.has(LegacyJSONConfigFile.USER_ID))
				userId = this.parameters.getString(LegacyJSONConfigFile.USER_ID);

			if (this.parameters.has(LegacyJSONConfigFile.USER_HASH))
				userHash = this.parameters.getString(LegacyJSONConfigFile.USER_HASH);
		}
		catch (JSONException e)
		{
			LogManager.getInstance(context).logException(e);
		}

		SharedPreferences prefs = LegacyJSONConfigFile.getPreferences(context);
		Editor editor = prefs.edit();

		if (userId == null)
			userId = prefs.getString("config_user_id", null);

		if (userId == null)
		{
			AccountManager manager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
			Account[] list = manager.getAccountsByType("com.google");

			if (list.length == 0)
				list = manager.getAccounts();

			if (list.length > 0)
			{
				userId = list[0].name;
			}
		}

		if (userId != null && userHash == null)
		{
			try
			{
				MessageDigest md = MessageDigest.getInstance("MD5");
				byte[] digest = md.digest(userId.getBytes("UTF-8"));

				userHash = (new BigInteger(1, digest)).toString(16);

				while(userHash.length() < 32 )
				{
					userHash = "0" + userHash;
				}
			}
			catch (NoSuchAlgorithmException e)
			{
				LogManager.getInstance(context).logException(e);
			}
			catch (UnsupportedEncodingException e)
			{
				LogManager.getInstance(context).logException(e);
			}
		}

		if (userId != null)
			editor.putString("config_user_id", userId);

		if (userHash != null)
			editor.putString("config_user_hash", userHash);

		if (this.parameters.has(LegacyJSONConfigFile.JSON_PROBE_SETTINGS))
		{
			try
			{
				JSONArray probeSettings = this.parameters.getJSONArray(LegacyJSONConfigFile.JSON_PROBE_SETTINGS);

				ProbeManager.updateProbesFromJSON(context, probeSettings);
			}
			catch (JSONException e)
			{
				LogManager.getInstance(context).logException(e);
			}
		}

		if (this.parameters.has(LegacyJSONConfigFile.FEATURES))
		{
			ProbeManager.clearFeatures();

			try
			{
				JSONArray features = this.parameters.getJSONArray(LegacyJSONConfigFile.FEATURES);

				for (int i = 0; i < features.length(); i++)
				{
					JSONObject feature = features.getJSONObject(i);

					String name = context.getString(R.string.label_unknown_feature);
					String script = "";
					String formatter = "";

					if (feature.has("name"))
						name = feature.getString("name");

					if (feature.has("feature"))
						script = feature.getString("feature");

					if (feature.has("formatter"))
						formatter = feature.getString("formatter");

					ArrayList<String> sources = new ArrayList<String>();

					if (feature.has("sources"))
					{
						JSONArray sourceArray = feature.getJSONArray("sources");

						for (int j = 0; j < sourceArray.length(); j++)
						{
							String source = sourceArray.getString(j);

							sources.add(source);
						}
					}

					ProbeManager.addFeature(name, name, script, formatter, sources, false);
				}
			}
			catch (JSONException e)
			{
				LogManager.getInstance(context).logException(e);
			}
		}

		editor.commit();
	}

	public static void update(final Context context)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				SharedPreferences prefs = LegacyJSONConfigFile.getPreferences(context);

				long lastUpdate = prefs.getLong(LegacyJSONConfigFile.JSON_LAST_UPDATE, 0);

				long now = System.currentTimeMillis();

				int interval = Integer.parseInt(prefs.getString("config_json_refresh_interval", "3600"));
				
				if (now - lastUpdate > 1000 * interval)
				{
					Editor edit = prefs.edit();

					String uriString = prefs.getString(LegacyJSONConfigFile.JSON_CONFIGURATION_URL, null);

					if (uriString != null)
						LegacyJSONConfigFile.updateFromOnline(context, Uri.parse(uriString));

					edit.putLong(LegacyJSONConfigFile.JSON_LAST_UPDATE, now);
					edit.commit();
					
					if (LegacyJSONConfigFile._sharedFile != null)
						LegacyJSONConfigFile._sharedFile.updateTriggers(context);
				}
			}
		};
		
		try
		{
			Thread t = new Thread(r);
			t.start();
		}
		catch (OutOfMemoryError e)
		{
			LogManager.getInstance(context).logException(e);
		}
	}

	protected String content()
	{
		return this.parameters.toString();
	}

	protected boolean isValid()
	{
		if (this.content().length() > Preferences.MAX_VALUE_LENGTH)
			return false;

		return true;
	}
}
