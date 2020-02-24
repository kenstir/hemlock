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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.evergreen_ils.Api;
import org.evergreen_ils.R;
import org.evergreen_ils.data.EgOrg;
import org.evergreen_ils.net.Gateway;
import org.evergreen_ils.net.GatewayJsonObjectRequest;
import org.evergreen_ils.net.VolleyWrangler;
import org.evergreen_ils.android.Log;
import org.opensrf.util.GatewayResult;

/** Async interface for loading RecordInfo metadata
 *
 * Created by kenstir on 12/27/2015.
 */
public class RecordLoader {
    private static final String TAG = RecordLoader.class.getSimpleName();

    public interface ResponseListener {
        public void onMetadataLoaded();
        public void onIconFormatLoaded();
    }
    public interface Listener {
        public void onDataAvailable();
    }

    public static void fetchSummaryMetadata(final RecordInfo record, final Context context, final ResponseListener responseListener) {
        fetchRecordMODS(record, context, responseListener);
        fetchRecordAttributes(record, context, responseListener);
    }

    public static void fetchDetailsMetadata(final RecordInfo record, final Context context, final ResponseListener responseListener) {
        fetchRecordMODS(record, context, responseListener);
        fetchRecordAttributes(record, context, responseListener);
        if (context.getResources().getBoolean(R.bool.ou_need_marc_record)) {
            fetchMARCXML(record, context, responseListener);
        }
    }

    public static void fetchRecordMODS(final RecordInfo record, Context context, final ResponseListener responseListener) {
        Log.d(TAG, "fetchRecordMODS id="+record.doc_id+ " title="+record.title);
        if (record.basic_metadata_loaded) {
            responseListener.onMetadataLoaded();
        } else {
            final long start_ms = System.currentTimeMillis();
            String url = Gateway.INSTANCE.buildUrl(
                    Api.SEARCH, Api.MODS_SLIM_RETRIEVE,
                    new Object[]{record.doc_id});
            Log.d(TAG, "fetch.basic "+url);
            GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                    url,
                    Request.Priority.HIGH,
                    new Response.Listener<GatewayResult>() {
                        @Override
                        public void onResponse(GatewayResult response) {
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
            String url = Gateway.INSTANCE.buildUrl(
                    Api.PCRUD, Api.RETRIEVE_BRE,
                    new Object[]{Api.ANONYMOUS, record.doc_id});
            Log.d(TAG, "fetch.marcxml "+url);
            GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                    url,
                    Request.Priority.HIGH,
                    new Response.Listener<GatewayResult>() {
                        @Override
                        public void onResponse(GatewayResult response) {
                            record.updateFromBREResponse(response);
                            Log.logElapsedTime(TAG, start_ms, "fetch.marcxml");
                            responseListener.onMetadataLoaded();
                        }
                    },
                    VolleyWrangler.logErrorListener(TAG));
            r.setRetryPolicy(new DefaultRetryPolicy(
                    Api.LONG_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            VolleyWrangler.getInstance(context).addToRequestQueue(r);
        }
    }

    public static void fetchRecordAttributes(final RecordInfo record, Context context, final ResponseListener responseListener) {
        Log.d(TAG, "fetchRecordAttributes id="+record.doc_id);
        if (record.attrs_loaded) {
            responseListener.onIconFormatLoaded();
        } else {
            final long start_ms = System.currentTimeMillis();
            String url = Gateway.INSTANCE.buildUrl(
                    Api.PCRUD, Api.RETRIEVE_MRA,
                    new Object[]{Api.ANONYMOUS, record.doc_id});
            Log.d(TAG, "fetch.attrs "+url);
            GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                    url,
                    Request.Priority.NORMAL,
                    new Response.Listener<GatewayResult>() {
                        @Override
                        public void onResponse(GatewayResult response) {
                            record.updateFromMRAResponse(response);
                            Log.logElapsedTime(TAG, start_ms, "fetch.attrs");
                            responseListener.onIconFormatLoaded();
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
            String url = Gateway.INSTANCE.buildUrl(
                    Api.SEARCH, Api.COPY_COUNT,
                    new Object[]{orgID, record.doc_id});
            GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                    url,
                    Request.Priority.LOW,
                    new Response.Listener<GatewayResult>() {
                        @Override
                        public void onResponse(GatewayResult response) {
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
                if (record.copySummaryList.get(i).getOrgId().equals(orgID)) {
                    total = record.copySummaryList.get(i).getCount();
                    available = record.copySummaryList.get(i).getAvailable();
                    break;
                }
            }
            String totalCopies = context.getResources().getQuantityString(R.plurals.number_of_copies, total, total);
            copySummaryText = String.format(context.getString(R.string.n_of_m_available),
                    available, totalCopies, EgOrg.getOrgNameSafe(orgID));
        }
        return copySummaryText;
    }
}
