package edu.northwestern.cbits.purple_robot_manager.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.preference.PreferenceManager;

public class LocalHttpServer
{
    public static final String BUILTIN_HTTP_SERVER_ENABLED = "config_enable_builtin_http_server";
    public static final boolean BUILTIN_HTTP_SERVER_ENABLED_DEFAULT = true;

    public static final String BUILTIN_HTTP_SERVER_PASSWORD = "config_builtin_http_server_password";
    public static final String BUILTIN_HTTP_SERVER_PASSWORD_DEFAULT = "";

    public static final String BUILTIN_ZEROCONF_ENABLED = "config_builtin_http_server_zeroconf";
    public static final boolean BUILTIN_ZEROCONF_ENABLED_DEFAULT = false;
    public static final String BUILTIN_ZEROCONF_NAME = "config_builtin_http_server_zeroconf_name";

    private static int SERVER_PORT = 12345;

    private Thread _serverThread = null;

    private static NsdManager.RegistrationListener _listener = null;

    public void start(final Context context)
    {
        if (this._serverThread == null || this._serverThread.isInterrupted())
        {
            BasicAuthHelper.getInstance(context);

            this._serverThread = new RequestListenerThread(context.getApplicationContext(), SERVER_PORT);
            this._serverThread.setDaemon(false);
            this._serverThread.start();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && LocalHttpServer._listener == null) {
            LocalHttpServer._listener = new NsdManager.RegistrationListener() {
                @Override
                public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {
                }

                @Override
                public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {
                }

                @Override
                public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                }

                @Override
                public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
                }
            };
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String name = prefs.getString(LocalHttpServer.BUILTIN_ZEROCONF_NAME, null);

        if (name == null)
        {
            name = EncryptionManager.getInstance().getUserId(context);

            SharedPreferences.Editor e = prefs.edit();
            e.putString(LocalHttpServer.BUILTIN_ZEROCONF_NAME, name);
            e.commit();
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {

                if (prefs.getBoolean(LocalHttpServer.BUILTIN_ZEROCONF_ENABLED, LocalHttpServer.BUILTIN_ZEROCONF_ENABLED_DEFAULT)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        NsdServiceInfo serviceInfo = new NsdServiceInfo();

                        String name = prefs.getString(LocalHttpServer.BUILTIN_ZEROCONF_NAME, null);

                        serviceInfo.setServiceName("Purple Robot (" + name + ")");
                        serviceInfo.setServiceType("_http._tcp");
                        serviceInfo.setPort(12345);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            serviceInfo.setAttribute("path", "/docs/scripting/all");

                        NsdManager manager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

                        manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, LocalHttpServer._listener);
                    }
                }
            }
        };

        Thread t = new Thread(r);
        t.start();

        LogManager.getInstance(context).log("started_builtin_http_server", null);
    }

    public void stop(final Context context)
    {
        if (this._serverThread != null)
        {
            this._serverThread.interrupt();
            this._serverThread = null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            NsdManager manager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

            try {
                manager.unregisterService(LocalHttpServer._listener);
            }
            catch (IllegalArgumentException e)
            {
                // Listener not registered...
            }

            LocalHttpServer._listener = null;
        }


        LogManager.getInstance(context).log("stopped_builtin_http_server", null);
    }

    static class RequestListenerThread extends Thread
    {
        private ServerSocket serversocket = null;
        private final HttpParams params = new BasicHttpParams();
        private HttpService httpService;
        private int _port = 0;

        public RequestListenerThread(final Context context, final int port)
        {
            this._port = port;

            this.params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 1000);
            this.params.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024);
            this.params.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);
            this.params.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true);
            this.params.setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

            BasicHttpProcessor httpproc = new BasicHttpProcessor();
            httpproc.addInterceptor(new ResponseDate());
            httpproc.addInterceptor(new ResponseServer());
            httpproc.addInterceptor(new ResponseContent());
            httpproc.addInterceptor(new ResponseConnControl());

            HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
            reqistry.register("/json/submit", new JsonScriptRequestHandler(context));
            reqistry.register("/json/store", new JsonStoreRequestHandler(context));
            reqistry.register("/json/variables.json", new JsonVariablesRequestHandler(context));
            reqistry.register("/store", new HttpStoreRequestHandler(context));
            reqistry.register("/snapshots.json", new SnapshotJsonRequestHandler(context));
            reqistry.register("/snapshot.html", new SnapshotRequestHandler(context));
            reqistry.register("/snapshot/audio.html", new SnapshotAudioRequestHandler(context));
            reqistry.register("/log", new LogServerEmulatorRequestHandler(context));
            reqistry.register("/docs/scripting/*", new ScriptHelpRequestHandler(context));
            reqistry.register("/docs/probes/*", new ProbesHelpRequestHandler(context));
            reqistry.register("*", new StaticContentRequestHandler(context));

            this.httpService = new HttpService(httpproc, new DefaultConnectionReuseStrategy(),
                    new DefaultHttpResponseFactory());
            this.httpService.setParams(this.params);
            this.httpService.setHandlerResolver(reqistry);
        }

        public void interrupt()
        {
            if (this.serversocket != null)
            {
                try
                {
                    this.serversocket.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            super.interrupt();
        }

        public void run()
        {
            try
            {
                this.serversocket = new ServerSocket();
                this.serversocket.setReuseAddress(true);
                this.serversocket.bind(new InetSocketAddress(this._port));

                while (!Thread.interrupted())
                {
                    try
                    {
                        Socket socket = this.serversocket.accept();
                        DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                        conn.bind(socket, this.params);

                        Thread t = new WorkerThread(this.httpService, conn);
                        t.setDaemon(true);
                        t.start();
                    }
                    catch (InterruptedIOException ex)
                    {
                        break;
                    }
                    catch (IOException e)
                    {
                        break;
                    }
                    catch (NullPointerException e)
                    {
                        break;
                    }
                }

                this.serversocket.close();
            }
            catch (IOException e)
            {
                try
                {
                    LogManager.getInstance(null).logException(e);
                }
                catch (Throwable th)
                {
                    th.printStackTrace();
                }
            }
        }
    }

    static class WorkerThread extends Thread
    {
        private final HttpService httpservice;
        private final HttpServerConnection conn;

        public WorkerThread(HttpService httpservice, HttpServerConnection conn)
        {
            super();
            this.httpservice = httpservice;
            this.conn = conn;
        }

        public void run()
        {
            HttpContext context = new BasicHttpContext(null);

            try
            {
                while (!Thread.interrupted() && this.conn.isOpen())
                {
                    try
                    {
                        this.httpservice.handleRequest(this.conn, context);
                    }
                    catch (SocketTimeoutException e)
                    {

                    }
                }
            }
            catch (ConnectionClosedException e)
            {
                // e.printStackTrace();
            }
            catch (IOException | HttpException e)
            {
                e.printStackTrace();
            } finally
            {
                try
                {
                    this.conn.shutdown();
                }
                catch (IOException ignore)
                {
                    ignore.printStackTrace();
                }
            }
        }
    }
}
