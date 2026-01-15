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

package net.kenstir.mock

import net.kenstir.data.service.AuthService
import net.kenstir.data.service.BiblioService
import net.kenstir.data.service.CircService
import net.kenstir.data.service.ConsortiumService
import net.kenstir.data.service.SearchService
import net.kenstir.data.service.ServiceConfig

class MockServiceConfig : ServiceConfig {
    override val loaderService = MockLoaderService
    override val authService: AuthService
        get() = TODO("Not yet implemented")
    override val biblioService: BiblioService
        get() = TODO("Not yet implemented")
    override val circService: CircService
        get() = TODO("Not yet implemented")
    override val consortiumService: ConsortiumService
        get() = TODO("Not yet implemented")
    override val searchService: SearchService
        get() = TODO("Not yet implemented")
    override val userService = MockUserService
}
