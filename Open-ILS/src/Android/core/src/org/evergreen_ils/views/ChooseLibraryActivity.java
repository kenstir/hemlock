package org.evergreen_ils.views;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import org.evergreen_ils.R;
import org.evergreen_ils.globals.AppPrefs;
import org.evergreen_ils.globals.GlobalConfigs;
import org.evergreen_ils.globals.Utils;
import org.evergreen_ils.searchCatalog.Library;
import org.evergreen_ils.views.splashscreen.SplashActivity;
import org.opensrf.util.JSONException;
import org.opensrf.util.JSONReader;

import java.util.*;

/**
 * Created by kenstir on 2015-11-05.
 */
public class ChooseLibraryActivity extends ActionBarActivity {

    /*
    todo: DELETE ME DELETE ME DELETE ME DELETE ME DELETE ME DELETE ME DELETE ME DELETE ME DELETE ME
    DELETE ME DELETE ME DELETE ME DELETE ME DELETE ME DELETE ME DELETE ME DELETE ME DELETE ME
    DELETE ME DELETE ME DELETE ME DELETE ME DELETE ME DELETE ME DELETE ME DELETE ME DELETE ME
    public final static int CHOOSE_LIBRARY_REQUEST = 0;
    private final String TAG = ChooseLibraryActivity.class.getSimpleName();
    public static String librariesJSONUrl = "http://evergreen-ils.org/testing/libraries.json";
    Context context;
    Spinner consortiumSpinner;
    Library selected_library = null;
    List<Library> libraries = new ArrayList<Library>();
    private boolean restarted = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();

        setContentView(R.layout.activity_choose_library);

        AppPrefs.init(getApplicationContext());

        ActionBar actionBar = getSupportActionBar();

        consortiumSpinner = (Spinner) findViewById(R.id.choose_library_spinner);
        consortiumSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selected_library = libraries.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selected_library = null;
            }
        });
        ((Button)findViewById(R.id.choose_library_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectLibrary();
            }
        });
    }

    private void selectLibrary() {
        if (selected_library != null) {
            Log.d(TAG, "selected library " + selected_library.directory_name);
            AppPrefs.setString(AppPrefs.LIBRARY_URL, selected_library.url);
            AppPrefs.setString(AppPrefs.LIBRARY_NAME, selected_library.short_name);

            startSplashActivity();
        }
    }

    private void startSplashActivity() {
        // This is different from SplashActivity.restartApp(this) in that it does not clear the back stack.
        // This is so that a user can back up and choose a different library.
        Intent i = new Intent(this, SplashActivity.class);
        startActivity(i);
    }

    @Override
    protected void onStart() {
        super.onStart();
        GlobalConfigs.httpAddress = AppPrefs.getString(AppPrefs.LIBRARY_URL);// kenstir todo: replace all refs to GlobalConfigs.httpAddress
        Log.d(TAG, "kcxxx: onstart: url=" + GlobalConfigs.httpAddress);
        if (!TextUtils.isEmpty(GlobalConfigs.httpAddress) && !restarted) {
            startSplashActivity();
        } else {
            startTask();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        restarted = true;
        Log.d(TAG, "kcxxx: onrestart");
    }

    private void startTask() {
        new FetchConsortiumsTask().execute(librariesJSONUrl);
    }

    private void parseLibrariesJSON(String json) {
        libraries.clear();

        if (json != null) {
            List<Map<String,?>> l;
            try {
                l = (List<Map<String,?>>) new JSONReader(json).readArray();
            } catch (JSONException e) {
                Log.d(TAG, "failed parsing libraries array", e);
                return;
            }
            for (Map<String, ?> map : l) {
                String url = (String) map.get("url");
                String directory_name = (String) map.get("directory_name");
                String short_name = (String) map.get("short_name");
                Library library = new Library(url, short_name, directory_name);
                libraries.add(library);
            }

            Collections.sort(libraries, new Comparator<Library>() {
                @Override
                public int compare(Library a, Library b) {
                    return a.directory_name.compareTo(b.directory_name);
                }
            });

            for (int i = 0; i< libraries.size(); ++i) {
                Log.d(TAG, "c["+i+"]: "+ libraries.get(i).directory_name);
            }
        }
    }
*/
}
