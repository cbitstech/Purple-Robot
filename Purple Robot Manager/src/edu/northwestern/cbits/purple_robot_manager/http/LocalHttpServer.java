package edu.northwestern.cbits.purple_robot_manager.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

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
        
        public RequestListenerThread(final Context context, final int port) 
        {
        	final RequestListenerThread me = this;
        	
        	Runnable r = new Runnable()
        	{
				public void run() 
				{
					try
					{
						me.serversocket = new ServerSocket(port, 1, InetAddress.getLocalHost());
			            
			            me.params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
			            me.params.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024);
			            me.params.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);
			            me.params.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true);
			            me.params.setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");
	
			            BasicHttpProcessor httpproc = new BasicHttpProcessor();
			            httpproc.addInterceptor(new ResponseDate());
			            httpproc.addInterceptor(new ResponseServer());
			            httpproc.addInterceptor(new ResponseContent());
			            httpproc.addInterceptor(new ResponseConnControl());
			            
			            HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
			            reqistry.register("/json/submit", new JsonScriptRequestHandler(context));
			            reqistry.register("*", new StaticContentRequestHandler(context));
			            
			            me.httpService = new HttpService(httpproc, new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory());
			            me.httpService.setParams(me.params);
			            me.httpService.setHandlerResolver(reqistry);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
        	};
        	
        	Thread t = new Thread(r);
        	t.start();
        }
        
        public void run() 
        {
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
                    this.httpservice.handleRequest(this.conn, context);
                }
            } 
            catch (ConnectionClosedException e) 
            {
            	e.printStackTrace();
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
                	
                }
            }
        }
    }
}
