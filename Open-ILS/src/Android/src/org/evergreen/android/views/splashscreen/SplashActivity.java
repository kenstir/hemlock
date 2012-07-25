package org.evergreen.android.views.splashscreen;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.globals.NoAccessToServer;
import org.evergreen.android.globals.NoNetworkAccessException;
import org.evergreen.android.globals.Utils;
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

	private Dialog dialog = null;

	private ProgressDialog progressDialog = null;

	private EditText server_http;

	private EditText username;

	private EditText password;

	private ProgressBar progressBar;

	private SplashActivity activity;

	private String TAG = "SplashActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.context = this;
		// Show the splash screen
		setContentView(R.layout.activity_splash);

		activity = this;
		dialog = new Dialog(context);
		dialog.setContentView(R.layout.dialog_configure_application);
		dialog.setTitle("Configure application");

		server_http = (EditText) dialog.findViewById(R.id.server_http_adress);
		username = (EditText) dialog.findViewById(R.id.username);
		password = (EditText) dialog.findViewById(R.id.password);
		Button cancel = (Button) dialog.findViewById(R.id.cancel_button);
		Button connect = (Button) dialog.findViewById(R.id.connect_button);

		cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Toast.makeText(context, "Exit application", Toast.LENGTH_SHORT)
						.show();
				finish();
			}
		});

		connect.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				Thread checkConn = new Thread(new Runnable() {
					@Override
					public void run() {

						boolean server_address = false;
						boolean auth = false;

						try {
							server_address = Utils
									.checkIfNetAddressIsReachable(server_http
											.getText().toString());
						} catch (NoAccessToServer e) {
							server_address = false;
						}

						if (server_address == true) {

							SharedPreferences preferences = PreferenceManager
									.getDefaultSharedPreferences(context);
							SharedPreferences.Editor editor = preferences
									.edit();
							//store into preference
							editor.putString("library_url", server_http
									.getText().toString());

							
							editor.putString("username", username.getText().toString());
							editor.putString("password", password.getText().toString());
							
							editor.commit();
							GlobalConfigs.httpAddress = server_http.getText().toString();
							AccountAccess accountAccess = AccountAccess
									.getAccountAccess(GlobalConfigs.httpAddress,(ConnectivityManager) activity.getSystemService(Service.CONNECTIVITY_SERVICE));
							
							AccountAccess.setAccountInfo(username.getText().toString(),password.getText().toString());

							try {
								auth = accountAccess.authenticate();
								Log.d(TAG, "Auth " + auth);
							} catch (Exception e) {
								System.out.println("Exception " + e.getMessage());
							}

							if (auth == true) {

								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										progressDialog.dismiss();
										dialog.dismiss();
										// Start your loading
										new LoadingTask(progressBar, activity,
												activity, progressText, activity)
												.execute("download");
									}
								});

							} else {
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										progressDialog.dismiss();
										Toast.makeText(context,
												"Bad user login information",
												Toast.LENGTH_LONG).show();
									}
								});
							}

						} else {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									progressDialog.dismiss();
									Toast.makeText(context,
											"Bad library server url",
											Toast.LENGTH_LONG).show();
								}
							});

						}

					}
				});

				progressDialog = ProgressDialog.show(context, "Please wait",
						"Checking server and credentials");
				checkConn.start();
			}
		});

		dialog.setCancelable(true);

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

			dialog.show();

		}

		if (abort != true) {
			// Start your loading
			new LoadingTask(progressBar, this, this, progressText, this)
					.execute("download"); // Pass in whatever you need a url is
											// just an example we don't use it
											// in this tutorial
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
				MainScreenDashboard.class);
		startActivity(intent);
	}
}