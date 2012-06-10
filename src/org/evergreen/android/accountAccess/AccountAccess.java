
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
	
	public static String SERVICE_SERIAL = "open-ils.serial";
	
	public static String SERVICE_FIELDER = "open-ils.fielder";
	
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
	 * description : Retrieves a circ object by ID.
	 * @param : authtoken, circ_id
	 * @returns : "circ" class. Fields of interest : renewal_remaining, due_date 
	 */
	public static String METHOD_FETCH_CIRC_BY_ID = "open-ils.circ.retrieve";
	
	/** The METHOD_FETCH_MODS_FROM_COPY
	 * description : used to return info
	 * @param : target_copy
	 * @returns : mvr class OSRF Object. Fields of interest : title, author 
	 */
	public static String METHOD_FETCH_MODS_FROM_COPY = "open-ils.search.biblio.mods_from_copy";
	
	/** The METHOD_FETCH_COPY 
	 * description : used to return info for a PRE_CATALOGED object
	 * @param : target_copy
	 * @returns : acp class OSRF Object. Fields of interest : dummy_title, dummy_author
	 */
	public static String METHOD_FETCH_COPY = "open-ils.search.asset.copy.retrieve";
	/**
	 * The METHOD_RENEW_CIRC
	 * description : used to renew a circulation object
	 * @param : HashMap ex :{	{"patron":id,"copyid":copy_id,"opac_renewal":1} }
	 * @returnes : acn, acp, circ, mus, mbts
	 */
	public static String METHOD_RENEW_CIRC = "open-ils.circ.renew";
	
	// Used for Holds Tab
	
	/** The METHOD_FETCH_HOLDS
	 *  @param : authtoken, userID
	 *  @returns: List of "ahr" OSPFObject . Fields of interest : pickup_lib
	 */
	public static String METHOD_FETCH_HOLDS = "open-ils.circ.holds.retrieve";
	
	/** The METHOD_FETCH_ORG_SETTINGS
	 *  description : retrieves a setting from the organization unit
	 *  @param : org_id, String with setting property to return
	 *  @returns : returns the requested value of the setting
	 */
	public static String METHOD_FETCH_ORG_SETTINGS = "open-ils.actor.ou_setting.ancestor_default";
	
	/** The METHOD_FETCH_MRMODS
	 * 
	 */
	// if holdtype == M
	public static String METHOD_FETCH_MRMODS = "open-ils.search.biblio.metarecord.mods_slim.retrieve";
	// if holdtype == T
	public static String METHOD_FETCH_RMODS = "open-ils.search.biblio.records.mods_slim.retrieve";
	//if hold type V
	public static String METHOD_FETCH_VOLUME = "open-ils.search.asset.call_number.retrieve";
	//if hold type I
	public static String METHOD_FETCH_ISSUANCE = "open-ils.serial.issuance.pub_fleshed.batch.retrieve";
	
	public static String METHOD_FETCH_HOLD_STATUS = "open-ils.circ.hold.queue_stats.retrieve";
	
	/** The METHOD_UPDATE_HOLD
	 *  description : Updates the specified hold. If session user != hold user then session user 
	 *  must have UPDATE_HOLD permissions 
	 *  @param : authtoken, ahr object
	 *  @returns : hold_is on success, event or error on failure 
	 */
	public static String METHOD_UPDATE_HOLD = "open-ils.circ.hold.update";

	/** The METHOD_CANCEL_HOLD
	 *  description : Cancels the specified hold. session user != hold user 
	 *  must have CANCEL_HOLD permissions.
	 *  @param : authtoken, hold_ids, one after another : 1,21,33,.... 
	 *  @returns : 1 on success, event or error on failure
	 */
	public static String METHOD_CANCEL_HOLD = "open-ils.circ.hold.cancel";
	
	/** The METHOD_VERIFY_HOLD_POSSIBLE
	 *  description : 
	 *  @param : authtoken , hashmap 	{"titleid":38,"mrid":35,"volume_id":null,"issuanceid":null,
	 *  "copy_id":null,"hold_type":"T","holdable_formats":null,"patronid":2,"depth":0,"pickup_lib":"8","partid":null}
	 *  @returns :  hashmap with "success" : 1 field or 
	 */
	public static String METHOD_VERIFY_HOLD_POSSIBLE = "open-ils.circ.title_hold.is_possible";
	
	/** The METHOD_CREATE_HOLD
	 *  description : 
	 *  @param : authtoken, ahr OSRFObject 
	 *  @returns : hash with messages : "success" : 1 field or 
	 */
	public static String METHOD_CREATE_HOLD = "	open-ils.circ.holds.create";
	
	//Used for Fines 
	
	/** The METHODS_FETCH_FINES_SUMMARY
	 * description : 
	 * @param : authToken, UserID
	 * @returns: "mous" OSRFObject. fields: balance_owed, total_owed, total_paid
	 */
	public static String METHOD_FETCH_FINES_SUMMARY = "open-ils.actor.user.fines.summary";
	
	/** The METHOD_FETCH_TRANSACTIONS
	 * description: For a given user retrieves a list of fleshed transactions. List of objects, each object is a hash 
	 * containing : transaction, circ, record
	 * @param : authToken, userID
	 * @returns : array of objects, must investigate
	 */
	public static String METHOD_FETCH_TRANSACTIONS = "open-ils.actor.user.transactions.have_charged.fleshed";
	
	/** The METHOD_FETCH_MONEY_BILLING
	 *  description :
	 *  @param : authToken, transaction_id;
	 */
	public static String METHOD_FETCH_MONEY_BILLING = "open-ils.circ.money.billing.retrieve.all";
	
	
	//Used for book bags
	/** The METHOD_FLESH_CONTAINERS
	 * description : Retrieves all un-fleshed buckets by class assigned to a given user
	 * VIEW_CONTAINER permissions is requestID != owner ID
	 * @param : authtoken, UserID, "biblio", "bookbag"
	 * @returns : array of "cbreb" OSRFObjects
	 */
	public static String METHOD_FLESH_CONTAINERS = "open-ils.actor.container.retrieve_by_class.authoritative";
	
	
	/** The METHOD_FLESH_PUBLIC_CONTAINER
	 * description : array of contaoners correspondig to a id 
	 * @param : authtoken , "biblio" , boobkbag ID
	 * @returns : array of "crebi" OSRF objects (content of bookbag, id's of elements to get more info)
	 */
	public static String METHOD_FLESH_PUBLIC_CONTAINER = "open-ils.actor.container.flesh";
	
	
	
	
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
	
	private Integer userID = null;
	//for demo purpose
	/** The user name. */
	private String userName = "daniel";

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
	public OSRFObject getAccountSummary(){
		
		Method method = new Method(METHOD_AUTH_SESSION_RETRV);

		method.addParam(authToken);

		//sync request
		HttpRequest req = new GatewayRequest(conn, SERVICE_AUTH, method).send();
		Object resp;

		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			 OSRFObject au = (OSRFObject) resp;
			 userID = au.getInt("id");
			 System.out.println("User Id " + userID);
			 
			 return au;
		}
		return null;
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
		//TODO parameter for user login
		complexParam.put("type", "opac");
		
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

	
	//------------------------Checked Out Items Section -------------------------//
	
	public void getItemsCheckedOut(){
		
		Method method = new Method(METHOD_FETCH_CHECKED_OUT_SUM);

		method.addParam(authToken);
		method.addParam(userID);
		
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
				fetchInfoForCheckedOutItem(out.get(i).get("target_copy")+"");
				
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
	/* Fetch info for Checked Out Items
	 * It uses two methods  : open-ils.search.biblio.mods_from_copy or in case
	 * of pre-cataloged records it uses open-ils.search.asset.copy.retriev
	 * Usefull info : title and author 
	 *  (for acp : dummy_title, dummy_author)
	 */
	private OSRFObject fetchInfoForCheckedOutItem(String target_copy){
		
		OSRFObject result;
		OSRFObject info_mvr = fetchModsFromCopy(target_copy);
		//if title or author not inserted, request acp with copy_target
		result = info_mvr;
		OSRFObject info_acp = null;
		
		if(info_mvr.get("title") == null || info_mvr.get("author") == null){
			info_acp = fetchAssetCopy(target_copy);
			result = info_acp;
		}
		
		return result;
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
	
	private OSRFObject fetchModsFromCopy(String target_copy){
		
		Method method = new Method(METHOD_FETCH_MODS_FROM_COPY);

		method.addParam(target_copy);
		
		//sync request
		HttpRequest req = new GatewayRequest(conn, SERVICE_SEARCH, method).send();
		Object resp;

		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			OSRFObject mvr = (OSRFObject) resp;
			
			return mvr;
		}
		
		return null;
	}
	
	private OSRFObject fetchAssetCopy(String target_copy){
		
		Method method = new Method(METHOD_FETCH_COPY);

		method.addParam(target_copy);
		
		//sync request
		HttpRequest req = new GatewayRequest(conn, SERVICE_SEARCH, method).send();
		Object resp;

		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			OSRFObject acp = (OSRFObject) resp;
			
			return acp;
		}
		
		return null;
	}
	/* Method used to renew a circulation record based on target_copy_id
	 * Returns many objects, don't think they are needed
	 */
	private void renewCirc(Integer target_copy){
		
		Method method = new Method(METHOD_RENEW_CIRC);

		HashMap<String,Integer> complexParam = new HashMap<String, Integer>();
		complexParam.put("patron", this.userID);		
		complexParam.put("copyid", target_copy);
		complexParam.put("opac_renewal", 1);

		method.addParam(complexParam);
		
		//sync request
		HttpRequest req = new GatewayRequest(conn, SERVICE_CIRC, method).send();
		Object resp;

		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			OSRFObject a_lot = (OSRFObject) resp;

		}
	}

	//------------------------Holds Section --------------------------------------//
	
	public Object fetchOrgSettings(Integer org_id, String setting){
		
		Method method = new Method(METHOD_FETCH_ORG_SETTINGS);

		method.addParam(org_id);
		method.addParam(setting);
		
		//sync request
		HttpRequest req = new GatewayRequest(conn, SERVICE_ACTOR, method).send();
		Object resp;

		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			//TODO Do something with the property
			OSRFObject response = (OSRFObject) resp;
			return response;
		}
		return null;
	}
	
	
	
	public void getHolds(){
		
		Method method = new Method(METHOD_FETCH_HOLDS);

		method.addParam(authToken);
		method.addParam(userID);
		
		//sync request
		HttpRequest req = new GatewayRequest(conn, SERVICE_CIRC, method).send();
		Object resp;

		List<OSRFObject> listHoldsAhr = null;
		
		// holds description for each hold
		List<OSRFObject> listHoldsMvr = null;
		
		//status of holds, fields like : potential_copies, status, total_holds, queue_position, estimated_wait
		List<HashMap<String,Integer>> listHoldsStatus = null; 

		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			//list of ahr objects
			listHoldsAhr = (List<OSRFObject>) resp;
		
		}
		
		for(int i=0;i<listHoldsAhr.size();i++){
			fetchHoldTitleInfo(listHoldsAhr.get(i));
			fetchHoldStatus(listHoldsAhr.get(i));
		}
		
	}
	
	/* hold target type :
	 *  M - metarecord
	 *  T - record
	 *  V - volume
	 *  I - issuance
	 *  C - copy
	 *  P - pat
	 */
	
	private Object fetchHoldTitleInfo(OSRFObject holdArhObject){
		
		
		String holdType = (String)holdArhObject.get("hold_type");
		
		String method = null;
		
		Object response;
		Object holdInfo = null;
		if(holdType.equals("T") || holdType.equals("M")){
			
			if(holdType.equals("T")) 
				method = METHOD_FETCH_MRMODS;
			if(holdType.equals("M"))
				method = METHOD_FETCH_RMODS;
			
			holdInfo = doRequest(SERVICE_SEARCH, method, new Object[]{holdArhObject.get("target")});

		}
		else{
				//multiple objects per hold ????
				holdInfo = holdFetchObjects(holdArhObject);
;
			}
		return holdInfo;
	}
	
	private Object holdFetchObjects(OSRFObject hold){
		
		String type = (String)hold.get("hold_type");
		
		if(type.equals("C")){
			//fetch_copy
			OSRFObject copyObject = fetchAssetCopy(hold.getString("target"));	
			//fetch_volume from copyObject.call_number field
			Integer call_number = copyObject.getInt("call_number");
			
			if(call_number != null){
				OSRFObject volume = (OSRFObject) doRequest(SERVICE_CIRC, METHOD_FETCH_VOLUME, new Object[]{copyObject.getInt("call_number")});	
			//in volume object : record
			}
			
			return copyObject;
		}
		else
			if(type.equals("V")){
				//fetch_volume
				OSRFObject volume = (OSRFObject) doRequest(SERVICE_CIRC, METHOD_FETCH_VOLUME, new Object[]{hold.getInt("target")});
				//in volume object : record 
			}
			else
				if(type.equals("I")){	
					OSRFObject issuance = (OSRFObject) doRequest(SERVICE_SERIAL, METHOD_FETCH_ISSUANCE, new Object[]{hold.getInt("target")});
				}
				else
					if(type.equals("P")){
						HashMap<String, Object> param = new HashMap<String, Object>(0);
						
						param.put("cache", 1);
						param.put("fields", new String[]{"label","record"});
							HashMap<String, Integer> queryParam = new HashMap<String, Integer>();
							//PART_ID use "target field in hold"
							queryParam.put("id", hold.getInt("target"));
						param.put("query",queryParam);
						
						//returns mvr object
						OSRFObject part = (OSRFObject) doRequest(SERVICE_FIELDER,"open-ils.fielder.bmp.atomic",new Object[]{});
					}
			
		return null;
	}
	
	
	public Object fetchHoldStatus(OSRFObject hold){
		
		Integer hold_id = hold.getInt("id");
		// MAP : potential_copies, status, total_holds, queue_position, estimated_wait
		Object hold_status = doRequest(SERVICE_CIRC, METHOD_FETCH_HOLD_STATUS, new Object[]{authToken,hold_id});
	
		return hold_status;
	}
	
	
	public Object cancelHold(OSRFObject hold){
		
		Integer hold_id = hold.getInt("id");
		
		Object response = doRequest(SERVICE_CIRC, METHOD_CANCEL_HOLD, new Object[]{authToken,hold_id});
		
		return response;
	}
	
	public Object updateHold(OSRFObject newHoldObject){
		//TODO verify that object is correct passed to the server
		Object response = doRequest(SERVICE_CIRC, METHOD_UPDATE_HOLD, new Object[]{authToken,newHoldObject});
		
		return response;
	}
	
	public Object createHold(OSRFObject newHoldObject){
		
	Object response = doRequest(SERVICE_CIRC, METHOD_CREATE_HOLD, new Object[]{authToken,newHoldObject});
		
		return response;
	}
	// ?? return boolean 
	public Object isHoldPossible(HashMap<String,?> valuesHold){
		
		
		Object response = doRequest(SERVICE_CIRC, METHOD_VERIFY_HOLD_POSSIBLE, new Object[]{authToken,valuesHold});
		
		return response;
	}
	
	//----------------------------Fines Summary------------------------------------//
	
	public OSRFObject getFinesSummary(){
		
		OSRFObject finesSummary = (OSRFObject) doRequest(SERVICE_ACTOR, METHOD_FETCH_FINES_SUMMARY, new Object[]{authToken,userID});
		
		return finesSummary;
	}
	
	private Object getTransactions(){
		
		Object transactions = doRequest(SERVICE_ACTOR, METHOD_FETCH_TRANSACTIONS, new Object[]{authToken,userID});
		
		return transactions;
	}
	
	public Object getBookbags(){
		
		Object response = doRequest(SERVICE_ACTOR, METHOD_FLESH_CONTAINERS, new Object[]{authToken,userID,"biblio","bookbag"});
	
		List<OSRFObject> bookbags = (List<OSRFObject>)response;
		
		for(int i=0;i<bookbags.size();i++){
			
			getBookbagContent(bookbags.get(i).getInt("id"));
		}
		
		
		return bookbags;
	}
	
	private Object getBookbagContent(Integer bookbagID){
		
		return doRequest(SERVICE_ACTOR, METHOD_FLESH_PUBLIC_CONTAINER, new Object[]{authToken,"biblio",bookbagID});
	}
	
	private Object doRequest(String service, String methodName, Object[] params){
		
		
		//TODO check params and throw errors
		Method method = new Method(methodName);

		for(int i=0;i<params.length;i++)
		method.addParam(params[i]);
		
		//sync request
		HttpRequest req = new GatewayRequest(conn, service, method).send();
		Object resp;

		while ((resp = req.recv()) != null) {
			System.out.println("Sync Response: " + resp);
			Object response = (Object) resp;
			return response;
		}
		return null;
		
	}
	
	
	
}
