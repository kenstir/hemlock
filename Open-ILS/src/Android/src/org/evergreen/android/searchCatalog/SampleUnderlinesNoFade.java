package org.evergreen.android.searchCatalog;

import java.util.List;

import org.evergreen.android.R;
import org.evergreen.android.utils.ui.BaseSampleActivity;
import org.evergreen.android.utils.ui.BasicDetailsFragment;
import org.evergreen.android.utils.ui.TestFragmentAdapter;
import org.evergreen.android.utils.ui.UnderlinePageIndicator;
import org.evergreen.android.views.AccountScreenDashboard;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class SampleUnderlinesNoFade extends BaseSampleActivity {

	private List<RecordInfo> records;
	
	private Button myAccountButton;
	
	private ImageButton homeButton;
	
	private TextView headerTitle;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_underlines);
   
        records = (List<RecordInfo>)getIntent().getSerializableExtra("recordList");
        
        if(records.get(records.size()-1).dummy == true)
        	records.remove(records.size()-1);
        
        
        //header portion actions
        homeButton = (ImageButton) findViewById(R.id.library_logo);
        myAccountButton = (Button) findViewById(R.id.my_account_button);
        headerTitle = (TextView) findViewById(R.id.header_title);
        headerTitle.setText(R.string.search_details_title);
        
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
        
        int record_position = getIntent().getIntExtra("recordPosition", 0);
        mAdapter = new SearchFragmentAdapter(getSupportFragmentManager());

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);


        mPager.setCurrentItem(record_position);
        
        UnderlinePageIndicator indicator = (UnderlinePageIndicator)findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
        indicator.setFades(false);
        mIndicator = indicator;
        
    }
    
    class SearchFragmentAdapter extends TestFragmentAdapter {
        public SearchFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
        		// position +1 for 1 - size values
        		return BasicDetailsFragment.newInstance(records.get(position),position+1,records.size());
        }

        @Override
        public int getCount() {
            return records.size();
        	//return TabsView.CONTENT.length;
        }

    }
}