/*
 * Copyright (c) 2020 Kenneth H. Cox
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

/**
 * Conversion of constants from Const.pm
 */
public class Const {

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

    public static final String HOLD_TYPE_COPY        = "C";
    public static final String HOLD_TYPE_FORCE       = "F";
    public static final String HOLD_TYPE_RECALL      = "R";
    public static final String HOLD_TYPE_ISSUANCE    = "I";
    public static final String HOLD_TYPE_VOLUME      = "V";
    public static final String HOLD_TYPE_TITLE       = "T";
    public static final String HOLD_TYPE_METARECORD  = "M";
    public static final String HOLD_TYPE_MONOPART    = "P";
}
