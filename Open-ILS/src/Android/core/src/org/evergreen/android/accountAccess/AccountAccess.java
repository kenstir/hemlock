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

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import android.accounts.*;
import android.app.Activity;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import org.evergreen.android.R;
import org.evergreen.android.accountAccess.bookbags.BookBag;
import org.evergreen.android.accountAccess.bookbags.BookBagItem;
import org.evergreen.android.accountAccess.checkout.CircRecord;
import org.evergreen.android.accountAccess.fines.FinesRecord;
import org.evergreen.android.accountAccess.holds.HoldRecord;
import org.evergreen.android.globals.Utils;
import org.evergreen.android.searchCatalog.RecordInfo;
import org.evergreen_ils.auth.Const;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.OSRFObject;

import android.net.ConnectivityManager;

/**
 * The Class AuthenticateUser. Singleton class
 */
public class AccountAccess {

    public static String SERVICE_AUTH = "open-ils.auth";

    /** The METHOD Auth init. */
    public static String METHOD_AUTH_INIT = "open-ils.auth.authenticate.init";

    /** The METHOD Auth complete. */
    public static String METHOD_AUTH_COMPLETE = "open-ils.auth.authenticate.complete";

    /** The METHOD Auth session retrieve. */
    public static String METHOD_AUTH_SESSION_RETRV = "open-ils.auth.session.retrieve";

    public static String SERVICE_ACTOR = "open-ils.actor";
    public static String SERVICE_CIRC = "open-ils.circ";
    public static String SERVICE_SEARCH = "open-ils.search";
    public static String SERVICE_SERIAL = "open-ils.serial";
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
    private ArrayList<BookBag> bookBags = new ArrayList<BookBag>();

    /** The conn. */
    public HttpConnection conn;

    /** The http address. */
    private String httpAddress = "http://ulysses.calvin.edu";

    /** The TAG. */
    private final String TAG = AccountAccess.class.getName();

    /**
     * The auth token. Sent with every request that needs authentication
     * */
    private String authToken = null;

    /** The cm. */
    private ConnectivityManager cm = null;

    /** The auth time. */
    private Integer authTime = null;

    /** The user id. */
    private Integer userID = null;
    
    /** home library ID. */
    private Integer homeLibraryID = null;

    private boolean haveSession;

    /** The user name. */
    public static String userName = null;
    
    /** Whether we have ever established a session  **/

    /** The account access. */
    private static AccountAccess accountAccess = null;

    /**
     * Instantiates a new authenticate user.
     *
     * @param httpAddress the http address
     */
    private AccountAccess(String httpAddress) {

        Log.d(TAG, "AccountAccess ctor: " + httpAddress);
        this.httpAddress = httpAddress;

        try {
            // configure the connection
            Log.d(TAG, "Connection with " + httpAddress);
            conn = new HttpConnection(httpAddress + "/osrf-gateway-v1");

        } catch (Exception e) {
            Log.d(TAG, "Exception in establishing connection", e);
        }

    }

    /**
     * Gets the account access.
     *
     * @param httpAddress the http address
     * @return the account access
     */
    public static AccountAccess getAccountAccess(String httpAddress) {

        if (accountAccess == null) {
            accountAccess = new AccountAccess(httpAddress);
        }
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
        return accountAccess;
    }

    public Integer getHomeLibraryID() {
        return homeLibraryID;
    }

    public void setHomeLibraryID(Integer homeLibraryID) {
        this.homeLibraryID = homeLibraryID;
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
        Log.d(TAG, "update http address of account access to "
                + httpAddress);
        try {
            // configure the connection
            this.httpAddress = httpAddress;
            Log.d(TAG, "Connection with " + httpAddress);
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
     * Retrieve session.
     * @throws SessionNotFoundException
     */
    public boolean retrieveSession(String auth_token, boolean force) throws SessionNotFoundException {

        if (!force && this.haveSession && this.authToken.equals(auth_token))
            return true;
        this.haveSession = false;
        this.authToken = auth_token;
        
        Object resp = Utils.doRequest(conn, SERVICE_AUTH, METHOD_AUTH_SESSION_RETRV, authToken, new Object[] {authToken});
        if (resp != null) {
            OSRFObject au = (OSRFObject) resp;
            userID = au.getInt("id");
            homeLibraryID = au.getInt("home_ou");
            userName = au.getString("usrname");
            //email = au.getString("email");
            this.haveSession = true;
        }
        return this.haveSession;
    }

    public static boolean runningOnUIThread() {
        return (Looper.myLooper() == Looper.getMainLooper());
    }

    /** invalidate current auth token and get a new one
     *
     * @param activity
     * @return true if auth successful
     */
    public boolean reauthenticate(Activity activity) throws SessionNotFoundException, AuthenticatorException, OperationCanceledException, IOException {
        boolean ok = false;
        final AccountManager am = AccountManager.get(activity);
        final String accountType = activity.getString(R.string.ou_account_type);
        final Account account = new Account(userName, accountType);
        am.invalidateAuthToken(accountType, authToken);
        haveSession = false;
        authToken = null;
        if (runningOnUIThread())
            return false;
        Bundle b = am.getAuthToken(account, Const.AUTHTOKEN_TYPE, null, activity, null, null).getResult();
        final String new_authToken = b.getString(AccountManager.KEY_AUTHTOKEN);
        if (TextUtils.isEmpty(new_authToken))
            return false;
        return retrieveSession(new_authToken, true);
    }

    // ------------------------Checked Out Items Section
    // -------------------------//

    /**
     * Gets the items checked out.
     *
     * @return the items checked out
     * @throws SessionNotFoundException the session not found exception
     */
    public ArrayList<CircRecord> getItemsCheckedOut()
            throws SessionNotFoundException {

        ArrayList<CircRecord> circRecords = new ArrayList<CircRecord>();

        Object resp = Utils.doRequest(conn, SERVICE_ACTOR,
                METHOD_FETCH_CHECKED_OUT_SUM, authToken, new Object[] {
                        authToken, userID });
        if (resp == null)
            return circRecords;
        Map<String, ?> resp_map = ((Map<String, ?>) resp);

        if (resp_map.get("out") != null) {
            List<String> out_id = (List<String>) resp_map.get("out");
            for (int i = 0; i < out_id.size(); i++) {
                OSRFObject circ = retrieveCircRecord(out_id.get(i));
                CircRecord circRecord = new CircRecord(circ, CircRecord.OUT,
                        Integer.parseInt(out_id.get(i)));
                fetchInfoForCheckedOutItem(circ.getInt("target_copy"), circRecord);
                circRecords.add(circRecord);
            }
        }

        if (resp_map.get("overdue") != null) {
            List<String> overdue_id = (List<String>) resp_map.get("overdue");
            for (int i = 0; i < overdue_id.size(); i++) {
                OSRFObject circ = retrieveCircRecord(overdue_id.get(i));
                CircRecord circRecord = new CircRecord(circ, CircRecord.OVERDUE,
                        Integer.parseInt(overdue_id.get(i)));
                fetchInfoForCheckedOutItem(circ.getInt("target_copy"), circRecord);
                circRecords.add(circRecord);
            }
        }

        /* Other fields returned by the request; apparently not used in OPAC
        resp_map.get("claims_returned");
        resp_map.get("long_overdue")
        resp_map.get("lost");
        */

        Collections.sort(circRecords, new Comparator<CircRecord>() {
            @Override
            public int compare(CircRecord lhs, CircRecord rhs) {
                return lhs.getDueDate().compareTo(rhs.getDueDate());
            }
        });

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
     */
    private OSRFObject retrieveCircRecord(String id)
            throws SessionNotFoundException {

        OSRFObject circ = (OSRFObject) Utils.doRequest(conn, SERVICE_CIRC,
                METHOD_FETCH_CIRC_BY_ID, authToken, new Object[] {
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
     */
    private OSRFObject fetchInfoForCheckedOutItem(Integer target_copy,
            CircRecord circRecord) {

        if (target_copy == null)
            return null;

        OSRFObject result;
        Log.d(TAG, "Mods from copy");
        OSRFObject info_mvr = fetchModsFromCopy(target_copy);
        // if title or author not inserted, request acp with copy_target
        result = info_mvr;
        OSRFObject info_acp = null;

        // the logic to establish mvr or acp is copied from the opac
        if (info_mvr.getString("title") == null
                || info_mvr.getString("author") == null) {
            Log.d(TAG, "Asset");
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
     */
    private OSRFObject fetchModsFromCopy(Integer target_copy) {

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
     */
    private OSRFObject fetchAssetCopy(Integer target_copy) {

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
     */
    public void renewCirc(Integer target_copy) throws MaxRenewalsException,
            ServerErrorMessage, SessionNotFoundException {

        HashMap<String, Integer> complexParam = new HashMap<String, Integer>();
        complexParam.put("patron", this.userID);
        complexParam.put("copyid", target_copy);
        complexParam.put("opac_renewal", 1);

        Object a_lot = (Object) Utils.doRequest(conn, SERVICE_CIRC,
                METHOD_RENEW_CIRC, authToken, new Object[] { authToken,
                        complexParam });

        Map<String, String> resp = (Map<String, String>) a_lot;

        if (resp.get("textcode") != null && !resp.get("textcode").equals("SUCCESS")) {
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
     */
    public Object fetchOrgSettings(Integer org_id, String setting)
            throws SessionNotFoundException {

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
     */
    public List<HoldRecord> getHolds() throws SessionNotFoundException {

        ArrayList<HoldRecord> holds = new ArrayList<HoldRecord>();

        // fields of interest : expire_time
        List<OSRFObject> listHoldsAhr = null;

        Object resp = Utils.doRequest(conn, SERVICE_CIRC, METHOD_FETCH_HOLDS,
                authToken, new Object[] { authToken, userID });
        if (resp == null) {
            Log.d(TAG, "Result: null");
            return holds;
        }

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
     */
    private Object fetchHoldTitleInfo(OSRFObject holdArhObject, HoldRecord hold) {

        String holdType = (String) holdArhObject.get("hold_type");

        String method = null;

        Object response;
        OSRFObject holdInfo = null;
        if (holdType.equals("T") || holdType.equals("M")) {

            if (holdType.equals("M"))
                method = METHOD_FETCH_MRMODS;
            if (holdType.equals("T"))
                method = METHOD_FETCH_RMODS;
            holdInfo = (OSRFObject) Utils.doRequest(conn, SERVICE_SEARCH,
                    method, cm, new Object[] { holdArhObject.get("target") });

            // Log.d(TAG, "Hold here " + holdInfo);
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
     */
    private OSRFObject holdFetchObjects(OSRFObject hold, HoldRecord holdObj) {

        String type = (String) hold.get("hold_type");

        Log.d(TAG, "Hold Type " + type);
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

                Log.d(TAG, "Record " + record);
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

            Log.d(TAG, "Record " + record);
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
        }

        return null;
    }

    /**
     * Fetch hold status.
     *
     * @param hold the hold
     * @param holdObj the hold obj
     * @throws SessionNotFoundException the session not found exception
     */
    public void fetchHoldStatus(OSRFObject hold, HoldRecord holdObj)
            throws SessionNotFoundException {

        Integer hold_id = hold.getInt("id");
        // MAP : potential_copies, status, total_holds, queue_position,
        // estimated_wait
        Object resp = Utils.doRequest(conn, SERVICE_CIRC,
                METHOD_FETCH_HOLD_STATUS, authToken, new Object[] {
                        authToken, hold_id });

        Map<String, Integer> map = (Map<String, Integer>)resp;
        holdObj.status = map.get("status");
        holdObj.potentialCopies = map.get("potential_copies");
        holdObj.estimatedWaitInSeconds = map.get("estimated_wait");
        holdObj.queuePosition = map.get("queue_position");
        holdObj.totalHolds = map.get("total_holds");
    }

    /**
     * Cancel hold.
     *
     * @param hold the hold
     * @return true, if successful
     * @throws SessionNotFoundException the session not found exception
     */
    public boolean cancelHold(OSRFObject hold) throws SessionNotFoundException {

        Integer hold_id = hold.getInt("id");

        Object response = Utils.doRequest(conn, SERVICE_CIRC,
                METHOD_CANCEL_HOLD, authToken, new Object[] { authToken,
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
     * @param suspendHold the suspend hold
     * @param expire_time the expire_time
     * @param thaw_date the thaw_date
     * @return the object
     * @throws SessionNotFoundException the session not found exception
     */
    public Object updateHold(OSRFObject ahr, Integer pickup_lib,
            boolean suspendHold, String expire_time, String thaw_date)
            throws SessionNotFoundException {
        // TODO verify that object is correct passed to the server

        ahr.put("pickup_lib", pickup_lib);
        ahr.put("expire_time", expire_time);
        // frozen set, what this means ?
        ahr.put("frozen", suspendHold);
        // only if it is frozen
        ahr.put("thaw_date", thaw_date);

        Object response = Utils.doRequest(conn, SERVICE_CIRC,
                METHOD_UPDATE_HOLD, authToken, new Object[] { authToken,
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
     */
    public String[] createHold(Integer recordID, Integer pickup_lib,
            boolean email_notify, boolean phone_notify, String phone,
            boolean suspendHold, String expire_time, String thaw_date)
            throws SessionNotFoundException {

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
                METHOD_CREATE_HOLD, authToken, new Object[] { authToken,
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

        Log.d(TAG, "Result " + resp[1] + " " + resp[2]);

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
     */
    public Object isHoldPossible(Integer pickup_lib, Integer recordID)
            throws SessionNotFoundException {

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
                METHOD_VERIFY_HOLD_POSSIBLE, authToken, new Object[] {
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
     */
    public HashMap<String, Integer> getHoldPreCreateInfo(Integer recordID,
            Integer pickup_lib) {

        HashMap<String, Integer> param = new HashMap<String, Integer>();

        param.put("pickup_lib", pickup_lib);
        param.put("record", recordID);

        Map<String, ?> response = (Map<String, ?>) Utils.doRequest(conn,
                SERVICE_SEARCH,
                "open-ils.search.metabib.record_to_descriptors", cm,
                new Object[] { param });

        Object obj = response.get("metarecord");
        Log.d(TAG, "metarecord="+obj);
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
     */
    public float[] getFinesSummary() throws SessionNotFoundException {

        // mous object
        OSRFObject finesSummary = (OSRFObject) Utils.doRequest(conn,
                SERVICE_ACTOR, METHOD_FETCH_FINES_SUMMARY, authToken,
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
     */
    public ArrayList<FinesRecord> getTransactions()
            throws SessionNotFoundException {

        ArrayList<FinesRecord> finesRecords = new ArrayList<FinesRecord>();

        Object transactions = Utils.doRequest(conn, SERVICE_ACTOR,
                METHOD_FETCH_TRANSACTIONS, authToken, new Object[] {
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
     * Retrieve bookbags from the server.
     *
     * @return the bookbags
     * @throws SessionNotFoundException the session not found exception
     */
    public boolean retrieveBookbags() throws SessionNotFoundException {

        Object response = Utils.doRequest(conn, SERVICE_ACTOR,
                METHOD_FLESH_CONTAINERS, authToken, new Object[] {
                        authToken, userID, "biblio", "bookbag" });

        List<OSRFObject> bookbags = (List<OSRFObject>) response;

        ArrayList<BookBag> bookBagObj = new ArrayList<BookBag>();
        // in order to refresh bookbags
        this.bookBags = bookBagObj;

        if (bookbags == null)
            return true;

        for (int i = 0; i < bookbags.size(); i++) {

            BookBag bag = new BookBag(bookbags.get(i));
            getBookbagContent(bag, bookbags.get(i).getInt("id"));

            bookBagObj.add(bag);
        }
        return true;
    }
    
    public ArrayList<BookBag> getBookbags() {
        return this.bookBags;
    }

    /**
     * Gets the bookbag content.
     *
     * @param bag the bag
     * @param bookbagID the bookbag id
     * @return the bookbag content
     * @throws SessionNotFoundException the session not found exception
     */
    private Object getBookbagContent(BookBag bag, Integer bookbagID)
            throws SessionNotFoundException {

        Map<String, ?> map = (Map<String, ?>) Utils.doRequest(conn,
                SERVICE_ACTOR, METHOD_FLESH_PUBLIC_CONTAINER, authToken,
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
     */
    public void removeBookbagItem(Integer id) throws SessionNotFoundException {

        removeContainer("biblio", id);

    }

    /**
     * Creates the bookbag.
     *
     * @param name the name
     * @throws SessionNotFoundException the session not found exception
     */
    public void createBookbag(String name) throws SessionNotFoundException {

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
     */
    public void deleteBookBag(Integer id) throws SessionNotFoundException {

        Object response = Utils.doRequest(conn, SERVICE_ACTOR,
                METHOD_CONTAINER_FULL_DELETE, authToken, new Object[] {
                        authToken, "biblio", id });
    }

    /**
     * Adds the record to book bag.
     *
     * @param record_id the record_id
     * @param bookbag_id the bookbag_id
     * @throws SessionNotFoundException the session not found exception
     */
    public void addRecordToBookBag(Integer record_id, Integer bookbag_id)
            throws SessionNotFoundException {

        OSRFObject cbrebi = new OSRFObject("cbrebi");
        cbrebi.put("bucket", bookbag_id);
        cbrebi.put("target_biblio_record_entry", record_id);
        cbrebi.put("id", null);

        Object response = Utils.doRequest(conn, SERVICE_ACTOR,
                METHOD_CONTAINER_ITEM_CREATE, authToken, new Object[] {
                        authToken, "biblio", cbrebi });
    }

    /**
     * Removes the container.
     *
     * @param container the container
     * @param id the id
     * @throws SessionNotFoundException the session not found exception
     */
    private void removeContainer(String container, Integer id)
            throws SessionNotFoundException {

        Object response = Utils.doRequest(conn, SERVICE_ACTOR,
                METHOD_CONTAINER_DELETE, authToken, new Object[] {
                        authToken, container, id });
    }

    /**
     * Creates the container.
     *
     * @param container the container
     * @param parameter the parameter
     * @throws SessionNotFoundException the session not found exception
     */
    private void createContainer(String container, Object parameter)
            throws SessionNotFoundException {

        Object response = Utils.doRequest(conn, SERVICE_ACTOR,
                METHOD_CONTAINER_CREATE, authToken, new Object[] {
                        authToken, container, parameter });
    }
}
