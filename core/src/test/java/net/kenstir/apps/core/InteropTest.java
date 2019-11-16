/*
 * Copyright (c) 2019 Kenneth H. Cox
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

import org.evergreen_ils.utils.Link;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// Tests for Java/Kotlin interop
public class InteropTest {
    @Test
    public void test_Link_interop() {
        Link a = new Link("http://google.com", "Link to somewhere");
        Link b = new Link("http://google.com", "Link to somewhere");
        Link c = new Link("http://google.com", "Different text same href");
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        Link d = null;
        assertFalse(b.equals(d));
    }
}
