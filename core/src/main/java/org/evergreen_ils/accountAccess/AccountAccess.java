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
package org.evergreen_ils.accountAccess;

import android.app.Activity;
import android.text.TextUtils;

import org.evergreen_ils.Api;
import org.evergreen_ils.Result;
import org.evergreen_ils.accountAccess.bookbags.BookBag;
import org.evergreen_ils.accountAccess.bookbags.BookBagItem;
import org.evergreen_ils.android.AccountUtils;
import org.evergreen_ils.android.App;
import org.evergreen_ils.auth.Const;
import org.evergreen_ils.data.Account;
import org.evergreen_ils.net.Gateway;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.system.Utils;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.OSRFObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountAccess {

    private final static String TAG = AccountAccess.class.getSimpleName();

    private static AccountAccess mInstance = null;

    private ArrayList<BookBag> bookBags = new ArrayList<>();

    private AccountAccess() {
    }

    public static AccountAccess getInstance() {
        if (mInstance == null)
            mInstance = new AccountAccess();
        return mInstance;
    }

    private HttpConnection conn() {
        return Gateway.INSTANCE.getConn();
    }

    /** invalidate current auth token and get a new one
     *
     * @param activity
     * @return true if auth successful
     */
    public boolean reauthenticate(Activity activity) {
        Log.d(Const.AUTH_TAG, "reauthenticate");
        Account account = App.getAccount();
        AccountUtils.invalidateAuthToken(activity, account.getAuthToken());
        App.getAccount().setAuthToken(null);

        try {
            String auth_token = AccountUtils.getAuthTokenForAccount(activity, account.getUsername());
            if (TextUtils.isEmpty(auth_token))
                return false;
            account.clearAuthToken();
            return true;
        } catch (Exception e) {
            Log.d(Const.AUTH_TAG, "[auth] reauthenticate exception", e);
            return false;
        }
    }

    // ------------------------Holds Section
    // --------------------------------------//

    public Result testAndCreateHold(Integer recordID, Integer pickup_lib,
                                    boolean email_notify, String phone_notify,
                                    String sms_notify, Integer sms_carrier_id,
                                    String expire_time, boolean suspendHold, String thaw_date)
            throws SessionNotFoundException {
        Account account = App.getAccount();
        /*
        The named fields in the hash are:

        patronid     - ID of the hold recipient  (required)
        depth        - hold range depth          (default 0)
        pickup_lib   - destination for hold, fallback value for selection_ou
        selection_ou - ID of org_unit establishing hard and soft hold boundary settings
        issuanceid   - ID of the issuance to be held, required for Issuance level hold
        partid       - ID of the monograph part to be held, required for monograph part level hold
        titleid      - ID (BRN) of the title to be held, required for Title level hold
        volume_id    - required for Volume level hold
        copy_id      - required for Copy level hold
        mrid         - required for Meta-record level hold
        hold_type    - T, C (or R or F), I, V or M for Title, Copy, Issuance, Volume or Meta-record  (default "T")
         */
        HashMap<String, Object> args = new HashMap<>();
        args.put("patronid", account.getId());
        args.put("pickup_lib", pickup_lib);
        args.put("titleid", recordID);//is this required?
        args.put("hold_type", "T");
        args.put("email_notify", email_notify);
        args.put("expire_time", expire_time);
        if (!TextUtils.isEmpty(phone_notify))
            args.put("phone_notify", phone_notify);
        if (sms_carrier_id != null && !TextUtils.isEmpty(sms_notify)) {
            args.put("sms_carrier", sms_carrier_id);
            args.put("sms_notify", sms_notify);
        }
        if (suspendHold && thaw_date != null) {
            args.put("frozen", suspendHold);
            args.put("thaw_date", thaw_date);
        }

        ArrayList<Integer> ids = new ArrayList<>(1);
        ids.add(recordID);

        Object resp = Utils.doRequest(conn(), Api.CIRC,
                Api.HOLD_TEST_AND_CREATE, account.getAuthToken(), new Object[] {
                        account.getAuthToken(), args, ids });

        Result result_obj = Result.createUnknownError();
        try {
            Map<String, ?> resp_map = ((Map<String, ?>) resp);
            Object result = resp_map.get("result");
            if (result instanceof Integer) {
                Integer hold_id = (Integer) result;
                if (hold_id > -1) {
                    result_obj = Result.createFromSuccess(result);
                }
            } else if (result instanceof List) {
                // List of error events
                List<?> l = (List<?>) result;
                result_obj = Result.createFromEvent(l.get(0));
            } else if (result instanceof Map) {
                Map<String, ?> result_map = (Map<String, ?>) result;
                result_obj = Result.createFromEvent(result_map.get("last_event"));
            } else {
                Log.d(TAG, "unknown response from test_and_create: "+result);
            }
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }

        return result_obj;
    }

    // ---------------------------------------Book
    // bags-----------------------------------//

    /**
     * Retrieve bookbags from the server.
     *
     * @return the bookbags
     * @throws SessionNotFoundException the session not found exception
     */
    // todo: load on demand.  It takes ~750ms to load my 4 bookbags on startup.
    public boolean retrieveBookbags() throws SessionNotFoundException {
        Account account = App.getAccount();

        Object response = Utils.doRequest(conn(), Api.ACTOR,
                Api.CONTAINERS_BY_CLASS, account.getAuthToken(), new Object[] {
                        account.getAuthToken(), account.getId(), Api.CONTAINER_CLASS_BIBLIO, Api.CONTAINER_BUCKET_TYPE_BOOKBAG });

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

        Collections.sort(this.bookBags, new Comparator<BookBag>() {
            @Override
            public int compare(BookBag lhs, BookBag rhs) {
                return lhs.name.compareTo(rhs.name);
            }
        });

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
        Account account = App.getAccount();

        Map<String, ?> map = (Map<String, ?>) Utils.doRequest(conn(), Api.ACTOR,
                Api.CONTAINER_FLESH, account.getAuthToken(), new Object[] {
                        account.getAuthToken(), Api.CONTAINER_CLASS_BIBLIO, bookbagID });
        
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

        removeContainerItem(Api.CONTAINER_CLASS_BIBLIO, id);

    }

    /**
     * Creates the bookbag.
     *
     * @param name the name
     * @throws SessionNotFoundException the session not found exception
     */
    public void createBookbag(String name) throws SessionNotFoundException {
        Account account = App.getAccount();

        OSRFObject cbreb = new OSRFObject("cbreb");
        cbreb.put("btype", Api.CONTAINER_BUCKET_TYPE_BOOKBAG);
        cbreb.put("name", name);
        cbreb.put("pub", false);
        cbreb.put("owner", account.getId());

        createContainer(Api.CONTAINER_CLASS_BIBLIO, cbreb);
    }

    /**
     * Delete book bag.
     *
     * @param id the id
     * @throws SessionNotFoundException the session not found exception
     */
    public void deleteBookBag(Integer id) throws SessionNotFoundException {
        Account account = App.getAccount();

        Object response = Utils.doRequest(conn(), Api.ACTOR,
                Api.CONTAINER_FULL_DELETE, account.getAuthToken(), new Object[] {
                        account.getAuthToken(), Api.CONTAINER_CLASS_BIBLIO, id });
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
        Account account = App.getAccount();

        OSRFObject cbrebi = new OSRFObject("cbrebi");
        cbrebi.put("bucket", bookbag_id);
        cbrebi.put("target_biblio_record_entry", record_id);
        cbrebi.put("id", null);

        Object response = Utils.doRequest(conn(), Api.ACTOR,
                Api.CONTAINER_ITEM_CREATE, account.getAuthToken(), new Object[] {
                        account.getAuthToken(), Api.CONTAINER_CLASS_BIBLIO, cbrebi });
    }

    /**
     * Removes the container.
     *
     * @param container the container
     * @param id the id
     * @throws SessionNotFoundException the session not found exception
     */
    private void removeContainerItem(String container, Integer id)
            throws SessionNotFoundException {
        Account account = App.getAccount();

        Object response = Utils.doRequest(conn(), Api.ACTOR,
                Api.CONTAINER_ITEM_DELETE, account.getAuthToken(), new Object[] {
                        account.getAuthToken(), container, id });
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
        Account account = App.getAccount();

        Object response = Utils.doRequest(conn(), Api.ACTOR,
                Api.CONTAINER_CREATE, account.getAuthToken(), new Object[] {
                        account.getAuthToken(), container, parameter });
    }

    private OSRFObject getItemShortInfo(Integer id) {
        OSRFObject response = (OSRFObject) Utils.doRequest(conn(), Api.SEARCH,
                Api.MODS_SLIM_RETRIEVE, new Object[] {
                        id });
        return response;
    }

    public ArrayList<RecordInfo> getRecordsInfo(ArrayList<Integer> ids) {

        ArrayList<RecordInfo> recordInfoArray = new ArrayList<RecordInfo>();

        for (int i = 0; i < ids.size(); i++) {
            RecordInfo recordInfo = new RecordInfo(getItemShortInfo(ids.get(i)));
            recordInfoArray.add(recordInfo);
        }

        return recordInfoArray;
    }
}
