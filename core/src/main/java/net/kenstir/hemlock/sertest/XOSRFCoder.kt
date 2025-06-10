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

package net.kenstir.hemlock.sertest

/*** [OSRFCoder] decodes OSRF objects from OpenSRF wire format. */
class XOSRFCoder(val netClass: String, val fields: List<String>) {


    companion object {
        var registry: HashMap<String, XOSRFCoder> = HashMap()

        fun clearRegistry() {
            registry.clear()
        }

        fun registerCoder(netClass: String, fields: List<String>) {
            registry[netClass] = XOSRFCoder(netClass, fields)
        }

        fun getCoder(netClass: String): XOSRFCoder? {
            return registry[netClass]
        }
    }
}
