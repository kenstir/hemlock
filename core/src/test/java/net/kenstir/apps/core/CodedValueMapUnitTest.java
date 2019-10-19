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

package net.kenstir.apps.core;

import org.evergreen_ils.searchCatalog.CodedValueMap;
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.system.Organization;
import org.evergreen_ils.system.StdoutLogProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensrf.util.OSRFObject;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CodedValueMapUnitTest {
    EvergreenServer eg;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Log.setProvider(new StdoutLogProvider());
    }

    @Before
    public void setUp() throws Exception {
        ArrayList<OSRFObject> objects = new ArrayList<>();
        {
            OSRFObject obj = new OSRFObject();
            obj.put("ctype", CodedValueMap.SEARCH_FORMAT);
            obj.put("opac_visible", true);
            obj.put("code", "book");
            obj.put("value", "Book (All)");
            objects.add(obj);
        }
        {
            OSRFObject obj = new OSRFObject();
            obj.put("ctype", CodedValueMap.ICON_FORMAT);
            obj.put("opac_visible", true);
            obj.put("code", "book");
            obj.put("value", "Book");
            objects.add(obj);
        }
        CodedValueMap.loadCodedValueMaps(objects);
    }

    @Test
    public void test_basic() throws Exception {
        assertNull(CodedValueMap.getValueFromCode(CodedValueMap.SEARCH_FORMAT, "missing"));
        assertNull(CodedValueMap.getCodeFromValue(CodedValueMap.ICON_FORMAT, "Missing"));

        assertEquals("Book (All)", CodedValueMap.getValueFromCode(CodedValueMap.SEARCH_FORMAT, "book"));
        assertEquals("Book", CodedValueMap.getValueFromCode(CodedValueMap.ICON_FORMAT, "book"));
    }
}