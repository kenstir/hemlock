package org.evergreen.android.searchCatalog;

import java.util.ArrayList;
import java.util.List;

import org.evergreen.android.R;
import org.evergreen.android.globals.GlobalConfigs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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

	private final ImageDownloader imageDownloader = new ImageDownloader();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_result_list);

        //singleton initialize necessary IDL and Org data
        globalConfigs = GlobalConfigs.getGlobalConfigs(this);
        
        context = this;
        search = new SearchCatalog(this);
                
        recordList= new ArrayList<RecordInfo>();

        // Create a customized ArrayAdapter
        adapter = new SearchArrayAdapter(
    				getApplicationContext(), R.layout.search_result_item, recordList);
    		
    	// Get reference to ListView holder
    	lv = (ListView) this.findViewById(R.id.search_results_list);
    	
    	//System.out.println("Here it is "  + lv);
    	// Set the ListView adapter
    	lv.setAdapter(adapter);
    	
    	
    	
    	registerForContextMenu(lv);
    	
    	lv.setOnItemClickListener(new OnItemClickListener() {
    		
    		@Override
    		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
    				long arg3) {
    			
    		
    			RecordInfo info = (RecordInfo)lv.getItemAtPosition(position);
    			//start activity with book details
    			
    			Intent intent = new Intent(getBaseContext(),RecordDetails_Simple.class);
    			//serialize object and pass it to next activity
    			intent.putExtra("recordInfo", info);
    			
    			startActivity(intent);
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
				progressDialog.show();
				
				if(text.length()>0){
					
					Thread searchThread = new Thread(new Runnable() {
						
						@Override
						public void run() {
							searchResults = search.getSearchResults(text);
							
							runOnUiThread(new Runnable() {
								
								@Override
								public void run() {

									recordList.clear();
									if(searchResults.size()>0){
										
										for(int j=0;j<searchResults.size();j++)
											recordList.add(searchResults.get(j));
									}
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
        for(int i=0;i<globalConfigs.organisations.size();i++){
        	list.add(globalConfigs.organisations.get(i).name);
        	
        	if(globalConfigs.organisations.get(i).level -1 == 0)
        		selectedPos = i;
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
    	
    	switch(item.getItemId()){
    		
    		case DETAILS : {
       			RecordInfo info = (RecordInfo)lv.getItemAtPosition(menuArrayItem.position);
    			//start activity with book details
    			
    			Intent intent = new Intent(getBaseContext(),RecordDetails_Simple.class);
    			//serialize object and pass it to next activity
    			intent.putExtra("recordInfo", info);
    			
    			startActivity(intent);
    		}
    		break;
    		case PLACE_HOLD : {
    			//TODO
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
    		if (row == null) {
    			// ROW INFLATION
    			Log.d(tag, "Starting XML Row Inflation ... ");
    			LayoutInflater inflater = (LayoutInflater) this.getContext()
    					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			row = inflater.inflate(R.layout.search_result_item, parent, false);
    			Log.d(tag, "Successfully completed XML Row Inflation!");
    		}

    		// Get item
    		RecordInfo record = getItem(position);
    		
    		// Get reference to ImageView 
    		recordImage = (ImageView) row.findViewById(R.id.search_record_img);
    		
    		//TODO fix bugs + features
    		String imageHref= GlobalConfigs.httpAddress + "/opac/extras/ac/jacket/small/"+record.doc_id;
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

    		return row;
    	}
    }

}


