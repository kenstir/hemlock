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
package net.kenstir.ui

import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.Link

/** AppBehavior - customizable app behaviors
 *
 * to override, create a subclass of AppBehavior in the xxx_app module,
 * and specify the name of that class in R.string.ou_behavior_provider.
 */
open class AppBehavior {
    open fun isOnlineResource(record: BibRecord?): Boolean? {
        throw NotImplementedError("isOnlineResource must be overridden in subclass")
    }

    open fun getOnlineLocations(record: BibRecord, orgShortName: String): List<Link> {
        throw NotImplementedError("getOnlineLocations must be overridden in subclass")
    }
}
