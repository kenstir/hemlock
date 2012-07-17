package org.evergreen.android.utils.ui;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.evergreen.android.R;
import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.searchCatalog.CopyInformation;
import org.evergreen.android.searchCatalog.RecordInfo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
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
	
	
	
	    public static BasicDetailsFragment newInstance(RecordInfo record, Integer position, Integer total) {
	    	BasicDetailsFragment fragment = new BasicDetailsFragment(record,position,total);
	    	
	        return fragment;
	    }

	    public BasicDetailsFragment(RecordInfo record, Integer position, Integer total){
	    	
	    	this.record = record;
	    	this.position = position;
	    	this.total = total;
	    	
	    }

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);

	    }

	    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    
	    	
	    	GlobalConfigs gl = GlobalConfigs.getGlobalConfigs(getActivity());
	    	
	    	LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.record_details_basic_fragment, null);

	    	record_header = (TextView) layout.findViewById(R.id.record_header_text);
	    	
	    	titleTextView = (TextView) layout.findViewById(R.id.record_details_simple_title);
			authorTextView = (TextView) layout.findViewById(R.id.record_details_simple_author);
			publisherTextView = (TextView) layout.findViewById(R.id.record_details_simple_publisher);
		
			seriesTextView = (TextView) layout.findViewById(R.id.record_details_simple_series);
			subjectTextView = (TextView) layout.findViewById(R.id.record_details_simple_subject);
			synopsisTextView = (TextView) layout.findViewById(R.id.record_details_simple_synopsis);
			isbnTextView = (TextView) layout.findViewById(R.id.record_details_simple_isbn);
			
			record_header.setText("Record :" + position + " out of " + total  );
			
			titleTextView.setText(record.title);
			authorTextView.setText(record.author);
			publisherTextView.setText(record.pubdate + " " + record.publisher);
			
			seriesTextView.setText(record.series);
			subjectTextView.setText(record.subject);
			synopsisTextView.setText(record.synopsis);
			
			isbnTextView.setText(record.isbn);
			
			
			
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
				LinearLayout insertPoint = (LinearLayout) layout.findViewById(R.id.content_layout);
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
			
			
			
			
	        return layout;
	    }

	    @Override
	    public void onSaveInstanceState(Bundle outState) {
	        super.onSaveInstanceState(outState);
	    }
}
