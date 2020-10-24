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

package org.evergreen_ils.searchCatalog;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcel;

import org.evergreen_ils.android.Log;

import java.util.ArrayList;

public class RecordDetails {
    public static void launchDetailsFlow(Context context, ArrayList<RecordInfo> recordList, int recordPosition) {
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
        ArrayList<RecordInfo> recordListForTransaction = recordList;
        int recordPositionForTransaction = recordPosition;
        if (recordList.size() > MAX_RECORDS_IN_TRANSACTION) {
            recordListForTransaction = new ArrayList<>();
            recordListForTransaction.add(recordList.get(recordPosition));
            recordPositionForTransaction = 0;
        }

        Intent intent = new Intent(context, SampleUnderlinesNoFade.class);
        intent.putExtra("recordList", recordListForTransaction);
        intent.putExtra("recordPosition", recordPositionForTransaction);
        intent.putExtra("title", parentActivityLabel);
//        Log.d("xzyyz", "intent size: " + bundleSize(intent.getExtras()));
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
