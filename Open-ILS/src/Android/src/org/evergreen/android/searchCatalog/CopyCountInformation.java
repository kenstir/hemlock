package org.evergreen.android.searchCatalog;

import java.io.Serializable;
import java.util.Map;

public class CopyCountInformation implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 12343248767867L;
    public Integer org_id;
    public Integer count;
    public Integer available;
    public Integer depth;
    public Integer unshadow;

    public CopyCountInformation(Object map) {

        this.org_id = ((Map<String, Integer>) map).get("org_unit");
        this.count = ((Map<String, Integer>) map).get("count");
        this.available = ((Map<String, Integer>) map).get("available");
        this.depth = ((Map<String, Integer>) map).get("depth");
        this.unshadow = ((Map<String, Integer>) map).get("unshadow");

        System.out.println(org_id + " " + available + " " + count);
    }

}
