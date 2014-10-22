package edu.northwestern.cbits.purple_robot_manager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public class WiFiHelper {
    private static boolean _wifiAvailable = false;
    private static long _lastWifiCheck = 0;

    public static boolean wifiAvailable(Context context) {
        long now = System.currentTimeMillis();

        if (now - WiFiHelper._lastWifiCheck > 10000) {
            WiFiHelper._lastWifiCheck = now;

            WifiManager wifi = (WifiManager) context
                    .getSystemService(Context.WIFI_SERVICE);

            if (wifi.isWifiEnabled()) {
                WiFiHelper._wifiAvailable = true;

                ConnectivityManager connection = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo netInfo = connection.getActiveNetworkInfo();

                if (netInfo != null) {
                    if (netInfo.getType() != ConnectivityManager.TYPE_WIFI)
                        WiFiHelper._wifiAvailable = false;
                    else if (netInfo.getState() != NetworkInfo.State.CONNECTED
                            && netInfo.getState() != NetworkInfo.State.CONNECTING)
                        WiFiHelper._wifiAvailable = false;
                } else
                    WiFiHelper._wifiAvailable = false;
            } else
                WiFiHelper._wifiAvailable = false;
        }

        return WiFiHelper._wifiAvailable;
    }
}
