package org.evergreen.android.accountAccess.holds;

import java.util.ArrayList;
import java.util.List;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.accountAccess.SessionNotFoundException;
import org.evergreen.android.globals.NoAccessToServer;
import org.evergreen.android.globals.NoNetworkAccessException;
import org.evergreen.android.globals.Utils;
import org.evergreen.android.searchCatalog.SearchCatalogListView;
import org.evergreen.android.views.AccountScreenDashboard;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class HoldsListView extends Activity{

	private String TAG = "HoldsListView";
	
	private AccountAccess accountAccess = null;
	
	private ListView lv;
	
	private HoldsArrayAdapter listAdapter = null;

	private List<HoldRecord> holdRecords = null;

	private Context context;
	
	Runnable getHoldsRunnable= null;
	
	private Button homeButton;
	
	private Button myAccountButton;
	
	private TextView headerTitle;

	private ProgressDialog progressDialog;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.holds_list);
		
		 //header portion actions
        homeButton = (Button) findViewById(R.id.library_logo);
        myAccountButton = (Button) findViewById(R.id.my_acount_button);
        headerTitle = (TextView) findViewById(R.id.header_title);
        headerTitle.setText(R.string.hold_items_title);
        
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
        //end header portion actions
		
		lv = (ListView)findViewById(R.id.holds_item_list);
		context = this;
		accountAccess = AccountAccess.getAccountAccess();

		holdRecords = new ArrayList<HoldRecord>();
		listAdapter = new HoldsArrayAdapter(context, R.layout.holds_list_item, holdRecords);
		lv.setAdapter(listAdapter);
		
		getHoldsRunnable = new Runnable() {
			@Override
			public void run() {
				
				try {
					holdRecords = accountAccess.getHolds();
				} catch (SessionNotFoundException e) {
					//TODO other way?
					try{
						if(accountAccess.authenticate())
							holdRecords = accountAccess.getHolds();
					}catch(Exception eauth){
						System.out.println("Exception in reAuth");
					}
				} catch (NoNetworkAccessException e) {
					Utils.showNetworkNotAvailableDialog(context);
				} catch (NoAccessToServer e) {
					Utils.showServerNotAvailableDialog(context);
				}
			
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						listAdapter.clear();
						
						for(int i=0;i<holdRecords.size();i++)
							listAdapter.add(holdRecords.get(i));
						
						progressDialog.dismiss();
						listAdapter.notifyDataSetChanged();
						
					}
				});
			}
		};
		
		if(accountAccess.isAuthenticated()){
			progressDialog = new ProgressDialog(context);
			progressDialog.setMessage("Loading holds");
			progressDialog.show();
			
			//thread to retrieve holds
			Thread getHoldsThread = new Thread(getHoldsRunnable);
			getHoldsThread.start();
			
		}
		else
			Toast.makeText(context, "You must be authenticated to retrieve circ records", Toast.LENGTH_LONG);

		
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {
					HoldRecord record = (HoldRecord) lv.getItemAtPosition(position);
					
					Intent intent = new Intent(getApplicationContext(),HoldDetails.class);
					
					intent.putExtra("holdRecord", record);
					
					//doae not matter request code, only result code
					startActivityForResult(intent, 0);
			}
		});
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		
		switch(resultCode){
		
		case HoldDetails.RESULT_CODE_CANCEL : { 
			//nothing
			}break;
			
		case HoldDetails.RESULT_CODE_DELETE_HOLD : {
			//refresh ui
			progressDialog = new ProgressDialog(context);
			progressDialog.setMessage("Loading holds");
			progressDialog.show();
			//thread to retrieve holds
			Thread getHoldsThread = new Thread(getHoldsRunnable);
			getHoldsThread.start();
		}break;
		
		case HoldDetails.RESULT_CODE_UPDATE_HOLD : {
			//refresh ui
			progressDialog = new ProgressDialog(context);
			progressDialog.setMessage("Loading holds");
			progressDialog.show();
			//thread to retrieve holds
			Thread getHoldsThread = new Thread(getHoldsRunnable);
			getHoldsThread.start();
		}break;
		
		}
	}
	
	class HoldsArrayAdapter extends ArrayAdapter<HoldRecord> {
    	private static final String tag = "CheckoutArrayAdapter";
    	
    	private TextView holdTitle;
    	private TextView holdAuthor;
    	private TextView status;
    	
    	private List<HoldRecord> records = new ArrayList<HoldRecord>();

    	public HoldsArrayAdapter(Context context, int textViewResourceId,
    			List<HoldRecord> objects) {
    		super(context, textViewResourceId, objects);
    		this.records = objects;
    	}

    	public int getCount() {
    		return this.records.size();
    	}

    	public HoldRecord getItem(int index) {
    		return this.records.get(index);
    	}

    	public View getView(int position, View convertView, ViewGroup parent) {
    		View row = convertView;
    		
    		// Get item
    		final HoldRecord record = getItem(position);
    		
    		if(row == null){
	
    			Log.d(tag, "Starting XML view more infaltion ... ");
    			LayoutInflater inflater = (LayoutInflater) this.getContext()
    					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			row = inflater.inflate(R.layout.holds_list_item, parent, false);
    			Log.d(tag, "Successfully completed XML view more Inflation!");


    		}
    		// Get reference to TextView - title
    		holdTitle = (TextView) row.findViewById(R.id.hold_title);
    		
    		// Get reference to TextView author
    		holdAuthor = (TextView) row.findViewById(R.id.hold_author);

    		//Get hold status
    		status = (TextView) row.findViewById(R.id.hold_status);
    		
	    		//set text
	    		
	    	System.out.println("Row" + record.title + " " + record.author + " " + record.getHoldStatus());
	    	//set raw information
	    	holdTitle.setText(record.title);
	    	holdAuthor.setText(record.author);
	    	status.setText(record.getHoldStatus());
    		
    		
    		return row;
    	}
    }
}
