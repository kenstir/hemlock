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

package net.kenstir.ui.account

import android.accounts.AccountManagerFuture
import kotlinx.coroutines.delay
import java.util.concurrent.Future
import java.util.concurrent.TimeoutException

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun <T> Future<T>.await(timeoutMs: Int = 30_000): T {
    val start = System.currentTimeMillis()
    while (!isDone) {
        if (System.currentTimeMillis() - start > timeoutMs)
            throw TimeoutException()
        delay(50)
    }
    return get()
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun <T> AccountManagerFuture<T>.await(timeoutMs: Int = 30_000): T {
    val start = System.currentTimeMillis()
    while (!isDone) {
        if (System.currentTimeMillis() - start > timeoutMs)
            throw TimeoutException()
        delay(50)
    }
    return result
}
