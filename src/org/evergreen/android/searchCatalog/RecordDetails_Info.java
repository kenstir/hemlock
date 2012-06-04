package org.evergreen.android.searchCatalog;

import java.security.PublicKey;

import org.evergreen.android.R;
import org.evergreen.android.R.id;
import org.evergreen.android.R.layout;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class RecordDetails_Info extends Activity{

	
	private TextView titleTextView;
	
	private TextView authorTextView;
	
	private TextView publisherTextView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.record_details_info);
		
		RecordInfo record = (RecordInfo)getIntent().getSerializableExtra("recordInfo");
		
		titleTextView = (TextView) findViewById(R.id.record_details_info_title);
		authorTextView = (TextView) findViewById(R.id.record_details_info_author);
		publisherTextView = (TextView) findViewById(R.id.record_details_info_publisher);
		
		
		titleTextView.setText(record.title);
		authorTextView.setText(record.author);
		publisherTextView.setText(record.publisher);
		
	}
}
