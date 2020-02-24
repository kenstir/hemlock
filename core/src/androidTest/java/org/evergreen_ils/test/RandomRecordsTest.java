package org.evergreen_ils.test;

import android.os.Bundle;

import org.evergreen_ils.Api;
import org.evergreen_ils.searchCatalog.RecordInfo;
import org.evergreen_ils.system.EvergreenServer;
import org.evergreen_ils.android.Log;
import org.evergreen_ils.android.StdoutLogProvider;
import org.evergreen_ils.system.Utils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensrf.net.http.HttpConnection;
import org.opensrf.util.OSRFObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

@RunWith(AndroidJUnit4.class)
public class RandomRecordsTest {
    private static final String TAG = RandomRecordsTest.class.getSimpleName();

    private static HttpConnection mConn;
    private static String mServer;
    private static String mAuthToken;
    private static Random mRandom = new Random();

    private static HttpConnection conn() {
        return mConn;
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        Log.setProvider(new StdoutLogProvider());

        Bundle b = InstrumentationRegistry.getArguments();
        mServer = b.getString("server", "https://gapines.org");
        mAuthToken = Api.ANONYMOUS;

        // init like the app does in LoadingTask
        EvergreenServer eg = EvergreenServer.getInstance();
        eg.connect(mServer);
        Log.d(TAG, "connected to " + mServer);
        mConn = EvergreenServer.getInstance().gatewayConnection();
    }

    @Before
    public void setUp() throws Exception {
    }

    // Check the MRA for random record numbers until we collect 100 samples,
    // then print them by item_type.  I had hoped for a properties-based test;
    // this ain't it.
    @Ignore("todo: reimpl service tests using mocks")
    @Test
    public void sampleItemFormRandomRecords() throws Exception {

        int samples_needed = 10;
        HashMap<String, Integer> seen = new HashMap<>();
        HashMap<String, String> url = new HashMap<>();

        while (samples_needed > 0) {

            Integer id = mRandom.nextInt(6000000);
            RecordInfo record = new RecordInfo(id);

            OSRFObject resp = (OSRFObject) Utils.doRequest(conn(), Api.PCRUD,
                Api.RETRIEVE_MRA, new Object[] {
                    Api.ANONYMOUS, id });
            if (resp == null) {
                Log.d(TAG, "id:"+id+" not found");
                continue; // record does not exist
            }
            --samples_needed;

            record.updateFromMRAResponse(resp);
            String item_form = record.getAttr("item_form");
            if (item_form == null) item_form = "<null>";
            Log.d(TAG, "id:"+id+" item_form:"+item_form);
            Integer count = seen.getOrDefault(item_form, 0);
            if (count == 0)
                url.put(item_form, mServer + "/eg/opac/record/" + id);
            seen.put(item_form, ++count);
        }

        for (Iterator itr = url.keySet().iterator(); itr.hasNext(); ) {
            String key = (String) itr.next();
            Log.d(TAG, "item_form:" + key + " count:" + seen.get(key) + " url:" + url.get(key));
        }
        Log.d(TAG, "done");
    }
}
