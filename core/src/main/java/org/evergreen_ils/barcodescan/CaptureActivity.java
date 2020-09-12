package org.evergreen_ils.barcodescan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import org.evergreen_ils.R;
import org.evergreen_ils.android.App;
import org.evergreen_ils.barcodescan.camera.CameraManager;
import org.evergreen_ils.android.Log;
import org.evergreen_ils.android.Analytics;

import java.io.IOException;
import java.util.Vector;

public class CaptureActivity extends Activity implements SurfaceHolder.Callback {

	private static final String TAG = CaptureActivity.class.getSimpleName();
	
	private ViewfinderView viewfinderView;

	private CaptureActivityHandler handler;

	private CameraManager cameraManager;
	private boolean hasSurface;
	private Vector<BarcodeFormat> decodeFormats;
	// private HistoryManager historyManager;
	private Result lastResult;
	private String characterSet;
	public static final int BARCODE_SEARCH = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Analytics.initialize(this);
		if (!App.isStarted()) {
            App.restartApp(this);
            return;
        }

		// Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Remove notification bar
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.barcode_scan);

		Log.d("BARCODE","Start application 1");
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// init camera manager
		cameraManager = new CameraManager(getApplication());
		hasSurface = false;
		handler = null;

		viewfinderView = findViewById(R.id.viewfinder_view);

		viewfinderView.setCameraManager(cameraManager);
		//database = new DBHelper(this);
		
		Log.d("BARCODE","Start application 2");
	}
	

	@Override
	public void onResume() {
		super.onResume();

		SurfaceView surfaceView = findViewById(R.id.camera_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still
			// exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		cameraManager.closeDriver();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			
			
			cameraManager.openDriver(surfaceHolder);
			// Creating the handler starts the preview, which can also throw a
			// RuntimeException.
			if (handler == null) {
			    //decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
				handler = new CaptureActivityHandler(this, decodeFormats,
						characterSet, cameraManager);
			}
		} catch (IOException ioe) {
			Log.w(TAG, "Error initializing camera", ioe);
			displayFrameworkBugMessageAndExit("IOException");
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			Log.w(TAG, "Unexpected error initializing camera", e);
			displayFrameworkBugMessageAndExit("RuntimeException");
		}
	}

	public Handler getHandler() {
		return handler;
	}

	ViewfinderView getViewfinderView() {
		return viewfinderView;
	}


	private void displayFrameworkBugMessageAndExit(String info) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.ou_library_name));
		builder.setMessage("[" + info + "] "
				+ getString(R.string.msg_camera_framework_bug));
		builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
		builder.setOnCancelListener(new FinishListener(this));
		builder.show();
	}

	public void drawViewfinder() {
		// Draw image result

		viewfinderView.drawViewfinder();
	}

	public void removePoints() {

	}


	/**
	   * A valid barcode has been found, so give an indication of success and show the results.
	   *
	   * @param rawResult The contents of the barcode.
	   * @param barcode   A greyscale bitmap of the camera data which was decoded.
	   */
	  public void handleDecode(Result rawResult, Bitmap barcode) {
		  

		  /*
		  AlertDialog.Builder builder = new AlertDialog.Builder(this);
		  builder.setMessage("Code bar Message : " + rawResult.getText())
		         .setCancelable(false)
		         .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		             public void onClick(DialogInterface dialog, int id) {
		             //restart preview to decode more barcodes
		            	 if(handler != null){
		                	handler.sendEmptyMessage(R.id.restart_preview);
		                }
		             }
		         });

		  AlertDialog alert = builder.create();
		  alert.show();
		  */
		 	
		 //Toast.makeText(this, rawResult.getText(), Toast.LENGTH_LONG).show(); 
		 Log.d("BARCODE","Value"+ rawResult.getText());
		 Intent intent = new Intent();
		 intent.putExtra("barcodeValue", rawResult.getText());
		 setResult(BARCODE_SEARCH, intent);
		 finish();
	  }

}
