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

import java.security.MessageDigest
import kotlin.random.Random

/** custom message for particular exceptional conditions
 * Not all exception messages are suitable as is for the general public
 */
fun Exception.getCustomMessage(): String {
    when (this) {
        is android.accounts.OperationCanceledException -> return "Cancelled"
        is java.util.concurrent.CancellationException -> return "Cancelled"
        is java.util.concurrent.TimeoutException -> return "Timeout"
        is java.net.SocketTimeoutException -> return "Timeout"
        is java.io.InterruptedIOException -> return "Timeout"
    }
    if (this is java.lang.IllegalStateException && cause is android.accounts.OperationCanceledException) {
        return "Cancelled"
    }
    this.message?.let { if (it.isNotEmpty()) return it }
    return "Cancelled"
}

/** returns index of item in array that matches [element], or 0 if not found
 *
 * like indexOf but with a safe default
 */
fun <T> List<T>.indexOfOrZero(element: T): Int =
    indexOf(element).takeIf { it >= 0 } ?: 0
fun <T> Array<T>.indexOfOrZero(element: T?): Int =
    indexOf(element).takeIf { it >= 0 } ?: 0

/** returns index of first item in array that matches predicate, or 0 if not found
 *
 * like indexOfFirst but with a safe default
 */
fun <T> List<T>.indexOfFirstOrZero(predicate: (T) -> Boolean): Int =
    indexOfFirst(predicate).takeIf { it >= 0 } ?: 0

/** Injects a random failure for testing purposes
 */
fun injectRandomFailure(where: String, percentChance: Int) {
    val random = Random.nextInt(100)
    if (random < percentChance) {
        throw Exception("Random failure in $where")
    }
}

/** returns the MD5 hash of the string
 */
fun String.md5(): String {

    val digest = MessageDigest.getInstance("MD5")
    digest.update(this.toByteArray())
    val messageDigest = digest.digest()

    // Create Hex String
    val hexString = StringBuilder()
    for (i in messageDigest.indices) {
        val hex = Integer.toHexString(0xFF and messageDigest[i].toInt())
        if (hex.length == 1) {
            // could use a for loop, but we're only dealing with a single byte
            hexString.append('0')
        }
        hexString.append(hex)
    }
    return hexString.toString()
}
