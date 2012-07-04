package org.evergreen.android.accountAccess.holds;

import java.util.Calendar;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.AccountAccess;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

public class HoldDetails extends Activity{

	
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
	
	private DatePickerDialog datePicker = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.hold_details);
		
		final HoldRecord record = (HoldRecord) getIntent().getSerializableExtra("holdRecord");
		
		accountAccess = AccountAccess.getAccountAccess();
		
		recipient = (TextView) findViewById(R.id.hold_recipient);
		title = (TextView) findViewById(R.id.hold_title);
		author = (TextView) findViewById(R.id.hold_author);
		physical_description = (TextView) findViewById(R.id.hold_physical_description);
		screen_title = (TextView) findViewById(R.id.header_title);
		cancelHold = (Button) findViewById(R.id.cancel_hold_button);
		updateHold = (Button) findViewById(R.id.update_hold_button);
		back = (Button) findViewById(R.id.back_button);
		
		expiration_date = (EditText) findViewById(R.id.hold_expiration_date);
		
		screen_title.setText("Place Hold");
		
		recipient.setText(accountAccess.userName);
		title.setText(record.title);
		author.setText(record.author);
		if(record.recordInfo != null)
			physical_description.setText(record.recordInfo.physical_description);
		
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
			

				Builder confirmationDialogBuilder = new AlertDialog.Builder(getApplicationContext());
			    confirmationDialogBuilder.setMessage(R.string.cancel_hold_dialog_message);
			    
			    confirmationDialogBuilder.setNegativeButton(android.R.string.no, null);
			    confirmationDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			      @Override
			      public void onClick(DialogInterface dialog, int which) {
			        
			    	  System.out.println("Remove hold with id" + record.ahr.getInt("id"));
			    	  accountAccess.cancelHold(record.ahr);
			      }
			    });
			    confirmationDialogBuilder.create().show();
				
			}
		});
		
		updateHold.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				//update new values
				accountAccess.updateHold(record.ahr);
			}
		});
		
		Calendar cal = Calendar.getInstance();
		datePicker = new DatePickerDialog(this,
		         new DatePickerDialog.OnDateSetListener() {
		 
		         public void onDateSet(DatePicker view, int year,
		                                             int monthOfYear, int dayOfMonth)
		         {
		                    Time chosenDate = new Time();
		                    chosenDate.set(dayOfMonth, monthOfYear, year);
		                    long dtDob = chosenDate.toMillis(true);
		                    CharSequence strDate = DateFormat.format("MMMM dd, yyyy", dtDob);
		                    expiration_date.setText(strDate);
		                    //set current date          
		        }}, cal.get(Calendar.YEAR),cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
	
		expiration_date.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				datePicker.show();
			}
		});
		
	}
	
}
