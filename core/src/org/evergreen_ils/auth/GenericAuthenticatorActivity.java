/*
 * Copyright (C) 2016 Kenneth H. Cox
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

package org.evergreen_ils.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountUtils;
import org.evergreen_ils.globals.Library;
import org.evergreen_ils.globals.Log;
import org.evergreen_ils.net.VolleyWrangler;
import org.opensrf.util.JSONException;
import org.opensrf.util.JSONReader;

import java.util.*;

public class GenericAuthenticatorActivity extends AuthenticatorActivity {

    private final static String TAG = GenericAuthenticatorActivity.class.getSimpleName();

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

        libraries_directory_json_url = getString(R.string.evergreen_libraries_url);

        librarySpinner = (Spinner) findViewById(R.id.choose_library_spinner);
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
    protected void initDefaultSelectedLibrary() {
        selected_library = null;
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
        VolleyWrangler.getInstance(this).addToRequestQueue(r);
    }

    private void handleLibrariesJSON(String result) {
        // parse the response
        parseLibrariesJSON(result);

        // if the user has any existing accounts, then we can select a reasonable default library
        Library default_library = null;
        Location last_location = null;
        Account[] existing_accounts = AccountUtils.getAccountsByType(GenericAuthenticatorActivity.this);
        Log.d(Const.AUTH_TAG, "there are " + existing_accounts.length + " existing accounts");
        if (existing_accounts.length > 0) {
            default_library = AccountUtils.getLibraryForAccount(GenericAuthenticatorActivity.this, existing_accounts[0]);
            Log.d(Const.AUTH_TAG, "default_library=" + default_library);
        } else {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) last_location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        // Build a List<String> for use in the spinner adapter
        // While we're at it choose a default library; first by prior account, second by proximity
        Integer default_library_index = null;
        float min_distance = Float.MAX_VALUE;
        ArrayList<String> l = new ArrayList<String>(libraries.size());
        for (Library library : libraries) {
            if (default_library != null && TextUtils.equals(default_library.url, library.url)) {
                default_library_index = l.size();
            } else if (last_location != null && library.location != null) {
                float distance = last_location.distanceTo(library.location);
                if (distance < min_distance) {
                    default_library_index = l.size();
                    min_distance = distance;
                }
            }
            l.add(library.directory_name);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, l);
        librarySpinner.setAdapter(adapter);
        if (default_library_index != null) {
            librarySpinner.setSelection(default_library_index);
        }
    }

    private void parseLibrariesJSON(String json) {
        libraries.clear();

        if (isDebuggable(this)) {
            Library library = new Library("https://demo.evergreencatalog.com", "evergreencatalog.com Demo", "0ut There, US  (evergreencatalog.com Demo)", null);
            //Library library = new Library("http://mlnc4.mvlcstaff.org"), "MVLC Demo", "0ut There, US (MVLC Demo)", null);// Android does not like this cert
            libraries.add(library);
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
                    return a.directory_name.compareTo(b.directory_name);
                }
            });
        }
    }

    public static boolean isDebuggable(Context context) {
        return (0 != (context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
    }
}
