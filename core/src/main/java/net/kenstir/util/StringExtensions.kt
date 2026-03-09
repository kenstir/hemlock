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

import android.net.Uri
import java.security.MessageDigest
import java.util.regex.Matcher.quoteReplacement
import java.util.regex.Pattern

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

/**
 * Returns a string with tokens of the form `{key}` replaced with the corresponding value from [values].
 *
 * It is an error if a key in the template is not present in the map.
 */
fun String.expandTemplate(values: Map<String, String>): String {
    var result = this

    // raw tokens {name}
    val rawPattern = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}")
    val mr = rawPattern.matcher(result)
    val sbr = StringBuffer()
    while (mr.find()) {
        val key = mr.group(1)
        val replacement = values[key] ?: throw IllegalArgumentException("No value for key '$key'")
        mr.appendReplacement(sbr, quoteReplacement(replacement))
    }
    mr.appendTail(sbr)
    result = sbr.toString()

    return result
}
