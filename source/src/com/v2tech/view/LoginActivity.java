package com.v2tech.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.ConfigRequest;
import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.UserService;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestLogInResponse;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.SPUtil;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {

	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_EMAIL = "com.example.android.authenticatordemo.extra.EMAIL";

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */

	private ConfigRequest mCR = new ConfigRequest();

	private UserService us = new UserService();

	// Values for email and password at the time of the login attempt.
	private String mEmail;
	private String mPassword;

	// UI references.
	private EditText mEmailView;
	private EditText mPasswordView;
	private TextView mLoginStatusMessageView;
	private View mShowIpSettingButton;
	private Dialog mSettingDialog;

	private Activity mContext;

	private View loginView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (GlobalConfig.GLOBAL_LAYOUT_SIZE == Configuration.SCREENLAYOUT_SIZE_XLARGE
				|| GlobalConfig.GLOBAL_LAYOUT_SIZE == Configuration.SCREENLAYOUT_SIZE_LARGE) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		setContentView(R.layout.activity_login);
		mContext = this;

		// Set up the login form.
		mEmail = getIntent().getStringExtra(EXTRA_EMAIL);
		mEmailView = (EditText) findViewById(R.id.email);
		mEmailView.setText(mEmail);
		mEmailView.addTextChangedListener(userNameTextWAtcher);

		mPasswordView = (EditText) findViewById(R.id.password);

		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.login_form_ll).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(
								mEmailView.getWindowToken(), 0);
						imm.hideSoftInputFromWindow(
								mPasswordView.getWindowToken(), 0);
					}
				});

		findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(
								mEmailView.getWindowToken(), 0);
						imm.hideSoftInputFromWindow(
								mPasswordView.getWindowToken(), 0);
						attemptLogin();
					}
				});
		mShowIpSettingButton = findViewById(R.id.show_setting);
		mShowIpSettingButton.setOnClickListener(showIpSetting);

		loginView = findViewById(R.id.login_layout);
		loginView.setVisibility(View.GONE);
		init();
	}

	private String[] local;

	private void init() {
		String user = SPUtil.getConfigStrValue(this, "user");
		String password = SPUtil.getConfigStrValue(this, "passwd");
		mEmailView.setText(user);
		mPasswordView.setText(password);
	}

	@Override
	protected void onResume() {
		super.onResume();

		loginView.setVisibility(View.VISIBLE);
		final Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
				this, R.animator.login_container_down_in);
		tabBlockHolderAnimation.setDuration(700);
		loginView.startAnimation(tabBlockHolderAnimation);
	}

	private TextWatcher userNameTextWAtcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable arg0) {

		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {

		}

		@Override
		public void onTextChanged(CharSequence str, int arg1, int arg2, int arg3) {
			if (local == null || local.length < 2) {
				return;
			}
			if (str.toString().equals(local[0])) {
				mPasswordView.setText(local[1]);
			} else {
				mPasswordView.setText("");
			}
		}

	};

	private OnClickListener showIpSetting = new OnClickListener() {

		@Override
		public void onClick(final View vButton) {
			if (mSettingDialog != null) {
				mSettingDialog.show();
				return;
			}
			final Dialog dialog = new Dialog(mContext, R.style.IpSettingDialog);
			dialog.setContentView(R.layout.ip_setting);

			Button cancelButton = (Button) dialog
					.findViewById(R.id.ip_setting_cancel);
			Button saveButton = (Button) dialog
					.findViewById(R.id.ip_setting_save);

			final EditText et = (EditText) dialog.findViewById(R.id.ip);
			final EditText port = (EditText) dialog.findViewById(R.id.port);

			et.setText(SPUtil.getConfigStrValue(mContext, "ip"));
			port.setText(SPUtil.getConfigStrValue(mContext, "port"));

			saveButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String ets = et.getText().toString();
					String portStr5 = port.getText().toString();
					if (!checkIPorDNS(ets)) {
						et.setError(mContext
								.getText(R.string.error_host_invalid));
						return;
					}
					if (!saveHostConfig(ets, portStr5)) {
						Toast.makeText(mContext,
								R.string.error_save_host_config,
								Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(mContext,
								R.string.succeed_save_host_config,
								Toast.LENGTH_LONG).show();
					}
					dialog.dismiss();
				}
			});

			cancelButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					et.setText(SPUtil.getConfigStrValue(mContext, "ip"));
					port.setText(SPUtil.getConfigStrValue(mContext, "port"));
					dialog.dismiss();
				}
			});

			dialog.setCancelable(true);
			dialog.setCanceledOnTouchOutside(true);

			mSettingDialog = dialog;
			dialog.show();
		}

	};

	private boolean checkIPorDNS(String str) {

		String ValidIpAddressRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";

		String ValidHostnameRegex = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.){1,}([A-Za-z][A-Za-z][A-Za-z]*)$";

		return str.matches(ValidIpAddressRegex)
				|| str.matches(ValidHostnameRegex);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mSettingDialog != null && mSettingDialog.isShowing()) {
			mSettingDialog.dismiss();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}

	private boolean saveHostConfig(String ip, String port) {
		return SPUtil.putConfigStrValue(this, new String[] { "ip", "port" },
				new String[] { ip, port });
	}

	private boolean saveUserConfig(String user, String passwd) {
		return SPUtil.putConfigStrValue(this,
				new String[] { "user", "passwd" },
				new String[] { user, passwd });
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		String ip = SPUtil.getConfigStrValue(this, "ip");
		String port = SPUtil.getConfigStrValue(this, "port");

		if (ip == null || ip.isEmpty() || port == null || port.isEmpty()) {
			Toast.makeText(mContext, R.string.error_no_host_configuration,
					Toast.LENGTH_SHORT).show();
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
		} 

		// Check for a valid email address.
		if (TextUtils.isEmpty(mEmail)) {
			mEmailView.setError(getString(R.string.error_field_required));
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
			us.login(mEmailView.getText().toString(), mPasswordView.getText()
					.toString(), new Registrant(mHandler, LOG_IN_CALL_BACK,
					null));
		}
	}

	private Dialog mProgressDialog;

	private void showProgress(final boolean show) {
		if (show == false) {
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				return;
			}
		}

		if (mProgressDialog != null) {
			mProgressDialog.show();
			return;
		}
		final Dialog dialog = new Dialog(mContext, R.style.IpSettingDialog);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		LinearLayout ll = new LinearLayout(mContext);
		ll.setBackgroundResource(R.drawable.progress_bg);
		ll.setOrientation(LinearLayout.VERTICAL);

		TextView tv = new TextView(mContext);
		tv.setText(R.string.login_progress_signing_in);
		tv.setTextSize(20F);
		tv.setPadding(60, 80, 60, 60);
		tv.setGravity(Gravity.CENTER);
		ll.addView(tv, new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));

		ImageView iv = new ImageView(mContext);
		iv.setImageResource(R.drawable.progress_animation);
		iv.setPadding(60, 30, 60, 60);
		ll.addView(iv, new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));

		dialog.setContentView(ll);

		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);

		mProgressDialog = dialog;
		dialog.show();

	}

	private static final int LOG_IN_CALL_BACK = 1;
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LOG_IN_CALL_BACK:

				JNIResponse rlr = (JNIResponse) msg.obj;
				if (rlr.getResult() == JNIResponse.Result.TIME_OUT) {
					Toast.makeText(mContext, R.string.error_time_out,
							Toast.LENGTH_LONG).show();
				} else if (rlr.getResult() == JNIResponse.Result.FAILED) {
					mPasswordView
							.setError(getString(R.string.error_incorrect_password));
					mPasswordView.requestFocus();
				} else if (rlr.getResult() == JNIResponse.Result.CONNECT_ERROR) {
					Toast.makeText(mContext, R.string.error_connect_to_server,
							Toast.LENGTH_LONG).show();
				} else {
					// Save user info
					saveUserConfig(mEmailView.getText().toString(), "");
					GlobalHolder.getInstance().setCurrentUser(((RequestLogInResponse)rlr).getUser());
					SPUtil.putConfigIntValue(mContext,
							GlobalConfig.KEY_LOGGED_IN, 1);
					mContext.startActivity(new Intent(mContext,
							MainActivity.class));
					finish();
				}

				showProgress(false);
				break;
			}
		}

	};

	@Override
	protected void onStart() {
		super.onStart();

	}

	@Override
	protected void onStop() {
		super.onStop();

	}

}
