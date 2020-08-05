package org.opensrf.util;

public class JSONUnregisteredClassException extends JSONException {
    public JSONUnregisteredClassException(String netClass) {
        super("Unregistered class: "+netClass);
    }
}
