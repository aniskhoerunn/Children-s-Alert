package contoh1.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
 
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

 
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
 
public class LoadAnak extends ListActivity {

    // Progress Dialog
	String kode_anak;
	final private static int DIALOG_INPUT_JARAK = 1;
    private ProgressDialog pDialog;
    UserSessionManager session;
    // Creating JSON Parser object
    JSONParser jParser = new JSONParser();
    String name, phone, pass, ibuanak;
    ArrayList<HashMap<String, String>> productsList;
    ScheduledExecutorService scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
	
 
    // url to get all products list
    private static final String url_connect = "http://192.168.137.201/CobaProject/connect.php";
    private static String url_view_anak = "http://192.168.137.201/CobaProject/selectAnak.php";
 
    // JSON Node names
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_PRODUCTS = "products";
    private static final String TAG_KODE = "kdAnaknya";
    private static final String TAG_NAME = "nmAnaknya";
    private static final String TAG_JARAK = "jaraknya";
 
    // products JSONArray
    JSONArray products = null;
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.all_products);
        
        session = new UserSessionManager(getApplicationContext());
		HashMap<String, String> user = session.getUserDetails();
	    name = user.get(UserSessionManager.KEY_NAME);
	    pass = user.get(UserSessionManager.KEY_PASSWORD);
	    phone = user.get(UserSessionManager.KEY_PHONE);
	    ibuanak = user.get(UserSessionManager.KEY_IBUANAK);
	    
 
        // Hashmap for ListView
        productsList = new ArrayList<HashMap<String, String>>();
 
        // Loading products in Background Thread
     // This schedule a runnable task every 5 second
     			scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
     				public void run() {
     					runOnUiThread(new Runnable() {
     						public void run() {
     							new LoadAllProducts().execute();
     						}
     					});
     				}
     			}, 0, 5, TimeUnit.SECONDS);
        //
 
        // Get listview
        ListView lv = getListView();
 
        // on selecting single product
        // launching Edit Product Screen
        lv.setOnItemClickListener(new OnItemClickListener() {
 
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // getting values from selected ListItem

         	   kode_anak = ((TextView) view.findViewById(R.id.pid)).getText().toString();
        	    showDialog(DIALOG_INPUT_JARAK);
            }
        });
 
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	// User Session Manager
        
     AlertDialog dialogDetails = null;

     switch (id) {
     case DIALOG_INPUT_JARAK:
      LayoutInflater inflater = LayoutInflater.from(this);
      View dialogview = inflater.inflate(R.layout.dialoginputjarak, null);

      AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(this);
      dialogbuilder.setTitle("MASUKKAN JARAK");
      dialogbuilder.setView(dialogview);
      dialogDetails = dialogbuilder.create();

      break;
     }

     return dialogDetails;
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {

     switch (id) {
     case DIALOG_INPUT_JARAK:
    	 
	     final AlertDialog alertDialog = (AlertDialog) dialog;
	     
	     Button button_jarak = (Button) alertDialog.findViewById(R.id.btnOk);
	     Button cancelbutton = (Button) alertDialog.findViewById(R.id.btn_cancel);
	     
	     final EditText jarak_aman = (EditText) alertDialog.findViewById(R.id.txtJarak);
	     
      button_jarak.setOnClickListener(new View.OnClickListener() {

       @Override
   	    public void onClick(View v) {
    	   // Get username, password from EditText
           String jarak_amannya = jarak_aman.getText().toString();
           
           session.createUserLoginSession(name, pass, phone,jarak_amannya,ibuanak);
           
           new connect().execute();
           scheduleTaskExecutor.shutdown();
        // 1. create an intent pass class name or intnet action name 
           Intent intent = new Intent(v.getContext(), tampilmap.class);
           intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
           intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
           intent.putExtra(TAG_JARAK, jarak_amannya);
           finish();
           startActivityForResult(intent, 100);
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
     }
    }
 
    // Response from Edit Product Activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // if result code 100
        if (resultCode == 100) {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
 
    }
    
    class connect extends AsyncTask<String, String, String> {
        protected String doInBackground(String... args) {
        	
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("namane", name));
 
            JSONObject json = jParser.makeHttpRequest(url_connect,"POST", params);
 
            // check json success tag
            try {
                int success = json.getInt(TAG_SUCCESS);
 
                if (success == 1) {
                    // successfully updated
                    Intent i = getIntent();
                    // send result code 100 to notify about product update
                    setResult(100, i);
                    finish();
                } else {
                    // failed to update product
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
 
            return null;
        }
    }
 
    class LoadAllProducts extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(LoadAnak.this);
            pDialog.setMessage("Sedang menghubungkan. Mohon tunggu...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }
 
        /**
         * getting All products from url
         * */
        protected String doInBackground(String... args) {
            // Building Parameters
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("namane", name));
            
            // getting JSON string from URL
            JSONObject json = jParser.makeHttpRequest(url_view_anak, "GET", params);
 
            // Check your log cat for JSON reponse
            Log.d("All Products: ", json.toString());
 
            try {
                // Checking for SUCCESS TAG
                int success = json.getInt(TAG_SUCCESS);
 
                if (success == 1) {
                    // products found
                    // Getting Array of Products
                    products = json.getJSONArray(TAG_PRODUCTS);
 
                    // looping through All Products
                    for (int i = 0; i < products.length(); i++) {
                        JSONObject c = products.getJSONObject(i);
 
                        // Storing each json item in variable
                        String kd = c.getString(TAG_KODE);
                        String name = c.getString(TAG_NAME);
 
                        // creating new HashMap
                        HashMap<String, String> map = new HashMap<String, String>();
 
                        // adding each child node to HashMap key => value
                        map.put(TAG_KODE, kd);
                        map.put(TAG_NAME, name);
 
                        // adding HashList to ArrayList
                        productsList.add(map);
                    }
                    scheduleTaskExecutor.shutdown();
                } else {
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
            // dismiss the dialog after getting all products
            pDialog.dismiss();
            // updating UI from Background Thread
            runOnUiThread(new Runnable() {
                public void run() {
                    /**
                     * Updating parsed JSON data into ListView
                     * */
                    ListAdapter adapter = new SimpleAdapter(
                    		LoadAnak.this, productsList,
                            R.layout.list_item, new String[] { TAG_KODE,
                                    TAG_NAME},
                            new int[] { R.id.pid, R.id.name });
                    // updating listview
                    setListAdapter(adapter);
                    
                }
            });
        }
 
    }
}
