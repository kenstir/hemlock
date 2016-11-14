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
package org.evergreen_ils.utils.ui;

import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.bookbags.BookBag;
import org.evergreen_ils.accountAccess.bookbags.BookBagUtils;
import org.evergreen_ils.accountAccess.holds.PlaceHoldActivity;
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.net.VolleyWrangler;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.evergreen_ils.searchCatalog.CopyInformationActivity;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.searchCatalog.RecordLoader;
import org.evergreen_ils.searchCatalog.SearchFormat;

import java.util.ArrayList;

public class BasicDetailsFragment extends Fragment {

    private final static String TAG = BasicDetailsFragment.class.getSimpleName();

    private Activity activity;
    private RecordInfo record;
    private Integer orgID;
    private Integer position;
    private Integer total;

    private TextView record_header;
    private TextView titleTextView;
    private TextView formatTextView;
    private TextView authorTextView;
    private TextView publisherTextView;
    private TextView seriesTextView;
    private TextView subjectTextView;
    private TextView synopsisTextView;
    private TextView isbnTextView;
    private TextView descriptionTextView;
    private Button placeHoldButton;
    private Button showCopiesButton;
    private Button onlineAccessButton;

    private EvergreenServer eg;

    private Button addToBookbagButton;
    private ArrayList<BookBag> bookBags;

    private NetworkImageView recordImage;

    public static BasicDetailsFragment newInstance(RecordInfo record, Integer position, Integer total, Integer orgID) {
        BasicDetailsFragment fragment = new BasicDetailsFragment();
        fragment.record = record;
        fragment.orgID = orgID;
        fragment.position = position;
        fragment.total = total;
        return fragment;
    }

    public BasicDetailsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            record = (RecordInfo) savedInstanceState.getSerializable("recordInfo");
            orgID = savedInstanceState.getInt("orgID");
            position = savedInstanceState.getInt("position");
            total = savedInstanceState.getInt("total");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("recordInfo", record);
        outState.putInt("orgID", orgID);
        outState.putInt("position", position);
        outState.putInt("total", total);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        activity = getActivity();
        eg = EvergreenServer.getInstance();

        LinearLayout layout = (LinearLayout) inflater.inflate(
                R.layout.record_details_basic_fragment, null);

        record_header = (TextView) layout.findViewById(R.id.record_header_text);
        titleTextView = (TextView) layout.findViewById(R.id.record_details_simple_title);
        formatTextView = (TextView) layout.findViewById(R.id.record_details_format);
        authorTextView = (TextView) layout.findViewById(R.id.record_details_simple_author);
        publisherTextView = (TextView) layout.findViewById(R.id.record_details_simple_publisher);
        seriesTextView = (TextView) layout.findViewById(R.id.record_details_simple_series);
        subjectTextView = (TextView) layout.findViewById(R.id.record_details_simple_subject);
        synopsisTextView = (TextView) layout.findViewById(R.id.record_details_simple_synopsis);
        isbnTextView = (TextView) layout.findViewById(R.id.record_details_simple_isbn);
        recordImage = (NetworkImageView) layout.findViewById(R.id.record_details_simple_image);
        descriptionTextView = (TextView) layout.findViewById(R.id.record_details_brief_description);
        placeHoldButton = (Button) layout.findViewById(R.id.simple_place_hold_button);
        showCopiesButton = (Button) layout.findViewById(R.id.show_copy_information_button);
        onlineAccessButton = (Button) layout.findViewById(R.id.record_details_online_button);
        addToBookbagButton = (Button) layout.findViewById(R.id.add_to_bookbag_button);

        record_header.setText(String.format(getString(R.string.record_of), position+1, total));
        descriptionTextView.setText("");

        initButtons();

        // Start async image load
        final String imageHref = EvergreenServer.getInstance().getUrl("/opac/extras/ac/jacket/medium/r/" + record.doc_id);
        ImageLoader imageLoader = VolleyWrangler.getInstance(getActivity()).getImageLoader();
        recordImage.setImageUrl(imageHref, imageLoader);

        // Start async record load
        fetchRecordInfo(record);
        initBookbagStuff();

        return layout;
    }

    private void initButtons() {
        updateButtonViews();

        placeHoldButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity().getApplicationContext(), PlaceHoldActivity.class);
                intent.putExtra("recordInfo", record);
                startActivity(intent);
            }
        });
        showCopiesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity().getApplicationContext(), CopyInformationActivity.class);
                intent.putExtra("recordInfo", record);
                intent.putExtra("orgID", orgID);
                startActivity(intent);
            }
        });
        onlineAccessButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                launchOnlineAccess();
            }
        });
        addToBookbagButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                BookBagUtils.showAddToListDialog(activity, bookBags, record);
            }
        });
    }

    private void launchOnlineAccess() {
        if (TextUtils.isEmpty(record.online_loc))
            return;
        Uri uri = Uri.parse(record.online_loc);
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(i);
    }

    // This is a record of a few approaches I tried to get Baker-Taylor e-books from CW/MARS links; nothing worked.
    // Only approach left might be an interstitial activity that does this:
    // 1. Copies the title to the clipboard
    // 2. Launches the axis360 app using getLaunchIntentForPackage("com.bt.mdd")
//    private void launchOnlineAccess() {
//        if (TextUtils.isEmpty(record.online_loc))
//            return;
//        if (record.online_loc.contains("axis360.baker-taylor.com")) {
//            /* try 1: crashes axis360 */
//            Intent i = new Intent(Intent.ACTION_SEARCH);
//            i.putExtra(SearchManager.QUERY, record.title);
//            /* try 2: landing page seems abandoned */
//            Uri uri = Uri.parse("blioapp://blioapp/Title?itemid=0014619811");
//            Intent i = new Intent(Intent.ACTION_VIEW, uri);
//            /* try 3: lands on some mit license page */
//            Uri orig = Uri.parse(record.online_loc);
//            Uri uri = Uri.parse("com.bt.mdd://" + orig.getHost() + orig.getPath() + "?" + orig.getQuery());
//            Intent i = new Intent(Intent.ACTION_VIEW, uri);
//            /* try 4: library-specific url works no better than cwmars-generic url; that is, lousy */
//            Uri orig = Uri.parse(record.online_loc);
//            Uri uri = Uri.parse("http://" + "marl.axis360.baker-taylor.com" + orig.getPath() + "?" + orig.getQuery());
//            Intent i = new Intent(Intent.ACTION_VIEW, uri);
//            startActivity(i);
//            /* try 5: this starts the app directly, at its home screen.  Works but you lose the book. */
//            Intent i = getActivity().getPackageManager().getLaunchIntentForPackage("com.bt.mdd");
//            try {
//                startActivity(i);
//            } catch (Exception e) {
//                Log.d(TAG, "caught", e);
//            }
//                } else {
//                    Uri uri = Uri.parse(record.online_loc);
//                    Intent i = new Intent(Intent.ACTION_VIEW, uri);
//                }
//            }
//        });
//    }

    private void updateButtonViews() {
        boolean is_online_resource = record.isOnlineResource();
        onlineAccessButton.setVisibility(is_online_resource ? View.VISIBLE : View.GONE);
        placeHoldButton.setVisibility(is_online_resource ? View.GONE : View.VISIBLE);
        showCopiesButton.setVisibility(is_online_resource ? View.GONE : View.VISIBLE);
    }

    private void updateSearchFormatView() {
        if (!isAdded()) return; // discard late results
        formatTextView.setText(SearchFormat.getItemLabelFromSearchFormat(record.search_format));
        updateButtonViews();
    }

    private void updateBasicMetadataViews() {
        if (!isAdded()) return; // discard late results
        titleTextView.setText(record.title);
        authorTextView.setText(record.author);
        publisherTextView.setText(record.getPublishingInfo());
        seriesTextView.setText(record.series);
        subjectTextView.setText(record.subject);
        synopsisTextView.setText(record.synopsis);
        isbnTextView.setText(record.isbn);

        if (record.isOnlineResource()) {
            Uri uri = Uri.parse(record.online_loc);
            descriptionTextView.setText(uri.getHost());
        }

        updateButtonViews();
    }

    private void fetchRecordInfo(final RecordInfo record) {
        RecordLoader.fetch(record, getActivity(),
                new RecordLoader.ResponseListener() {
                    @Override
                    public void onMetadataLoaded() {
                        updateBasicMetadataViews();
                        fetchCopyCountInfo(record);
                    }
                    @Override
                    public void onSearchFormatLoaded() {
                        updateSearchFormatView();
                        fetchCopyCountInfo(record);
                    }
                });
    }

    private void fetchCopyCountInfo(RecordInfo record) {
        // Check for copy counts only after we know it is not an e-book.
        // The problem is we do not really know yet whether it is an e-book
        // until both online_loc and search_format are loaded.
        // See RecordInfo.isOnlineResource().
        Log.d(TAG, "fetchCopyCountInfo id=" + record.doc_id
              + " basic_metadata_loaded=" + record.basic_metadata_loaded
              + " search_format_loaded=" + record.search_format_loaded
              + " isOnlineResource=" + record.isOnlineResource());
        if (record.basic_metadata_loaded
            && record.search_format_loaded
            && !record.isOnlineResource())
        {
            RecordLoader.fetchCopyCount(record, orgID, getActivity(), new RecordLoader.Listener() {
                @Override
                public void onDataAvailable() {
                    updateCopyCountView();
                }
            });
        }
    }

    private void updateCopyCountView() {
        if (!isAdded()) return; // discard late results
        if (record.copySummaryList == null) {
            Log.d(TAG, "updateCopyCountView " + record.doc_id + " list=null");
        } else {
            Log.d(TAG, "updateCopyCountView " + record.doc_id + " list=" + record.copySummaryList.size() + " items");
        }
        int total = 0;
        int available = 0;
        if (record.copySummaryList == null) {
            descriptionTextView.setText("");
        } else {
            for (int i = 0; i < record.copySummaryList.size(); i++) {
                if (record.copySummaryList.get(i).org_id.equals(orgID)) {
                    total = record.copySummaryList.get(i).count;
                    available = record.copySummaryList.get(i).available;
                    break;
                }
            }
            String totalCopies = getResources().getQuantityString(R.plurals.number_of_copies, total, total);
            descriptionTextView.setText(String.format(getString(R.string.n_of_m_available),
                    available, totalCopies, eg.getOrganizationName(orgID)));
        }
    }

    private void initBookbagStuff() {
        AccountAccess ac = AccountAccess.getInstance();
        bookBags = ac.getBookbags();
    }
}
