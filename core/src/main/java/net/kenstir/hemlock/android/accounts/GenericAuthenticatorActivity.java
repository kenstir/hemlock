/*
 * Copyright (c) 2025 Kenneth H. Cox
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
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package net.kenstir.hemlock.android.accounts;

import android.Manifest;
import android.accounts.Account;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import net.kenstir.hemlock.R;
import net.kenstir.hemlock.android.AccountUtils;
import org.evergreen_ils.net.Volley;
import org.evergreen_ils.data.Library;
import net.kenstir.hemlock.android.Log;
import net.kenstir.hemlock.android.Analytics;
import org.opensrf.util.JSONException;
import org.opensrf.util.JSONReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GenericAuthenticatorActivity extends AuthenticatorActivity {

    private final static String TAG = GenericAuthenticatorActivity.class.getSimpleName();
    private final static int PERMISSIONS_REQUEST_COARSE_LOCATION = 1;

    private Spinner librarySpinner;
    List<Library> libraries = new ArrayList<Library>();
    public String libraries_directory_json_url;
    private long start_ms;

    @Override
    protected void setContentViewImpl() {
        setContentView(R.layout.activity_generic_login);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Analytics.initialize(this);

        libraries_directory_json_url = getString(R.string.evergreen_libraries_url);

        librarySpinner = findViewById(R.id.choose_library_spinner);
        librarySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selected_library = libraries.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selected_library = null;
            }
        });
    }

    @Override
    protected void initSelectedLibrary() {
        selected_library = null;
        Analytics.log(TAG, "initSelectedLibrary null");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onstart");
        startFetchLibraries();
    }

    private void startFetchLibraries() {
        start_ms = System.currentTimeMillis();
        StringRequest r = new StringRequest(Request.Method.GET, libraries_directory_json_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        long duration_ms = System.currentTimeMillis() - start_ms;
                        Log.d(TAG, "volley fetch took " + duration_ms + "ms");
                        handleLibrariesJSON(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showAlert(error.getMessage());
                    }
                });
        Volley.getInstance(this).addToRequestQueue(r);
    }

    private void chooseNearestLibrary() {
        Location location = null;
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            try {
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            } catch (SecurityException ex) {
            }
        }

        float min_distance = Float.MAX_VALUE;
        Integer default_library_index = null;
        for (int i = 0; i < libraries.size(); ++i) {
            Library library = libraries.get(i);
            if (location != null && library.getLocation() != null) {
                float distance = location.distanceTo(library.getLocation());
                if (distance < min_distance) {
                    default_library_index = i;
                    min_distance = distance;
                }
            }
        }
        if (default_library_index != null) {
            librarySpinner.setSelection(default_library_index);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_COARSE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                chooseNearestLibrary();
            }
        }
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            chooseNearestLibrary();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // we could show an expanation to the user here, but it really is not worth it
            return;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_COARSE_LOCATION);
        }
    }

    private void handleLibrariesJSON(String result) {
        // parse the response
        parseLibrariesJSON(result);

        // if the user has any existing accounts, then we can select a reasonable default library
        Library default_library = null;
        Account[] existing_accounts = AccountUtils.getAccountsByType(GenericAuthenticatorActivity.this);
        Log.d(Const.AUTH_TAG, "there are " + existing_accounts.length + " existing accounts");
        if (existing_accounts.length > 0) {
            default_library = AccountUtils.getLibraryForAccount(GenericAuthenticatorActivity.this, existing_accounts[0]);
            Log.d(Const.AUTH_TAG, "default_library=" + default_library);
        }

        // Build a List<String> for use in the spinner adapter
        // While we're at it choose a default library; first by prior account, second by proximity
        Integer default_library_index = null;
        ArrayList<String> l = new ArrayList<String>(libraries.size());
        for (Library library : libraries) {
            if (default_library != null && TextUtils.equals(default_library.getUrl(), library.getUrl())) {
                default_library_index = l.size();
            }
            l.add(library.getDirectoryName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, l);
        librarySpinner.setAdapter(adapter);
        if (default_library_index != null) {
            librarySpinner.setSelection(default_library_index);
        } else {
            requestPermission();
        }
    }

    private void parseLibrariesJSON(String json) {
        libraries.clear();

        if (isDebuggable(this)) {
            //libraries.add(new Library("https://webby.evergreencatalog.com", "evergreencatalog.com Demo", "0ut There, US  (evergreencatalog.com Demo)", null));
            //new Library("https://demo.evergreencatalog.com", "evergreencatalog.com Demo", "0ut There, US  (evergreencatalog.com Demo)", null);
            //new Library("http://mlnc4.mvlcstaff.org"), "MVLC Demo", "0ut There, US (MVLC Demo)", null);// Android does not like this cert
            //libraries.add(new Library("https://kenstir.ddns.net", "debug catalog", "00debug catalog", null));
            libraries.add(new Library("http://192.168.1.8", "debug catalog", "00debug catalog", null));
        }

        if (json != null) {
            List<Map<String, ?>> l;
            try {
                l = (List<Map<String, ?>>) new JSONReader(json).readArray();
                Log.d(TAG, "fetched " + l.size() + " library listings");
            } catch (JSONException e) {
                Log.d(TAG, "failed parsing libraries array", e);
                return;
            }
            for (Map<String, ?> map : l) {
                String url = (String) map.get("url");
//                if (isDebuggable(this)) url = url.replace("https:", "http:");
                String directory_name = (String) map.get("directory_name");
                String short_name = (String) map.get("short_name");
                Double latitude = (Double) map.get("latitude");
                Double longitude= (Double) map.get("longitude");
                Location location = new Location("");
                location.setLatitude(latitude);
                location.setLongitude(longitude);
                Library library = new Library(url, short_name, directory_name, location);
                libraries.add(library);
            }

            Collections.sort(libraries, new Comparator<Library>() {
                @Override
                public int compare(Library a, Library b) {
                    return a.getDirectoryName().compareTo(b.getDirectoryName());
                }
            });
        }
    }

    public static boolean isDebuggable(Context context) {
        return (0 != (context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
    }
}
