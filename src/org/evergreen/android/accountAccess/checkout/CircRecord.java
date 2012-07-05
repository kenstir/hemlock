package org.evergreen.android.accountAccess.checkout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.evergreen.android.globals.GlobalConfigs;
import org.opensrf.util.OSRFObject;

/**
 * This is a wrapper class that get the information out for a circ object 
 * @author daniel
 *
 */
public class CircRecord {

	public static final int MVR_OBJ_TYPE = 1;
	public static final int ACP_OBJ_TYPE = 2;
	public static final int UNDEF_OBJ_TYPE = 0;
	
	public OSRFObject mvr = null;
	
	public OSRFObject acp = null;
	
	public OSRFObject circ = null;
	
	public int circ_info_type = UNDEF_OBJ_TYPE;
	
	public int circ_type;
	
	public static final int OUT = 0;
	public static final int CLAIMS_RETURNED = 1;
	public static final int LONG_OVERDUE = 2;
	public static final int OVERDUE = 3;
	public static final int LOST = 4;
	
	public int circ_id = -1;
	
	private Date circ_due_date = null;
	
	public CircRecord(OSRFObject circ, OSRFObject mvr, OSRFObject acp, int circ_type, int circ_id) {
		
		this.circ = circ;
		
		//one of the acp or mvr will be null this will determine the circ OSRFObject type
		this.acp = acp;
		this.mvr = mvr;
		
		if(this.acp != null)
			this.circ_info_type  = ACP_OBJ_TYPE;
		
		if(this.mvr != null)
			this.circ_info_type = MVR_OBJ_TYPE;
		
		this.circ_type = circ_type;
		this.circ_id = circ_id;
		//parse due date
		parseDate(circ);
	}
	
	public CircRecord(OSRFObject circ,int circ_type, int circ_id){
		this.circ = circ;
		this.circ_type = circ_type;
		this.circ_id = circ_id;
		//parse due date
		parseDate(circ);
	}
	
	public String getAuthor(){
		
		String author = null;
		
		if(this.circ_info_type == MVR_OBJ_TYPE)
			author = mvr.getString("author");
		if(this.circ_info_type == ACP_OBJ_TYPE)
			author = acp.getString("dummy_author");
			
		return author;
	}
	
	
	public String getDueDate(){
		
		return circ_due_date.toLocaleString();
	}
	
	public String getTitle(){
		
		String title = null;
		
		if(this.circ_info_type == MVR_OBJ_TYPE)
			title = mvr.getString("title");
		if(this.circ_info_type == ACP_OBJ_TYPE)
			title = acp.getString("dummy_title");
		
		return title;
	}
	
	public Integer getRenewals(){

		if(circ != null)
			return circ.getInt("renewal_remaining");
			
		return null;
	}
	
	public Integer getTargetCopy(){
		if(circ != null)
			return circ.getInt("target_copy");
		
		return null;
	}
	
	private void parseDate(OSRFObject circ){
		
		this.circ_due_date = GlobalConfigs.parseDate(circ.getString("due_date"));
		System.out.println(this.circ_due_date);

	}
	
}
