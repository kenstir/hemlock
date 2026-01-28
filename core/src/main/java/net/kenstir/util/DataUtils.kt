/*
 * Copyright (c) 2026 Kenneth H. Cox
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

package net.kenstir.util

import net.kenstir.data.model.CopyLocationCounts
import net.kenstir.data.service.ConsortiumService

/**
 * filter copy location counts to only those at orgs that are opacVisible
 */
fun visibleCopyLocationCounts(copyLocationCounts: List<CopyLocationCounts>, consortium: ConsortiumService): List<CopyLocationCounts> {
    return copyLocationCounts.filter { clc ->
        val org = consortium.findOrg(clc.orgId)
        // if a branch is not opac_visible, its copies should not be visible
        org != null && org.opacVisible
    }
}
