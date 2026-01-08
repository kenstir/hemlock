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

package net.kenstir.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.kenstir.data.model.Account;

public class App {
    private static final String TAG = "App";

    // request/result codes for use with startActivityForResult
    public static final int REQUEST_MESSAGES = 10002;

    private static @NonNull Account account = Account.Companion.getNoAccount();
    private static String fcmNotificationToken = null;

    @Nullable
    public static String getFcmNotificationToken() {
        return fcmNotificationToken;
    }

    public static void setFcmNotificationToken(@Nullable String fcmNotificationToken) {
        App.fcmNotificationToken = fcmNotificationToken;
    }

    public static @NonNull Account getAccount() {
        return account;
    }

    public static void setAccount(@NonNull Account account) {
        App.account = account;
    }
}
