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

package net.kenstir.ui

import androidx.annotation.Keep
import net.kenstir.data.service.ServiceConfig
import net.kenstir.mock.MockServiceConfig

@Keep
@Suppress("unused")
class TestAppFactory : AppFactory() {
    override fun makeBehavior(): AppBehavior {
        return TestAppBehavior()
    }

    override fun makeServiceConfig(isAndroidTest: Boolean): ServiceConfig {
        return MockServiceConfig()
    }
}
