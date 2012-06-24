package org.evergreen.android.accountAccess;

import java.util.ArrayList;
import java.util.List;

import org.evergreen.android.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ItemsCheckOutListView extends Activity{

	private String TAG = "ItemsCheckOutListView";
	
	private AccountAccess accountAccess = null;
	
	private ListView lv;
	
	private CheckOutArrayAdapter listAdapter = null;

	private ArrayList<CircRecord> circRecords = null;

	private Context context;
	
	private ProgressDialog progressDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.checkout_list);
		
		context = this;
		accountAccess = AccountAccess.getAccountAccess();
		
		lv = (ListView) findViewById(R.id.checkout_items_list);
		
		circRecords = accountAccess.getItemsCheckedOut();
		
		listAdapter = new CheckOutArrayAdapter(this, R.layout.checkout_list_item, circRecords);
		lv.setAdapter(listAdapter);
		

		
		listAdapter.notifyDataSetChanged();

	}
	
	  class CheckOutArrayAdapter extends ArrayAdapter<CircRecord> {
	    	private static final String tag = "CheckoutArrayAdapter";
	    	
	    	private TextView recordTitle;
	    	private TextView recordAuthor;
	    	private TextView recordDueDate;
	    	private TextView recordRenewals;
	    	private TextView renewButton;
	    	
	    	private List<CircRecord> records = new ArrayList<CircRecord>();

	    	public CheckOutArrayAdapter(Context context, int textViewResourceId,
	    			List<CircRecord> objects) {
	    		super(context, textViewResourceId, objects);
	    		this.records = objects;
	    	}

	    	public int getCount() {
	    		return this.records.size();
	    	}

	    	public CircRecord getItem(int index) {
	    		return this.records.get(index);
	    	}

	    	public View getView(int position, View convertView, ViewGroup parent) {
	    		View row = convertView;
	    		
	    		// Get item
	    		final CircRecord record = getItem(position);
	    		
	    		
	    		if(record == null)
	    		{
					Log.d(tag, "Starting XML view more infaltion ... ");
	    			LayoutInflater inflater = (LayoutInflater) this.getContext()
	    					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    			row = inflater.inflate(R.layout.search_result_footer_view, parent, false);
	    			Log.d(tag, "Successfully completed XML view more Inflation!");

	    			
				}
	    		else{
	    		
	    			//if it is the right type of view
			    		if (row == null ) {
		
				    			Log.d(tag, "Starting XML Row Inflation ... ");
				    			LayoutInflater inflater = (LayoutInflater) this.getContext()
				    					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				    			row = inflater.inflate(R.layout.checkout_list_item, parent, false);
				    			Log.d(tag, "Successfully completed XML Row Inflation!");
		
			    		}

		    		// Get reference to TextView - title
		    		recordTitle = (TextView) row.findViewById(R.id.checkout_record_title);
		    		
		    		// Get reference to TextView - author
		    		recordAuthor = (TextView) row.findViewById(R.id.checkout_record_author);
		    		
		    		//Get reference to TextView - record Publisher date+publisher
		    		recordDueDate = (TextView) row.findViewById(R.id.checkout_due_date);
		    
		    		//Get remaining renewals
		    		recordRenewals = (TextView) row.findViewById(R.id.checkout_renewals_remaining);
		    		
		    		renewButton = (TextView) row.findViewById(R.id.renew_button);
		    		
		    		renewButton.setText("renew : " + record.getRenewals());
		    		
		    		renewButton.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
								
								Thread renew = new Thread(new Runnable() {
									
									@Override
									public void run() {
										boolean refresh = true;
										AccountAccess ac = AccountAccess.getAccountAccess();
										
										runOnUiThread(new Runnable() {	
											@Override
											public void run() {
												progressDialog = new ProgressDialog(context);
												progressDialog.setMessage("Renew item please wait.");
												progressDialog.show();
											}
										});
										
										try{
											ac.renewCirc(record.getTargetCopy());
										}catch(MaxRenewalsException e){
											runOnUiThread(new Runnable() {
												
												@Override
												public void run() {
													progressDialog.dismiss();
													Toast.makeText(context, "Max renewals reached", Toast.LENGTH_SHORT).show();
												}
											});
											
											refresh = false;
										}
										if(refresh){
											circRecords = accountAccess.getItemsCheckedOut();
											listAdapter.clear();
											for(int i=0;i<circRecords.size();i++){
												listAdapter.add(circRecords.get(i));
											}
											runOnUiThread(new Runnable() {
												
												@Override
												public void run() {
													progressDialog.dismiss();
													listAdapter.notifyDataSetChanged();
												}
											});
										}
									}
								});
								
								renew.start();
						}
					});
		    		//set text
		    		System.out.println("Row" + record.getTitle() + " " + record.getAuthor() + " " + record.getDueDate() + " " + record.getRenewals());
		    		recordTitle.setText(record.getTitle());
		    		recordAuthor.setText(record.getAuthor());
		    		recordDueDate.setText(record.getDueDate());
		    		recordRenewals.setText(record.getRenewals()+"");
	    		}
	    		
	    		return row;
	    	}
	    }
}
