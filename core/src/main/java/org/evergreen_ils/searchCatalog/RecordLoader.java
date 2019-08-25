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
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.android.App;
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
        // TODO: MARCXML
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

    public static void fetchMARCXML(final RecordInfo record, Context context, final ResponseListener responseListener) {
        Log.d(TAG, "fetchMARCXML id="+record.doc_id);
        if (record.marcxml_loaded) {
            responseListener.onMetadataLoaded();
        } else {
            final long start_ms = System.currentTimeMillis();
            String url = EvergreenServer.getInstance().getUrl(Utils.buildGatewayUrl(
                    Api.PCRUD_SERVICE, Api.RETRIEVE_BRE,
                    new Object[]{Api.ANONYMOUS, record.doc_id}));
            Log.d(TAG, "fetch.marcxml "+url);
            GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                    url,
                    Request.Priority.HIGH,
                    new Response.Listener<GatewayResponse>() {
                        @Override
                        public void onResponse(GatewayResponse response) {
                            record.updateFromBREResponse(response);
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
            final long start_ms = System.currentTimeMillis();
            String url = EvergreenServer.getInstance().getUrl(Utils.buildGatewayUrl(
                    Api.PCRUD_SERVICE, Api.RETRIEVE_MRA,
                    new Object[]{Api.ANONYMOUS, record.doc_id}));
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

    public static void fetchCopySummary(final RecordInfo record, final int orgID, Context context, final Listener listener) {
        Log.d(TAG, "fetchCopySummary id="+record.doc_id+" loaded="+record.copy_summary_loaded);
        if (record.copy_summary_loaded) {
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
                            RecordInfo.setCopySummary(record, response);
                            listener.onDataAvailable();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "caught", error);
                            RecordInfo.setCopySummary(record, null);
                        }
                    });
            VolleyWrangler.getInstance(context).addToRequestQueue(r);
        }
    }

    public static String getCopySummary(final RecordInfo record, final int orgID, Context context) {
        String copySummaryText;
        int total = 0;
        int available = 0;
        if (record.copySummaryList == null) {
            copySummaryText = "";
        } else {
            for (int i = 0; i < record.copySummaryList.size(); i++) {
                if (record.copySummaryList.get(i).org_id.equals(orgID)) {
                    total = record.copySummaryList.get(i).count;
                    available = record.copySummaryList.get(i).available;
                    break;
                }
            }
            String totalCopies = context.getResources().getQuantityString(R.plurals.number_of_copies, total, total);
            copySummaryText = String.format(context.getString(R.string.n_of_m_available),
                    available, totalCopies, EvergreenServer.getInstance().getOrganizationName(orgID));
        }
        return copySummaryText;
    }
}
