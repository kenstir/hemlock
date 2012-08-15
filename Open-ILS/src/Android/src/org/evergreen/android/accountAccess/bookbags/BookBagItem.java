package org.evergreen.android.accountAccess.bookbags;

import java.io.Serializable;

import org.evergreen.android.searchCatalog.RecordInfo;
import org.opensrf.util.OSRFObject;

public class BookBagItem implements Serializable {

    public int target_copy;

    public int id;

    public RecordInfo recordInfo;

    public BookBagItem(OSRFObject cbrebi) {

        this.target_copy = cbrebi.getInt("target_biblio_record_entry");
        this.id = cbrebi.getInt("id");

    }
}
