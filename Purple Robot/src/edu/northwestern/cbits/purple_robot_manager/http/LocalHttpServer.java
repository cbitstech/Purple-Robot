package edu.northwestern.cbits.purple_robot_manager.http;

import java.io.IOException;
import java.io.InterruptedIOException;
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

import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;

import android.content.Context;

public class LocalHttpServer 
{
	private static int SERVER_PORT = 12345;

	public void start(final Context context) 
	{
		Thread t = new RequestListenerThread(context.getApplicationContext(), SERVER_PORT);
        t.setDaemon(false);
        t.start();
	}

    static class RequestListenerThread extends Thread 
    {
        private ServerSocket serversocket;
        private final HttpParams params = new BasicHttpParams(); 
        private HttpService httpService;
        private int _port = 0;
        
        public RequestListenerThread(final Context context, final int port) 
        {
        	this._port = port;
        	
        	this.params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
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
            reqistry.register("*", new StaticContentRequestHandler(context));
            
            this.httpService = new HttpService(httpproc, new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory());
            this.httpService.setParams(this.params);
            this.httpService.setHandlerResolver(reqistry);
        }
        
        public void run() 
        {
			try 
			{
//				this.serversocket = new ServerSocket(this._port, 8, InetAddress.getLocalHost());
				this.serversocket = new ServerSocket(this._port, 1);
				
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
            catch (IOException e) 
            {
            	e.printStackTrace();
            }
            catch (HttpException e) 
            {
            	e.printStackTrace();
            }
            finally 
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
