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
public interface LogProvider {
    void v(String TAG, String msg);
    void d(String TAG, String msg);
    void d(String TAG, String msg, Throwable tr);
    void i(String TAG, String msg);
    void w(String TAG, String msg);
    void w(String TAG, String msg, Throwable tr);
}
