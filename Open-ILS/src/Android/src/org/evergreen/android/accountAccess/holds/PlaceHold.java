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
import org.evergreen.android.searchCatalog.RecordInfo;
import org.evergreen.android.searchCatalog.SearchCatalogListView;
import org.evergreen.android.views.AccountScreenDashboard;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
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

public class PlaceHold extends Activity{

	
	private TextView recipient;
	
	private TextView title;
	
	private TextView author;
	
	private TextView physical_description;
	
	private TextView screen_title;
	
	private AccountAccess accountAccess;
	
	private EditText expiration_date;
	
	private EditText phone_number;
	
	private CheckBox phone_notification;
	
	private CheckBox email_notification;
	
	private Button placeHold;
	
	private Button cancel;
	
	private CheckBox suspendHold;
	
	private Spinner orgSelector;
	
	private DatePickerDialog datePicker = null;
	
	private DatePickerDialog thaw_datePicker = null;
	
	private EditText thaw_date_edittext;
	
	private Date expire_date = null;
	
	private Date thaw_date = null;
	
	private Runnable placeHoldRunnable;
	
	private GlobalConfigs globalConfigs = null;
	
	private int selectedOrgPos = 0;
	
	private ImageButton homeButton;
	
	private Button myAccountButton;
	
	private TextView headerTitle;
	
	private ProgressDialog progressDialog;
	
	
	private Context context;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.place_hold);
		globalConfigs = GlobalConfigs.getGlobalConfigs(this);
		RecordInfo record = (RecordInfo) getIntent().getSerializableExtra("recordInfo");
		
		homeButton = (ImageButton) findViewById(R.id.library_logo);
		myAccountButton = (Button) findViewById(R.id.my_acount_button);
		headerTitle = (TextView) findViewById(R.id.header_title);
		headerTitle.setText(R.string.hold_place_title);
		
		myAccountButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(),AccountScreenDashboard.class);
				startActivity(intent);
			}
		});
		
		homeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(),SearchCatalogListView.class);
				startActivity(intent);
			}
		});
		
		context = this;
		
		accountAccess = AccountAccess.getAccountAccess();
		
		recipient = (TextView) findViewById(R.id.hold_recipient);
		title = (TextView) findViewById(R.id.hold_title);
		author = (TextView) findViewById(R.id.hold_author);
		physical_description = (TextView) findViewById(R.id.hold_physical_description);
		screen_title = (TextView) findViewById(R.id.header_title);
		cancel = (Button) findViewById(R.id.cancel_hold);
		placeHold = (Button) findViewById(R.id.place_hold);
		expiration_date = (EditText) findViewById(R.id.hold_expiration_date);
		phone_notification = (CheckBox) findViewById(R.id.hold_enable_phone_notification);
		phone_number= (EditText) findViewById(R.id.hold_contact_telephone);
		email_notification = (CheckBox) findViewById(R.id.hold_enable_email_notification);
		suspendHold = (CheckBox) findViewById(R.id.hold_suspend_hold);
		orgSelector = (Spinner) findViewById(R.id.hold_pickup_location);
		thaw_date_edittext = (EditText) findViewById(R.id.hold_thaw_date);
		screen_title.setText("Place Hold");
		
		recipient.setText(accountAccess.userName);
		title.setText(record.title);
		author.setText(record.author);
		physical_description.setText(record.physical_description);

		//hide edit text
		disableView(thaw_date_edittext);
		disableView(phone_number);
		
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		final Integer record_id = record.doc_id;
		
		placeHoldRunnable = new Runnable() {
			
			@Override
			public void run() {
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						progressDialog = ProgressDialog.show(context, "Please wait", "Placing hold");
					}
				});
				//TODO verify hold possible 
				
				//accountAccess.getHoldPreCreateInfo(record_id, 4);
				//accountAccess.isHoldPossible(4, record_id);
				
				
				String expire_date_s = null;
				String thaw_date_s = null;
				if (expire_date != null)
					expire_date_s = GlobalConfigs.getStringDate(expire_date);
				if (thaw_date != null)
					thaw_date_s = GlobalConfigs.getStringDate(thaw_date);

				System.out.println("date expire: " + expire_date_s + " " + expire_date);
				int selectedOrgID = -1;
				if(globalConfigs.organisations.size() > selectedOrgPos)
					selectedOrgID = globalConfigs.organisations.get(selectedOrgPos).id;
				
				
				
				String[] stringResponse = new String[]{"false"};
				try {
					stringResponse = accountAccess.createHold(record_id,selectedOrgID,email_notification.isChecked(),phone_notification.isChecked(),phone_number.getText().toString(),suspendHold.isChecked(),expire_date_s,thaw_date_s);
				} catch (SessionNotFoundException e) {
					
					try{
						if(accountAccess.authenticate())
							stringResponse = accountAccess.createHold(record_id,selectedOrgID,email_notification.isChecked(),phone_notification.isChecked(),phone_number.getText().toString(),suspendHold.isChecked(),expire_date_s,thaw_date_s);
					}catch(Exception e1){}
					
				} catch (NoNetworkAccessException e) {
					Utils.showNetworkNotAvailableDialog(context);
				} catch (NoAccessToServer e) {
					Utils.showServerNotAvailableDialog(context);
				}
			
				final String[] holdPlaced = stringResponse;
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						progressDialog.dismiss();	
						
						if(holdPlaced[0].equals("true")){
							Toast.makeText(context, "Hold Succesfully placed", Toast.LENGTH_LONG).show();
							finish();	
							}
						else
							Toast.makeText(context, "Error in placing hold : " + holdPlaced[2], Toast.LENGTH_LONG).show();
						
							
					}
				});
			}
		};
		
		placeHold.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				Thread placeholdThread = new Thread(placeHoldRunnable);
				placeholdThread.start();
			}
		});
		
		phone_notification.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				if(isChecked){
					enableView(phone_number);
				}
				else
					disableView(phone_number);
			}
		});
		
		suspendHold.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				if(isChecked){
					enableView(thaw_date_edittext);
				}else
				{
					disableView(thaw_date_edittext);
				}
			}
		});
		
		Calendar cal = Calendar.getInstance();
		
		datePicker = new DatePickerDialog(this,
		         new DatePickerDialog.OnDateSetListener() {
		 
		         public void onDateSet(DatePicker view, int year,
		                                             int monthOfYear, int dayOfMonth)
		         {
		        	 
		        	 		
		                    Date chosenDate = new Date(year-1900, monthOfYear,dayOfMonth);                 
		                    expire_date = chosenDate;
		                    CharSequence strDate = DateFormat.format("MMMM dd, yyyy", chosenDate);
		                    expiration_date.setText(strDate);
		                    //set current date          
		        }}, cal.get(Calendar.YEAR),cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
	
		expiration_date.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				datePicker.show();
			}
		});
		
		thaw_datePicker = new DatePickerDialog(this,
		         new DatePickerDialog.OnDateSetListener() {
		 
		         public void onDateSet(DatePicker view, int year,
		                                             int monthOfYear, int dayOfMonth)
		         {
		        	 
		        	 
		                    Date chosenDate = new Date(year-1900, monthOfYear,dayOfMonth);                 
		                    thaw_date = chosenDate;
		                    CharSequence strDate = DateFormat.format("MMMM dd, yyyy", chosenDate);
		                    thaw_date_edittext.setText(strDate);
		                    //set current date          
		        }}, cal.get(Calendar.YEAR),cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
	
		thaw_date_edittext.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				thaw_datePicker.show();	
			}
		});
		
		ArrayList<String> list = new ArrayList<String>();
        for(int i=0;i<globalConfigs.organisations.size();i++){
        	list.add(globalConfigs.organisations.get(i).padding + globalConfigs.organisations.get(i).name);
        	
        	if(globalConfigs.organisations.get(i).level -1 == 0)
        		selectedOrgPos = i;
        }
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,list);
        orgSelector.setAdapter(adapter);
        
        orgSelector.setSelection(selectedOrgPos);
        
        orgSelector.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int ID, long arg3) {
			
				selectedOrgPos = ID;
				
			}
			
			public void onNothingSelected(android.widget.AdapterView<?> arg0) {
				}
        });
	}
	public void disableView(View view){
		
		//view.setFocusable(false);
		view.setFocusable(false);
		
		view.setBackgroundColor(Color.argb(255, 100, 100, 100));
		//view.setVisibility(View.INVISIBLE);
	}
	
	public void enableView(View view){
		//view.setVisibility(View.VISIBLE);
		
		view.setFocusableInTouchMode(true);
		
		view.setBackgroundColor(Color.argb(255, 255,255,255));
	}
	
}
