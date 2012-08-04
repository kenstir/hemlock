package org.evergreen.android.views.splashscreen;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.globals.NoAccessToServer;
import org.evergreen.android.globals.NoNetworkAccessException;
import org.evergreen.android.globals.Utils;
import org.evergreen.android.searchCatalog.SearchCatalogListView;
import org.evergreen.android.views.ConfigureApplicationActivity;
import org.evergreen.android.views.MainScreenDashboard;
import org.evergreen.android.views.splashscreen.LoadingTask.LoadingTaskFinishedListener;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SplashActivity extends Activity implements
		LoadingTaskFinishedListener {

	private TextView progressText;

	private Context context;

	private ProgressBar progressBar;


	private String TAG = "SplashActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.context = this;
		// Show the splash screen
		setContentView(R.layout.activity_splash);


		progressText = (TextView) findViewById(R.id.action_in_progress);

		// Find the progress bar
		progressBar = (ProgressBar) findViewById(R.id.activity_splash_progress_bar);

		boolean abort = false;
		
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		GlobalConfigs.httpAddress = prefs.getString("library_url", ""); 
		String username = prefs.getString("username", "username");
		String password = prefs.getString("password", "pas");
		AccountAccess.setAccountInfo(username, password);
		try {
			
			Utils.checkNetworkStatus((ConnectivityManager) getSystemService(Service.CONNECTIVITY_SERVICE));
		} catch (NoNetworkAccessException e) {
			abort = true;
			e.printStackTrace();
		} catch (NoAccessToServer e) {
			abort = true;

			//dialog.show();
			Intent configureIntent = new Intent(this,ConfigureApplicationActivity.class);
			startActivityForResult(configureIntent,0);
			
		}

		if (abort != true) {
			// Start your loading
			new LoadingTask(progressBar, this, this, progressText, this)
					.execute("download"); // Pass in whatever you need a url is
											// just an example we don't use it
											// in this tutorial
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		Log.d(TAG, "Result code return " + resultCode);
		
		switch(resultCode){
		
		case ConfigureApplicationActivity.RESULT_CONFIGURE_SUCCESS : {
			new LoadingTask(progressBar, this, this, progressText, this).execute("download");
		
		} break;
		
		}
	}
	
	// This is the callback for when your async task has finished
	@Override
	public void onTaskFinished() {
		completeSplash();
	}

	private void completeSplash() {
		startApp();
		finish(); // Don't forget to finish this Splash Activity so the user
					// can't return to it!
	}

	private void startApp() {
		Intent intent = new Intent(SplashActivity.this,
				SearchCatalogListView.class);
		startActivity(intent);
	}
}