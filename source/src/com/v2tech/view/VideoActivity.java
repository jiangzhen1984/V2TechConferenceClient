package com.v2tech.view;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import v2av.VideoCaptureDevInfo;
import v2av.VideoPlayer;
import v2av.VideoRecorder;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

import com.V2.jni.ConfRequest;
import com.V2.jni.ImRequest;
import com.V2.jni.VideoRequest;
import com.v2tech.R;
import com.v2tech.logic.ConfUserDeviceInfo;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.User;

public class VideoActivity extends Activity {

	private static final int ONLY_SHOW_LOCAL_VIDEO = 1;
	private static final int FILL_USER_LIST = 2;
	private static final int APPLY_SPEAK = 3;
	private static final int FINISH_BY_USER = 9;

	private static final int SEND_ENTER_CONF_EVENT = 10;
	private static final int SEND_EXIT_CONF_EVENT = 11;

	private static final int GET_CONF_USER_LIST = 20;
	private static final int USER_ENTERED_CONF = 21;
	private static final int USER_EXITED_CONF = 22;
	private static final int CONF_USER_DEVICE_EVENT = 23;
	
	private static final int UPDATE_ATTENDEE_INFO = 40;

	public static final String JNI_EVENT_VIDEO_CATEGORY = "com.v2tech.conf_video_event";
	public static final String JNI_EVENT_VIDEO_CATEGORY_OPEN_VIDEO_EVENT_ACTION = "com.v2tech.conf_video_event.open_video_event";

	public static final String JNI_EVENT_CONF_USER_CATEGORY = "com.v2tech.conf_user_event";
	public static final String JNI_EVENT_CONF_USER_CATEGORY_GET_USER_LIST_ACTION = "com.v2tech.conf_user_event.get_user_list";
	public static final String JNI_EVENT_CONF_USER_CATEGORY_NEW_USER_ENTERED_ACTION = "com.v2tech.conf_user_event.new_user_entered";
	public static final String JNI_EVENT_CONF_USER_CATEGORY_USER_EXITED_ACTION = "com.v2tech.conf_user_event.user_exited";
	public static final String JNI_EVENT_CONF_USER_CATEGORY_USER_DEVICE_NOTIFICATION = "com.v2tech.conf_user_event.user_device_notificaiton";
	public static final String JNI_EVENT_CONF_USER_CATEGORY_UPDATE_ATTENDEE_INFO = "com.v2tech.conf_user_event.update_attendee_info";

	private VideoRequest mVideo = VideoRequest.getInstance(this);
	private ConfRequest mCR = ConfRequest.getInstance(this);

	private Handler mVideoHandler = new VideoHandler();

	private Context mContext;
	private SurfaceView[] mSurfaceViewArr;
	private int[] mSurfaceUsedFlag;
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
	private String mDeviceId;
	private Map<Long, UserDeviceHolder> mVPHolder = new HashMap<Long, UserDeviceHolder>();
	private Set<User> mCurrentUserList = new HashSet<User>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_in_metting);
		mContext = this;
		mSurfaceViewArr = new SurfaceView[] { new SurfaceView(this),
				new SurfaceView(this), new SurfaceView(this),
				new SurfaceView(this) };
		mSurfaceUsedFlag = new int[] { 0, 0, 0, 0 };
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
			Message.obtain(mVideoHandler, APPLY_SPEAK).sendToTarget();
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
					// TODO handle open video failed;
					Toast.makeText(context,
							R.string.error_in_meeting_open_video_falied,
							Toast.LENGTH_LONG).show();
					;
				}
			} else if (intent.getAction().equals(
					JNI_EVENT_CONF_USER_CATEGORY_NEW_USER_ENTERED_ACTION)
					|| intent.getAction().equals(
							JNI_EVENT_CONF_USER_CATEGORY_USER_EXITED_ACTION)) {
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
								.equals(JNI_EVENT_CONF_USER_CATEGORY_NEW_USER_ENTERED_ACTION) ? USER_ENTERED_CONF
								: USER_EXITED_CONF, new User(uid, name))
						.sendToTarget();

			} else if (intent.getAction().equals(
					JNI_EVENT_CONF_USER_CATEGORY_USER_DEVICE_NOTIFICATION)) {
				ConfUserDeviceInfo cud = (ConfUserDeviceInfo) intent
						.getExtras().get("ud");
				Message.obtain(mVideoHandler, CONF_USER_DEVICE_EVENT, cud)
						.sendToTarget();
			} else if(intent.getAction().equals(JNI_EVENT_CONF_USER_CATEGORY_UPDATE_ATTENDEE_INFO)) {
				User u = new User();
				u.setmUserId(intent.getExtras().getLong("uid"));
				u.setName(intent.getExtras().getString("name"));
				if (u.getmUserId() ==  GlobalHolder.getLoggedUserId()) {
					return;
				}
				Message.obtain(mVideoHandler, UPDATE_ATTENDEE_INFO, u)
				.sendToTarget();
				
			}
		}

	};

	private void initConfsListener() {
		IntentFilter filter = new IntentFilter();
		filter.addCategory(JNI_EVENT_VIDEO_CATEGORY);
		filter.addAction(JNI_EVENT_VIDEO_CATEGORY_OPEN_VIDEO_EVENT_ACTION);

		filter.addCategory(JNI_EVENT_CONF_USER_CATEGORY);
		filter.addAction(JNI_EVENT_CONF_USER_CATEGORY_GET_USER_LIST_ACTION);
		filter.addAction(JNI_EVENT_CONF_USER_CATEGORY_NEW_USER_ENTERED_ACTION);
		filter.addAction(JNI_EVENT_CONF_USER_CATEGORY_USER_EXITED_ACTION);
		filter.addAction(JNI_EVENT_CONF_USER_CATEGORY_USER_DEVICE_NOTIFICATION);
		filter.addAction(JNI_EVENT_CONF_USER_CATEGORY_UPDATE_ATTENDEE_INFO);
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

		mDeviceId = this.getIntent().getExtras().getString("did");
		if (mDeviceId == null || mDeviceId.equals("")) {
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

	private void showLocalSurViewOnly() {
		mSurfaceViewArr[0].getHolder().setType(
				SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mSurfaceViewArr[0].getHolder().setFormat(PixelFormat.TRANSPARENT);
		VideoRecorder.VideoPreviewSurfaceHolder = mSurfaceViewArr[0]
				.getHolder();
		mVideo.openVideoDevice(mGroupId, GlobalHolder.getLoggedUserId(), "",
				null, 1);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Message.obtain(mVideoHandler, SEND_ENTER_CONF_EVENT).sendToTarget();
		adjustLayout();
		Message.obtain(mVideoHandler, ONLY_SHOW_LOCAL_VIDEO).sendToTarget();
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
		Message.obtain(mVideoHandler, SEND_EXIT_CONF_EVENT).sendToTarget();
		mVideoLayout.removeAllViews();
	}

	@Override
	protected void onDestroy() {
		mContext.unregisterReceiver(mConfUserChangeReceiver);
		super.onDestroy();
		mVPHolder.clear();
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
					Message.obtain(mVideoHandler, FINISH_BY_USER).sendToTarget();
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
		mVideo.closeVideoDevice(mGroupId, GlobalHolder.getLoggedUserId(), "",
				null, 1);
		VideoCaptureDevInfo.CreateVideoCaptureDevInfo().reverseCamera();
		mVideo.openVideoDevice(mGroupId, GlobalHolder.getLoggedUserId(), "",
				null, 1);
	}
	
	
	/**
	 * user quit conference, however positive or negative
	 */
	private void quit() {
		for (Map.Entry<Long, UserDeviceHolder> entry : this.mVPHolder
				.entrySet()) {
			UserDeviceHolder holder = entry.getValue();
			mVideo.closeVideoDevice(mGroupId, holder.getUserId(),
					holder.getDeviceId(), holder.getVp(), 1);
		}
		mVideo.closeVideoDevice(mGroupId, GlobalHolder.getLoggedUserId(), "",
				null, 1);
		VideoRecorder.VideoPreviewSurfaceHolder = null;
		mVPHolder.clear();

		mCR.exitConf(mGroupId);

		Intent i = new Intent();
		i.putExtra("error_msg", "退出失败");
		setResult(0, i);
		finish();
	}

	private void getConfsUserList() {
		mCR.getConfUserList(this.mGroupId);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showQuitDialog();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	// TODO do log out

	private void fillUserList() {
		mUserListContainer.addView(getUserTextView(GlobalHolder.getLoggedUserId(), GlobalHolder.getInstance().getUser().getName(), true));
		for (User u : mCurrentUserList) {
			mUserListContainer.addView(getUserTextView(u.getmUserId(), u.getName(), false));
		}
	}

	private void doApplySpeak() {
		// 3 means apply speak
		mCR.applyForControlPermission(3);
	}

	/**
	 * Handle event which new user entered conference
	 * 
	 * @param user
	 */
	private void doHandleNewUserEntered(User user) {
		mCurrentUserList.add(user);

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
					showAttendeeVideo(mD.get(id));
				}
				
			});
			tv.setTextSize(20);
		} else {
			tv.setTextSize(22);
			tv.setTypeface(null, Typeface.BOLD);
		}
		return tv;
	}
	
	
	private void closeUserDevice(User user) {
		UserDeviceHolder h = mVPHolder.get(user.getmUserId());
		if (h !=null && h.getDeviceId() != null) {
			mVideo.closeVideoDevice(mGroupId, h.getUserId(), h.getDeviceId(),
					h.getVp(), 1);
			mVPHolder.remove(user.getmUserId());
			mSurfaceUsedFlag[h.getSurIndex()] = 0;
			h.clearReference();
		}
	}

	/**
	 * Handle event which user exited conference
	 * 
	 * @param user
	 */
	private void doHandleUserExited(User user) {
		Toast.makeText(mContext, user.getmUserId() + "退出会议室! ",
				Toast.LENGTH_SHORT).show();

		UserDeviceHolder h = mVPHolder.get(user.getmUserId());
		if (h !=null && h.getDeviceId() != null) {
			mVideo.closeVideoDevice(mGroupId, h.getUserId(), h.getDeviceId(),
					h.getVp(), 1);
			mVPHolder.remove(user.getmUserId());
			mSurfaceUsedFlag[h.getSurIndex()] = 0;
			h.clearReference();
		}

		mCurrentUserList.remove(user);
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
					// TODO close
					return;
				}
			}
		}
	}
	
	private Map<Long, ConfUserDeviceInfo> mD = new HashMap<Long, ConfUserDeviceInfo>();
	private void recordUserDevice(ConfUserDeviceInfo d) {
		mD.put(d.getUserID(), d);
	}

	private void showAttendeeVideo(ConfUserDeviceInfo d) {
		// close Device
		if (mVPHolder.get(d.getUserID()) != null) {
			
			UserDeviceHolder h = mVPHolder.get(d.getUserID());
			if (h !=null && h.getDeviceId() != null) {
				mVideo.closeVideoDevice(mGroupId, h.getUserId(), h.getDeviceId(),
						h.getVp(), 1);
				mVPHolder.remove(d.getUserID());
				mSurfaceUsedFlag[h.getSurIndex()] = 0;
				h.clearReference();
			}
			return;
		}
		
		//open device
		// index 0 always as local
		for (int i = 1; i < mSurfaceUsedFlag.length; i++) {
			if (mSurfaceUsedFlag[i] == 0) {
				VideoPlayer vp = new VideoPlayer();
				vp.SetSurface(this.mSurfaceViewArr[i].getHolder());
				mVideo.openVideoDevice(mGroupId, d.getUserID(),
						d.getDefaultDeviceId(), vp, 1);
				this.mVPHolder.put(
						d.getUserID(),
						new UserDeviceHolder(d.getUserID(), d
								.getDefaultDeviceId(), vp, mSurfaceViewArr[i], i));
				mSurfaceUsedFlag[i] = 1;
				return;
			}
		}
		// TODO max support video
	}

	class UserDeviceHolder {
		private VideoPlayer vp;
		private long userId;
		private String deviceId;
		private SurfaceView sv;
		private int surIndex;

		public UserDeviceHolder(long userId, String deviceId, VideoPlayer vp, SurfaceView sv, int surIndex) {
			super();
			this.vp = vp;
			this.userId = userId;
			this.deviceId = deviceId;
			this.sv = sv;
			this.surIndex = surIndex;
		}
		

		public SurfaceView getSv() {
			return sv;
		}


		public VideoPlayer getVp() {
			return vp;
		}

		public long getUserId() {
			return userId;
		}

		public String getDeviceId() {
			return deviceId;
		}
		
		
		public int getSurIndex() {
			return surIndex;
		}


		public void clearReference() {
			this.vp = null;
			this.userId = -1;
			this.deviceId = null;
			this.sv = null;
		}

	}

	class VideoHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ONLY_SHOW_LOCAL_VIDEO:
				showLocalSurViewOnly();
				break;
			case FILL_USER_LIST:
				fillUserList();
				break;
			case APPLY_SPEAK:
				doApplySpeak();
				break;
			case CONF_USER_DEVICE_EVENT:
				//showAttendeeVideo((ConfUserDeviceInfo) msg.obj);
				recordUserDevice((ConfUserDeviceInfo) msg.obj);
				break;
			case SEND_ENTER_CONF_EVENT:
				mCR.enterConf(mGroupId);
				break;
			case GET_CONF_USER_LIST:
				getConfsUserList();
				break;
			case USER_ENTERED_CONF:
				//doHandleNewUserEntered((User)msg.obj);
				User u = (User) msg.obj;
				ImRequest.getInstance().getUserBaseInfo(u.getmUserId());				
				break;
			case USER_EXITED_CONF:
				doHandleUserExited((User) msg.obj);
				break;
			case UPDATE_ATTENDEE_INFO:
				doHandleNewUserEntered((User)msg.obj);
				break;
			case SEND_EXIT_CONF_EVENT:
			case FINISH_BY_USER:
				quit();
				break;
			}
		}

	}

}
