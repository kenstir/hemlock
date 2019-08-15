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

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.MenuItem;
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

import org.evergreen_ils.Api;
import org.evergreen_ils.R;
import org.evergreen_ils.Result;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.SessionNotFoundException;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Organization;
import org.evergreen_ils.system.SMSCarrier;
import org.evergreen_ils.utils.ui.ActionBarUtils;
import org.evergreen_ils.system.Analytics;
import org.evergreen_ils.utils.ui.ProgressDialogSupport;
import org.evergreen_ils.views.splashscreen.SplashActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.evergreen_ils.system.Utils.safeString;

public class PlaceHoldActivity extends AppCompatActivity {

    private static final String TAG = PlaceHoldActivity.class.getSimpleName();
    private TextView title;
    private TextView author;
    private TextView format;
    private AccountAccess accountAccess;
    private EditText expiration_date;
    private EditText sms_notify;
    private EditText phone_notify;
    private CheckBox phone_notification;
    private CheckBox email_notification;
    private CheckBox sms_notification;
    private Spinner sms_spinner;
    private Button placeHold;
    private CheckBox suspendHold;
    private Spinner orgSpinner;
    private TextView phone_notification_label;
    private TextView sms_notification_label;
    private TextView sms_spinner_label;
    private DatePickerDialog datePicker = null;
    private DatePickerDialog thaw_datePicker = null;
    private EditText thaw_date_edittext;
    private Date expire_date = null;
    private Date thaw_date = null;
    private Runnable placeHoldRunnable;
    private EvergreenServer eg = null;
    private int selectedOrgPos = 0;
    private int selectedSMSPos = 0;
    private ProgressDialogSupport progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Analytics.initialize(this);
        if (!SplashActivity.isAppInitialized()) {
            SplashActivity.restartApp(this);
            return;
        }

        setContentView(R.layout.place_hold);
        ActionBarUtils.initActionBarForActivity(this);

        eg = EvergreenServer.getInstance();
        RecordInfo record = (RecordInfo) getIntent().getSerializableExtra("recordInfo");

        accountAccess = AccountAccess.getInstance();
        progress = new ProgressDialogSupport();

        title = findViewById(R.id.hold_title);
        author = findViewById(R.id.hold_author);
        format = findViewById(R.id.hold_format);
        placeHold = findViewById(R.id.place_hold);
        expiration_date = findViewById(R.id.hold_expiration_date);
        email_notification = findViewById(R.id.hold_enable_email_notification);
        phone_notification_label = findViewById(R.id.hold_phone_notification_label);
        sms_notification_label = findViewById(R.id.hold_sms_notification_label);
        sms_spinner_label = findViewById(R.id.hold_sms_spinner_label);
        phone_notification = findViewById(R.id.hold_enable_phone_notification);
        phone_notify = findViewById(R.id.hold_phone_notify);
        sms_notify = findViewById(R.id.hold_sms_notify);
        sms_notification = findViewById(R.id.hold_enable_sms_notification);
        sms_spinner = findViewById(R.id.hold_sms_carrier);
        suspendHold = findViewById(R.id.hold_suspend_hold);
        orgSpinner = findViewById(R.id.hold_pickup_location);
        thaw_date_edittext = findViewById(R.id.hold_thaw_date);

        title.setText(record.title);
        author.setText(record.author);
        format.setText(RecordInfo.getFormatLabel(record));

        email_notification.setChecked(accountAccess.getDefaultEmailNotification());
        initPhoneControls(getResources().getBoolean(R.bool.ou_enable_phone_notification));
        initSMSControls(eg.getSMSEnabled());
        initPlaceHoldRunnable(record);
        initPlaceHoldButton();
        initSuspendHoldButton();
        initDatePickers();
        initOrgSpinner();
    }

    private String getPhoneNotify() {
        return phone_notification.isChecked() ? phone_notify.getText().toString() : null;
    }

    private String getSMSNotify() {
        return sms_notification.isChecked() ? sms_notify.getText().toString() : null;
    }

    private Integer getSMSNotifyCarrier(int id) {
        return sms_notification.isChecked() ? id : null;
    }

    private void initPlaceHoldRunnable(RecordInfo record) {
        final Integer record_id = record.doc_id;
        placeHoldRunnable = new Runnable() {

            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.show(PlaceHoldActivity.this, "Placing hold");
                    }
                });

                String expire_date_s = null;
                if (expire_date != null)
                    expire_date_s = Api.formatDate(expire_date);
                String thaw_date_s = null;
                if (thaw_date != null)
                    thaw_date_s = Api.formatDate(thaw_date);

                int selectedOrgID = -1;
                if (eg.getOrganizations().size() > selectedOrgPos)
                    selectedOrgID = eg.getOrganizations().get(selectedOrgPos).id;
                int selectedSMSCarrierID = -1;
                if (eg.getSMSCarriers().size() > selectedSMSPos)
                    selectedSMSCarrierID = eg.getSMSCarriers().get(selectedSMSPos).id;

                Result temp_result = Result.createUnknownError();
                try {
                    temp_result = accountAccess.testAndCreateHold(record_id, selectedOrgID,
                            email_notification.isChecked(), getPhoneNotify(),
                            getSMSNotify(), getSMSNotifyCarrier(selectedSMSCarrierID),
                            expire_date_s, suspendHold.isChecked(), thaw_date_s);
                } catch (SessionNotFoundException e) {
                    try {
                        if (accountAccess.reauthenticate(PlaceHoldActivity.this))
                            temp_result = accountAccess.testAndCreateHold(
                                    record_id, selectedOrgID,
                                    email_notification.isChecked(),  getPhoneNotify(),
                                    getSMSNotify(), getSMSNotifyCarrier(selectedSMSCarrierID),
                                    expire_date_s, suspendHold.isChecked(), thaw_date_s);
                    } catch (Exception e1) {
                    }
                }
                final Result result = temp_result;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logPlaceHoldResult(result.getErrorMessage());
                        if (isFinishing()) return;
                        progress.dismiss();
                        if (result.isSuccess()) {
                            Toast.makeText(PlaceHoldActivity.this, "Hold successfully placed",
                                    Toast.LENGTH_LONG).show();
                            startActivity(new Intent(PlaceHoldActivity.this, HoldsActivity.class));
                            finish();
                        } else if (!isFinishing()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(PlaceHoldActivity.this);
                            builder.setTitle("Failed to place hold")
                                    .setMessage(result.getErrorMessage())
                                    .setPositiveButton(android.R.string.ok, null);
                            builder.create().show();
                        }
                    }
                });
            }
        };
    }

    private String pickupEventValue(Organization pickup_org, Organization home_org) {
        if (home_org == null) { return "homeless"; }
        else if (pickup_org == null) { return "null_pickup"; }
        else if (TextUtils.equals(pickup_org.name, home_org.name)) { return "home"; }
        else if (pickup_org.isConsortium()) { return pickup_org.shortname; }
        else return "other";
    }

    private void logPlaceHoldResult(String result) {
        ArrayList<String> notify = new ArrayList<>();
        if (email_notification.isChecked()) notify.add("email");
        if (phone_notification.isChecked()) notify.add("phone");
        if (sms_notification.isChecked()) notify.add("sms");
        String notifyTypes = TextUtils.join("|", notify);
        try {
            Organization pickup_org = eg.getOrganizations().get(selectedOrgPos);
            Organization home_org = eg.getOrganization(AccountAccess.getInstance().getHomeLibraryID());
            String pickup_val = pickupEventValue(pickup_org, home_org);
            Analytics.logEvent("Place Hold: Execute",
                    "result", result,
                    "hold_notify", notifyTypes,
                    "expires", expire_date != null,
                    "pickup_org", pickup_val);
        } catch(Exception e) {
            Analytics.logException(e);
        }
    }

    private void initPlaceHoldButton() {
        placeHold.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Organization selectedOrg = eg.getOrganizations().get(selectedOrgPos);
                if (!selectedOrg.isPickupLocation()) {
                    logPlaceHoldResult("not_pickup_location");
                    AlertDialog.Builder builder = new AlertDialog.Builder(PlaceHoldActivity.this);
                    builder.setTitle("Failed to place hold")
                            .setMessage(selectedOrg.name + " is not a valid pickup location; choose a different one.")
                            .setPositiveButton(android.R.string.ok, null);
                    builder.create().show();
                } else if (phone_notification.isChecked() && TextUtils.isEmpty(phone_notify.getText().toString())) {
                    phone_notify.setError(getString(R.string.error_phone_notify_empty));
                } else if (sms_notification.isChecked() && TextUtils.isEmpty(sms_notify.getText().toString())) {
                    sms_notify.setError(getString(R.string.error_sms_notify_empty));
                } else {
                    placeHold();
                }
            }
        });
    }

    private void placeHold() {
        Thread placeHoldThread = new Thread(placeHoldRunnable);
        placeHoldThread.start();
    }

    private void initSuspendHoldButton() {
        suspendHold.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                thaw_date_edittext.setEnabled(isChecked);
            }
        });
    }

    private void initPhoneControls(boolean systemwide_phone_enabled) {
        boolean defaultPhoneNotification = accountAccess.getDefaultPhoneNotification();
        String defaultPhoneNumber = accountAccess.getDefaultPhoneNumber();
        if (systemwide_phone_enabled) {
            phone_notification.setChecked(defaultPhoneNotification);
            phone_notify.setText(safeString(defaultPhoneNumber));
            phone_notification.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    phone_notify.setEnabled(isChecked);
                }
            });
            phone_notify.setEnabled(defaultPhoneNotification);
        } else {
            phone_notification_label.setVisibility(View.GONE);
            phone_notification.setVisibility(View.GONE);
            phone_notify.setVisibility(View.GONE);
            // As a special case, we set the checkbox and text field for patrons with phone
            // notification turned on with a phone number, even for apps where the checkbox is hidden.
            // This causes us to set phone_notify=### on holds, which makes it print on hold slips,
            // allowing those few remaining patrons to continue getting notifications by phone.
            if (defaultPhoneNotification && !TextUtils.isEmpty(defaultPhoneNumber)) {
                phone_notification.setChecked(defaultPhoneNotification);
                phone_notify.setText(safeString(defaultPhoneNumber));
            }
        }
    }

    private void initSMSControls(boolean systemwide_sms_enabled) {
        if (systemwide_sms_enabled) {
            boolean isChecked = accountAccess.getDefaultSMSNotification();
            sms_notification.setChecked(isChecked);
            sms_notification.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    sms_spinner.setEnabled(isChecked);
                    sms_notify.setEnabled(isChecked);
                }
            });
            sms_notify.setEnabled(isChecked);
            sms_notify.setText(safeString(accountAccess.getDefaultSMSNumber()));
            sms_spinner.setEnabled(isChecked);
            initSMSSpinner(accountAccess.getDefaultSMSCarrierID());
        } else {
            sms_notification.setChecked(false);
            sms_notification_label.setVisibility(View.GONE);
            sms_spinner_label.setVisibility(View.GONE);
            sms_notification.setVisibility(View.GONE);
            sms_spinner.setVisibility(View.GONE);
            sms_notify.setVisibility(View.GONE);
        }
    }

    private void initSMSSpinner(Integer defaultCarrierID) {
        ArrayList<String> entries = new ArrayList<>();
        List<SMSCarrier> carriers = eg.getSMSCarriers();
        if (carriers == null) {
            // todo Crashed here once.  It seems the async loading of SMS carriers was not done.
            return;
        }
        for (int i = 0; i < carriers.size(); i++) {
            SMSCarrier carrier = carriers.get(i);
            entries.add(carrier.name);
            if (carrier.id.equals(defaultCarrierID)) {
                selectedSMSPos = i;
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.org_item_layout, entries);
        sms_spinner.setAdapter(adapter);
        sms_spinner.setSelection(selectedSMSPos);
        sms_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSMSPos = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void initDatePickers() {
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
    }

    private void initOrgSpinner() {
        Integer defaultLibraryID = AccountAccess.getInstance().getDefaultPickupLibraryID();
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < eg.getOrganizations().size(); i++) {
            Organization org = eg.getOrganizations().get(i);
            list.add(org.getTreeDisplayName());
            if (org.id.equals(defaultLibraryID)) {
                selectedOrgPos = i;
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.org_item_layout, list) {
            @Override
            public boolean isEnabled(int pos) {
                Organization org = eg.getOrganizations().get(pos);
                return org.isPickupLocation();
            }
        };
        orgSpinner.setAdapter(adapter);
        orgSpinner.setSelection(selectedOrgPos);
        orgSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedOrgPos = position;
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> arg0) {
            }
        });
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
