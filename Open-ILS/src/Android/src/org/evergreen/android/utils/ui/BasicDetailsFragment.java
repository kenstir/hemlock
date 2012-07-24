package org.evergreen.android.utils.ui;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.Inflater;

import org.evergreen.android.R;
import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.searchCatalog.CopyInformation;
import org.evergreen.android.searchCatalog.RecordInfo;
import org.evergreen.android.searchCatalog.SearchCatalog;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BasicDetailsFragment extends Fragment{

	
	private RecordInfo record;
	private Integer position;
	private Integer total;
	
	private TextView record_header;
	
	private TextView titleTextView;
	private TextView authorTextView;
	private TextView publisherTextView;
	
	private TextView seriesTextView;
	private TextView subjectTextView;
	private TextView synopsisTextView;
	private TextView isbnTextView;
	
	private TextView copyCountTestView;
	
	private LinearLayout showMore;
	
	private SearchCatalog search = null;
	
	private GlobalConfigs gl;

	//max display info
	private int list_size = 3;
	
	    public static BasicDetailsFragment newInstance(RecordInfo record, Integer position, Integer total) {
	    	BasicDetailsFragment fragment = new BasicDetailsFragment(record,position,total);
	    	
	        return fragment;
	    }

	    public BasicDetailsFragment(RecordInfo record, Integer position, Integer total){
	    	
	    	this.record = record;
	    	this.position = position;
	    	this.total = total;

	    	search = SearchCatalog.getInstance();
	    }

	    public BasicDetailsFragment(){

	    	search = SearchCatalog.getInstance();
	    }
	    
	    
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);

	    }

	    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    
	    	
	    	gl = GlobalConfigs.getGlobalConfigs(getActivity());
	    	
	    	LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.record_details_basic_fragment, null);

	    	record_header = (TextView) layout.findViewById(R.id.record_header_text);
	    	copyCountTestView = (TextView) layout.findViewById(R.id.record_details_simple_copy_count);
	    	showMore = (LinearLayout) layout.findViewById(R.id.record_details_show_more);
	    	titleTextView = (TextView) layout.findViewById(R.id.record_details_simple_title);
			authorTextView = (TextView) layout.findViewById(R.id.record_details_simple_author);
			publisherTextView = (TextView) layout.findViewById(R.id.record_details_simple_publisher);
		
			seriesTextView = (TextView) layout.findViewById(R.id.record_details_simple_series);
			subjectTextView = (TextView) layout.findViewById(R.id.record_details_simple_subject);
			synopsisTextView = (TextView) layout.findViewById(R.id.record_details_simple_synopsis);
			isbnTextView = (TextView) layout.findViewById(R.id.record_details_simple_isbn);
			
			record_header.setText("Record " + position + "of " + total  );
			
			titleTextView.setText(record.title);
			authorTextView.setText(record.author);
			publisherTextView.setText(record.pubdate + " " + record.publisher);
			
			seriesTextView.setText(record.series);
			subjectTextView.setText(record.subject);
			synopsisTextView.setText(record.synopsis);
			
			isbnTextView.setText(record.isbn);
			
			int current_org = 0;
			if(search != null)
			 current_org = search.selectedOrganization.id;
			
			System.out.println("Size " + record.copyCountListInfo.size());
			for(int i=0;i<record.copyCountListInfo.size();i++){
				//TODO
				System.out.println(current_org + " " + record.copyCountListInfo.get(i).org_id + " " + record.copyCountListInfo.get(i).count);
				if(record.copyCountListInfo.get(i).org_id == current_org){
					int total = record.copyCountListInfo.get(i).count;
					int available = record.copyCountListInfo.get(i).available;
					copyCountTestView.setText(available + " / " + total);
					break;
				}
			}

			final LayoutInflater inf = inflater;
			final LinearLayout lay = layout;
			
			//add more details
			showMore.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					
				
					
					System.out.println("Show more tabed");
					// insert into main view
					LinearLayout insertPoint = (LinearLayout) lay.findViewById(R.id.record_details_copy_information);
					addCopyInfo(list_size, record.copyCountListInfo.size(), inf, insertPoint);
					
				}
			});

			if(list_size > record.copyInformationList.size())
				list_size = record.copyInformationList.size();

			// insert into main view
			LinearLayout insertPoint = (LinearLayout) layout.findViewById(R.id.record_details_copy_information);
			addCopyInfo(0, list_size, inflater, insertPoint);
			

	        return layout;
	    }

	    @Override
	    public void onSaveInstanceState(Bundle outState) {
	        super.onSaveInstanceState(outState);
	    }
	    
	    
	    public void addCopyInfo(int start, int stop, LayoutInflater inflater, LinearLayout insertPoint){
	    	
	    	for(int i=start;i<stop;i++){
				
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
					TextView statusName = new TextView(getActivity());
					statusName.setText(ent.getKey() + " : " + ent.getValue());
					
					copy_statuses.addView(statusName, new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
					
				}
			
			}
	    	
	    }
}
