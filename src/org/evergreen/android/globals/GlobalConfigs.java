package org.evergreen.android.globals;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.accountAccess.SessionNotFoundException;
import org.evergreen.android.searchCatalog.Organisation;
import org.evergreen.android.searchCatalog.SearchCatalog;
import org.evergreen.android.views.ApplicationPreferences;
import org.open_ils.idl.IDLParser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class GlobalConfigs {

	public static String httpAddress = "http://ulysses.calvin.edu";

	private boolean init = false;

	private static String TAG = "GlobalConfigs";
	
	public static boolean loadedIDL = false;
	
	public static boolean loadedOrgTree = false;
	
	//to parse date from requests
	public static final String datePattern = "yyyy-MM-dd'T'hh:mm:ssZ";

	/** The locale. */
	public String locale = "en-US";  
	
	private static GlobalConfigs globalConfigSingleton = null;
	/** The organisations. */
	public ArrayList<Organisation> organisations;
	
	/** The collections request. */
	private String collectionsRequest = "/opac/common/js/" + locale + "/OrgTree.js";
	
	
	private GlobalConfigs(Context context){
		
		initialize(context);
	}
	
	public static GlobalConfigs getGlobalConfigs(Context context){
		
		if(globalConfigSingleton == null)
		{
			globalConfigSingleton = new GlobalConfigs(context);
		}
		
		return globalConfigSingleton;
	}
	
	/* Initialize function that retrieves IDL file and Orgs file
	 */
	private boolean initialize(Context context){
		
		if(init == false){
			
			init = true;
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			GlobalConfigs.httpAddress = preferences.getString("library_url", "");
			boolean noNetworkAccess = false;
			System.out.println("Check for network conenctivity");
			try{
				Utils.checkNetworkStatus((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE));
				
			}catch(NoNetworkAccessException e){
				noNetworkAccess = true;
			}catch(NoAccessToServer e){

				System.out.println("No access to network");
				Intent preferencesAnctivity = new Intent(context, ApplicationPreferences.class);
				context.startActivity(preferencesAnctivity);
				
				noNetworkAccess = true;
				
			}
			if(!noNetworkAccess){
				loadIDLFile();
				getOrganisations();
			
				getCopyStatusesAvailable((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE));
				
				AccountAccess.setAccountInfo(preferences.getString("username", ""), preferences.getString("password", ""));

				//TODO getorg hidding levels
				//getOrgHiddentDepth();
				
				return true;
			}
			return false;
		}
		return false;
	}
	
	public void loadIDLFile(){
	    	
		   	 String idlFile = "/reports/fm_IDL.xml";
		   	 try{
		   		Log.d("debug","Read fm");
		   		InputStream in_IDL = Utils.getNetInputStream(httpAddress + idlFile);
		   		IDLParser parser = new IDLParser(in_IDL);
		   		parser.parse();
		   	}catch(Exception e){
		   		System.err.println("Error in parsing IDL file " + idlFile + " " + e.getMessage());
		   	};
	   	
		   	loadedIDL = true; 
	   }

	/**
	 * Gets the organisations get's OrgTree.js from server, parses the input and recovers the organisations
	 *
	 * @return the organisations
	 */
	public void getOrganisations(){
		
		String orgFile = null;
		
		
		organisations = new ArrayList<Organisation>();
		
		try{
			//using https: address
			orgFile = Utils.getNetPageContent(httpAddress+collectionsRequest);
			System.out.println("Request org " + httpAddress + collectionsRequest );
		}catch(Exception e){};
	
		if(orgFile != null){
			organisations = new ArrayList<Organisation>();
			
			System.out.println("Page content " + orgFile);
			//in case of wrong file
			if(orgFile.indexOf("=") == -1)
				return;
			
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
				System.out.println("Id  " + element);
				try{
					org.id = Integer.parseInt(element);
				}catch(Exception e){};
				
				//level
				element = (String)tokenizer.nextElement();
				System.out.println("Level   " + element);
				try{
					org.level = Integer.parseInt(element);
				}catch(Exception e){};
				
				//parent
				element = (String)tokenizer.nextElement();
				System.out.println("parent  " + element);
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
			
			ArrayList<Organisation> orgs = new ArrayList<Organisation>();
			
			for(int i=0;i<organisations.size();i++){

				StringBuilder padding = new StringBuilder();
				for(int j=0; j<organisations.get(i).level-1;j++)
					padding.append("  ");
					
				organisations.get(i).padding = padding.toString();
			}
			
			int size = organisations.size();
			int level = 0;
			while(orgs.size() < size){
				
				
				for(int i=0;i<organisations.size();i++){
					Organisation org = organisations.get(i);
					if(level == org.level){
						boolean add = false;
						for(int j=0;j<orgs.size();j++){
							
							if(orgs.get(j).id == org.parent){
								orgs.add(j+1,org);
								add = true;
								Log.d(TAG, "Added " + org.name + " " + org.level);
								break;
							}
						}
						
						if(add == false){
							orgs.add(org);
							Log.d(TAG, "Added " + org.name + " " + org.level);
						}
					}
						
				}
				level ++;
			}
			organisations = orgs;
			
			loadedOrgTree = true;
		}
	}
	
	public void getCopyStatusesAvailable(ConnectivityManager cm){
		
		SearchCatalog search = SearchCatalog.getInstance(cm);
		
		try {
			search.getCopyStatuses();
		} catch (NoNetworkAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoAccessToServer e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	public void getOrgHiddentDepth(){
		
		// logic can be found in the opac_utils.js file in web/opac/common/js
		
		
		for(int i=0; i<organisations.size();i++){
			
			AccountAccess ac = AccountAccess.getAccountAccess();
		
			try{
				Object obj = ac.fetchOrgSettings(organisations.get(i).id, "opac.org_unit_hiding.depth");
			}catch(NoNetworkAccessException e){}
			catch(NoAccessToServer e){}
			catch(SessionNotFoundException e) {// not used here
				}
			
			}

	}
	
	
	
	public static String getStringDate(Date date){
		
		final SimpleDateFormat sdf = new SimpleDateFormat(GlobalConfigs.datePattern);
		  
		return sdf.format(date);
		
	}
	//parse from opac methods query results to Java date
	public static Date parseDate(String dateString){
		
		if(dateString == null)
			return null;
		
		Date date = null;
		final SimpleDateFormat sdf = new SimpleDateFormat(GlobalConfigs.datePattern);
        
			try
	        {
	            date = sdf.parse(dateString);
	            System.out.println(date);
	        } 
	        catch (ParseException e)
	        {
	            e.printStackTrace();
	        }
			
			return date;
	}
	
	public String getOrganizationName(int id){
		
		for(int i=0;i<organisations.size();i++){
			System.out.println("Id " + organisations.get(i).id + " " + i);
			if(organisations.get(i).id == id)
				return organisations.get(i).name;
		}
		
		System.out.println("out here");
		return null;
	}
}
