package org.evergreen.android.globals;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

class ShowNetworkNotAvailableRunnable implements Runnable {

    public Context context;

    public ShowNetworkNotAvailableRunnable(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage("You need to have an internet connection");
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        alertDialog.show();
    }

}