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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.evergreen_ils.Api;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.system.EvergreenServer;
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

    public Integer visible;

    public final Integer searchLimit = 500;
    
    public String searchText = null;
    public String searchClass = null;
    public String searchFormat = null;

    public static SearchCatalog getInstance() {
        if (instance == null) {
            instance = new SearchCatalog();
        }
        return instance;
    }

    /**
     * Instantiates a new search catalog.
     */
    private SearchCatalog() {
    }

    private static HttpConnection conn() {
        return EvergreenServer.getInstance().gatewayConnection();
    }

    /**
     * Return o as an Integer
     *
     * Sometimes search returns a count as a json number ("count":0), sometimes a string ("count":"1103").
     * Seems to be the same for result "ids" list (See Issue #1).  Handle either form and return as an int.
     */
    public static Integer toInteger(Object o) {
        if (o instanceof Integer) {
            return (Integer)o;
        } else if (o instanceof String) {
            return Integer.parseInt((String)o);
        } else {
            Log.d(TAG, "unexpected type: "+o);
            return null;
        }
    }

    /**
     * Gets the search results
     * 
     * @param searchText the search words
     * @return the search results
     */
    public ArrayList<RecordInfo> getSearchResults(String searchText, String searchClass, String searchFormat, Integer offset) {

        this.searchText = searchText;
        this.searchClass = searchClass;
        this.searchFormat = searchFormat;
        
        ArrayList<RecordInfo> results = new ArrayList<RecordInfo>();

        HashMap complexParm = new HashMap<String, Integer>();
        if (this.selectedOrganization != null) {
            if (this.selectedOrganization.id != null) complexParm.put("org_unit", this.selectedOrganization.id);
            // I'm not too sure about this depth option
            if (this.selectedOrganization.level != null) complexParm.put("depth", this.selectedOrganization.level);
        }
        complexParm.put("limit", searchLimit);
        complexParm.put("offset", offset);
        if (searchClass != null) complexParm.put("default_class", searchClass);

        String queryString = searchText;
        if (!searchFormat.isEmpty())
            queryString += " search_format(" + searchFormat + ")";

        long start_ms = System.currentTimeMillis();
        long now_ms = start_ms;

        // do request
        Object resp = Utils.doRequest(conn(), Api.SEARCH, Api.MULTICLASS_QUERY,
                new Object[] { complexParm, queryString, 1 });
        Log.d(TAG, "Sync Response: " + resp);
        now_ms = Log.logElapsedTime(TAG, now_ms, "search.query");
        if (resp == null)
            return results; // search failed or server crashed

        Map<String, ?> response = (Map<String, ?>) resp;
        visible = toInteger(response.get("count"));

        // result_lol is a list of lists and looks like one of:
        //   [[32673,null,"0.0"],[886843,null,"0.0"]] // integer ids+?
        //   [["503610",null,"0.0"],["502717",null,"0.0"]] // string ids+?
        //   [["1805532"],["2385399"]] // string ids only
        List<List<?>> record_ids_lol = (List<List<?>>) response.get("ids");
        Log.d(TAG, "length:"+record_ids_lol.size());
        for (int i = 0; i < record_ids_lol.size(); i++) {
            Integer record_id = toInteger(record_ids_lol.get(i).get(0));
            results.add(new RecordInfo(record_id));
        }

        if (LOAD_BASIC_METADATA_SYNCHRONOUSLY) {
            fetchBasicMetadataBatch(results);
            now_ms = Log.logElapsedTime(TAG, now_ms, "search.fetchBasicMetadataBatch");
        }
        if (LOAD_SEARCH_FORMAT_SYNCHRONOUSLY) {
            fetchSearchFormatBatch(results);
            now_ms = Log.logElapsedTime(TAG, now_ms, "search.fetchSearchFormatBatch");
        }

        Log.logElapsedTime(TAG, start_ms, "search.total");

        return results;
    }

    /**
     * Gets the item short info.
     * 
     * @param id
     *            the id
     * @return the item short info
     */
    //todo replace callers of this method with RecordLoader.fetchBasicMetadata
    private OSRFObject getItemShortInfo(Integer id) {
        OSRFObject response = (OSRFObject) Utils.doRequest(conn(), Api.SEARCH,
                Api.MODS_SLIM_RETRIEVE, new Object[] {
                        id });
        return response;
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

    public static void fetchSearchFormatBatch(ArrayList<RecordInfo> records) {
        if (records.size() == 0)
            return;
        // todo newer EG supports using "ANONYMOUS" as the auth_token in PCRUD requests.
        // Older EG does not, and requires a valid auth_token.
        ArrayList<Integer> ids = new ArrayList<Integer>(records.size());
        HashMap<Integer, RecordInfo> records_by_id = new HashMap<Integer, RecordInfo>(records.size());
        for (RecordInfo record : records) {
            ids.add(record.doc_id);
            records_by_id.put(record.doc_id, record);
        }
        HashMap<String, Object> args = new HashMap<String, Object>();
        args.put("id", ids);
        Object response = Utils.doRequest(conn(), Api.PCRUD_SERVICE,
                Api.SEARCH_MRAF, new Object[] {
                AccountAccess.getInstance().getAuthToken(),
                args });
        try {
            ArrayList<OSRFObject> responses = (ArrayList<OSRFObject>) response;
            for (OSRFObject attr_obj : responses) {
                String attr = attr_obj.getString("attr");
                if (!attr.equals("search_format"))
                    continue;
                String value = attr_obj.getString("value");
                Integer id = attr_obj.getInt("id");
                RecordInfo record = records_by_id.get(id);
                if (record == null)
                    continue;
                record.setSearchFormat(value);
            }
        } catch (ClassCastException ex) {
            Log.d(TAG, "caught", ex);
        }
    }

    // candidate for on-demand loading
    public static List<OSRFObject> fetchCopyStatuses() {

        List<OSRFObject> ccs_list = (List<OSRFObject>) Utils.doRequest(conn(), Api.SEARCH,
                Api.COPY_STATUS_ALL, new Object[] {});
        return ccs_list;
    }

    public ArrayList<CopyLocationCounts> getCopyLocationCounts(Integer recordID, Integer orgID, Integer orgDepth) {

        Object response = Utils.doRequest(conn(), Api.SEARCH,
                Api.COPY_LOCATION_COUNTS, new Object[] {
                        recordID, orgID, orgDepth });

        ArrayList<CopyLocationCounts> ret = new ArrayList<CopyLocationCounts>();
        try {
            List<List<Object>> list = (List<List<Object>>) response;
            for (List<Object> elem : list) {
                CopyLocationCounts copyInfo = new CopyLocationCounts(elem);
                ret.add(copyInfo);
            }
        } catch (Exception e) {
            Log.d(TAG, "exception in getCopyLocationCounts", e);
        }

        return ret;
    }

    // todo replace callers with RecordLoader
    public ArrayList<RecordInfo> getRecordsInfo(ArrayList<Integer> ids) {

        ArrayList<RecordInfo> recordInfoArray = new ArrayList<RecordInfo>();

        for (int i = 0; i < ids.size(); i++) {
            RecordInfo recordInfo = new RecordInfo(getItemShortInfo(ids.get(i)));
            recordInfoArray.add(recordInfo);
        }

        return recordInfoArray;
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
