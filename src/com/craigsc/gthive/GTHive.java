package com.craigsc.gthive;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/** 
 * Main class file for the application, handles loading and storing of login
 * credentials. Also contains the logic for connecting and authenticating to
 * the Georgia Tech LAWN wifi network.
 * @author Craig Campbell
 *
 */
public class GTHive extends Activity implements OnClickListener, TextWatcher {
	public static final String PREFS_NAME = "gthive_data";
	public static final int CLEARED_DATA = 3333;
	private static final String TAG = "GTHive";
	private SharedPreferences settings;
	private SharedPreferences.Editor editor;
	private EditText user;
	private EditText pass;
	/** Whether any login info has changed since the screen was loaded */
	private boolean newText = false;
	private ProgressDialog dialog;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        user = (EditText)findViewById(R.id.username);
        pass = (EditText)findViewById(R.id.password);
        settings = getSharedPreferences(PREFS_NAME,0);
        editor = settings.edit();
        
        /* Set user/password textfields using stored info
         * if available, use "" if none is saved.
         */
        user.setText(settings.getString("user", ""));
        pass.setText(settings.getString("pass", ""));
        
        /* Hook up listeners */
        user.addTextChangedListener(this);
        pass.addTextChangedListener(this);
        ((Button)findViewById(R.id.login)).setOnClickListener(this);
    }

    /**
     * Button listener, validates the user/password text fields and
     * then proceeds with the authentication process by calling
     * {@link #checkWifi()}.
     */
	public void onClick(View v) {
		//set errors if user/password is blank
		if (pass.getText().toString().equals("") ||
				user.getText().toString().equals("")) {
			if (pass.getText().toString().equals("")) {
				pass.setError("Password is required");
				pass.requestFocus();
			}
			if (user.getText().toString().equals("")) {
				user.setError("Username is required");
				user.requestFocus();
			}
		}
		else {
			//clear errors, save new login info (if newText), and proceed.
			pass.setError(null);
			user.setError(null);
			if (newText && Prefs.getRememberLogin(this)) {
				editor.putString("user", user.getText().toString());
				editor.putString("pass", pass.getText().toString());
				editor.commit();
				newText = false;
			}
			checkWifi();
		}	
	}
	
	/**
	 * The meat and potatoes of the program, this method spawns a new thread which
	 * enables the wifi (if need be) and attempts to connect to a wifi network. Once connected,
	 * it checks the ssid of the network to ensure it is 'GTwireless' and then proceeds to 
	 * actually log the user in to the lawn network using the supplied username and password.
	 * It also checks if ISS is enabled via the preferences and sends the appropriate param
	 * if it is.
	 * 
	 * It is important to note that all of these network calls are happening on a separate thread
	 * from the main UI thread so that the progress dialog proceeds smoothly. The two inner private
	 * methods (updateDialog and display), however, run on the main UI thread since they deal with
	 * changes made to the UI.
	 */
	private void login() {
		final Context c = this;
		new Thread() {
			public void run() {
				WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
				ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
				
				//if wifi !enabled then attempt to enable
				if (!wifi.isWifiEnabled()) {
					if (!wifi.setWifiEnabled(true)) {
						display("Failed to enable wifi, ensure that your device is wifi enabled before trying again.");
						return;
					}
					updateDialog("Connecting to network...");
					int i = 0;
					
					//force a scan so connection occurs if wifi is setup properly in phone
					wifi.startScan();
					
					//if wifi is not connected to a network then check every second for 30 seconds for a connection
					while (!cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
						if (i == 30) {
							display("Device timed out while attempting to connect to a wifi network. Please ensure that you have " +
									"configured the device to use the Lawn wifi network and try again.");
							return;
						}
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {}
						i++;
					}
				}
				
				//If not connected or if SSID of connected network is not GTwireless then fail
				if (!cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected() || 
						!wifi.getConnectionInfo().getSSID().equalsIgnoreCase("GTwireless")) {
					display("Device is not connected to the Lawn wifi network. Please connect to the proper network and try again.");
					return;
				}
				updateDialog("Authenticating...");
				String result = "";
				HttpClient client = new DefaultHttpClient();
				HttpPost post = new HttpPost("https://auth.lawn.gatech.edu/index.php");
				try {
					//set up post parameters
					List<NameValuePair> data = new ArrayList<NameValuePair>(3);
					data.add(new BasicNameValuePair("username", user.getText().toString()));
					data.add(new BasicNameValuePair("password", pass.getText().toString()));
					data.add(new BasicNameValuePair("output", "text"));
					//check preferences for ISS status
					if (Prefs.getISS(c)) {
						Log.d("GTHive", "ISS ENABLED");
						data.add(new BasicNameValuePair("iss", "on"));
					}
					post.setEntity(new UrlEncodedFormEntity(data));
					
					ResponseHandler<String> rh = new BasicResponseHandler();
					result = client.execute(post,rh);
					
					//check text response and alert user of success/failure
					if (result.equals("Logging you into LAWN...")) {
						result = "Success!";
					}
					else if (result.equals("")) {
						result = "Already logged in.";
					} 
					display(result);
				}
				catch (UnsupportedEncodingException e) {
					Log.d(TAG, "UnsupportedEncodingException");
					display("Invalid characters, please re-enter information.");
				}
				catch (HttpResponseException e) {
					Log.d(TAG, "HttpResponseException");
					display("Lawn appears to be down, please try again later.");
				}
				catch (ClientProtocolException e) {
					Log.d(TAG, "ClientProtocolException");
					display("Could not connect to lawn, please try again later.");
				}
				catch (IOException e) {
					Log.d(TAG, "IOException");
					display("Connection interrupted, please try again.");
				}
				catch (Exception e) {
					Log.d(TAG, e.toString());
					display("Service appears down, please try again later.");
				}
			}
			
			/**
			 * Updates the progress dialog message on the UI thread from within the
			 * spawned network thread.
			 * @param s the string to set the progress dialog message to
			 */
			private void updateDialog(final String s) {
				runOnUiThread(new Runnable() {
					public void run() {
						dialog.setMessage(s);
					}
				});
			}
			
			/**
			 * Displays a custom made toast notification (see notification.xml) to display
			 * the result of the attempted login.
			 * @param s the string the toast notification should display
			 */
			private void display(final String s) {
				runOnUiThread(new Runnable() {
					public void run() {
						dialog.dismiss();
						Toast t = new Toast(GTHive.this);
						LayoutInflater inflater = getLayoutInflater();
						View layout = inflater.inflate(R.layout.notification,
								(ViewGroup) findViewById(R.id.layout_root));
						((TextView)layout.findViewById(R.id.notification_text)).setText(s);
						layout.setBackgroundResource(android.R.drawable.toast_frame);
						t.setDuration(Toast.LENGTH_LONG);
						t.setView(layout);
						t.show();
					}
				});
			}
		}.start();
	}
	
	/**
	 * Checks if wifi !enabled and preferences are set to NOT automatically enable
     * then show an AlertDialog to the user asking if wifi should be enabled.
	 * If user selects yes then continue with login, if user cancels then
	 * cancel with entire login process since it requires wifi.
	 */
	private void checkWifi() {
		if (!((WifiManager)getSystemService(Context.WIFI_SERVICE)).isWifiEnabled()) {
			//check if preferences are set to NOT enable wifi automatically
			if (!Prefs.getAutoWifi(this)) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				//build alert dialog to display to user with callbacks
				builder.setTitle("Wifi is Currently Disabled")
					   .setMessage("Wifi connection required. Enable wifi?")
				       .setCancelable(false)
				       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface d, int id) {
				                d.cancel();
				           }
				       })
				       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface d, int id) {
				                d.cancel();
				                dialog = ProgressDialog.show(GTHive.this, "",
				        				"Enabling wifi...", true);
				                login();
				           }
				       });
				AlertDialog alert = builder.create();
				alert.show();
			}
			else {
				//if autoWifi enabled then simply start progressDialog and call login()
				dialog = ProgressDialog.show(GTHive.this, "",
        				"Enabling wifi...", true);
                login();
			}
		}
		else {
			//if wifi is enabled then start progressDialog and call login()
			dialog = ProgressDialog.show(GTHive.this, "",
    				"Authenticating...", true);
			login();
		}
	}
	
	/**
	 * Standard method, called when user presses menu button and displays
	 * the menu defined in menu.xml
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.menu, menu);
		return true;
	}
	
	/**
	 * Called when a menu item is pressed, checks which menu item was pressed
	 * and calls the appropriate method.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.settings:
			/*
			 *  Need to check for result because user/pass might need to be reloaded
			 *  upon completion in the case that user chose to clear all saved data.
			 */
			startActivityForResult(new Intent(this, Prefs.class), CLEARED_DATA);
			return true;
		case R.id.quit:
			finish();
			return true;
		case R.id.email:
			//TODO this is not entirely supported yet and needs major work, see Web.java
			startActivity(new Intent(this, Web.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Only used in the case of a user entering the preference screen, this method is
	 * called when the Preference activity finishes and checks for a resultCode of
	 * "333". This has been specially defined to denote that the user chose to clear
	 * all saved data in GTHive so the username and password textfields are reloaded
	 * so that the previously saved information is no longer displayed to the user since
	 * it has been wiped from the program.
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CLEARED_DATA && resultCode == 333) {
			user.setText(settings.getString("user", ""));
	        pass.setText(settings.getString("pass", ""));
		}
	}
	
	/**
	 * Workaround for progressDialog errors on rotation changes. The issue occurs because
	 * the progress dialog is started in the UI thread but on rotation change the UI is "reset"
	 * automatically by Android. When the spawned network thread from {@link #login()} attempts
	 * to update the progressDialog after this orientation change a null pointer error occurs
	 * since the progress dialog no longer exists.
	 */
	public void onConfigurationChanged(Configuration c) {
		super.onConfigurationChanged(c);
		switch (c.orientation) {
		case Configuration.ORIENTATION_PORTRAIT :
			findViewById(R.id.background).setBackgroundResource(R.drawable.stadiumsmall);
			break;
		case Configuration.ORIENTATION_LANDSCAPE :
			findViewById(R.id.background).setBackgroundResource(0);
			break;
		}
	}

	/**
	 * Set newText variable to true if username or password textfields change so that the 
	 * program knows it has to save the new information.
	 */
	public void afterTextChanged(Editable s) {
		newText = true;
	}

	/** Unused. */
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	/** Unused. */
	public void onTextChanged(CharSequence s, int start, int before, int count) {}
    
}