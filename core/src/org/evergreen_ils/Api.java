/*
 * Copyright (C) 2016 Kenneth H. Cox
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.evergreen_ils;

/** API constants
 *
 * Created by kenstir on 9/26/2016.
 */
public class Api {

    /// actor

    public static final String SERVICE_ACTOR = "open-ils.actor";
    /** The METHOD_FETCH_CHECKED_OUT_SUM description : for a given user returns a a structure of circulation objects sorted by out, overdue, lost, claims_returned, long_overdue; A list of ID's returned for each type : "out":[id1,id2,...] @returns: { "out":[id 's],"claims_returned":[],"long_overdue":[],"overdue":[],"lost":[] } */
    public static final String METHOD_FETCH_CHECKED_OUT_SUM = "open-ils.actor.user.checked_out";
    public static final String METHOD_ACTOR_USER_FLESHED_RETRIEVE = "open-ils.actor.user.fleshed.retrieve";
    public static final String METHOD_ORG_TREE_RETRIEVE = "open-ils.actor.org_tree.retrieve";
    /** The METHOD_FETCH_ORG_SETTINGS description : retrieves a setting from the organization unit. @returns : returns the requested value of the setting */
    public static final String METHOD_FETCH_ORG_SETTINGS = "open-ils.actor.ou_setting.ancestor_default";
    public static final String METHOD_FETCH_FINES_SUMMARY = "open-ils.actor.user.fines.summary";
    public static final String METHOD_FETCH_TRANSACTIONS = "open-ils.actor.user.transactions.have_charge.fleshed";
    /** The METHOD_FLESH_CONTAINERS description : Retrieves all un-fleshed buckets by class assigned to a given user VIEW_CONTAINER permissions is requestID != owner ID. @returns : array of "cbreb" OSRFObjects */
    public static final String METHOD_FLESH_CONTAINERS = "open-ils.actor.container.retrieve_by_class.authoritative";
    /** The METHOD_FLESH_PUBLIC_CONTAINER description : array of contaoners correspondig to a id. @returns : array of "crebi" OSRF objects (content of bookbag, id's of elements to get more info) */
    public static final String METHOD_FLESH_PUBLIC_CONTAINER = "open-ils.actor.container.flesh";
    public static final String METHOD_CONTAINER_DELETE = "open-ils.actor.container.item.delete";
    public static final String METHOD_CONTAINER_CREATE = "open-ils.actor.container.create";
    public static final String METHOD_CONTAINER_ITEM_CREATE = "open-ils.actor.container.item.create";
    public static final String METHOD_CONTAINER_FULL_DELETE = "open-ils.actor.container.full_delete";

    /// auth

    public static final String AUTH = "open-ils.auth";
    public static final String AUTH_INIT = "open-ils.auth.authenticate.init";
    public static final String AUTH_COMPLETE = "open-ils.auth.authenticate.complete";
    public static final String AUTH_SESSION_RETRIEVE = "open-ils.auth.session.retrieve";

    /// circ

    public static final String SERVICE_CIRC = "open-ils.circ";
    /** The METHOD_FETCH_NON_CAT_CIRCS description : for a given user, returns an id-list of non-cataloged circulations that are considered open for now. A circ is open if circ time + circ duration (based on type) is > than now @returns: Array of non-catalogen circ IDs, event or error */
    public static final String METHOD_FETCH_NON_CAT_CIRCS = "open-ils.circ.open_non_cataloged_circulation.user";
    /** The METHOD_FETCH_CIRC_BY_ID description : Retrieves a circ object by ID. @returns : "circ" class. Fields of interest : renewal_remaining, due_date */
    public static final String METHOD_FETCH_CIRC_BY_ID = "open-ils.circ.retrieve";
    /** The METHOD_FETCH_MODS_FROM_COPY description : used to return info. @returns : mvr class OSRF Object. Fields of interest : title, author */
    /** The METHOD_FETCH_COPY description : used to return info for a PRE_CATALOGED object. @returns : acp class OSRF Object. Fields of interest : dummy_title, dummy_author */
    /** The METHOD_RENEW_CIRC description : used to renew a circulation object. @returnes : acn, acp, circ, mus, mbts */
    public static final String METHOD_RENEW_CIRC = "open-ils.circ.renew";
    /** The METHOD_FETCH_HOLDS. @returns: List of "ahr" OSPFObject . Fields of interest : pickup_lib */
    public static final String METHOD_FETCH_HOLDS = "open-ils.circ.holds.retrieve";
    public static final String METHOD_FETCH_HOLD_STATUS = "open-ils.circ.hold.queue_stats.retrieve";
    /** The METHOD_UPDATE_HOLD description : Updates the specified hold. If session user != hold user then session user must have UPDATE_HOLD permissions @returns : hold_is on success, event or error on failure */
    public static final String METHOD_UPDATE_HOLD = "open-ils.circ.hold.update";
    /** The METHOD_CANCEL_HOLD description : Cancels the specified hold. session user != hold user must have CANCEL_HOLD permissions. @returns : 1 on success, event or error on failure */
    public static final String METHOD_CANCEL_HOLD = "open-ils.circ.hold.cancel";
    /** The METHOD_VERIFY_HOLD_POSSIBLE description :. @returns : hashmap with "success" : 1 field or */
    public static final String METHOD_VERIFY_HOLD_POSSIBLE = "open-ils.circ.title_hold.is_possible";
    /** The METHOD_CREATE_HOLD description :. @returns : hash with messages : "success" : 1 field or */
    //bug#1506207
    public static final String METHOD_CREATE_HOLD = "open-ils.circ.holds.create";
    public static final String METHOD_TEST_AND_CREATE_HOLD = "open-ils.circ.holds.test_and_create.batch";
    /** The METHODS_FETCH_FINES_SUMMARY description :. @returns: "mous" OSRFObject. fields: balance_owed, total_owed, total_paid */
    /** The METHOD_FETCH_TRANSACTIONS description: For a given user retrieves a list of fleshed transactions. List of objects, each object is a hash containing : transaction, circ, record @returns : array of objects, must investigate */
    /** The METHOD_FETCH_MONEY_BILLING description :. */
    public static final String METHOD_FETCH_MONEY_BILLING = "open-ils.circ.money.billing.retrieve.all";

    /// fielder

    public static final String SERVICE_FIELDER = "open-ils.fielder";
    public static final String FIELDER_BMP_ATOMIC = "open-ils.fielder.bmp.atomic";

    /// pcrud

    public static final String PCRUD_SERVICE = "open-ils.pcrud";
    public static final String RETRIEVE_MRA = "open-ils.pcrud.retrieve.mra";
    public static final String SEARCH_MRA = "open-ils.pcrud.search.mra.atomic";
    public static final String SEARCH_MRAF = "open-ils.pcrud.search.mraf.atomic";

    /// search

    public static final String SEARCH = "open-ils.search";
    public static final String SEARCH_MULTICLASS_QUERY = "open-ils.search.biblio.multiclass.query";
    public static final String MODS_SLIM_RETRIEVE = "open-ils.search.biblio.record.mods_slim.retrieve";
    public static final String MODS_SLIM_BATCH = "open-ils.search.biblio.record.mods_slim.batch.retrieve.atomic";
    public static final String COPY_LOCATION_COUNTS = "open-ils.search.biblio.copy_location_counts.summary.retrieve";
    /**
     * Get copy statuses like Available, Checked_out , in_progress and others,
     * @returns: ccs
     */
    public static final String COPY_STATUS_ALL = "open-ils.search.config.copy_status.retrieve.all";
    /**
     * Get copy count information
     *
     * @param : org_unit_id, record_id
     * @returns: objects
     *           [{"transcendant":null,"count":35,"org_unit":1,"depth":0,"unshadow":35,"available":35},
     *           {"transcendant":null,"count":14,"org_unit":2,"depth":1,"unshadow":14,"available":14},
     *           {"transcendant":null,"count":7,"org_unit":4,"depth":2,"unshadow":7,"available":7}]
     */
    public static final String GET_COPY_COUNT = "open-ils.search.biblio.record.copy_count";
    public static final String METABIB_RECORD_TO_DESCRIPTORS = "open-ils.search.metabib.record_to_descriptors";
    public static final String METHOD_FETCH_VOLUME = "open-ils.search.asset.call_number.retrieve";
    public static final String METHOD_FETCH_RMODS = "open-ils.search.biblio.record.mods_slim.retrieve";
    public static final String METHOD_FETCH_MRMODS = "open-ils.search.biblio.metarecord.mods_slim.retrieve";
    public static final String METHOD_FETCH_COPY = "open-ils.search.asset.copy.retrieve";
    public static final String METHOD_FETCH_MODS_FROM_COPY = "open-ils.search.biblio.mods_from_copy";

    /// serial

    public static final String SERVICE_SERIAL = "open-ils.serial";
    public static final String METHOD_FETCH_ISSUANCE = "open-ils.serial.issuance.pub_fleshed.batch.retrieve";

}
