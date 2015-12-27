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

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.accountAccess.holds.PlaceHold;
import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.globals.Log;
import org.evergreen_ils.globals.Utils;
import org.evergreen_ils.net.GatewayJsonObjectRequest;
import org.evergreen_ils.net.VolleyWrangler;
import org.evergreen_ils.searchCatalog.*;

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
import org.opensrf.util.GatewayResponse;

public class BasicDetailsFragment extends Fragment {

    private final static String TAG = BasicDetailsFragment.class.getSimpleName();

    private RecordInfo record;
    private Integer orgId;
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

    private TextView copyCountTextView;

    private Button placeHoldButton;

    private GlobalConfigs globalConfigs;

    /*
    private Button addToBookbagButton;
    private ProgressDialog progressDialog;
    private Integer bookbag_selected;
    private Dialog dialog;
    private ArrayList<BookBag> bookBags;
    private int list_size = 3;
    */

    private NetworkImageView recordImage;

    public static BasicDetailsFragment newInstance(RecordInfo record,
            Integer position, Integer total, Integer orgID) {
        BasicDetailsFragment fragment = new BasicDetailsFragment();
        fragment.orgId = orgID;
        fragment.record = record;
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
            orgId = savedInstanceState.getInt("orgId");
            this.position = savedInstanceState.getInt("position");
            this.total = savedInstanceState.getInt("total");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        globalConfigs = GlobalConfigs.getInstance(getActivity());

        LinearLayout layout = (LinearLayout) inflater.inflate(
                R.layout.record_details_basic_fragment, null);

        record_header = (TextView) layout.findViewById(R.id.record_header_text);
        copyCountTextView = (TextView) layout.findViewById(R.id.record_details_simple_copy_count);
//        showMore = (LinearLayout) layout.findViewById(R.id.record_details_show_more);
        titleTextView = (TextView) layout.findViewById(R.id.record_details_simple_title);
        formatTextView = (TextView) layout.findViewById(R.id.record_details_format);
        authorTextView = (TextView) layout.findViewById(R.id.record_details_simple_author);
        publisherTextView = (TextView) layout.findViewById(R.id.record_details_simple_publisher);

        seriesTextView = (TextView) layout.findViewById(R.id.record_details_simple_series);
        subjectTextView = (TextView) layout.findViewById(R.id.record_details_simple_subject);
        synopsisTextView = (TextView) layout.findViewById(R.id.record_details_simple_synopsis);
        isbnTextView = (TextView) layout.findViewById(R.id.record_details_simple_isbn);

        recordImage = (NetworkImageView) layout.findViewById(R.id.record_details_simple_image);

        placeHoldButton = (Button) layout.findViewById(R.id.simple_place_hold_button);
//        addToBookbagButton = (Button) layout.findViewById(R.id.simple_add_to_bookbag_button);
        placeHoldButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity().getApplicationContext(), PlaceHold.class);
                intent.putExtra("recordInfo", record);
                startActivity(intent);
            }
        });

        // Start async image load
        final String imageHref = GlobalConfigs.getUrl("/opac/extras/ac/jacket/medium/r/" + record.doc_id);
        ImageLoader imageLoader = VolleyWrangler.getInstance(getActivity()).getImageLoader();
        recordImage.setImageUrl(imageHref, imageLoader);

        initBookbagStuff();

        record_header.setText(String.format(getString(R.string.record_of), position, total));

        titleTextView.setText(record.title);
        formatTextView.setText(SearchFormat.getItemLabelFromSearchFormat(record.search_format));
        authorTextView.setText(record.author);
        publisherTextView.setText(record.pubdate + " " + record.publisher);
        seriesTextView.setText(record.series);
        subjectTextView.setText(record.subject);
        synopsisTextView.setText(record.synopsis);
        isbnTextView.setText(record.isbn);

        updateCopyCount();
        initCopyCount();

        ((Button)layout.findViewById(R.id.show_copy_information_button)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity().getApplicationContext(), CopyInformationActivity.class);
                intent.putExtra("recordInfo", record);
                intent.putExtra("orgId", orgId);
                startActivity(intent);
            }
        });

        return layout;
    }

    private void initCopyCount() {
        Log.d(TAG, "kcx.initCopyCount, id="+record.doc_id+" info="+record.copyCountListInfo);
        if (record.copyCountListInfo == null) {
            final long start_ms = System.currentTimeMillis();
            RequestQueue q = VolleyWrangler.getInstance(getActivity()).getRequestQueue();
            String url = GlobalConfigs.getUrl(Utils.buildGatewayUrl(
                    SearchCatalog.SERVICE, SearchCatalog.METHOD_GET_COPY_COUNT,
                    new Object[]{orgId, record.doc_id}));
            Log.d(TAG, "kcx.updateCopyCount, id="+record.doc_id+" url="+url);
            GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                    url,
                    new Response.Listener<GatewayResponse>() {
                        @Override
                        public void onResponse(GatewayResponse response) {
                            long duration_ms = System.currentTimeMillis() - start_ms;
                            Log.d(TAG, "kcx.volley fetch "+record.doc_id+" took " + duration_ms + "ms");
                            SearchCatalog.setCopyCountListInfo(record, response);
                            updateCopyCount();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "kcx.initCopyCount caught", error);
                            SearchCatalog.setCopyCountListInfo(record, null);
                        }
                    });
            q.add(r);
        }
    }

    private void updateCopyCount() {
        int total = 0;
        int available = 0;
        Log.d(TAG, "kcx.updateCopyCount, info="+record.copyCountListInfo);
        if (record.copyCountListInfo == null) {
            copyCountTextView.setText("");
        } else {
            for (int i = 0; i < record.copyCountListInfo.size(); i++) {
//            Log.d(TAG, "xxx orgId=" + orgId
//                    + " rec.org_id=" + record.copyCountListInfo.get(i).org_id
//                    + " rec.count=" + record.copyCountListInfo.get(i).count);
                if (record.copyCountListInfo.get(i).org_id.equals(orgId)) {
                    total = record.copyCountListInfo.get(i).count;
                    available = record.copyCountListInfo.get(i).available;
                    break;
                }
            }
            String totalCopies = getResources().getQuantityString(R.plurals.number_of_copies, total, total);
            copyCountTextView.setText(String.format(getString(R.string.n_of_m_available),
                    available, totalCopies, globalConfigs.getOrganizationName(orgId)));
        }
    }

    private void initBookbagStuff() {
        /*
        AccountAccess ac = AccountAccess.getInstance();
        bookBags = ac.getBookbags();
        String array_spinner[] = new String[bookBags.size()];

        for (int i = 0; i < array_spinner.length; i++)
            array_spinner[i] = bookBags.get(i).name;

        dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.bookbag_spinner);
        dialog.setTitle("Choose bookbag");
        Spinner s = (Spinner) dialog.findViewById(R.id.bookbag_spinner);
        Button add = (Button) dialog.findViewById(R.id.add_to_bookbag_button);
        ArrayAdapter adapter = new ArrayAdapter(getActivity()
                .getApplicationContext(), android.R.layout.simple_spinner_item,
                array_spinner);
        s.setAdapter(adapter);

        add.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Thread addtoBookbag = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        AccountAccess ac = AccountAccess.getInstance();
                        try {
                            ac.addRecordToBookBag(record.doc_id,
                                    ac.getBookbags().get(bookbag_selected).id);
                        } catch (SessionNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                dialog.dismiss();
                            }
                        });

                    }
                });
                progressDialog = ProgressDialog.show(getActivity(),
                        getResources().getText(R.string.dialog_please_wait),
                        "Adding to bookbag");
                addtoBookbag.start();

            }
        });
        s.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                bookbag_selected = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }

        });

        addToBookbagButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (bookBags.size() > 0)
                            dialog.show();
                        else
                            Toast.makeText(getActivity(), "No bookbags",
                                    Toast.LENGTH_SHORT).show();
                    }

                });
            }
        });
        */
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("recordInfo", record);
        outState.putInt("orgId", this.orgId);
        outState.putInt("position", this.position);
        outState.putInt("total", this.total);
        super.onSaveInstanceState(outState);
    }
}
