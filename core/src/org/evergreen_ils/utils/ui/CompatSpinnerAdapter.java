package org.evergreen_ils.utils.ui;

import android.content.Context;
import android.os.Build;
import android.widget.ArrayAdapter;
import org.evergreen_ils.R;

import java.util.ArrayList;

/**
 * Created by kenstir on 1/5/14.
 */
public class CompatSpinnerAdapter {
    /** Create an ArrayAdapter for use in a spinner.
     * This solves the problem running under theme Theme.AppCompat: under Gingerbread, the spinner dialog
     * has a white background, even though the theme is dark, and the text is white-on-white.
     * The fix is to use two different spinner layouts, one for GB with textColor="@color/dark"
     * and one for newer versions with textColor="?android:textColorPrimary".
     */
    public static ArrayAdapter<String> CreateCompatSpinnerAdapter(Context context, ArrayList<String> list) {
        int spinner_layout = (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                ? R.layout.spinner_gb_layout :
                R.layout.spinner_layout;
        ArrayAdapter<String> adapter  = new ArrayAdapter<String>(context, spinner_layout, list);
        return adapter;
    }
}
