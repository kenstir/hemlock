package org.evergreen.android.accountAccess.holds;

import java.util.Date;

import org.evergreen.android.globals.GlobalConfigs;
import org.opensrf.util.OSRFObject;

public class HoldRecord {

	//metarecord
	public static final int M = 0;
	//record
	public static final int T = 1;
	//volume
	public static final int V = 2;
	//issuance
	public static final int I = 3;
	//copy
	public static final int C = 4;
	//part
	public static final int P = 5;
	
	private Integer requestLibID = null; 
	
	private Integer pickupLibID = null;
	
	public Integer holdType = null;
	//id for target object
	public Integer target = null;
	public Date expire_time = null;
	
	public String title = null;
	
	public String author = null;
	
	
	/* Hold status 
	*  holdStatus == 4 => AVAILABLE
	*  holdStatus == 3 => WAITING
	*  holdStatus <= 3 => TRANSIT 
	*/
	
	//only for P types
	public String part_label = null; 
	
	public Integer status = null;
	
	public Boolean active = null;
	
	public HoldRecord(OSRFObject ahr){
		
		this.target = ahr.getInt("target");
		String type = ahr.getString("hold_type");
		
		if(type.equals("M")){
			holdType = M;
		}else
			if(type.equals("T")){
				holdType = T;
			}else
				if(type.equals("V")){
					holdType = V;
				}else
					if(type.equals("I")){
						holdType = I;
					}else
						if(type.equals("C")){
							holdType = C;
						}else
							if(type.equals("P"))
								holdType = P;
	
		this.expire_time = GlobalConfigs.parseDate(ahr.getString("expire_time"));
						
	}
	//based on status integer field retreive hold status in text
	public String getHoldStatus(){
		
		String holdStatus = "";
		
		if(holdType == 7)
			return "Suspended";
		if(holdType == 4)
			return "Available";
		if(holdType == 3)
			return "Waiting";
		if(holdType < 3)
			return "Transit";
		
		return holdStatus;
	}
	
}
