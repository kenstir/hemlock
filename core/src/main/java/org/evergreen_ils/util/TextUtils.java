/*
 * Copyright (C) 2006 The Android Open Source Project
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

package org.evergreen_ils.util;

import java.util.Iterator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Partial copy of android.text.TextUtils, to make things work in unit tests
 *
 * TODO: replace with kotlin native equivalents
 */
public class TextUtils {
    /**
     * Returns true if the string is null or 0-length.
     * @param str the string to be examined
     * @return true if str is null or zero length
     */
    public static boolean isEmpty(@Nullable CharSequence str) {
        return str == null || str.length() == 0;
    }

    /**
     * Returns a string containing the tokens joined by delimiters.
     *
     * @param delimiter a CharSequence that will be inserted between the tokens. If null, the string
     *     "null" will be used as the delimiter.
     * @param tokens an array objects to be joined. Strings will be formed from the objects by
     *     calling object.toString(). If tokens is null, a NullPointerException will be thrown. If
     *     tokens is an empty array, an empty string will be returned.
     */
    public static String join(@NonNull CharSequence delimiter, @NonNull Object[] tokens) {
        final int length = tokens.length;
        if (length == 0) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(tokens[0]);
        for (int i = 1; i < length; i++) {
            sb.append(delimiter);
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

    /**
     * Returns a string containing the tokens joined by delimiters.
     *
     * @param delimiter a CharSequence that will be inserted between the tokens. If null, the string
     *     "null" will be used as the delimiter.
     * @param tokens an array objects to be joined. Strings will be formed from the objects by
     *     calling object.toString(). If tokens is null, a NullPointerException will be thrown. If
     *     tokens is empty, an empty string will be returned.
     */
    public static String join(@NonNull CharSequence delimiter, @NonNull Iterable tokens) {
        final Iterator<?> it = tokens.iterator();
        if (!it.hasNext()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(it.next());
        while (it.hasNext()) {
            sb.append(delimiter);
            sb.append(it.next());
        }
        return sb.toString();
    }
}
