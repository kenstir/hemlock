package org.evergreen.android.accountAccess.bookbags;

import org.opensrf.util.OSRFObject;

public class BookBagItem {

	public int target_copy;

	public BookBagItem(OSRFObject cbrebi){
		
		
		this.target_copy = cbrebi.getInt("target_biblio_record_entry");
	}
}
