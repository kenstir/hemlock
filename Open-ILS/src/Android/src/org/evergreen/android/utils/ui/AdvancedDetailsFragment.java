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

public class AdvancedDetailsFragment extends Fragment{

	private RecordInfo record;
	
	private TextView seriesTextView;
	private TextView subjectTextView;
	private TextView synopsisTextView;
	private TextView isbnTextView;
	
	
	
	    public static AdvancedDetailsFragment newInstance(RecordInfo record) {
	    	AdvancedDetailsFragment fragment = new AdvancedDetailsFragment(record);


	        return fragment;
	    }

	    public AdvancedDetailsFragment(RecordInfo record){
	    	this.record = record;
	    }

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);

	    }

	    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    
	    	ScrollView layout = (ScrollView) inflater.inflate(R.layout.record_details_more_fragment, null);

			seriesTextView = (TextView) layout.findViewById(R.id.record_details_simple_series);
			subjectTextView = (TextView) layout.findViewById(R.id.record_details_simple_subject);
			synopsisTextView = (TextView) layout.findViewById(R.id.record_details_simple_synopsis);
			isbnTextView = (TextView) layout.findViewById(R.id.record_details_simple_isbn);
			
	    	
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
