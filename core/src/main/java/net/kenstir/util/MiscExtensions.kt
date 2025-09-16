/*
 * Copyright (c) 2019 Kenneth H. Cox
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package net.kenstir.util

/** custom message for particular exceptional conditions
 * Not all exception messages are suitable as is for the general public
 */
fun Exception.getCustomMessage(): String {
    when (this) {
        is java.util.concurrent.TimeoutException -> return "Timeout"
        is java.net.SocketTimeoutException -> return "Timeout"
        is java.io.InterruptedIOException -> return "Timeout"
    }
    this.message?.let { if (it.isNotEmpty()) return it }
    return "Cancelled"
}

/** returns index of item in array that matches string, or 0 if not found
 *
 * like indexOf but with a safe default
 */
fun <T> List<T>.indexOfOrZero(element: T): Int =
    indexOf(element).takeIf { it >= 0 } ?: 0
