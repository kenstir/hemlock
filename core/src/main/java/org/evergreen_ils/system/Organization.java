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
package org.evergreen_ils.system;

import org.evergreen_ils.barcodescan.Intents;

import java.io.Serializable;

public class Organization /*implements Serializable*/ {

    public Integer level = null;
    public Integer id = null;
    public Integer parent_ou = null;
    public String name = null;
    public String shortname = null;
    public OrgType orgType = null;
    public String indentedDisplayPrefix = "";

    public Boolean opac_visible = null;
    public Boolean is_pickup_location = null; // null=not loaded

    public Organization() {
    }

    public String getTreeDisplayName() {
        return indentedDisplayPrefix + name;
    }

    public boolean isPickupLocation() {
        if (is_pickup_location != null)
            return is_pickup_location;
        return defaultIsPickupLocation();
    }

    public boolean defaultIsPickupLocation() {
        if (orgType == null)
            return true;//should not happen
        return orgType.can_have_vols;
    }
}
