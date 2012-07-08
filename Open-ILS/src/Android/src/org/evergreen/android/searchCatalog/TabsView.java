package org.evergreen.android.searchCatalog;

import org.evergreen.android.R;
import org.evergreen.android.utils.ui.AdvancedDetailsFragment;
import org.evergreen.android.utils.ui.BaseSampleActivity;
import org.evergreen.android.utils.ui.BasicDetailsFragment;
import org.evergreen.android.utils.ui.TabPageIndicator;
import org.evergreen.android.utils.ui.TestFragment;
import org.evergreen.android.utils.ui.TestFragmentAdapter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;

public class TabsView extends BaseSampleActivity {
    private static final String[] CONTENT = new String[] { "Details", "Advanced"};

    
    private RecordInfo record;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_tabs);

        record = (RecordInfo) getIntent().getSerializableExtra("recordInfo");
		
        
        mAdapter = new SearchFragmentAdapter(getSupportFragmentManager());

        
        //mAdapter.getItem(0).
        
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        mIndicator = (TabPageIndicator)findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
    }

    class SearchFragmentAdapter extends TestFragmentAdapter {
        public SearchFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
        	
        	if(position == 1)
        		return AdvancedDetailsFragment.newInstance(record);
        	if(position == 0)
        		return BasicDetailsFragment.newInstance(record);
        	
            return TestFragment.newInstance(TabsView.CONTENT[position % TabsView.CONTENT.length]);
        }

        @Override
        public int getCount() {
            return 2;
        	//return TabsView.CONTENT.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TabsView.CONTENT[position % TabsView.CONTENT.length].toUpperCase();
        }
    }
}
