package org.evergreen.android.searchCatalog;

import java.util.List;

import org.evergreen.android.R;
import org.evergreen.android.utils.ui.BaseSampleActivity;
import org.evergreen.android.utils.ui.BasicDetailsFragment;
import org.evergreen.android.utils.ui.TestFragmentAdapter;
import org.evergreen.android.utils.ui.UnderlinePageIndicator;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.widget.ListView;

public class SampleUnderlinesNoFade extends BaseSampleActivity {
    
	
	private RecordInfo record;
	
	private List<RecordInfo> records;
	
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_underlines);

        
        
        records = (List<RecordInfo>)getIntent().getSerializableExtra("recordList");
        
        record = (RecordInfo) getIntent().getSerializableExtra("recordInfo");
        
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