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
package net.kenstir.ui.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import net.kenstir.hemlock.R
import net.kenstir.ui.App
import net.kenstir.util.BarcodeUtils
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.util.compatEnableEdgeToEdge

class BarcodeActivity : BaseActivity() {
    private var barcodeText: TextView? = null
    private var barcodeWarning: TextView? = null
    private var imageView: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        compatEnableEdgeToEdge()
        setContentView(R.layout.activity_barcode)
        setupActionBar()
        adjustPaddingForEdgeToEdge()
        setupNavigationDrawer()

        barcodeText = findViewById(R.id.barcode_text)
        barcodeWarning = findViewById(R.id.barcode_warning)
        imageView = findViewById(R.id.barcode_image)

        initBarcodeViews()
    }

    override fun onResume() {
        super.onResume()

        // Increase screenBrightness to max to make barcode scanning easier
        val layout = window.attributes
        layout.screenBrightness = 1f
        window.attributes = layout
    }

    // get lesser of display width or height
    private fun getDisplayWidth(): Int {
        /* this is the Right Way according to
           https://stackoverflow.com/questions/63407883/getting-screen-width-on-api-level-30-android-11-getdefaultdisplay-and-getme
           but the old deprecated way is working for now and is far less code
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            // Gets all excluding insets
            val windowInsets = metrics.windowInsets
            val insets = windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.navigationBars()
                        or WindowInsets.Type.displayCutout()
            )

            val insetsWidth = insets.right + insets.left
            val insetsHeight = insets.top + insets.bottom

            // Legacy size that Display#getSize reports
            val bounds = metrics.bounds
            return minOf(bounds.width() - insetsWidth, bounds.height() - insetsHeight, 480)
        }
        */
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        return minOf(metrics.widthPixels, metrics.heightPixels, 480)
    }

    private fun initBarcodeViews() {
        val barcode = App.getAccount().barcode
        val imageWidth = getDisplayWidth() * 8 / 10
        val imageHeight = imageWidth * 4 / 10
        val bitmap = createBitmap(barcode, imageWidth, imageHeight)
        if (bitmap != null) {
            barcodeText?.text = barcode
            imageView?.setImageBitmap(bitmap)
        } else {
            barcodeText?.text = getString(R.string.invalid_barcode, barcode)
            imageView?.setImageResource(R.drawable.invalid_barcode)
        }

        if (resources.getBoolean(R.bool.ou_enable_barcode_expiration)) {
            val date = App.getAccount().expireDateString
            barcodeWarning?.text = resources.getString(R.string.barcode_expires_msg, date)
        } else {
            barcodeWarning?.visibility = View.GONE
        }

        barcodeText?.setOnClickListener { copyBarcodeToClipboard() }
        imageView?.setOnClickListener { copyBarcodeToClipboard() }
    }

    private fun createBitmap(data: String?, imageWidth: Int, imageHeight: Int): Bitmap? {
        if (imageWidth <= 0 || imageHeight <= 0) return null
        if (data.isNullOrEmpty()) return null

        // Try formats until we successfully encode the data
        var bitMatrix: BitMatrix? = null
        if (bitMatrix == null) bitMatrix = BarcodeUtils.tryEncode(data, imageWidth, imageHeight, BarcodeFormat.CODABAR)
        if (bitMatrix == null) bitMatrix = BarcodeUtils.tryEncode(data, imageWidth, imageHeight, BarcodeFormat.CODE_39)
        if (bitMatrix == null) return null

        // Create a Bitmap from the BitMatrix
        // NB: use width/Height from bitMatrix; they may be larger than imageWidth/Height
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
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
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.msg_barcode_copied), Toast.LENGTH_SHORT).show()
    }
}
