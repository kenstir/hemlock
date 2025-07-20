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

import android.view.View
import android.view.ViewGroup

fun View.setMargins(
    left: Int? = null,
    top: Int? = null,
    right: Int? = null,
    bottom: Int? = null
) {
    val layoutParams = layoutParams as? ViewGroup.MarginLayoutParams ?: return

    layoutParams.setMargins(
        left ?: layoutParams.leftMargin,
        top ?: layoutParams.topMargin,
        right ?: layoutParams.rightMargin,
        bottom ?: layoutParams.bottomMargin
    )

    // Update start and end margins for RTL support
    layoutParams.marginStart = left ?: layoutParams.leftMargin
    layoutParams.marginEnd = right ?: layoutParams.rightMargin

    this.layoutParams = layoutParams
}
