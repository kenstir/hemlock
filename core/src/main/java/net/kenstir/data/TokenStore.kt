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

package net.kenstir.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

@Serializable
data class TokenEntry(
    val token: String,
    @SerialName("added_at") val addedAt: Long)

@Serializable
class TokenStore {
    @Serializable
    val entries = ArrayList<TokenEntry>()

    @Transient
    var currentToken: String? = null

    /**
     * True if the store has changed in any way and should be saved to storage.
     */
    @Transient
    var isModified = false

    /**
     * Adds [token] to the store, unless it's already present.
     *
     * If [token] is present, but was added more than [TOKEN_REFRESH_INTERVAL_SECONDS] ago,
     * it is moved to the end of the list with the current timestamp.
     */
    fun addCurrentToken(token: String) {
        currentToken = token
        val now = System.currentTimeMillis() / 1000

        // first remove any expired entries
        val changed = entries.removeAll { now - it.addedAt > TOKEN_EXPIRATION_SECONDS }
        if (changed) {
            isModified = true
        }

        // check if token exists and if it needs to be refreshed
        val currentEntry = entries.find { it.token == token }
        if (currentEntry != null) {
            if (now - currentEntry.addedAt > TOKEN_REFRESH_INTERVAL_SECONDS) {
                // exists but needs to be refreshed; remove and re-add
                entries.remove(currentEntry)
                pushToken(token, now)
            }
            return
        }

        pushToken(token, now)
    }

    /**
     * Adds a new token entry and trims the list if it exceeds [MAX_TOKEN_ENTRIES].
     *
     * Does not check expiration or refresh intervals.
     */
    private fun pushToken(token: String, addedAt: Long) {
        entries.add(TokenEntry(token, addedAt))
        while (entries.size > MAX_TOKEN_ENTRIES) {
            entries.removeAt(0)
        }
        isModified = true
    }

    fun encodeToString(): String {
        return Json.encodeToString(this)
    }

    companion object {
        const val MAX_TOKEN_ENTRIES = 4
        const val TOKEN_EXPIRATION_SECONDS = 86400 * 365 // 1 year
        const val TOKEN_REFRESH_INTERVAL_SECONDS = TOKEN_EXPIRATION_SECONDS / 2

        fun makeFromString(storedData: String): TokenStore {
            // if it looks like a JSON object, try to decode it
            val trimmed = storedData.trim()
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                try {
                    return Json.decodeFromString<TokenStore>(trimmed)
                } catch (_: Exception) {
                    // if it fails to decode, fall back to treating it as a plain token string
                }
            }

            val ts = TokenStore()
            ts.addCurrentToken(storedData)
            return ts
        }
    }
}
