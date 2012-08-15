package org.evergreen.android.accountAccess;

public class ServerErrorMessage extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 3341617529835568018L;

    public String message;

    public ServerErrorMessage(String message) {
        this.message = message;
    }

}
