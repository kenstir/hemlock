package org.evergreen.android.accountAccess.bookbags;

import java.util.ArrayList;
import java.util.List;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.accountAccess.MaxRenewalsException;
import org.evergreen.android.accountAccess.SessionNotFoundException;
import org.evergreen.android.accountAccess.checkout.CircRecord;
import org.evergreen.android.globals.NoAccessToServer;
import org.evergreen.android.globals.NoNetworkAccessException;
import org.evergreen.android.globals.Utils;

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

public class BookbagsListView extends Activity{

	private String TAG = "BookBags";
	
	private AccountAccess accountAccess = null;
	
	private ListView lv;
	
	private BookBagsArrayAdapter listAdapter = null;

	private ArrayList<BookBag> bookBags = null;

	private Context context;
	
	private ProgressDialog progressDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.checkout_list);
		
		context = this;
		accountAccess = AccountAccess.getAccountAccess();
		lv = (ListView) findViewById(R.id.bookbag_list);
		bookBags = new ArrayList<BookBag>();
		listAdapter = new BookBagsArrayAdapter(context, R.layout.bookbag_list_item, bookBags);
		lv.setAdapter(listAdapter);
		
		Thread getBookBags = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				
				try {
					bookBags = accountAccess.getBookbags();
					
				}  catch (NoNetworkAccessException e) {
					Utils.showNetworkNotAvailableDialog(context);
				} catch (NoAccessToServer e) {
					Utils.showServerNotAvailableDialog(context);
					
				}catch (SessionNotFoundException e) {
					//TODO other way?
					try{
						if(accountAccess.authenticate())
							accountAccess.getBookbags();
					}catch(Exception eauth){
						System.out.println("Exception in reAuth");
					}
				}			
	
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						for(int i=0;i<bookBags.size();i++)
							listAdapter.add(bookBags.get(i));
						
						
						progressDialog.dismiss();	
						
						if(bookBags.size() == 0)
							Toast.makeText(context, "No circ records", Toast.LENGTH_LONG);
						
						listAdapter.notifyDataSetChanged();
					}
				});
				
				
			}
		});
		
		
		if(accountAccess.isAuthenticated()){
			progressDialog = new ProgressDialog(context);
			progressDialog.setMessage("Please wait while retrieving Book Bag data");
			progressDialog.show();
			getBookBags.start();
			
		}
		else
			Toast.makeText(context, "You must be authenticated to retrieve circ records", Toast.LENGTH_LONG);

	
				

	}
	
	  class BookBagsArrayAdapter extends ArrayAdapter<BookBag> {
	    	private static final String tag = "BookbagArrayAdapter";
	    	
	    	private TextView name;
	    	private TextView items;
	    	private TextView shared;
	    	
	    	
	    	private List<BookBag> records = new ArrayList<BookBag>();

	    	public BookBagsArrayAdapter(Context context, int textViewResourceId,
	    			List<BookBag> objects) {
	    		super(context, textViewResourceId, objects);
	    		this.records = objects;
	    	}

	    	public int getCount() {
	    		return this.records.size();
	    	}

	    	public BookBag getItem(int index) {
	    		return this.records.get(index);
	    	}

	    	public View getView(int position, View convertView, ViewGroup parent) {
	    		View row = convertView;
	    		
	    		// Get item
	    		final BookBag record = getItem(position);

	    		
	    			//if it is the right type of view
			    		if (row == null ) {
		
				    			Log.d(tag, "Starting XML Row Inflation ... ");
				    			LayoutInflater inflater = (LayoutInflater) this.getContext()
				    					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				    			row = inflater.inflate(R.layout.bookbag_list_item, parent, false);
				    			Log.d(tag, "Successfully completed XML Row Inflation!");
		
			    		}

		    		name = (TextView) row.findViewById(R.id.bookbag_name);
		    		
		    		items = (TextView) row.findViewById(R.id.bookbag_items);
		    		
		    		shared = (TextView) row.findViewById(R.id.bookbag_shared);
		    		
		    		name.setText(record.name+"");
		    		
		    		items.setText("items :" + record.items.size());
		    		
		    		shared.setText(record.shared);
		    		
	    		return row;
	    	}
	    }
}
