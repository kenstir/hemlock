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

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.utils.ui.CompatSpinnerAdapter;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class HoldDetails extends ActionBarActivity {

    private final String TAG = HoldDetails.class.getSimpleName();

    public static final int RESULT_CODE_DELETE_HOLD = 5;

    public static final int RESULT_CODE_UPDATE_HOLD = 6;

    public static final int RESULT_CODE_CANCEL = 7;

    private TextView title;

    private TextView author;

    private TextView physical_description;

    private AccountAccess accountAccess;

    private EditText expiration_date;

    private Button updateHold;

    private Button cancelHold;

    private Button back;

    private DatePickerDialog datePicker = null;

    private CheckBox suspendHold;

    private Spinner orgSelector;

    private DatePickerDialog thaw_datePicker = null;

    private EditText thaw_date_edittext;

    private Date expire_date = null;

    private Date thaw_date = null;

    private Context context;

    private int selectedOrgPos = 0;

    public Runnable updateHoldRunnable;

    private ProgressDialog progressDialog;

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
        globalConfigs = GlobalConfigs.getGlobalConfigs(this);

        final HoldRecord record = (HoldRecord) getIntent()
                .getSerializableExtra("holdRecord");

        Log.d(TAG, "Record " + record + " " + record.title + " "
                + record.ahr);

        accountAccess = AccountAccess.getAccountAccess();

        title = (TextView) findViewById(R.id.hold_title);
        author = (TextView) findViewById(R.id.hold_author);
        physical_description = (TextView) findViewById(R.id.hold_physical_description);
        cancelHold = (Button) findViewById(R.id.cancel_hold_button);
        updateHold = (Button) findViewById(R.id.update_hold_button);
        back = (Button) findViewById(R.id.back_button);
        suspendHold = (CheckBox) findViewById(R.id.hold_suspend_hold);
        orgSelector = (Spinner) findViewById(R.id.hold_pickup_location);
        expiration_date = (EditText) findViewById(R.id.hold_expiration_date);
        thaw_date_edittext = (EditText) findViewById(R.id.hold_thaw_date);

        title.setText(record.title);
        author.setText(record.author);
        if (record.recordInfo != null)
            physical_description
                    .setText(record.recordInfo.physical_description);

        // set record info
        suspendHold.setChecked(record.suspended);

        if (record.thaw_date != null) {
            thaw_date = record.thaw_date;
            thaw_date_edittext.setText(DateFormat.format("MMMM dd, yyyy",
                    thaw_date));
        }
        if (record.expire_time != null) {
            expire_date = record.expire_time;
            expiration_date.setText(DateFormat.format("MMMM dd, yyyy",
                    expire_date));
        }

        if (suspendHold.isChecked()) {
            enableView(thaw_date_edittext);
        } else {
            disableView(thaw_date_edittext);
        }

        Log.d(TAG, record.title + " " + record.author);

        back.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        cancelHold.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                Builder confirmationDialogBuilder = new AlertDialog.Builder(
                        context);
                confirmationDialogBuilder
                        .setMessage(R.string.cancel_hold_dialog_message);

                confirmationDialogBuilder.setNegativeButton(
                        android.R.string.no, null);
                confirmationDialogBuilder.setPositiveButton(
                        android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {

                                Log.d(TAG, "Remove hold with id"
                                        + record.ahr.getInt("id"));

                                progressDialog = ProgressDialog.show(context,
                                        getResources().getText(R.string.dialog_please_wait),
                                        "Canceling hold");
                                Thread cancelHoldThread = new Thread(
                                        new Runnable() {

                                            @Override
                                            public void run() {

                                                try {
                                                    accountAccess
                                                            .cancelHold(record.ahr);
                                                } catch (SessionNotFoundException e) {
                                                    try {
                                                        if (accountAccess.reauthenticate(HoldDetails.this))
                                                            accountAccess
                                                                    .cancelHold(record.ahr);
                                                    } catch (Exception eauth) {
                                                        Log.d(TAG, "Exception in reAuth");
                                                    }
                                                }

                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        progressDialog
                                                                .dismiss();
                                                        setResult(RESULT_CODE_DELETE_HOLD);

                                                        finish();
                                                    }
                                                });
                                            }
                                        });
                                cancelHoldThread.start();

                            }
                        });
                confirmationDialogBuilder.create().show();

            }
        });

        updateHoldRunnable = new Runnable() {
            @Override
            public void run() {
                // update new values
                String expire_date_s = null;
                String thaw_date_s = null;
                if (expire_date != null)
                    expire_date_s = GlobalConfigs.getStringDate(expire_date);
                if (thaw_date != null)
                    thaw_date_s = GlobalConfigs.getStringDate(thaw_date);

                try {
                    accountAccess.updateHold(record.ahr, selectedOrgPos,
                            suspendHold.isChecked(),
                            expire_date_s, thaw_date_s);
                } catch (SessionNotFoundException e) {
                    try {
                        if (accountAccess.reauthenticate(HoldDetails.this))
                            accountAccess.updateHold(record.ahr,
                                    selectedOrgPos,
                                    suspendHold.isChecked(), expire_date_s,
                                    thaw_date_s);
                    } catch (Exception eauth) {
                        Log.d(TAG, "Exception in reAuth");
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Toast.makeText(context, "Hold updated",
                                Toast.LENGTH_SHORT);
                        setResult(RESULT_CODE_UPDATE_HOLD);
                        finish();
                    }
                });
            }
        };

        updateHold.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog = ProgressDialog.show(context,
                        getResources().getText(R.string.dialog_please_wait),
                        "Updating hold");
                Thread updateHoldThread = new Thread(updateHoldRunnable);
                updateHoldThread.start();
            }
        });

        suspendHold.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {

                if (isChecked) {
                    enableView(thaw_date_edittext);
                } else {
                    disableView(thaw_date_edittext);
                }
            }
        });
        Calendar cal = Calendar.getInstance();

        datePicker = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {

                    public void onDateSet(DatePicker view, int year,
                            int monthOfYear, int dayOfMonth) {

                        Date chosenDate = new Date(year - 1900, monthOfYear,
                                dayOfMonth);
                        expire_date = chosenDate;
                        CharSequence strDate = DateFormat.format(
                                "MMMM dd, yyyy", chosenDate);
                        expiration_date.setText(strDate);
                        // set current date
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));

        expiration_date.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                datePicker.show();
            }
        });

        thaw_datePicker = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {

                    public void onDateSet(DatePicker view, int year,
                            int monthOfYear, int dayOfMonth) {

                        Date chosenDate = new Date(year - 1900, monthOfYear,
                                dayOfMonth);
                        thaw_date = chosenDate;
                        CharSequence strDate = DateFormat.format(
                                "MMMM dd, yyyy", chosenDate);
                        thaw_date_edittext.setText(strDate);
                        // set current date
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));

        thaw_date_edittext.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                thaw_datePicker.show();
            }
        });

        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < globalConfigs.organisations.size(); i++) {
            list.add(globalConfigs.organisations.get(i).padding
                    + globalConfigs.organisations.get(i).name);

            if (globalConfigs.organisations.get(i).id == record.pickup_lib)
                selectedOrgPos = i;
        }
        ArrayAdapter<String> adapter = CompatSpinnerAdapter.CreateCompatSpinnerAdapter(this, list);
        orgSelector.setAdapter(adapter);
        orgSelector.setSelection(selectedOrgPos);

        orgSelector.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int ID,
                    long arg3) {

                selectedOrgPos = ID;

            }

            public void onNothingSelected(android.widget.AdapterView<?> arg0) {
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // make the action bar "up" caret work like "back"
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void disableView(View view) {

        // view.setFocusable(false);
        view.setFocusable(false);

        view.setBackgroundColor(Color.argb(255, 100, 100, 100));
        // view.setVisibility(View.INVISIBLE);
    }

    public void enableView(View view) {
        // view.setVisibility(View.VISIBLE);

        view.setFocusableInTouchMode(true);

        view.setBackgroundColor(Color.argb(255, 255, 255, 255));
    }

}
