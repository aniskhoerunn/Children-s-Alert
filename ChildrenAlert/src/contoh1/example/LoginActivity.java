package contoh1.example;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {

	Button btnRegistrasi;
	Button btnJoin;
	final private static int DIALOG_LOGIN = 1;
	final private static int DIALOG_JOIN = 2;
	private ProgressDialog pDialog;
	private static final String url_register = "http://192.168.137.201/CobaProject/insert.php";
	private static final String url_join = "http://192.168.137.201/CobaProject/join.php";
	JSONParser jsonParser = new JSONParser();
	private boolean backPressedToExitOnce = false;
	private Toast toast = null;
	String name, pass, phone, code_anaknya, name_anak;
	InputStream is = null;
	String result = null;
	String line = null;
	int kode;

	// Class User Session Manager
	UserSessionManager session;
    private NetworkHandler networkHandler = new NetworkHandler(this);

	private static final String TAG_SUCCESS = "success";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		
		if (!networkHandler.isNetworkEnabled()) {
			networkHandler.showConnectionSettingsAlert();
		} else {
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
				showGPSDisabledAlertToUser();
				//finish();
	        }else{
	        	Toast.makeText(this, "GPS is Enabled in your devide", Toast.LENGTH_SHORT).show();
	        }
		}

		btnRegistrasi = (Button) findViewById(R.id.btnDaftar);
		btnRegistrasi.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				showDialog(DIALOG_LOGIN);
			}
		});

		btnJoin = (Button) findViewById(R.id.btnJoin);
		btnJoin.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				showDialog(DIALOG_JOIN);
			}
		});
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

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog dialogDetails = null;
		switch (id) {
		case DIALOG_LOGIN:
			LayoutInflater inflater = LayoutInflater.from(this);
			View dialogview = inflater.inflate(R.layout.logindialog, null);

			AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(this);
			dialogbuilder.setTitle("DAFTAR");
			dialogbuilder.setView(dialogview);
			dialogDetails = dialogbuilder.create();
			break;

		case DIALOG_JOIN:
			LayoutInflater inflaterJoin = LayoutInflater.from(this);
			View dialogviewJoin = inflaterJoin.inflate(R.layout.joindialog,
					null);

			AlertDialog.Builder dialogbuilderJoin = new AlertDialog.Builder(
					this);
			dialogbuilderJoin.setTitle("HUBUNG");
			dialogbuilderJoin.setView(dialogviewJoin);
			dialogDetails = dialogbuilderJoin.create();
			break;
		}
		return dialogDetails;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {

		switch (id) {
		case DIALOG_LOGIN:
			session = new UserSessionManager(getApplicationContext());

			final AlertDialog alertDialog = (AlertDialog) dialog;
			Button loginbutton = (Button) alertDialog
					.findViewById(R.id.btn_login);
			Button cancelbutton = (Button) alertDialog
					.findViewById(R.id.btn_cancel);

			final EditText userName = (EditText) alertDialog
					.findViewById(R.id.txtUsername);
			final EditText password = (EditText) alertDialog
					.findViewById(R.id.txtPassword);
			final EditText hp = (EditText) alertDialog
					.findViewById(R.id.txtPhone);

			loginbutton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
						// Ambil username, password, phonenumber
						name = userName.getText().toString();
						pass = password.getText().toString();
						phone = hp.getText().toString();

						new insert().execute();
		           
					
				}

				class insert extends AsyncTask<String, String, String> {
					@Override
					protected void onPreExecute() {
						super.onPreExecute();
						pDialog = new ProgressDialog(LoginActivity.this);
						pDialog.setMessage("Membuat akun..");
						pDialog.setIndeterminate(false);
						pDialog.setCancelable(true);
						pDialog.show();
					}

					protected String doInBackground(String... args) {
						List<NameValuePair> params = new ArrayList<NameValuePair>();

						params.add(new BasicNameValuePair("namane", name));
						params.add(new BasicNameValuePair("passw", pass));
						params.add(new BasicNameValuePair("telpon", phone));

						JSONObject json = jsonParser.makeHttpRequest(
								url_register, "POST", params);

						Log.d("Create Response", json.toString());

						// check for success tag
						try {
							int success = json.getInt(TAG_SUCCESS);

							if (success == 1) {
								session.createUserLoginSession(name, pass,
										phone, "","1");
								Intent i = new Intent(getApplicationContext(),
										tambahanak.class);
								i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(i);
								alertDialog.dismiss();
								finish();
							} 
						} catch (JSONException e) {
							e.printStackTrace();
						}
						return null;
					}

					protected void onPostExecute(String file_url) {
						pDialog.dismiss();
					}
				}
			});

			cancelbutton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					alertDialog.dismiss();
				}
			});
			break;

		case DIALOG_JOIN:
			session = new UserSessionManager(getApplicationContext());

			final AlertDialog alertDialogJoin = (AlertDialog) dialog;
			Button joinbutton = (Button) alertDialogJoin
					.findViewById(R.id.btn_join);
			Button canceljoinbutton = (Button) alertDialogJoin
					.findViewById(R.id.btn_cancel);

			final EditText kode_anaknya = (EditText) alertDialogJoin
					.findViewById(R.id.txtKodeName);

			joinbutton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					// Get username, password from EditText
					name = kode_anaknya.getText().toString();
					new update().execute();
				}

				class update extends AsyncTask<String, String, String> {
					/**
					 * Before starting background thread Show Progress Dialog
					 * */
					@Override
					protected void onPreExecute() {
						super.onPreExecute();
						pDialog = new ProgressDialog(LoginActivity.this);
						pDialog.setMessage("Menghubungkan dengan ibu..");
						pDialog.setIndeterminate(false);
						pDialog.setCancelable(true);
						pDialog.show();
					}

					/**
					 * Creating product
					 * */
					protected String doInBackground(String... args) {
						List<NameValuePair> params = new ArrayList<NameValuePair>();

						params.add(new BasicNameValuePair("namaAnake", name));

						// getting JSON Object
						// Note that create product url accepts POST method
						JSONObject json = jsonParser.makeHttpRequest(url_join,
								"POST", params);

						// check log cat fro response
						Log.d("Create Response", json.toString());

						// check for success tag
						try {
							int success = json.getInt(TAG_SUCCESS);

							if (success == 1) {
								session.createUserLoginSession(name, "", "", "", "0");
								// closing this screen
								finish();
								alertDialogJoin.dismiss();
							} else {
								// failed to create product
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
						return null;
					}

					/**
					 * After completing background task Dismiss the progress
					 * dialog
					 * **/
					protected void onPostExecute(String file_url) {
						// dismiss the dialog once done
						Intent i = new Intent(getApplicationContext(),
								tampilmapAnak.class);
						i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

						// Add new Flag to start new Activity
						i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(i);
						setResult(100, i);

						pDialog.dismiss();
						finish();
					}
				}
			});

			canceljoinbutton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					alertDialogJoin.dismiss();
				}
			});
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
