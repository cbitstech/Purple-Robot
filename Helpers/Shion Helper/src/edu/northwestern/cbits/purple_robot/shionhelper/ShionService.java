package edu.northwestern.cbits.purple_robot.shionhelper;

import java.io.IOException;
import java.util.ArrayList;

import net.hockeyapp.android.CrashManager;

import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class ShionService extends IntentService implements IQProvider
{
	public static final String CONFIG_SERVER = "config_server";
	public static final String CONFIG_USERNAME = "config_username";
	public static final String CONFIG_PASSWORD = "config_password";
	public static final String CONFIG_SITE = "config_site";

	public static final String SERVER = "server";
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	public static final String SITE = "site";

	private static final String TYPE = "type";
	private static final String LOCATION = "location";
	private static final String ADDRESS = "address";
	private static final String MODEL = "model";
	private static final String LEVEL = "level";
	private static final String PLATFORM = "platform";
	private static final String NAME = "name";
	
	public static final String FETCH_INTENT = "fetch_shion_devices";
	private static final String DEVICES = "devices";
	
	private static final String APP_ID = "961620153026f515359a75150eca0212";
	
	private XMPPConnection _connection = null;

	public ShionService() 
	{
		super("Shion");
	}

	public ShionService(String name) 
	{
		super(name);
		
		CrashManager.register(this, APP_ID, new net.hockeyapp.android.CrashManagerListener() 
		{
			public boolean shouldAutoUploadCrashes() 
			{
			    return true;
			}
		});
	}

	protected void onHandleIntent(Intent intent) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		final String server = prefs.getString(ShionService.CONFIG_SERVER, null);
		final String username = prefs.getString(ShionService.CONFIG_USERNAME, null);
		final String password = prefs.getString(ShionService.CONFIG_PASSWORD, null);
		final String site = prefs.getString(ShionService.CONFIG_SITE, null);
		
		if (server == null || site == null || username == null || password == null)
		{
			Log.e("Shion Helper", "Incomplete configuration. Exiting...");

			return;
		}
		
        final ShionService me = this;
        
        Runnable r = new Runnable()
        {
			public void run() 
			{
				SmackAndroid.init(me); 

				ProviderManager providerManager = ProviderManager.getInstance();
				providerManager.addIQProvider("site-information", "shion:site-information", me);
				
				XMPPConnection.DEBUG_ENABLED = true;
						
				me._connection = new XMPPConnection(server);
				
				try 
				{
					me._connection.connect();
					me._connection.login(username, password);

					IQ command = new IQ()
					{
						public String getChildElementXML() 
						{
							return "<query xmlns=\"shion:script\" language=\"lua\">shion:sendEnvironmentToJid(\"" + me._connection.getUser() + "\")</query>";
						}
					};
					
					String to = me._connection.getUser().replace("Smack", site);
					
					command.setFrom(me._connection.getUser());
					command.setTo(to);
					
					me._connection.sendPacket(command);
				} 
				catch (XMPPException e) 
				{
					e.printStackTrace();
				}
			}
        };
        
        Thread t = new Thread(r);
        t.start();
        
        Thread u = new Thread(new Runnable()
        {
			public void run() 
			{
				try 
				{
					Thread.sleep(5000);
				} 
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
				
				me._connection.disconnect();
				me._connection = null;
			}
        });

        u.start();
	}

	public IQ parseIQ(XmlPullParser xpp) throws Exception
	{
        int eventType = xpp.next();
        
        ArrayList<Bundle> devices = new ArrayList<Bundle>();

        do
        {
            if (eventType == XmlPullParser.START_TAG)
            {
            	Bundle device = processElement(xpp);
            	
            	if (device != null)
            		devices.add(device);
            }

            eventType = xpp.next();
        }
        while (eventType != XmlPullParser.END_DOCUMENT);

        Bundle[] deviceExtras = new Bundle[devices.size()];
        
        for (int i = 0; i < deviceExtras.length; i++)
        {
        	deviceExtras[i] = devices.get(i);
        }
        
        Intent intent = new Intent(ShionService.FETCH_INTENT);
        intent.putExtra(ShionService.DEVICES, deviceExtras);
        
        this.sendBroadcast(intent);
        
		return new IQ()
		{
			public String getChildElementXML() 
			{
				return "<ok />";
			}
		};
	}
	
    public Bundle processElement(XmlPullParser xpp) throws XmlPullParserException, IOException
    {
        String uri = xpp.getNamespace();

        if (uri.equals("shion:site-information"))
        {
            String name = xpp.getName();
            
            if ("d".equalsIgnoreCase(name))
            {
            	String type = xpp.getAttributeValue(null, "t");
            	String platform = xpp.getAttributeValue(null, "p");
            	String location = xpp.getAttributeValue(null, "l");
            	String deviceName = xpp.getAttributeValue(null, "n");
            	String address = xpp.getAttributeValue(null, "a");
            	String level = xpp.getAttributeValue(null, "lv");
            	String model = xpp.getAttributeValue(null, "m");
            	
            	if (type != null && location != null)
            	{
            		Bundle device = new Bundle();
            		
            		if (type != null)
            			device.putString(ShionService.TYPE, type);

            		if (platform != null)
            			device.putString(ShionService.PLATFORM, platform);

            		if (location != null)
            			device.putString(ShionService.LOCATION, location);

            		if (deviceName != null)
            			device.putString(ShionService.NAME, deviceName);

            		if (address != null)
            			device.putString(ShionService.ADDRESS, address);

            		if (level != null)
            			device.putString(ShionService.LEVEL, level);

            		if (model != null)
            			device.putString(ShionService.MODEL, model);
            		
            		return device;
            	}
            }
        }
        
        return null;
    }
}
