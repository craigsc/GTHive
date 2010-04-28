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

/**
 * Handles all preferences/settings for GTHive.
 * @author Craig Campbell
 *
 */
public class Prefs extends PreferenceActivity {
	public static final int DIALOG_CLEARDATA = 0;
	
	/**
	 * Simply expand and display the layout defined in
	 * settings.xml when the activity is created.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		Preference clear = findPreference("clear_data");
		//hook up dialog box to the clear_data button
		clear.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference pref) {
				showDialog(DIALOG_CLEARDATA);
				return true;
			}
		});
	}
	
	/**
	 * Standard onCreateDialog method as defined by Android. The only dialog
	 * defined at the moment is the clear data dialog which is displayed when
	 * the user clicks the clear all saved data button.
	 */
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		switch (id) {
		case DIALOG_CLEARDATA:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			//setup dialog and callbacks
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
	
	/**
	 * Removes all values saved to preferences.
	 */
	private void deleteLoginData() {
		SharedPreferences settings = getSharedPreferences(GTHive.PREFS_NAME, 0);
		settings.edit().clear().commit();
	}
	
	/**
	 * Returns true if preferences are set to remember logins, false otherwise.
	 * @param c the Context of the Activity
	 * @return boolean true if GTHive should remember logins, false otherwise
	 */
	public static boolean getRememberLogin(Context c) {
		return PreferenceManager.getDefaultSharedPreferences(c)
				.getBoolean("remember_login", true);
	}
	
	/**
	 * Returns true if preferences are set to auto signin whenever wifi
	 * is connected to GTWireless, false otherwise.
	 * @param c Context of the activity
	 * @return boolean true if GTHive should auto signin, false otherwise
	 */
	public static boolean getAutoSignin(Context c) {
		return PreferenceManager.getDefaultSharedPreferences(c)
				.getBoolean("auto_signin", true);
	}
	
	/**
	 * Determines if preferences denote that wifi should be automatically 
	 * enabled when needed.
	 * @param c the Context of the activity
	 * @return boolean, true if wifi should be auto-enabled, false otherwise
	 */
	public static boolean getAutoWifi(Context c) {
		return PreferenceManager.getDefaultSharedPreferences(c)
				.getBoolean("wifi", false);
	}
	
	/**
	 * Determines if preferences denote that ISS should be enabled or disabled.
	 * @param c the Context of the Activity
	 * @return boolean, true if ISS should be enables, false otherwise.
	 */
	public static boolean getISS(Context c) {
		return PreferenceManager.getDefaultSharedPreferences(c)
				.getBoolean("iss", true);
	}
	
	/**
	 * Called when Preference activity finishes. This method checks whether the
	 * user has set the remember_login preference to false and deletes all saved
	 * login information if they have. 
	 */
	@Override
	public void onStop() {
		SharedPreferences settings = getPreferenceManager().getSharedPreferences();
		if (!settings.getBoolean("remember_login", true)) {
			deleteLoginData();
		}
		super.onStop();
	}
}
