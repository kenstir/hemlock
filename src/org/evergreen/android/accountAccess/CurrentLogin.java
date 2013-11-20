package org.evergreen.android.accountAccess;

/**
 * data representing the current logged-in user 
 * @author kenstir
 */
public class CurrentLogin {
    
    protected static CurrentLogin mInstance = null;
    
    protected String mUsername = null;
    protected String mAuthToken = null; 

    protected CurrentLogin() {
    }

    protected static CurrentLogin getInstance() {
        if (mInstance == null)
            mInstance = new CurrentLogin();
        return mInstance;
    }
    
    public static void clear() {
        mInstance = null;
    }

    public static void setAccountInfo(String username, String auth_token) {
        getInstance().mUsername = username;
        getInstance().mAuthToken = auth_token;
    }

    public static String getUsername() {
        return getInstance().mUsername;
    }

    public static String getAuthToken() {
        return getInstance().mAuthToken;
    }
}
