//
//  Copyright (C) 2019 Kenneth H. Cox
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; either version 2
//  of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package org.evergreen_ils.test;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;

import org.evergreen_ils.system.Log;
import org.evergreen_ils.system.StdoutLogProvider;
import org.evergreen_ils.utils.Link;
import org.evergreen_ils.utils.MARCRecord;
import org.evergreen_ils.utils.MARCXMLParser;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

public class MARCXMLParserTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        Log.setProvider(new StdoutLogProvider());
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test_marcrecord() throws Exception {
        MARCRecord marcRecord = new MARCRecord();
        assertEquals(0, marcRecord.datafields.size());
    }

    @Test
    public void test_marcxml_partial() throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InputStream is = ctx.getResources().getAssets().open("marcxml_partial_3185816.xml");
        MARCXMLParser parser = new MARCXMLParser(is);
        assertNotNull(parser);
        MARCRecord marcRecord = parser.parse();

        // Only a subset of 856 tags are kept, see MARCXMLParser
        List<MARCRecord.MARCDatafield> datafields = marcRecord.datafields;
        assertEquals(4, datafields.size());

        // First datafield has 4 subfields
        MARCRecord.MARCDatafield df = datafields.get(0);
        List<MARCRecord.MARCSubfield> subfields = df.subfields;
        assertEquals(4, subfields.size());

        // 4 links
        List<Link> links = marcRecord.getLinks();
        assertEquals(4, links.size());

        // First link is an Excerpt
        Link link = links.get(0);
        assertEquals("Excerpt", link.text);
        assertEquals("https://samples.overdrive.com/hunger-games-c540fc?.epub-sample.overdrive.com", link.href);
    }
}