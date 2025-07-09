/*
 * Copyright (c) 2025 Kenneth H. Cox
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
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package net.kenstir.util;

import net.kenstir.data.model.Link;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JavaToKotlinInteropTest {
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

    @Test
    public void test_IntUtils() {
        Integer I1 = null;
        Integer I2 = 2;
        int i1 = 1;
        int i2 = 2;
        assertTrue(IntUtils.equals(I2, i2));
        assertTrue(IntUtils.equals(i2, I2));
        assertFalse(IntUtils.equals(I1, i1));
        assertFalse(IntUtils.equals(i1, I1));
        assertFalse(IntUtils.equals(I2, i1));
        assertFalse(IntUtils.equals(i1, I2));
    }
}
