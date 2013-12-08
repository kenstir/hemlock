/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be usefull,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 * 
 */
package org.evergreen.android.utils.ui;

import java.util.Random;

import org.evergreen.android.R;

import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public abstract class BaseSampleActivity extends FragmentActivity {
    private static final Random RANDOM = new Random();

    public TestFragmentAdapter mAdapter;
    public ViewPager mPager;
    public PageIndicator mIndicator;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.random:
            final int page = RANDOM.nextInt(mAdapter.getCount());
            Toast.makeText(this, "Changing to page " + page, Toast.LENGTH_SHORT);
            mPager.setCurrentItem(page);
            return true;

        case R.id.add_page:
            if (mAdapter.getCount() < 10) {
                mIndicator.notifyDataSetChanged();
            }
            return true;

        case R.id.remove_page:
            if (mAdapter.getCount() > 1) {
                mIndicator.notifyDataSetChanged();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
