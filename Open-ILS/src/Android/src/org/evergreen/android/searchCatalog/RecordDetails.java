package org.evergreen.android.searchCatalog;

import org.evergreen.android.R;
import org.evergreen.android.utils.ui.RealViewSwitcher;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class RecordDetails extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

     // create the view switcher
     		RealViewSwitcher realViewSwitcher = new RealViewSwitcher(getApplicationContext());

     		// add some views to it
     		final int[] backgroundColors = { Color.RED, Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW };
     		for (int i = 0; i < 5; i++) {
     			
     			
     			
     			TextView textView = new TextView(getApplicationContext());
     			textView.setText(Integer.toString(i + 1));
     			textView.setTextSize(100);
     			textView.setTextColor(Color.BLACK);
     			textView.setGravity(Gravity.CENTER);
     			textView.setBackgroundColor(backgroundColors[i]);
     			realViewSwitcher.addView(textView);
     		}

     		LayoutInflater factory = getLayoutInflater();

     		
     		LinearLayout layout = (LinearLayout) factory.inflate(R.layout.record_details_info, null);
     		
     		
     		ScrollView scroll = (ScrollView) factory.inflate(R.layout.record_details_simple, null);
     		
     		
     		System.out.println( layout + " scrol" + scroll);
     		
     		realViewSwitcher.addView(layout);
     		realViewSwitcher.addView(scroll);
     		
     		// set as content view
     		setContentView(realViewSwitcher);
     		
     		
     		
     		// yeah, it really is as simple as this :-)
     		
     		// note that you can also define your own views directly in a resource XML, too by using:
     		// <de.marcreichelt.android.RealViewSwitcher
     		//     android:layout_width="fill_parent"
     		//     android:layout_height="fill_parent"
     		//     android:id="@+id/real_view_switcher">
     		//         <!-- your views here -->
     		// </de.marcreichelt.android.RealViewSwitcher>
     		
     		
     		
     		// OPTIONAL: listen for screen changes
     		realViewSwitcher.setOnScreenSwitchListener(onScreenSwitchListener);
		
	}
	private final RealViewSwitcher.OnScreenSwitchListener onScreenSwitchListener = new RealViewSwitcher.OnScreenSwitchListener() {
		
		@Override
		public void onScreenSwitched(int screen) {
			// this method is executed if a screen has been activated, i.e. the screen is completely visible
			//  and the animation has stopped (might be useful for removing / adding new views)
			Log.d("RealViewSwitcher", "switched to screen: " + screen);
		}
		
	};
}
