package org.evergreen.android.utils.ui;

import org.evergreen.android.R;
import org.evergreen.android.searchCatalog.RecordInfo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

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
			
			
	        return layout;
	    }

	    @Override
	    public void onSaveInstanceState(Bundle outState) {
	        super.onSaveInstanceState(outState);
	    }
}
