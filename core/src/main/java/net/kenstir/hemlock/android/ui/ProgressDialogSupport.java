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

package net.kenstir.hemlock.android.ui;

import android.app.Activity;
import android.app.ProgressDialog;

/** Methods to manage the lifecycle of a ProgressBar used to indicate async activity
 */
public class ProgressDialogSupport {
    private ProgressDialog progressDialog;

    public ProgressDialogSupport() {
    }

    public void show(Activity activity, CharSequence msg) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(activity);
            progressDialog.setOwnerActivity(activity);
        }
        progressDialog.setMessage(msg);
        progressDialog.show();
    }

    public void dismiss() {
        if (progressDialog == null)
            return;
        Activity activity = progressDialog.getOwnerActivity();
        if (activity != null && activity.isFinishing())
            return;
        if (progressDialog.isShowing())
            progressDialog.dismiss();
    }
}
