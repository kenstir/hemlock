package org.opensrf.util;

import org.evergreen_ils.Api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;
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


    public OSRFObject(Map<String, Object> map) {
        super(map);
    }

    public OSRFObject(String netClass, Map<String, Object> map) {
        super(map);
        registry = OSRFRegistry.getRegistry(netClass);
    }

    /**
     * @return This object's registry
     */
    public OSRFRegistry getRegistry() {
        return registry;
    }

    public @Nullable String getNetClass() {
        return (registry != null) ? registry.getNetClass() : null;
    }

    /**
     * Implement get() to fulfill our contract with OSRFSerializable
     */
    public Object get(@NonNull String field) {
        return super.get(field);
    }

    @Nullable
    public String getString(@NonNull String field) {
        return getString(field, null);
    }

    @Nullable
    public String getString(@NonNull String field, String dflt) {
        String ret = (String) get(field);
        return (ret != null) ? ret : dflt;
    }

    @Nullable
    public Integer getInt(@NonNull String field) {
        Object o = get(field);
        if (o == null)
            return null;
        else if (o instanceof String)
            return Integer.parseInt((String) o);
        return (Integer) get(field);
    }

    @NonNull
    public Boolean getBoolean(@NonNull String field) {
        return Api.parseBoolean(get(field));
    }

    @Nullable
    public OSRFObject getObject(@NonNull String field) {
        Object o = get(field);
        if (o == null) return null;
        if (o instanceof OSRFObject)
            return (OSRFObject) o;
        if (o instanceof HashMap) {
            try {
                HashMap<String, Object> map = (HashMap<String, Object>) o;
                return new OSRFObject(map);
            } catch (ClassCastException e) {
            }
        }
        return null;
    }

    @Nullable
    public Date getDate(@NonNull String field) {
        return Api.parseDate(getString(field));
    }
}
