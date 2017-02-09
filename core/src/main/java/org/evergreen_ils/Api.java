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

import org.evergreen_ils.system.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** OSRF API constants
 *
 * See also https://webby.evergreencatalog.com/opac/extras/docgen.xsl
 * for online service/method documentation.
 *
 * Created by kenstir on 9/26/2016.
 */
public class Api {

    /// actor

    public static final String ACTOR = "open-ils.actor";
    public static final String CHECKED_OUT = "open-ils.actor.user.checked_out";
    public static final String USER_FLESHED_RETRIEVE = "open-ils.actor.user.fleshed.retrieve"; // au,aua,ac,auact,cuat
    public static final String ORG_TREE_RETRIEVE = "open-ils.actor.org_tree.retrieve";
    public static final String ORG_SETTING_ANCESTOR = "open-ils.actor.ou_setting.ancestor_default";
    public static final String FINES_SUMMARY = "open-ils.actor.user.fines.summary";
    public static final String TRANSACTIONS_WITH_CHARGES = "open-ils.actor.user.transactions.have_charge.fleshed";
    public static final String CONTAINERS_BY_CLASS = "open-ils.actor.container.retrieve_by_class.authoritative"; // [cbreb]
    public static final String CONTAINER_FLESH = "open-ils.actor.container.flesh"; // [cbrebi]
    public static final String CONTAINER_ITEM_DELETE = "open-ils.actor.container.item.delete";
    public static final String CONTAINER_CREATE = "open-ils.actor.container.create";
    public static final String CONTAINER_ITEM_CREATE = "open-ils.actor.container.item.create";
    public static final String CONTAINER_FULL_DELETE = "open-ils.actor.container.full_delete";

    /// auth

    public static final String AUTH = "open-ils.auth";
    public static final String AUTH_INIT = "open-ils.auth.authenticate.init";
    public static final String AUTH_COMPLETE = "open-ils.auth.authenticate.complete";
    public static final String AUTH_SESSION_RETRIEVE = "open-ils.auth.session.retrieve"; // au

    /// circ

    public static final String SERVICE_CIRC = "open-ils.circ";
    public static final String CIRC_RETRIEVE = "open-ils.circ.retrieve"; // circ
    public static final String CIRC_RENEW = "open-ils.circ.renew";
    public static final String HOLDS_RETRIEVE = "open-ils.circ.holds.retrieve"; // [ahr]
    public static final String HOLD_QUEUE_STATS = "open-ils.circ.hold.queue_stats.retrieve";
    public static final String HOLD_UPDATE = "open-ils.circ.hold.update";
    public static final String HOLD_CANCEL = "open-ils.circ.hold.cancel";
    // bug#1506207 - do not call HOLD_CREATE w/o calling HOLD_IS_POSSIBLE; better to call HOLD_TEST_AND_CREATE
    public static final String HOLD_IS_POSSIBLE = "open-ils.circ.title_hold.is_possible";
    public static final String HOLD_CREATE = "open-ils.circ.holds.create";
    public static final String HOLD_TEST_AND_CREATE = "open-ils.circ.holds.test_and_create.batch";
    public static final String MONEY_BILLING_RETRIEVE = "open-ils.circ.money.billing.retrieve.all";

    /// fielder

    public static final String FIELDER = "open-ils.fielder";
    public static final String FIELDER_BMP_ATOMIC = "open-ils.fielder.bmp.atomic";

    /// pcrud

    public static final String PCRUD_SERVICE = "open-ils.pcrud";
    public static final String RETRIEVE_MRA = "open-ils.pcrud.retrieve.mra";
    public static final String SEARCH_MRA = "open-ils.pcrud.search.mra.atomic";
    public static final String SEARCH_MRAF = "open-ils.pcrud.search.mraf.atomic";

    /// search

    public static final String SEARCH = "open-ils.search";
    public static final String MULTICLASS_QUERY = "open-ils.search.biblio.multiclass.query";
    public static final String MODS_SLIM_RETRIEVE = "open-ils.search.biblio.record.mods_slim.retrieve";
    public static final String MODS_SLIM_BATCH = "open-ils.search.biblio.record.mods_slim.batch.retrieve.atomic";
    public static final String COPY_LOCATION_COUNTS = "open-ils.search.biblio.copy_location_counts.summary.retrieve";
    public static final String COPY_STATUS_ALL = "open-ils.search.config.copy_status.retrieve.all"; // [ccs]
    public static final String COPY_COUNT = "open-ils.search.biblio.record.copy_count";
    public static final String METABIB_RECORD_TO_DESCRIPTORS = "open-ils.search.metabib.record_to_descriptors";
    public static final String ASSET_CALL_NUMBER_RETRIEVE = "open-ils.search.asset.call_number.retrieve";
    public static final String RECORD_MODS_SLIM_RETRIEVE = "open-ils.search.biblio.record.mods_slim.retrieve";
    public static final String METARECORD_MODS_SLIM_RETRIEVE = "open-ils.search.biblio.metarecord.mods_slim.retrieve";
    public static final String ASSET_COPY_RETRIEVE = "open-ils.search.asset.copy.retrieve";
    public static final String MODS_FROM_COPY = "open-ils.search.biblio.mods_from_copy";

    /// serial

    public static final String SERVICE_SERIAL = "open-ils.serial";
    public static final String METHOD_FETCH_ISSUANCE = "open-ils.serial.issuance.pub_fleshed.batch.retrieve";

    /// misc
    public static final String ILS_VERSION = "opensrf.open-ils.system.ils_version";

    /// general

    public static final String DATE_PATTERN = "yyyy-MM-dd'T'hh:mm:ssZ";

    // get date string to pass to API methods
    public static String formatDate(Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
        return sdf.format(date);
    }

    // parse date string returned from API methods
    public static Date parseDate(String dateString) {

        if (dateString == null)
            return null;

        Date date = null;
        final SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);

        try {
            date = sdf.parse(dateString);
        } catch (ParseException e) {
            Log.d("Api", "error parsing date \""+dateString+"\"", e);
            date = new Date();
        }

        return date;
    }

    // parse bool string returned from API methods
    public static boolean parseBoolean(String boolString) {
        return (boolString != null && boolString.equals("t"));
    }
}
