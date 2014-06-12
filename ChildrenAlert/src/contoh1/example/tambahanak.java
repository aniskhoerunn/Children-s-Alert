package contoh1.example;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class tambahanak extends Activity {
	
	final private static int DIALOG_ADD_CHILD = 1;
    private ProgressDialog pDialog;
    ArrayList<HashMap<String, String>> productsList;
    private static final String url_insert = "http://192.168.137.201/CobaProject/insertAnak.php";
    UserSessionManager session;
    JSONParser jsonParser = new JSONParser();
	
	String kdAnak, nmAnak, name;
    InputStream is=null;
	String result=null;
	String line=null;
	int kode;
	
	private static final String TAG_SUCCESS = "success";

    JSONArray products = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tambahanak);
		
		//Button next = (Button) findViewById(R.id.buttoninputjarak);
		Button add = (Button) findViewById(R.id.btnAddChild);
		
		add.setOnClickListener(new View.OnClickListener(){

			@Override
	        public void onClick(View arg0) {
        	    showDialog(DIALOG_ADD_CHILD);
	            }
		});
	}
	
	 @Override
	    protected Dialog onCreateDialog(int id) {
	    	// User Session Manager
	        
	     AlertDialog dialogDetails = null;

	     switch (id) {
	     case DIALOG_ADD_CHILD:
	      LayoutInflater inflater = LayoutInflater.from(this);
	      View dialogview = inflater.inflate(R.layout.formtambahanak, null);

	      AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(this);
	      dialogbuilder.setTitle("TAMBAH ANAK");
	      dialogbuilder.setView(dialogview);
	      dialogDetails = dialogbuilder.create();

	      break;
	     }

	     return dialogDetails;
	    }
	 	
	 @Override
	    protected void onPrepareDialog(int id, Dialog dialog) {

	     switch (id) {
	     case DIALOG_ADD_CHILD:
	    	 
	    	 session = new UserSessionManager(getApplicationContext());
			 HashMap<String, String> user = session.getUserDetails();
		     name = user.get(UserSessionManager.KEY_NAME);
		     
		     final AlertDialog alertDialog = (AlertDialog) dialog;
		     
		     Button addbutton = (Button) alertDialog.findViewById(R.id.btnTambah);
		     Button cancelbutton = (Button) alertDialog.findViewById(R.id.btn_cancel);
		     
		     final EditText kodeAnak = (EditText) alertDialog.findViewById(R.id.txtKdAnak);
		     final EditText namaAnak = (EditText) alertDialog.findViewById(R.id.txtNmAnak);
	      
	      addbutton.setOnClickListener(new View.OnClickListener() {

	       @Override
	   	    public void onClick(View v) {
	    	// Get username, password from EditText
	           kdAnak = kodeAnak.getText().toString();
	           nmAnak = namaAnak.getText().toString();
	           
	           new insertAnak().execute();
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
	  /**
	     * Background Async Task to Create new product
	     * */
	    class insertAnak extends AsyncTask<String, String, String> {
	    	
	    	/**
	         * Before starting background thread Show Progress Dialog
	         * */
	        @Override
	        protected void onPreExecute() {
	            super.onPreExecute();
	            pDialog = new ProgressDialog(tambahanak.this);
	            pDialog.setMessage("Menambahkan anak..");
	            pDialog.setIndeterminate(false);
	            pDialog.setCancelable(true);
	            pDialog.show();
	        }
	        
	        /**
	         * Creating product
	         * */
	        protected String doInBackground(String... args) {
	            // Building Parameters
	            List<NameValuePair> params = new ArrayList<NameValuePair>();
	            params.add(new BasicNameValuePair("kdAnaknya", kdAnak));
	            params.add(new BasicNameValuePair("nmAnaknya", nmAnak));
	            params.add(new BasicNameValuePair("namane", name));
	 
	            // getting JSON Object
	            // Note that create product url accepts POST method
	            JSONObject json = jsonParser.makeHttpRequest(url_insert, "POST", params);
	 
	            // check log cat fro response
	            Log.d("Create Response", json.toString());
	            
	         // check for success tag
	            try {
	                int success = json.getInt(TAG_SUCCESS);
	 
	                if (success == 1) {
	                	// successfully created product
	                    
	                    
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
	            // dismiss the dialog once done
	            pDialog.dismiss();
	            Intent i = new Intent(getApplicationContext(), LoadAnak.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                finish();
                startActivity(i);
	        }
	        
	    }
}
