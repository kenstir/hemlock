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

package net.kenstir.hemlock.data.model

import android.location.Location

data class Library constructor(val url: String              // e.g. "https://bark.cwmars.org"
                               , val name: String           // e.g. "C/W MARS"
                               , val directoryName: String? // e.g. "Massachusetts, US (C/W MARS)"
                               , val location: Location?) {
    constructor(url: String, name: String) : this(url, name, null, null)
}
