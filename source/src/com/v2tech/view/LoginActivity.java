package com.v2tech.view;

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
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
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
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.V2.jni.ConfigRequest;
import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.db.DataBaseContext;
import com.v2tech.db.V2TechDBHelper;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.service.UserService;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestLogInResponse;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.ProgressUtils;
import com.v2tech.util.SPUtil;
import com.v2tech.util.V2Toast;
import com.v2tech.vo.User;

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
	private Boolean isLoggingIn = false;
	
	private final String TAG = "LoginActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		mContext = this;

		// Set up the login form.
		mEmailView = (EditText) findViewById(R.id.email);
		mEmailView.addTextChangedListener(userNameTextWAtcher);
		mEmailView.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View arg0, boolean focus) {
				if (focus) {
					if (mContext.getResources()
							.getText(R.string.login_user_name)
							.equals(mEmailView.getText().toString())) {
						mEmailView.setText("");
					}
					mEmailView.setTextColor(Color.BLACK);
				} else {
					if (mEmailView.getText().toString().trim().isEmpty()) {
						mEmailView.setText(R.string.login_user_name);
						mEmailView
								.setTextColor(mContext
										.getResources()
										.getColor(
												R.color.login_activity_login_box_text_color));
					}
				}
			}
		});

		mEmailView.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView tv, int actionId,
					KeyEvent event) {
				mPasswordView.setInputType(InputType.TYPE_CLASS_TEXT
						| InputType.TYPE_TEXT_VARIATION_PASSWORD);
				return false;
			}

		});

		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View arg0, boolean focus) {
				if (focus) {
					mPasswordView.setTextColor(Color.BLACK);
					if (mContext.getResources()
							.getText(R.string.prompt_password)
							.equals(mPasswordView.getText().toString())) {
						mPasswordView.setText("");
					}
				} else {
					if (mPasswordView.getText().toString().trim().isEmpty()) {
						mPasswordView.setText(R.string.prompt_password);
						mPasswordView
								.setTextColor(mContext
										.getResources()
										.getColor(
												R.color.login_activity_login_box_text_color));
						mPasswordView.setInputType(InputType.TYPE_CLASS_TEXT);
					} else {
						mPasswordView.setInputType(InputType.TYPE_CLASS_TEXT
								| InputType.TYPE_TEXT_VARIATION_PASSWORD);
					}
				}
			}
		});

		mPasswordView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View view, MotionEvent event) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(mPasswordView, InputMethodManager.SHOW_FORCED);
				EditText et = ((EditText) view);
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					et.setInputType(InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_VARIATION_PASSWORD);
					et.requestFocus();
				}
				return true;
			}

		});

		mPasswordView.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				// T
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mEmailView.getWindowToken(), 0);
					imm.hideSoftInputFromWindow(mPasswordView.getWindowToken(),
							0);
					attemptLogin();
				}
				return false;
			}

		});

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
						mPasswordView.setError(null);
						mEmailView.setError(null);
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
		
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		GlobalConfig.SCREEN_WIDTH = dm.widthPixels;
		GlobalConfig.SCREEN_HEIGHT = dm.heightPixels;
		
		String user = SPUtil.getConfigStrValue(this, "user");
		String password = SPUtil.getConfigStrValue(this, "passwd");
		if (user != null && !user.trim().isEmpty()) {
			mEmailView.setText(user);
			mEmailView.setTextColor(Color.BLACK);
		}
		if (password != null && !password.trim().isEmpty()) {
			mPasswordView.setText(password);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		loginView.setVisibility(View.VISIBLE);
		final Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
				this, R.animator.login_container_down_in);
		tabBlockHolderAnimation.setDuration(1000);
		tabBlockHolderAnimation.setFillAfter(true);
		// tabBlockHolderAnimation.setInterpolator(new BounceInterpolator());
		loginView.startAnimation(tabBlockHolderAnimation);
	}
	
	

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		us.clearCalledBack();
	}

	private TextWatcher userNameTextWAtcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable et) {
			if (!et.toString().trim().isEmpty()) {
				mEmailView.setError(null);
			}
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
					if (portStr5 == null || portStr5.isEmpty()) {
						port.setError(mContext
								.getText(R.string.error_field_required));
						return;
					}
					if (!saveHostConfig(ets, portStr5)) {
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
					et.setText(SPUtil.getConfigStrValue(mContext, "ip"));
					port.setText(SPUtil.getConfigStrValue(mContext, "port"));
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
		// getMenuInflater().inflate(R.menu.login, menu);
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
			V2Toast.makeText(mContext, R.string.error_no_host_configuration,
					V2Toast.LENGTH_SHORT).show();
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

		// Check user name is initial user name or not.
		if (mContext.getResources().getText(R.string.login_user_name)
				.equals(mEmail)) {
			mEmailView.setError(getString(R.string.error_field_required));
			mEmailView.requestFocus();
			return;
		}

		// Check password is initial password
		if (mContext.getResources().getText(R.string.prompt_password)
				.equals(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			mPasswordView.requestFocus();
			return;
		}

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
			synchronized (isLoggingIn) {
				if (isLoggingIn) {
					V2Log.w("Current state is logging in");
					return;
				}
				isLoggingIn = true;
				// Show a progress spinner, and kick off a background task to
				// perform the user login attempt.
				mLoginStatusMessageView
						.setText(R.string.login_progress_signing_in);
				ProgressUtils.showNormalProgress(mContext , true);
				us.login(mEmailView.getText().toString(), mPasswordView
						.getText().toString(), new MessageListener(mHandler,
						LOG_IN_CALL_BACK, null));
			}
		}
	}

	private static final int LOG_IN_CALL_BACK = 1;
	private Handler mHandler = new Handler() {

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
					mPasswordView
							.setError(getString(R.string.error_incorrect_password));
					mPasswordView.requestFocus();
				} else if (rlr.getResult() == JNIResponse.Result.CONNECT_ERROR) {
					V2Toast.makeText(mContext, R.string.error_connect_to_server,
							V2Toast.LENGTH_LONG).show();
				} else if (rlr.getResult() == JNIResponse.Result.NO_RESOURCE) {
					V2Toast.makeText(mContext, R.string.error_no_resource,
							V2Toast.LENGTH_LONG).show();
				} else if (rlr.getResult() == JNIResponse.Result.LOGED_OVER_TIME) {
					V2Toast.makeText(mContext, R.string.error_resource_over_time,
							V2Toast.LENGTH_LONG).show();
				}else if (rlr.getResult() == JNIResponse.Result.SERVER_REJECT) {
					V2Toast.makeText(mContext, R.string.error_connect_to_server,
							V2Toast.LENGTH_LONG).show();
				} else if (rlr.getResult() == JNIResponse.Result.SUCCESS){
					//获取到登陆用户对象
					User user = ((RequestLogInResponse) rlr).getUser();
					String serverID = ((RequestLogInResponse) rlr).getServerID();
					if(user == null || serverID == null)
						throw new RuntimeException(getResources().getString(R.string.login_error_init_user_id));
					//构建文件夹路径
					GlobalConfig.SERVER_DATABASE_ID = serverID;
					V2Log.d(TAG, "Build folder finish! Globle Path is : " + GlobalConfig.getGlobalPath());
					//为登陆用户创建个人资料文件夹
					createPersonFolder(user);
					// Save user info
					saveUserConfig(mEmailView.getText().toString(), "");
					GlobalHolder.getInstance().setCurrentUser(
							user);
					SPUtil.putConfigIntValue(mContext,
							GlobalConfig.KEY_LOGGED_IN, 1);
					mContext.startActivity(new Intent(mContext,
							MainActivity.class));
					//init all database table names;
					initDataBaseTableCacheNames();
					finish();
				}
				ProgressUtils.showNormalProgress(mContext , false);
				break;
			}
		}

	};
	
	/**
	 * 初始化获取数据库中所有表
	 */
	private void initDataBaseTableCacheNames() {
		DataBaseContext mContext = new DataBaseContext(getApplicationContext());
		V2TechDBHelper mOpenHelper = new V2TechDBHelper(mContext);
		SQLiteDatabase dataBase = mOpenHelper.getReadableDatabase();
		Cursor cursor = null;
		try{
			cursor = dataBase.rawQuery("select name as c from sqlite_master where type ='table'",
					null);
			if (cursor != null) {
				List<String> dataBaseTableCacheName = GlobalHolder.getInstance()
						.getDataBaseTableCacheName();
				while (cursor.moveToNext()) {
					// 遍历出表名
					String name = cursor.getString(0);
					V2Log.d(TAG , "iteration DataBase table name : " + name);
					dataBaseTableCacheName.add(name);
				}
			} else
				throw new RuntimeException(
						"init DataBase table names failed.. get null , please check");
		}
		catch(Exception e){
			throw new RuntimeException(
					"init DataBase table names failed.. get null , please check" + e.getStackTrace());
		}
		finally{
			if(cursor != null)
				cursor.close();
			
			if(dataBase != null){
				dataBase.close();
			}
		}
	}
	
	/**
	 * 创建登陆用户存储数据的文件夹
	 * @param user
	 */
	private void createPersonFolder(User user){
		
		GlobalConfig.LOGIN_USER_ID = String.valueOf(user.getmUserId());
		
		File pa = new File(GlobalConfig.getGlobalUserAvatarPath());
		if (!pa.exists()) {
			boolean res = pa.mkdirs();
			V2Log.i(" create avatar dir " + pa.getAbsolutePath() + "  " + res);
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

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();

	}

}
