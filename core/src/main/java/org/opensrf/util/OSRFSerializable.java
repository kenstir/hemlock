package org.opensrf.util;

/**
 * All network-serializable OpenSRF object must implement this interface.
 */
public interface OSRFSerializable {

    /**
     * Returns the object registry object for the implementing class.
     */
    OSRFRegistry getRegistry();

    /**
     * Returns the object found at the given field
     */
    Object get(String field);
}


