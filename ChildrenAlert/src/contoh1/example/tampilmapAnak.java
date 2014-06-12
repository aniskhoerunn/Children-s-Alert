package contoh1.example;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import contoh1.example.tampilmap.select;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class tampilmapAnak extends FragmentActivity implements
	ConnectionCallbacks, OnConnectionFailedListener, LocationListener {
	private GoogleMap mMap;
	UserSessionManager session;
	public boolean view = true;
	private LocationClient mLocationClient;
	double konvertStatusAnak;

	String name, password, phone, statusAlert;
	String nama;
	String lati;
	String longe;

	// JSON parser class
	JSONParser jsonParser = new JSONParser();

	private static final String url_insert_lokasi = "http://192.168.137.201/CobaProject/insertLocation.php";
	private static final String url_view_alert = "http://192.168.137.201/CobaProject/selectAlert.php";

	// JSON Node names
	private static final String TAG_SUCCESS = "success";
	private static final String TAG_PRODUCT = "products";
	String latIbuku, longIbuku, latAnakku, longAnakku;
	double latIbu, longIbu, latAnak, longAnak, distance, jar;
	ArrayList<HashMap<String, String>> productsList;
	private boolean backPressedToExitOnce = false;
	private Toast toast = null;
	
	final private static int DIALOG_ALARM = 1;

	// products JSONArray
	JSONArray products = null;

	private String myLat = null; // ** Declare myLat
	private String myLon = null; // ** Declare myLon
	String kode_anake, jarak, jarakaman2;
	InputStream is = null;
	String result = null;
	String line = null;
	int kode;

	ScheduledExecutorService scheduleTaskExecutor2 = Executors
			.newScheduledThreadPool(5);
	ScheduledExecutorService scheduleTaskExecutor = Executors
			.newScheduledThreadPool(5);

	static PendingIntent pendingIntent;
	private NetworkHandler networkHandler = new NetworkHandler(this);
	static AlarmManager alarmManager;

	private static final LocationRequest REQUEST = LocationRequest.create()
			.setInterval(5000) // 5 seconds
			.setFastestInterval(16) // 16ms = 60fps
			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapanak);
		
		if (!networkHandler.isNetworkEnabled()) {
			networkHandler.showConnectionSettingsAlert();
		} else {
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
				showGPSDisabledAlertToUser();
				//finish();
	        }
		}
		
    	session = new UserSessionManager(getApplicationContext());
		HashMap<String, String> user = session.getUserDetails();
		name = user.get(UserSessionManager.KEY_NAME);

		productsList = new ArrayList<HashMap<String, String>>();

		Intent intentsOpen = new Intent(this, AlarmReceiver.class);
		intentsOpen.setAction("com.manish.alarm.ACTION");
		pendingIntent = PendingIntent.getBroadcast(this, 111, intentsOpen, 0);
		alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		try {
			// Loading map
			initializeMap();
			session = new UserSessionManager(getApplicationContext());

			Button logout = (Button) findViewById(R.id.btnLogout);

			// This schedule a runnable task every 5 second
			scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
				public void run() {
					runOnUiThread(new Runnable() {
						public void run() {
							new insertLokasi().execute();
							//new selectAlertstatus().execute();
						}
					});
				}
			}, 0, 5, TimeUnit.SECONDS);
			
			scheduleTaskExecutor2.scheduleAtFixedRate(new Runnable() {
				public void run() {
					runOnUiThread(new Runnable() {
						public void run() {
							//new insertLokasi().execute();
							new selectAlertstatus().execute();
							if (konvertStatusAnak == 1){
								//Toast.makeText(getBaseContext(), statusAlert, Toast.LENGTH_LONG).show();
								showDialog(DIALOG_ALARM);
								fireAlarm();
							}else {
								stopAlarm();
								//scheduleTaskExecutor2.shutdown();
							}
						}
					});
				}
			}, 0, 8, TimeUnit.SECONDS);

			// showDialog(DIALOG_HITUNG_JARAK);

			logout.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					session.logoutUser();
					Intent MyIntent = new Intent(v.getContext(),
							LoginActivity.class);
					startActivityForResult(MyIntent, 0);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void showGPSDisabledAlertToUser(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS tidak aktif. Aktifkan terlebih dahulu!")
        .setCancelable(false)
        .setPositiveButton("Setting",new DialogInterface.OnClickListener(){
            
        	public void onClick(DialogInterface dialog, int id){
                Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(callGPSSettingIntent);
            }
        });
        
        alertDialogBuilder.setNegativeButton("Cancel",
            new DialogInterface.OnClickListener(){
	            public void onClick(DialogInterface dialog, int id){
	                dialog.cancel();
	            }
        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }
	
	@Override
    protected Dialog onCreateDialog(int id) {
    	// User Session Manager
        
     AlertDialog dialogDetails = null;

     switch (id) {
     case DIALOG_ALARM:
 		LayoutInflater inflaterJoin = LayoutInflater.from(this);
 		View dialogviewJoin = inflaterJoin.inflate(R.layout.alarm_anak, null);

 		AlertDialog.Builder dialogbuilderJoin = new AlertDialog.Builder(this);
 		dialogbuilderJoin.setTitle("AWAS!");
 		dialogbuilderJoin.setView(dialogviewJoin);
 		dialogDetails = dialogbuilderJoin.create();
 		break;
     }

     return dialogDetails;
    }
	
	@Override
    protected void onPrepareDialog(int id, Dialog dialog) {

     switch (id) {
	case DIALOG_ALARM:
		final AlertDialog alertDialogAlarm = (AlertDialog) dialog;
		Button matikan = (Button) alertDialogAlarm.findViewById(R.id.btn_off);
		matikan.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stopAlarm();
				alertDialogAlarm.dismiss();
			}
		});
		break;
     }
    }

	@Override
	public void onBackPressed() {
		if (backPressedToExitOnce) {
			finish();
			super.onBackPressed();
		} else {
			this.backPressedToExitOnce = true;
			showToast("Press again to exit");
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					backPressedToExitOnce = false;
				}
			}, 2000);
		}
	}

	private void showToast(String message) {
		if (this.toast == null) {
			// Create toast if found null, it would he the case of first call
			// only
			this.toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);

		} else if (this.toast.getView() == null) {
			// Toast not showing, so create new one
			this.toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);

		} else {
			// Updating toast message is showing
			this.toast.setText(message);
		}

		// Showing toast finally
		this.toast.show();
	}

	private void killToast() {
		if (this.toast != null) {
			this.toast.cancel();
		}
	}

	public void fireAlarm() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 5000, pendingIntent);
	}

	public void stopAlarm() {
		alarmManager.cancel(pendingIntent);
	}
	
	class selectAlertstatus extends AsyncTask<String, String, String> {
		protected String doInBackground(String... params) {

			ArrayList<NameValuePair> param = new ArrayList<NameValuePair>();
			param.add(new BasicNameValuePair("namane", name));

			JSONObject json = jsonParser.makeHttpRequest(url_view_alert, "GET", param);

			Log.d("Status Alert: ", json.toString());

			try {
				int success = json.getInt(TAG_SUCCESS);
				if (success == 1) {
					// successfully received product details
					JSONArray productObj = json.getJSONArray(TAG_PRODUCT); // JSON
					
					JSONObject product = productObj.getJSONObject(0);

					// display product data in EditText
					statusAlert = (product.getString("alertnya"));

					konvertStatusAnak = Double.valueOf(statusAlert).doubleValue();
					
				} else {
					// product with pid not found
					stopAlarm();
				}
			} catch (JSONException e) {
				e.printStackTrace();
			} 
			return null;
		}

		protected void onPostExecute(String file_url) {
			
		}

	}

	class insertLokasi extends AsyncTask<String, String, String> {
		protected String doInBackground(String... args) {
			session = new UserSessionManager(getApplicationContext());

			HashMap<String, String> user = session.getUserDetails();
			name = user.get(UserSessionManager.KEY_NAME);

			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();

			params.add(new BasicNameValuePair("namane", name));
			params.add(new BasicNameValuePair("latitude", myLat));
			params.add(new BasicNameValuePair("longitude", myLon));

			JSONObject json = jsonParser.makeHttpRequest(url_insert_lokasi,
					"POST", params);

			Log.d("Create Response", json.toString());

			try {
				int success = json.getInt(TAG_SUCCESS);
				if (success == 1) {
					// finish();
				} else {
					// failed to create product
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}

		protected void onPostExecute(String file_url) {
			
		}
	}

	private void initializeMap() {
		if (mMap == null) {
			// Try to obtain the map from the SupportMapFragment.
			mMap = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.mapAnak)).getMap();

			if (mMap != null) {
				Log.e("Sukses", "sukses");
				// Menampilkan tombol my location pada peta
				mMap.setMyLocationEnabled(true);
			}
		}
	}

	@Override
	protected void onResume() {
		view = false;
		super.onResume();
		initializeMap();
		setUpLocationClientIfNeeded();
		mLocationClient.connect();
	}

	@Override
	public void onPause() {
		killToast();
		super.onPause();
		if (mLocationClient != null) {
			mLocationClient.disconnect();
		}
	}

	// jika lokasi belum didapat maka return lokasi
	private void setUpLocationClientIfNeeded() {
		if (mLocationClient == null) {
			mLocationClient = new LocationClient(getApplicationContext(), this, // ConnectionCallbacks
					this); // OnConnectionFailedListener
		}
	}

	// method ketika lokasi user berubah
	@Override
	public void onLocationChanged(Location location) {
		double myLatDouble = location.getLatitude();
		myLat = Double.toString(myLatDouble);
		double myLonDouble = location.getLongitude();
		myLon = Double.toString(myLonDouble);

		LatLng lokasi = new LatLng(myLatDouble, myLonDouble);
		//String x = String.valueOf(lokasi);

		// Toast.makeText(getBaseContext(), x, Toast.LENGTH_SHORT).show();
		// setting lokasi, zoom, dan animasi kamera
		CameraPosition cameraPosition = new CameraPosition.Builder()
				.target(lokasi) // Sets the center of the map to Mountain View
				.zoom(18) // Sets the zoom
				.bearing(90) // Sets the orientation of the camera to east
				.tilt(30) // Sets the tilt of the camera to 30 degrees
				.build(); // Creates a CameraPosition from the builder

		if (view == false) {
			// animasi kamera langsung ke posisi user
			mMap.animateCamera(CameraUpdateFactory
					.newCameraPosition(cameraPosition));
		}
	}

	public void onConnected(Bundle connectionHint) {
		mLocationClient.requestLocationUpdates(REQUEST, this); // LocationListener
	}

	@Override
	public void onDisconnected() {
		// Do nothing
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// Do nothing
		Toast.makeText(getBaseContext(), "Koneksi gagal", Toast.LENGTH_SHORT)
				.show();
	}

}
