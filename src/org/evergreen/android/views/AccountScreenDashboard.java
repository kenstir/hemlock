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
package org.evergreen.android.views;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.bookbags.BookbagsListView;
import org.evergreen.android.accountAccess.checkout.ItemsCheckOutListView;
import org.evergreen.android.accountAccess.fines.FinesActivity;
import org.evergreen.android.accountAccess.holds.HoldsListView;
import org.evergreen.android.searchCatalog.SearchCatalogListView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class AccountScreenDashboard extends Activity {

    /**
     * onCreate - called when the activity is first created.
     * 
     * Called when the activity is first created. This is where you should do
     * all of your normal static set up: create views, bind data to lists, etc.
     * This method also provides you with a Bundle containing the activity's
     * previously frozen state, if there was one.
     * 
     * Always followed by onStart().
     * 
     */

    private Button homeButton = null;

    private Button myAccountButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashbord_account);
        myAccountButton = (Button) findViewById(R.id.my_account_button);

        myAccountButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        homeButton = (Button) findViewById(R.id.library_logo);

        homeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getApplicationContext(),
                        SearchCatalogListView.class);
                startActivity(intent);
            }
        });
    }

    /**
     * onDestroy The final call you receive before your activity is destroyed.
     * This can happen either because the activity is finishing (someone called
     * finish() on it, or because the system is temporarily destroying this
     * instance of the activity to save space. You can distinguish between these
     * two scenarios with the isFinishing() method.
     * 
     */

    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * onPause Called when the system is about to start resuming a previous
     * activity. This is typically used to commit unsaved changes to persistent
     * data, stop animations and other things that may be consuming CPU, etc.
     * Implementations of this method must be very quick because the next
     * activity will not be resumed until this method returns. Followed by
     * either onResume() if the activity returns back to the front, or onStop()
     * if it becomes invisible to the user.
     * 
     */

    protected void onPause() {
        super.onPause();
    }

    /**
     * onRestart Called after your activity has been stopped, prior to it being
     * started again. Always followed by onStart().
     * 
     */

    protected void onRestart() {
        super.onRestart();
    }

    /**
     * onResume Called when the activity will start interacting with the user.
     * At this point your activity is at the top of the activity stack, with
     * user input going to it. Always followed by onPause().
     * 
     */

    protected void onResume() {
        super.onResume();
    }

    /**
     * onStart Called when the activity is becoming visible to the user.
     * Followed by onResume() if the activity comes to the foreground, or
     * onStop() if it becomes hidden.
     * 
     */

    protected void onStart() {
        super.onStart();
    }

    /**
     * onStop Called when the activity is no longer visible to the user because
     * another activity has been resumed and is covering this one. This may
     * happen either because a new activity is being started, an existing one is
     * being brought in front of this one, or this one is being destroyed.
     * 
     * Followed by either onRestart() if this activity is coming back to
     * interact with the user, or onDestroy() if this activity is going away.
     */

    protected void onStop() {
        super.onStop();
    }

    public void onClickFeature(View v) {
        int id = v.getId();
        switch (id) {

        case R.id.account_btn_check_out:
            startActivity(new Intent(getApplicationContext(),
                    ItemsCheckOutListView.class));
            break;
        case R.id.account_btn_holds:
            startActivity(new Intent(getApplicationContext(),
                    HoldsListView.class));
            break;
        case R.id.account_btn_fines:
            startActivity(new Intent(getApplicationContext(),
                    FinesActivity.class));
            break;
        case R.id.account_btn_book_bags:
            startActivity(new Intent(getApplicationContext(),
                    BookbagsListView.class));
            break;
        default:
            break;

        }

    }

}
