package org.evergreen_ils.searchCatalog;

import android.content.Context;
import android.text.TextUtils;
import com.android.volley.Response;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.globals.Utils;
import org.evergreen_ils.net.GatewayJsonObjectRequest;
import org.evergreen_ils.net.VolleyWrangler;
import org.opensrf.util.GatewayResponse;
import org.opensrf.util.OSRFObject;

/**
 * Created by kenstir on 12/27/2015.
 */
public class RecordLoader {
    private static final String TAG = RecordLoader.class.getSimpleName();

    public interface ResponseListener {
        public void onMetadataLoaded();
        public void onSearchFormatLoaded();
    }

    public static void fetch(final RecordInfo record, final Context context, final ResponseListener responseListener) {
        fetchBasicMetadata(record, context, responseListener);
        fetchSearchFormat(record, context, responseListener);
    }

    private static void fetchBasicMetadata(final RecordInfo record, Context context, final ResponseListener responseListener) {
        if (!TextUtils.isEmpty(record.title)) {
            responseListener.onMetadataLoaded();
        } else {
            String url = GlobalConfigs.getUrl(Utils.buildGatewayUrl(
                    SearchCatalog.SERVICE, SearchCatalog.METHOD_SLIM_RETRIEVE,
                    new Object[]{record.doc_id}));
            GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                    url,
                    new Response.Listener<GatewayResponse>() {
                        @Override
                        public void onResponse(GatewayResponse response) {
                            RecordInfo.updateFromMODSResponse(record, response.payload);
                            responseListener.onMetadataLoaded();
                        }
                    },
                    VolleyWrangler.logErrorListener(TAG));
            VolleyWrangler.getInstance(context).addToRequestQueue(r);
        }
    }

    private static void fetchSearchFormat(final RecordInfo record, Context context, final ResponseListener responseListener) {
        if (!TextUtils.isEmpty(record.search_format)) {
            responseListener.onSearchFormatLoaded();
        } else {
            // todo newer EG supports "ANONYMOUS" PCRUD which should be faster w/o authToken
            String url = GlobalConfigs.getUrl(Utils.buildGatewayUrl(
                    AccountAccess.PCRUD_SERVICE, AccountAccess.PCRUD_METHOD_RETRIEVE_MRA,
                    new Object[]{AccountAccess.getInstance().getAuthToken(), record.doc_id}));
            GatewayJsonObjectRequest r = new GatewayJsonObjectRequest(
                    url,
                    new Response.Listener<GatewayResponse>() {
                        @Override
                        public void onResponse(GatewayResponse response) {
                            record.search_format = AccountAccess.getSearchFormatFromMRAResponse((OSRFObject) response.payload);
                            responseListener.onSearchFormatLoaded();
                        }
                    },
                    VolleyWrangler.logErrorListener(TAG));
            VolleyWrangler.getInstance(context).addToRequestQueue(r);
        }
    }
}
