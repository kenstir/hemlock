/*
 * Copyright (c) 2025 Kenneth H. Cox
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
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */
package org.evergreen_ils

/** OSRF API constants
 */
object Api {
    const val ANONYMOUS = "ANONYMOUS"
    const val IDL_CLASSES_USED = "ac,acn,acp,aec,aecs,ahr,ahrn,ahtc,aoa,aou,aoucd,aouhoo,aout,au,aua,auact,auch,aum,aus,bmp,bre,cbreb,cbrebi,cbrebin,cbrebn,ccs,ccvm,cfg,circ,csc,cuat,ex,mbt,mbts,mous,mra,mraf,mus,mvr,perm_ex"

    /// actor

    const val ACTOR = "open-ils.actor"
    const val CHECKED_OUT = "open-ils.actor.user.checked_out"
    const val CHECKOUT_HISTORY = "open-ils.actor.history.circ"
    const val CLEAR_CHECKOUT_HISTORY = "open-ils.actor.history.circ.clear"
    const val USER_FLESHED_RETRIEVE = "open-ils.actor.user.fleshed.retrieve" // au,aua,ac,auact,cuat
    const val ORG_TREE_RETRIEVE = "open-ils.actor.org_tree.retrieve"
    const val ORG_TYPES_RETRIEVE = "open-ils.actor.org_types.retrieve"
    const val ORG_UNIT_RETRIEVE = "open-ils.actor.org_unit.retrieve"
    const val ORG_UNIT_SETTING_RETRIEVE = "open-ils.actor.org_unit_setting.values.ranged.retrieve"
    const val ORG_UNIT_SETTING_BATCH = "open-ils.actor.ou_setting.ancestor_default.batch"
    const val ORG_UNIT_SETTING = "open-ils.actor.ou_setting.ancestor_default"
    const val PATRON_SETTINGS_UPDATE = "open-ils.actor.patron.settings.update"
    const val FINES_SUMMARY = "open-ils.actor.user.fines.summary"
    const val TRANSACTIONS_WITH_CHARGES = "open-ils.actor.user.transactions.have_charge.fleshed"
    const val CONTAINERS_BY_CLASS = "open-ils.actor.container.retrieve_by_class.authoritative" // [cbreb]
    const val CONTAINER_FLESH = "open-ils.actor.container.flesh" // [cbrebi]
    const val CONTAINER_ITEM_DELETE = "open-ils.actor.container.item.delete"
    const val CONTAINER_CREATE = "open-ils.actor.container.create"
    const val CONTAINER_ITEM_CREATE = "open-ils.actor.container.item.create"
    const val CONTAINER_FULL_DELETE = "open-ils.actor.container.full_delete"
    const val CONTAINER_CLASS_BIBLIO = "biblio"
    const val CONTAINER_BUCKET_TYPE_BOOKBAG = "bookbag"
    const val MESSAGES_RETRIEVE = "open-ils.actor.message.retrieve" // [aum]
    const val HOURS_OF_OPERATION_RETRIEVE = "open-ils.actor.org_unit.hours_of_operation.retrieve" // [aouhoo]
    const val HOURS_CLOSED_RETRIEVE = "open-ils.actor.org_unit.closed.retrieve.all"
    const val ADDRESS_RETRIEVE = "open-ils.actor.org_unit.address.retrieve" // [aoa]
    //const val VITAL_STATS = "open-ils.actor.user.opac.vital_stats"; // Used by OPAC for summary stats

    /// Hemlock-specific settings

    const val SETTING_HEMLOCK_CACHE_KEY = "hemlock.cache_key"
    const val SETTING_HEMLOCK_ERESOURCES_URL = "hemlock.eresources_url"
    const val SETTING_HEMLOCK_EVENTS_URL = "hemlock.events_calendar_url"
    const val SETTING_HEMLOCK_MEETING_ROOMS_URL = "hemlock.meeting_rooms_url"
    const val SETTING_HEMLOCK_MUSEUM_PASSES_URL = "hemlock.museum_passes_url"
    const val USER_SETTING_HEMLOCK_PUSH_NOTIFICATION_DATA = "hemlock.push_notification_data"
    const val USER_SETTING_HEMLOCK_PUSH_NOTIFICATION_ENABLED = "hemlock.push_notification_enabled"

    /// org and user settings

    const val SETTING_REQUIRE_MONOGRAPHIC_PART = "circ.holds.api_require_monographic_part_when_present"
    const val SETTING_UI_REQUIRE_MONOGRAPHIC_PART = "circ.holds.ui_require_monographic_part_when_present"
    const val SETTING_CREDIT_PAYMENTS_ALLOW = "credit.payments.allow"
    const val SETTING_INFO_URL = "lib.info_url"
    const val SETTING_OPAC_ALERT_BANNER_SHOW = "opac.alert_banner_show" // bool
    const val SETTING_OPAC_ALERT_BANNER_TEXT = "opac.alert_banner_text"
    const val SETTING_OPAC_ALERT_BANNER_TYPE = "opac.alert_banner_type" // success|info|warning|danger - unused
    const val SETTING_ORG_UNIT_NOT_PICKUP_LIB = "opac.holds.org_unit_not_pickup_lib"
    const val SETTING_SMS_ENABLE = "sms.enable"
    const val USER_SETTING_CIRC_HISTORY_START = "history.circ.retention_start"
    const val USER_SETTING_HOLD_HISTORY_START = "history.hold.retention_start"
    const val USER_SETTING_HOLD_NOTIFY = "opac.hold_notify" // e.g. "email|sms" or "phone:email"
    const val USER_SETTING_DEFAULT_PHONE = "opac.default_phone"
    const val USER_SETTING_DEFAULT_PICKUP_LOCATION = "opac.default_pickup_location"
    const val USER_SETTING_DEFAULT_SEARCH_LOCATION = "opac.default_search_location"
    const val USER_SETTING_DEFAULT_SMS_CARRIER = "opac.default_sms_carrier"
    const val USER_SETTING_DEFAULT_SMS_NOTIFY = "opac.default_sms_notify"

    /// auth

    const val AUTH = "open-ils.auth"
    const val AUTH_INIT = "open-ils.auth.authenticate.init"
    const val AUTH_COMPLETE = "open-ils.auth.authenticate.complete"
    const val AUTH_SESSION_RETRIEVE = "open-ils.auth.session.retrieve" // au
    const val AUTH_SESSION_DELETE = "open-ils.auth.session.delete"

    /// circ

    const val CIRC = "open-ils.circ"
    const val CIRC_RETRIEVE = "open-ils.circ.retrieve" // circ
    const val CIRC_RENEW = "open-ils.circ.renew"
    const val HOLDS_RETRIEVE = "open-ils.circ.holds.retrieve" // [ahr]
    const val HOLD_QUEUE_STATS = "open-ils.circ.hold.queue_stats.retrieve"
    const val HOLD_UPDATE = "open-ils.circ.hold.update"
    const val HOLD_CANCEL = "open-ils.circ.hold.cancel"
    const val HOLD_TEST_AND_CREATE = "open-ils.circ.holds.test_and_create.batch"
    const val HOLD_TEST_AND_CREATE_OVERRIDE = "open-ils.circ.holds.test_and_create.batch.override"
    const val TITLE_HOLD_IS_POSSIBLE = "open-ils.circ.title_hold.is_possible"

    /// fielder

    const val FIELDER = "open-ils.fielder"
    const val FIELDER_BMP_ATOMIC = "open-ils.fielder.bmp.atomic"

    /// pcrud

    const val PCRUD = "open-ils.pcrud"
    const val RETRIEVE_BRE = "open-ils.pcrud.retrieve.bre"
    const val RETRIEVE_MRA = "open-ils.pcrud.retrieve.mra"
    const val SEARCH_CCVM = "open-ils.pcrud.search.ccvm.atomic"
    const val SEARCH_SMS_CARRIERS = "open-ils.pcrud.search.csc.atomic" // [csc]

    /// search

    const val SEARCH = "open-ils.search"
    const val MULTICLASS_QUERY = "open-ils.search.biblio.multiclass.query"
    const val MODS_SLIM_RETRIEVE = "open-ils.search.biblio.record.mods_slim.retrieve"
    const val COPY_LOCATION_COUNTS = "open-ils.search.biblio.copy_location_counts.summary.retrieve"
    const val COPY_STATUS_ALL = "open-ils.search.config.copy_status.retrieve.all" // [ccs]
    const val COPY_COUNT = "open-ils.search.biblio.record.copy_count"
    const val ASSET_CALL_NUMBER_RETRIEVE = "open-ils.search.asset.call_number.retrieve"
    const val METARECORD_MODS_SLIM_RETRIEVE = "open-ils.search.biblio.metarecord.mods_slim.retrieve"
    const val ASSET_COPY_RETRIEVE = "open-ils.search.asset.copy.retrieve"
    const val MODS_FROM_COPY = "open-ils.search.biblio.mods_from_copy"
    const val HOLD_PARTS = "open-ils.search.biblio.record_hold_parts"

    /// serial

    //const val SERVICE_SERIAL = "open-ils.serial"
    //const val METHOD_FETCH_ISSUANCE = "open-ils.serial.issuance.pub_fleshed.batch.retrieve"

    /// misc

    const val ILS_VERSION = "opensrf.open-ils.system.ils_version"
}
