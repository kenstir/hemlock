/*
 * Copyright (C) 2013 Evergreen Open-ILS
 * @author kenstir
 */
package org.evergreen_ils.globals;

/**
 * Conversion of constants from Const.pm
 */
class EvergreenConstants {

    // Copy Statuses
    public static final int COPY_STATUS_AVAILABLE     = 0;
    public static final int COPY_STATUS_CHECKED_OUT   = 1;
    public static final int COPY_STATUS_BINDERY       = 2;
    public static final int COPY_STATUS_LOST          = 3;
    public static final int COPY_STATUS_MISSING       = 4;
    public static final int COPY_STATUS_IN_PROCESS    = 5;
    public static final int COPY_STATUS_IN_TRANSIT    = 6;
    public static final int COPY_STATUS_RESHELVING    = 7;
    public static final int COPY_STATUS_ON_HOLDS_SHELF= 8;
    public static final int COPY_STATUS_ON_ORDER      = 9;
    public static final int COPY_STATUS_ILL           = 10;
    public static final int COPY_STATUS_CATALOGING    = 11;
    public static final int COPY_STATUS_RESERVES      = 12;
    public static final int COPY_STATUS_DISCARD       = 13;
    public static final int COPY_STATUS_DAMAGED       = 14;
    public static final int COPY_STATUS_ON_RESV_SHELF = 15;

    // Circ defaults for pre-cataloged copies
    public static final int PRECAT_COPY_FINE_LEVEL    = 2;
    public static final int PRECAT_COPY_LOAN_DURATION = 2;
    public static final int PRECAT_CALL_NUMBER        = -1;
    public static final int PRECAT_RECORD             = -1;

    // Circ constants
    public static final int CIRC_DURATION_SHORT       = 1;
    public static final int CIRC_DURATION_NORMAL      = 2;
    public static final int CIRC_DURATION_EXTENDED    = 3;
    public static final int REC_FINE_LEVEL_LOW        = 1;
    public static final int REC_FINE_LEVEL_NORMAL     = 2;
    public static final int REC_FINE_LEVEL_HIGH       = 3;
    public static final String STOP_FINES_CHECKIN        = "CHECKIN";
    public static final String STOP_FINES_RENEW          = "RENEW";
    public static final String STOP_FINES_LOST           = "LOST";
    public static final String STOP_FINES_CLAIMSRETURNED = "CLAIMSRETURNED";
    public static final String STOP_FINES_LONGOVERDUE    = "LONGOVERDUE";
    public static final String STOP_FINES_MAX_FINES      = "MAXFINES";
    public static final String STOP_FINES_CLAIMS_NEVERCHECKEDOUT = "CLAIMSNEVERCHECKEDOUT";
    public static final String UNLIMITED_CIRC_DURATION   = "unlimited";

    // Settings
    public static final String SETTING_LOST_PROCESSING_FEE = "circ.lost_materials_processing_fee";
    public static final String SETTING_DEF_ITEM_PRICE = "cat.default_item_price";
    public static final String SETTING_ORG_BOUNCED_EMAIL = "org.bounced_emails";
    public static final String SETTING_CHARGE_LOST_ON_ZERO = "circ.charge_lost_on_zero";
    public static final String SETTING_VOID_OVERDUE_ON_LOST = "circ.void_overdue_on_lost";
    public static final String SETTING_HOLD_SOFT_STALL = "circ.hold_stalling.soft";
    public static final String SETTING_HOLD_HARD_STALL = "circ.hold_stalling.hard";
    public static final String SETTING_HOLD_SOFT_BOUNDARY = "circ.hold_boundary.soft";
    public static final String SETTING_HOLD_HARD_BOUNDARY = "circ.hold_boundary.hard";
    public static final String SETTING_HOLD_EXPIRE = "circ.hold_expire_interval";
    public static final String SETTING_HOLD_ESIMATE_WAIT_INTERVAL = "circ.holds.default_estimated_wait_interval";
    public static final String SETTING_VOID_LOST_ON_CHECKIN                = "circ.void_lost_on_checkin";
    public static final String SETTING_MAX_ACCEPT_RETURN_OF_LOST           = "circ.max_accept_return_of_lost";
    public static final String SETTING_VOID_LOST_PROCESS_FEE_ON_CHECKIN    = "circ.void_lost_proc_fee_on_checkin";
    public static final String SETTING_RESTORE_OVERDUE_ON_LOST_RETURN      = "circ.restore_overdue_on_lost_return";
    public static final String SETTING_LOST_IMMEDIATELY_AVAILABLE          = "circ.lost_immediately_available";
    public static final String SETTING_BLOCK_HOLD_FOR_EXPIRED_PATRON       = "circ.holds.expired_patron_block";
    public static final String SETTING_GENERATE_OVERDUE_ON_LOST_RETURN     = "circ.lost.generate_overdue_on_checkin";

    public static final String HOLD_TYPE_COPY        = "C";
    public static final String HOLD_TYPE_FORCE       = "F";
    public static final String HOLD_TYPE_RECALL      = "R";
    public static final String HOLD_TYPE_ISSUANCE    = "I";
    public static final String HOLD_TYPE_VOLUME      = "V";
    public static final String HOLD_TYPE_TITLE       = "T";
    public static final String HOLD_TYPE_METARECORD  = "M";
    public static final String HOLD_TYPE_MONOPART    = "P";

    public static final String BILLING_TYPE_OVERDUE_MATERIALS = "Overdue materials";
    public static final String BILLING_TYPE_COLLECTION_FEE = "Long Overdue Collection Fee";
    public static final String BILLING_TYPE_DEPOSIT = "System: Deposit";
    public static final String BILLING_TYPE_RENTAL = "System: Rental";
    public static final String BILLING_NOTE_SYSTEM = "SYSTEM GENERATED";

    public static final String ACQ_DEBIT_TYPE_PURCHASE = "purchase";
    public static final String ACQ_DEBIT_TYPE_TRANSFER = "xfer";

    // all penalties with ID < 100 are managed automatically
    public static final int PENALTY_AUTO_ID = 100;
    public static final int PENALTY_PATRON_EXCEEDS_FINES = 1;
    public static final int PENALTY_PATRON_EXCEEDS_OVERDUE_COUNT = 2;
    public static final int PENALTY_INVALID_PATRON_ADDRESS = 29;

    public static final int BILLING_TYPE_NOTIFICATION_FEE = 9;
}
