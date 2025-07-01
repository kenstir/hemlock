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

package org.evergreen_ils.views.search;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

import net.kenstir.hemlock.android.Key;
import net.kenstir.hemlock.data.model.BibRecord;

import org.evergreen_ils.data.MBRecord;

import java.util.ArrayList;

public class RecordDetails {

    public static void launchDetailsFlow(Context context, ArrayList<BibRecord> recordList, int recordPosition) {
        // determine name of parent activity
        PackageManager pm = context.getPackageManager();
        String parentActivityLabel = null;
        try {
            ActivityInfo info = pm.getActivityInfo(new ComponentName(context, context.getClass().getCanonicalName()), 0);
            parentActivityLabel = context.getString(info.labelRes);
        } catch (PackageManager.NameNotFoundException e) {
        }

        // Prevent TransactionTooLargeException by limiting the data passed via Intent.
        // In my testing, 100 records ~= 100KB, well below the limit of ~500KB.  If the
        // list is too long, start the details flow with just the selected item.
        final int MAX_RECORDS_IN_TRANSACTION = 100;
        ArrayList<BibRecord> recordListForTransaction = recordList;
        int recordPositionForTransaction = recordPosition;
        if (recordList.size() > MAX_RECORDS_IN_TRANSACTION) {
            recordListForTransaction = new ArrayList<>();
            recordListForTransaction.add(recordList.get(recordPosition));
            recordPositionForTransaction = 0;
        }

        Intent intent = new Intent(context, RecordDetailsActivity.class);
        intent.putExtra(Key.RECORD_LIST, recordListForTransaction);
        intent.putExtra(Key.RECORD_POSITION, recordPositionForTransaction);
        intent.putExtra(Key.TITLE, parentActivityLabel);
        context.startActivity(intent);
    }

//    private static int bundleSize(Bundle bundle) {
//        Parcel parcel = Parcel.obtain();
//        parcel.writeBundle(bundle);
//        int size = parcel.dataSize();
//        parcel.recycle();
//        return size;
//    }
}
