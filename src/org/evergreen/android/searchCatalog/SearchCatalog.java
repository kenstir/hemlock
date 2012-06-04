
package org.evergreen.android.searchCatalog;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.open_ils.idl.IDLParser;
import org.opensrf.Method;
import org.opensrf.net.http.GatewayRequest;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.net.http.HttpRequest;
import org.opensrf.net.http.HttpRequestHandler;

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
	
	/** The collections request. */
	public static String collectionsRequest = "/opac/common/js/";
	
	/** The locale. */
	public String locale = "en-US";  
	
	/** The http address. */
	public String httpAddress = "http://ulysses.calvin.edu";
	
	/** The TAG. */
	public String TAG = "SearchCatalog";
	
	/** The organisations. */
	public ArrayList<Organisation> organisations;
	
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
	public SearchCatalog(String httpAddress, String locale, Context context) {
		super();
		this.context = context;
		this.httpAddress = httpAddress;
		try{
			// configure the connection
			conn = new HttpConnection(httpAddress+"/osrf-gateway-v1");
			this.locale = locale;
			collectionsRequest += locale + "/OrgTree.js";
			
		}catch(Exception e){
			System.err.println("Exception in establishing connection " +  e.getMessage());
		}
		//registering classes so no longer necessary to register object classes manually
		readIDLFile();

		this.organisations = new ArrayList<Organisation>();
		getOrganisations();
	}
	
	
	/**
	 * Gets the organisations get's OrgTree.js from server, parses the input and recovers the organisations
	 *
	 * @return the organisations
	 */
	private void getOrganisations(){
		
		String orgFile = null;
		try{
			orgFile = getNetPageContent(httpAddress+collectionsRequest);
			System.out.println("Request org " + httpAddress + collectionsRequest );
		}catch(Exception e){};
		
		
		if(orgFile != null){
			System.out.println("Page content " + orgFile);
			
			String orgArray = orgFile.substring( orgFile.indexOf("=")+1, orgFile.indexOf(";"));  
			
			String arrayContent = orgArray.substring(orgArray.indexOf("[")+1,orgArray.lastIndexOf("]"));
			
			
			Log.d(TAG,"Array to pe parsed " + arrayContent);
			
			//parser for list
			
			//format [104,2,1,"Curriculum Center",1,"CURRICULUM"]  : [id,level,parent,name,can_have_volumes_bool,short_name]
			
			int index = 0;
			while(true){
				
				if(index >= arrayContent.length())
					break;
				
				int start = arrayContent.indexOf("[", index)+1;
				int stop = arrayContent.indexOf("]", index);
				
				Log.d(TAG," start stop length index" + start+ " " + stop + " " + arrayContent.length() + " " + index);
				if(start == -1 || stop == -1)
					break;
				
				index = stop+1;
				
				String content = arrayContent.substring(start,stop);
				
				System.out.println("Content " + content);
				
				StringTokenizer tokenizer = new StringTokenizer(content,",");
				
				Organisation org = new Organisation();
				
				//first is ID
				String element = (String)tokenizer.nextElement();
				System.out.println("Element  " + element);
				try{
					org.id = Integer.parseInt(element);
				}catch(Exception e){};
				
				//level
				element = (String)tokenizer.nextElement();
				System.out.println("Element  " + element);
				try{
					org.level = Integer.parseInt(element);
				}catch(Exception e){};
				
				//parent
				element = (String)tokenizer.nextElement();
				System.out.println("Element  " + element);
				try{
					org.parent = Integer.parseInt(element);
				}catch(Exception e){};
				
				//name
				element = (String)tokenizer.nextToken("\",");
				System.out.println("Element  " + element);
				org.name = element;
				
				//can_have_volume_boo.
				element = (String)tokenizer.nextElement();
				System.out.println("Element  " + element);
				try{
					org.canHaveVolumesBool = Integer.parseInt(element);
				}catch(Exception e){};
				
				//short name
				element = (String)tokenizer.nextToken("\",");
				System.out.println("Element  " + element);
				org.shortName = element;
				
				organisations.add(org);
			}
			
		}
	}
	
	
	
	/**
	 * Gets the net page content.
	 *
	 * @param url the url of the page to be retrieved
	 * @return the net page content
	 */
	private String getNetPageContent(String url){
		
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
	        	/*
	        	complexParm.put("limit", 10);
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
	private Map<String,?> getItemShortInfo(String id){
		
		Method method = new Method(METHOD_SLIM_RETRIVE);
		
		method.addParam(id);

		 HttpRequest req = new GatewayRequest(conn, SERVICE, method).send();
	        Object resp;
	        while ( (resp = req.recv()) != null) {
	            System.out.println("Sync Response: " + resp);
	            return (Map<String,?>)resp;
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
	
    private void readIDLFile(){
    	
	   	 String idlFile = "fm_IDL.xml";
	   	 try{
	   		Log.d("debug","Read fm");
	   		IDLParser parser = new IDLParser(context.getAssets().open(idlFile));
	   		parser.parse();
	   	}catch(Exception e){
	   		System.err.println("Error in parsing IDL file " + idlFile + " " + e.getMessage());
	   	};
   	
   }
}
