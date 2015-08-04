package com.mapmybus.newmapmybus;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class TrackingService extends Service {
	
	@Override
	public IBinder onBind(Intent intent) {
		return null; // Not used...
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(getApplicationContext(), "onStart called!", Toast.LENGTH_SHORT).show();
		
		Notification notification = new Notification(R.drawable.ic_launcher, "App is running...", System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, CoordinatesActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, "Title", "Message...", pendingIntent);
		startForeground(66645, notification);
		
//		return super.onStartCommand(intent, flags, startId);
		return Service.START_STICKY;
	}
	
	@Override
	public void onCreate() {
		Toast.makeText(getApplicationContext(), "onCreate called!", Toast.LENGTH_SHORT).show();
		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Toast.makeText(getApplicationContext(), "onDestroy called!", Toast.LENGTH_SHORT).show();
	}
}
