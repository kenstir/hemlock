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

    public static String SERVICE = "open-ils.search";
    public static String METHOD_MULTICLASS_QUERY = "open-ils.search.biblio.multiclass.query";
    public static String METHOD_SLIM_RETRIVE = "open-ils.search.biblio.record.mods_slim.retrieve";

    /**
     * Method that returns library where record with id is
     * 
     * @param : record ID to get all libraries, or just book ID, Current Library
     *        ID, User ID
     * @returns :
     *          [[["4","","CONCERTO 27","","Stacks",{"0":5}],["4","","PERFORM 27"
     *          ,"","Stacks",{"0":2}]]] "0":% is the available books [org_id,
     *          call_number_sufix, copy_location, status1:count, status2:count
     *          ..]
     */
    public static String METHOD_COPY_LOCATION_COUNTS = "open-ils.search.biblio.copy_location_counts.summary.retrieve";

    /**
     * Get copy statuses like Available, Checked_out , in_progress and others,
     * ccs OSRFObjects
     */
    public static String METHOD_COPY_STATUS_ALL = "open-ils.search.config.copy_status.retrieve.all";

    /**
     * Get copy count information
     * 
     * @param : org_unit_id, record_id
     * @returns: objects
     *           [{"transcendant":null,"count":35,"org_unit":1,"depth":0,
     *           "unshadow":35,"available":35},
     *           {"transcendant":null,"count":14,"org_unit"
     *           :2,"depth":1,"unshadow"
     *           :14,"available":14},{"transcendant":null,
     *           "count":7,"org_unit":4,"depth":2,"unshadow":7,"available":7}]
     */
    public static String METHOD_GET_COPY_COUNT = "open-ils.search.biblio.record.copy_count";

    private static SearchCatalog instance = null;

    private static final String TAG = SearchCatalog.class.getSimpleName();

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
     * @param searchText
     *            the search words
     * @return the search results
     */
    public ArrayList<RecordInfo> getSearchResults(String searchText, String searchClass, String searchFormat, Integer offset) {

        this.searchText = searchText;
        this.searchClass = searchClass;
        this.searchFormat = searchFormat;
        
        ArrayList<RecordInfo> resultsRecordInfo = new ArrayList<RecordInfo>();

        HashMap complexParm = new HashMap<String, Integer>();
        if (this.selectedOrganization != null) {
            if (this.selectedOrganization.id != null)
                complexParm.put("org_unit", this.selectedOrganization.id);
            // I'm not too sure about this depth option
            if (this.selectedOrganization.level != null)
                complexParm.put("depth", this.selectedOrganization.level);
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
        Object resp = Utils.doRequest(conn(), SERVICE, METHOD_MULTICLASS_QUERY,
                new Object[] { complexParm, queryString, 1 });
        Log.d(TAG, "Sync Response: " + resp);
        now_ms = logElapsedTime(TAG, now_ms, "search");
        if (resp == null)
            return resultsRecordInfo; // search failed or server crashed

        Map<String, ?> response = (Map<String, ?>) resp;

        List<List<String>> result_ids;
        result_ids = (List<List<String>>) response.get("ids");
        Log.d(TAG, "length:"+result_ids.size());
        
        // sometimes count is an int ("count":0) and sometimes string ("count":"1103")
        visible = Integer.parseInt(response.get("count").toString());

        ArrayList<String> ids = new ArrayList<String>();
        for (int i = 0; i < result_ids.size(); i++) {
            ids.add(result_ids.get(i).get(0));
        }
        Log.d(TAG, "Ids " + ids);

        // request other info based on ids
        for (int i = 0; i < ids.size(); i++) {
            Integer record_id = Integer.parseInt(ids.get(i));

            RecordInfo record = new RecordInfo(getItemShortInfo(record_id));
            now_ms = logElapsedTime(TAG, now_ms, "search.getItemShortInfo");
            resultsRecordInfo.add(record);

            AccountAccess ac = AccountAccess.getInstance();
            record.search_format = ac.fetchFormat(record_id.toString());
            now_ms = logElapsedTime(TAG, now_ms, "search.fetchFormat");

            // todo This takes 30% of the total search time but is not required for SearchCatalogListView
            // it is needed only for BasicDetailsFragment, but that would cause it to be run on the main thread
            // and that fails, so continue to do it here for now
            //record.copyCountListInfo = getCopyCount(record_id, this.selectedOrganization.id);
            //record.copyInformationList = getCopyLocationCounts(record_id, this.selectedOrganization.id, this.selectedOrganization.level);
            //now_ms = logElapsedTime(TAG, now_ms, "search.getCopyXXX");

            Log.d(TAG, "Title:" + record.title
                    + " Author:" + record.author
                    + " Pubdate:" + record.pubdate
                    + " Publisher:" + record.publisher);
        }
        logElapsedTime(TAG, start_ms, "search.total");

        return resultsRecordInfo;
    }

    public long logElapsedTime(String tag, long start_ms, String s) {
        long now_ms = System.currentTimeMillis();
        Log.d(tag, s + ": " + (now_ms - start_ms) + "ms");
        return now_ms;
    }

    /**
     * Gets the item short info.
     * 
     * @param id
     *            the id
     * @return the item short info
     */
    private OSRFObject getItemShortInfo(Integer id) {
        OSRFObject response = (OSRFObject) Utils.doRequestSimple(conn(), SERVICE,
                METHOD_SLIM_RETRIVE, new Object[] {
                        id });
        // todo remove this check once I can trust this assumption.
        // Elsewhere we look up record images based on doc_id.
        if (response.getInt("doc_id") != id) {
            throw(new AssertionError(id));
        }
        return response;
    }

    public Object getCopyStatuses() {

        List<OSRFObject> ccs_list = (List<OSRFObject>) Utils.doRequestSimple(conn(), SERVICE,
                METHOD_COPY_STATUS_ALL, new Object[] {});

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

        Object response = Utils.doRequestSimple(conn(), SERVICE,
                METHOD_COPY_LOCATION_COUNTS, new Object[] {
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

    public void setMethodCopyLocationCounts(R)

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

    public static void setCopyCountListInfo(RecordInfo record, GatewayResponse response) {
        record.copyCountListInfo = new ArrayList<CopyCountInformation>();
        if (response.failed)
            return;
        try {
            List<?> list = (List<?>) response.payload;
            for (Object obj : list) {
                CopyCountInformation info = new CopyCountInformation(obj);
                record.copyCountListInfo.add(info);
            }
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }
    }

    public static ArrayList<CopyCountInformation> getCopyCount(Integer recordID, Integer orgID) {

        List<?> list = (List<?>) Utils.doRequestSimple(conn(), SERVICE,
                METHOD_GET_COPY_COUNT, new Object[] { orgID, recordID });

        ArrayList<CopyCountInformation> copyInfoList = new ArrayList<CopyCountInformation>();

        if (list == null)
            return copyInfoList;

        for (int i = 0; i < list.size(); i++) {
            CopyCountInformation copyInfo = new CopyCountInformation(list.get(i));
            copyInfoList.add(copyInfo);
        }

        return copyInfoList;
    }
}
