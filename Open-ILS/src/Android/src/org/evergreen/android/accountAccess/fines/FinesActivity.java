package org.evergreen.android.accountAccess.fines;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.AccountAccess;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

public class FinesActivity extends Activity{

	
	private TextView total_owned;
	
	private TextView total_paid;
	
	private TextView balance_owed;
	
	private ListView overdue_materials;
	
	private Runnable getFinesInfo;
	
	private AccountAccess ac;
	
	private ProgressDialog progressDialog;
	
	private Context context;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.fines);
		
		total_owned = (TextView) findViewById(R.id.fines_total_owned);
		total_paid = (TextView) findViewById(R.id.fines_total_paid);
		balance_owed = (TextView) findViewById(R.id.fined_balance_owed);
		context = this;
		
		ac = AccountAccess.getAccountAccess();
		
		
		progressDialog = ProgressDialog.show(this, null, "Retrieving fines");
		
		getFinesInfo = new Runnable() {
			@Override
			public void run() {
				
				final float[] fines = ac.getFinesSummary();
				
				runOnUiThread(new Runnable() {		
					@Override
					public void run() {	
						
						total_owned.setText(fines[0]+"");
						total_paid.setText(fines[1]+"");
						balance_owed.setText(fines[2]+"");
						progressDialog.dismiss();
					}
				});
			}
		};
		
		Thread getFinesTh = new Thread(getFinesInfo);
		getFinesTh.start();
	}
}
