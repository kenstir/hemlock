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


import org.opensrf.util.OSRFObject;

/** Copy summary for the given org_id, e.g. 1 available of 4 copies at MPL
 *
 * returned by open-ils.search.biblio.record.copy_count
 */
public class CopySummary {

    public Integer org_id;
    public Integer count;
    public Integer available;
    public Integer depth;
    //public Integer unshadow;

    public CopySummary(OSRFObject obj) {

        this.org_id = obj.getInt("org_unit");
        this.count = obj.getInt("count");
        this.available = obj.getInt("available");
        this.depth = obj.getInt("depth");
        //this.unshadow = obj.getInt("unshadow");
    }

}
