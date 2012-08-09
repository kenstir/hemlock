package org.evergreen.android.accountAccess.checkout;

import java.util.Date;

import org.androwrapee.db.DatabaseClass;
import org.androwrapee.db.DatabaseField;
import org.androwrapee.db.IdField;

@DatabaseClass
public class NotificationAlert {
	
	public static final String tableName = "notifications";
	
	public static final int NOTIFICATION_INTENT = 123456;
	
	@IdField
	public long id;
	
	@DatabaseField
	public int intent_val;
	
	@DatabaseField
	public Date triggerDate; 
	
	@DatabaseField
	public String message;
	
	//required constructor for DAO
	public NotificationAlert(){
		
	}
	
	public NotificationAlert(int id, int intent_val, Date triggerDate, String message ){
		
		this.id = id;
		this.intent_val = intent_val;
		this.triggerDate = triggerDate;
		this.message = message;
	}
	
	
	@Override
	public String toString() {
		
		return " Notification:[ id: " + id+ "; intent_val: "+intent_val+"; triggerDate : "+triggerDate+"; message: "+message+"]";
	}
}
