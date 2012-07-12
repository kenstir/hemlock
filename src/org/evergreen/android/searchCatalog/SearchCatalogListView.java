package org.evergreen.android.searchCatalog;

import java.util.ArrayList;
import java.util.List;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.holds.PlaceHold;
import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.globals.NoAccessToServer;
import org.evergreen.android.globals.NoNetworkAccessException;
import org.evergreen.android.globals.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class SearchCatalogListView extends Activity{

	private String TAG ="SearchCatalogListView";
	
	private List<RecordInfo> recordList;
	
	private EditText searchText;
	
	private ImageButton searchButton;

	private SearchCatalog search;
	
	private ListView lv;
	
	private SearchArrayAdapter adapter;
	
	private Context context;
	
	private ProgressDialog progressDialog;
	
	private ArrayList<RecordInfo> searchResults;
	
	private Spinner choseOrganisation;
	
	private GlobalConfigs globalConfigs;
	
	private static final int PLACE_HOLD = 0;
	
	private static final int DETAILS = 1;

	private TextView searchResultsNumber;
	
	private final ImageDownloader imageDownloader = new ImageDownloader();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_result_list);

        //singleton initialize necessary IDL and Org data
        globalConfigs = GlobalConfigs.getGlobalConfigs(this);
        
        context = this;
        search = SearchCatalog.getInstance((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE));
                
        recordList= new ArrayList<RecordInfo>();

        // Create a customized ArrayAdapter
        adapter = new SearchArrayAdapter(
    				getApplicationContext(), R.layout.search_result_item, recordList);
    	
        searchResultsNumber = (TextView) findViewById(R.id.search_result_number);
        
    	// Get reference to ListView holder
    	lv = (ListView) this.findViewById(R.id.search_results_list);
    	
    	 
    	// Creating a button - Load More
    	Button btnLoadMore = new Button(this);
    	btnLoadMore.setText("Load More");
    	
    	// Adding button to listview at footer
    	//lv.addFooterView(btnLoadMore);
    	
    	View footerView = findViewById(R.layout.search_result_footer_view);
    	//call before set adapter
    	//lv.addFooterView(footerView);
    	
    	//System.out.println("Here it is "  + lv);
    	
		progressDialog = new ProgressDialog(context);
		
		progressDialog.setMessage("Fetching data");
    	// Set the ListView adapter
    	lv.setAdapter(adapter);

    	searchResults = new ArrayList<RecordInfo>();
    	
    	registerForContextMenu(lv);
    	
    	lv.setOnItemClickListener(new OnItemClickListener() {
    		
    		@Override
    		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
    				long arg3) {
    			
    		
    			RecordInfo info = (RecordInfo)lv.getItemAtPosition(position);
    			
    			if(info.dummy == true){
    				//this is the more view item button 
    				progressDialog = new ProgressDialog(context);
    				
    				progressDialog.setMessage("Fetching data");
    				progressDialog.show();
    				final String text = searchText.getText().toString();
    				
    				Thread searchThreadwithOffset = new Thread(new Runnable() {
						
						@Override
						public void run() {
							
							searchResults.clear();
							
							try {
								searchResults = search.getSearchResults(text,recordList.size()-1);
							} catch (NoNetworkAccessException e) {
								runOnUiThread(Utils.showNetworkNotAvailableDialog(context));
							} catch (NoAccessToServer e) {
								runOnUiThread(Utils.showServerNotAvailableDialog(context));
							}
							
							runOnUiThread(new Runnable() {
								
								@Override
								public void run() {

									//don't clear record list
									//recordList.clear();
									if(searchResults.size()>0){
										
										//remove previous more button
										recordList.remove(recordList.size()-1);
										
										for(int j=0;j<searchResults.size();j++)
											recordList.add(searchResults.get(j));
										
									//add extra record to display more option button
									if(search.visible > recordList.size()){
											recordList.add(new RecordInfo());
											searchResultsNumber.setText(adapter.getCount()-1 +" out of "+search.visible);
										}
									else
										searchResultsNumber.setText(adapter.getCount() +" out of "+search.visible);
									}
									else{
										searchResultsNumber.setText(adapter.getCount() +" out of "+search.visible);
									}
									adapter.notifyDataSetChanged();
									progressDialog.dismiss();
								}
							});
							
						}
					});
					
					searchThreadwithOffset.start();
    			}
    			else{
	    			//start activity with book details
	    			
	    			Intent intent = new Intent(getBaseContext(),TabsView.class);
	    			//serialize object and pass it to next activity
	    			intent.putExtra("recordInfo", info);
	    			intent.putExtra("orgID",search.selectedOrganization.id);
	    			intent.putExtra("depth",(search.selectedOrganization.level-1));
	    			startActivity(intent);
    			}
    		}
		});
        
        searchText = (EditText) findViewById(R.id.searchText);
 
        choseOrganisation = (Spinner) findViewById(R.id.chose_organisation);
        
        searchButton = (ImageButton) findViewById(R.id.searchButton);

        searchButton.setOnClickListener(new OnClickListener() {
	
        
			@Override
			public void onClick(View v) {
				
				final String text = searchText.getText().toString();				
				progressDialog = new ProgressDialog(context);
				
				progressDialog.setMessage("Fetching data");
				//progressDialog.show();
				
				if(text.length()>0){
					
					Thread searchThread = new Thread(new Runnable() {
						
						@Override
						public void run() {
							
							searchResults.clear();
							
							try {
								searchResults = search.getSearchResults(text,0);
							} catch (NoNetworkAccessException e) {	
								System.out.println("no network access in search");
								SearchCatalogListView.this.runOnUiThread(Utils.showNetworkNotAvailableDialog(context));
								
							} catch (NoAccessToServer e) {
								SearchCatalogListView.this.runOnUiThread(Utils.showServerNotAvailableDialog(context));								
							}
								
							runOnUiThread(new Runnable() {
								
								@Override
								public void run() {

									recordList.clear();
									
									if(searchResults.size()>0){
										
										for(int j=0;j<searchResults.size();j++)
											recordList.add(searchResults.get(j));
										
									//add extra record to display more option button
									if(search.visible > recordList.size()){
										recordList.add(new RecordInfo());
										searchResultsNumber.setText(recordList.size()-1 +" out of "+search.visible);
										}
									else
										searchResultsNumber.setText(recordList.size() +" out of "+search.visible);
									}
									else
										searchResultsNumber.setText(recordList.size() +" out of "+search.visible);
									
									adapter.notifyDataSetChanged();
									progressDialog.dismiss();
									
									
								}
							});
							
						}
					});
					
					searchThread.start();

				}
			}
		});

        
        int selectedPos = 0;
        ArrayList<String> list = new ArrayList<String>();
        if(globalConfigs.organisations != null){
	        for(int i=0;i<globalConfigs.organisations.size();i++){
	        	list.add(globalConfigs.organisations.get(i).padding + globalConfigs.organisations.get(i).name);
	        	
	        	if(globalConfigs.organisations.get(i).level -1 == 0)
	        		selectedPos = i;
	        }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,list);
        choseOrganisation.setAdapter(adapter);
    	
        choseOrganisation.setSelection(selectedPos);
        
        choseOrganisation.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int ID, long arg3) {
				//select the specific organization
				search.selectOrganisation(globalConfigs.organisations.get(ID));
				
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
        
        });
    
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
        ContextMenuInfo menuInfo) {
    	
    	Log.d(TAG, "context menu");
      if (v.getId()==R.id.search_results_list) {
    	  
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        menu.setHeaderTitle("Options");
        
          menu.add(Menu.NONE, DETAILS,0,"Details");
          menu.add(Menu.NONE,PLACE_HOLD,1,"Place Hold");
        
      }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterView.AdapterContextMenuInfo menuArrayItem = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
    	int menuItemIndex = item.getItemId();
    	
    	RecordInfo info = (RecordInfo)lv.getItemAtPosition(menuArrayItem.position);
		//start activity with book details
		
    	
    	switch(item.getItemId()){
    		
    		case DETAILS : {
       			
    			Intent intent = new Intent(getBaseContext(),RecordDetails_Simple.class);
    			//serialize object and pass it to next activity
    			intent.putExtra("recordInfo", info);
    			
    			startActivity(intent);
    		}
    		break;
    		case PLACE_HOLD : {

    			Intent intent = new Intent(getBaseContext(),PlaceHold.class);
    			
    			intent.putExtra("recordInfo", info);
    			
    			startActivity(intent);
    		}
    		break;
    	}
    	
    	return super.onContextItemSelected(item);
    }
    
    class SearchArrayAdapter extends ArrayAdapter<RecordInfo> {
    	private static final String tag = "SearchArrayAdapter";
    	private Context context;
    	private ImageView recordImage;
    	private TextView recordTitle;
    	private TextView recordAuthor;
    	private TextView recordPublisher;
    	
    	private List<RecordInfo> records = new ArrayList<RecordInfo>();

    	public SearchArrayAdapter(Context context, int textViewResourceId,
    			List<RecordInfo> objects) {
    		super(context, textViewResourceId, objects);
    		this.context = context;
    		this.records = objects;
    	}

    	public int getCount() {
    		return this.records.size();
    	}

    	public RecordInfo getItem(int index) {
    		return this.records.get(index);
    	}

    	public View getView(int position, View convertView, ViewGroup parent) {
    		View row = convertView;
    		
    		// Get item
    		RecordInfo record = getItem(position);
    		
    		if(record.dummy == true)
    		{
				Log.d(tag, "Starting XML view more infaltion ... ");
    			LayoutInflater inflater = (LayoutInflater) this.getContext()
    					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			row = inflater.inflate(R.layout.search_result_footer_view, parent, false);
    			Log.d(tag, "Successfully completed XML view more Inflation!");

    			
			}
    		else{
    		
    			//if it is the right type of view
		    		if (row == null || row.findViewById(R.id.search_record_title) == null) {
	
			    			Log.d(tag, "Starting XML Row Inflation ... ");
			    			LayoutInflater inflater = (LayoutInflater) this.getContext()
			    					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			    			row = inflater.inflate(R.layout.search_result_item, parent, false);
			    			Log.d(tag, "Successfully completed XML Row Inflation!");
	
		    		}
    		
		    

    		Log.d(TAG, "reord image value " + recordImage);
    		// Get reference to ImageView 
    		recordImage = (ImageView) row.findViewById(R.id.search_record_img);
    		//TODO fix bugs + features
    		String imageHref= GlobalConfigs.httpAddress + "/opac/extras/ac/jacket/small/"+record.isbn;
    		//start async download of image 
    		imageDownloader.download(imageHref, recordImage);
    		// Get reference to TextView - title
    		recordTitle = (TextView) row.findViewById(R.id.search_record_title);
    		
    		// Get reference to TextView - author
    		recordAuthor = (TextView) row.findViewById(R.id.search_record_author);

    		//Get referance to TextView - record Publisher date+publisher
    		recordPublisher = (TextView) row.findViewById(R.id.search_record_publishing);
    
    		//set text
    		
    		recordTitle.setText(record.title);
    		recordAuthor.setText(record.author);
    		recordPublisher.setText(record.pubdate + " " + record.publisher);
    		}
    		
    		return row;
    	}
    }

}


