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

import android.text.TextUtils;
import org.evergreen_ils.system.Log;

/** Interface to get the keys necessary to perform billing.  Concrete implementation is provided by the app
 * and the name of that class is specified in R.string.ou_billing_data_provider .
 *
 * Created by kenstir on 1/1/2016.
 */
public abstract class BillingDataProvider {
    private static String TAG = BillingDataProvider.class.getSimpleName();

    static public BillingDataProvider create(String clazzName) {
        if (TextUtils.isEmpty(clazzName)) {
            return null;
        }
        try {
            Class clazz = Class.forName(clazzName);
            return (BillingDataProvider) clazz.newInstance();
        } catch (Exception e) {
            Log.d(TAG, "error instantiating "+clazzName, e);
            return null;
        }
    }

    abstract public String getPublicKey();
}
