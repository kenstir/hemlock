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

package org.evergreen_ils.views.bookbags;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.android.Log;
import org.evergreen_ils.data.BookBag;
import org.evergreen_ils.searchCatalog.RecordInfo;

import java.util.List;

public class BookBagUtils {
    private static final String TAG = BookBagUtils.class.getSimpleName();

    public static void showAddToListDialog(final Activity activity, final List<BookBag> bookBags, final RecordInfo info) {
        final String list_names[] = new String[bookBags.size()];
        for (int i = 0; i < list_names.length; i++)
            list_names[i] = bookBags.get(i).name;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.choose_list_message);
        builder.setItems(list_names, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addRecordToList(activity, bookBags.get(which), info);
            }
        });
        builder.create().show();
    }

    public static void addRecordToList(final Activity activity, final BookBag bookbag, final RecordInfo info) {
        final ProgressDialog progressDialog = ProgressDialog.show(activity,
                activity.getString(R.string.dialog_please_wait),
                activity.getString(R.string.adding_to_list_message));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                AccountAccess ac = AccountAccess.getInstance();
                try {
                    ac.addRecordToBookBag(info.doc_id, bookbag.id);
                } catch (SessionNotFoundException e) {
                    Log.d(TAG, "caught", e);
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                });

            }
        });
        thread.start();
    }
}
