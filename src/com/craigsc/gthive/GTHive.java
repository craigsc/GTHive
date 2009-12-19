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

public class GTHive extends Activity implements OnClickListener, TextWatcher {
	public static final String PREFS_NAME = "gthive_data";
	public static final int CLEARED_DATA = 3333;
	private static final String TAG = "GTHive";
	private SharedPreferences settings;
	private SharedPreferences.Editor editor;
	private EditText user;
	private EditText pass;
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
        
        user.setText(settings.getString("user", ""));
        pass.setText(settings.getString("pass", ""));
        
        user.addTextChangedListener(this);
        pass.addTextChangedListener(this);
        ((Button)findViewById(R.id.login)).setOnClickListener(this);
    }

	public void onClick(View v) {
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
	
	private void login() {
		final Context c = this;
		new Thread() {
			public void run() {
				WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
				ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
				if (!wifi.isWifiEnabled()) {
					if (!wifi.setWifiEnabled(true)) {
						display("Failed to enable wifi, ensure that your device is wifi enabled before trying again.");
						return;
					}
					wifi.startScan();
					updateDialog("Connecting to network...");
					int i = 0;
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
					List<NameValuePair> data = new ArrayList<NameValuePair>(3);
					data.add(new BasicNameValuePair("username", user.getText().toString()));
					data.add(new BasicNameValuePair("password", pass.getText().toString()));
					data.add(new BasicNameValuePair("output", "text"));
					if (Prefs.getISS(c)) {
						Log.d("GTHive", "ISS ENABLED");
						data.add(new BasicNameValuePair("iss", "on"));
					}
					post.setEntity(new UrlEncodedFormEntity(data));
					
					ResponseHandler<String> rh = new BasicResponseHandler();
					result = client.execute(post,rh);
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
			
			private void updateDialog(final String s) {
				runOnUiThread(new Runnable() {
					public void run() {
						dialog.setMessage(s);
					}
				});
			}
			
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
	
	private void checkWifi() {
		if (!((WifiManager)getSystemService(Context.WIFI_SERVICE)).isWifiEnabled()) {
			if (!Prefs.getAutoWifi(this)) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
				dialog = ProgressDialog.show(GTHive.this, "",
        				"Enabling wifi...", true);
                login();
			}
		}
		else {
			dialog = ProgressDialog.show(GTHive.this, "",
    				"Authenticating...", true);
			login();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.settings:
			startActivityForResult(new Intent(this, Prefs.class), CLEARED_DATA);
			return true;
		case R.id.quit:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CLEARED_DATA && resultCode == 333) {
			user.setText(settings.getString("user", ""));
	        pass.setText(settings.getString("pass", ""));
		}
	}
	
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

	public void afterTextChanged(Editable s) {
		newText = true;
	}

	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	public void onTextChanged(CharSequence s, int start, int before, int count) {}
    
}