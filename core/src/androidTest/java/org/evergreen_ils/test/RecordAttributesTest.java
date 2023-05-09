/*
 * Copyright (C) 2019 Kenneth H. Cox
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

import org.evergreen_ils.android.Log;
import org.evergreen_ils.android.StdoutLogProvider;
import org.evergreen_ils.utils.RecordAttributes;
import org.evergreen_ils.utils.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RecordAttributesTest {

    @BeforeClass
    public static void setUpClass() {
        Log.setProvider(new StdoutLogProvider());
    }

    @Test
    public void test_basic() {
        String attrs = "'item_form'=>' ', 'item_type'=>'a', 'icon_format'=>'book', 'content_type'=>'still image', 'search_format'=>'book', 'mr_hold_format'=>'book'".replace("'", "\"");
        Map<String, String> map = RecordAttributes.parseAttributes(attrs);
        assertEquals(6, map.size());
        assertEquals("book", map.get("icon_format"));
        assertEquals(" ", map.get("item_form"));
        assertEquals(null, map.get("xyzzy"));
    }

    @Test
    public void test_xxxxx() {
        String s1 = null;
        assertEquals("", StringUtils.take(s1, 4));
        String s2 = "abcdef";
        assertEquals("abcd", StringUtils.take(s2, 4));
        assertEquals(s2, StringUtils.take(s2, 6));
        assertEquals(s2, StringUtils.take(s2, 8));
    }
}
