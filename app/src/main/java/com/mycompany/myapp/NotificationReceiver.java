package com.mycompany.myapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class NotificationReceiver extends BroadcastReceiver {

	/**
	 * This method is called when a notification is received.
	 * 
	 * @param message
	 *            The message entered in PixLive Maker
	 * @param nid
	 *            The notification identifier that should be passed to the Model
	 *            Manager
	 * @param extras
	 *            The bundle settings
	 */
	private void notificationReceived(Context context, String message,
			String nid, Bundle extras) {
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				new Intent(context, ARActivity.class)
						.putExtra("nid", nid).putExtra("remote",true), PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				context).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("Notification from AR App")
				.setContentText(message).setContentIntent(contentIntent).setAutoCancel(true);

		//For setting the light on
		mBuilder.setLights(0xFF23E223, 200, 100).setPriority(NotificationCompat.PRIORITY_MAX);
		
		mBuilder.setSound(RingtoneManager
				.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

		mNotificationManager.notify(5, mBuilder.build());
	}

	/************************* DO NOT MODIFY AFTER THIS LINE ***************************/

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/** You should not modify this method */
	@Override
	public void onReceive(Context context, Intent intent) {

		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);

		// The getMessageType() intent parameter must be the intent you received
		// in your BroadcastReceiver.
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) { // has effect of unparcelling Bundle
			/*
			 * Filter messages based on message type. Since it is likely that
			 * GCM will be extended in the future with new message types, just
			 * ignore any message types you're not interested in, or that you
			 * don't recognize.
			 */
			if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				String message = extras.getString("message");
				String nid = extras.getString("nid");
				notificationReceived(context, message, nid, extras);
			}
		}

	}


}