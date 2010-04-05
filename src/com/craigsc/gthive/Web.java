package com.craigsc.gthive;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * This is an unfinished class that is meant to load a
 * webview into GTHive when the user clicks the email menu
 * item. In its current form, it simply loads the gatech email
 * website but DOES NOT authenticate automatically.
 * @author Craig Campbell
 *
 */
public class Web extends Activity {
	WebView browser;
	
	/**
	 * Called when activity begins, sets up the browser and loads
	 * the gatech web email client. DOES NOT automatically authenticate
	 * in its current state.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.web);
		browser = (WebView) findViewById(R.id.webview);
		browser.setWebViewClient(new CustomWebViewClient());
		browser.getSettings().setJavaScriptEnabled(true);
		browser.loadUrl("http://www.mail.gatech.edu");
		//TODO automatically log in using saved credentials
	}
	
	
	private class CustomWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}
	
	/**
	 * Called when user presses the back button. If the browser can go back
	 * then it simply tells the browser to go back. If not then the default
	 * back button behavior continues.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && browser.canGoBack()) {
			browser.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
