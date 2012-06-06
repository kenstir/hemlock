
package org.evergreen.android.accountAccess;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opensrf.Method;
import org.opensrf.net.http.GatewayRequest;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.net.http.HttpRequest;
import org.opensrf.util.OSRFObject;

/**
 * The Class AuthenticateUser.
 */
public class AccountAccess {

	//Used for authentication purpose
	
	/** The SERVICE. */
	public static String SERVICE_AUTH = "open-ils.auth";
	
	/** The METHOD Auth init. */
	public static String METHOD_AUTH_INIT = "open-ils.auth.authenticate.init";

	/** The METHOD Auth complete. */
	public static String METHOD_AUTH_COMPLETE = "open-ils.auth.authenticate.complete";
	
	/** The METHOD Auth session retrieve. */
	public static String METHOD_AUTH_SESSION_RETRV = "open-ils.auth.session.retrieve";

	// Used for the Checked out Items Tab
	
	/** The SERVIC e_ actor. */
	public static String SERVICE_ACTOR = "open-ils.actor";
	
	/** The SERVIC e_ circ. */
	public static String SERVICE_CIRC = "open-ils.circ";
	
	public static String SERVICE_SEARCH = "open-ils.search";
	
	/** The METHOD_FETCH_CHECKED_OUT_SUM
	 *  description : for a given user returns a a structure of circulation objects sorted by
	 *  out, overdue, lost, claims_returned, long_overdue; A list of ID's returned for each type : "out":[id1,id2,...]   
	 *  @param : authtoken , UserID 
	 *  @returns: { "out":[id's],"claims_returned":[],"long_overdue":[],"overdue":[],"lost":[]}
	 */
	public static String METHOD_FETCH_CHECKED_OUT_SUM = "open-ils.actor.user.checked_out";

	/** The METHOD_FETCH_NON_CAT_CIRCS  
	 * description : for a given user, returns an id-list of non-cataloged circulations that are considered open for now.
	 * A circ is open if circ time + circ duration (based on type) is > than now 
	 * @param : authtoken, UserID
	 * @returns: Array of non-catalogen circ IDs, event or error
	 */
	public static String METHOD_FETCH_NON_CAT_CIRCS = "open-ils.circ.open_non_cataloged_circulation.user";
	
	/** The METHOD_FETCH_CIRC_BY_ID 
	 * description : Retrieves a circ object by ID
	 * @param : authtoken, circ_id
	 * @returns : "circ" class
	 */
	public static String METHOD_FETCH_CIRC_BY_ID = "open-ils.circ.retrieve";
	
	/** The METHOD_FETCH_MODS_FROM_COPY
	 * description :
	 * @param : target_copy
	 * @returns : mvr class OSRF Object
	 */
	public static String METHOD_FETCH_MODS_FROM_COPY = "open-ils.search.biblio.mods_from_copy";
	
	public static String METHOD_FETCH_COPY = "open-ils.search.asset.copy.retrieve";
	
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
	
	/** The auth time. */
	private Integer authTime = null;
	
	//for demo purpose
	/** The user name. */
	private String userName = "admin";

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
		HttpRequest req = new GatewayRequest(conn, SERVICE_AUTH, method).send();
		Object resp;

		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			 OSRFObject au = (OSRFObject) resp;
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
		HttpRequest req = new GatewayRequest(conn, SERVICE_AUTH, method).send();
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
		HttpRequest req = new GatewayRequest(conn, SERVICE_AUTH, method).send();
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

	
	public void getItemsCheckedOut(){
		
		Method method = new Method(METHOD_FETCH_CHECKED_OUT_SUM);

		method.addParam(authToken);
		method.addParam(1);
		
		//sync request
		HttpRequest req = new GatewayRequest(conn, SERVICE_ACTOR, method).send();
		Object resp;

		
		ArrayList<OSRFObject> long_overdue = new ArrayList<OSRFObject>();
		ArrayList<OSRFObject> claims_returned = new ArrayList<OSRFObject>();
		ArrayList<OSRFObject> lost = new ArrayList<OSRFObject>();
		ArrayList<OSRFObject> out = new ArrayList<OSRFObject>();
		
		List<String> long_overdue_id;
		List<String> claims_returned_id;
		List<String> lost_id;
		List<String> out_id;
		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			
			long_overdue_id = (List<String>)((Map<String,?>)resp).get("long_overdue");
			claims_returned_id = (List<String>)((Map<String,?>)resp).get("claims_returned");
			lost_id = (List<String>)((Map<String,?>)resp).get("lost");
			out_id = (List<String>)((Map<String,?>)resp).get("out");
			
			//get all the record circ info
			for(int i=0;i<out_id.size();i++){
				//System.out.println(out.get(i));
				out.add(retrieveCircRecord(out_id.get(i)));

				System.out.println(out.get(i).get("target_copy"));
				fetchModsFromCopy((out.get(i).get("target_copy"))+"");
			}
			for(int i=0;i<lost_id.size();i++){
				//System.out.println(out.get(i));
				lost.add(retrieveCircRecord(lost_id.get(i)));
			}
			for(int i=0;i<claims_returned.size();i++){
				//System.out.println(out.get(i));
				claims_returned.add(retrieveCircRecord(claims_returned_id.get(i)));
			}
			for(int i=0;i<long_overdue_id.size();i++){
				//System.out.println(out.get(i));
				long_overdue.add(retrieveCircRecord(long_overdue_id.get(i)));
			}
		}
	}
	/* Retreives the Circ record
	 * @param : target_copy from circ
	 * @returns : "circ" OSRFObject 
	 */
	private OSRFObject retrieveCircRecord(String id){
		
		Method method = new Method(METHOD_FETCH_CIRC_BY_ID);

		method.addParam(authToken);
		method.addParam(id);
		
		//sync request
		HttpRequest req = new GatewayRequest(conn, SERVICE_CIRC, method).send();
		Object resp;

		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			OSRFObject circ = (OSRFObject) resp;
			return circ;
		}
		
		return null;
	}
	
	private void fetchModsFromCopy(String target_copy){
		
		Method method = new Method(METHOD_FETCH_MODS_FROM_COPY);

		method.addParam(target_copy);
		
		//sync request
		HttpRequest req = new GatewayRequest(conn, SERVICE_SEARCH, method).send();
		Object resp;

		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			OSRFObject mvr = (OSRFObject) resp;
		}
	}

}
