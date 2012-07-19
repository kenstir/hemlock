package org.evergreen.android.accountAccess.bookbags;

import java.io.Serializable;
import java.util.ArrayList;

import org.opensrf.util.OSRFObject;

public class BookBag implements Serializable{
	
	public int id;
	
	public String name = null;

	public String description = null;
	
	public Boolean shared = null;
	
	public ArrayList<BookBagItem> items = null;
	
	public BookBag(OSRFObject object){
	
		this.id = object.getInt("id");
		this.name = object.getString("name");
		this.description = object.getString("description");
		this.items = new ArrayList<BookBagItem>();
		
		
		String pub_visible  = object.getString("pub");
		
		if(pub_visible.equals("f"))
			this.shared = false;
		else
			this.shared = true;
	}
	
}
