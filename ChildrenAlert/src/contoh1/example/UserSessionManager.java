package contoh1.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;

public class UserSessionManager {

	SharedPreferences pref;
	Editor editor;
	double iniIbu;
	Context _context;
	int PRIVATE_MODE = 0;
	JSONParser jParser = new JSONParser();

	// single product url
	private static final String url_cekIbuAnak = "http://192.168.43.97/CobaProject/selectIbu.php";
	private static final String PREFER_NAME = "AndroidExamplePref";
	private static final String TAG_SUCCESS = "success";
	private static final String TAG_PRODUCT = "products";
	private static final String TAG_NAME = "kdIbunya";
	String namaDatabase, namaLokal, cekIbuOrAnak;
	JSONArray productObj = null;

	private static final String IS_USER_LOGIN = "IsUserLoggedIn";

    //private NetworkHandler networkHandler = new NetworkHandler();

	public static final String KEY_NAME = "name";
	public static final String KEY_PASSWORD = "pass";
	public static final String KEY_PHONE = "phone";
	public static final String KEY_JARAK = "jarake";
	public static final String KEY_IBUANAK = "ibu_anak";

	// Constructor
	public UserSessionManager(Context context) {
		this._context = context;
		pref = _context.getSharedPreferences(PREFER_NAME, PRIVATE_MODE);
		editor = pref.edit();
	}

	// Create login session
	public void createUserLoginSession(String name, String pass, String phone,
			String jarake, String ibu_anak) {
		// Storing login value as TRUE
		editor.putBoolean(IS_USER_LOGIN, true);

		// Storing name in pref
		editor.putString(KEY_NAME, name);
		editor.putString(KEY_PASSWORD, pass);
		editor.putString(KEY_PHONE, phone);
		editor.putString(KEY_JARAK, jarake);
		editor.putString(KEY_IBUANAK, ibu_anak);

		// commit changes
		editor.commit();
	}
	
	public boolean checkLogin() {
		if (!this.isUserLoggedIn()) {
			Intent i = new Intent(_context, LoginActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			//finish();
			_context.startActivity(i);
			return true;
		} else {
			HashMap<String, String> user = this.getUserDetails();
			namaLokal = user.get(UserSessionManager.KEY_NAME);
			cekIbuOrAnak = user.get(UserSessionManager.KEY_IBUANAK);
			
			iniIbu = Double.valueOf(cekIbuOrAnak).doubleValue();
			
			if (iniIbu==1) {
				Intent i = new Intent(_context, tampilmap.class);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				//finish();
				_context.startActivity(i);
            } else if (iniIbu==0) {
            	Intent i = new Intent(_context, tampilmapAnak.class);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				//finish();
				_context.startActivity(i);
            }
			
		}
		return false;
	}

	public HashMap<String, String> getUserDetails() {
		HashMap<String, String> user = new HashMap<String, String>();
		user.put(KEY_NAME, pref.getString(KEY_NAME, null));
		user.put(KEY_PASSWORD, pref.getString(KEY_PASSWORD, null));
		user.put(KEY_PHONE, pref.getString(KEY_PHONE, null));
		user.put(KEY_JARAK, pref.getString(KEY_JARAK, null));
		user.put(KEY_IBUANAK, pref.getString(KEY_IBUANAK, null));

		return user;
	}

	public void logoutUser() {
		editor.clear();
		editor.commit();

		Intent i = new Intent(_context, LoginActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		//finish();
		_context.startActivity(i);
	}

	public boolean isUserLoggedIn() {
		return pref.getBoolean(IS_USER_LOGIN, false);
	}
	
	class CekIbuAnak extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
 
        /**
         * getting All products from url
         * */
        protected String doInBackground(String... args) {
            // Building Parameters
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("namane", namaLokal));
            
            // getting JSON string from URL
            JSONObject json = jParser.makeHttpRequest(url_cekIbuAnak, "GET", params);
 
            // Check your log cat for JSON reponse
            Log.d("All Productsnyaaaaaaaaa: ", json.toString());
 
            try {
                // Checking for SUCCESS TAG
                int success = json.getInt(TAG_SUCCESS);
 
                if (success == 1) {
                    productObj = json.getJSONArray(TAG_PRODUCT);
                    JSONObject product = productObj.getJSONObject(0);
                    namaDatabase = product.getString(TAG_NAME);

					iniIbu = Double.valueOf(namaDatabase).doubleValue();
                    
                } else if(success == 0) {
                	iniIbu = 0;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
 
        /**
         * After completing background task Dismiss the progress dialog
         * **/
        protected void onPostExecute(String file_url) {
        	if (iniIbu==1) {
				Intent i = new Intent(_context, tampilmap.class);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				//finish();
				_context.startActivity(i);
            } else if (iniIbu==0) {
            	Intent i = new Intent(_context, tampilmapAnak.class);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				//finish();
				_context.startActivity(i);
            }
        }
    }
}
