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
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.widget.TextViewCompat
import net.kenstir.hemlock.R

class BusyOverlay(val activity: Activity) {
    private var busyOverlay: FrameLayout? = null

    fun showOverlay(msg: String) {
        if (busyOverlay != null) return

        val rootLayout = activity.findViewById<ViewGroup>(android.R.id.content)
        val density = activity.resources.displayMetrics.density
        val dp = { value: Int -> (value * density).toInt() }

        // Helper to resolve theme attributes (colors)
        fun resolveColorAttr(attr: Int, fallback: Int): Int {
            val typedValue = TypedValue()
            return if (activity.theme.resolveAttribute(attr, typedValue, true)) {
                typedValue.data
            } else {
                ContextCompat.getColor(activity, fallback)
            }
        }

        // Use colorBackgroundFloating for the card background to match Dialogs
        val surfaceColor = resolveColorAttr(androidx.appcompat.R.attr.colorBackgroundFloating, android.R.color.white)
        val textColor = resolveColorAttr(android.R.attr.textColorPrimary, android.R.color.black)

        // Create a container
        busyOverlay = FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            // Dim the background using a semi-transparent black
            setBackgroundColor(Color.argb(0xa0, 0, 0, 0))
            isClickable = true
            isFocusable = true
            alpha = 0f

            // a card-like container for the progress bar and text
            val card = LinearLayout(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    dp(360),
                    WRAP_CONTENT,
                    Gravity.CENTER
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(24), dp(24), dp(24), dp(24))
                background = GradientDrawable().apply {
                    setColor(surfaceColor)
                    cornerRadius = dp(2).toFloat()
                }
                elevation = dp(12).toFloat()
            }

            val progressBar = ProgressBar(activity, null, 0, R.style.HemlockCircularProgressBar).apply {
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            }

            val textView = TextView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    marginStart = dp(20)
                }
                text = msg
                TextViewCompat.setTextAppearance(this, R.style.HemlockText_PagePrimary)
            }

            // Add views to container
            card.addView(progressBar)
            card.addView(textView)
            addView(card)
        }

        rootLayout.addView(busyOverlay)
        busyOverlay?.animate()?.alpha(1f)?.setDuration(200)?.start()
    }

    fun hideOverlay() {
        val rootLayout = activity.findViewById<ViewGroup>(android.R.id.content)
        busyOverlay?.let {
            rootLayout.removeView(it)
            busyOverlay = null
        }
    }
}
