/*
 * Copyright (C) 2015 Kenneth H. Cox
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.evergreen_ils.searchCatalog;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import org.evergreen_ils.Api;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.system.Log;
import org.evergreen_ils.system.Utils;
import org.evergreen_ils.net.GatewayJsonObjectRequest;
import org.evergreen_ils.net.VolleyWrangler;
import org.opensrf.util.GatewayResponse;

/** Async interface for loading RecordInfo metadata
 *
 * Created by kenstir on 12/27/2015.
 */
public class RecordLoader {
    private static final String TAG = RecordLoader.class.getSimpleName();

    public interface ResponseListener {
        public void onMetadataLoaded();
        public void onSearchFormatLoaded();
    }
    public interface Listener {
        public void onDataAvailable();
    }

    public static void fetch(final RecordInfo record, final Context context, final ResponseListener responseListener) {
        fetchBasicMetadata(record, context, responseListener);
        fetchSearchFormat(record, context, responseListener);
    }

    public static void fetchBasicMetadata(final RecordInfo record, Context context, final ResponseListener responseListener) {
        Log.d(TAG, "fetchBasicMetadata id="+record.doc_id+ " title="+record.title);
        if (record.basic_metadata_loaded) {
            responseListener.onMetadataLoaded();
        } else {
            final long start_ms = System.currentTimeMillis();
            String url = EvergreenServer.getInstance().getUrl(Utils.buildGatewayUrl(
                    Api.SEARCH, Api.MODS_SLIM_RETRIEVE,
                    new Object[]{record.doc_id}));
            Log.d(TAG, "fetch.basic "+url);
            GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                    url,
                    Request.Priority.HIGH,
                    new Response.Listener<GatewayResponse>() {
                        @Override
                        public void onResponse(GatewayResponse response) {
                            RecordInfo.updateFromMODSResponse(record, response.payload);
                            Log.logElapsedTime(TAG, start_ms, "fetch.basic");
                            responseListener.onMetadataLoaded();
                        }
                    },
                    VolleyWrangler.logErrorListener(TAG));
            VolleyWrangler.getInstance(context).addToRequestQueue(r);
        }
    }

    public static void fetchSearchFormat(final RecordInfo record, Context context, final ResponseListener responseListener) {
        Log.d(TAG, "fetchSearchFormat id="+record.doc_id+ " format="+record.search_format);
        if (record.search_format_loaded) {
            responseListener.onSearchFormatLoaded();
        } else {
            // todo newer EG supports using "ANONYMOUS" as the auth_token in PCRUD requests.
            // Older EG does not, and requires a valid auth_token.
            final long start_ms = System.currentTimeMillis();
            String url = EvergreenServer.getInstance().getUrl(Utils.buildGatewayUrl(
                    Api.PCRUD_SERVICE, Api.RETRIEVE_MRA,
                    new Object[]{AccountAccess.getInstance().getAuthToken(), record.doc_id}));
            Log.d(TAG, "fetch.searchFormat "+url);
            GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                    url,
                    Request.Priority.NORMAL,
                    new Response.Listener<GatewayResponse>() {
                        @Override
                        public void onResponse(GatewayResponse response) {
                            record.setSearchFormat(AccountAccess.getSearchFormatFromMRAResponse(response.payload));
                            Log.logElapsedTime(TAG, start_ms, "fetch.searchFormat");
                            responseListener.onSearchFormatLoaded();
                        }
                    },
                    VolleyWrangler.logErrorListener(TAG));
            VolleyWrangler.getInstance(context).addToRequestQueue(r);
        }
    }

    public static void fetchCopyCount(final RecordInfo record, final int orgID, Context context, final Listener listener) {
        Log.d(TAG, "fetchCopyCount id="+record.doc_id
                +" list=" + ((record.copySummaryList == null) ? "null" : "non-null"));
        if (record.copy_info_loaded) {
            listener.onDataAvailable();
        } else {
            String url = EvergreenServer.getInstance().getUrl(Utils.buildGatewayUrl(
                    Api.SEARCH, Api.COPY_COUNT,
                    new Object[]{orgID, record.doc_id}));
            GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                    url,
                    Request.Priority.LOW,
                    new Response.Listener<GatewayResponse>() {
                        @Override
                        public void onResponse(GatewayResponse response) {
                            RecordInfo.setCopyCountInfo(record, response);
                            listener.onDataAvailable();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "caught", error);
                            RecordInfo.setCopyCountInfo(record, null);
                        }
                    });
            VolleyWrangler.getInstance(context).addToRequestQueue(r);
        }
    }
}
