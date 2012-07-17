package org.evergreen.android.views.splashscreen;



import org.evergreen.android.R;
import org.evergreen.android.views.MainScreenDashboard;
import org.evergreen.android.views.splashscreen.LoadingTask.LoadingTaskFinishedListener;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SplashActivity extends Activity implements LoadingTaskFinishedListener {

	
	private TextView progressText;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Show the splash screen
        setContentView(R.layout.activity_splash);
        
        progressText = (TextView) findViewById(R.id.action_in_progress);
        
        // Find the progress bar
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.activity_splash_progress_bar);
        // Start your loading
        new LoadingTask(progressBar, this, this, progressText).execute("download"); // Pass in whatever you need a url is just an example we don't use it in this tutorial
    }

    // This is the callback for when your async task has finished
    @Override
	public void onTaskFinished() {
		completeSplash();
	}

    private void completeSplash(){
		startApp();
		finish(); // Don't forget to finish this Splash Activity so the user can't return to it!
    }

    private void startApp() {
		Intent intent = new Intent(SplashActivity.this, MainScreenDashboard.class);
		startActivity(intent);
	}
}