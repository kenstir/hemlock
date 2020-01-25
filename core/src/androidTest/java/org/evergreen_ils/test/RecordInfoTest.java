/*
 * Copyright (C) 2020 Kenneth H. Cox
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

import org.evergreen_ils.searchCatalog.RecordInfo;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensrf.util.JSONException;
import org.opensrf.util.JSONReader;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RecordInfoTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
//        Log.setProvider(new StdoutLogProvider());
    }

    @Before
    public void setUp() throws Exception {
    }

    private ArrayList<RecordInfo> makeArrayFromJson(String json) throws JSONException {
        List<List<?>> idsList =(List<List<?>>) new JSONReader(json).readArray();
        return RecordInfo.makeArray(idsList);
    }

    @Test
    public void test_makeArray1() throws Exception {
        String json = "[[32673,null,\"0.0\"],[886843,null,\"0.0\"]]";
        ArrayList<RecordInfo> records = makeArrayFromJson(json);
        assertEquals(2, records.size());
        assertEquals((Integer) 32673, records.get(0).doc_id);
    }

    @Test
    public void test_makeArray2() throws Exception {
        String json = "[[\"503610\",null,\"0.0\"],[\"502717\",null,\"0.0\"]]";
        ArrayList<RecordInfo> records = makeArrayFromJson(json);
        assertEquals(2, records.size());
        assertEquals((Integer) 503610, records.get(0).doc_id);
        assertEquals((Integer) 502717, records.get(1).doc_id);
    }

    @Test
    public void test_makeArray3() throws Exception {
        String json = "[[\"1805532\"],[\"2385399\"]]";
        ArrayList<RecordInfo> records = makeArrayFromJson(json);
        assertEquals(2, records.size());
        assertEquals((Integer) 1805532, records.get(0).doc_id);
        assertEquals((Integer) 2385399, records.get(1).doc_id);
    }
}
