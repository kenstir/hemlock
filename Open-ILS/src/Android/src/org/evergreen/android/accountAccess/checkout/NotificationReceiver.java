package org.evergreen.android.accountAccess.checkout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class NotificationReceiver extends BroadcastReceiver{

	private String TAG = "NotificationManager";
	@Override
	public void onReceive(Context context, Intent intent) {

		 Bundle bundle = intent.getExtras();
	     String checkoutItemName = bundle.getString("checkoutName");
	     Toast.makeText(context, "Notification received", Toast.LENGTH_SHORT).show();
	     
	     Log.d(TAG, "The " + checkoutItemName + " is about to expire");
	     
	     //send notification
	}

	
}
