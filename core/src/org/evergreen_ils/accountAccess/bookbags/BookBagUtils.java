package org.evergreen_ils.accountAccess.bookbags;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.globals.Log;
import org.evergreen_ils.searchCatalog.RecordInfo;

import java.util.List;

/**
 * Created by kenstir on 2/29/2016.
 */
public class BookBagUtils {
    private static final String TAG = BookBagUtils.class.getSimpleName();

    public static void showAddToListDialog(final Activity activity, final List<BookBag> bookBags, final RecordInfo info) {
        final String list_names[] = new String[bookBags.size()];
        for (int i = 0; i < list_names.length; i++)
            list_names[i] = bookBags.get(i).name;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.choose_list_message);
        builder.setItems(list_names, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addRecordToList(activity, bookBags.get(which), info);
            }
        });
        builder.create().show();
    }

    public static void addRecordToList(final Activity activity, final BookBag bookbag, final RecordInfo info) {
        final ProgressDialog progressDialog = ProgressDialog.show(activity,
                activity.getString(R.string.dialog_please_wait),
                activity.getString(R.string.adding_to_list_message));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                AccountAccess ac = AccountAccess.getInstance();
                try {
                    ac.addRecordToBookBag(info.doc_id, bookbag.id);
                } catch (SessionNotFoundException e) {
                    Log.d(TAG, "caught", e);
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                });

            }
        });
        thread.start();
    }
}
