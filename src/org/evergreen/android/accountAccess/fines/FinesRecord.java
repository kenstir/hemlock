package org.evergreen.android.accountAccess.fines;

import java.util.Date;

import org.evergreen.android.globals.GlobalConfigs;
import org.opensrf.util.OSRFObject;

public class FinesRecord {

	public String title;
	
	public String author;
	
	public Date checkoutDate;
	
	public Date dueDate;
	
	public Date dateReturned;
	
	public String balance_owed;
	
	private Date checkin_time;
	
	public FinesRecord(OSRFObject circ, OSRFObject mvr_record, OSRFObject mbts_transaction){
	
		
		title = mvr_record.getString("title");
		author = mvr_record.getString("author");
		
		balance_owed = mbts_transaction.getString("total_owed");
		
		if(circ.get("checkin_time") != null){
			checkin_time = GlobalConfigs.parseDate(circ.getString("checkin_time"));
		}
		else
			checkin_time = null;
		
	}
	
	//if returned or fines still acumulating
	public String getStatus(){
		
		if(checkin_time != null)
			return "returned";
		
		return "fines accruing";
			
	}
}
