package com.bizcom.vc.activity.main;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.V2.jni.ConfigRequest;
import com.V2.jni.util.V2Log;
import com.bizcom.db.DataBaseContext;
import com.bizcom.db.V2TechDBHelper;
import com.bizcom.request.V2ImRequest;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.request.jni.RequestLogInResponse;
import com.bizcom.request.util.HandlerWrap;
import com.bizcom.util.WaitDialogBuilder;
import com.bizcom.util.LocalSharedPreferencesStorage;
import com.bizcom.util.V2Toast;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.MainApplication;
import com.bizcom.vo.User;
import com.v2tech.R;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {
	private static final String TAG = "LoginActivity";
	// The default email to populate the email field with.
	public static final String EXTRA_EMAIL = "com.example.android.authenticatordemo.extra.EMAIL";
	private static final int LOG_IN_CALL_BACK = 1;

	// UI references.
	private RelativeLayout mRlLoginLayout;
	private LinearLayout mLlLoginFormLayout;
	private EditText mEtUserName;
	private EditText mEtPassword;
	private TextView mTvLoginStatus;
	private ImageView mIvSettingButton;
	private Dialog mSettingDialog;
	private TextView mTvLogin;

	private TextWatcher mUserNameTextWatcher = new UserNameTextWatcher();
	private EtUserNameOnEditorActionListener mEtUserNameOnEditorActionListener = new EtUserNameOnEditorActionListener();
	private EtUserNameOnFocusChangeListener mEtUserNameOnFocusChangeListener = new EtUserNameOnFocusChangeListener();
	private EtUserNameOnTouchListener mEtUserNameOnTouchListener = new EtUserNameOnTouchListener();
	private EtPasswordOnEditorActionListener mEtPasswordOnEditorActionListener = new EtPasswordOnEditorActionListener();
	private EtPasswordOnFocusChangeListener mEtPasswordOnFocusChangeListener = new EtPasswordOnFocusChangeListener();
	private EtPasswordOnTouchListener mEtPasswordOnTouchListener = new EtPasswordOnTouchListener();
	
	private RlLoginLayoutOnClickListener mRlLoginLayoutOnClickListener = new RlLoginLayoutOnClickListener();
	private TvLoginButtonOnClickListener mTvLoginButtonOnClickListener = new TvLoginButtonOnClickListener();
	private OnClickListener mIvSettingButtonOnClickListener = new IvSettingButtonOnClickListener();
	private Handler mHandler = new LocalHandler();

	private Activity mContext;
	// Keep track of the login task to ensure we can cancel it if requested.
	private ConfigRequest mConfigRequest = new ConfigRequest();
	private V2ImRequest mUserService = new V2ImRequest();

	private Boolean isLoggingIn = false;
	private boolean isFward = false;
	// Values for email and password at the time of the login attempt.
	private String mUserName;
	private String mPassword;
	private String[] localArray;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i("20150304 2","LoginActivity onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		mContext = this;
		initLayout();
		initUserNameAndPassword();
	}

	private void initLayout() {
		// Set up the login form.
		mEtUserName = (EditText) findViewById(R.id.email);
		// mEtUserName.addTextChangedListener(mUserNameTextWatcher);
		mEtUserName.setOnFocusChangeListener(mEtUserNameOnFocusChangeListener);
		mEtUserName
				.setOnEditorActionListener(mEtUserNameOnEditorActionListener);
		mEtUserName.setOnTouchListener(mEtUserNameOnTouchListener);

		mEtPassword = (EditText) findViewById(R.id.password);
		mEtPassword.setOnFocusChangeListener(mEtPasswordOnFocusChangeListener);
		mEtPassword.setOnTouchListener(mEtPasswordOnTouchListener);
		mEtPassword
				.setOnEditorActionListener(mEtPasswordOnEditorActionListener);

		mTvLoginStatus = (TextView) findViewById(R.id.login_status_message);

		mRlLoginLayout = (RelativeLayout) findViewById(R.id.login_layout);
		mRlLoginLayout.setOnClickListener(mRlLoginLayoutOnClickListener);

		mTvLogin = (TextView) findViewById(R.id.login_button);
		mTvLogin.setOnClickListener(mTvLoginButtonOnClickListener);

		mIvSettingButton = (ImageView) findViewById(R.id.show_setting);
		mIvSettingButton.setOnClickListener(mIvSettingButtonOnClickListener);

		mLlLoginFormLayout = (LinearLayout) findViewById(R.id.login_form);
		mLlLoginFormLayout.setVisibility(View.GONE);
	}

	private void initUserNameAndPassword() {
		String user = LocalSharedPreferencesStorage.getConfigStrValue(this,
				"user");
		String password = LocalSharedPreferencesStorage.getConfigStrValue(this,
				"passwd");
		if (user != null && !user.trim().isEmpty()) {
			mEtUserName.setText(user);
			mEtUserName.setTextColor(Color.BLACK);
		}
		
		if (password != null && !password.trim().isEmpty()) {
			mEtPassword.setText(password);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		startLlLoginFormLayoutAnim();
	}

	private void startLlLoginFormLayoutAnim() {
		mLlLoginFormLayout.setVisibility(View.VISIBLE);
		final Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
				this, R.anim.login_container_down_in);
		tabBlockHolderAnimation.setDuration(1000);
		tabBlockHolderAnimation.setFillAfter(true);
		// tabBlockHolderAnimation.setInterpolator(new BounceInterpolator());
		mLlLoginFormLayout.startAnimation(tabBlockHolderAnimation);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		Log.i("20150304 2","LoginActivity onDestroy()");
		super.onDestroy();
		mUserService.clearCalledBack();

		if (!isFward) {
			((MainApplication) getApplication()).uninitForExitProcess();
		}
		
		Log.i("20150228 1","LoginActivity onDestroy");
	}

	private boolean checkIPorDNS(String str) {
		if (str == null) {
			return false;
		}
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

	private boolean saveServiceSettingInfo(String ip, String port) {
		return LocalSharedPreferencesStorage.putStrValue(this, new String[] {
				"ip", "port" }, new String[] { ip, port });
	}

	private boolean saveUserNameAndPasswd(String user, String passwd) {
		return LocalSharedPreferencesStorage.putStrValue(this, new String[] {
				"user", "passwd" }, new String[] { user, passwd });
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	private void attemptLogin() {
		String ip = LocalSharedPreferencesStorage.getConfigStrValue(this, "ip");
		String port = LocalSharedPreferencesStorage.getConfigStrValue(this,
				"port");

		if (ip == null || ip.isEmpty() || port == null || port.isEmpty()) {
			V2Toast.makeText(mContext, R.string.error_no_host_configuration,
					V2Toast.LENGTH_SHORT).show();
			mIvSettingButton.performClick();
			return;
		}

		mConfigRequest.setServerAddress(ip, Integer.parseInt(port));

		// Reset errors.
		mEtUserName.setError(null);
		mEtPassword.setError(null);

		// Store values at the time of the login attempt.
		mUserName = mEtUserName.getText().toString();
		mPassword = mEtPassword.getText().toString();

		// Check user name is initial user name or not.
		if (mContext.getResources().getText(R.string.login_user_name)
				.equals(mUserName)) {
			mEtUserName.setError(getString(R.string.error_field_required));
			mEtUserName.requestFocus();
			return;
		}

		// Check password is initial password
		if (mContext.getResources().getText(R.string.prompt_password)
				.equals(mPassword)) {
			mEtPassword.setError(getString(R.string.error_field_required));
			mEtPassword.requestFocus();
			return;
		}

		boolean cancel = false;
		View focusView = null;

		// Check for a valid email address.
		if (TextUtils.isEmpty(mUserName)) {
			mEtUserName.setError(getString(R.string.error_field_required));
			focusView = mEtUserName;
			cancel = true;
		}

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mEtPassword.setError(getString(R.string.error_field_required));
			focusView = mEtPassword;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			synchronized (isLoggingIn) {
				if (isLoggingIn) {
					V2Log.w("Current state is logging in");
					return;
				}
				isLoggingIn = true;
				// Show a progress spinner, and kick off a background task to
				// perform the user login attempt.
				mTvLoginStatus.setText(R.string.login_progress_signing_in);
				WaitDialogBuilder.showNormalProgress(mContext, true);
				mUserService.login(mEtUserName.getText().toString(), mEtPassword
						.getText().toString(), new HandlerWrap(mHandler,
						LOG_IN_CALL_BACK, null));
			}
		}
	}

	// 创建登陆用户存储数据的文件夹
	private void createPersonFolder(User user) {
		GlobalConfig.LOGIN_USER_ID = String.valueOf(user.getmUserId());

		File avatarPath = new File(GlobalConfig.getGlobalUserAvatarPath());
		if (!avatarPath.exists()) {
			boolean res = avatarPath.mkdirs();
			V2Log.i(" create avatar dir " + avatarPath.getAbsolutePath() + "  " + res);
		}

		File image = new File(GlobalConfig.getGlobalPicsPath());
		if (!image.exists()) {
			boolean res = image.mkdirs();
			V2Log.i(" create image dir " + image.getAbsolutePath() + "  " + res);
		}

		File audioPath = new File(GlobalConfig.getGlobalAudioPath());
		if (!audioPath.exists()) {
			boolean res = audioPath.mkdirs();
			V2Log.i(" create audio dir " + audioPath.getAbsolutePath() + "  "
					+ res);
		}

		File filePath = new File(GlobalConfig.getGlobalFilePath());
		if (!filePath.exists()) {
			boolean res = filePath.mkdirs();
			V2Log.i(" create file dir " + filePath.getAbsolutePath() + "  "
					+ res);
		}

	}

	private class EtUserNameOnFocusChangeListener implements
			OnFocusChangeListener {

		@Override
		public void onFocusChange(View arg0, boolean focus) {
			if (focus) {
				if (mContext.getResources().getText(R.string.login_user_name)
						.equals(mEtUserName.getText().toString())) {
					mEtUserName.setText("");
				}
				mEtUserName.setTextColor(Color.BLACK);
			} else {
				if (mEtUserName.getText().toString().trim().isEmpty()) {
					mEtUserName.setText(R.string.login_user_name);
					mEtUserName.setTextColor(mContext.getResources().getColor(
							R.color.login_activity_login_box_text_color));
				}
			}
		}
	}

	private class EtUserNameOnEditorActionListener implements
			OnEditorActionListener {

		@Override
		public boolean onEditorAction(TextView tv, int actionId, KeyEvent event) {
			mEtPassword.setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_VARIATION_PASSWORD);
			return false;
		}

	}

	private class EtPasswordOnFocusChangeListener implements
			OnFocusChangeListener {

		@Override
		public void onFocusChange(View arg0, boolean focus) {
			if (focus) {
				mEtPassword.setTextColor(Color.BLACK);
				if (mContext.getResources().getText(R.string.prompt_password)
						.equals(mEtPassword.getText().toString())) {
					mEtPassword.setText("");
				}
			} else {
				if (mEtPassword.getText().toString().trim().isEmpty()) {
					mEtPassword.setText(R.string.prompt_password);
					mEtPassword.setTextColor(mContext.getResources().getColor(
							R.color.login_activity_login_box_text_color));
					mEtPassword.setInputType(InputType.TYPE_CLASS_TEXT);
				} else {
					mEtPassword.setInputType(InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_VARIATION_PASSWORD);
				}
			}
		}
	}

	private class EtUserNameOnTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View v, MotionEvent event) {

			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mEtUserName.requestFocus();
				mEtUserName.setError(null);
			}
			return false;
		}
	}

	private class EtPasswordOnTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View view, MotionEvent event) {

			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(mEtPassword, InputMethodManager.SHOW_FORCED);
				EditText et = ((EditText) view);

				et.setInputType(InputType.TYPE_CLASS_TEXT
						| InputType.TYPE_TEXT_VARIATION_PASSWORD);
				et.requestFocus();
				et.setError(null);
			}
			return true;
			
		}
	}

	private class EtPasswordOnEditorActionListener implements
			OnEditorActionListener {

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			// T
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mEtUserName.getWindowToken(), 0);
				imm.hideSoftInputFromWindow(mEtPassword.getWindowToken(), 0);
				attemptLogin();
			}
			return false;
		}

	}

	private class RlLoginLayoutOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View view) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mEtUserName.getWindowToken(), 0);
			imm.hideSoftInputFromWindow(mEtPassword.getWindowToken(), 0);
			mEtPassword.setError(null);
			mEtUserName.setError(null);
		}
	}

	private class TvLoginButtonOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View view) {
			
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mEtUserName.getWindowToken(), 0);
			imm.hideSoftInputFromWindow(mEtPassword.getWindowToken(), 0);
			attemptLogin();
			
		}
	}

	private class UserNameTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			Log.i("20150213 1", "afterTextChanged()" + " s = " + s.toString());
			// if (!s.toString().trim().isEmpty()) {
			// mEtUserName.setError(null);
			// }
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			Log.i("20150213 1", "beforeTextChanged()" + " s = " + s.toString()
					+ " start = " + start + " count = " + count + " after = "
					+ after);
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			Log.i("20150213 1", "onTextChanged()" + " s = " + s.toString()
					+ " start = " + start + " before = " + before + " count = "
					+ count);
			if (localArray == null || localArray.length < 2) {
				return;
			}
			if (s.toString().equals(localArray[0])) {
				mEtPassword.setText(localArray[1]);
			} else {
				mEtPassword.setText("");
			}
		}

	}

	private class IvSettingButtonOnClickListener implements OnClickListener {

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

			et.setText(LocalSharedPreferencesStorage.getConfigStrValue(
					mContext, "ip"));
			port.setText(LocalSharedPreferencesStorage.getConfigStrValue(
					mContext, "port"));

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
					if (portStr5 == null || portStr5.isEmpty()) {
						port.setError(mContext
								.getText(R.string.error_field_required));
						return;
					}
					if (!saveServiceSettingInfo(ets, portStr5)) {
						V2Toast.makeText(mContext,
								R.string.error_save_host_config,
								V2Toast.LENGTH_LONG).show();
					} else {
						V2Toast.makeText(mContext,
								R.string.succeed_save_host_config,
								V2Toast.LENGTH_LONG).show();
					}
					dialog.dismiss();
				}
			});

			cancelButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					et.setText(LocalSharedPreferencesStorage.getConfigStrValue(
							mContext, "ip"));
					port.setText(LocalSharedPreferencesStorage
							.getConfigStrValue(mContext, "port"));
					et.setError(null);
					port.setError(null);
					dialog.dismiss();
				}
			});

			dialog.setCancelable(true);
			dialog.setCanceledOnTouchOutside(true);

			mSettingDialog = dialog;
			dialog.show();
		}

	}

	private class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LOG_IN_CALL_BACK:
				isLoggingIn = false;
				JNIResponse rlr = (JNIResponse) msg.obj;
				if (rlr.getResult() == JNIResponse.Result.TIME_OUT) {
					V2Toast.makeText(mContext, R.string.error_time_out,
							V2Toast.LENGTH_LONG).show();
				} else if (rlr.getResult() == JNIResponse.Result.FAILED) {
					mEtPassword
							.setError(getString(R.string.error_incorrect_password));
					mEtPassword.requestFocus();
				} else if (rlr.getResult() == JNIResponse.Result.CONNECT_ERROR) {
					V2Toast.makeText(mContext,
							R.string.error_connect_to_server,
							V2Toast.LENGTH_LONG).show();
				} else if (rlr.getResult() == JNIResponse.Result.NO_RESOURCE) {
					V2Toast.makeText(mContext, R.string.error_no_resource,
							V2Toast.LENGTH_LONG).show();
				} else if (rlr.getResult() == JNIResponse.Result.LOGED_OVER_TIME) {
					V2Toast.makeText(mContext,
							R.string.error_resource_over_time,
							V2Toast.LENGTH_LONG).show();
				} else if (rlr.getResult() == JNIResponse.Result.SERVER_REJECT) {
					V2Toast.makeText(mContext,
							R.string.error_connect_to_server,
							V2Toast.LENGTH_LONG).show();
				} else if (rlr.getResult() == JNIResponse.Result.SUCCESS) {
					// 获取到登陆用户对象
					User user = ((RequestLogInResponse) rlr).getUser();
					String serverID = ((RequestLogInResponse) rlr)
							.getServerID();
					if (user == null || serverID == null) {
						throw new RuntimeException(getResources().getString(
								R.string.login_error_init_user_id));
					}
					// 构建文件夹路径
					GlobalConfig.SERVER_DATABASE_ID = serverID;
					V2Log.d(TAG, "Build folder finish! Globle Path is : "
							+ GlobalConfig.getGlobalPath());
					// 为登陆用户创建个人资料文件夹
					createPersonFolder(user);
					initDataBaseTableCacheNames();
					// Save user info
					saveUserNameAndPasswd(mEtUserName.getText().toString(), "");
					GlobalHolder.getInstance().setCurrentUser(user);
					LocalSharedPreferencesStorage.putIntValue(mContext,
							GlobalConfig.KEY_LOGGED_IN, 1);
					mContext.startActivity(new Intent(mContext,
							MainActivity.class));
					isFward = true;
					finish();
				}
				//关闭等待对话框
				WaitDialogBuilder.showNormalProgress(mContext, false);
				break;
			}
		}
	}
	
	/**
	 * 初始化获取数据库中所有表
	 */
	private void initDataBaseTableCacheNames() {
		DataBaseContext mContext = new DataBaseContext(getApplicationContext());
		V2TechDBHelper mOpenHelper = V2TechDBHelper.getInstance(mContext);
		SQLiteDatabase dataBase = mOpenHelper.getReadableDatabase();
		Cursor cursor = null;
		try {
			cursor = dataBase.rawQuery(
					"select name as c from sqlite_master where type ='table'",
					null);
			if (cursor != null) {
				List<String> dataBaseTableCacheName = GlobalHolder
						.getInstance().getDataBaseTableCacheName();
				while (cursor.moveToNext()) {
					// 遍历出表名
					String name = cursor.getString(0);
					V2Log.d("iteration DataBase table name : " + name);
					dataBaseTableCacheName.add(name);
				}
			} else
				throw new RuntimeException(
						"init DataBase table names failed.. get null , please check");
		} catch (Exception e) {
			throw new RuntimeException(
					"init DataBase table names failed.. get null , please check"
							+ e.getStackTrace());
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}


}
