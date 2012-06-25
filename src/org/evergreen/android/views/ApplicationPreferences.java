package org.evergreen.android.views;

import org.evergreen.android.R;
import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.globals.NoAccessToHttpAddress;
import org.evergreen.android.globals.NoNetworkAccessException;
import org.evergreen.android.globals.Utils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class ApplicationPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener{

	
	private ProgressDialog progressDialog;
	
	private ApplicationPreferences reference;
	
	private Context context;
	
	private String TAG = "ApplicationPreferences";
	
	private Thread connectionThread = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.application_preference_screen);

		context = this;
		reference = this;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		//register preference listener
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	
	@Override
	protected void onResume() {
		super.onResume();
		reference = this;
	}
	@Override
	protected void onPause() {
		super.onPause();
		reference = null;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		reference = null;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		reference = null;
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
					
					
						
						progressDialog = new ProgressDialog(context);
						progressDialog.setMessage("Please wait while downloading FM IDL file and OrgTree");
						progressDialog.show();
						
						Thread loadIDLThread = new Thread(new Runnable() {
							
							@Override
							public void run() {
								System.out.println("FM idl download");
								GlobalConfigs sg = GlobalConfigs.getGlobalConfigs(context);
								sg.loadIDLFile();
								sg.getOrganisations();
							}
						});
						
						loadIDLThread.start();
						
						//wait for execution
						try{
							loadIDLThread.join();
						}catch(Exception e){}
						
						progressDialog.dismiss();
					}
					
				
		
			
		//test connection
		if(!isFinishing())
			progressDialog = ProgressDialog.show(this, "Account login", "Please wait while we test the new user account information");	
		
		connectionThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				boolean routeToAddress = true;
				AccountAccess account = AccountAccess.getAccountAccess(GlobalConfigs.httpAddress);
				try{
					Utils.checkNetworkStatus((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE), context);
				}catch(NoNetworkAccessException e){
					routeToAddress = false;
					
					Log.d(TAG," No network access");
					
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							if(reference != null){
								progressDialog.dismiss();
								AlertDialog.Builder builder = new AlertDialog.Builder(context);
								builder.setMessage("There seams to be no network connectivity! Do you want to start network settings?").setPositiveButton("Yes", dialogClickListener)
								.setNegativeButton("No", dialogClickListener).show();
							}
						}
					});

				}catch(NoAccessToHttpAddress e){
					
					Log.d(TAG, " no route to hoast");
					routeToAddress = false;
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							if(reference != null)
								Toast.makeText(getApplicationContext(), "There is no route to host :" + GlobalConfigs.httpAddress, Toast.LENGTH_LONG).show();
						}
	
					});
				}

				
				if(routeToAddress){
					if(account.authenticate()){
									
						runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								if(reference != null){
									progressDialog.dismiss();
									Toast.makeText(context, "Autenthication successfully established :" + GlobalConfigs.httpAddress, Toast.LENGTH_LONG).show();
								}
							}
						});
					}else{
						runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								if(reference != null){
									progressDialog.dismiss();
									Toast.makeText(context, "Please check username and password ", Toast.LENGTH_LONG).show();
								}
							}
						});
					}
				}
				else
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
						if(reference != null)	
							progressDialog.dismiss();	
						}
					});
			}
		});
		
		connectionThread.start();

	}
/*
 *  Dialog interface for starting the network settings
 */
	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	        switch (which){
	        case DialogInterface.BUTTON_POSITIVE:
	            //Yes button clicked
	        	
	        	context.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));

	            break;

	        case DialogInterface.BUTTON_NEGATIVE:
	            //No button clicked
	        	
	            break;
	        }
	    }
	};
	
}
