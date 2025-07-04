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

package net.kenstir.hemlock.net

import org.evergreen_ils.net.EvergreenAuthService
import org.evergreen_ils.net.EvergreenBiblioService
import org.evergreen_ils.net.EvergreenCircService
import org.evergreen_ils.net.EvergreenLoaderService
import org.evergreen_ils.net.EvergreenOrgService
import org.evergreen_ils.net.EvergreenSearchService
import org.evergreen_ils.net.EvergreenUserService
import org.evergreen_ils.net.OrgService

class ServiceConfig(
    val loaderService: LoaderService = EvergreenLoaderService(),
    val authService: AuthService = EvergreenAuthService(),
    val biblioService: BiblioService = EvergreenBiblioService,
    val circService: CircService = EvergreenCircService(),
    val searchService: SearchService = EvergreenSearchService(),
    val userService: UserService = EvergreenUserService(),
    val orgService: OrgService = EvergreenOrgService,
)
