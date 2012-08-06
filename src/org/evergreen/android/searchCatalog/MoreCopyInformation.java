package org.evergreen.android.searchCatalog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.accountAccess.SessionNotFoundException;
import org.evergreen.android.accountAccess.fines.FinesRecord;
import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.globals.NoAccessToServer;
import org.evergreen.android.globals.NoNetworkAccessException;
import org.evergreen.android.globals.Utils;
import org.evergreen.android.views.AccountScreenDashboard;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class MoreCopyInformation extends Activity{


	private Button homeButton;
	
	private Button myAccountButton;
	
	private TextView headerTitle;
	
	private Context context;
	
	private RecordInfo record;
	
	private GlobalConfigs gl;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.copy_information_more);
		gl = GlobalConfigs.getGlobalConfigs(context);
		context = this;
		record = (RecordInfo) getIntent().getSerializableExtra("recordInfo");
		
		 //header portion actions
        homeButton = (Button) findViewById(R.id.library_logo);
        myAccountButton = (Button) findViewById(R.id.my_account_button);
        headerTitle = (TextView) findViewById(R.id.header_title);
        headerTitle.setText(R.string.copy_information_title);
        
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
		
        LayoutInflater inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// insert into main view
		LinearLayout insertPoint = (LinearLayout) findViewById(R.id.record_details_copy_information);
		addCopyInfo(inf, insertPoint);
        
	}

	public void addCopyInfo( LayoutInflater inflater, LinearLayout insertPoint){
    	
    	for(int i=0;i<record.copyInformationList.size();i++){
			
			View copy_info_view = inflater.inflate(R.layout.copy_information, null);

			// fill in any details dynamically here
			TextView library = (TextView) copy_info_view.findViewById(R.id.copy_information_library);
			TextView call_number = (TextView) copy_info_view.findViewById(R.id.copy_information_call_number);
			TextView copy_location = (TextView) copy_info_view.findViewById(R.id.copy_information_copy_location);

		
			
			library.setText(gl.getOrganizationName(record.copyInformationList.get(i).org_id) + " ");
			call_number.setText(record.copyInformationList.get(i).call_number_sufix);
			copy_location.setText(record.copyInformationList.get(i).copy_location);
			
			// insert into main view
			insertPoint.addView(copy_info_view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

			LinearLayout copy_statuses = (LinearLayout) copy_info_view.findViewById(R.id.copy_information_statuses);
			
			
			CopyInformation info = record.copyInformationList.get(i);
			
			Set<Entry<String,String>> set = info.statusInformation.entrySet();
			
			Iterator<Entry<String, String>> it = set.iterator();
			
			while(it.hasNext()){
				
				Entry<String,String> ent = it.next();
				TextView statusName = new TextView(context);
				statusName.setText(ent.getKey() + " : " + ent.getValue());
				
				copy_statuses.addView(statusName, new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
				
			}
		
		}
    	
    }
}
