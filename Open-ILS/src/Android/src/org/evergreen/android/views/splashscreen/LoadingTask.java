package org.evergreen.android.views.splashscreen;

import org.evergreen.android.accountAccess.AccountAccess;
import org.evergreen.android.accountAccess.SessionNotFoundException;
import org.evergreen.android.globals.GlobalConfigs;
import org.evergreen.android.globals.NoAccessToServer;
import org.evergreen.android.globals.NoNetworkAccessException;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LoadingTask extends AsyncTask<String, Integer, Integer> {

    public interface LoadingTaskFinishedListener {
        void onTaskFinished(); // If you want to pass something back to the
                               // listener add a param to this method
    }

    // This is the progress bar you want to update while the task is in progress
    private final ProgressBar progressBar;
    // This is the listener that will be told when this task is finished
    private final LoadingTaskFinishedListener finishedListener;

    private Context context;

    private TextView progressText;

    private Activity callingActivity;

    private String text;

    /**
     * A Loading task that will load some resources that are necessary for the
     * app to start
     * 
     * @param progressBar
     *            - the progress bar you want to update while the task is in
     *            progress
     * @param finishedListener
     *            - the listener that will be told when this task is finished
     */
    public LoadingTask(ProgressBar progressBar,
            LoadingTaskFinishedListener finishedListener, Context context,
            TextView progressText, Activity callingActivity) {
        this.progressBar = progressBar;
        this.finishedListener = finishedListener;
        this.context = context;
        this.progressText = progressText;
        this.callingActivity = callingActivity;
    }

    @Override
    protected Integer doInBackground(String... params) {
        Log.i("Start download", "Starting task with url: " + params[0]);
        if (resourcesDontAlreadyExist()) {
            downloadResources();
        }
        // Perhaps you want to return something to your post execute
        return 1234;
    }

    private boolean resourcesDontAlreadyExist() {
        // Here you would query your app's internal state to see if this
        // download had been performed before
        // Perhaps once checked save this in a shared preference for speed of
        // access next time
        return true; // returning true so we show the splash every time
    }

    private void downloadResources() {
        // We are just imitating some process thats takes a bit of time (loading
        // of resources / downloading)
        int count = 10;
        text = "download files";
        publishProgress(50);

        GlobalConfigs gl = GlobalConfigs.getGlobalConfigs(context);
        text = "authenticate user";
        publishProgress(70);

        AccountAccess ac = AccountAccess.getAccountAccess(
                GlobalConfigs.httpAddress, (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE));
        try {
            ac.authenticate();
        } catch (Exception e) {
        }
        text = "get user bookbags";
        publishProgress(90);

        try {
            ac.bookBags = ac.getBookbags();
        } catch (SessionNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoNetworkAccessException e) {
            e.printStackTrace();
        } catch (NoAccessToServer e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        text = "loading application";
        publishProgress(100);

    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        progressBar.setProgress(values[0]); // This is ran on the UI thread so
                                            // it is ok to update our progress
                                            // bar ( a UI view ) here
        progressText.setText(text);
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        finishedListener.onTaskFinished(); // Tell whoever was listening we have
                                           // finished
    }
}