
package org.evergreen.android.accountAccess;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.opensrf.Method;
import org.opensrf.net.http.GatewayRequest;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.net.http.HttpRequest;

/**
 * The Class AuthenticateUser.
 */
public class AuthenticateUser {

	/** The SERVICE. */
	public static String SERVICE = "open-ils.auth";

	/** The METHO d_ multicas s_ search. */
	public static String METHOD_AUTH_INIT = "open-ils.auth.authenticate.init";

	/** The METHO d_ sli m_ retrive. */
	public static String METHOD_AUTH_COMPLETE = "open-ils.auth.authenticate.complete";

	/** The conn. */
	public HttpConnection conn;

	/** The http address. */
	public String httpAddress = "http://ulysses.calvin.edu";

	/** The TAG. */
	public String TAG = "AuthenticareUser";
	
	/** The auth token. 
	 *  Sent with every request that needs authentication
	 * */
	private String authToken = null;
	
	//for demo purpose
	/** The user name. */
	private String userName = "staff";

	/** The password. */
	private String password = "demo123";
	
	/**
	 * Instantiates a new authenticate user.
	 *
	 * @param httpAddress the http address
	 */
	public AuthenticateUser(String httpAddress) {

		this.httpAddress = httpAddress;

		try {
			// configure the connection
			
			System.out.println("Connection with " + httpAddress);
			conn = new HttpConnection(httpAddress + "/osrf-gateway-v1");

		} catch (Exception e) {
			System.err.println("Exception in establishing connection "
					+ e.getMessage());
		}

		// OSRFRegistry.registerObject("mvr", WireProtocol.ARRAY, new String[]
		// {"title","author","doc_id","doc_type","pubdate","isbn","publisher","tcn","subject","type_of_resources","call_numbers","edition","online_loc","synopsis","physical_description","toc","copy_count","series","serials","foreign_copy_maps"});

	}

	/**
	 * Md5.
	 *
	 * @param s the s
	 * @return the string
	 */
	public String md5(String s) {
	    try {
	        // Create MD5 Hash
	        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
	        digest.update(s.getBytes());
	        byte messageDigest[] = digest.digest();

	        // Create Hex String
	        StringBuffer hexString = new StringBuffer();
	        for (int i=0; i<messageDigest.length; i++){
		        String hex = Integer.toHexString(0xFF & messageDigest[i]);
		        if (hex.length() == 1) {
		            // could use a for loop, but we're only dealing with a single byte
		            hexString.append('0');
		        }
		        hexString.append(hex);
	        }
	        return hexString.toString();
	        
	    } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
	    }
	    
	    return "";
	}
	
	/**
	 * Authenticate.
	 */
	public void authenticate(){
		
		String seed = authenticateInit();
		
		authenticateComplete(seed);
	}
	
	/**
	 * Authenticate init.
	 * @return seed for phase 2 of login
	 */
	private String authenticateInit() {

		Method method = new Method(METHOD_AUTH_INIT);

		method.addParam(userName);

		// sync test
		HttpRequest req = new GatewayRequest(conn, SERVICE, method).send();
		Object resp;
		
		String seed = null;
		
		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			seed = resp.toString();
		}
		
		System.out.println("Seed " + seed);
		
		return seed;
	}
	
	
	/**
	 * Authenticate complete.
	 * Phase 2 of login process
	 * Application send's username and hash to confirm login
	 * @param seed the seed
	 */
	private void authenticateComplete(String seed) {
		
		//calculate hash to pass to server for authentication process phase 2
		//seed = "b18a9063e0c6f49dfe7a854cc6ab5775";
		String hash = md5(seed+md5(password));
		System.out.println("Hash " + hash);
		
		Method method = new Method(METHOD_AUTH_COMPLETE);
		
		HashMap<String,String> complexParam = new HashMap<String, String>();
		
		
		complexParam.put("username", userName);
		complexParam.put("password", hash+"'");

		
		method.addParam(complexParam);
		System.out.println("Compelx param " + complexParam);
		
		// sync test
		HttpRequest req = new GatewayRequest(conn, SERVICE, method).send();
		Object resp;

		
		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			
			String queryResult = ((Map<String,String>) resp).get("desc");
			 
			System.out.println("Result " + queryResult);
			
			if(queryResult.equals("Success")){
				authToken = ((Map<String,String>) resp).get("authtoken");
			}
		}
		
		
	}

}
