/*
 * Copyright (C) 2018 Kenneth H. Cox
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
package org.evergreen_ils.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import org.evergreen_ils.R
import org.evergreen_ils.android.App
import org.evergreen_ils.utils.BarcodeUtils
import org.evergreen_ils.utils.ui.BaseActivity

class BarcodeActivity : BaseActivity() {
    private var barcode_text: TextView? = null
    private var image_view: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.activity_barcode)

        barcode_text = findViewById(R.id.barcode_text)
        image_view = findViewById(R.id.barcode_image)

        initBarcodeViews()
    }

    private fun initBarcodeViews() {
        val barcode = App.getAccount().barcode
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val imageWidth = Math.min(metrics.widthPixels, metrics.heightPixels) * 8 / 10
        val imageHeight = imageWidth * 4 / 10
        val bitmap = createBitmap(barcode, imageWidth, imageHeight)
        if (bitmap != null) {
            barcode_text?.text = barcode
            image_view?.setImageBitmap(bitmap)
        } else {
            barcode_text?.text = getString(R.string.invalid_barcode, barcode)
            image_view?.setImageResource(R.drawable.invalid_barcode)
        }

        barcode_text?.setOnClickListener { copyBarcodeToClipboard() }
        image_view?.setOnClickListener { copyBarcodeToClipboard() }
    }

    private fun createBitmap(data: String?, image_width: Int, image_height: Int): Bitmap? {
        if (image_width <= 0 || image_height <= 0) return null

        // Try formats until we successfully encode the data
        var bitMatrix: BitMatrix? = null
        if (bitMatrix == null) bitMatrix = BarcodeUtils.tryEncode(data, image_width, image_height, BarcodeFormat.CODABAR)
        if (bitMatrix == null) bitMatrix = BarcodeUtils.tryEncode(data, image_width, image_height, BarcodeFormat.CODE_39)
        if (bitMatrix == null) return null

        // Create a Bitmap from the BitMatrix
        val bitmap = Bitmap.createBitmap(image_width, image_height, Bitmap.Config.ARGB_8888)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun copyBarcodeToClipboard() {
        val clipboard =  getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.label_barcode), App.getAccount().barcode)
        clipboard.primaryClip = clip
        Toast.makeText(this, getString(R.string.msg_barcode_copied), Toast.LENGTH_SHORT).show()
    }
}
