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

package org.evergreen_ils.data.service

import net.kenstir.data.service.ServiceConfig

class EvergreenServiceConfig : ServiceConfig {
    override val loader = EvergreenLoaderService
    override val auth = EvergreenAuthService
    override val biblio = EvergreenBiblioService
    override val circ = EvergreenCircService
    override val consortium = EvergreenConsortiumService
    override val search = EvergreenSearchService
    override val user = EvergreenUserService
}
