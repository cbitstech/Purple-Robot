package edu.northwestern.cbits.purple_robot_manager.tests;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.http.AndroidHttpClient;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.test.AndroidTestCase;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.TestActivity;
import edu.northwestern.cbits.purple_robot_manager.logging.LiberalSSLSocketFactory;
import edu.northwestern.cbits.purple_robot_manager.plugins.DataUploadPlugin;

public abstract class RobotTestCase extends AndroidTestCase {
    protected int _priority = Integer.MIN_VALUE;
    protected Context _context;

    public RobotTestCase(Context context, int priority) {
        super();

        this._context = context;
        this._priority = priority;

        this.setName("test");
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public abstract void test();

    public abstract String name(Context context);

    public String description(Context context) {
        int minutes = this.estimatedMinutes();

        if (minutes < 1)
            return context.getString(R.string.description_minute_or_less_test);

        return context.getString(R.string.description_minutes_test, minutes);
    }

    public int estimatedMinutes() {
        return 1;
    }

    protected void broadcastUpdate(String message, long delay) {
        LocalBroadcastManager bcast = LocalBroadcastManager
                .getInstance(this._context);

        Intent intent = new Intent(TestActivity.INTENT_PROGRESS_MESSAGE);
        intent.putExtra(TestActivity.PROGRESS_MESSAGE, message);
        intent.putExtra(TestActivity.PROGRESS_DELAY, delay);

        bcast.sendBroadcastSync(intent);
    }

    protected void broadcastUpdate(String message) {
        this.broadcastUpdate(message, 500);
    }

    public boolean isSelected(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        return prefs.getBoolean("test_" + this.name(context), false);
    }

    public void setSelected(Context context, boolean isSelected) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean("test_" + this.name(context), isSelected);
        e.commit();
    }

    public int compareTo(Context context, RobotTestCase other) {
        if (this._priority < other._priority)
            return -1;
        else if (this._priority > other._priority)
            return 1;
        else if (this.estimatedMinutes() < other.estimatedMinutes())
            return -1;
        else if (this.estimatedMinutes() > other.estimatedMinutes())
            return 1;

        return this.name(context).compareToIgnoreCase(other.name(context));
    }

    public String syncHttpPost(String url, Map<String, String> payload)
            throws KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException, KeyManagementException,
            UnrecoverableKeyException, URISyntaxException {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this._context);

        AndroidHttpClient androidClient = AndroidHttpClient.newInstance(
                "Purple Robot", this._context);

        HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory
                .getSocketFactory(), 80));

        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();

        if (prefs.getBoolean(DataUploadPlugin.ALLOW_ALL_SSL_CERTIFICATES,
                DataUploadPlugin.ALLOW_ALL_SSL_CERTIFICATES_DEFAULT)) {
            KeyStore trustStore = KeyStore.getInstance(KeyStore
                    .getDefaultType());
            trustStore.load(null, null);

            socketFactory = new LiberalSSLSocketFactory(trustStore);
        }

        registry.register(new Scheme("https", socketFactory, 443));

        HttpParams params = androidClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, 180000);
        HttpConnectionParams.setSoTimeout(params, 180000);

        SingleClientConnManager mgr = new SingleClientConnManager(params,
                registry);
        HttpClient httpClient = new DefaultHttpClient(mgr, params);

        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);

        String body = null;

        URI siteUri = new URI(url);

        HttpPost httpPost = new HttpPost(siteUri);
        httpPost.setHeader("Content-Type",
                "application/x-www-form-urlencoded;charset=UTF-8");

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

        for (String key : payload.keySet())
            nameValuePairs.add(new BasicNameValuePair(key, payload.get(key)));

        HttpEntity entity = new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8);

        httpPost.setEntity(entity);

        HttpResponse response = httpClient.execute(httpPost);

        HttpEntity httpEntity = response.getEntity();

        String contentHeader = null;

        if (response.containsHeader("Content-Encoding"))
            contentHeader = response.getFirstHeader("Content-Encoding")
                    .getValue();

        if (contentHeader != null && contentHeader.endsWith("gzip")) {
            BufferedInputStream in = new BufferedInputStream(
                    AndroidHttpClient.getUngzippedContent(httpEntity));

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            int read = 0;
            byte[] buffer = new byte[1024];

            while ((read = in.read(buffer, 0, buffer.length)) != -1)
                out.write(buffer, 0, read);

            in.close();

            body = out.toString("UTF-8");
        } else
            body = EntityUtils.toString(httpEntity);

        androidClient.close();

        return body;
    }
}
