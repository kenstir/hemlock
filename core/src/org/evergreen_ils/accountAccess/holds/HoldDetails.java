/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be usefull,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 * 
 */
package org.evergreen_ils.accountAccess.holds;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.widget.*;
import org.evergreen_ils.Api;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.globals.Log;
import org.evergreen_ils.searchCatalog.SearchFormat;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.utils.ui.ProgressDialogSupport;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class HoldDetails extends ActionBarActivity {

    private final static String TAG = HoldDetails.class.getSimpleName();

    public static final int RESULT_CODE_DELETE_HOLD = 5;

    public static final int RESULT_CODE_UPDATE_HOLD = 6;

    public static final int RESULT_CODE_CANCEL = 7;

    private TextView title;
    private TextView author;
    private TextView format;
    private TextView physical_description;

    private AccountAccess accountAccess;

    private EditText expiration_date;

    private Button updateHold;

    private Button cancelHold;

    private DatePickerDialog datePicker = null;

    //private TableRow suspendHoldRow;
    private CheckBox suspendHold;

    private Spinner orgSelector;

    //private TableRow thawDateRow;
    private DatePickerDialog thaw_datePicker = null;

    private EditText thaw_date_edittext;

    private Date expire_date = null;

    private Date thaw_date = null;

    private Context context;

    private int selectedOrgPos = 0;

    public Runnable updateHoldRunnable;

    private ProgressDialogSupport progress;

    private GlobalConfigs globalConfigs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }

        setContentView(R.layout.hold_details);
        ActionBarUtils.initActionBarForActivity(this);

        context = this;
        globalConfigs = GlobalConfigs.getInstance(this);
        accountAccess = AccountAccess.getInstance();
        progress = new ProgressDialogSupport();

        final HoldRecord record = (HoldRecord) getIntent().getSerializableExtra("holdRecord");

        title = (TextView) findViewById(R.id.hold_title);
        author = (TextView) findViewById(R.id.hold_author);
        format = (TextView) findViewById(R.id.hold_format);
        physical_description = (TextView) findViewById(R.id.hold_physical_description);
        cancelHold = (Button) findViewById(R.id.cancel_hold_button);
        updateHold = (Button) findViewById(R.id.update_hold_button);
        //suspendHoldRow = (TableRow) findViewById(R.id.hold_suspend_hold_row);
        suspendHold = (CheckBox) findViewById(R.id.hold_suspend_hold);
        orgSelector = (Spinner) findViewById(R.id.hold_pickup_location);
        expiration_date = (EditText) findViewById(R.id.hold_expiration_date);
        //thawDateRow = (TableRow) findViewById(R.id.hold_thaw_date_row);
        thaw_date_edittext = (EditText) findViewById(R.id.hold_thaw_date);

        title.setText(record.title);
        author.setText(record.author);
        if (record.recordInfo != null) {
            format.setText(SearchFormat.getItemLabelFromSearchFormat(record.recordInfo.search_format));
            physical_description.setText(record.recordInfo.physical_description);
        }

        suspendHold.setChecked(record.suspended);
        if (record.suspended) {
            if (record.thaw_date != null) {
                thaw_date = record.thaw_date;
                thaw_date_edittext.setText(DateFormat.format("MMMM dd, yyyy", thaw_date));
            }
        }

        if (record.expire_time != null) {
            expire_date = record.expire_time;
            expiration_date.setText(DateFormat.format("MMMM dd, yyyy", expire_date));
        }

        thaw_date_edittext.setEnabled(suspendHold.isChecked());

        cancelHold.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(R.string.cancel_hold_dialog_message);
                builder.setNegativeButton(android.R.string.no, null);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cancelHold(record);
                    }
                });
                builder.create().show();
            }
        });

        updateHold.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateHold(record);
            }
        });

        suspendHold.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                thaw_date_edittext.setEnabled(isChecked);
            }
        });
        Calendar cal = Calendar.getInstance();

        datePicker = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {

                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Date chosenDate = new Date(year - 1900, monthOfYear, dayOfMonth);
                        expire_date = chosenDate;
                        CharSequence strDate = DateFormat.format("MMMM dd, yyyy", chosenDate);
                        expiration_date.setText(strDate);
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        expiration_date.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                datePicker.show();
            }
        });

        thaw_datePicker = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {

                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Date chosenDate = new Date(year - 1900, monthOfYear, dayOfMonth);
                        thaw_date = chosenDate;
                        CharSequence strDate = DateFormat.format("MMMM dd, yyyy", chosenDate);
                        thaw_date_edittext.setText(strDate);
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        thaw_date_edittext.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                thaw_datePicker.show();
            }
        });

        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < globalConfigs.organisations.size(); i++) {
            list.add(globalConfigs.organisations.get(i).indentedDisplayPrefix + globalConfigs.organisations.get(i).name);
            if (globalConfigs.organisations.get(i).id == record.pickup_lib) {
                selectedOrgPos = i;
            }
        }
        //ArrayAdapter<String> adapter = CompatSpinnerAdapter.CreateCompatSpinnerAdapter(this, list);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.org_item_layout, list);
        orgSelector.setAdapter(adapter);
        orgSelector.setSelection(selectedOrgPos);

        orgSelector.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int ID, long arg3) {
                selectedOrgPos = ID;
            }

            public void onNothingSelected(android.widget.AdapterView<?> arg0) {
            }
        });
    }

    @Override
    protected void onDestroy() {
        progress.dismiss();
        super.onDestroy();
    }

    private void cancelHold(final HoldRecord record) {
        Log.d(TAG, "Remove hold with id" + record.ahr.getInt("id"));
        progress.show(context, "Canceling hold");
        Thread cancelHoldThread = new Thread(
                new Runnable() {

                    @Override
                    public void run() {
                        try {
                            accountAccess.cancelHold(record.ahr);
                        } catch (SessionNotFoundException e) {
                            try {
                                if (accountAccess.reauthenticate(HoldDetails.this))
                                    accountAccess.cancelHold(record.ahr);
                            } catch (Exception eauth) {
                                Log.d(TAG, "Exception in reAuth");
                            }
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.dismiss();
                                setResult(RESULT_CODE_DELETE_HOLD);
                                finish();
                            }
                        });
                    }
                });
        cancelHoldThread.start();
    }

    private void updateHold(final HoldRecord record) {
        updateHoldRunnable = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      progress.show(context, "Updating hold");
                                  }
                              });

                String expire_date_s = null;
                String thaw_date_s = null;
                if (expire_date != null)
                    expire_date_s = Api.formatDate(expire_date);
                if (thaw_date != null)
                    thaw_date_s = Api.formatDate(thaw_date);

                try {
                    accountAccess.updateHold(record.ahr, globalConfigs.organisations.get(selectedOrgPos).id,
                            suspendHold.isChecked(), expire_date_s, thaw_date_s);
                } catch (SessionNotFoundException e) {
                    try {
                        if (accountAccess.reauthenticate(HoldDetails.this))
                            accountAccess.updateHold(record.ahr,
                                    globalConfigs.organisations.get(selectedOrgPos).id,
                                    suspendHold.isChecked(), expire_date_s, thaw_date_s);
                    } catch (Exception eauth) {
                        Log.d(TAG, "Exception in reAuth");
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.dismiss();
                        Toast.makeText(context, "Hold updated",
                                Toast.LENGTH_SHORT);
                        setResult(RESULT_CODE_UPDATE_HOLD);
                        finish();
                    }
                });
            }
        };

        Thread updateHoldThread = new Thread(updateHoldRunnable);
        updateHoldThread.start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
