/*
 * Copyright (c) 2024 Kenneth H. Cox
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.evergreen_ils.test

import androidx.test.platform.app.InstrumentationRegistry
import net.kenstir.hemlock.android.AppBehavior
import net.kenstir.hemlock.android.Log
import net.kenstir.hemlock.android.StdoutLogProvider
import net.kenstir.hemlock.data.evergreen.XOSRFObject
import org.evergreen_ils.data.MBRecord
import net.kenstir.hemlock.data.jsonMapOf
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.utils.Link
import org.evergreen_ils.utils.MARCRecord
import org.evergreen_ils.utils.MARCXMLParser
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class TestAppBehavior: AppBehavior() {
    override fun isVisibleToOrg(df: MARCRecord.MARCDatafield, orgShortName: String): Boolean {
        return isVisibleViaLocatedURI(df, orgShortName)
    }

    override fun getOnlineLocations(record: MBRecord, orgShortName: String): List<Link> {
        return getOnlineLocationsFromMARC(record, orgShortName)
    }
}

object TestUtils {

    fun loadExampleOrgs() {
        val br1 = XOSRFObject(
            jsonMapOf(
                "id" to 4,
                "ou_type" to 3,
                "shortname" to "BR1",
                "name" to "Example Branch 1",
                "opac_visible" to "t",
                "parent_ou" to 2,
                "children" to null
            )
        )
        val br2 = XOSRFObject(
            jsonMapOf(
                "id" to 5,
                "ou_type" to 3,
                "shortname" to "BR2",
                "name" to "Example Branch 2",
                "opac_visible" to "t",
                "parent_ou" to 2,
                "children" to null
            )
        )
        val sys1 = XOSRFObject(
            jsonMapOf(
                "id" to 2,
                "ou_type" to 2,
                "shortname" to "SYS1",
                "name" to "Example System 1",
                "opac_visible" to "t",
                "parent_ou" to 1,
                "children" to arrayListOf(br1, br2)
            )
        )
        val br3 = XOSRFObject(
            jsonMapOf(
                "id" to 6,
                "ou_type" to 3,
                "shortname" to "BR3",
                "name" to "Example Branch 3",
                "opac_visible" to "t",
                "parent_ou" to 3,
                "children" to null
            )
        )
        val sys2 = XOSRFObject(
            jsonMapOf(
                "id" to 3,
                "ou_type" to 2,
                "shortname" to "SYS2",
                "name" to "Example System 2",
                "opac_visible" to "t",
                "parent_ou" to 1,
                "children" to arrayListOf(br3)
            )
        )
        val cons = XOSRFObject(
            jsonMapOf(
                "id" to 1,
                "ou_type" to 1,
                "shortname" to "CONS",
                "name" to "Example Consortium",
                "opac_visible" to "t",
                "parent_ou" to null,
                "children" to arrayListOf(sys1, sys2)
            )
        )
        EgOrg.loadOrgs(cons, true)
    }

    fun loadMARCRecord(fileBaseName: String): MARCRecord {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val inStream = ctx.resources.assets.open(fileBaseName)
        val parser = MARCXMLParser(inStream)
        return parser.parse()
    }

}

class AppBehaviorTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            Log.setProvider(StdoutLogProvider())
        }
    }

    @Before
    fun setUp() {
        TestUtils.loadExampleOrgs()
    }

    fun printLinks(links: List<Link>) {
        print("${links.size} links:")
        for (link in links) {
            print("  [${link.text}] (${link.href})")
        }
    }

    @Test
    fun test_getLinksFromRecordWithConsortiumInSubfield9() {
        val appBehavior = TestAppBehavior()
        val marcRecord = TestUtils.loadMARCRecord("marcxml_ebook_1_cons.xml")

        // subfield 9 has CONS which is an ancestor of everything
        val linksForBR1 = appBehavior.getLinksFromMARCRecord(marcRecord, "BR1")
        printLinks(linksForBR1)
        assertEquals(1, linksForBR1.size)
        assertEquals("Click to access online", linksForBR1.firstOrNull()?.text)
        assertEquals("http://example.com/ebookapi/t/001", linksForBR1.firstOrNull()?.href)
        val linksForSYS1 = appBehavior.getLinksFromMARCRecord(marcRecord, "SYS1")
        printLinks(linksForSYS1)
        assertEquals(1, linksForSYS1.size)
        assertEquals("Click to access online", linksForSYS1.firstOrNull()?.text)
        assertEquals("http://example.com/ebookapi/t/001", linksForSYS1.firstOrNull()?.href)
    }

    @Test
    fun test_getLinksFromRecordWithTwo856Tags() {
        val appBehavior = TestAppBehavior()
        val marcRecord = TestUtils.loadMARCRecord("marcxml_ebook_2_two_856_tags.xml")

        // this record has 2 856 tags, one with BR1 and one with BR2
        val linksForBR1 = appBehavior.getLinksFromMARCRecord(marcRecord, "BR1")
        printLinks(linksForBR1)
        assertEquals(1, linksForBR1.size)
        assertEquals("Access for Branch 1 patrons only", linksForBR1.firstOrNull()?.text)
        assertEquals("http://example.com/ebookapi/t/002", linksForBR1.firstOrNull()?.href)
        val linksForBR2 = appBehavior.getLinksFromMARCRecord(marcRecord, "BR2")
        printLinks(linksForBR2)
        assertEquals(1, linksForBR2.size)
        assertEquals("Access for Branch 2 patrons only", linksForBR2.firstOrNull()?.text)
        assertEquals("http://example.com/ebookapi/t/002", linksForBR2.firstOrNull()?.href)
        val linksForSYS1 = appBehavior.getLinksFromMARCRecord(marcRecord, "SYS1")
        assertEquals(0, linksForSYS1.size)
    }

    @Test
    fun test_getLinksFromRecordWithTwoSubfield9s() {
        val appBehavior = TestAppBehavior()
        val marcRecord = TestUtils.loadMARCRecord("marcxml_ebook_2_two_subfield_9s.xml")

        // this record has 2 subfield 9s, with BR1 and BR2
        val linksForBR1 = appBehavior.getLinksFromMARCRecord(marcRecord, "BR1")
        printLinks(linksForBR1)
        assertEquals(1, linksForBR1.size)
        assertEquals("Access for Branch 1 or Branch 2 patrons", linksForBR1.firstOrNull()?.text)
        assertEquals("http://example.com/ebookapi/t/002", linksForBR1.firstOrNull()?.href)
        val linksForBR2 = appBehavior.getLinksFromMARCRecord(marcRecord, "BR1")
        printLinks(linksForBR2)
        assertEquals(1, linksForBR2.size)
        assertEquals("Access for Branch 1 or Branch 2 patrons", linksForBR2.firstOrNull()?.text)
        assertEquals("http://example.com/ebookapi/t/002", linksForBR2.firstOrNull()?.href)
        val linksForSYS1 = appBehavior.getLinksFromMARCRecord(marcRecord, "SYS1")
        assertEquals(0, linksForSYS1.size)
    }

    @Test
    fun test_getLinksFromRecordWithRelatedResource() {
        val appBehavior = TestAppBehavior()
        val marcRecord = TestUtils.loadMARCRecord("marcxml_item_with_eresource.xml")

        val linksForCONS = appBehavior.getLinksFromMARCRecord(marcRecord, "CONS")
        printLinks(linksForCONS)
        assertEquals(1, linksForCONS.size)
        assertEquals("user manual:", linksForCONS.firstOrNull()?.text)
    }
}
