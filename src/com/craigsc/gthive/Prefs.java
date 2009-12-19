package com.craigsc.gthive;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;

public class Prefs extends PreferenceActivity {
	public static final int DIALOG_CLEARDATA = 0;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		Preference clear = findPreference("clear_data");
		clear.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference pref) {
				showDialog(DIALOG_CLEARDATA);
				return true;
			}
		});
	}
	
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		switch (id) {
		case DIALOG_CLEARDATA:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("All saved login information will be deleted.")
				.setCancelable(false)
			    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
			    	public void onClick(DialogInterface dialog, int id) {
			    		dialog.dismiss();
			    		findPreference("clear_data").setEnabled(false);
			            deleteLoginData();
			            setResult(333);
			        }
			    })
			    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			    	public void onClick(DialogInterface dialog, int id) {
			    		dialog.dismiss();
			        }
			    });
			dialog = builder.create();
			break;
		}
		return dialog;
	}
	
	private void deleteLoginData() {
		SharedPreferences settings = getSharedPreferences(GTHive.PREFS_NAME, 0);
		settings.edit().clear().commit();
	}
	
	public static boolean getRememberLogin(Context c) {
		return PreferenceManager.getDefaultSharedPreferences(c)
				.getBoolean("remember_login", true);
	}
	
	public static boolean getAutoWifi(Context c) {
		return PreferenceManager.getDefaultSharedPreferences(c)
				.getBoolean("wifi", false);
	}
	
	public static boolean getISS(Context c) {
		return PreferenceManager.getDefaultSharedPreferences(c)
				.getBoolean("iss", true);
	}
	
	@Override
	public void onStop() {
		SharedPreferences settings = getPreferenceManager().getSharedPreferences();
		if (!settings.getBoolean("remember_login", true)) {
			deleteLoginData();
		}
		super.onStop();
	}
}
