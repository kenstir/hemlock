package org.evergreen.android.searchCatalog;

import org.evergreen.android.R;
import org.evergreen.android.R.id;
import org.evergreen.android.R.layout;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class RecordDetails_Simple extends Activity {

	
	private TextView titleTextView;
	private TextView authorTextView;
	private TextView publisherTextView;
	private TextView seriesTextView;
	private TextView subjectTextView;
	private TextView synopsisTextView;
	private TextView isbnTextView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.record_details_simple);
		
		
		
		titleTextView = (TextView) findViewById(R.id.record_details_simple_title);
		authorTextView = (TextView) findViewById(R.id.record_details_simple_author);
		publisherTextView = (TextView) findViewById(R.id.record_details_simple_publisher);
		
		seriesTextView = (TextView) findViewById(R.id.record_details_simple_series);
		subjectTextView = (TextView) findViewById(R.id.record_details_simple_subject);
		synopsisTextView = (TextView) findViewById(R.id.record_details_simple_synopsis);
		
		isbnTextView = (TextView) findViewById(R.id.record_details_simple_isbn);
		
		RecordInfo record = (RecordInfo) getIntent().getSerializableExtra("recordInfo");
		
		titleTextView.setText(record.title);
		authorTextView.setText(record.author);
		publisherTextView.setText(record.pubdate + " " + record.publisher);
		
		seriesTextView.setText(record.series);
		subjectTextView.setText(record.subject);
		synopsisTextView.setText(record.synopsis);
		
		isbnTextView.setText(record.isbn);
		
	}
}
