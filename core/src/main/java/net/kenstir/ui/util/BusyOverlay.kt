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

package net.kenstir.ui.util

import android.app.Activity
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.widget.TextViewCompat
import net.kenstir.hemlock.R

class BusyOverlay(val activity: Activity) {
    private var busyOverlay: FrameLayout? = null

    fun showOverlay(msg: String) {
        if (busyOverlay != null) return

        val rootLayout = activity.findViewById<ViewGroup>(android.R.id.content)

        // Create a container
        busyOverlay = FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setBackgroundColor("#A0000000".toColorInt()) // Semi-transparent black
            isClickable = true
            isFocusable = true

            // Create inner layout with ProgressBar and TextView
            val container = LinearLayout(activity).apply {
                layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            val progressBar = ProgressBar(activity, null, 0, R.style.HemlockCircularProgressBar).apply {
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            }
            val textView = TextView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    WRAP_CONTENT, WRAP_CONTENT).apply {
                    bottomMargin = 8
                }
                text = msg
                TextViewCompat.setTextAppearance(this, R.style.HemlockText_PagePrimary)
            }

            // Add views to container
            container.addView(textView)
            container.addView(progressBar)
            addView(container)
        }

        rootLayout.addView(busyOverlay)
    }

    fun hideOverlay() {
        val rootLayout = activity.findViewById<ViewGroup>(android.R.id.content)
        busyOverlay?.let {
            rootLayout.removeView(it)
            busyOverlay = null
        }
    }
}
