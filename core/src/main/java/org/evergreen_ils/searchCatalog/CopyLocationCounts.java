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


import android.text.TextUtils;
import org.evergreen_ils.system.EvergreenServer;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import kotlin.NotImplementedError;

/** Summary of copies by shelving location and status for the given org_id,
 * e.g. Adult Fiction, FIC DOERR, 3 Available, 1 Checked out
 *
 * returned by open-ils.search.biblio.copy_location_counts.summary.retrieve
 */
public class CopyLocationCounts implements Serializable {

    private static final long serialVersionUID = -7269334218707079463L;

    public Integer org_id;
    public String call_number_prefix;
    public String call_number_label;
    public String call_number_suffix;
    public String copy_location;
    public LinkedHashMap<String, String> counts_by_status;

    public CopyLocationCounts(List<Object> list) {

        org_id = Integer.parseInt((String) list.get(0));
        call_number_prefix = (String) list.get(1);
        call_number_label = (String) list.get(2);
        call_number_suffix = (String) list.get(3);
        copy_location = (String) list.get(4);
        HashMap<Integer, Integer> status_map = (HashMap<Integer, Integer>) list.get(5);

        counts_by_status = new LinkedHashMap<>();

        throw new NotImplementedError("removed getCopyStatuses");
                /*
        Set<Entry<String, String>> set = EvergreenServer.getInstance().getCopyStatuses().entrySet();
        Iterator<Entry<String, String>> it = set.iterator();
        while (it.hasNext()) {
            Entry<String, String> entry = it.next();
            if (status_map.containsKey(entry.getKey())) {
                counts_by_status.put(entry.getValue(), status_map.get(entry.getKey()) + "");
            }
        }
                 */
    }

    public List<String> getCountsByStatus() {
        ArrayList<String> statuses = new ArrayList<>();
        Set<Entry<String, String>> set = counts_by_status.entrySet();
        Iterator<Entry<String, String>> it = set.iterator();
        while (it.hasNext()) {
            Entry<String, String> ent = it.next();
            statuses.add(ent.getValue() + " " + ent.getKey());
        }
        return statuses;
    }

    public String getCallNumber() {
        String s = TextUtils.join(" ", new Object[] {call_number_prefix, call_number_label, call_number_suffix});
        return s.trim();
    }

}
