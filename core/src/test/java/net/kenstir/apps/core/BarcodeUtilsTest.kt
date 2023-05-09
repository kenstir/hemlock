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
package net.kenstir.apps.core

import com.google.zxing.BarcodeFormat
import org.evergreen_ils.utils.BarcodeUtils
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeUtilsTest {
    @Test
    fun test_encodingCodabar() {
        assertNotNull(BarcodeUtils.tryEncode("55555000001234", width, height, BarcodeFormat.CODABAR))
        assertNotNull(BarcodeUtils.tryEncode("11611", width, height, BarcodeFormat.CODABAR))
        assertNull(BarcodeUtils.tryEncode("D782515578", width, height, BarcodeFormat.CODABAR))
    }

    @Test
    fun test_encodingCode39() {
        assertNotNull(BarcodeUtils.tryEncode("55555000001234", width, height, BarcodeFormat.CODE_39))
        assertNotNull(BarcodeUtils.tryEncode("11611", width, height, BarcodeFormat.CODE_39))
        assertNotNull(BarcodeUtils.tryEncode("D782515578", width, height, BarcodeFormat.CODE_39))
    }

    companion object {
        var width = 400
        var height = 200
    }
}
