package edu.northwestern.cbits.purple_robot_manager;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class LocalHttpServer 
{
	private ServerSocket _serverSocket = null;
	private Socket _clientSocket = null;
	private Thread _serverThread = null;

	private boolean _accept = false;

	private static int SOCKET = 12345;
	
	public void start(final Context context) 
	{
		Log.e("PRM", "STARTING HTTP SERVER...");
		
		if (this._serverThread == null)
		{
			final LocalHttpServer me = this;
			
			Runnable r = new Runnable()
			{
				public void run() 
				{
					me._accept = true;
							
					WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
					WifiInfo wifiInfo = wifiManager.getConnectionInfo();
				   
					Log.e("PRM", "WIFI: " + wifiInfo);
				   
					if (wifiInfo != null)
					{
						int ip = wifiInfo.getIpAddress();

						String ipString = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
					   
						Log.e("PRM", "WIFI IP: " + ipString);
					   
						try 
						{
							InetAddress[] addrs = InetAddress.getAllByName(ipString);
						   
							if (addrs.length > 0)
							{
								InetSocketAddress socketAddr = new InetSocketAddress(addrs[0], SOCKET);
							   
								me._serverSocket = new ServerSocket();
								me._serverSocket.bind(socketAddr);

								while (me._accept)
								{
									Log.e("PRM", "WAITING FOR CONNECTION...");
									
									me._clientSocket = me._serverSocket.accept();
								   
									Log.e("PRM", "GOT CONNECTION FROM " + me._clientSocket.getInetAddress());
								
									DataOutputStream clientOut = new DataOutputStream(me._clientSocket.getOutputStream());

									BufferedReader clientIn = new BufferedReader(new InputStreamReader(me._clientSocket.getInputStream()));
									String line = null;

									StringBuilder responseData = new StringBuilder();

									boolean bail = false;
									
									while((line = clientIn.readLine()) != null) 
									{
										line = line.trim();
										
										if (line.length() == 0)
										{
											if (bail)
												break;
											else
												bail = true;
										}
										else
										{
											Log.e("PRM", "GOT " + line);
										
											responseData.append(line + "\n");

											bail = false;
										}
									}
								   
									Log.e("PRM", "SERVER GOT " + responseData.toString());
								   
									clientOut.write(responseData.toString().getBytes("UTF-8"));

									clientIn.close();
									clientOut.close();
									
									me._clientSocket.close();
									me._clientSocket = null;
								}
							}
						}
						catch (IOException e) 
						{
							e.printStackTrace();
						}
					}

					try 
					{
						if (me._serverSocket != null && me._serverSocket.isClosed() == false)
							me._serverSocket.close();
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
					}
				   
					me._serverSocket = null;
					me._serverThread = null;
				}
			};
			
			this._serverThread = new Thread(r);
			this._serverThread.start();
		}
	}
	
	public void stop()
	{
		this._accept = false;
		
		if (this._clientSocket != null && this._clientSocket.isConnected())
		{
			try 
			{
				this._clientSocket.shutdownInput();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}

		if (this._serverThread != null && this._serverThread.isAlive())
		{
			if (this._serverSocket != null && this._serverSocket.isBound())
			{
				try 
				{
					this._serverSocket.close();
					
					this._serverSocket = null;
					this._serverThread = null;
				}
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
	}
}
