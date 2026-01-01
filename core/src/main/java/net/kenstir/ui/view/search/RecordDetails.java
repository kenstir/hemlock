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

package net.kenstir.ui.view.search;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import net.kenstir.ui.Key;
import net.kenstir.data.model.BibRecord;

import java.util.ArrayList;

public class RecordDetails {

    public static void launchDetailsFlow(@NonNull Context context, ArrayList<BibRecord> recordList, int recordPosition) {
        // determine name of parent activity
        PackageManager pm = context.getPackageManager();
        String parentActivityLabel = null;
        try {
            ActivityInfo info = pm.getActivityInfo(new ComponentName(context, context.getClass().getCanonicalName()), 0);
            parentActivityLabel = context.getString(info.labelRes);
        } catch (PackageManager.NameNotFoundException ignored) {
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
}
