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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.evergreen_ils.Api;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.globals.Log;
import org.evergreen_ils.globals.Utils;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.GatewayResponse;
import org.opensrf.util.OSRFObject;

/**
 * The Class SearchCatalog.
 */
public class SearchCatalog {

    private static final String TAG = SearchCatalog.class.getSimpleName();

    public static final boolean LOAD_BASIC_METADATA_SYNCHRONOUSLY = false;
    public static final boolean LOAD_SEARCH_FORMAT_SYNCHRONOUSLY = true;

    private static SearchCatalog instance = null;

    // the org on which the searches will be made
    public Organisation selectedOrganization = null;

    public Integer offset;

    public Integer visible;

    public final Integer searchLimit = 20;
    
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
        return GlobalConfigs.gatewayConnection();
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
        List<List<String>> result_ids;
        result_ids = (List<List<String>>) response.get("ids");
        Log.d(TAG, "length:"+result_ids.size());
        
        // sometimes count is an int ("count":0) and sometimes string ("count":"1103"), handle it either way
        visible = Integer.parseInt(response.get("count").toString());

        ArrayList<String> ids = new ArrayList<String>();
        for (int i = 0; i < result_ids.size(); i++) {
            ids.add(result_ids.get(i).get(0));
        }
        Log.d(TAG, "ids " + ids);

        // construct result list
        for (int i = 0; i < ids.size(); i++) {
            Integer record_id = Integer.parseInt(ids.get(i));
            results.add(new RecordInfo(record_id));
            /*
            Original impl: load basic metadata synchronously
            RecordInfo record = new RecordInfo(getItemShortInfo(record_id));
            now_ms = Log.logElapsedTime(TAG, now_ms, "search.getItemShortInfo");
            results.add(record);
            */
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
        OSRFObject response = (OSRFObject) Utils.doRequestSimple(conn(), Api.SEARCH,
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
        Object response = Utils.doRequestSimple(conn(), Api.SEARCH,
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
        Object response = Utils.doRequestSimple(conn(), Api.PCRUD_SERVICE,
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

    public Object getCopyStatuses() {

        List<OSRFObject> ccs_list = (List<OSRFObject>) Utils.doRequestSimple(conn(), Api.SEARCH,
                Api.COPY_STATUS_ALL, new Object[] {});

        // todo wtf, why is SearchCatalog loading up a member var of CopyInformation???
        CopyInformation.availableOrgStatuses = new LinkedHashMap<String, String>();

        if (ccs_list != null) {
            for (int i = 0; i < ccs_list.size(); i++) {
                OSRFObject ccs_obj = ccs_list.get(i);
                if (ccs_obj.getString("opac_visible").equals("t")) {
                    CopyInformation.availableOrgStatuses.put(
                            ccs_obj.getInt("id") + "",
                            ccs_obj.getString("name"));
                    //Log.d(TAG, "Add status "+ccs_obj.getString("name"));
                }
            }
        }
        return ccs_list;
    }

    public ArrayList<CopyInformation> getCopyLocationCounts(Integer recordID, Integer orgID, Integer orgDepth) {

        Object response = Utils.doRequestSimple(conn(), Api.SEARCH,
                Api.COPY_LOCATION_COUNTS, new Object[] {
                        recordID, orgID, orgDepth });

        ArrayList<CopyInformation> ret = new ArrayList<CopyInformation>();
        try {
            List<List<Object>> list = (List<List<Object>>) response;
            for (List<Object> elem : list) {
                CopyInformation copyInfo = new CopyInformation(elem);
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
    public void selectOrganisation(Organisation org) {
        Log.d(TAG, "selectOrganisation id=" + org.id);
        this.selectedOrganization = org;
    }

    public static ArrayList<CopyCountInformation> getCopyCount(Integer recordID, Integer orgID) {

        List<?> list = (List<?>) Utils.doRequestSimple(conn(), Api.SEARCH,
                Api.COPY_COUNT, new Object[] { orgID, recordID });

        ArrayList<CopyCountInformation> copyInfoList = new ArrayList<CopyCountInformation>();

        if (list == null)
            return copyInfoList;

        for (int i = 0; i < list.size(); i++) {
            CopyCountInformation copyInfo = new CopyCountInformation(list.get(i));
            copyInfoList.add(copyInfo);
        }

        return copyInfoList;
    }

    public static void setCopyLocationCounts(RecordInfo record, GatewayResponse response) {
        Log.d(TAG, "record.doc_id "+record.doc_id);
        record.copyInformationList = new ArrayList<CopyInformation>();
        if (response.failed)
            return;
        try {
            List<List<Object>> list = (List<List<Object>>) response.payload;
            for (List<Object> elem : list) {
                CopyInformation copyInfo = new CopyInformation(elem);
                record.copyInformationList.add(copyInfo);
            }
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }
    }
}
