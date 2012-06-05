
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
public class AccountAccess {

	/** The SERVICE. */
	public static String SERVICE = "open-ils.auth";

	/** The METHOD Auth init  */
	public static String METHOD_AUTH_INIT = "open-ils.auth.authenticate.init";

	/** The METHOD Auth complete */
	public static String METHOD_AUTH_COMPLETE = "open-ils.auth.authenticate.complete";
	
	/** The METHOD Auth session retrieve */
	public static String METHOD_AUTH_SESSION_RETRV = "open-ils.auth.session.retrieve";

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
	
	private Integer authTime = null;
	
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
	public AccountAccess(String httpAddress) {

		this.httpAddress = httpAddress;

		try {
			// configure the connection
			
			System.out.println("Connection with " + httpAddress);
			conn = new HttpConnection(httpAddress + "/osrf-gateway-v1");

		} catch (Exception e) {
			System.err.println("Exception in establishing connection "
					+ e.getMessage());
		}

	}

	/**
	 * Md5.
	 *
	 * @param s the s
	 * @return the string
	 */
	private String md5(String s) {
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
	 * Gets the account summary.
	 *
	 * @return the account summary
	 */
	public void getAccountSummary(){
		
		Method method = new Method(METHOD_AUTH_SESSION_RETRV);

		method.addParam(authToken);

		//sync request
		HttpRequest req = new GatewayRequest(conn, SERVICE, method).send();
		Object resp;

		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			
		}
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
		complexParam.put("password", hash);

		
		method.addParam(complexParam);
		System.out.println("Compelx param " + complexParam);
		
		// sync test
		HttpRequest req = new GatewayRequest(conn, SERVICE, method).send();
		Object resp;

		
		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			
			String queryResult = ((Map<String,String>) resp).get("textcode");
			 
			System.out.println("Result " + queryResult);
			
			if(queryResult.equals("SUCCESS")){
				Object payload = ((Map<String,String>) resp).get("payload");
				authToken = ((Map<String,String>)payload).get("authtoken");
				try{
					System.out.println(((Map<String,Integer>)payload).get("authtoken"));
					authTime = ((Map<String,Integer>)payload).get("authtime");
					
				}catch(Exception e){
					System.err.println("Error in parsing authtime " + e.getMessage());
				}
				System.out.println();
			}
		}
		
		
	}

	

}
