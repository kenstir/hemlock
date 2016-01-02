/*
 * Copyright (C) 2016 Kenneth H. Cox
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

package org.evergreen_ils.billing;

import org.evergreen_ils.globals.Log;

/** Place to get the keys necessary to perform billing
 *
 * Created by kenstir on 1/1/2016.
 */
public abstract class BillingDataProvider {
    private static String TAG = BillingDataProvider.class.getSimpleName();

    static public BillingDataProvider create(String clazzName) {
        try {
            Class clazz = Class.forName(clazzName);
            return (BillingDataProvider) clazz.newInstance();
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
            return null;
        }
    }

    abstract public String getPublicKey();
}
