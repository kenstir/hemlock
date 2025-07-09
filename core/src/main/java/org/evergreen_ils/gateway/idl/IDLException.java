package org.evergreen_ils.gateway.idl;

public class IDLException extends Exception {
    public IDLException(String info) {
        super(info);
    }
    public IDLException(String info, Throwable cause) {
        super(info, cause);
    }
}
