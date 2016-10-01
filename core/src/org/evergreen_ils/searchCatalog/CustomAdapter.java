/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.evergreen_ils.searchCatalog;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.widget.AdapterView;
import android.widget.TextView;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import org.evergreen_ils.net.VolleyWrangler;
import org.evergreen_ils.R;
import org.evergreen_ils.globals.Log;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provide views to RecyclerView with data from records.
 */
public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
    private static final String TAG = CustomAdapter.class.getSimpleName();
    private static final int DETAILS = 0;
    private static final int PLACE_HOLD = 1;
    private static final int BOOK_BAG = 2;
    private static final String CATALOG_URL = "http://bark.cwmars.org";

    private List<RecordInfo> records;

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final NetworkImageView imageView;
        private final TextView titleText;
        private final TextView authorText;
        private final TextView searchFormatText;
        private final TextView publisherText;

        public ViewHolder(View v) {
            super(v);
            // Define click listener for the ViewHolder's View.
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //int pos = getPosition();
                    int pos = ((RecyclerView.LayoutParams) v.getLayoutParams()).getViewLayoutPosition();
                    Log.d(TAG, "Click: pos="+pos);
                }
            });
            v.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    int pos = ((RecyclerView.LayoutParams) v.getLayoutParams()).getViewLayoutPosition();
                    Log.d(TAG, "Context: pos="+pos);
                    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                    menu.setHeaderTitle("Options");
                    menu.add(Menu.NONE, DETAILS, 0, "Show Details");
                    menu.add(Menu.NONE, PLACE_HOLD, 1, "Place Hold");
                    menu.add(Menu.NONE, BOOK_BAG, 2, "Add to List");
                }
            });
            imageView = (NetworkImageView) v.findViewById(R.id.search_record_img);
            titleText = (TextView) v.findViewById(R.id.search_record_title);
            authorText = (TextView) v.findViewById(R.id.search_record_author);
            searchFormatText = (TextView) v.findViewById(R.id.search_record_format);
            publisherText = (TextView) v.findViewById(R.id.search_record_publishing);
        }

        public void fetchBasicMetadata(final RecordInfo record, Context context) {
            // this is a stripped down version of RecordLoader.fetchBasicMetadata
            Log.d(TAG, record.doc_id + ": fetchBasicMetadata title=" + record.title);
            if (record.title != null)
                return;
            final long start_ms = System.currentTimeMillis();
            //String url = CATALOG_URL + "/osrf-gateway-v1?service=open-ils.pcrud&method=open-ils.pcrud.search.mraf.atomic&param=%22ANONYMOUS%22&param={%22id%22:["+record.doc_id+"]}";
            String url = CATALOG_URL + "/osrf-gateway-v1?service=open-ils.search&method=open-ils.search.biblio.record.mods_slim.retrieve&param=" + record.doc_id;
            Log.d(TAG, record.doc_id + ": fetchBasicMetadata url=" + url);
            StringRequest r = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "resp=" + response);
                    // {"payload":[{"__c":"mvr","__p":["THE UNOFFICIAL HARRY POTTER COOKBOOK","BUCHOLZ DINAH",1148741,null,null,null,null,"1148741",{},["text"],[],null,[],null,"",null,null,[]]}],"status":200}
                    Pattern p = Pattern.compile("__p\":\\[\"([^\"]*)");
                    Matcher m = p.matcher(response);
                    if (m.find()) {
                        String title = m.group(1);
                        Log.d(TAG, "title=" + title);
                        record.title = title;
                        titleText.setText(title);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    Log.d(TAG, "error=" + volleyError.getMessage());
                }
            });
            VolleyWrangler.getInstance(context).addToRequestQueue(r);
        }

        public void bindView(final RecordInfo record) {
            Log.d(TAG, record.doc_id + ": bindView");
            final Context context = imageView.getContext();
            final String url = CATALOG_URL + "/opac/extras/ac/jacket/small/r/" + record.doc_id;
            imageView.setImageUrl(url, VolleyWrangler.getInstance(context).getImageLoader());
            titleText.setText((record.title != null) ? record.title : context.getString(R.string.title_busy_ellipsis));
            RecordLoader.fetch(record, context, new RecordLoader.ResponseListener() {
                @Override
                public void onMetadataLoaded() {
                    titleText.setText(record.title);
                    authorText.setText(record.author);
                    publisherText.setText(record.getPublishingInfo());
                }

                @Override
                public void onSearchFormatLoaded() {
                    searchFormatText.setText(record.search_format);
                }
            });
        }
    }


    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet String[] containing the data to populate views to be used by RecyclerView.
     */
    public CustomAdapter(List<RecordInfo> dataSet) {
        records = dataSet;
    }


    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.search_result_item, viewGroup, false);

        return new ViewHolder(v);
    }



    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        Log.d(TAG, "Element " + position + " set.");

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        viewHolder.bindView(records.get(position));
    }


    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return records.size();
    }
}
