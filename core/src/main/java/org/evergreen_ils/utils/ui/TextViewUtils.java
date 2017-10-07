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

package org.evergreen_ils.utils.ui;

import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
 * Created by kenstir on 6/10/2017.
 */

public class TextViewUtils {

    public static String makeLinkHtml(String url, String body) {
        String html = "<a href='" + url + "'>" + body + "</a>";
        return html;
    }

    public static void setTextHtml(TextView view, String html) {
        Spanned result;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//            result = Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY);
//        } else {
            result = Html.fromHtml(html);
//        }
        view.setText(result);
        view.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
