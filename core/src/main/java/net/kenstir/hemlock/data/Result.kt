/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.kenstir.hemlock.data

/**
 * A generic class that holds a value or an error.
 * @param <T>
 */
sealed class Result<out R> {

    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()

    // get function that returns data or throws
    // modelled after Swift 5
    fun get(): R {
        return when (this) {
            is Success -> data
            is Error -> throw exception
        }
    }

    // useful for tests
    val unwrappedError: Exception?
        get() {
            return when (this) {
                is Success<*> -> null
                is Error -> exception
            }
        }

    val succeeded: Boolean
        get() {
            return when (this) {
                is Success<*> -> true
                is Error -> false
            }
        }

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$exception]"
        }
    }
}
