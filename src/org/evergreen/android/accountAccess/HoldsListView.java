package org.evergreen.android.accountAccess;

import java.util.ArrayList;
import java.util.List;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.ItemsCheckOutListView.CheckOutArrayAdapter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class HoldsListView extends Activity{

	private String TAG = "HoldsListView";
	
	private AccountAccess accountAccess = null;
	
	private ListView lv;
	
	private HoldsArrayAdapter listAdapter = null;

	private ArrayList<CircRecord> holdRecords = null;

	private Context context;
	
	private ProgressDialog progressDialog;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.holds_list);
		
		context = this;
		accountAccess = AccountAccess.getAccountAccess();
		
		accountAccess.getHolds();
		/*
		lv = (ListView) findViewById(R.id.checkout_items_list);
		
		holdRecords = new ArrayList<CircRecord>();
		listAdapter = new HoldsArrayAdapter(context, R.layout.holds_list_item, holdRecords);
		lv.setAdapter(listAdapter);
		*/
		
		
	}
	
	
	class HoldsArrayAdapter extends ArrayAdapter<CircRecord> {
    	private static final String tag = "CheckoutArrayAdapter";
    	
    	private TextView holdTitle;
    	private TextView holdAuthor;
    	private TextView expirationDate;
    	private TextView status;
    	private TextView active;
    	
    	private List<CircRecord> records = new ArrayList<CircRecord>();

    	public HoldsArrayAdapter(Context context, int textViewResourceId,
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
	    		holdTitle = (TextView) row.findViewById(R.id.hold_title);
	    		
	    		// Get reference to TextView author
	    		holdAuthor = (TextView) row.findViewById(R.id.hold_author);
	    		
	    		//Get hold expiration date
	    		expirationDate = (TextView) row.findViewById(R.id.hold_expiration_date);
	    
	    		//Get hold status
	    		status = (TextView) row.findViewById(R.id.hold_status);
	    		
	    		active = (TextView) row.findViewById(R.id.hold_active);
	    		
	    		
	    		//set text
	    		/*
	    		System.out.println("Row" + record.getTitle() + " " + record.getAuthor() + " " + record.getDueDate() + " " + record.getRenewals());
	    		recordTitle.setText(record.getTitle());
	    		recordAuthor.setText(record.getAuthor());
	    		recordDueDate.setText(record.getDueDate());
	    		recordRenewals.setText(record.getRenewals()+"");
	    		*/
    		}
    		
    		return row;
    	}
    }
}
