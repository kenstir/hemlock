package org.evergreen.android.globals;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class Utils {

	
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
}
