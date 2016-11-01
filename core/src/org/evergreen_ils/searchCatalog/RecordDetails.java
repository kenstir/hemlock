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

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kenstir on 3/5/2016.
 */
public class RecordDetails {
    public static void launchDetailsFlow(Context context, RecordInfo info) {
        ArrayList<RecordInfo> recordList = new ArrayList<RecordInfo>(1);
        recordList.add(info);
        Intent intent = new Intent(context, SampleUnderlinesNoFade.class);
        intent.putExtra("recordList", recordList);
        context.startActivity(intent);
    }

    public static void launchDetailsFlow(Context context, ArrayList<RecordInfo> recordList, int recordPosition) {
        Intent intent = new Intent(context, SampleUnderlinesNoFade.class);
        intent.putExtra("recordList", recordList);
        intent.putExtra("recordPosition", recordPosition);
        context.startActivity(intent);
    }
}
