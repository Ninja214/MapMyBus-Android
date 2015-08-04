package com.mapmybus.newmapmybus;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

public class CoordinatesActivity extends FragmentActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {

	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private LocationClient mLocationClient;
	private Location mCurrentLocation;
	private TextView responseTextView;
	private Button startButton;
	private Handler locationHandler;
	private boolean trackingStarted;
	private long timeDelayed = 10000;
	private int busId;
	private String busName, departmentName, organizationName;
	private Intent serviceIntent;

	@Override
	protected void onStart() {
		super.onStart();
		// Connect the client.
		mLocationClient.connect();
		serviceIntent = new Intent(this, TrackingService.class);
	}

	@SuppressLint("NewApi") @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.coordinates_layout);

		busId = getIntent().getIntExtra("busid", 0);
		busName = getIntent().getStringExtra("busname");
		departmentName = getIntent().getStringExtra("departmentname");
		organizationName = getIntent().getStringExtra("organizationname");
		saveToPreferences(); // Save the data for later.
		
		if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setTitle(organizationName + " - " + departmentName + " - " + busName);
		}

		locationHandler = new Handler();

		responseTextView = (TextView) findViewById(R.id.responseTextView);

		startButton = (Button) findViewById(R.id.startButton);
		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!trackingStarted) {
					locationHandler.post(locationGetter);
					trackingStarted = true;
					startButton.setText("Stop tracking the bus!");
					if(serviceIntent != null) {
						serviceIntent.putExtra("busId", busId);
						startService(serviceIntent);
					}
				} else {
					locationHandler.removeCallbacks(locationGetter);
					trackingStarted = false;
					startButton.setText("Start tracking the bus!");
					if(serviceIntent != null) {
						stopService(serviceIntent);
					}
				}
			}
		});
		mLocationClient = new LocationClient(this, this, this);
		
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			showGPSDisabledAlertToUser();	
		}
	}
	
	@Override
	protected void onResume() {
		SharedPreferences prefs = getSharedPreferences("MapMyBus", MODE_PRIVATE);
//		if(busName == null) {
			busName = prefs.getString("busName", null);
//		}
//		if(departmentName == null) {
			departmentName = prefs.getString("departmentName", null);
//		}
//		if(organizationName == null) {
			organizationName = prefs.getString("organizationName", null);
//		}
		Toast.makeText(this, "BusId is:  " + busId, Toast.LENGTH_SHORT).show();
		if(busId < 1) {
			busId = prefs.getInt("busId", -1);
		}
		if(busId == -1) {
			Toast.makeText(this, "Something went wrong. Please restart the application. BusId was: " + busId, Toast.LENGTH_LONG).show();
			stopService(serviceIntent);
			finish();
		}
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		saveToPreferences();
		super.onPause();
	}

	private void saveToPreferences() {
		SharedPreferences.Editor editor = getSharedPreferences("MapMyBus", MODE_PRIVATE).edit();
		editor.putString("busName", busName);
		editor.putString("departmentName", departmentName);
		editor.putString("organizationName", organizationName);
		editor.putInt("busId", busId);
		editor.commit();
	}
	
	@Override
	protected void onStop() {
		// Disconnecting the client invalidates it.
		mLocationClient.disconnect();
		super.onStop();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Decide what to do based on the original request code
		switch (requestCode) {
		case CONNECTION_FAILURE_RESOLUTION_REQUEST:
			/*
			 * If the result code is Activity.RESULT_OK, try to connect again
			 */
			switch (resultCode) {
			case Activity.RESULT_OK:
				/*
				 * Try the request again
				 */
				break;
			}
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this,
						CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			/*
			 * If no resolution is available, display a dialog to the user with
			 * the error.
			 */
			ErrorDialogFragment.instantiate(this,
					"" + connectionResult.getErrorCode());
		}
	}

	private Runnable locationGetter = new Runnable() {
		@Override
		public void run() {
			if (mLocationClient.isConnected()) {
				mCurrentLocation = mLocationClient.getLastLocation();
				if (mCurrentLocation != null) {
					try {
						sendLocationToServer(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					responseTextView.setTextColor(Color.RED);
					String text = responseTextView.getText().toString();
					text += "No GPS coordinates - check your settings and try again.\n";
					responseTextView.setText(text);
				}
			}
			locationHandler.postDelayed(locationGetter, timeDelayed);
		}
	};

	private void sendLocationToServer(final double latitude,
			final double longitude) throws JSONException {
		RequestQueue queue = Volley.newRequestQueue(this);

		String url = "http://188.121.50.210:8080/MapMyBus/webresources/com.mapmybus.entities.coordinates";

		// busid: 1
		// longitude: 12.21
		// latitude: 23.21
		// time: 1379513311000

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("busId", busId);
		jsonObject.put("longitude", longitude);
		jsonObject.put("latitude", latitude);

		JsonObjectRequest jsObjRequest = new JsonObjectRequest(
			Request.Method.POST, url, jsonObject,
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						responseTextView.setTextColor(Color.GREEN);
						String textBefore = responseTextView.getText().toString();
						responseTextView.setText(textBefore + new Date() + " - All sent ok!\n");
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						String wrongException = "org.json.JSONException: End of input at character 0 of"; 
						if(error.getMessage().startsWith(wrongException)) {
							responseTextView.setTextColor(Color.GREEN);
							String textBefore = responseTextView.getText().toString();
							responseTextView.setText(textBefore + new Date() + " - All sent ok!\n");
						} else {
							// Something went wrong...
							responseTextView.setTextColor(Color.RED);
							String textBefore = responseTextView.getText().toString();
							responseTextView.setText(textBefore + new Date() + " - Something went wrong - please restart the application and check your Internet connection!\n");
						}
					}
				});
			queue.add(jsObjRequest);
			queue.start();
	}

	@Override
	public void onConnected(Bundle dataBundle) {
		// Display the connection status
		// Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
		// if (mLocationClient.isConnected()) {
		// mCurrentLocation = mLocationClient.getLastLocation();
		// locationString += DateFormat.format("dd-MMM-yyyy hh:mm:ss", new
		// Date()) + " - location: " + mCurrentLocation.getLatitude() + ", " +
		// mCurrentLocation.getLongitude();
		// Toast.makeText(this, "Location: " + mCurrentLocation.getLatitude() +
		// ", " + mCurrentLocation.getLongitude(), Toast.LENGTH_LONG).show();
		// locationTextView.setText(locationString);
		// } else {
		// Toast.makeText(this, "LocationClient not connected...",
		// Toast.LENGTH_LONG).show();
		// }
		// locationHandler.postDelayed(locationGetter, 5000);
	}

	@Override
	public void onDisconnected() {
		// Display the connection status
		Toast.makeText(this, "Disconnected. Please re-connect.",
				Toast.LENGTH_SHORT).show();
	}

	public static class ErrorDialogFragment extends DialogFragment {
		// Global field to contain the error dialog
		private Dialog mDialog;

		// Default constructor. Sets the dialog field to null
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}

		// Set the dialog to display
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}

		// Return a Dialog to the DialogFragment.
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}

	private void showGPSDisabledAlertToUser() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder
				.setMessage("GPS is disabled.")
				.setCancelable(false)
				.setPositiveButton("Settings",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
								startActivity(callGPSSettingIntent);
							}
						});
		alertDialogBuilder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = alertDialogBuilder.create();
		alert.show();
	}
}