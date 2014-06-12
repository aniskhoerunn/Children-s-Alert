package contoh1.example;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.PeriodicSync;
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


import java.util.Calendar;

import android.app.AlarmManager;
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
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class tampilmap extends FragmentActivity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {
	private GoogleMap mMap;
	UserSessionManager session;
	public boolean view = true;
	private LocationClient mLocationClient;

	String name, password, phone;
	String nama;
	String lati;
	String longe;

	// JSON parser class
	JSONParser jsonParser = new JSONParser();

	// single product url
	private static final String url_view_data = "http://192.168.137.201/CobaProject/select.php";
	private static final String url_insert_lokasi = "http://192.168.137.201/CobaProject/insertLocation.php";
	private static final String url_alert = "http://192.168.137.201/CobaProject/alertAnak.php";
	private static final String url_ubah_alert = "http://192.168.137.201/CobaProject/alertAnakUbah.php";

	// JSON Node names
	private static final String TAG_SUCCESS = "success";
	private static final String TAG_PRODUCT = "products";
	private static final String TAG_JARAK = "jaraknya";
	private static final String TAG_KODE = "kdAnaknya";
	private static final String TAG_USER = "usere";
	private static final String TAG_LAT = "latite";
	private static final String TAG_LNG = "longie";
	String latIbuku, longIbuku, latAnakku, longAnakku;
	double latIbu, longIbu, latAnak, longAnak, distance, jar;
	ArrayList<HashMap<String, String>> productsList;
	private boolean backPressedToExitOnce = false;
	private Toast toast = null;

	// products JSONArray
	JSONArray products = null;

	private String myLat = null; // ** Declare myLat
	private String myLon = null; // ** Declare myLon
	String kode_anake, jarak, jarakaman2;
	InputStream is = null;
	String result = null;
	String line = null;
	int kode;
	
	ScheduledExecutorService scheduleTaskExecutor2 = Executors.newScheduledThreadPool(5);
	ScheduledExecutorService scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
	final private static int DIALOG_HITUNG_JARAK = 1;
	final private static int DIALOG_ALARM = 2;
	
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
		setContentView(R.layout.map);
		
		if (!networkHandler.isNetworkEnabled()) {
			networkHandler.showConnectionSettingsAlert();
		} else {
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
				showGPSDisabledAlertToUser();
	        } 
		}
		
		session = new UserSessionManager(getApplicationContext());

		HashMap<String, String> user = session.getUserDetails();

		name = user.get(UserSessionManager.KEY_NAME);

		productsList = new ArrayList<HashMap<String, String>>();

		Intent i = getIntent();
		String jarakAman = i.getStringExtra(TAG_JARAK);
		jarakaman2 = user.get(UserSessionManager.KEY_JARAK);
		
		try {
			jar = new Double(jarakaman2);
		} catch (NumberFormatException e) {
			jar=0;
		}
		
		kode_anake = i.getStringExtra(TAG_KODE);
		
		Toast toast = Toast.makeText(this, jarakaman2 + ", " + name, Toast.LENGTH_LONG);
		toast.show();
		
		Intent intentsOpen = new Intent(this, AlarmReceiver.class);
		intentsOpen.setAction("com.manish.alarm.ACTION");
		pendingIntent = PendingIntent.getBroadcast(this,111, intentsOpen, 0);
		alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		
		try {
			// Loading map
			initializeMap();
			session = new UserSessionManager(getApplicationContext());

			Button putuskan = (Button) findViewById(R.id.btnOff);
			putuskan.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					scheduleTaskExecutor2.shutdown();
					finish();

					Intent MyIntent = new Intent(v.getContext(), LoadAnak.class);
					startActivityForResult(MyIntent, 0);
				}
			});

			// This schedule a runnable task every 5 second
			scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
				public void run() {
					runOnUiThread(new Runnable() {
						public void run() {
							new insertLokasi().execute();
						}
					});
				}
			}, 0, 5, TimeUnit.SECONDS);
			
			
			//showDialog(DIALOG_HITUNG_JARAK);
			
			Handler handler = new Handler();

			// Build the dialog
			AlertDialog dialog = new AlertDialog.Builder(this)
			    .setMessage("Tunggu hingga button OK muncul untuk mulai menghitung jarak..")
			    .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			        // the rest of your stuff
			    	scheduleTaskExecutor2.scheduleAtFixedRate(new Runnable() {
						public void run() {
							runOnUiThread(new Runnable() {
								public void run() {
									new select().execute();
								}
							});
						}
					}, 0, 10, TimeUnit.SECONDS);
			    }
			})
			.create();

			dialog.show();

			// Access the button and set it to invisible
			final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
			button.setVisibility(View.INVISIBLE);

			// Post the task to set it visible in 5000ms         
			handler.postDelayed(new Runnable(){
			    @Override
			    public void run() {
			        button.setVisibility(View.VISIBLE); 
			    }}, 20000);
			
			
			Button logout = (Button) findViewById(R.id.btnLogout);
			logout.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					session.logoutUser();
					Intent MyIntent = new Intent(v.getContext(), LoginActivity.class);
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
     case DIALOG_HITUNG_JARAK:
      LayoutInflater inflater = LayoutInflater.from(this);
      View dialogview = inflater.inflate(R.layout.hitungjarak, null);

      AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(this);
      dialogbuilder.setTitle("HITUNG JARAK");
      dialogbuilder.setView(dialogview);
      dialogDetails = dialogbuilder.create();

      break;
      
     case DIALOG_ALARM:
 		LayoutInflater inflaterJoin = LayoutInflater.from(this);
 		View dialogAlarm = inflaterJoin.inflate(R.layout.alarm, null);

 		AlertDialog.Builder dialogbuilderJoin = new AlertDialog.Builder(this);
 		//dialogbuilderJoin.setTitle("AWAS!");
 		dialogbuilderJoin.setView(dialogAlarm);
 		dialogDetails = dialogbuilderJoin.create();
 		break;
     }

     return dialogDetails;
    }
	
	@Override
    protected void onPrepareDialog(int id, Dialog dialog) {

     switch (id) {
     case DIALOG_HITUNG_JARAK:
	     final AlertDialog alertDialog = (AlertDialog) dialog;
	     
	     Button addbutton = (Button) alertDialog.findViewById(R.id.btn_hitung);
	     Button cancelbutton = (Button) alertDialog.findViewById(R.id.btn_cancel);
	    
			addbutton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					// This schedule a runnable task every 5 second
					scheduleTaskExecutor2.scheduleAtFixedRate(new Runnable() {
						public void run() {
							runOnUiThread(new Runnable() {
								public void run() {
									new select().execute();
								}
							});
						}
					}, 0, 10, TimeUnit.SECONDS);
					alertDialog.dismiss();
				}
			});

      cancelbutton.setOnClickListener(new View.OnClickListener() {

       @Override
       public void onClick(View v) {
        alertDialog.dismiss();
       }
      });
      break;
      
	case DIALOG_ALARM:
		final AlertDialog alertDialogAlarm = (AlertDialog) dialog;

		Button matikan = (Button) alertDialogAlarm.findViewById(R.id.btn_off);

		matikan.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stopAlarm();
				scheduleTaskExecutor2.shutdown();
				new pausealarmAnak().execute();
				alertDialogAlarm.dismiss();
				finish();

				Intent MyIntent = new Intent(v.getContext(), LoadAnak.class);
				startActivityForResult(MyIntent, 0);
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
	        // Create toast if found null, it would he the case of first call only
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
		new alarm().execute();
		//alarmManager.notify();
	}
	public void stopAlarm(){
		alarmManager.cancel(pendingIntent);
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
			//
		}
	}
	
	class alarm extends AsyncTask<String, String, String> {
		protected String doInBackground(String... args) {
			session = new UserSessionManager(getApplicationContext());

			HashMap<String, String> user = session.getUserDetails();
			name = user.get(UserSessionManager.KEY_NAME);

			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();

			params.add(new BasicNameValuePair("namane", name));

			JSONObject json = jsonParser.makeHttpRequest(url_alert,
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
			//
		}
	}
	
	class pausealarmAnak extends AsyncTask<String, String, String> {
		protected String doInBackground(String... args) {
			session = new UserSessionManager(getApplicationContext());

			HashMap<String, String> user = session.getUserDetails();
			name = user.get(UserSessionManager.KEY_NAME);

			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();

			params.add(new BasicNameValuePair("namane", name));

			JSONObject json = jsonParser.makeHttpRequest(url_ubah_alert,
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
			//
		}
	}

	class select extends AsyncTask<String, String, String> {
		protected String doInBackground(String... params) {

			ArrayList<NameValuePair> param = new ArrayList<NameValuePair>();
			param.add(new BasicNameValuePair("namane", name));

			JSONObject json = jsonParser.makeHttpRequest(url_view_data, "GET", param);

			Log.d("Data Lokasi", json.toString());

			try {
				//Thread.sleep(7000);
				int success = json.getInt(TAG_SUCCESS);
				if (success == 1) {
					// successfully received product details
					JSONArray productObj = json.getJSONArray(TAG_PRODUCT); // JSON
					
					JSONObject product = productObj.getJSONObject(0);

					// display product data in EditText
					String a = (product.getString(TAG_USER));
					String b = (product.getString(TAG_LAT));
					String c = (product.getString(TAG_LNG));

					JSONObject productnya = productObj.getJSONObject(1);

					String x = (productnya.getString(TAG_USER));
					String y = (productnya.getString(TAG_LAT));
					String z = (productnya.getString(TAG_LNG));

					Double latIbu = Double.valueOf(y).doubleValue();
					Double longIbu = Double.valueOf(z).doubleValue();

					Double latAnak = Double.valueOf(b).doubleValue();
					Double longAnak = Double.valueOf(c).doubleValue();

					LatLng ibu = new LatLng(latIbu, longIbu);
					LatLng anak = new LatLng(latAnak, longAnak);

					Location lokasiA = new Location("lokasi_ibu");
					lokasiA.setLatitude(ibu.latitude);
					lokasiA.setLongitude(ibu.longitude);

					Location lokasiB = new Location("lokasi_anak");
					lokasiB.setLatitude(anak.latitude);
					lokasiB.setLongitude(anak.longitude);

					distance = (int) lokasiA.distanceTo(lokasiB);
					jarak = String.valueOf(distance);
					Log.i("Jarak", jarak);
					
				} else {
					// product with pid not found
				}
			} catch (JSONException e) {
				e.printStackTrace();
			} 
			return null;
		}

		protected void onPostExecute(String file_url) {
			Toast.makeText(getBaseContext(), "Jarak: " + jarak + "meter", Toast.LENGTH_LONG).show();
			if (distance > jar){
				//Toast.makeText(getBaseContext(), "alarm", Toast.LENGTH_SHORT).show();
				showDialog(DIALOG_ALARM);
				fireAlarm();
			}
		}

	}
	
	private void initializeMap() {
		if (mMap == null) {
			// Try to obtain the map from the SupportMapFragment.
			mMap = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();

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
		Toast.makeText(getBaseContext(), "Koneksi gagal", Toast.LENGTH_SHORT).show();
	}

}
