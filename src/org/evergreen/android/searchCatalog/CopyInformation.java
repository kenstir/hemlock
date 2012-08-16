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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class CopyInformation implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = -7269334218707079463L;

    public Integer org_id = -1;

    public String call_number_sufix;

    public String copy_location;

    public HashMap<String, String> statuses;

    // the hash from the request method
    private HashMap<Integer, Integer> hashValCopy;

    // global, it is initialized when orgTree and fm_ild is downloaded

    public static LinkedHashMap<String, String> availableOrgStatuses;

    public LinkedHashMap<String, String> statusInformation = null;

    public CopyInformation(List<Object> list) {

        org_id = Integer.parseInt((String) list.get(0));
        call_number_sufix = (String) list.get(2);
        copy_location = (String) list.get(4);

        hashValCopy = (HashMap<Integer, Integer>) list.get(5);

        statusInformation = new LinkedHashMap<String, String>();

        Set<Entry<String, String>> set = availableOrgStatuses.entrySet();

        Iterator<Entry<String, String>> it = set.iterator();

        while (it.hasNext()) {
            Entry<String, String> entry = it.next();

            if (hashValCopy.containsKey(entry.getKey())) {
                statusInformation.put(entry.getValue(),
                        hashValCopy.get(entry.getKey()) + "");
                System.out.println("Added " + entry.getKey() + " "
                        + entry.getValue() + " "
                        + hashValCopy.get(entry.getKey()));
            }
        }
    }

}
