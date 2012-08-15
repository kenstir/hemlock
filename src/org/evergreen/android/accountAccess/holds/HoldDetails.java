package org.evergreen.android.accountAccess.holds;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.accountAccess.SessionNotFoundException;
import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.globals.NoAccessToServer;
import org.evergreen.android.globals.NoNetworkAccessException;
import org.evergreen.android.globals.Utils;
import org.evergreen.android.searchCatalog.SearchCatalogListView;
import org.evergreen.android.views.AccountScreenDashboard;

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
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class HoldDetails extends Activity {

    public static final int RESULT_CODE_DELETE_HOLD = 5;

    public static final int RESULT_CODE_UPDATE_HOLD = 6;

    public static final int RESULT_CODE_CANCEL = 7;

    private TextView recipient;

    private TextView title;

    private TextView author;

    private TextView physical_description;

    private TextView screen_title;

    private AccountAccess accountAccess;

    private EditText expiration_date;

    private Button updateHold;

    private Button cancelHold;

    private Button back;

    private EditText phone_number;

    private CheckBox phone_notification;

    private CheckBox email_notification;

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

    private Button homeButton;

    private Button myAccountButton;

    private TextView headerTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.hold_details);
        globalConfigs = GlobalConfigs.getGlobalConfigs(this);

        homeButton = (Button) findViewById(R.id.library_logo);
        myAccountButton = (Button) findViewById(R.id.my_account_button);
        headerTitle = (TextView) findViewById(R.id.header_title);
        headerTitle.setText(R.string.hold_details_title);
        homeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),
                        SearchCatalogListView.class);
                startActivity(intent);
            }
        });
        myAccountButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),
                        AccountScreenDashboard.class);
                startActivity(intent);
            }
        });

        final HoldRecord record = (HoldRecord) getIntent()
                .getSerializableExtra("holdRecord");

        System.out.println("Record " + record + " " + record.title + " "
                + record.ahr);

        accountAccess = AccountAccess.getAccountAccess();

        recipient = (TextView) findViewById(R.id.hold_recipient);
        title = (TextView) findViewById(R.id.hold_title);
        author = (TextView) findViewById(R.id.hold_author);
        physical_description = (TextView) findViewById(R.id.hold_physical_description);
        cancelHold = (Button) findViewById(R.id.cancel_hold_button);
        updateHold = (Button) findViewById(R.id.update_hold_button);
        back = (Button) findViewById(R.id.back_button);

        phone_number = (EditText) findViewById(R.id.hold_contact_telephone);
        phone_notification = (CheckBox) findViewById(R.id.hold_enable_phone_notification);
        email_notification = (CheckBox) findViewById(R.id.hold_enable_email_notification);
        suspendHold = (CheckBox) findViewById(R.id.hold_suspend_hold);

        orgSelector = (Spinner) findViewById(R.id.hold_pickup_location);
        expiration_date = (EditText) findViewById(R.id.hold_expiration_date);
        thaw_date_edittext = (EditText) findViewById(R.id.hold_thaw_date);

        recipient.setText(accountAccess.userName);
        title.setText(record.title);
        author.setText(record.author);
        if (record.recordInfo != null)
            physical_description
                    .setText(record.recordInfo.physical_description);

        // set record info
        phone_notification.setChecked(record.phone_notification);
        email_notification.setChecked(record.email_notification);
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

        // hide edit text
        if (record.thaw_date == null)
            disableView(thaw_date_edittext);

        if (!record.phone_notification)
            disableView(phone_number);

        System.out.println(record.title + " " + record.author);

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

                                System.out.println("Remove hold with id"
                                        + record.ahr.getInt("id"));

                                progressDialog = ProgressDialog.show(context,
                                        "Please wait", "Canceling hold");
                                Thread cancelHoldThread = new Thread(
                                        new Runnable() {

                                            @Override
                                            public void run() {

                                                try {
                                                    accountAccess
                                                            .cancelHold(record.ahr);
                                                } catch (NoNetworkAccessException e) {
                                                    Utils.showNetworkNotAvailableDialog(context);
                                                } catch (NoAccessToServer e) {
                                                    Utils.showServerNotAvailableDialog(context);

                                                } catch (SessionNotFoundException e) {
                                                    // TODO other way?
                                                    try {
                                                        if (accountAccess
                                                                .authenticate())
                                                            accountAccess
                                                                    .cancelHold(record.ahr);
                                                    } catch (Exception eauth) {
                                                        System.out
                                                                .println("Exception in reAuth");
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
                            email_notification.isChecked(), phone_notification
                                    .isChecked(), phone_number.getText()
                                    .toString(), suspendHold.isChecked(),
                            expire_date_s, thaw_date_s);
                } catch (NoNetworkAccessException e) {
                    Utils.showNetworkNotAvailableDialog(context);
                } catch (NoAccessToServer e) {
                    Utils.showServerNotAvailableDialog(context);

                } catch (SessionNotFoundException e) {
                    // TODO other way?
                    try {
                        if (accountAccess.authenticate())
                            accountAccess.updateHold(record.ahr,
                                    selectedOrgPos,
                                    email_notification.isChecked(),
                                    phone_notification.isChecked(),
                                    phone_number.getText().toString(),
                                    suspendHold.isChecked(), expire_date_s,
                                    thaw_date_s);
                    } catch (Exception eauth) {
                        System.out.println("Exception in reAuth");
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
                progressDialog = ProgressDialog.show(context, "Please wait",
                        "Updating hold");
                Thread updateHoldThread = new Thread(updateHoldRunnable);
                updateHoldThread.start();
            }
        });

        phone_notification
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {

                        if (isChecked) {
                            enableView(phone_number);
                        } else
                            disableView(phone_number);
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
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.spinner_layout, list);
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
