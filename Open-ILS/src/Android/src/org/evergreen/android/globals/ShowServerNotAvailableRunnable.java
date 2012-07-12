package org.evergreen.android.globals;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class ShowServerNotAvailableRunnable implements Runnable{
	
	private Context context;
	
	public ShowServerNotAvailableRunnable(Context context) {
		this.context = context;
	}
	
	@Override
	public void run() {
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();
	    alertDialog.setTitle("Error");
	    alertDialog.setMessage("There is no network connectivity to " + GlobalConfigs.httpAddress);
	    alertDialog.setButton("OK",
	    new DialogInterface.OnClickListener() {
	 
	      @Override
	      public void onClick(DialogInterface dialog, int id) {
	        dialog.dismiss();
	      }
	    });


		
		alertDialog.show();
	}
	
}