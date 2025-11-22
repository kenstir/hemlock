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

package net.kenstir.apps.owwl

import androidx.annotation.Keep
import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.Link
import net.kenstir.ui.AppBehavior

@Keep
@Suppress("unused")
class OwwlAppBehavior : AppBehavior() {
    override fun trimLinkTitle(s: String): String {
        return trimTrailing(s, '.').trim()
    }

    override fun getOnlineLocations(record: BibRecord, orgShortName: String): List<Link> {
        return getOnlineLocationsFromMARC(record, orgShortName)
    }
}
