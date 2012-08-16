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
package org.evergreen.android.searchCatalog;

import java.io.Serializable;
import java.util.Map;

public class CopyCountInformation implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 12343248767867L;
    public Integer org_id;
    public Integer count;
    public Integer available;
    public Integer depth;
    public Integer unshadow;

    public CopyCountInformation(Object map) {

        this.org_id = ((Map<String, Integer>) map).get("org_unit");
        this.count = ((Map<String, Integer>) map).get("count");
        this.available = ((Map<String, Integer>) map).get("available");
        this.depth = ((Map<String, Integer>) map).get("depth");
        this.unshadow = ((Map<String, Integer>) map).get("unshadow");

        System.out.println(org_id + " " + available + " " + count);
    }

}
