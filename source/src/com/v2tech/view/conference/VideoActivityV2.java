package com.v2tech.view.conference;

import java.util.ArrayList;
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
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
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
import android.view.ViewGroup;
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
import com.v2tech.logic.Attendee;
import com.v2tech.logic.CameraConfiguration;
import com.v2tech.logic.ConferencePermission;
import com.v2tech.logic.User;
import com.v2tech.logic.UserDeviceConfig;
import com.v2tech.util.V2Log;
import com.v2tech.view.JNIService;
import com.v2tech.view.JNIService.LocalBinder;

public class VideoActivityV2 extends Activity {

	private static final int SERVICE_BUNDED = 0;
	private static final int ONLY_SHOW_LOCAL_VIDEO = 1;
	private static final int FILL_USER_LIST = 2;
	private static final int APPLY_OR_RELEASE_SPEAK = 3;
	private static final int REQUEST_OPEN_DEVICE_RESPONSE = 4;
	private static final int REQUEST_CLOSE_DEVICE_RESPONSE = 5;
	private static final int REQUEST_OPEN_OR_CLOSE_DEVICE = 6;

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
	private List<SurfaceViewW> mCurrentShowedSV;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_in_metting);
		mContext = this;
		this.mCurrentShowedSV = new ArrayList<SurfaceViewW>();
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
			Message.obtain(mVideoHandler, APPLY_OR_RELEASE_SPEAK)
					.sendToTarget();
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
				view.findViewById(R.id.arrow).bringToFront();

				mUserListWindow = new PopupWindow(view,
						ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.WRAP_CONTENT, true);
				mUserListWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
				
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
				mUserListWindow.showAsDropDown(anchor, -50, 0);
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
						.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
				mSettingWindow.setFocusable(true);
				mSettingWindow.setTouchable(true);
				mSettingWindow.setOutsideTouchable(true);
				mSettingWindow.setOnDismissListener(new OnDismissListener() {

					@Override
					public void onDismiss() {
						mSettingWindow.dismiss();
					}

				});

				RelativeLayout ll = (RelativeLayout) view
						.findViewById(R.id.in_meeting_setting_reverse_camera);
				ll.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						doReverseCamera();
						mSettingWindow.dismiss();
					}

				});
			}

			if (!mSettingWindow.isShowing()) {
				mSettingWindow.showAsDropDown(v, -50, 0);
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
					|| intent
							.getAction()
							.equals(JNIService.JNI_BROADCAST_ATTENDEE_ENTERED_NOTIFICATION)) {
				long uid = intent.getLongExtra("uid", -1);
				String name = intent.getExtras().getString("name");
				if (uid < 0) {
					Toast.makeText(mContext, "Invalid user id",
							Toast.LENGTH_SHORT).show();
					return;
				}
				sendAttendActionMessage(intent.getAction(), new User(uid, name));

			}
		}

	};

	/** Defines callback for service binding, passed to bindService() */
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
		Intent i = mContext.registerReceiver(mConfUserChangeReceiver, filter);
		if (i != null && i.getAction().equals(JNIService.JNI_BROADCAST_ATTENDEE_ENTERED_NOTIFICATION) ) {
			V2Log.i(" get stickly intent");
			long uid = i.getLongExtra("uid", -1);
			String name = i.getExtras().getString("name");
			if (uid < 0) {
				Toast.makeText(mContext, "Invalid user id",
						Toast.LENGTH_SHORT).show();
				return;
			}
			sendAttendActionMessage(i.getAction(), new User(uid, name));
		}
		
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
	 * 
	 * @param action
	 * @param u
	 */
	private void sendAttendActionMessage(String action, User u) {
		V2Log.i(" send attendee action "+ action);
		Message.obtain(
				mVideoHandler,
				action
						.equals(JNIService.JNI_BROADCAST_ATTENDEE_ENTERED_NOTIFICATION) ? USER_ENTERED_CONF
						: USER_EXITED_CONF, u)
				.sendToTarget();
	}
	
	
	/**
	 * 
	 */
	private void showOrCloseLocalSurViewOnly() {
		boolean selfInAttendeeList = false;
		Attendee atd = null;
		UserDeviceConfig udc = null;
		for (Attendee a : mAttendeeList) {
			if (a.isSelf()) {
				selfInAttendeeList = true;
				atd = a;
				break;
			}
		}

		if (mService.getloggedUser() == null) {
			//TODO no user
			return;
		}
		if (selfInAttendeeList == false) {
			atd = new Attendee(mService.getloggedUser(), true, false);
			mAttendeeList.add(atd);
		}

		udc = atd.getDefaultDevice();
		// Add default device
		if (udc == null) {
			udc = new UserDeviceConfig(atd.getUser().getmUserId(), "", null);
			atd.addDevice(udc);
		}

		// layout must before open device
		showOrCloseAttendeeVideo(udc);
	}

	private void adjustLayout() {
		int maxWidth = 2;
		int marginTop = 0;
		int marginBottom = 0;
		int marginLeft = 0;
		int marginRight = 0;
		int size = mCurrentShowedSV.size();
		int rows = size / maxWidth + (size % maxWidth == 0 ? 0 : 1);
		if (size == 0) {
			V2Log.e(" No surface to show");
		}
		RelativeLayout.LayoutParams par = (RelativeLayout.LayoutParams) mVideoLayout
				.getLayoutParams();
		int fixedWidth = (Measuredwidth - par.leftMargin - par.rightMargin)
				/ (size > 1 ? maxWidth : 1);
		int fixedHeight = (Measuredheight - 100)
				/ (size > maxWidth ? maxWidth : 1);
		if (size > 1) {

			fixedHeight = fixedWidth = fixedWidth > fixedHeight ? fixedHeight
					: fixedWidth;
			marginTop = marginBottom = Math.abs((Measuredheight - 100)
					- fixedHeight * rows) / 2;
			marginLeft = marginRight = Math
					.abs((Measuredwidth - par.leftMargin - par.rightMargin)
							- fixedWidth * maxWidth) / 2;
		}
		int index = 0;
		for (SurfaceViewW sw : mCurrentShowedSV) {
			View v = mVideoLayout.findViewById(sw.getView().getId());
			RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
					fixedWidth, fixedHeight);
			int row = index / maxWidth;
			int column = index % maxWidth;
			if (column == 0 && row == 0) {
				p.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				p.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			} else {

				if (column > 0) {
					p.addRule(RelativeLayout.RIGHT_OF,
							mCurrentShowedSV.get(column - 1).getView().getId());
				} else {
					p.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				}
				if (row > 0) {
					p.addRule(RelativeLayout.BELOW,
							mCurrentShowedSV.get((row - 1) * maxWidth + column)
									.getView().getId());
				} else {
					p.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				}

			}
			if (column == 0) {
				p.leftMargin = marginLeft;
			}
			if (row == 0) {

				p.topMargin = marginTop;
			}
			if (column - 1 == maxWidth) {
				p.rightMargin = marginRight;
			}
			if (row == rows) {
				p.bottomMargin = marginBottom;
			}

			if (v != null) {
				mVideoLayout.updateViewLayout(v, p);
			} else {
				if (sw.getView().getParent() != null) {
					((ViewGroup)sw.getView().getParent()).removeView(sw.getView());
				}
				mVideoLayout.addView(sw.getView(), p);
			}
			if (sw.udc.getVp() != null) {
				sw.udc.getVp().SetViewSize(fixedWidth, fixedHeight);
			}
			index++;
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
		finish();
	}

	@Override
	protected void onDestroy() {
		mContext.unregisterReceiver(mConfUserChangeReceiver);
		super.onDestroy();
		mAttendeeList.clear();
		if (mCurrentShowedSV != null) {
			mCurrentShowedSV.clear();
		}
		mVideoLayout.removeAllViews();
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

		for (SurfaceViewW sw : this.mCurrentShowedSV) {
			if (sw.udc.getBelongsAttendee().isSelf()) {
//				mService.requestCloseVideoDevice(mGroupId, sw.udc, Message
//						.obtain(mVideoHandler, REQUEST_CLOSE_DEVICE_RESPONSE));
//				try {
//					Thread.currentThread().sleep(200);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
				VideoCaptureDevInfo.CreateVideoCaptureDevInfo().reverseCamera();
//				mService.requestOpenVideoDevice(mGroupId, sw.udc, Message
//						.obtain(mVideoHandler, REQUEST_OPEN_DEVICE_RESPONSE));
				mService.updateCameraParameters(new CameraConfiguration(""), null);
				return;
			}
		}

		Toast.makeText(mContext, R.string.error_does_not_open_local_video_yet,
				Toast.LENGTH_SHORT).show();

	}

	/**
	 * user quit conference, however positive or negative
	 */
	private void quit() {
		for (SurfaceViewW sw : this.mCurrentShowedSV) {
			Message.obtain(mVideoHandler, REQUEST_OPEN_OR_CLOSE_DEVICE, 0, 0,
					sw.udc).sendToTarget();
			// mService.requestCloseVideoDevice(mGroupId, sw.udc,
			// Message.obtain(
			// mVideoHandler, REQUEST_CLOSE_DEVICE_RESPONSE));
		}

		VideoRecorder.VideoPreviewSurfaceHolder = null;
		mAttendeeList.clear();
		mCurrentShowedSV.clear();
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
		for (Attendee at : mAttendeeList) {
			mUserListContainer.addView(getAttendeeView(at));
		}
	}

	private void doApplyOrReleaseSpeak() {
		if (isSpeaking) {
			mService.applyForReleasePermission(ConferencePermission.SPEAKING, null);
			isSpeaking = false;
		} else {
			// 3 means apply speak
			mService.applyForControlPermission(ConferencePermission.SPEAKING, null);
			isSpeaking = true;
		}
	}

	/**
	 * Handle event which new user entered conference
	 * 
	 * @param user
	 */
	private void doHandleNewUserEntered(User user) {
		Attendee a = new Attendee(user);
		this.mAttendeeList.add(a);
		List<UserDeviceConfig> ld = mService.getAttendeeDevice(a.getUser()
				.getmUserId());
		if (ld == null || ld.size() <= 0) {
			V2Log.w(" No available device config for user:" + user.getmUserId()
					+ "  name:" + user.getName());
		}
		for (UserDeviceConfig udc : ld) {
			a.addDevice(udc);
		}

		if (mUserListContainer != null) {
			mUserListContainer.addView(getAttendeeView(a));
		}
		Toast.makeText(mContext, user.getName() + "进入会议室! ", Toast.LENGTH_SHORT)
				.show();
	}

	/**
	 * FIXME use stand alone view object
	 * 
	 * @param a
	 * @return
	 */
	private View getAttendeeView(final Attendee a) {
		LinearLayout root = new LinearLayout(mContext);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setId((int) a.getUser().getmUserId());

		RelativeLayout rl = new RelativeLayout(mContext);
		rl.setBackgroundColor(Color.WHITE);

		TextView tv = new TextView(mContext);
		final ImageView iv = new ImageView(mContext);
		tv.setTextColor(Color.BLACK);
		tv.setText(a.getUser().getName());
		
		if (a.isSelf() == false) {
			tv.setTextSize(20);
			iv.setImageResource(R.drawable.camera);
			iv.setTag("camera");
		} else {
			tv.setTextSize(22);
			tv.setTypeface(null, Typeface.BOLD);
			iv.setImageResource(R.drawable.camera_showing);
			iv.setTag("camera_showing");
		}

		RelativeLayout.LayoutParams rp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		rp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		rp.addRule(RelativeLayout.CENTER_VERTICAL);
		rp.leftMargin = 20;
		rl.addView(tv, rp);
		
		
		
		rl.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (iv.getTag().toString().equals("camera_showing")) {
					iv.setImageResource(R.drawable.camera);
					iv.setTag("camera");
				} else {
					iv.setImageResource(R.drawable.camera_showing);
					iv.setTag("camera_showing");
				}
				showOrCloseAttendeeVideo(a.getDefaultDevice());
				if (mUserListWindow != null) {
					mUserListWindow.dismiss();
				}
			}
		});
		
		
		
		// add secondary video
		RelativeLayout.LayoutParams rpIv = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		rpIv.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		rpIv.addRule(RelativeLayout.CENTER_VERTICAL);
		rpIv.rightMargin = 20;
		rl.addView(iv, rpIv);

		root.addView(rl, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);

		for (int i = 1; i < a.getmDevices().size(); i++) {
			RelativeLayout rlm = new RelativeLayout(mContext);

			final UserDeviceConfig udc = a.getmDevices().get(i);
			TextView tt = new TextView(mContext);
			tt.setText(a.getUser().getName() + (i > 0 ? ("_视频" + i) : ""));
			tt.setTextSize(20);
			tt.setTextColor(Color.BLACK);

			RelativeLayout.LayoutParams rp1 = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);

			rp1.leftMargin = 20;
			rlm.addView(tt, rp1);

			final ImageView ivm = new ImageView(mContext);
			ivm.setImageResource(R.drawable.camera);
			ivm.setTag("camera");
			rlm.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (ivm.getTag().toString().equals("camera_showing")) {
						ivm.setImageResource(R.drawable.camera);
						ivm.setTag("camera");
					} else {
						ivm.setImageResource(R.drawable.camera_showing);
						ivm.setTag("camera_showing");
					}
					showOrCloseAttendeeVideo(udc);
					if (mUserListWindow != null) {
						mUserListWindow.dismiss();
					}
				}
			});

			rlm.addView(ivm, rpIv);

			root.addView(rlm, lp);
		}

		return root;
	}

	/**
	 * Handle event which user exited conference
	 * 
	 * @param user
	 */
	private void doHandleUserExited(User user) {
		Toast.makeText(mContext, user.getName() + "退出会议室! ", Toast.LENGTH_SHORT)
				.show();
		Attendee a = getAttendee(user.getmUserId());
		if (a != null) {
			for (UserDeviceConfig udc : a.getmDevices()) {
				if (udc.isShowing()) {
					showOrCloseAttendeeVideo(udc);
				}
			}
			this.mAttendeeList.remove(a);
		}

		if (mUserListContainer == null) {
			return;
		}
		int count = mUserListContainer.getChildCount();
		for (int i = 0; i < count; i++) {
			View v = mUserListContainer.getChildAt(i);
			if (v.getId() == (int) user.getmUserId()) {
				mUserListContainer.removeView(v);
				break;
			}
		}
	}

	private void showOrCloseAttendeeVideo(UserDeviceConfig udc) {
		if (udc == null) {
			V2Log.e(" can't not open or close device");
			return;
		}
		// if already opened attendee's video, switch action to close
		if (udc.isShowing()) {
			mService.requestCloseVideoDevice(mGroupId, udc, Message.obtain(
					mVideoHandler, REQUEST_CLOSE_DEVICE_RESPONSE));

			for (SurfaceViewW sw : mCurrentShowedSV) {
				if (sw.udc == udc) {
					mCurrentShowedSV.remove(sw);
					mVideoLayout.removeView(sw.getView());
					sw.rl.removeAllViews();
					break;
				}
			}

			// if (udc.getBelongsAttendee().isSelf()) {
			// VideoRecorder.VideoPreviewSurfaceHolder = null;
			// }
			udc.doClose();
			adjustLayout();
		} else {
			if (!udc.getBelongsAttendee().isSelf()) {
				VideoPlayer vp = new VideoPlayer();
				udc.setSVHolder(new SurfaceView(this));
				vp.SetSurface(udc.getSVHolder().getHolder());
				udc.setVp(vp);
			} else {
				udc.setSVHolder(new SurfaceView(this));
				VideoRecorder.VideoPreviewSurfaceHolder = udc.getSVHolder()
						.getHolder();
				VideoRecorder.VideoPreviewSurfaceHolder
						.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
				VideoRecorder.VideoPreviewSurfaceHolder
						.setFormat(PixelFormat.TRANSPARENT);
			}
			mCurrentShowedSV
					.add(new SurfaceViewW(udc.getBelongsAttendee(), udc));
			// Do adjust layout first, then request open device.
			// otherwise can't show video
			adjustLayout();

			Message m = Message.obtain(mVideoHandler,
					REQUEST_OPEN_OR_CLOSE_DEVICE, 1, 0, udc);
			mVideoHandler.sendMessageDelayed(m, 300);
			udc.setShowing(true);
		}

	}

	private Attendee getAttendee(long uid) {
		Attendee at = null;
		for (Attendee a : this.mAttendeeList) {
			if (uid == a.getUser().getmUserId()) {
				at = a;
				break;
			}
		}
		return at;
	}

	class SurfaceViewW {

		Attendee at;
		UserDeviceConfig udc;
		int layId;
		RelativeLayout rl;

		public SurfaceViewW(Attendee at, UserDeviceConfig udc) {
			this.at = at;
			this.udc = udc;
		}

		public View getView() {
			if (rl == null) {
				rl = new RelativeLayout(mContext);
				//FIXME make sure hash code is unique.
				layId = (int) udc.hashCode();

				if (udc.getSVHolder() == null) {
					udc.setSVHolder(new SurfaceView(mContext));
				}
				rl.addView(udc.getSVHolder(), new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT,
						RelativeLayout.LayoutParams.MATCH_PARENT));
				TextView tv = new TextView(mContext);
				tv.setText(at.getUser().getName());
				tv.setBackgroundColor(Color.rgb(138, 138, 138));
				tv.setPadding(10, 10, 10, 10);
				tv.setTextSize(20);
				RelativeLayout.LayoutParams tvrl = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.WRAP_CONTENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);
				tvrl.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				tvrl.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				rl.addView(tv, tvrl);
				rl.setId(layId);
			}
			return rl;
		}
	}

	class VideoHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			if (isBound == false || mService == null) {
				V2Log.w(" service not bound yet");
				Message m = Message.obtain(msg);
				this.sendMessageDelayed(m, 400);
				return;
			}
			switch (msg.what) {
			case SERVICE_BUNDED:
			case ONLY_SHOW_LOCAL_VIDEO:
				showOrCloseLocalSurViewOnly();
				break;
			case FILL_USER_LIST:
				fillUserList();
				break;
			case APPLY_OR_RELEASE_SPEAK:
				doApplyOrReleaseSpeak();
				break;
			case REQUEST_OPEN_DEVICE_RESPONSE:
			case REQUEST_CLOSE_DEVICE_RESPONSE:
				// TODO open device result
				break;
			case CONF_USER_DEVICE_EVENT:
				// recordUserDevice((ConfUserDeviceInfo) msg.obj);
				break;
			case USER_ENTERED_CONF:
				doHandleNewUserEntered((User) msg.obj);
				break;
			case USER_EXITED_CONF:
				doHandleUserExited((User) msg.obj);
				break;
			case REQUEST_OPEN_OR_CLOSE_DEVICE:
				if (msg.arg1 == 0) {
					mService.requestCloseVideoDevice(mGroupId,
							(UserDeviceConfig) msg.obj, Message.obtain(
									mVideoHandler,
									REQUEST_CLOSE_DEVICE_RESPONSE));
				} else if (msg.arg1 == 1) {
					mService.requestOpenVideoDevice(mGroupId,
							(UserDeviceConfig) msg.obj, Message
									.obtain(mVideoHandler,
											REQUEST_OPEN_DEVICE_RESPONSE));
				}
				break;
			}
		}

	}

}
