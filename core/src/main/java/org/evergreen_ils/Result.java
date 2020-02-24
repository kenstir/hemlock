/*
 * Copyright (C) 2017 Kenneth H. Cox
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

package org.evergreen_ils;

import org.evergreen_ils.android.Log;

import java.util.HashMap;
import java.util.Map;

/** Result of making an Api call
 */
//TODO: reconcile this with GatewayResponse, model after hemlock-ios
public class Result {
    private static final String TAG = Result.class.getSimpleName();

    // error message overrides for terrible Evergreen messages
    private static Map<String, String> mMessageOverrides = new HashMap<>();
    static {
        mMessageOverrides.put("HIGH_LEVEL_HOLD_HAS_NO_COPIES", "The selected item is not holdable.  Call your local library with any questions.");
    }


    private boolean mIsSuccess = false;
    private Object mObject = null;
    private Map<String, ?> mEvent = null;

    private Result() {
    }

    public static Result createUnknownError() {
        Result r = new Result();
        return r;
    }

    public static Result createFromSuccess(Object obj) {
        Result r = new Result();
        r.mIsSuccess = true;
        r.mObject = obj;
        return r;
    }

    public static Result createFromEvent(Object obj) {
        Result r = new Result();
        r.mIsSuccess = false;
        try {
            r.mEvent = (Map<String, ?>) obj;
        } catch (Exception e) {
            Log.d(TAG, "caught", e);
        }
        return r;
    }

    public boolean isSuccess() {
        return mIsSuccess;
    }

    public String getErrorMessage() {
        if (isSuccess()) {
            return "OK";
        } else if (mEvent != null && mEvent.containsKey("desc")) {
            String errorMessage = mMessageOverrides.get(mEvent.get("textcode"));
            if (errorMessage == null)
                errorMessage = (String) mEvent.get("desc");
            return errorMessage;
        } else {
            return "Unknown error";
        }
    }
}
