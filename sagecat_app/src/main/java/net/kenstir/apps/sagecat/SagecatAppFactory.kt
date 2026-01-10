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

package net.kenstir.apps.sagecat

import androidx.annotation.Keep
import net.kenstir.data.service.ServiceConfig
import net.kenstir.ui.AppBehavior
import net.kenstir.ui.AppFactory
import org.evergreen_ils.data.service.EvergreenServiceConfig

@Keep
@Suppress("unused")
class SagecatAppFactory : AppFactory() {
    override fun makeBehavior(): AppBehavior {
        return SagecatAppBehavior()
    }

    override fun makeServiceConfig(isAndroidTest: Boolean): ServiceConfig {
        return EvergreenServiceConfig()
    }
}
