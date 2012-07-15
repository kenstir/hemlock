package org.evergreen.android.searchCatalog;

import java.util.HashMap;
import java.util.List;

public class CopyInformation {

	
	public Integer org_id;
	
	public String call_number_sufix;
	
	public String copy_location;
	
	public HashMap<String,String> statuses;
	
	
	public CopyInformation(List<String> list){
		
		
		org_id = Integer.parseInt(list.get(0));
		call_number_sufix = list.get(1);
		copy_location = list.get(2);
		
		
	}
	
	
	
}
