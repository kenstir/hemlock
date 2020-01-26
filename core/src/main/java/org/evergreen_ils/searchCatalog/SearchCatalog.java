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

import org.evergreen_ils.Api;
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.system.Organization;
import org.evergreen_ils.system.Utils;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.OSRFObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchCatalog {

    private static final String TAG = SearchCatalog.class.getSimpleName();

    private static SearchCatalog instance = null;

    // the org on which the searches will be made
    public Organization selectedOrganization = null;

    public Integer visible = 0;

    // With limit at 500, we saw some TransactionTooLargeException: data parcel size 684000 bytes
    // 2017-11-24, still seeing the crash with searchLimit=400
    // 2020-01-25, still seeing it with searchLimit=200
    private static int searchLimit = 100;

    private ArrayList<RecordInfo> results;
    
    public static SearchCatalog getInstance() {
        if (instance == null) {
            instance = new SearchCatalog();
        }
        return instance;
    }

    private SearchCatalog() {
        this.results = new ArrayList<>(searchLimit);
    }

    private static HttpConnection conn() {
        return EvergreenServer.getInstance().gatewayConnection();
    }

    /**
     * Gets the search results
     * 
     * @param searchText the search words
     * @return the search results
     */
    public ArrayList<RecordInfo> getSearchResults(String searchText, String searchClass, String searchFormat, String sort, Integer offset) {

        HashMap<String, Integer> argHash = new HashMap<>();
        argHash.put("limit", searchLimit);
        argHash.put("offset", offset);

        String queryString = makeQueryString(searchText, searchClass, searchFormat, sort);

        long start_ms = System.currentTimeMillis();
        long now_ms = start_ms;

        // do request
        Object resp = Utils.doRequest(conn(), Api.SEARCH, Api.MULTICLASS_QUERY,
                new Object[] { argHash, queryString, 1 });
        Log.d(TAG, "Sync Response: " + resp);
        now_ms = Log.logElapsedTime(TAG, now_ms, "search.query");

        // handle cases of no results
        Map<String, ?> response = (Map<String, ?>) resp;
        visible = (response != null) ? Api.parseInteger(response.get("count"), 0) : 0;
        if (visible == 0)
            return new ArrayList<>();

        // parse ids list
        List<List<?>> record_ids_lol = (List<List<?>>) response.get("ids");
        results = RecordInfo.makeArray(record_ids_lol);

        Log.logElapsedTime(TAG, start_ms, "search.total");

        return results;
    }

    // Build query string, taken with a grain of salt from
    // https://wiki.evergreen-ils.org/doku.php?id=documentation:technical:search_grammar
    // e.g. "title:Harry Potter chamber of secrets search_format(book) site(MARLBORO)"
    private String makeQueryString(String searchText, String searchClass, String searchFormat, String sort) {
        StringBuilder sb = new StringBuilder();
        sb.append(searchClass).append(":").append(searchText);
        if (!TextUtils.isEmpty(searchFormat))
            sb.append(" search_format(").append(searchFormat).append(")");
        if (this.selectedOrganization != null)
            sb.append(" site(").append(this.selectedOrganization.shortname).append(")");
        if (!TextUtils.isEmpty(sort))
            sb.append(" sort(").append(sort).append(")");
        return sb.toString();
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

    public static void setSearchLimit(int searchLimit) {
        SearchCatalog.searchLimit = searchLimit;
    }

    public static int getSearchLimit() {
        return searchLimit;
    }

    public ArrayList<RecordInfo> getResults() {
        return results;
    }
}
