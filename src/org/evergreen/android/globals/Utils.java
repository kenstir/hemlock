package org.evergreen.android.globals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.test.IsolatedContext;
import android.util.Log;

public class Utils {

	private static String TAG = "Utils";
	/**
	 * Gets the net page content.
	 *
	 * @param url the url of the page to be retrieved
	 * @return the net page content
	 */
	public static String getNetPageContent(String url){
		
		String result = "";
		
		HttpResponse response = null;
		
		try{
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			response = client.execute(request);
		}catch(Exception e){
			System.out.println("Exception to GET page " + url);	
		}
		StringBuilder str = null;
		
		try{
			InputStream in = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			str = new StringBuilder();
			String line = null;
			while((line = reader.readLine()) != null)
			{
			    str.append(line);
			}
			in.close();
		}catch(Exception e){
			System.err.println("Error in retrieving response " + e.getMessage());
		}
		
		result = str.toString();

		
		return result;
	}
	
	public static InputStream getNetInputStream(String url){
		
		InputStream in = null;
		
		HttpResponse response = null;
		
		try{
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			response = client.execute(request);
		}catch(Exception e){
			System.out.println("Exception to GET page " + url);	
		}
		StringBuilder str = null;
		
		try{
			in = response.getEntity().getContent();
			
			return in;
		}catch(Exception e){
			System.err.println("Error in retrieving response " + e.getMessage());
		}
		
		return in;
	}
	
	/**
	 *  Checks to see if network access is enabled
	 *  
	 *  @throws NoNetworkAccessException if neither data connection or wifi are enabled
	 *  		NoAccessToHttpAddress if the library remote server can't be reached
	 *   
	 */
	public static boolean checkNetworkStatus(ConnectivityManager cm, Context context )
			throws NoNetworkAccessException, NoAccessToHttpAddress{
		
		boolean HaveConnectedWifi = false;
		boolean HaveConnectedMobile = false;
		
		
		boolean networkAccessEnabled = false;
		boolean httpAddressAccessReachable = false;
		
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if ( ni != null )
		{
		    if (ni.getType() == ConnectivityManager.TYPE_WIFI)
		        if (ni.isConnected()){
		            HaveConnectedWifi = true;
		            networkAccessEnabled = true;
		        }
		    if (ni.getType() == ConnectivityManager.TYPE_MOBILE)
		        if (ni.isConnected()){
		            HaveConnectedMobile = true;
		            networkAccessEnabled = true;
		        }
		}

		
		if(networkAccessEnabled){
			//check to see if httpAddress is avaialble using the network connection 
			// 2 seconds timeout
			httpAddressAccessReachable = checkIfNetAddressIsReachable(GlobalConfigs.httpAddress);
		
			if(httpAddressAccessReachable == false)
				throw new NoAccessToHttpAddress();
		}

		if(!networkAccessEnabled)
			throw new NoNetworkAccessException();
		
		return networkAccessEnabled;
		
	}
	
	public static boolean checkIfNetAddressIsReachable(String url){
		
		boolean result = false;
		try
		{
		    HttpGet request = new HttpGet(url);

		    HttpParams httpParameters = new BasicHttpParams();  

		    //timeout to 3 seconds
		    HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);
		    HttpClient httpClient = new DefaultHttpClient(httpParameters);
		    HttpResponse response = httpClient.execute(request);

		    int status = response.getStatusLine().getStatusCode();

		    if (status == HttpStatus.SC_OK) 
		    {
		        result = true;
		    }

		}
		catch (SocketTimeoutException e)
		{
		    result = false; // this is somewhat expected
		}
		catch (Exception e) {
			Log.d(TAG, "Exception in is reachable " + e.getMessage());
		}

		return result;
	}
	
}
