package com.craigsc.gthive;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class GTHiveReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Prefs.getAutoSignin(context)) {
			WifiInfo wi = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
			if (wi != null && wi.getSSID().equalsIgnoreCase(GTHive.SSID)) {
				SharedPreferences settings = context.getSharedPreferences(GTHive.PREFS_NAME, 0);
				String user = settings.getString("user", null);
				String pass = settings.getString("pass", null);
				if (user != null && pass != null) {
					
					HttpClient client = new DefaultHttpClient();
					HttpPost post = new HttpPost("https://auth.lawn.gatech.edu/index.php");
					try {
						//set up post parameters
						List<NameValuePair> data = new ArrayList<NameValuePair>(3);
						data.add(new BasicNameValuePair("username", user));
						data.add(new BasicNameValuePair("password", pass));
						data.add(new BasicNameValuePair("output", "text"));
						//check preferences for ISS status
						if (Prefs.getISS(context)) {
							Log.d("GTHive", "ISS ENABLED");
							data.add(new BasicNameValuePair("iss", "on"));
						}
						post.setEntity(new UrlEncodedFormEntity(data));
						
						ResponseHandler<String> rh = new BasicResponseHandler();
						String result = client.execute(post,rh);
						
						//check text response and alert user of success/failure
						if (result.equals("Logging you into LAWN...")) {
							result = "Successfully logged into LAWN";
						}
						else if (result.equals("")) {
							result = "Already logged in to LAWN.";
						} 
						//Toast.makeText(context, result, Toast.LENGTH_LONG).show();
					}
					catch (Exception e) {
						//Toast.makeText(context, "Error authenticating to LAWN. Try using GTHive.", Toast.LENGTH_LONG).show();
						Log.d("GTHive_BROADCAST_RECEIVER", e.toString());
					}
					
				}
			}
		}
	}
}
