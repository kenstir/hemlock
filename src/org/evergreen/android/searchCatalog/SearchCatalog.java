
package org.evergreen.android.searchCatalog;

import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.evergreen.android.accountAccess.SessionNotFoundException;
import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.globals.NoAccessToServer;
import org.evergreen.android.globals.NoNetworkAccessException;
import org.evergreen.android.globals.Utils;
import org.opensrf.Method;
import org.opensrf.net.http.GatewayRequest;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.net.http.HttpRequest;
import org.opensrf.net.http.HttpRequestHandler;
import org.opensrf.util.OSRFObject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

/**
 * The Class SearchCatalog.
 */
public class SearchCatalog {

	/** The SERVICE. */
	public static String SERVICE = "open-ils.search";
	
	/** The METHO d_ multicas s_ search. */
	public static String METHOD_MULTICASS_SEARCH = "open-ils.search.biblio.multiclass.query";
	
	/** The METHO d_ sli m_ retrive. */
	public static String METHOD_SLIM_RETRIVE  = "open-ils.search.biblio.record.mods_slim.retrieve";

	/** Method 
	 * @param : no parameters 
	 * @returns : returns array of ccs objects
	 */
	
	public static String METHOD_COPY_STATUS_ALL ="open-ils.search.config.copy_status.retrieve.all";
	
	/**
	* Method that returns library where record with id is
	* @param : record ID to get all libraries, or just book ID, Current Library ID, User ID
	* @returns : [[["4","","CONCERTO 27","","Stacks",{"0":5}],["4","","PERFORM 27","","Stacks",{"0":2}]]]
	*    "0":% is the available books
	*  [org_id, call_number_sufix, copy_location, status1:count, status2:count ..]
	*/
	public static String METHOD_COPY_LOCATION_COUNTS = "open-ils.search.biblio.copy_location_counts.summary.retrieve";

	public static SearchCatalog searchCatalogSingleton = null;
	/** The conn. */
	public HttpConnection conn;
	
	
	/** The TAG. */
	public String TAG = "SearchCatalog";
	
	//the org on witch the searches will be made
	/** The selected organization. */
	public Organisation selectedOrganization = null;

	public Integer offset;
	
	public Integer visible;
	
	public Integer searchLimit = 10;
	
	private ConnectivityManager cm;
	
	public static SearchCatalog getInstance(ConnectivityManager cm){
		
		if(searchCatalogSingleton == null){
			searchCatalogSingleton = new SearchCatalog(cm);
		}
		
		return searchCatalogSingleton;
	}
	/**
	 * Instantiates a new search catalog.
	 *
	 * @param httpAddress the http address
	 * @param locale the locale
	 */
	private SearchCatalog(ConnectivityManager cm) {
		super();
		
		this.cm = cm;
		
		try{
			// configure the connection
			conn = new HttpConnection(GlobalConfigs.httpAddress+"/osrf-gateway-v1");

			
		}catch(Exception e){
			System.err.println("Exception in establishing connection " +  e.getMessage());
		}
		//registering classes so no longer necessary to register object classes manually

	}
	
	
	

	/**
	 * Gets the search results
	 * 
	 * @param searchWords the search words
	 * @return the search results
	 */
	public ArrayList<RecordInfo> getSearchResults(String searchWords, Integer offset) throws NoNetworkAccessException, NoAccessToServer{
		
		
		ArrayList<RecordInfo> resultsRecordInfo = new ArrayList<RecordInfo>();
		
		
		 Method method = new Method(METHOD_MULTICASS_SEARCH);

		 HashMap complexParm = new HashMap<String,Integer>();

		   try{
	        	if(this.selectedOrganization != null){
	        		if(this.selectedOrganization.id != null)
	        			complexParm.put("org_unit", this.selectedOrganization.id);
	        		if(this.selectedOrganization.level != null)
	        			complexParm.put("depth", this.selectedOrganization.level-1);
	        	}
	        	//TODO change here, multiple result per page
	        	complexParm.put("limit", searchLimit);
	        	
		        complexParm.put("offset",offset);
	        	
	        	/*
	        	complexParm.put("offset",0);
	        	complexParm.put("visibility_limit", 3000);
	        	complexParm.put("default_class","keyword");
	        	*/
	        	
	        }catch(Exception e)
	        {
	        	System.out.println("Exception in JSON " + e.getMessage());
	        }

	        
	        Object resp = Utils.doRequest(conn, SERVICE, METHOD_MULTICASS_SEARCH, cm, new Object[]{complexParm,searchWords,1});

	        ArrayList<String> ids = new ArrayList<String>();

	        System.out.println("Sync Response: " + resp);
	            
	        Map<String,?> response = (Map<String,?>) resp;
	            
	        System.out.println(" ids : " + response.get("ids") + " " );
	            
	        List<List<String>> result_ids = (List) response.get("ids");

	        visible =Integer.parseInt((String)response.get("count"));
	            
	            for(int i=0;i<result_ids.size();i++){
	            	ids.add(result_ids.get(i).get(0));
	            }

	        
	        System.out.println("Ids " + ids);
	        
	        //request other info based on ids
	        

	        for(int i=0;i<ids.size();i++){
	        	
	        	RecordInfo record = new RecordInfo(getItemShortInfo(ids.get(i)));
	        	resultsRecordInfo.add(record);
	        	System.out.println("Title " + record.title + " Author " + record.author + " Pub date" + record.pubdate +" Publisher" + record.publisher);
	        }
		
		return resultsRecordInfo;
	}
	
	
	
	/**
	 * Gets the item short info.
	 *
	 * @param id the id
	 * @return the item short info
	 */
	private OSRFObject getItemShortInfo(String id){
		
		Method method = new Method(METHOD_SLIM_RETRIVE);
		
		method.addParam(id);

		 HttpRequest req = new GatewayRequest(conn, SERVICE, method).send();
	        Object resp;
	        while ( (resp = req.recv()) != null) {
	            System.out.println("Sync Response: " + resp);
	            return (OSRFObject)resp;
	        }
		
	        return null;
	}
	
	
	/**
	 * Search catalog.
	 *
	 * @param searchWords the search words
	 * @return the object
	 */
	public Object searchCatalog(String searchWords) 
			throws NoNetworkAccessException, NoAccessToServer{

        Object response = Utils.doRequest(conn, SERVICE, METHOD_SLIM_RETRIVE, cm, new Object[]{"keyword", searchWords});
        
        return response;
        
	}
	
	/**
	 * Search catalog.
	 *
	 * @param searchWords the search words
	 * @param requestHandler the request handler
	 */
	public void searchCatalog(String searchWords, HttpRequestHandler requestHandler){
			
        Method method = new Method(METHOD_SLIM_RETRIVE);
    
        method.addParam(searchWords);  

        // sync test
        HttpRequest req = new GatewayRequest(conn, SERVICE, method).send();
        req.sendAsync(requestHandler);

	}

	
	public Object getLocationCount(Integer recordID, Integer orgID, Integer orgDepth) throws NoNetworkAccessException, NoAccessToServer{
		
		List<?> list = (List<?>)Utils.doRequest(conn, SERVICE, METHOD_COPY_LOCATION_COUNTS, cm, new Object[]{recordID, orgID, orgDepth});
		return list;
		
	}
	
	/**
	 * Select organisation.
	 *
	 * @param org the organization on witch the searches will be made
	 */
	public void selectOrganisation(Organisation org){
		
		
		Log.d(TAG,"Select search organisation " + (org.level-1) + " " + org.id ); 
		this.selectedOrganization = org;
		
		
	}
	

}
