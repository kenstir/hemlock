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
package org.evergreen_ils.searchCatalog;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.evergreen_ils.Api;
import org.evergreen_ils.data.CopyLocationCounts;
import org.evergreen_ils.net.Gateway;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.system.Utils;
import org.evergreen_ils.system.Organization;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.OSRFObject;

/**
 * The Class SearchCatalog.
 */
public class SearchCatalog {

    private static final String TAG = SearchCatalog.class.getSimpleName();

    public static final boolean LOAD_BASIC_METADATA_SYNCHRONOUSLY = false;
    public static final boolean LOAD_SEARCH_FORMAT_SYNCHRONOUSLY = false;

    private static SearchCatalog instance = null;

    // the org on which the searches will be made
    public Organization selectedOrganization = null;

    public Integer offset;

    public Integer visible = 0;

    // With limit at 500, we saw some TransactionTooLargeException: data parcel size 684000 bytes
    // 2017-11-24, still seeing the crash with searchLimit=400
    public int searchLimit;
    
    public String searchText = null;
    public String searchClass = null;
    public String searchFormat = null;

    public static SearchCatalog getInstance(int searchLimit) {
        if (instance == null) {
            instance = new SearchCatalog(searchLimit);
        }
        return instance;
    }

    /**
     * Instantiates a new search catalog.
     */
    private SearchCatalog(int searchLimit) {
        this.searchLimit = searchLimit;
    }

    private static HttpConnection conn() {
        return Gateway.INSTANCE.getConn();
    }

    /**
     * Gets the search results
     * 
     * @param searchText the search words
     * @return the search results
     */
    public ArrayList<RecordInfo> getSearchResults(String searchText, String searchClass, String searchFormat, String sort, Integer offset) {

        this.searchText = searchText;
        this.searchClass = searchClass;
        this.searchFormat = searchFormat;
        
        ArrayList<RecordInfo> results = new ArrayList<RecordInfo>();

        HashMap argHash = new HashMap<String, Integer>();
        argHash.put("limit", searchLimit);
        argHash.put("offset", offset);

        // build queryString, taken with a grain of salt from
        // https://wiki.evergreen-ils.org/doku.php?id=documentation:technical:search_grammar
        // e.g. "title:Harry Potter chamber of secrets search_format(book) site(MARLBORO)"
        StringBuilder sb = new StringBuilder();
        sb.append(searchClass).append(":").append(searchText);
        if (!TextUtils.isEmpty(searchFormat))
            sb.append(" search_format(").append(searchFormat).append(")");
        if (this.selectedOrganization != null)
            sb.append(" site(").append(this.selectedOrganization.shortname).append(")");
        if (!TextUtils.isEmpty(sort))
            sb.append(" sort(").append(sort).append(")");
        String queryString = sb.toString();

        long start_ms = System.currentTimeMillis();
        long now_ms = start_ms;

        // do request
        Object resp = Utils.doRequest(conn(), Api.SEARCH, Api.MULTICLASS_QUERY,
                new Object[] { argHash, queryString, 1 });
        Log.d(TAG, "Sync Response: " + resp);
        now_ms = Log.logElapsedTime(TAG, now_ms, "search.query");

        // handle cases of no results
        Map<String, ?> response = (Map<String, ?>) resp;
        visible = (response != null) ? Api.parseInt(response.get("count"), 0) : 0;
        if (visible == 0)
            return results;

        // record_ids_lol is a list of lists and looks like one of:
        //   [[32673,null,"0.0"],[886843,null,"0.0"]] // integer ids+?
        //   [["503610",null,"0.0"],["502717",null,"0.0"]] // string ids+?
        //   [["1805532"],["2385399"]] // string ids only
        List<List<?>> record_ids_lol = (List<List<?>>) response.get("ids");
        Log.d(TAG, "length:"+record_ids_lol.size());
        for (int i = 0; i < record_ids_lol.size(); i++) {
            Integer record_id = Api.parseInt(record_ids_lol.get(i).get(0));
            results.add(new RecordInfo(record_id));
        }

        if (LOAD_BASIC_METADATA_SYNCHRONOUSLY) {
            fetchBasicMetadataBatch(results);
            now_ms = Log.logElapsedTime(TAG, now_ms, "search.fetchBasicMetadataBatch");
        }
//        if (LOAD_SEARCH_FORMAT_SYNCHRONOUSLY) {
//            fetchSearchFormatBatch(results);
//            now_ms = Log.logElapsedTime(TAG, now_ms, "search.fetchSearchFormatBatch");
//        }

        Log.logElapsedTime(TAG, start_ms, "search.total");

        return results;
    }

    private void fetchBasicMetadataBatch(ArrayList<RecordInfo> records) {
        if (records.size() == 0)
            return;
        ArrayList<Integer> ids = new ArrayList<Integer>();
        for (RecordInfo record : records) {
            ids.add(record.doc_id);
        }
        Object response = Utils.doRequest(conn(), Api.SEARCH,
                Api.MODS_SLIM_BATCH, new Object[] {
                        ids });
        try {
            ArrayList<OSRFObject> responses = (ArrayList<OSRFObject>) response;
            for (int i = 0; i < records.size(); ++i) {
                RecordInfo.updateFromMODSResponse(records.get(i), responses.get(i));
            }
        } catch (ClassCastException ex) {
            Log.d(TAG, "caught", ex);
        }
    }

    /**
     * Select organisation.
     * 
     * @param org
     *            the organization on witch the searches will be made
     */
    public void selectOrganisation(Organization org) {
        Log.d(TAG, "selectOrganisation id=" + org.id);
        this.selectedOrganization = org;
    }

    public static ArrayList<CopySummary> getCopyCount(Integer recordID, Integer orgID) {

        List<?> list = (List<?>) Utils.doRequest(conn(), Api.SEARCH,
                Api.COPY_COUNT, new Object[] { orgID, recordID });

        ArrayList<CopySummary> copyInfoList = new ArrayList<CopySummary>();

        if (list == null)
            return copyInfoList;

        for (int i = 0; i < list.size(); i++) {
            CopySummary copyInfo = new CopySummary(list.get(i));
            copyInfoList.add(copyInfo);
        }

        return copyInfoList;
    }
}
