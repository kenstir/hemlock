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
package org.evergreen_ils.system;

import android.text.TextUtils;

import org.evergreen_ils.Api;
import org.evergreen_ils.net.Gateway;
import org.evergreen_ils.android.Log;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.data.Organization;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.OSRFObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

public class EgSearch {

    private static final String TAG = EgSearch.class.getSimpleName();

    private static EgSearch instance = null;

    public Organization selectedOrganization = null;

    public @NonNull Integer visible = 0;

    private static int searchLimit = 100;

    final private @NonNull ArrayList<RecordInfo> results;
    
    public static EgSearch getInstance() {
        if (instance == null) {
            instance = new EgSearch();
        }
        return instance;
    }

    private EgSearch() {
        this.results = new ArrayList<>(searchLimit);
    }

    public void loadResults(@NonNull OSRFObject obj) {
        clearResults();
        visible = Api.parseInt(obj.get("count"), 0);
        if (visible == 0) return;

        // parse ids list
        List<List<?>> record_ids_lol = (List<List<?>>) obj.get("ids");

        // add to existing array, because SearchResultsFragment has an Adapter on it
        results.addAll(RecordInfo.makeArray(record_ids_lol));
    }

    // Build query string, taken with a grain of salt from
    // https://wiki.evergreen-ils.org/doku.php?id=documentation:technical:search_grammar
    // e.g. "title:Harry Potter chamber of secrets search_format(book) site(MARLBORO)"
    public String makeQueryString(String searchText, String searchClass, String searchFormat, String sort) {
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
        EgSearch.searchLimit = searchLimit;
    }

    public static int getSearchLimit() {
        return searchLimit;
    }

    public ArrayList<RecordInfo> getResults() {
        return results;
    }

    public void clearResults() {
        results.clear();
        visible = 0;
    }
}
