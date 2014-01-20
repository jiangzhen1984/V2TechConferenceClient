package com.v2tech.view;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import v2av.VideoCaptureDevInfo;
import v2av.VideoPlayer;
import v2av.VideoRecorder;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.logic.User;
import com.v2tech.logic.UserDeviceConfig;
import com.v2tech.view.JNIService.LocalBinder;

public class VideoActivityV2 extends Activity {

	private static final int SERVICE_BUNDED = 0;
	private static final int ONLY_SHOW_LOCAL_VIDEO = 1;
	private static final int FILL_USER_LIST = 2;
	private static final int APPLY_OR_RELEASE_SPEAK = 3;
	private static final int REQUEST_OPEN_DEVICE_RESPONSE = 4;
	private static final int REQUEST_CLOSE_DEVICE_RESPONSE = 5;

	private static final int USER_ENTERED_CONF = 21;
	private static final int USER_EXITED_CONF = 22;
	private static final int CONF_USER_DEVICE_EVENT = 23;
	
	public static final String JNI_EVENT_VIDEO_CATEGORY = "com.v2tech.conf_video_event";
	public static final String JNI_EVENT_VIDEO_CATEGORY_OPEN_VIDEO_EVENT_ACTION = "com.v2tech.conf_video_event.open_video_event";

	
	
	private JNIService mService;
	private boolean isBound;
	private boolean isSpeaking;
	
	
	private Handler mVideoHandler = new VideoHandler();

	private Context mContext;
	private SurfaceView[] mSurfaceViewArr;
	private RelativeLayout mVideoLayout;

	private ImageView mSettingIV;
	private ImageView mListUserIV;
	private ImageView mQuitIV;
	private ImageView mSpeakerIV;
	private PopupWindow mSettingWindow;
	private PopupWindow mUserListWindow;
	private LinearLayout mUserListContainer;
	private Dialog mQuitDialog;
	private static int Measuredwidth = 0;
	private static int Measuredheight = 0;

	private Long mGroupId;
	private Set<Attendee> mAttendeeList = new HashSet<Attendee>();
	private SurfaceView mLocalSurface;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_in_metting);
		mContext = this;
		mSurfaceViewArr = new SurfaceView[] { new SurfaceView(this),
				new SurfaceView(this), new SurfaceView(this),
				new SurfaceView(this) };
		this.mVideoLayout = (RelativeLayout) findViewById(R.id.in_metting_video_main);
		this.mSettingIV = (ImageView) findViewById(R.id.in_meeting_setting_iv);
		this.mSettingIV.setOnClickListener(mShowSettingListener);
		this.mListUserIV = (ImageView) findViewById(R.id.in_meeting_show_attendee_iv);
		this.mListUserIV.setOnClickListener(mShowConfUsersListener);
		this.mQuitIV = (ImageView) findViewById(R.id.in_meeting_log_out_iv);
		this.mQuitIV.setOnClickListener(mShowQuitWindowListener);
		this.mSpeakerIV = (ImageView) findViewById(R.id.speaker_iv);
		this.mSpeakerIV.setOnClickListener(mApplySpeakerListener);
		initConfsListener();
		init();
	}

	private OnClickListener mApplySpeakerListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			if (isSpeaking) {
				mSpeakerIV.setImageResource(R.drawable.speaker);
			} else {
				mSpeakerIV.setImageResource(R.drawable.speaking);
			}
			Message.obtain(mVideoHandler, APPLY_OR_RELEASE_SPEAK).sendToTarget();
		}
	};

	private OnClickListener mShowQuitWindowListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			showQuitDialog();
		}
	};

	private OnClickListener mShowConfUsersListener = new OnClickListener() {

		@Override
		public void onClick(View anchor) {
			if (mUserListWindow == null) {
				LayoutInflater inflater = (LayoutInflater) mContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View view = inflater.inflate(
						R.layout.in_meeting_user_list_pop_up_window, null);
				mUserListContainer = (LinearLayout) view
						.findViewById(R.id.in_meeting_user_list_layout);

				mUserListWindow = new PopupWindow(view,
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				mUserListWindow
						.setBackgroundDrawable(mContext
								.getResources()
								.getDrawable(
										R.drawable.rounded_corners_of_in_metting_setting_pop_up_window));
				mUserListWindow.setFocusable(true);
				mUserListWindow.setTouchable(true);
				mUserListWindow.setOutsideTouchable(true);
				mUserListWindow.setOnDismissListener(new OnDismissListener() {

					@Override
					public void onDismiss() {
						mUserListWindow.dismiss();
					}

				});
				
				
				fillUserList();
			}

			if (!mUserListWindow.isShowing()) {
				mUserListWindow.showAsDropDown(anchor);
			}
		}

	};

	private OnClickListener mShowSettingListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mSettingWindow == null) {
				LayoutInflater inflater = (LayoutInflater) mContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View view = inflater.inflate(
						R.layout.in_meeting_setting_pop_up_window, null);
				mSettingWindow = new PopupWindow(view,
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				mSettingWindow
						.setBackgroundDrawable(mContext
								.getResources()
								.getDrawable(
										R.drawable.rounded_corners_of_in_metting_setting_pop_up_window));
				mSettingWindow.setFocusable(true);
				mSettingWindow.setTouchable(true);
				mSettingWindow.setOutsideTouchable(true);
				mSettingWindow.setOnDismissListener(new OnDismissListener() {

					@Override
					public void onDismiss() {
						mSettingWindow.dismiss();
					}

				});
				
				RelativeLayout ll = (RelativeLayout)view.findViewById(R.id.in_meeting_setting_reverse_camera);
				ll.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						doReverseCamera();
						mSettingWindow.dismiss();
					}
					
				});
			}

			if (!mSettingWindow.isShowing()) {
				mSettingWindow.showAsDropDown(v);
			}

		}

	};

	private BroadcastReceiver mConfUserChangeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					JNI_EVENT_VIDEO_CATEGORY_OPEN_VIDEO_EVENT_ACTION)) {
				if (intent.getIntExtra("result", 1) != 0) {
					Toast.makeText(context,
							R.string.error_in_meeting_open_video_falied,
							Toast.LENGTH_LONG).show();
					;
				}
			} else if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_ATTENDEE_EXITED_NOTIFICATION)
					|| intent.getAction().equals(
							JNIService.JNI_BROADCAST_ATTENDEE_ENTERED_NOTIFICATION)) {
				long uid = intent.getLongExtra("uid", -1);
				String name = intent.getExtras().getString("name");
				if (uid < 0) {
					Toast.makeText(mContext, "Invalid user id",
							Toast.LENGTH_SHORT).show();
					return;
				}
				Message.obtain(
						mVideoHandler,
						intent.getAction()
								.equals(JNIService.JNI_BROADCAST_ATTENDEE_ENTERED_NOTIFICATION) ? USER_ENTERED_CONF
								: USER_EXITED_CONF, new User(uid, name))
						.sendToTarget();

			}
		}

	};

	
	
	
	
	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			isBound = true;
			// Send server bounded message
			Message.obtain(mVideoHandler, SERVICE_BUNDED).sendToTarget();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			isBound = false;
		}
	};
	
	
	
	@Override
	protected void onStart() {
		super.onStart();
		adjustLayout();
		isBound = bindService(new Intent(this.getApplicationContext(),
				JNIService.class), mConnection, Context.BIND_AUTO_CREATE);
	}
	
	
	private void initConfsListener() {
		IntentFilter filter = new IntentFilter();
		filter.addCategory(JNI_EVENT_VIDEO_CATEGORY);
		filter.addAction(JNI_EVENT_VIDEO_CATEGORY_OPEN_VIDEO_EVENT_ACTION);

		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		filter.addAction(JNIService.JNI_BROADCAST_ATTENDEE_EXITED_NOTIFICATION);
		filter.addAction(JNIService.JNI_BROADCAST_ATTENDEE_ENTERED_NOTIFICATION);
		mContext.registerReceiver(mConfUserChangeReceiver, filter);
	}

	
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void init() {
		mGroupId = this.getIntent().getLongExtra("gid", 0);
		if (mGroupId <= 0) {
			Toast.makeText(this, R.string.error_in_meeting_invalid_gid,
					Toast.LENGTH_LONG).show();
			return;
		}

		Point size = new Point();
		WindowManager w = getWindowManager();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			w.getDefaultDisplay().getSize(size);

			Measuredwidth = size.x;
			Measuredheight = size.y;
		} else {
			Display d = w.getDefaultDisplay();
			Measuredwidth = d.getWidth();
			Measuredheight = d.getHeight();
		}

	}

	
	/**
	 * FIXME should use attendee list instead of this 
	 */
	private void showLocalSurViewOnly() {
		mSurfaceViewArr[0].getHolder().setType(
				SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mSurfaceViewArr[0].getHolder().setFormat(PixelFormat.TRANSPARENT);
		VideoRecorder.VideoPreviewSurfaceHolder = mSurfaceViewArr[0]
				.getHolder();
		mService.requestOpenVideoDevice(mGroupId,
				new UserDeviceConfig(mService.getLoggedUserId(), "", null),
				Message.obtain(mVideoHandler, REQUEST_OPEN_DEVICE_RESPONSE));
		mLocalSurface = mSurfaceViewArr[0];
	}

	

	private void adjustLayout() {
		mVideoLayout.removeAllViews();
		int t = mSurfaceViewArr.length / 2;
		RelativeLayout.LayoutParams par = (RelativeLayout.LayoutParams)mVideoLayout.getLayoutParams();
		int iWid = (Measuredwidth - par.leftMargin - par.rightMargin) / t;
		int iHei = (Measuredheight -100) / 2;
		for (int i = 0; i < mSurfaceViewArr.length; i++) {
			mSurfaceViewArr[i].setId(i + 1000);
			RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(
					iWid, iHei);
			int r = i % t;
			if (r == 0) {
				rl.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			} else {
				rl.addRule(RelativeLayout.RIGHT_OF,
						mSurfaceViewArr[i - 1].getId());
			}
			if (i >= t) {
				rl.addRule(RelativeLayout.BELOW, mSurfaceViewArr[i - t].getId());
			}
			mVideoLayout.addView(mSurfaceViewArr[i], rl);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		mVideoLayout.removeAllViews();
		if (isBound) {
			this.unbindService(mConnection);
		}
		quit();
	}

	@Override
	protected void onDestroy() {
		mContext.unregisterReceiver(mConfUserChangeReceiver);
		super.onDestroy();
		mAttendeeList.clear();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mQuitDialog != null && mQuitDialog.isShowing()) {
			mQuitDialog.dismiss();
		}

		if (mSettingWindow != null && mSettingWindow.isShowing()) {
			mSettingWindow.dismiss();
		}
		if (this.mUserListWindow != null && mUserListWindow.isShowing()) {
			mUserListWindow.dismiss();
		}
	}

	private void showQuitDialog() {
		if (mQuitDialog == null) {

			final Dialog d = new Dialog(mContext, R.style.InMeetingQuitDialog);

			d.setContentView(R.layout.in_meeting_quit_window);
			final Button cancelB = (Button) d
					.findViewById(R.id.IMWCancelButton);
			cancelB.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					d.dismiss();
				}

			});
			final Button quitB = (Button) d.findViewById(R.id.IMWQuitButton);
			quitB.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					d.dismiss();
					Intent i = new Intent();
					i.putExtra("gid", mGroupId);
					setResult(0, i);
					finish();
				}

			});
			mQuitDialog = d;
		}

		mQuitDialog.show();
	}

	
	
	
	
	/**
	 * reverse local camera from back to front or from front to back
	 */
	private void doReverseCamera() {
		//FIXME fix sequence bug
		mService.requestCloseVideoDevice(mGroupId, new UserDeviceConfig(
				mService.getLoggedUserId(), "", null), Message.obtain(
				mVideoHandler, REQUEST_CLOSE_DEVICE_RESPONSE));
		VideoCaptureDevInfo.CreateVideoCaptureDevInfo().reverseCamera();
		mService.requestOpenVideoDevice(mGroupId,
				new UserDeviceConfig(mService.getLoggedUserId(), "", null),
				Message.obtain(mVideoHandler, REQUEST_OPEN_DEVICE_RESPONSE));
	}
	
	
	/**
	 * user quit conference, however positive or negative
	 */
	private void quit() {
		for (Attendee at : this.mAttendeeList) {
			if (at.isShowing) {
			mService.requestCloseVideoDevice(mGroupId, at.config, Message.obtain(
					mVideoHandler, REQUEST_CLOSE_DEVICE_RESPONSE));
			}
		}
		
		mService.requestCloseVideoDevice(mGroupId, new UserDeviceConfig(
				mService.getLoggedUserId(), "", null), Message.obtain(
				mVideoHandler, REQUEST_CLOSE_DEVICE_RESPONSE));
		
		VideoRecorder.VideoPreviewSurfaceHolder = null;
		mAttendeeList.clear();		
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showQuitDialog();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}


	private void fillUserList() {
		mUserListContainer.addView(getUserTextView(mService.getloggedUser().getmUserId(), mService.getloggedUser().getName(), true));
		for (Attendee at : mAttendeeList) {
			mUserListContainer.addView(getUserTextView(at.u.getmUserId(), at.u.getName(), false));
		}
	}

	private void doApplyOrReleaseSpeak() {
		if (isSpeaking) {
			mService.applyForReleasePermission(3);
			isSpeaking = false;
		} else {
			// 3 means apply speak
			mService.applyForControlPermission(3);
			isSpeaking = true;
		}
	}
	
	

	/**
	 * Handle event which new user entered conference
	 * 
	 * @param user
	 */
	private void doHandleNewUserEntered(User user) {
		this.mAttendeeList.add(new Attendee(user));

		if (mUserListContainer != null) {
			mUserListContainer.addView(getUserTextView(user.getmUserId(), user.getName(), false));
		}
		Toast.makeText(mContext, user.getName() + "进入会议室! ",
				Toast.LENGTH_SHORT).show();
	}
	
	
	private TextView getUserTextView(final long id, final String name, boolean local) {
		TextView tv = new TextView(mContext);
		tv.setId((int)id);
		tv.setText(name);
		
		tv.setPadding(15, 5, 5, 5);
		if (local ==  false) {
			tv.setOnClickListener(new OnClickListener() {
	
				@Override
				public void onClick(View arg0) {
					showOrCloseAttendeeVideo(id);
					if (mUserListWindow != null) {
						mUserListWindow.dismiss();
					}
				}
				
			});
			tv.setTextSize(20);
		} else {
			tv.setTextSize(22);
			tv.setTypeface(null, Typeface.BOLD);
		}
		return tv;
	}
	
	

	/**
	 * Handle event which user exited conference
	 * 
	 * @param user
	 */
	private void doHandleUserExited(User user) {
		Toast.makeText(mContext, user.getName() + "退出会议室! ",
				Toast.LENGTH_SHORT).show();
		Attendee a = getAttendee(user.getmUserId());
		if (a != null && a.isShowing) {
			mService.requestCloseVideoDevice(mGroupId, a.config, Message.obtain(
					mVideoHandler, REQUEST_CLOSE_DEVICE_RESPONSE));
			a.isShowing = false;
			a.sv = null;
			if (a.config != null) {
				a.config.setVp(null);
				a.config = null;
			}
			if (a.config1 != null) {
				a.config1.setVp(null);
				a.config1 = null;
			}
		}
		this.mAttendeeList.remove(a);
		
		if (mUserListContainer == null) {
			return;
		}
		int count = mUserListContainer.getChildCount();
		for (int i = 0; i < count; i++) {
			View v = mUserListContainer.getChildAt(i);
			if (v instanceof TextView) {
				long uid = ((TextView) v).getId();
				if (uid ==user.getmUserId()) {
					mUserListContainer.removeView(v);
					return;
				}
			}
		}
	}
	

	private void showOrCloseAttendeeVideo(long id) {
		Attendee a = getAttendee(id);
		// if already opened attendee's video, switch action to close
		if (a.isShowing) {
			mService.requestCloseVideoDevice(mGroupId, a.config, Message.obtain(
					mVideoHandler, REQUEST_CLOSE_DEVICE_RESPONSE));
			a.config.setVp(null);
			a.sv = null;
			a.isShowing = false;
			return;
		}
		
		//open video device
		SurfaceView avaiSV = null;
		for (SurfaceView sv: this.mSurfaceViewArr) {
			boolean used = false;
			for(Attendee at : this.mAttendeeList) {
				if (at.sv == sv) {
					used = true;
					break;
				}
			}
			if (used || sv == mLocalSurface) {
				continue;
			} else {
				avaiSV =  sv;
				//break;
			}
		}
		
		if (avaiSV == null) {
			//TODO do up limit max surface view
			return;
		}
		
		
		if (a.config == null) {
			List<UserDeviceConfig>  udl = mService.getAttendeeDevice(id);
			if(udl.size() <= 0) {
				Toast.makeText(mContext, "No Device", Toast.LENGTH_LONG).show();
				return;
			} else {
				a.config = udl.get(0);
			}
			//a.config = new UserDeviceConfig(a.config.getUserID(), a.config.getDeviceID(), null);
		}
		
		VideoPlayer vp = new VideoPlayer();
		vp.SetSurface(avaiSV.getHolder());
		a.sv = avaiSV;
		a.config.setVp(vp);
		mService.requestOpenVideoDevice(mGroupId,
				a.config,
				Message.obtain(mVideoHandler, REQUEST_OPEN_DEVICE_RESPONSE));
		a.isShowing = true;
	}
	
	
	
	private Attendee getAttendee(long uid) {
		Attendee at = null;
		for (Attendee a : this.mAttendeeList) {
			if (uid == a.u.getmUserId()) {
				at = a;
				break;
			}
		}
		return at;
	}
	

	class Attendee {
		User u;
		UserDeviceConfig config;
		UserDeviceConfig config1;
		SurfaceView sv;
		boolean isShowing;
		boolean isShowing1;
		boolean isChairMan;
		boolean isSelf;
		
		
		public Attendee(User u) {
			this(u, null, null);
		}
		
		public Attendee(User u, UserDeviceConfig config) {
			this(u, config, null);
		}
		

		public Attendee(User u, UserDeviceConfig config, SurfaceView sv) {
			if (u == null) {
				throw new RuntimeException(" Incorrect parameter user can't be null");
			}
			this.u = u;
			this.config = config;
			this.sv = sv;
		}

		@Override
		public boolean equals(Object o) {
			return (o == this || u.getmUserId() == ((Attendee)o).u.getmUserId());
		}
		@Override
		public int hashCode() {
			return (int)u.getmUserId();
		}
		
	}
	
	


	class VideoHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SERVICE_BUNDED:
			case ONLY_SHOW_LOCAL_VIDEO:
				showLocalSurViewOnly();
				break;
			case FILL_USER_LIST:
				fillUserList();
				break;
			case APPLY_OR_RELEASE_SPEAK:
				doApplyOrReleaseSpeak();
				break;
			case REQUEST_OPEN_DEVICE_RESPONSE:
			case REQUEST_CLOSE_DEVICE_RESPONSE:
				//TODO open device result
				break;
			case CONF_USER_DEVICE_EVENT:
				//recordUserDevice((ConfUserDeviceInfo) msg.obj);
				break;
			case USER_ENTERED_CONF:
				doHandleNewUserEntered((User)msg.obj);		
				break;
			case USER_EXITED_CONF:
				doHandleUserExited((User) msg.obj);
				break;
			}
		}

	}

}
