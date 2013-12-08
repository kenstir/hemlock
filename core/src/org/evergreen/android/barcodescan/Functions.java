package org.evergreen.android.barcodescan;

import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author George Oprina
 * 
 *         08.06.2011
 */

public class Functions {

	// makes toast length short
	public static void makeToast(String s, Context c) {
		CharSequence text = s;
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(c, text, duration);
		toast.show();
	}

	// makes toast selectable length
	public static void makeToast(String s, Context c, int length) {
		CharSequence text = s;
		int duration = Toast.LENGTH_SHORT;

		if (length == 1)
			duration = Toast.LENGTH_LONG;

		Toast toast = Toast.makeText(c, text, duration);
		toast.show();
	}

	// makes text view from string
	public static TextView makeTextView(String s, Context c) {
		TextView text = new TextView(c);
		text.setBackgroundColor(Color.WHITE);
		text.setText(s);
		text.setTextColor(Color.BLACK);
		return text;
	}

	// thread sleep
	public static void sleep(int msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
