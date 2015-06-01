/*
 * PixLive SDK Sample for Android
 * Copyright (C) 2012-2015 PixLive SDK 
 *
 */

package com.mycompany.myapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vidinoti.android.vdarsdk.DeviceCameraImageSender;
import com.vidinoti.android.vdarsdk.NotificationCompat;
import com.vidinoti.android.vdarsdk.NotificationFactory;
import com.vidinoti.android.vdarsdk.VDARAnnotationView;
import com.vidinoti.android.vdarsdk.VDARCode;
import com.vidinoti.android.vdarsdk.VDARContext;
import com.vidinoti.android.vdarsdk.VDARPrior;
import com.vidinoti.android.vdarsdk.VDARRemoteController;
import com.vidinoti.android.vdarsdk.VDARRemoteController.ObserverUpdateInfo;
import com.vidinoti.android.vdarsdk.VDARRemoteControllerListener;
import com.vidinoti.android.vdarsdk.VDARSDKController;
import com.vidinoti.android.vdarsdk.VDARSDKControllerEventReceiver;

/**
 * Is a sample code of an Android activity demonstrating the integration of the
 * VDARSDK.
 */
public class ARActivity extends Activity implements
		VDARSDKControllerEventReceiver,
		VDARRemoteControllerListener {

	private DeviceCameraImageSender imageSender = null;
	private VDARAnnotationView annotationView = null;

	private static final String TAG = "ARActivity";

	/** Your SDK license key available from the ARManager */
	private static final String MY_SDK_LICENSE_KEY = "qub0iooegrh2rz8ub1rz";

	/** Your Project ID in Google APIs Console for Push Notification (GCM) */
	private static final String GOOGLE_API_PROJECT_ID_FOR_NOTIFICATIONS = "AIzaSyDECy31a7iLLhuk6gz9lW9a2HJz1Mv2qiU";

	private static boolean syncInProgress = false;

	private ProgressBar progressSync;

	private RelativeLayout rl;

	private View dlAnimView;

	private View dlAnimSmallView;

	/** Initiates the sample activity */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		/*
		 * Start the AR SDK. We need to create a static method for this so that
		 * the SDK can be also started from the background when a beacon is
		 * detected
		 */
		startSDK(this);

		/* Activate the SDK for this activity */
		VDARSDKController.getInstance().setActivity(this);

		/* Register ourself to receive detection events */
		VDARSDKController.getInstance().registerEventReceiver(this);

		/* Synchronizes AR models with the server. */
		synchronize();

		/* Starts the annotation system by creating the views. */
		setupAnnotationView();

		/* Setup the views for the application */
		rl = new RelativeLayout(this);

		addDLAnim();
		addDLAnimSmall();
		addProgSync();

		addContentView(rl, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));

		final Intent intent = getIntent();

		/*
		 * If the activity has been launched in response to a notification, we
		 * have to tell the PixLive SDK to process this notification
		 */
		if (intent != null && intent.getExtras() != null
				&& intent.getExtras().getString("nid") != null) {

			final String nid = intent.getExtras().getString("nid");

			VDARSDKController.getInstance().addNewAfterLoadingTask(
					new Runnable() {
						@Override
						public void run() {
							VDARSDKController.getInstance()
									.processNotification(
											nid,
											intent.getExtras().getBoolean(
													"remote"));
						}
					});
		}
	}

	/**
	 * Start the SDK on the context c. Doesn't do anything if already started.
	 * @param c The Android context to start the SDK on.
	 */
	static void startSDK(final Context c) {

		if (VDARSDKController.getInstance() != null) {
			return;
		}

		/* Start the PixLive SDK on the below path (the data will be stored there) */
		String modelPath = c.getApplicationContext().getFilesDir()
				.getAbsolutePath()
				+ "/models";

		VDARSDKController.startSDK(c, modelPath, MY_SDK_LICENSE_KEY);

		/* Comment out to disable QR code detection */
		VDARSDKController.getInstance().setEnableCodesRecognition(true);

		/* Enable push notifications */
		/* ------------------------- */

		/*
		 * See the documentation at
		 * http://doc.vidinoti.com/vdarsdk/web/android/latest for instructions
		 * on how to setup it
		 */
		/*
		 * You need your app project ID from the Google APIs Console at
		 * https://code.google.com/apis/console
		 */
		VDARSDKController.getInstance().setNotificationsSupport(true,
				GOOGLE_API_PROJECT_ID_FOR_NOTIFICATIONS);

		/* Tells the SDK what to do when a notification has to be shown to the user.
		 * If you don't provide it, the SDK will create a standard notification with sound and vibration.
		 */
		VDARSDKController.getInstance().setNotificationFactory(
				new NotificationFactory() {

					@Override
					public Notification createNotification(String title,
							String message, String notificationID) {
						Intent appintent = new Intent(c,
								ARActivity.class);

						// These lines are mandatory.
						appintent.putExtra("nid", notificationID);
						appintent.putExtra("remote", false);

						PendingIntent contentIntent = PendingIntent
								.getActivity(c, 0, appintent,
										PendingIntent.FLAG_UPDATE_CURRENT);

						// The notification settings, you can customize it to your needs.
						NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
								c).setSmallIcon(R.drawable.ic_launcher)
								.setContentTitle(title).setContentText(message)
								.setContentIntent(contentIntent)
								.setAutoCancel(true)
								.setVibrate(new long[] { 100, 400, 200, 800 })
								.setLights(Color.BLUE, 500, 1500);

						mBuilder.setSound(RingtoneManager
								.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

						return mBuilder.getNotification();
					}
				});

	}

	/**
	 * Method that adds a progress bar for synchronization progress
	 */
	private void addProgSync() {

		progressSync = new ProgressBar(this, null,
				android.R.style.Widget_ProgressBar_Horizontal);
		progressSync.setProgressDrawable(Resources.getSystem().getDrawable(
				android.R.drawable.progress_horizontal));
		progressSync.setMax(1000);
		progressSync.setVisibility(View.INVISIBLE);
		progressSync.setIndeterminate(false);
		Resources r = getResources();
		RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
				r.getDisplayMetrics());
		float py = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
				r.getDisplayMetrics());

		layout.leftMargin = (int) px;
		layout.rightMargin = (int) px;
		layout.bottomMargin = (int) py;
		layout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		progressSync.setLayoutParams(layout);
		rl.addView(progressSync);

		progressSync.setProgress(0);
	}

	/**
	 * Add the downloading message on the AR view
	 */
	private void addDLAnim() {

		RelativeLayout.LayoutParams dlAnimViewLayoutParams = new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		LayoutInflater vi = (LayoutInflater) getApplicationContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		dlAnimView = vi.inflate(R.layout.dl_anim, null);
		dlAnimView.setLayoutParams(dlAnimViewLayoutParams);
		((TextView) dlAnimView.findViewById(R.id.dl_anim_text))
				.setText("Now loading");
		rl.addView(dlAnimView);
		dlAnimView.setVisibility(View.VISIBLE);

	}

	private void addDLAnimSmall() {

		RelativeLayout.LayoutParams dlAnimViewLayoutParams = new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		LayoutInflater vi = (LayoutInflater) getApplicationContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		dlAnimSmallView = vi.inflate(R.layout.dl_anim_small, null);
		dlAnimSmallView.setLayoutParams(dlAnimViewLayoutParams);
		((TextView) dlAnimSmallView.findViewById(R.id.dl_anim_small_text))
				.setText("Now loading");
		rl.addView(dlAnimSmallView);
		dlAnimSmallView.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onStop() {
		super.onStop();
		VDARSDKController.getInstance().onStop();
	}

	/**
	 * Start a new PixLive SDK content synchronization.
	 */
	private void synchronize() {
		
		//We have to make sure not to synchronized twice at the same time.
		synchronized (this) {
			if (syncInProgress)
				return;

			syncInProgress = true;
		}

		// Synchronization has to be started after the SDK is loaded. The
		// addNewAfterLoadingTask method allows that.
		VDARSDKController.getInstance().addNewAfterLoadingTask(new Runnable() {

			@Override
			public void run() {
				ArrayList<VDARPrior> priors = new ArrayList<VDARPrior>();
				
				// You can add a tag this way to do tag based synchronization.
				// Leaving will synchronize all the models you have created and
				// that are published on PixLive Maker.
				// priors.add(new VDARTagPrior("MyTag"));

				Log.v(TAG, "Starting sync");

				//Add our self so that we receive progress event for the sync.
				VDARRemoteController.getInstance().addProgressListener(
						ARActivity.this);

				// Launch sync.
				VDARRemoteController.getInstance()
						.syncRemoteContextsAsynchronouslyWithPriors(priors,
								new Observer() {

									@Override
									public void update(Observable observable,
											Object data) {
										ObserverUpdateInfo info = (ObserverUpdateInfo) data;

										if (info.isCompleted()) {
											Log.v(TAG, "Done syncing. Synced "
													+ info.getFetchedContexts()
															.size()
													+ " models.");
											synchronized (ARActivity.this) {
												syncInProgress = false;
											}
										}

									}
								});
			}
		});
	}

	/** Is called when the activity is paused. */
	@Override
	public void onPause() {
		super.onPause();
		
		//Pause the SDK and pause our AR View. Mandatory.
		VDARSDKController.getInstance().onPause();
		annotationView.onPause();
	}

	/**
	 * We override this method to make the interface orientation smoother as it
	 * is properly handled internally in the PixLive SDK. It allows a proper
	 * rotation without having the system to restart the activity.
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	/** Is called when the activity is resumed. */
	@Override
	public void onResume() {
		super.onResume();

		VDARSDKController.getInstance().setActivity(this);
		VDARSDKController.getInstance().onResume();
		annotationView.onResume();

		/*
		 * Trigger a synchronization so that every time we load the app,
		 * everything is up to date.
		 */
		synchronize();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		/* Process the notification if needed. */
		if (intent != null && intent.getExtras() != null
				&& intent.getExtras().getString("nid") != null) {
			VDARSDKController.getInstance().processNotification(
					intent.getExtras().getString("nid"),
					intent.getExtras().getBoolean("remote"));
		}
	}

	/** Setup the AR view */
	private void setupAnnotationView() {
		try {

			SurfaceView surfaceView = null;
			if (!DeviceCameraImageSender.doesSupportDirectRendering()) {
				// This is for Android 2.3 devices. If you don't supported them, you can remove this if block.
				surfaceView = new SurfaceView(this);
				surfaceView.getHolder().setType(
						SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
				setContentView(surfaceView);
			}

			imageSender = new DeviceCameraImageSender(surfaceView);

		} catch (IOException e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}

		annotationView = new VDARAnnotationView(this);

		if (!DeviceCameraImageSender.doesSupportDirectRendering()) {
			addContentView(annotationView, new LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		} else {
			setContentView(annotationView, new LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}
		annotationView.setDarkScreenMode(false);
		annotationView.setAnimationSpeed(1.0f);
	}

	/**
	 * Is called when the overall system is running low on memory.
	 * 
	 * //@see http
	 *      ://developer.android.com/reference/android/content/ComponentCallbacks
	 *      .html#onLowMemory()
	 * */
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		/* Tell the system to release as much memory as it can */
		if (VDARSDKController.getInstance() != null)
			VDARSDKController.getInstance().releaseMemory();
	}

	@Override
	public void onCodesRecognized(ArrayList<VDARCode> codes) {
		Log.v(TAG, "Code recongized:");
		Log.v(TAG, "" + codes);

		for (final VDARCode c : codes) {
			if (c.isSpecialCode())
				continue; // Ignore special code handled by the SDK

			final Uri u = Uri.parse(c.getCodeData());

			// Open URL
			if (u != null) {
				this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						try {
							Intent browserIntent = new Intent(
									Intent.ACTION_VIEW, u);
							startActivity(browserIntent);
						} catch (Exception e) {
							new AlertDialog.Builder(
									ARActivity.this)
									.setTitle("QR Code")
									.setMessage(
											"Invalid URL in recognized QR Code: "
													+ c.getCodeData())
									.setNeutralButton("OK",
											new OnClickListener() {

												@Override
												public void onClick(
														DialogInterface dialog,
														int which) {
													dialog.dismiss();
												}
											}).show();
						}
					}
				});

			}
		}
	}

	@Override
	public void onFatalError(final String errorDescription) {
		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				try {
					new AlertDialog.Builder(ARActivity.this)
							.setTitle("Augmented reality system error")
							.setMessage(errorDescription)
							.setNeutralButton("OK", new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).show();
				} catch (Exception e) {
				}
			}
		});
	}

	@Override
	public void onPresentAnnotations() {
		// Hide overlay
	}

	@Override
	public void onAnnotationsHidden() {
		// Show overlay
	}

	@Override
	public void onSyncProgress(VDARRemoteController controller, float progress,
			boolean isReady, String folder) {

		if (progress < 100) {

			progressSync.setProgress((int) (progress * 10));
			if (progressSync.getVisibility() != View.VISIBLE) {
				progressSync.setVisibility(View.VISIBLE);
				progressSync.bringToFront();
			}

			if (folder != null && folder.length() > 0) {
				((TextView) dlAnimView.findViewById(R.id.dl_anim_text))
						.setText("Downloading " + folder);
				((TextView) dlAnimSmallView
						.findViewById(R.id.dl_anim_small_text))
						.setText("Downloading " + folder);
			} else if (folder != null) {
				((TextView) dlAnimView.findViewById(R.id.dl_anim_text))
						.setText("Downloading AR contents");
				((TextView) dlAnimSmallView
						.findViewById(R.id.dl_anim_small_text))
						.setText("Downloading AR contents");

			}

			if (isReady) {

				if (annotationView.getDarkScreenMode()) {
					annotationView.setDarkScreenMode(false);
					dlAnimView.setVisibility(View.INVISIBLE);
					dlAnimSmallView.setVisibility(View.VISIBLE);
					dlAnimSmallView.bringToFront();

					if (progressSync.getVisibility() != View.VISIBLE) {
						progressSync.setVisibility(View.INVISIBLE);
					}

				}
			}

		} else {

			if (progressSync.getProgress() < 1000) {
				progressSync.setProgress(1000);
				progressSync.setVisibility(View.INVISIBLE);

				if (annotationView.getDarkScreenMode()) {
					annotationView.setDarkScreenMode(false);
				}

				if (dlAnimView != null) {
					dlAnimView.setVisibility(View.INVISIBLE);
				}

				if (dlAnimSmallView != null) {
					dlAnimSmallView.setVisibility(View.INVISIBLE);
				}

			}
		}

	}

	@Override
	public void onTrackingStarted(int imageWidth, int imageHeight) {
		// Empty - not needed
	}

	@Override
	public void onEnterContext(VDARContext context) {
		Log.v(TAG,"Context "+context+" detected.");
	}

	@Override
	public void onExitContext(VDARContext context) {
		Log.v(TAG,"Context "+context+" lost.");
	}
}
