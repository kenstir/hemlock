package org.evergreen.android.globals;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.evergreen.android.accountAccess.AccountAccess;
import org.opensrf.Method;
import org.opensrf.net.http.GatewayRequest;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.net.http.HttpRequest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.ImageView;

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

		System.out.println("Network access " + networkAccessEnabled);
		
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

		    //timeout to 1 seconds
		    HttpConnectionParams.setConnectionTimeout(httpParameters, 1000);
		    HttpClient httpClient = new DefaultHttpClient(httpParameters);
		    HttpResponse response = httpClient.execute(request);

		    //System.out.println("Check network response" + response);
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

	public static void bookCoverImage(ImageView picture, String imageID, int size){
		
		String urlS = (GlobalConfigs.httpAddress + "/opac/extras/ac/jacket/small/" + imageID);
        
        Bitmap bmp = null; //create a new Bitmap variable called bmp, and initialize it to null

        try {

                URL url = new URL(urlS); //create a URL object from the urlS string above
                URLConnection conn = url.openConnection(); //save conn as a URLConnection

                conn.connect(); //connect to the URLConnection conn
                InputStream is = conn.getInputStream(); //get the image from the URLConnection conn using InputStream is
                BufferedInputStream bis = new BufferedInputStream(is); //create a BufferedInputStream called bis from is
                bmp = BitmapFactory.decodeStream(bis); //use bis to convert the stream to a bitmap image, and save it to bmp
                int bmpHeight = bmp.getHeight(); //stores the original height of the image
                if(bmpHeight != 1){
                        double scale = size/(double)bmpHeight; //sets the scaling number to match the desired size
                        double bmpWidthh = (double)bmp.getWidth() * scale; //scales and stores the original width into the desired one
                        int bmpWidth = (int)bmpWidthh; //gets the width of the picture and saves it
                        bmp = Bitmap.createScaledBitmap(bmp, bmpWidth, size, true); //creates and stores a new bmp with desired dimensions
                }
                
        } catch (MalformedURLException e) { //catch errors
                e.printStackTrace();
        } catch(IOException e){
                e.printStackTrace();
        } catch(IllegalStateException e){
                e.printStackTrace();
        }
        picture.setImageBitmap(bmp); //send the Bitmap image bmp to pic, and call the method to set the image.
        

	}
	//TODO throw NO_SESSION 
	public static Object doRequest(HttpConnection conn, String service, String methodName, Object[] params) //throws SessionNotFoundException{
	{
		//TODO check params and throw errors
		Method method = new Method(methodName);
		System.out.println();
		for(int i=0;i<params.length;i++){
			method.addParam(params[i]);
			System.out.print("Param "+i+":" + params[i]);
		}
		System.out.println();
		//sync request
		HttpRequest req = new GatewayRequest(conn, service, method).send();
		Object resp;

		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			Object response = (Object) resp;
			
				try{
					String textcode = ((Map<String,String>)response).get("textcode");
					if(textcode != null){
						if(textcode.equals("NO_SESSION")){
							response = requireNewSession(conn, service, methodName, params);
						}
						
					}
				}catch(Exception e){
					
				}
				return response;
			
		}
		return null;
		
	}
	
	public static Object requireNewSession(HttpConnection conn, String service, String methodName, Object[] params){
		
		AccountAccess ac = AccountAccess.getAccountAccess();
		boolean success = ac.authenticate();
		
		Object response = null;
		
		if(success){
			response =  doRequest(conn, service, methodName, params);
		}
		
		return response;
	}

}
