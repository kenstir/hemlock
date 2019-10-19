package org.opensrf.util;

import java.util.Map;
import java.util.HashMap;


/**
 * Generic OpenSRF network-serializable object.  This allows
 * access to object fields.  
 */
public class OSRFObject extends HashMap<String, Object> implements OSRFSerializable {
    
    /** This objects registry */
    private OSRFRegistry registry;

    public OSRFObject() {
    }


    /**
     * Creates a new object with the provided registry
     */
    public OSRFObject(OSRFRegistry reg) {
        this();
        registry = reg;
    }


    /**
     * Creates a new OpenSRF object based on the net class string
     * */
    public OSRFObject(String netClass) {
        this(OSRFRegistry.getRegistry(netClass));
    }


    /**
     * @return This object's registry
     */
    public OSRFRegistry getRegistry() {
        return registry;
    }

    /**
     * Implement get() to fulfill our contract with OSRFSerializable
     */
    public Object get(String field) {
        return super.get(field);
    }

    /** Returns the string value found at the given field */
    public String getString(String field) {
        return getString(field, null);
    }

    public String getString(String field, String dflt) {
        String ret = (String) get(field);
        return (ret != null) ? ret : dflt;
    }

    /** Returns the int value found at the given field */
    public Integer getInt(String field) {
        Object o = get(field);
        if (o == null)
            return null;
        else if (o instanceof String)
            return Integer.parseInt((String) o);
        return (Integer) get(field);
    }
}
