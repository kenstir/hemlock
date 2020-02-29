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
import org.evergreen_ils.android.AccountUtils;
import org.evergreen_ils.android.App;
import org.evergreen_ils.android.Log;
import org.evergreen_ils.auth.Const;
import org.evergreen_ils.data.Account;
import org.evergreen_ils.data.BookBag;
import org.evergreen_ils.net.Gateway;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.system.Utils;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.OSRFObject;

import java.util.ArrayList;

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

    // ---------------------------------------Book
    // bags-----------------------------------//

    public ArrayList<BookBag> getBookbags() {
        return this.bookBags;
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
