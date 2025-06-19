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
package org.evergreen_ils.utils

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import net.kenstir.hemlock.android.Analytics.logException

object BarcodeUtils {
    // Try to encode the barcode data using the given format
    // @returns BitMatrix if successful, null if not
    fun tryEncode(data: String, image_width: Int, image_height: Int, format: BarcodeFormat): BitMatrix? {
        try {
            val barcodeWriter = MultiFormatWriter()
            return barcodeWriter.encode(data, format, image_width, image_height)
        } catch (e: Exception) {
            // IllegalArgumentException happens for invalid chars in barcode, don't log that
            if (e !is IllegalArgumentException) logException(e)
            return null
        }
    }
}
