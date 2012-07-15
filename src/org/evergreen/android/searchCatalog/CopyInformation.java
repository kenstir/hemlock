package org.evergreen.android.searchCatalog;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class CopyInformation implements Serializable{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7269334218707079463L;

	public Integer org_id = -1;
	
	public String call_number_sufix;
	
	public String copy_location;
	
	public HashMap<String,String> statuses;

	//the hash from the request method 
	private HashMap<Integer,Integer> hashValCopy;
	
	//global, it is initialized when orgTree and fm_ild is downloaded
	
	public static LinkedHashMap<String,String> availableOrgStatuses;
	
	public LinkedHashMap<String, String> statusInformation = null;
	
	public CopyInformation(List<Object> list){
		
		org_id = Integer.parseInt((String)list.get(0));
		call_number_sufix = (String)list.get(2);
		copy_location = (String)list.get(4);

		hashValCopy = (HashMap<Integer,Integer>)list.get(5);
		
		statusInformation = new LinkedHashMap<String, String>();
		
		Set<Entry<String,String>> set = availableOrgStatuses.entrySet();
		
		Iterator<Entry<String,String>> it = set.iterator();
		
		while(it.hasNext()){
			Entry<String,String> entry = it.next();
			
			if(hashValCopy.containsKey(entry.getKey())){
				statusInformation.put(entry.getValue(), hashValCopy.get(entry.getKey())+"");
				System.out.println("Added " + entry.getKey()+ " " + entry.getValue() + " " + hashValCopy.get(entry.getKey()));
			}
		}
	}
	
	
	
}
