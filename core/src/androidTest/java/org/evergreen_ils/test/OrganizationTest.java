/*
 * Copyright (C) 2017 Kenneth H. Cox
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

package org.evergreen_ils.test;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Organization;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensrf.util.OSRFObject;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class OrganizationTest {
    private static String TAG = OrganizationTest.class.getSimpleName();
    private EvergreenServer eg;

    @Before
    public void setUp() throws Exception {
        eg = EvergreenServer.getInstance();

        OSRFObject orgType = new OSRFObject();
        orgType.put("name", new String("Consortium"));
        orgType.put("id", 1);
        orgType.put("opac_label", "All PINES Libraries");
        orgType.put("can_have_users", "f");
        orgType.put("can_have_vols", "f");
        ArrayList<OSRFObject> orgTypes = new ArrayList<>();
        orgTypes.add(orgType);
        eg.loadOrgTypes(orgTypes);
    }

    @Test
    public void test_loadOrganizations() throws Exception {
        // no orgs yet
        assertEquals(0, eg.getOrganizations().size());
        assertNull(eg.getOrganization(null));
        assertNull(eg.getOrganization(1));

        // add org
        OSRFObject o = new OSRFObject();
        o.put("name", new String("Example Consortium"));
        o.put("ou_type", 1);
        o.put("opac_visible", new String("t"));
        o.put("parent_ou", null);
        o.put("id", 1);
        o.put("shortname", new String("CONS"));
        eg.loadOrganizations(o, true);

        // now we can find it
        Organization org = eg.getOrganization(1);
        assertEquals(new Integer(1), org.id);
        assertEquals("CONS", org.shortname);

        // misc
        assertEquals("Example Consortium", eg.getOrganizationName(1));
        assertEquals("", eg.getOrganizationName(2));
        assertNull(eg.getOrganization(2));
    }
}
