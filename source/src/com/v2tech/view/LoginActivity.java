package com.v2tech.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.ConfigRequest;
import com.V2.jni.ImRequest;
import com.v2tech.R;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {
	/**
	 * A dummy authentication store containing known user names and passwords.
	 * TODO: remove after connecting to a real authentication system.
	 */
	private static final String[] DUMMY_CREDENTIALS = new String[] {
			"foo@example.com:hello", "bar@example.com:world" };

	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_EMAIL = "com.example.android.authenticatordemo.extra.EMAIL";

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserLoginTask mAuthTask = null;

	// Values for email and password at the time of the login attempt.
	private String mEmail;
	private String mPassword;

	// UI references.
	private EditText mEmailView;
	private EditText mPasswordView;
	private CheckBox mRemPwdCkbx;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;
	private View mShowIpSettingButton;

	private Activity mContext;

	private ImRequest mIM = ImRequest.getInstance(this);
	private ConfigRequest mCR = new ConfigRequest();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.activity_login);

		// Set up the login form.
		mEmail = getIntent().getStringExtra(EXTRA_EMAIL);
		mEmailView = (EditText) findViewById(R.id.email);
		mEmailView.setText(mEmail);

		mPasswordView = (EditText) findViewById(R.id.password);

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
		mShowIpSettingButton = findViewById(R.id.show_setting);
		mShowIpSettingButton.setOnClickListener(showIpSetting);

		mRemPwdCkbx = (CheckBox) findViewById(R.id.login_rem_pwd);

		init();
	}

	private void init() {
		
		SharedPreferences sf = mContext.getSharedPreferences("config",
				Context.MODE_PRIVATE);
		mEmailView.setText(sf.getString("user", ""));
		mPasswordView.setText(sf.getString("passwd", ""));
		if(!sf.getString("user", "").equals("") && !sf.getString("passwd", "").equals("")) {
			mRemPwdCkbx.setChecked(true);
		}
	}

	private OnClickListener showIpSetting = new OnClickListener() {

		@Override
		public void onClick(final View vButton) {
			vButton.setEnabled(false);

			final Dialog dialog = new Dialog(mContext, R.style.IpSettingDialog);
			dialog.setContentView(R.layout.ip_setting);

			Button cancelButton = (Button) dialog
					.findViewById(R.id.ip_setting_cancel);
			Button saveButton = (Button) dialog
					.findViewById(R.id.ip_setting_save);

			final EditText et1 = (EditText) dialog.findViewById(R.id.ip1);
			final EditText et2 = (EditText) dialog.findViewById(R.id.ip2);
			final EditText et3 = (EditText) dialog.findViewById(R.id.ip3);
			final EditText et4 = (EditText) dialog.findViewById(R.id.ip4);
			final EditText port = (EditText) dialog.findViewById(R.id.port);

			SharedPreferences sf = mContext.getSharedPreferences("config",
					Context.MODE_PRIVATE);
			String cacheIp = sf.getString("ip", null);
			if (cacheIp != null && cacheIp.length() <= 15) {
				String[] ips = cacheIp.split("\\.");
				et1.setText(ips[0]);
				et2.setText(ips[1]);
				et3.setText(ips[2]);
				et4.setText(ips[3]);
			}

			port.setText(sf.getString("port", ""));

			saveButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					final String ip = et1.getText().toString() + "."
							+ et2.getText().toString() + "."
							+ et3.getText().toString() + "."
							+ et4.getText().toString();
					if (!saveHostConfig(ip,
							Integer.parseInt(port.getText().toString()))) {
						Toast.makeText(mContext,
								R.string.error_save_host_config,
								Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(mContext,
								R.string.succeed_save_host_config,
								Toast.LENGTH_LONG).show();
					}
					dialog.dismiss();
					vButton.setEnabled(true);
				}
			});

			cancelButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
					vButton.setEnabled(true);
				}
			});

			dialog.setOnKeyListener(new Dialog.OnKeyListener() {

				@Override
				public boolean onKey(DialogInterface arg0, int keyCode,
						KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_BACK) {
						dialog.dismiss();
					}
					vButton.setEnabled(true);
					return false;
				}
			});

			dialog.show();
		}

	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}

	private boolean saveHostConfig(String ip, int port) {
		SharedPreferences sf = mContext.getSharedPreferences("config",
				Context.MODE_PRIVATE);
		Editor e = sf.edit();
		e.putString("ip", ip);
		e.putString("port", port + "");
		return e.commit();
	}

	private boolean saveUserConfig(String user, String passwd) {
		SharedPreferences sf = mContext.getSharedPreferences("config",
				Context.MODE_PRIVATE);
		Editor e = sf.edit();
		e.putString("user", user);
		e.putString("passwd", passwd);
		return e.commit();
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {		
		if (mAuthTask != null) {
			return;
		}
		SharedPreferences sf = mContext.getSharedPreferences("config",
				Context.MODE_PRIVATE);
		String ip = sf.getString("ip", null);
		String port = sf.getString("port", null);
		
		if (ip == null || ip.isEmpty() || port == null || port.isEmpty()) {
			Toast.makeText(mContext, R.string.error_no_host_configuration, Toast.LENGTH_SHORT).show();
			mShowIpSettingButton.performClick();
			return;
		}
		mCR.setServerAddress(ip, Integer.parseInt(port));
		
		// Reset errors.
		mEmailView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mEmail = mEmailView.getText().toString();
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (mPassword.length() < 4) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(mEmail)) {
			mEmailView.setError(getString(R.string.error_field_required));
			focusView = mEmailView;
			cancel = true;
		} else if (!mEmail.contains("@")) {
			mEmailView.setError(getString(R.string.error_invalid_email));
			focusView = mEmailView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);
			mAuthTask = new UserLoginTask();
			mAuthTask.execute((Void) null);
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
			if (!mIM.loginSync(mEmail, mPassword)) {
				return false;
			}
			// TODO: register the new account here.

			// Save user info
			if (mRemPwdCkbx.isChecked()) {				
				saveUserConfig(mEmailView.getText().toString(), mPasswordView
						.getText().toString());
			} else {
				saveUserConfig("", "");
			}
			return true;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			mAuthTask = null;
			showProgress(false);

			if (success) {
				finish();
				// TODO start activiy
				mContext.startActivity(new Intent(mContext, ConfsActivity.class));
			} else {
				mPasswordView
						.setError(getString(R.string.error_incorrect_password));
				mPasswordView.requestFocus();
			}
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			showProgress(false);
		}
	}

	static {
		System.loadLibrary("event");
		System.loadLibrary("udt");
		System.loadLibrary("v2vi");
		System.loadLibrary("v2ve");	
		System.loadLibrary("v2client");
	}

}
