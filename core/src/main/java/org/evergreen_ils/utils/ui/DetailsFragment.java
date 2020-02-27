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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.data.BookBag;
import org.evergreen_ils.accountAccess.bookbags.BookBagUtils;
import org.evergreen_ils.views.holds.PlaceHoldActivity;
import org.evergreen_ils.android.App;
import org.evergreen_ils.system.EgOrg;
import org.evergreen_ils.net.Gateway;
import org.evergreen_ils.net.VolleyWrangler;
import org.evergreen_ils.searchCatalog.CopyInformationActivity;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.searchCatalog.RecordLoader;
import org.evergreen_ils.android.Analytics;
import org.evergreen_ils.android.Log;
import org.evergreen_ils.data.Organization;
import org.evergreen_ils.utils.Link;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;

public class DetailsFragment extends Fragment {

    private final static String TAG = DetailsFragment.class.getSimpleName();

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
    View synopsisTableRow;
    View subjectTableRow;
    View seriesTableRow;
    View isbnTableRow;
    private Button placeHoldButton;
    private Button showCopiesButton;
    private Button onlineAccessButton;

    private Button addToBookbagButton;
    private ArrayList<BookBag> bookBags;

    private NetworkImageView recordImage;

    public static DetailsFragment newInstance(RecordInfo record, Integer position, Integer total, Integer orgID) {
        DetailsFragment fragment = new DetailsFragment();
        fragment.record = record;
        fragment.orgID = orgID;
        fragment.position = position;
        fragment.total = total;
        return fragment;
    }

    public DetailsFragment() {
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

        LinearLayout layout = (LinearLayout) inflater.inflate(
                R.layout.record_details_fragment, null);

        record_header = layout.findViewById(R.id.record_header_text);
        titleTextView = layout.findViewById(R.id.record_details_simple_title);
        formatTextView = layout.findViewById(R.id.record_details_format);
        authorTextView = layout.findViewById(R.id.record_details_simple_author);
        publisherTextView = layout.findViewById(R.id.record_details_simple_publisher);
        seriesTextView = layout.findViewById(R.id.record_details_series_text);
        subjectTextView = layout.findViewById(R.id.record_details_subject_text);
        synopsisTextView = layout.findViewById(R.id.record_details_synopsis_text);
        isbnTextView = layout.findViewById(R.id.record_details_isbn_text);
        recordImage = layout.findViewById(R.id.record_details_simple_image);
        descriptionTextView = layout.findViewById(R.id.record_details_brief_description);
        placeHoldButton = layout.findViewById(R.id.simple_place_hold_button);
        showCopiesButton = layout.findViewById(R.id.show_copy_information_button);
        onlineAccessButton = layout.findViewById(R.id.record_details_online_button);
        addToBookbagButton = layout.findViewById(R.id.add_to_bookbag_button);
        synopsisTableRow = layout.findViewById(R.id.record_details_synopsis_row);
        seriesTableRow = layout.findViewById(R.id.record_details_series_row);
        subjectTableRow = layout.findViewById(R.id.record_details_subject_row);
        isbnTableRow = layout.findViewById(R.id.record_details_isbn_row);

        record_header.setText(String.format(getString(R.string.record_of), position+1, total));
        descriptionTextView.setText("");

        initButtons();

        // Start async image load
        final String imageHref = Gateway.INSTANCE.getUrl("/opac/extras/ac/jacket/medium/r/" + record.doc_id);
        ImageLoader imageLoader = VolleyWrangler.getInstance(getActivity()).getImageLoader();
        recordImage.setImageUrl(imageHref, imageLoader);
        //recordImage.setDefaultImageResId(R.drawable.missing_art);//for screenshots

        // Start async record load
        fetchRecordInfo(record);
        initBookbagStuff();

        return layout;
    }

    private void initButtons() {
        // disable most buttons until the record is loaded
        placeHoldButton.setEnabled(false);
        showCopiesButton.setEnabled(false);
        onlineAccessButton.setEnabled(false);

        updateButtonViews();

        placeHoldButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Analytics.logEvent("Place Hold: Open", "via", "details_button");
                Intent intent = new Intent(getActivity().getApplicationContext(), PlaceHoldActivity.class);
                intent.putExtra("recordInfo", record);
                startActivity(intent);
            }
        });
        showCopiesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Analytics.logEvent("Copy Info: Open", "via", "details_button");
                Intent intent = new Intent(getActivity().getApplicationContext(), CopyInformationActivity.class);
                intent.putExtra("recordInfo", record);
                intent.putExtra("orgID", orgID);
                startActivity(intent);
            }
        });
        onlineAccessButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Analytics.logEvent("Online Access: Open", "via", "details_button");
                launchOnlineAccess();
            }
        });
        addToBookbagButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Analytics.logEvent("Lists: Add to List", "via", "details_button");
                BookBagUtils.showAddToListDialog(activity, bookBags, record);
            }
        });
    }

    private void launchURL(String url) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void launchOnlineAccess() {
        Organization org = EgOrg.findOrg(orgID);
        final List<Link> links = App.getBehavior().getOnlineLocations(record, org.shortname);
        if (links == null || links.size() == 0)
            return; // TODO: alert

        // if there's only one link, launch it without ceremony
        if (links.size() == 1 && !getResources().getBoolean(R.bool.ou_always_popup_online_links)) {
            launchURL(links.get(0).getHref());
            return;
        }

        // show an alert dialog to choose between links
        final String titles[] = new String[links.size()];
        for (int i = 0; i < titles.length; i++)
            titles[i] = links.get(i).getText();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.record_online_access);
        builder.setItems(titles, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                launchURL(links.get(which).getHref());
            }
        });
        builder.create().show();
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
        Boolean is_online_resource = App.getBehavior().isOnlineResource(record);
        Log.d(TAG, "yyy: updateButtonViews: title:"+record.title+" is_online_resource:"+is_online_resource);
        if (is_online_resource == null) return; // not ready yet

        placeHoldButton.setEnabled(true);
        showCopiesButton.setEnabled(true);
        onlineAccessButton.setEnabled(true);

        if (is_online_resource) {
            Organization org = EgOrg.findOrg(orgID);
            List<Link> links = App.getBehavior().getOnlineLocations(record, org.shortname);
            Log.d(TAG, "yyy: updateButtonViews: title:"+record.title+" links:"+links.size());
            if (links == null || links.isEmpty()) {
                onlineAccessButton.setEnabled(false);
            } else if (getResources().getBoolean(R.bool.ou_show_online_access_hostname)) {
                Uri uri = Uri.parse(links.get(0).getHref());
                descriptionTextView.setText(uri.getHost());
            }
        }

        onlineAccessButton.setVisibility(is_online_resource ? View.VISIBLE : View.GONE);
        placeHoldButton.setVisibility(is_online_resource ? View.GONE : View.VISIBLE);
        showCopiesButton.setVisibility(is_online_resource ? View.GONE : View.VISIBLE);
    }

    private void updateIconFormatView() {
        if (!isAdded()) return; // discard late results
        formatTextView.setText(RecordInfo.getIconFormatLabel(record));
        updateButtonViews();
    }

    private void updateBasicMetadataViews() {
        if (!isAdded()) return; // discard late results
        titleTextView.setText(record.title);
        authorTextView.setText(record.author);
        publisherTextView.setText(record.getPublishingInfo());
        synopsisTextView.setText(record.synopsis);
        seriesTextView.setText(record.series);
        seriesTableRow.setVisibility(TextUtils.isEmpty(record.series) ? View.GONE : View.VISIBLE);
        subjectTextView.setText(record.subject);
        subjectTableRow.setVisibility(TextUtils.isEmpty(record.subject) ? View.GONE : View.VISIBLE);
        isbnTextView.setText(record.isbn);
        isbnTableRow.setVisibility(TextUtils.isEmpty(record.isbn) ? View.GONE : View.VISIBLE);

        updateButtonViews();
    }

    private void fetchRecordInfo(final RecordInfo record) {
        RecordLoader.fetchDetailsMetadata(record, getActivity(),
                new RecordLoader.ResponseListener() {
                    @Override
                    public void onMetadataLoaded() {
                        Log.d(TAG, "yyyyy: onMetadataLoaded()");
                        updateBasicMetadataViews();
                        fetchCopyCountInfo(record);
                    }
                    @Override
                    public void onIconFormatLoaded() {
                        Log.d(TAG, "yyyyy: onIconFormatLoaded()");
                        updateIconFormatView();
                        fetchCopyCountInfo(record);
                    }
                });
    }

    private void fetchCopyCountInfo(RecordInfo record) {
        // Check for copy counts only after we know it is not an online_resource.
        Boolean is_online_resource = App.getBehavior().isOnlineResource(record);
        Log.d(TAG, "fetchCopyCountInfo id=" + record.doc_id
              + " is_online_resource=" + is_online_resource);
        if (is_online_resource == null) return; // not ready yet
        if (!is_online_resource
            && !record.copy_summary_loaded)
        {
            RecordLoader.fetchCopySummary(record, orgID, getContext(), new RecordLoader.Listener() {
                @Override
                public void onDataAvailable() {
                    updateCopyCountView();
                }
            });
        } else {
            Log.d(TAG, "not updating copy count view for some reason, STOPHERE");
        }
    }

    private void updateCopyCountView() {
        if (!isAdded()) return; // discard late results
        descriptionTextView.setText(RecordLoader.getCopySummary(record, orgID, getContext()));
    }

    private void initBookbagStuff() {
        AccountAccess ac = AccountAccess.getInstance();
        bookBags = ac.getBookbags();
    }
}
