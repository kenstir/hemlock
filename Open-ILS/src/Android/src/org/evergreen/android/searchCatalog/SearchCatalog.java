
package org.evergreen.android.searchCatalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.globals.Utils;
import org.open_ils.idl.IDLParser;
import org.opensrf.Method;
import org.opensrf.net.http.GatewayRequest;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.net.http.HttpRequest;
import org.opensrf.net.http.HttpRequestHandler;
import org.opensrf.util.OSRFObject;

import android.content.Context;
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

	/** The conn. */
	public HttpConnection conn;
	
	
	/** The TAG. */
	public String TAG = "SearchCatalog";
	
	//the org on witch the searches will be made
	/** The selected organization. */
	private Organisation selectedOrganization = null;
	
	private Context context;
	/**
	 * Instantiates a new search catalog.
	 *
	 * @param httpAddress the http address
	 * @param locale the locale
	 */
	public SearchCatalog(Context context) {
		super();
		this.context = context;
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
	public ArrayList<RecordInfo> getSearchResults(String searchWords){
		
		
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
	        	complexParm.put("limit", 50);
	        	
	        	/*
	        	complexParm.put("offset",0);
	        	complexParm.put("visibility_limit", 3000);
	        	complexParm.put("default_class","keyword");
	        	*/
	        	
	        }catch(Exception e)
	        {
	        	System.out.println("Exception in JSON " + e.getMessage());
	        }
	        
	        System.out.println("JSON argument " + complexParm);
	        method.addParam(complexParm);
	        method.addParam(searchWords);
	        method.addParam(1);
	        
	        // sync test
	        HttpRequest req = new GatewayRequest(conn, SERVICE, method).send();
	        Object resp;
	        
	        //why in while ?
	        
	        ArrayList<String> ids = new ArrayList<String>();
	        
	        while ( (resp = req.recv()) != null){
	            System.out.println("Sync Response: " + resp);
	            
	            Map<String,?> response = (Map<String,?>) resp;
	            
	            System.out.println(" ids : " + response.get("ids") + " " );
	            
	            List<List<String>> result_ids = (List) response.get("ids");
	            
	            for(int i=0;i<result_ids.size();i++){
	            	ids.add(result_ids.get(i).get(0));
	            }

	        }
	        // exceptions are captured instead of thrown, 
	        // primarily to better support async requests
	        if (req.failed()) {
	            req.getFailure().printStackTrace();
	            return null;
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
	public Object searchCatalog(String searchWords){
		
		
        Method method = new Method(METHOD_SLIM_RETRIVE);

        method.addParam("keyword");
        method.addParam(searchWords);
        
        
        // sync test
        HttpRequest req = new GatewayRequest(conn, SERVICE, method).send();
        Object resp;
        while ( (resp = req.recv()) != null) {
            System.out.println("Sync Response: " + resp);
            return resp;
        }
     
        
        // exceptions are captured instead of thrown, 
        // primarily to better support async requests
        if (req.failed()) {
            req.getFailure().printStackTrace();
            return null;
        }
        
        return null;
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
