package edu.northwestern.cbits.purple_robot_manager.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
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
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.util.EntityUtils;

import android.content.Context;

public class LocalHttpServer 
{
	private static int SERVER_PORT = 12345;

	public void start(final Context context) 
	{
		try 
		{
			Thread t = new RequestListenerThread(context.getApplicationContext(), SERVER_PORT);
	        t.setDaemon(false);
	        t.start();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

    static class WebRequestHandler implements HttpRequestHandler  
    {
    	private static String WEB_PREFIX = "embedded_website";
    	
    	private Context _context = null;
    	
    	public WebRequestHandler(Context context)
    	{
    		super();
    		
    		this._context = context;
    	}
    	
        public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException
        {
            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);

            if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST"))
                throw new MethodNotSupportedException(method + " method not supported"); 

            String target = request.getRequestLine().getUri();
            
            if (target.trim().length() == 1)
            	target = "/index.html";
            
            if (request instanceof HttpEntityEnclosingRequest) 
            {
				try 
				{
	                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
	                byte[] entityContent = EntityUtils.toByteArray(entity);

	                System.out.println("Incoming entity content (bytes): " + entityContent.length);
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
            }

            final String path = WEB_PREFIX + target;

            try
            {
            	final InputStream in = this._context.getAssets().open(path);

                response.setStatusCode(HttpStatus.SC_OK);

                EntityTemplate body = new EntityTemplate(new ContentProducer() 
                {
					public void writeTo(OutputStream out) throws IOException 
					{
						byte[] b = new byte[1024];
						int read = 0;
						
						while ((read = in.read(b, 0, b.length)) != -1)
							out.write(b, 0, read);

						out.close();
						in.close();
					}
                });

                response.setEntity(body);
            }
            catch (IOException e)
            {
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);

				try 
				{
	                final InputStream in = this._context.getAssets().open("embedded_website/404.html");

	                EntityTemplate body = new EntityTemplate(new ContentProducer() 
	                {
						public void writeTo(OutputStream out) throws IOException 
						{
							byte[] b = new byte[1024];
							int read = 0;
							
							while ((read = in.read(b, 0, b.length)) != -1)
								out.write(b, 0, read);

							out.close();
							in.close();
						}
	                });

	                response.setEntity(body);

	                body.setContentType("text/html; charset=UTF-8");
	                response.setEntity(body);
				}
				catch (IOException e1) 
				{
					e1.printStackTrace();
				}
            }
        }
    }

    static class RequestListenerThread extends Thread 
    {
        private final ServerSocket serversocket;
        private final HttpParams params; 
        private final HttpService httpService;
        
        public RequestListenerThread(Context context, int port) throws IOException 
        {
            this.serversocket = new ServerSocket(port);
            this.params = new BasicHttpParams();
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
            reqistry.register("*", new WebRequestHandler(context));
            
            this.httpService = new HttpService(httpproc, new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory());
            this.httpService.setParams(this.params);
            this.httpService.setHandlerResolver(reqistry);
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
