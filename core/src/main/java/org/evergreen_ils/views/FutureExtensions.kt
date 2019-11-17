// courtesy of Matt Klein
// https://stackoverflow.com/questions/50793362/how-to-use-suspendcoroutine-to-turn-java-7-future-into-kotlin-suspending-functio
// implicitly released under the CC by-SA license

package org.evergreen_ils.views

import android.accounts.AccountManagerFuture
import kotlinx.coroutines.delay
import java.util.concurrent.Future

suspend fun <T> Future<T>.await(timeoutMs: Int = 60000): T? {
    val start = System.currentTimeMillis()
    while (!isDone) {
        if (System.currentTimeMillis() - start > timeoutMs)
            return null
        delay(1)
    }
    return get()
}

suspend fun <T> AccountManagerFuture<T>.await(timeoutMs: Int = 60000): T? {
    val start = System.currentTimeMillis()
    while (!isDone) {
        if (System.currentTimeMillis() - start > timeoutMs)
            return null
        delay(100)
    }
    return getResult()
}
