package org.evergreen.android.views;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.globals.GlobalConfigs;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class ApplicationPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener{

	
	private ProgressDialog progressDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.application_preference_screen);
		
		Context context = getApplicationContext();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		//register preference listener
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		
		if(key.equals("username")){
			AccountAccess.userName = sharedPreferences.getString("username", "");
		}else
			if(key.equals("password")){
				AccountAccess.password = sharedPreferences.getString("password", "");
			}else
				if(key.equals("library_url")){
					GlobalConfigs.httpAddress = sharedPreferences.getString("library_url", "");
				}
		
		//test connection
		
		progressDialog = ProgressDialog.show(this, "Account login", "Please wait while we test the new user account information");
		
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				AccountAccess account = new AccountAccess(GlobalConfigs.httpAddress);
				account.authenticate();
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						progressDialog.dismiss();
						
					}
				});
			}
		});
		
		thread.start();
	}

	
	
}
