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

package net.kenstir.hemlock.android;

/**
 * Created by kenstir on 1/29/2017.
 */
public class AndroidLogProvider implements LogProvider {
    @Override
    public void v(String TAG, String msg) {
        android.util.Log.v(TAG, msg);
    }

    @Override
    public void d(String TAG, String msg) {
        android.util.Log.d(TAG, msg);
    }

    @Override
    public void d(String TAG, String msg, Throwable tr) {
        android.util.Log.d(TAG, msg, tr);
    }

    @Override
    public void i(String TAG, String msg) {
        android.util.Log.i(TAG, msg);
    }

    @Override
    public void w(String TAG, String msg) {
        android.util.Log.w(TAG, msg);
    }

    @Override
    public void w(String TAG, String msg, Throwable tr) {
        android.util.Log.w(TAG, msg, tr);
    }
}
