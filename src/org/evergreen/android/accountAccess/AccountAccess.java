/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be usefull,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 * 
 */
package org.evergreen.android.accountAccess;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.evergreen.android.accountAccess.bookbags.BookBag;
import org.evergreen.android.accountAccess.bookbags.BookBagItem;
import org.evergreen.android.accountAccess.checkout.CircRecord;
import org.evergreen.android.accountAccess.fines.FinesRecord;
import org.evergreen.android.accountAccess.holds.HoldRecord;
import org.evergreen.android.globals.NoAccessToServer;
import org.evergreen.android.globals.NoNetworkAccessException;
import org.evergreen.android.globals.Utils;
import org.evergreen.android.searchCatalog.RecordInfo;
import org.opensrf.Method;
import org.opensrf.net.http.GatewayRequest;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.net.http.HttpRequest;
import org.opensrf.util.OSRFObject;

import android.net.ConnectivityManager;
import android.util.Log;

/**
 * The Class AuthenticateUser. Singleton class
 */
public class AccountAccess {

    // Used for authentication purpose

    /** The SERVICE. */
    public static String SERVICE_AUTH = "open-ils.auth";

    /** The METHOD Auth init. */
    public static String METHOD_AUTH_INIT = "open-ils.auth.authenticate.init";

    /** The METHOD Auth complete. */
    public static String METHOD_AUTH_COMPLETE = "open-ils.auth.authenticate.complete";

    /** The METHOD Auth session retrieve. */
    public static String METHOD_AUTH_SESSION_RETRV = "open-ils.auth.session.retrieve";

    // Used for the Checked out Items Tab

    /** The SERVIC e_ actor. */
    public static String SERVICE_ACTOR = "open-ils.actor";

    /** The SERVIC e_ circ. */
    public static String SERVICE_CIRC = "open-ils.circ";

    /** The SERVIC e_ search. */
    public static String SERVICE_SEARCH = "open-ils.search";

    /** The SERVIC e_ serial. */
    public static String SERVICE_SERIAL = "open-ils.serial";

    /** The SERVIC e_ fielder. */
    public static String SERVICE_FIELDER = "open-ils.fielder";

    /** The METHOD_FETCH_CHECKED_OUT_SUM description : for a given user returns a a structure of circulation objects sorted by out, overdue, lost, claims_returned, long_overdue; A list of ID's returned for each type : "out":[id1,id2,...] @returns: { "out":[id 's],"claims_returned":[],"long_overdue":[],"overdue":[],"lost":[] } */
    public static String METHOD_FETCH_CHECKED_OUT_SUM = "open-ils.actor.user.checked_out";

    /** The METHOD_FETCH_NON_CAT_CIRCS description : for a given user, returns an id-list of non-cataloged circulations that are considered open for now. A circ is open if circ time + circ duration (based on type) is > than now @returns: Array of non-catalogen circ IDs, event or error */
    public static String METHOD_FETCH_NON_CAT_CIRCS = "open-ils.circ.open_non_cataloged_circulation.user";

    /** The METHOD_FETCH_CIRC_BY_ID description : Retrieves a circ object by ID. @returns : "circ" class. Fields of interest : renewal_remaining, due_date */
    public static String METHOD_FETCH_CIRC_BY_ID = "open-ils.circ.retrieve";

    /** The METHOD_FETCH_MODS_FROM_COPY description : used to return info. @returns : mvr class OSRF Object. Fields of interest : title, author */
    public static String METHOD_FETCH_MODS_FROM_COPY = "open-ils.search.biblio.mods_from_copy";

    /** The METHOD_FETCH_COPY description : used to return info for a PRE_CATALOGED object. @returns : acp class OSRF Object. Fields of interest : dummy_title, dummy_author */
    public static String METHOD_FETCH_COPY = "open-ils.search.asset.copy.retrieve";
    
    /** The METHOD_RENEW_CIRC description : used to renew a circulation object. @returnes : acn, acp, circ, mus, mbts */
    public static String METHOD_RENEW_CIRC = "open-ils.circ.renew";

    // Used for Holds Tab

    /** The METHOD_FETCH_HOLDS. @returns: List of "ahr" OSPFObject . Fields of interest : pickup_lib */
    public static String METHOD_FETCH_HOLDS = "open-ils.circ.holds.retrieve";

    /** The METHOD_FETCH_ORG_SETTINGS description : retrieves a setting from the organization unit. @returns : returns the requested value of the setting */
    public static String METHOD_FETCH_ORG_SETTINGS = "open-ils.actor.ou_setting.ancestor_default";

    /** The METHOD_FETCH_MRMODS. */
    // if holdtype == M return mvr OSRFObject
    public static String METHOD_FETCH_MRMODS = "open-ils.search.biblio.metarecord.mods_slim.retrieve";
    // if holdtype == T return mvr OSRFObject
    /** The METHO d_ fetc h_ rmods. */
    public static String METHOD_FETCH_RMODS = "open-ils.search.biblio.record.mods_slim.retrieve";
    // if hold type V
    /** The METHO d_ fetc h_ volume. */
    public static String METHOD_FETCH_VOLUME = "open-ils.search.asset.call_number.retrieve";
    // if hold type I
    /** The METHO d_ fetc h_ issuance. */
    public static String METHOD_FETCH_ISSUANCE = "open-ils.serial.issuance.pub_fleshed.batch.retrieve";

    /** The METHO d_ fetc h_ hol d_ status. */
    public static String METHOD_FETCH_HOLD_STATUS = "open-ils.circ.hold.queue_stats.retrieve";

    /** The METHOD_UPDATE_HOLD description : Updates the specified hold. If session user != hold user then session user must have UPDATE_HOLD permissions @returns : hold_is on success, event or error on failure */
    public static String METHOD_UPDATE_HOLD = "open-ils.circ.hold.update";

    /** The METHOD_CANCEL_HOLD description : Cancels the specified hold. session user != hold user must have CANCEL_HOLD permissions. @returns : 1 on success, event or error on failure */
    public static String METHOD_CANCEL_HOLD = "open-ils.circ.hold.cancel";

    /** The METHOD_VERIFY_HOLD_POSSIBLE description :. @returns : hashmap with "success" : 1 field or */
    public static String METHOD_VERIFY_HOLD_POSSIBLE = "open-ils.circ.title_hold.is_possible";

    /** The METHOD_CREATE_HOLD description :. @returns : hash with messages : "success" : 1 field or */
    public static String METHOD_CREATE_HOLD = "open-ils.circ.holds.create";

    // Used for Fines

    /** The METHODS_FETCH_FINES_SUMMARY description :. @returns: "mous" OSRFObject. fields: balance_owed, total_owed, total_paid */
    public static String METHOD_FETCH_FINES_SUMMARY = "open-ils.actor.user.fines.summary";

    /** The METHOD_FETCH_TRANSACTIONS description: For a given user retrieves a list of fleshed transactions. List of objects, each object is a hash containing : transaction, circ, record @returns : array of objects, must investigate */
    public static String METHOD_FETCH_TRANSACTIONS = "open-ils.actor.user.transactions.have_charge.fleshed";

    /** The METHOD_FETCH_MONEY_BILLING description :. */
    public static String METHOD_FETCH_MONEY_BILLING = "open-ils.circ.money.billing.retrieve.all";

    // Used for book bags
    /** The METHOD_FLESH_CONTAINERS description : Retrieves all un-fleshed buckets by class assigned to a given user VIEW_CONTAINER permissions is requestID != owner ID. @returns : array of "cbreb" OSRFObjects */
    public static String METHOD_FLESH_CONTAINERS = "open-ils.actor.container.retrieve_by_class.authoritative";

    /** The METHOD_FLESH_PUBLIC_CONTAINER description : array of contaoners correspondig to a id. @returns : array of "crebi" OSRF objects (content of bookbag, id's of elements to get more info) */
    public static String METHOD_FLESH_PUBLIC_CONTAINER = "open-ils.actor.container.flesh";

    /** The METHO d_ containe r_ delete. */
    public static String METHOD_CONTAINER_DELETE = "open-ils.actor.container.item.delete";

    /** The METHO d_ containe r_ create. */
    public static String METHOD_CONTAINER_CREATE = "open-ils.actor.container.create";

    /** The METHO d_ containe r_ ite m_ create. */
    public static String METHOD_CONTAINER_ITEM_CREATE = "open-ils.actor.container.item.create";

    /** The METHO d_ containe r_ ful l_ delete. */
    public static String METHOD_CONTAINER_FULL_DELETE = "open-ils.actor.container.full_delete";

    /** The book bags. */
    public ArrayList<BookBag> bookBags = null;

    /** The conn. */
    public HttpConnection conn;

    /** The http address. */
    private String httpAddress = "http://ulysses.calvin.edu";

    /** The TAG. */
    public String TAG = "AuthenticareUser";

    /**
     * The auth token. Sent with every request that needs authentication
     * */
    public String authToken = null;

    /** The cm. */
    private ConnectivityManager cm;

    /** The auth time. */
    private Integer authTime = null;

    /** The user id. */
    private Integer userID = null;
    // for demo purpose
    /** The user name. */
    public static String userName = "daniel";

    /** The password. */
    public static String password = "demo123";

    /** The account access. */
    private static AccountAccess accountAccess = null;

    /**
     * Instantiates a new authenticate user.
     *
     * @param httpAddress the http address
     * @param cm the cm
     */
    private AccountAccess(String httpAddress, ConnectivityManager cm) {

        this.httpAddress = httpAddress;
        this.cm = cm;

        try {
            // configure the connection

            System.out.println("Connection with " + httpAddress);
            conn = new HttpConnection(httpAddress + "/osrf-gateway-v1");

        } catch (Exception e) {
            System.err.println("Exception in establishing connection "
                    + e.getMessage());
        }

    }

    /**
     * Checks if is authenticated.
     *
     * @return true, if is authenticated
     */
    public boolean isAuthenticated() {

        if (authToken != null)
            return true;

        return false;
    }

    /**
     * Gets the account access.
     *
     * @param httpAddress the http address
     * @param cm the cm
     * @return the account access
     */
    public static AccountAccess getAccountAccess(String httpAddress,
            ConnectivityManager cm) {

        if (accountAccess == null) {
            accountAccess = new AccountAccess(httpAddress, cm);
        }
        System.out.println(" Addresses " + httpAddress + " "
                + accountAccess.httpAddress);
        if (!httpAddress.equals(accountAccess.httpAddress))
            accountAccess.updateHttpAddress(httpAddress);

        return accountAccess;
    }

    // the object must be initialized before
    /**
     * Gets the account access.
     *
     * @return the account access
     */
    public static AccountAccess getAccountAccess() {

        if (accountAccess != null) {
            return accountAccess;
        }

        return null;
    }

    /*
     * Change the Http conn to a new library address
     */
    /**
     * Update http address.
     *
     * @param httpAddress the http address
     */
    public void updateHttpAddress(String httpAddress) {
        System.out.println("update http address of account access to "
                + httpAddress);
        try {
            // configure the connection
            this.httpAddress = httpAddress;
            System.out.println("Connection with " + httpAddress);
            conn = new HttpConnection(httpAddress + "/osrf-gateway-v1");

        } catch (Exception e) {
            System.err.println("Exception in establishing connection "
                    + e.getMessage());
        }
    }

    /**
     * Md5.
     * 
     * @param s
     *            the s
     * @return the string
     */
    private String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) {
                    // could use a for loop, but we're only dealing with a
                    // single byte
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Sets the account info.
     *
     * @param username the username
     * @param password the password
     */
    public static void setAccountInfo(String username, String password) {

        AccountAccess.userName = username;
        AccountAccess.password = password;

    }

    /**
     * Authenticate.
     *
     * @return true, if successful
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public boolean authenticate() throws NoNetworkAccessException,
            NoAccessToServer {

        String seed = authenticateInit();

        return authenticateComplete(seed);
    }

    /**
     * Gets the account summary.
     * 
     * @return the account summary
     */
    public OSRFObject getAccountSummary() {

        Method method = new Method(METHOD_AUTH_SESSION_RETRV);

        method.addParam(authToken);

        // sync request
        HttpRequest req = new GatewayRequest(conn, SERVICE_AUTH, method).send();
        Object resp;

        while ((resp = req.recv()) != null) {
            System.out.println("Sync Response: " + resp);
            OSRFObject au = (OSRFObject) resp;
            userID = au.getInt("id");
            System.out.println("User Id " + userID);

            return au;
        }
        return null;
    }

    /**
     * Authenticate init.
     *
     * @return seed for phase 2 of login
     * @throws NoAccessToServer the no access to server
     * @throws NoNetworkAccessException the no network access exception
     */
    private String authenticateInit() throws NoAccessToServer,
            NoNetworkAccessException {

        String seed = null;

        System.out.println("Send request to " + httpAddress);
        Object resp = (Object) Utils.doRequest(conn, SERVICE_AUTH,
                METHOD_AUTH_INIT, cm, new Object[] { userName });
        if (resp != null)
            seed = resp.toString();

        System.out.println("Seed " + seed);

        return seed;
    }

    /**
     * Authenticate complete. Phase 2 of login process Application send's
     * username and hash to confirm login
     *
     * @param seed the seed
     * @return true, if successful
     * @throws NoAccessToServer the no access to server
     * @throws NoNetworkAccessException the no network access exception
     * @returns bollean if auth was ok
     */
    private boolean authenticateComplete(String seed) throws NoAccessToServer,
            NoNetworkAccessException {

        // calculate hash to pass to server for authentication process phase 2
        // seed = "b18a9063e0c6f49dfe7a854cc6ab5775";
        String hash = md5(seed + md5(password));
        System.out.println("Hash " + hash);

        HashMap<String, String> complexParam = new HashMap<String, String>();
        // TODO parameter for user login
        complexParam.put("type", "opac");

        complexParam.put("username", userName);
        complexParam.put("password", hash);

        System.out.println("Password " + password);
        System.out.println("Compelx param " + complexParam);

        Object resp = Utils.doRequest(conn, SERVICE_AUTH, METHOD_AUTH_COMPLETE,
                cm, new Object[] { complexParam });
        if (resp == null)
            return false;

        String queryResult = ((Map<String, String>) resp).get("textcode");

        System.out.println("Result " + queryResult);

        if (queryResult.equals("SUCCESS")) {
            Object payload = ((Map<String, String>) resp).get("payload");
            accountAccess.authToken = ((Map<String, String>) payload)
                    .get("authtoken");
            try {
                System.out.println(authToken);
                accountAccess.authTime = ((Map<String, Integer>) payload)
                        .get("authtime");

            } catch (Exception e) {
                System.err.println("Error in parsing authtime "
                        + e.getMessage());
            }

            // get user ID
            try {
                accountAccess.getAccountSummary();
            } catch (Exception e) {
                Log.d(TAG,
                        "Error in retrieving account info, this is normal if it is before IDL load");
            }

            return true;
        }

        return false;

    }

    // ------------------------Checked Out Items Section
    // -------------------------//

    /**
     * Gets the items checked out.
     *
     * @return the items checked out
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public ArrayList<CircRecord> getItemsCheckedOut()
            throws SessionNotFoundException, NoNetworkAccessException,
            NoAccessToServer {

        ArrayList<CircRecord> circRecords = new ArrayList<CircRecord>();
        /*
         * ArrayList<OSRFObject> long_overdue = new ArrayList<OSRFObject>();
         * ArrayList<OSRFObject> claims_returned = new ArrayList<OSRFObject>();
         * ArrayList<OSRFObject> lost = new ArrayList<OSRFObject>();
         * ArrayList<OSRFObject> out = new ArrayList<OSRFObject>();
         * ArrayList<OSRFObject> overdue = new ArrayList<OSRFObject>();
         */

        // fetch ids
        List<String> long_overdue_id;
        List<String> overdue_id;
        List<String> claims_returned_id;
        List<String> lost_id;
        List<String> out_id;

        Object resp = Utils.doRequest(conn, SERVICE_ACTOR,
                METHOD_FETCH_CHECKED_OUT_SUM, authToken, cm, new Object[] {
                        authToken, userID });

        long_overdue_id = (List<String>) ((Map<String, ?>) resp)
                .get("long_overdue");
        claims_returned_id = (List<String>) ((Map<String, ?>) resp)
                .get("claims_returned");
        lost_id = (List<String>) ((Map<String, ?>) resp).get("lost");
        out_id = (List<String>) ((Map<String, ?>) resp).get("out");
        overdue_id = (List<String>) ((Map<String, ?>) resp).get("overdue");

        // get all the record circ info
        if (out_id != null)
            for (int i = 0; i < out_id.size(); i++) {
                // get circ
                OSRFObject circ = retrieveCircRecord(out_id.get(i));
                CircRecord circRecord = new CircRecord(circ, CircRecord.OUT,
                        Integer.parseInt(out_id.get(i)));
                // get info
                fetchInfoForCheckedOutItem(circ.getInt("target_copy"),
                        circRecord);
                circRecords.add(circRecord);

                // System.out.println(out.get(i).get("target_copy"));
                // fetchInfoForCheckedOutItem(out.get(i).get("target_copy")+"");
            }

        if (overdue_id != null)
            for (int i = 0; i < overdue_id.size(); i++) {
                // get circ
                OSRFObject circ = retrieveCircRecord(overdue_id.get(i));
                CircRecord circRecord = new CircRecord(circ,
                        CircRecord.OVERDUE, Integer.parseInt(overdue_id.get(i)));
                // fetch info
                fetchInfoForCheckedOutItem(circ.getInt("target_copy"),
                        circRecord);
                circRecords.add(circRecord);

            }
        // TODO are we using this too? In the opac they are not used
        /*
         * for(int i=0;i<lost_id.size();i++){ //System.out.println(out.get(i));
         * lost.add(retrieveCircRecord(lost_id.get(i))); } for(int
         * i=0;i<claims_returned.size();i++){ //System.out.println(out.get(i));
         * claims_returned.add(retrieveCircRecord(claims_returned_id.get(i))); }
         * for(int i=0;i<long_overdue_id.size();i++){
         * //System.out.println(out.get(i));
         * long_overdue.add(retrieveCircRecord(long_overdue_id.get(i))); }
         */

        return circRecords;
    }

    /*
     * Retrieves the Circ record
     * 
     * @param : target_copy from circ
     * 
     * @returns : "circ" OSRFObject
     */
    /**
     * Retrieve circ record.
     *
     * @param id the id
     * @return the oSRF object
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    private OSRFObject retrieveCircRecord(String id)
            throws SessionNotFoundException, NoNetworkAccessException,
            NoAccessToServer {

        OSRFObject circ = (OSRFObject) Utils.doRequest(conn, SERVICE_CIRC,
                METHOD_FETCH_CIRC_BY_ID, authToken, cm, new Object[] {
                        authToken, id });
        return circ;
    }

    /*
     * Fetch info for Checked Out Items It uses two methods :
     * open-ils.search.biblio.mods_from_copy or in case of pre-cataloged records
     * it uses open-ils.search.asset.copy.retriev Usefull info : title and
     * author (for acp : dummy_title, dummy_author)
     */
    /**
     * Fetch info for checked out item.
     *
     * @param target_copy the target_copy
     * @param circRecord the circ record
     * @return the oSRF object
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    private OSRFObject fetchInfoForCheckedOutItem(Integer target_copy,
            CircRecord circRecord) throws NoNetworkAccessException,
            NoAccessToServer {

        if (target_copy == null)
            return null;

        OSRFObject result;
        System.out.println("Mods from copy");
        OSRFObject info_mvr = fetchModsFromCopy(target_copy);
        // if title or author not inserted, request acp with copy_target
        result = info_mvr;
        OSRFObject info_acp = null;

        // the logic to establish mvr or acp is copied from the opac
        if (info_mvr.getString("title") == null
                || info_mvr.getString("author") == null) {
            System.out.println("Asset");
            info_acp = fetchAssetCopy(target_copy);
            result = info_acp;
            circRecord.acp = info_acp;
            circRecord.circ_info_type = CircRecord.ACP_OBJ_TYPE;
        } else {
            circRecord.mvr = info_mvr;
            circRecord.circ_info_type = CircRecord.MVR_OBJ_TYPE;
        }
        return result;
    }

    /**
     * Fetch mods from copy.
     *
     * @param target_copy the target_copy
     * @return the oSRF object
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    private OSRFObject fetchModsFromCopy(Integer target_copy)
            throws NoNetworkAccessException, NoAccessToServer {

        // sync request
        OSRFObject mvr = (OSRFObject) Utils.doRequest(conn, SERVICE_SEARCH,
                METHOD_FETCH_MODS_FROM_COPY, cm, new Object[] { target_copy });

        return mvr;
    }

    /**
     * Fetch asset copy.
     *
     * @param target_copy the target_copy
     * @return the oSRF object
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    private OSRFObject fetchAssetCopy(Integer target_copy)
            throws NoNetworkAccessException, NoAccessToServer {

        OSRFObject acp = (OSRFObject) Utils.doRequest(conn, SERVICE_SEARCH,
                METHOD_FETCH_COPY, cm, new Object[] { target_copy });

        return acp;
    }

    /*
     * Method used to renew a circulation record based on target_copy_id Returns
     * many objects, don't think they are needed
     */
    /**
     * Renew circ.
     *
     * @param target_copy the target_copy
     * @throws MaxRenewalsException the max renewals exception
     * @throws ServerErrorMessage the server error message
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public void renewCirc(Integer target_copy) throws MaxRenewalsException,
            ServerErrorMessage, SessionNotFoundException,
            NoNetworkAccessException, NoAccessToServer {

        HashMap<String, Integer> complexParam = new HashMap<String, Integer>();
        complexParam.put("patron", this.userID);
        complexParam.put("copyid", target_copy);
        complexParam.put("opac_renewal", 1);

        Object a_lot = (Object) Utils.doRequest(conn, SERVICE_CIRC,
                METHOD_RENEW_CIRC, authToken, cm, new Object[] { authToken,
                        complexParam });

        Map<String, String> resp = (Map<String, String>) a_lot;

        if (resp.get("textcode") != null) {
            if (resp.get("textcode").equals("MAX_RENEWALS_REACHED"))
                throw new MaxRenewalsException();
            throw new ServerErrorMessage(resp.get("desc").toString());
        }

    }

    // ------------------------Holds Section
    // --------------------------------------//

    /**
     * Fetch org settings.
     *
     * @param org_id the org_id
     * @param setting the setting
     * @return the object
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public Object fetchOrgSettings(Integer org_id, String setting)
            throws SessionNotFoundException, NoNetworkAccessException,
            NoAccessToServer {

        OSRFObject response = (OSRFObject) Utils
                .doRequest(conn, SERVICE_ACTOR, METHOD_FETCH_ORG_SETTINGS, cm,
                        new Object[] { org_id, setting });
        return response;

    }

    /**
     * Gets the holds.
     *
     * @return the holds
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public List<HoldRecord> getHolds() throws SessionNotFoundException,
            NoNetworkAccessException, NoAccessToServer {

        ArrayList<HoldRecord> holds = new ArrayList<HoldRecord>();

        // fields of interest : expire_time
        List<OSRFObject> listHoldsAhr = null;

        Object resp = Utils.doRequest(conn, SERVICE_CIRC, METHOD_FETCH_HOLDS,
                authToken, cm, new Object[] { authToken, userID });

        listHoldsAhr = (List<OSRFObject>) resp;

        for (int i = 0; i < listHoldsAhr.size(); i++) {
            // create hold item
            HoldRecord hold = new HoldRecord(listHoldsAhr.get(i));
            // get title
            fetchHoldTitleInfo(listHoldsAhr.get(i), hold);

            // get status
            fetchHoldStatus(listHoldsAhr.get(i), hold);

            holds.add(hold);
        }
        return holds;
    }

    /*
     * hold target type : M - metarecord T - record V - volume I - issuance C -
     * copy P - pat
     */

    /**
     * Fetch hold title info.
     *
     * @param holdArhObject the hold arh object
     * @param hold the hold
     * @return the object
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    private Object fetchHoldTitleInfo(OSRFObject holdArhObject, HoldRecord hold)
            throws NoNetworkAccessException, NoAccessToServer {

        String holdType = (String) holdArhObject.get("hold_type");

        String method = null;

        Object response;
        OSRFObject holdInfo = null;
        if (holdType.equals("T") || holdType.equals("M")) {

            if (holdType.equals("M"))
                method = METHOD_FETCH_MRMODS;
            if (holdType.equals("T"))
                method = METHOD_FETCH_RMODS;
            System.out.println();
            holdInfo = (OSRFObject) Utils.doRequest(conn, SERVICE_SEARCH,
                    method, cm, new Object[] { holdArhObject.get("target") });

            // System.out.println("Hold here " + holdInfo);
            hold.title = ((OSRFObject) holdInfo).getString("title");
            hold.author = ((OSRFObject) holdInfo).getString("author");
            hold.recordInfo = new RecordInfo((OSRFObject) holdInfo);
            try {
                hold.types_of_resource = ((List<Object>) holdInfo
                        .get("types_of_resource")).get(0).toString();
            } catch (Exception e) {
                System.err.println("Can't get types of resurce type"
                        + e.getMessage());
            }
            ;
        } else {
            // multiple objects per hold ????
            holdInfo = holdFetchObjects(holdArhObject, hold);

        }
        return holdInfo;
    }

    /**
     * Hold fetch objects.
     *
     * @param hold the hold
     * @param holdObj the hold obj
     * @return the oSRF object
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    private OSRFObject holdFetchObjects(OSRFObject hold, HoldRecord holdObj)
            throws NoNetworkAccessException, NoAccessToServer {

        String type = (String) hold.get("hold_type");

        System.out.println("Hold Type " + type);
        if (type.equals("C")) {

            /*
             * steps asset.copy'->'asset.call_number'->'biblio.record_entry' or,
             * in IDL ids, acp->acn->bre
             */

            // fetch_copy
            OSRFObject copyObject = fetchAssetCopy(hold.getInt("target"));
            // fetch_volume from copyObject.call_number field
            Integer call_number = copyObject.getInt("call_number");

            if (call_number != null) {

                OSRFObject volume = (OSRFObject) Utils.doRequest(conn,
                        SERVICE_SEARCH, METHOD_FETCH_VOLUME, cm,
                        new Object[] { copyObject.getInt("call_number") });
                // in volume object : record
                Integer record = volume.getInt("record");

                // part label
                holdObj.part_label = volume.getString("label");

                System.out.println("Record " + record);
                OSRFObject holdInfo = (OSRFObject) Utils.doRequest(conn,
                        SERVICE_SEARCH, METHOD_FETCH_RMODS, cm,
                        new Object[] { record });

                holdObj.title = holdInfo.getString("title");
                holdObj.author = holdInfo.getString("author");
                holdObj.recordInfo = new RecordInfo((OSRFObject) holdInfo);
                try {
                    holdObj.types_of_resource = ((List<Object>) holdInfo
                            .get("types_of_resource")).get(0).toString();
                } catch (Exception e) {
                    System.err.println("Can't get types of resurce type"
                            + e.getMessage());
                }
                ;
            }

            return copyObject;
        } else if (type.equals("V")) {
            // must test

            // fetch_volume
            OSRFObject volume = (OSRFObject) Utils.doRequest(conn,
                    SERVICE_SEARCH, METHOD_FETCH_VOLUME, cm,
                    new Object[] { hold.getInt("target") });
            // in volume object : record

            // in volume object : record
            Integer record = volume.getInt("record");

            // part label
            holdObj.part_label = volume.getString("label");

            System.out.println("Record " + record);
            OSRFObject holdInfo = (OSRFObject) Utils.doRequest(conn,
                    SERVICE_SEARCH, METHOD_FETCH_RMODS, cm,
                    new Object[] { record });

            holdObj.title = holdInfo.getString("title");
            holdObj.author = holdInfo.getString("author");
            holdObj.recordInfo = new RecordInfo((OSRFObject) holdInfo);
            try {
                holdObj.types_of_resource = ((List<Object>) holdInfo
                        .get("types_of_resource")).get(0).toString();
            } catch (Exception e) {
                System.err.println("Can't get types of resurce type"
                        + e.getMessage());
            }
            ;
        } else if (type.equals("I")) {
            OSRFObject issuance = (OSRFObject) Utils.doRequest(conn,
                    SERVICE_SERIAL, METHOD_FETCH_ISSUANCE, cm,
                    new Object[] { hold.getInt("target") });
            // TODO

        } else if (type.equals("P")) {
            HashMap<String, Object> param = new HashMap<String, Object>();

            param.put("cache", 1);

            ArrayList<String> fieldsList = new ArrayList<String>();
            fieldsList.add("label");
            fieldsList.add("record");

            param.put("fields", fieldsList);
            HashMap<String, Integer> queryParam = new HashMap<String, Integer>();
            // PART_ID use "target field in hold"
            queryParam.put("id", hold.getInt("target"));
            param.put("query", queryParam);

            // returns [{record:id, label=part label}]

            List<Object> part = (List<Object>) Utils.doRequest(conn,
                    SERVICE_FIELDER, "open-ils.fielder.bmp.atomic", cm,
                    new Object[] { param });

            Map<String, ?> partObj = (Map<String, ?>) part.get(0);

            Integer recordID = (Integer) partObj.get("record");
            String part_label = (String) partObj.get("label");

            OSRFObject holdInfo = (OSRFObject) Utils.doRequest(conn,
                    SERVICE_SEARCH, METHOD_FETCH_RMODS, cm,
                    new Object[] { recordID });

            holdObj.part_label = part_label;
            holdObj.title = holdInfo.getString("title");
            holdObj.author = holdInfo.getString("author");
            holdObj.recordInfo = new RecordInfo((OSRFObject) holdInfo);
            try {
                holdObj.types_of_resource = ((List<Object>) holdInfo
                        .get("types_of_resource")).get(0).toString();
            } catch (Exception e) {
                System.err.println("Can't get types of resurce type"
                        + e.getMessage());
            }
            ;
        }

        return null;
    }

    /**
     * Fetch hold status.
     *
     * @param hold the hold
     * @param holdObj the hold obj
     * @return the object
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public Object fetchHoldStatus(OSRFObject hold, HoldRecord holdObj)
            throws SessionNotFoundException, NoNetworkAccessException,
            NoAccessToServer {

        Integer hold_id = hold.getInt("id");
        // MAP : potential_copies, status, total_holds, queue_position,
        // estimated_wait
        Object hold_status = Utils.doRequest(conn, SERVICE_CIRC,
                METHOD_FETCH_HOLD_STATUS, authToken, cm, new Object[] {
                        authToken, hold_id });

        // get status
        holdObj.status = ((Map<String, Integer>) hold_status).get("status");
        return hold_status;
    }

    /**
     * Cancel hold.
     *
     * @param hold the hold
     * @return true, if successful
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public boolean cancelHold(OSRFObject hold) throws SessionNotFoundException,
            NoNetworkAccessException, NoAccessToServer {

        Integer hold_id = hold.getInt("id");

        Object response = Utils.doRequest(conn, SERVICE_CIRC,
                METHOD_CANCEL_HOLD, authToken, cm, new Object[] { authToken,
                        hold_id });

        // delete successful
        if (response.toString().equals("1"))
            return true;

        return false;

    }

    /**
     * Update hold.
     *
     * @param ahr the ahr
     * @param pickup_lib the pickup_lib
     * @param email_notify the email_notify
     * @param phone_notify the phone_notify
     * @param phone the phone
     * @param suspendHold the suspend hold
     * @param expire_time the expire_time
     * @param thaw_date the thaw_date
     * @return the object
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public Object updateHold(OSRFObject ahr, Integer pickup_lib,
            boolean email_notify, boolean phone_notify, String phone,
            boolean suspendHold, String expire_time, String thaw_date)
            throws SessionNotFoundException, NoNetworkAccessException,
            NoAccessToServer {
        // TODO verify that object is correct passed to the server

        ahr.put("pickup_lib", pickup_lib); // pick-up lib
        ahr.put("phone_notify", phone);
        ahr.put("email_notify", email_notify);
        ahr.put("expire_time", expire_time);
        // frozen set, what this means ?
        ahr.put("frozen", suspendHold);
        // only if it is frozen
        ahr.put("thaw_date", thaw_date);

        Object response = Utils.doRequest(conn, SERVICE_CIRC,
                METHOD_UPDATE_HOLD, authToken, cm, new Object[] { authToken,
                        ahr });

        return response;
    }

    /**
     * Creates the hold.
     *
     * @param recordID the record id
     * @param pickup_lib the pickup_lib
     * @param email_notify the email_notify
     * @param phone_notify the phone_notify
     * @param phone the phone
     * @param suspendHold the suspend hold
     * @param expire_time the expire_time
     * @param thaw_date the thaw_date
     * @return the string[]
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public String[] createHold(Integer recordID, Integer pickup_lib,
            boolean email_notify, boolean phone_notify, String phone,
            boolean suspendHold, String expire_time, String thaw_date)
            throws SessionNotFoundException, NoNetworkAccessException,
            NoAccessToServer {

        OSRFObject ahr = new OSRFObject("ahr");
        ahr.put("target", recordID);
        ahr.put("usr", userID);
        ahr.put("requestor", userID);

        // TODO
        // only gold type 'T' for now
        ahr.put("hold_type", "T");
        ahr.put("pickup_lib", pickup_lib); // pick-up lib
        ahr.put("phone_notify", phone);
        ahr.put("email_notify", email_notify);
        ahr.put("expire_time", expire_time);
        // frozen set, what this means ?
        ahr.put("frozen", suspendHold);
        // only if it is frozen
        ahr.put("thaw_date", thaw_date);

        // extra parameters (not mandatory for hold creation)

        Object response = Utils.doRequest(conn, SERVICE_CIRC,
                METHOD_CREATE_HOLD, authToken, cm, new Object[] { authToken,
                        ahr });

        String[] resp = new String[3];
        // if we can get hold ID then we return true
        try {

            Integer id = Integer.parseInt(response.toString());
            if (id > -1)
                resp[0] = "true";

        } catch (Exception e) {

            List<?> respErrorMessage = (List<?>) response;

            Object map = respErrorMessage.get(0);
            resp[0] = "false";

            resp[1] = ((Map<String, String>) map).get("textcode");
            resp[2] = ((Map<String, String>) map).get("desc");
        }
        ;

        System.out.println("Result " + resp[1] + " " + resp[2]);

        // else we return false
        return resp;
    }

    // ?? return boolean
    /**
     * Checks if is hold possible.
     *
     * @param pickup_lib the pickup_lib
     * @param recordID the record id
     * @return the object
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public Object isHoldPossible(Integer pickup_lib, Integer recordID)
            throws SessionNotFoundException, NoNetworkAccessException,
            NoAccessToServer {

        HashMap<String, Integer> mapAsk = getHoldPreCreateInfo(recordID,
                pickup_lib);
        mapAsk.put("pickup_lib", pickup_lib);
        mapAsk.put("hold_type", null);
        mapAsk.put("patronid", userID);
        mapAsk.put("volume_id", null);
        mapAsk.put("issuanceid", null);
        mapAsk.put("copy_id", null);
        mapAsk.put("depth", 0);
        mapAsk.put("part_id", null);
        mapAsk.put("holdable_formats", null);
        // {"titleid":63,"mrid":60,"volume_id":null,"issuanceid":null,"copy_id":null,"hold_type":"T","holdable_formats":null,
        // "patronid":2,"depth":0,"pickup_lib":"8","partid":null}

        Object response = Utils.doRequest(conn, SERVICE_CIRC,
                METHOD_VERIFY_HOLD_POSSIBLE, authToken, cm, new Object[] {
                        authToken, mapAsk });

        return response;
    }

    // return
    /**
     * Gets the hold pre create info.
     *
     * @param recordID the record id
     * @param pickup_lib the pickup_lib
     * @return the hold pre create info
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public HashMap<String, Integer> getHoldPreCreateInfo(Integer recordID,
            Integer pickup_lib) throws NoNetworkAccessException,
            NoAccessToServer {

        HashMap<String, Integer> param = new HashMap<String, Integer>();

        param.put("pickup_lib", pickup_lib);
        param.put("record", recordID);

        Map<String, ?> response = (Map<String, ?>) Utils.doRequest(conn,
                SERVICE_SEARCH,
                "open-ils.search.metabib.record_to_descriptors", cm,
                new Object[] { param });

        Object obj = response.get("metarecord");
        System.out.println(obj);
        Integer metarecordID = Integer.parseInt(obj.toString());

        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("titleid", recordID);
        map.put("mrid", metarecordID);

        return map;
        /*
         * Methods to get necessary info on hold
         * open-ils.search.metabib.record_to_descriptors
         * 
         * open-ils.search.biblio.record_hold_parts
         */
    }

    // ----------------------------Fines
    // Summary------------------------------------//

    /**
     * Gets the fines summary.
     *
     * @return the fines summary
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public float[] getFinesSummary() throws SessionNotFoundException,
            NoNetworkAccessException, NoAccessToServer {

        // mous object
        OSRFObject finesSummary = (OSRFObject) Utils.doRequest(conn,
                SERVICE_ACTOR, METHOD_FETCH_FINES_SUMMARY, authToken, cm,
                new Object[] { authToken, userID });

        float fines[] = new float[3];
        try {
            fines[0] = Float.parseFloat(finesSummary.getString("total_owed"));
            fines[1] = Float.parseFloat(finesSummary.getString("total_paid"));
            fines[2] = Float.parseFloat(finesSummary.getString("balance_owed"));
        } catch (Exception e) {
            System.err.println("Exception in parsing fines " + e.getMessage());
        }

        return fines;
    }

    /**
     * Gets the transactions.
     *
     * @return the transactions
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public ArrayList<FinesRecord> getTransactions()
            throws SessionNotFoundException, NoNetworkAccessException,
            NoAccessToServer {

        ArrayList<FinesRecord> finesRecords = new ArrayList<FinesRecord>();

        Object transactions = Utils.doRequest(conn, SERVICE_ACTOR,
                METHOD_FETCH_TRANSACTIONS, authToken, cm, new Object[] {
                        authToken, userID });

        // get Array

        List<Map<String, OSRFObject>> list = (List<Map<String, OSRFObject>>) transactions;

        for (int i = 0; i < list.size(); i++) {

            Map<String, OSRFObject> item = list.get(i);

            FinesRecord record = new FinesRecord(item.get("circ"),
                    item.get("record"), item.get("transaction"));
            finesRecords.add(record);
        }

        return finesRecords;
    }

    // ---------------------------------------Book
    // bags-----------------------------------//

    /**
     * Gets the bookbags.
     *
     * @return the bookbags
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public ArrayList<BookBag> getBookbags() throws SessionNotFoundException,
            NoNetworkAccessException, NoAccessToServer {

        Object response = Utils.doRequest(conn, SERVICE_ACTOR,
                METHOD_FLESH_CONTAINERS, authToken, cm, new Object[] {
                        authToken, userID, "biblio", "bookbag" });

        List<OSRFObject> bookbags = (List<OSRFObject>) response;

        ArrayList<BookBag> bookBagObj = new ArrayList<BookBag>();
        // in order to refresh bookbags
        this.bookBags = bookBagObj;

        if (bookbags == null)
            return bookBagObj;

        for (int i = 0; i < bookbags.size(); i++) {

            BookBag bag = new BookBag(bookbags.get(i));
            getBookbagContent(bag, bookbags.get(i).getInt("id"));

            bookBagObj.add(bag);
        }
        return bookBagObj;
    }

    /**
     * Gets the bookbag content.
     *
     * @param bag the bag
     * @param bookbagID the bookbag id
     * @return the bookbag content
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    private Object getBookbagContent(BookBag bag, Integer bookbagID)
            throws SessionNotFoundException, NoNetworkAccessException,
            NoAccessToServer {

        Map<String, ?> map = (Map<String, ?>) Utils.doRequest(conn,
                SERVICE_ACTOR, METHOD_FLESH_PUBLIC_CONTAINER, authToken, cm,
                new Object[] { authToken, "biblio", bookbagID });
        
        List<OSRFObject> items  = new ArrayList<OSRFObject>();
        
        try{
            items = (List<OSRFObject>) map.get("items");
    
            for (int i = 0; i < items.size(); i++) {
    
                BookBagItem bookBagItem = new BookBagItem(items.get(i));
    
                bag.items.add(bookBagItem);
        }

        }catch(Exception e){};
        
        return items;
    }

    /**
     * Removes the bookbag item.
     *
     * @param id the id
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public void removeBookbagItem(Integer id) throws SessionNotFoundException,
            NoNetworkAccessException, NoAccessToServer {

        removeContainer("biblio", id);

    }

    /**
     * Creates the bookbag.
     *
     * @param name the name
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public void createBookbag(String name) throws SessionNotFoundException,
            NoNetworkAccessException, NoAccessToServer {

        OSRFObject cbreb = new OSRFObject("cbreb");
        cbreb.put("btype", "bookbag");
        cbreb.put("name", name);
        cbreb.put("pub", false);
        cbreb.put("owner", userID);

        createContainer("biblio", cbreb);
    }

    /**
     * Delete book bag.
     *
     * @param id the id
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    public void deleteBookBag(Integer id) throws SessionNotFoundException,
            NoNetworkAccessException, NoAccessToServer {

        Object response = Utils.doRequest(conn, SERVICE_ACTOR,
                METHOD_CONTAINER_FULL_DELETE, authToken, cm, new Object[] {
                        authToken, "biblio", id });
    }

    /**
     * Adds the record to book bag.
     *
     * @param record_id the record_id
     * @param bookbag_id the bookbag_id
     * @throws SessionNotFoundException the session not found exception
     * @throws NoAccessToServer the no access to server
     * @throws NoNetworkAccessException the no network access exception
     */
    public void addRecordToBookBag(Integer record_id, Integer bookbag_id)
            throws SessionNotFoundException, NoAccessToServer,
            NoNetworkAccessException {

        OSRFObject cbrebi = new OSRFObject("cbrebi");
        cbrebi.put("bucket", bookbag_id);
        cbrebi.put("target_biblio_record_entry", record_id);
        cbrebi.put("id", null);

        Object response = Utils.doRequest(conn, SERVICE_ACTOR,
                METHOD_CONTAINER_ITEM_CREATE, authToken, cm, new Object[] {
                        authToken, "biblio", cbrebi });
    }

    /**
     * Removes the container.
     *
     * @param container the container
     * @param id the id
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    private void removeContainer(String container, Integer id)
            throws SessionNotFoundException, NoNetworkAccessException,
            NoAccessToServer {

        Object response = Utils.doRequest(conn, SERVICE_ACTOR,
                METHOD_CONTAINER_DELETE, authToken, cm, new Object[] {
                        authToken, container, id });
    }

    /**
     * Creates the container.
     *
     * @param container the container
     * @param parameter the parameter
     * @throws SessionNotFoundException the session not found exception
     * @throws NoNetworkAccessException the no network access exception
     * @throws NoAccessToServer the no access to server
     */
    private void createContainer(String container, Object parameter)
            throws SessionNotFoundException, NoNetworkAccessException,
            NoAccessToServer {

        Object response = Utils.doRequest(conn, SERVICE_ACTOR,
                METHOD_CONTAINER_CREATE, authToken, cm, new Object[] {
                        authToken, container, parameter });
    }

}
