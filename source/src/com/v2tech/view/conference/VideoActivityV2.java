package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import v2av.VideoCaptureDevInfo;
import v2av.VideoPlayer;
import v2av.VideoRecorder;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.service.ChatService;
import com.v2tech.service.ConferenceService;
import com.v2tech.service.DocumentService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.PermissionUpdateIndication;
import com.v2tech.service.jni.RequestEnterConfResponse;
import com.v2tech.util.V2Log;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.bo.GroupUserObject;
import com.v2tech.view.bo.UserStatusObject;
import com.v2tech.view.widget.MixVideoLayout;
import com.v2tech.vo.Attendee;
import com.v2tech.vo.AttendeeMixedDevice;
import com.v2tech.vo.CameraConfiguration;
import com.v2tech.vo.Conference;
import com.v2tech.vo.ConferenceGroup;
import com.v2tech.vo.ConferencePermission;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.Group;
import com.v2tech.vo.MixVideo;
import com.v2tech.vo.NetworkStateCode;
import com.v2tech.vo.PermissionState;
import com.v2tech.vo.User;
import com.v2tech.vo.UserDeviceConfig;
import com.v2tech.vo.V2Doc;
import com.v2tech.vo.V2Doc.Page;
import com.v2tech.vo.V2ShapeMeta;
import com.v2tech.vo.VMessage;

public class VideoActivityV2 extends Activity {

	private static final int ONLY_SHOW_LOCAL_VIDEO = 1;
	private static final int APPLY_OR_RELEASE_SPEAK = 3;
	private static final int REQUEST_OPEN_DEVICE_RESPONSE = 4;
	private static final int REQUEST_CLOSE_DEVICE_RESPONSE = 5;
	private static final int REQUEST_OPEN_OR_CLOSE_DEVICE = 6;
	private static final int NOTIFICATION_KICKED = 7;
	private static final int REQUEST_ENTER_CONF = 8;
	private static final int REQUEST_ENTER_CONF_RESPONSE = 9;
	private static final int REQUEST_EXIT_CONF = 10;
	private static final int NOTIFY_USER_PERMISSION_UPDATED = 11;

	private static final int ATTENDEE_LISTENER = 21;
	private static final int CONF_USER_DEVICE_EVENT = 23;
	private static final int USER_DELETE_GROUP = 24;
	private static final int GROUP_ADD_USER = 25;

	private static final int NEW_DOC_NOTIFICATION = 50;
	private static final int DOC_PAGE_NOTIFICATION = 51;
	private static final int DOC_PAGE_ACTIVITE_NOTIFICATION = 52;
	private static final int DOC_PAGE_ADDED_NOTIFICATION = 53;
	private static final int DOC_DOWNLOADED_NOTIFICATION = 54;
	private static final int DOC_CLOSED_NOTIFICATION = 55;
	private static final int DOC_PAGE_CANVAS_NOTIFICATION = 56;
	private static final int DESKTOP_SYNC_NOTIFICATION = 57;

	private static final int VIDEO_MIX_NOTIFICATION = 70;

	public static final String JNI_EVENT_VIDEO_CATEGORY = "com.v2tech.conf_video_event";
	public static final String JNI_EVENT_VIDEO_CATEGORY_OPEN_VIDEO_EVENT_ACTION = "com.v2tech.conf_video_event.open_video_event";

	private boolean isSpeaking;

	private Handler mVideoHandler = new VideoHandler();

	private Context mContext;
	private List<SurfaceViewW> mCurrentShowedSV;
	private RelativeLayout mVideoLayoutMain;
	private RelativeLayout mVideoLayout;

	private TextView mGroupNameTV;
	private ImageView mSettingIV;
	private ImageView mQuitIV;
	private ImageView mSpeakerIV;
	private PopupWindow mSettingWindow;
	private Dialog mQuitDialog;
	private ProgressDialog mWaitingDialog;
	private VideoMsgChattingLayout mMessageContainer;
	private VideoAttendeeListLayout mAttendeeContainer;
	private VideoDocLayout mDocContainer;

	private ImageView mMenuButton;
	private LinearLayout mMenuButtonContainer;
	private LinearLayout mMenuLine;
	private LinearLayout mVideoLine;

	private View mMenuMessageButton;
	private View mMenuAttendeeButton;
	private View mMenuDocButton;
	private View mConverseLocalCameraButton;

	private View localSurfaceViewLy;
	private SurfaceView mLocalSurface;

	private Conference conf;

	private ConferenceService cb = new ConferenceService();

	private ChatService cs = new ChatService();

	private Long mGroupId;
	private Set<Attendee> mAttendeeList = new HashSet<Attendee>();
	private DocumentService ds = new DocumentService();

	private Map<String, V2Doc> mDocs = new HashMap<String, V2Doc>();
	private V2Doc mCurrentActivateDoc = null;

	private Map<String, MixerWrapper> mMixerWrapper = new HashMap<String, MixerWrapper>();

	private SubViewListener subViewListener = new SubViewListener();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(R.layout.activity_in_metting);
		mContext = this;
		this.mVideoLayoutMain = (RelativeLayout) findViewById(R.id.video_layout_root);
		this.mCurrentShowedSV = new ArrayList<SurfaceViewW>();

		this.mVideoLayout = (RelativeLayout) findViewById(R.id.in_metting_video_main);
		// setting button
		this.mSettingIV = (ImageView) findViewById(R.id.in_meeting_setting_iv);
		this.mSettingIV.setOnClickListener(mShowSettingListener);
		// request exit button
		this.mQuitIV = (ImageView) findViewById(R.id.in_meeting_log_out_iv);
		this.mQuitIV.setOnClickListener(mShowQuitWindowListener);
		// request speak or mute button
		this.mSpeakerIV = (ImageView) findViewById(R.id.speaker_iv);
		this.mSpeakerIV.setOnClickListener(mApplySpeakerListener);
		// conference name text view
		this.mGroupNameTV = (TextView) findViewById(R.id.in_meeting_name);

		this.mMenuLine = (LinearLayout) findViewById(R.id.in_meeting_menu_separation_line);
		this.mVideoLine = (LinearLayout) findViewById(R.id.in_meeting_video_separation_line1);

		// local camera surface view
		this.mLocalSurface = (SurfaceView) findViewById(R.id.local_surface_view);

		this.localSurfaceViewLy = findViewById(R.id.local_surface_view_ly);

		// show menu list(Include show message layout, show attendee list layout
		// show document layout) button
		mMenuButton = (ImageView) findViewById(R.id.in_meeting_menu_button);
		mMenuButton.setOnClickListener(mMenuButtonListener);
		mMenuButtonContainer = (LinearLayout) findViewById(R.id.in_meeting_menu_layout);
		// show message layout button
		mMenuMessageButton = findViewById(R.id.in_meeting_menu_show_msg_button);
		mMenuMessageButton.setTag("msg");
		mMenuMessageButton.setOnClickListener(mMenuShowButtonListener);
		// show attendee list layout button
		mMenuAttendeeButton = findViewById(R.id.in_meeting_menu_show_attendees_button);
		mMenuAttendeeButton.setTag("attendee");
		mMenuAttendeeButton.setOnClickListener(mMenuShowButtonListener);

		// show doucment display button
		mMenuDocButton = findViewById(R.id.in_meeting_menu_show_doc_button);
		mMenuDocButton.setTag("doc");
		mMenuDocButton.setOnClickListener(mMenuShowButtonListener);

		mConverseLocalCameraButton = findViewById(R.id.converse_camera_button);
		mConverseLocalCameraButton.setOnClickListener(mConverseCameraListener);

		// Initialize broadcast receiver
		initConfsListener();
		// Initialize conference object and show local camera
		init();

		// Update main activity tab notificatior
		notificateConversationUpdate();

		// make sure local is in front of any view
		localSurfaceViewLy.bringToFront();
		// Start animation
		this.overridePendingTransition(R.animator.nonam_scale_center_0_100,
				R.animator.nonam_scale_null);
	}

	private OnClickListener mMenuButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (mMenuButtonContainer.getVisibility() == View.GONE) {
				Animation animation = AnimationUtils.loadAnimation(mContext,
						R.animator.nonam_scale_y_0_100);
				animation.setDuration(400);
				mMenuButtonContainer.startAnimation(animation);
				mMenuButtonContainer.setVisibility(View.VISIBLE);
				((ImageView)view).setImageResource(R.drawable.video_menu_button);

			} else {
				//Do not hide other window
//				showOrHidenAttendeeContainer(View.GONE);
//				showOrHidenMsgContainer(View.GONE);	
//				showOrHidenDocContainer(View.GONE);
				Animation animation = AnimationUtils.loadAnimation(mContext,
						R.animator.nonam_scale_y_100_0);
				animation.setDuration(400);
				mMenuButtonContainer.startAnimation(animation);
				mMenuButtonContainer.setVisibility(View.GONE);
				((ImageView)view).setImageResource(R.drawable.video_menu_button_pressed);
			}
		}

	};

	private OnClickListener mMenuShowButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v.getTag().equals("msg")) {
				showOrHidenMsgContainer(View.VISIBLE);
				showOrHidenAttendeeContainer(View.GONE);
				showOrHidenDocContainer(View.GONE);
			} else if (v.getTag().equals("attendee")) {
				showOrHidenAttendeeContainer(View.VISIBLE);
				showOrHidenMsgContainer(View.GONE);
				showOrHidenDocContainer(View.GONE);
			} else if (v.getTag().equals("doc")) {
				showOrHidenDocContainer(View.VISIBLE);
				showOrHidenAttendeeContainer(View.GONE);
				showOrHidenMsgContainer(View.GONE);
			}
		}

	};

	private OnClickListener mApplySpeakerListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			// FIXME code
			if (isSpeaking) {
				mSpeakerIV.setImageResource(R.drawable.mute_button);
			} else {
				mSpeakerIV.setImageResource(R.drawable.speaking_button);
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

	private OnClickListener mShowSettingListener = new OnClickListener() {

		@Override
		public void onClick(View v) {

			if (mSettingWindow == null) {
				LayoutInflater inflater = (LayoutInflater) mContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View view = inflater.inflate(
						R.layout.in_meeting_setting_pop_up_window, null);
				 DisplayMetrics m = new DisplayMetrics();
				 getWindowManager().getDefaultDisplay().getMetrics(m);
				 
				int height =(int)( m.heightPixels * 0.5);
				// set
				mSettingWindow = new PopupWindow(view,
						LayoutParams.WRAP_CONTENT, height);
				mSettingWindow.setBackgroundDrawable(new ColorDrawable(
						Color.TRANSPARENT));
				mSettingWindow.setFocusable(true);
				mSettingWindow.setTouchable(true);
				mSettingWindow.setOutsideTouchable(true);
				mSettingWindow.setOnDismissListener(new OnDismissListener() {
					@Override
					public void onDismiss() {
						mSettingWindow.dismiss();
					}

				});

				/*
				 * DisplayMetrics m = new DisplayMetrics();
				 * getWindowManager().getDefaultDisplay().getMetrics(m);
				 * v.measure(m.widthPixels, m.heightPixels);
				 */
				mSettingWindow
						.getContentView()
						.findViewById(R.id.camera_setting_window_arrow)
						.measure(View.MeasureSpec.UNSPECIFIED,
								View.MeasureSpec.UNSPECIFIED);
			}

			int[] pos = new int[2];
			v.getLocationInWindow(pos);
			pos[0] += v.getMeasuredWidth() / 2;
			pos[1] += v.getMeasuredHeight();
			// calculate arrow offset
			View arrow = mSettingWindow.getContentView().findViewById(
					R.id.camera_setting_window_arrow);
			arrow.bringToFront();
			RelativeLayout.LayoutParams rl = (RelativeLayout.LayoutParams) arrow
					.getLayoutParams();
			pos[0] -= (rl.leftMargin + arrow.getMeasuredWidth() / 2);

			mSettingWindow
					.setAnimationStyle(R.style.InMeetingCameraSettingAnim);
			mSettingWindow
					.showAtLocation(v, Gravity.NO_GRAVITY, pos[0], pos[1]);
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
			} else if (JNIService.JNI_BROADCAST_NEW_CONF_MESSAGE.equals(intent
					.getAction())) {
				String content = intent.getExtras().getString("content");
				long fromUid = intent.getLongExtra("fromuid", 0);
				VMessage vm = new VMessage(GlobalHolder.getInstance().getUser(
						fromUid), GlobalHolder.getInstance().getCurrentUser(),
						content);
				if (mMessageContainer != null) {
					mMessageContainer.addNewMessage(vm);
				}
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {
				long confID = intent.getLongExtra("gid", 0);
				if (confID == mGroupId) {
					Group confGroup = GlobalHolder.getInstance().findGroupById(
							mGroupId);
					// load conference attendee list
					if (confGroup != null) {
						List<User> l = new ArrayList<User>(confGroup.getUsers());
						for (User u : l) {
							mAttendeeList.add(new Attendee(u));
						}
						if (mAttendeeContainer != null) {
							mAttendeeContainer.setAttendsList(mAttendeeList);
							//TODO optize code
							synchronized (mMixerWrapper) {
								for (MixerWrapper mw : mMixerWrapper.values()) {
									mAttendeeContainer.updateEnteredAttendee(mw.amd);
								}
							}
						}
					}
				}

			} else if (JNIService.JNI_BROADCAST_GROUP_USER_REMOVED
					.equals(intent.getAction())) {
				Object obj = intent.getExtras().get("obj");
				Message.obtain(mVideoHandler, USER_DELETE_GROUP, obj)
						.sendToTarget();

			} else if (JNIService.JNI_BROADCAST_GROUP_USER_ADDED.equals(intent
					.getAction())) {
				Object obj = intent.getExtras().get("obj");
				Message.obtain(mVideoHandler, GROUP_ADD_USER, obj)
						.sendToTarget();

			} else if (JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION
					.equals(intent.getAction())) {
				NetworkStateCode code = (NetworkStateCode) intent.getExtras()
						.get("state");
				if (code != NetworkStateCode.CONNECTED) {
					Toast.makeText(mContext, R.string.error_connect_to_server,
							Toast.LENGTH_SHORT).show();
				}
			} else if (JNIService.JNI_BROADCAST_USER_STATUS_NOTIFICATION
					.equals(intent.getAction())) {
				long uid = intent.getExtras().getLong("uid");
				Attendee at = findAttendee(uid);
				// If exited user is not attendee in conference, then return
				if (at == null) {
					return;
				}
				User user = GlobalHolder.getInstance().getUser(uid);
				UserStatusObject us = (UserStatusObject)intent.getExtras().get("status");
				User.Status st = User.Status.fromInt(us.getStatus());
				// If client applciation exit directly, we don't receive exit
				// conference notification
				if (st == User.Status.OFFLINE && user != null) {
					Message.obtain(mVideoHandler, ATTENDEE_LISTENER, 0, 0, user)
							.sendToTarget();
				}
			} else if (PublicIntent.PREPARE_FINISH_APPLICATION.equals(intent
					.getAction())) {
				// Listen quit request to make sure close all device
				quit();
				finish();
			}

		}

	};

	@Override
	protected void onStart() {
		super.onStart();
		boolean  inFlag  = this.getIntent().getExtras().getBoolean("in", false);
		//If doesn't enter conference, then request
		if (!inFlag) {
			requestEnterConf();
		}
		// adjustLayout();
		Message.obtain(mVideoHandler, ONLY_SHOW_LOCAL_VIDEO).sendToTarget();
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.cancel(PublicIntent.VIDEO_NOTIFICATION_ID);
		// keep screen on
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private void initConfsListener() {
		IntentFilter filter = new IntentFilter();
		filter.addCategory(JNI_EVENT_VIDEO_CATEGORY);
		filter.addAction(JNI_EVENT_VIDEO_CATEGORY_OPEN_VIDEO_EVENT_ACTION);
		filter.addAction(JNIService.JNI_BROADCAST_NEW_CONF_MESSAGE);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_ADDED);
		filter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		filter.addAction(PublicIntent.PREPARE_FINISH_APPLICATION);
		filter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		filter.addAction(JNIService.JNI_BROADCAST_USER_STATUS_NOTIFICATION);
		mContext.registerReceiver(mConfUserChangeReceiver, filter);

		// register listener for conference service
		cb.registerPermissionUpdateListener(mVideoHandler,
				NOTIFY_USER_PERMISSION_UPDATED, null);
		cb.registerAttendeeListener(this.mVideoHandler, ATTENDEE_LISTENER, null);
		cb.registerKickedConfListener(mVideoHandler, NOTIFICATION_KICKED, null);
		cb.registerSyncDesktopListener(mVideoHandler,
				DESKTOP_SYNC_NOTIFICATION, null);

		cb.registerVideoMixerListener(mVideoHandler, VIDEO_MIX_NOTIFICATION,
				null);

		// Register listen to document notification
		ds.registerNewDocNotification(mVideoHandler, NEW_DOC_NOTIFICATION, null);
		ds.registerDocDisplayNotification(mVideoHandler,
				DOC_DOWNLOADED_NOTIFICATION, null);
		ds.registerdocPageActiveNotification(mVideoHandler,
				DOC_PAGE_ACTIVITE_NOTIFICATION, null);
		ds.registerDocPageNotification(mVideoHandler, DOC_PAGE_NOTIFICATION,
				null);
		ds.registerDocClosedNotification(mVideoHandler,
				DOC_CLOSED_NOTIFICATION, null);
		ds.registerDocPageAddedNotification(mVideoHandler,
				DOC_PAGE_ADDED_NOTIFICATION, null);
		ds.registerPageCanvasUpdateNotification(mVideoHandler,
				DOC_PAGE_CANVAS_NOTIFICATION, null);
	}

	private void init() {
		mGroupId = this.getIntent().getLongExtra("gid", 0);
		if (mGroupId <= 0) {
			Toast.makeText(this, R.string.error_in_meeting_invalid_gid,
					Toast.LENGTH_LONG).show();
			return;
		}

		conf = new Conference(mGroupId);
		Group g = GlobalHolder.getInstance().findGroupById(mGroupId);
		if (g == null) {
			V2Log.e(" doesn't receive group information  yet");
			return;
		}
		mGroupNameTV.setText(g.getName());

		Group confGroup = GlobalHolder.getInstance().findGroupById(
				this.mGroupId);
		// load conference attendee list
		if (confGroup != null) {
			List<User> l = new ArrayList<User>(confGroup.getUsers());
			for (User u : l) {
				mAttendeeList.add(new Attendee(u));
			}
		}
		V2Log.i(" Conference size:" + mAttendeeList.size());
	}

	private void notificateConversationUpdate() {
		Intent i = new Intent(PublicIntent.REQUEST_UPDATE_CONVERSATION);
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);
		i.putExtra("type", Conversation.TYPE_CONFERNECE);
		i.putExtra("extId", this.mGroupId);
		i.putExtra("noti", false);
		mContext.sendBroadcast(i);
	}

	/**
	 * Show or hide message layout according to parameter
	 * 
	 * @param visible
	 *            {@link View#VISIBLE} {@link View#GONE}
	 */
	private void showOrHidenMsgContainer(int visible) {
		if (mMessageContainer == null) {
			mMessageContainer = new VideoMsgChattingLayout(this);
			mMessageContainer.setId(0x7ffff000);
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
					(int) (mVideoLayout.getMeasuredWidth() * 0.4),
					mVideoLayout.getMeasuredHeight());
			lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			lp.addRule(RelativeLayout.RIGHT_OF, mMenuLine.getId());
			lp.addRule(RelativeLayout.BELOW, mVideoLine.getId());
			mVideoLayoutMain.addView(mMessageContainer, lp);
			mMessageContainer.bringToFront();
			mMessageContainer.setVisibility(View.GONE);
			mMessageContainer.setListener(subViewListener);
		}

		if (visible == View.GONE
				&& View.GONE == mMessageContainer.getVisibility()) {
			return;
		}

		if (visible == View.GONE
				|| visible == mMessageContainer.getVisibility()) {
			mMessageContainer.setVisibility(View.GONE);
			mMenuMessageButton.setBackgroundColor(Color.rgb(255, 255, 255));

			Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
					this, R.animator.normal_scale_x_100_0);
			tabBlockHolderAnimation.setDuration(500);
			tabBlockHolderAnimation.setInterpolator(new AccelerateInterpolator());
			mMessageContainer.startAnimation(tabBlockHolderAnimation);
			mMessageContainer.requestFloatLayout();

		} else {
			mMessageContainer.setVisibility(View.VISIBLE);
			mMenuMessageButton.setBackgroundColor(mContext.getResources()
					.getColor(R.color.confs_common_bg));

			Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
					this, R.animator.normal_scale_x_0_100);
			tabBlockHolderAnimation.setInterpolator(new AccelerateInterpolator());
			tabBlockHolderAnimation.setDuration(500);
			mMessageContainer.startAnimation(tabBlockHolderAnimation);
			mMessageContainer.requestScrollToNewMessage();

		}
	}

	/**
	 * Show or hide attendees list layout according to parameter
	 * 
	 * @param visible
	 *            {@link View#VISIBLE} {@link View#GONE}
	 */
	private void showOrHidenAttendeeContainer(int visible) {
		if (mAttendeeContainer == null) {
			mAttendeeContainer = new VideoAttendeeListLayout(this);
			mAttendeeContainer.setId(0x7ffff001);
			mAttendeeContainer.setAttendsList(this.mAttendeeList);
			synchronized (mMixerWrapper) {
				for (MixerWrapper mw : mMixerWrapper.values()) {
					mAttendeeContainer.updateEnteredAttendee(mw.amd);
				}
			}

			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
					(int) (mVideoLayout.getMeasuredWidth() * 0.4),
					mVideoLayout.getMeasuredHeight());
			lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			lp.addRule(RelativeLayout.RIGHT_OF, mMenuLine.getId());
			lp.addRule(RelativeLayout.BELOW, mVideoLine.getId());
			mVideoLayoutMain.addView(mAttendeeContainer, lp);
			mAttendeeContainer.bringToFront();
			mAttendeeContainer.setVisibility(View.GONE);
			mAttendeeContainer.setListener(subViewListener);
		}

		// View is hidded, do not need to hide again
		if (visible == View.GONE
				&& View.GONE == mAttendeeContainer.getVisibility()) {
			return;
		}
		if (visible == View.GONE
				|| visible == mAttendeeContainer.getVisibility()) {
			mAttendeeContainer.setVisibility(View.GONE);
			mMenuAttendeeButton.setBackgroundColor(Color.rgb(255, 255, 255));

			Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
					this, R.animator.normal_scale_x_100_0);
			tabBlockHolderAnimation.setDuration(500);
			mAttendeeContainer.startAnimation(tabBlockHolderAnimation);
			mAttendeeContainer.requestFloatLayout();
		} else {
			mAttendeeContainer.setVisibility(View.VISIBLE);
			mMenuAttendeeButton.setBackgroundColor(mContext.getResources()
					.getColor(R.color.confs_common_bg));
			// set animation when visible
			Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
					this, R.animator.normal_scale_x_0_100);
			tabBlockHolderAnimation.setDuration(500);
			mAttendeeContainer.startAnimation(tabBlockHolderAnimation);
			mAttendeeContainer.bringToFront();
		}
	}

	/**
	 * Show or hide document layout according to parameter
	 * 
	 * @param visible
	 *            {@link View#VISIBLE} {@link View#GONE}
	 */
	private void showOrHidenDocContainer(int visible) {
		if (mDocContainer == null) {
			mDocContainer = new VideoDocLayout(this);
			mDocContainer.setId(0x7ffff002);
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
					(int) (mVideoLayoutMain.getWidth() * 0.5 - mMenuButtonContainer
							.getWidth()), mMenuButtonContainer.getHeight());
			lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			lp.addRule(RelativeLayout.RIGHT_OF, mMenuLine.getId());
			lp.addRule(RelativeLayout.BELOW, mVideoLine.getId());

			mDocContainer.setListener(subViewListener);

			mVideoLayoutMain.addView(mDocContainer, lp);

			mDocContainer.bringToFront();
			mDocContainer.setVisibility(View.GONE);
			synchronized (mDocs) {
				for (Entry<String, V2Doc> e : mDocs.entrySet()) {
					mDocContainer.addDoc(e.getValue());
				}
			}
			mDocContainer.updateCurrentDoc(mCurrentActivateDoc);
			Group g = GlobalHolder.getInstance().findGroupById(mGroupId);
			if (g != null && g instanceof ConferenceGroup) {
				mDocContainer.updateSyncStatus(((ConferenceGroup) g).isSyn());
			}

		}
		// View is hidded, do not need to hide again
		if (visible == View.GONE && View.GONE == mDocContainer.getVisibility()) {
			return;
		}

		if (visible == View.GONE || visible == mDocContainer.getVisibility()) {
			mDocContainer.setVisibility(View.GONE);
			mMenuDocButton.setBackgroundColor(Color.rgb(255, 255, 255));

			Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
					this, R.animator.normal_scale_x_100_0);
			tabBlockHolderAnimation.setDuration(500);
			mDocContainer.startAnimation(tabBlockHolderAnimation);
			// Call this function to inform listener
			// Notice must restore first, then request float
			// because if in full screen size, will ignore float request
			mDocContainer.requestRestore();
			mDocContainer.requestFloatLayout();
		} else {
			mDocContainer.setVisibility(View.VISIBLE);
			mMenuDocButton.setBackgroundColor(mContext.getResources().getColor(
					R.color.confs_common_bg));

			// set animation when visible
			Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
					this, R.animator.normal_scale_x_0_100);
			tabBlockHolderAnimation.setDuration(500);
			mDocContainer.startAnimation(tabBlockHolderAnimation);
		}
	}

	private OnClickListener mConverseCameraListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			doReverseCamera();
		}

	};

	private Attendee findAttendee(long uid) {
		for (Attendee at : mAttendeeList) {
			if (at != null && at.getAttId() == uid) {
				return at;
			}
		}
		return null;
	}

	/**
	 * 
	 */
	private void showOrCloseLocalSurViewOnly() {
		boolean selfInAttendeeList = false;
		Attendee atd = null;
		UserDeviceConfig udc = null;
		for (Attendee a : mAttendeeList) {
			if (a.getAttId() == GlobalHolder.getInstance().getCurrentUserId()) {
				selfInAttendeeList = true;
				atd = a;
				break;
			}
		}

		if (selfInAttendeeList == false) {
			atd = new Attendee(GlobalHolder.getInstance().getCurrentUser(),
					true, false);
			mAttendeeList.add(atd);
		} else {
			atd.setSelf(true);
		}

		udc = atd.getDefaultDevice();
		// Add default device
		if (udc == null) {
			udc = new UserDeviceConfig(atd.getAttId(), "", null);
			atd.addDevice(udc);
		}

		// layout must before open device
		// showOrCloseAttendeeVideo(udc);

		udc.setSVHolder(mLocalSurface);
		VideoRecorder.VideoPreviewSurfaceHolder = udc.getSVHolder().getHolder();
		VideoRecorder.VideoPreviewSurfaceHolder
				.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		VideoRecorder.VideoPreviewSurfaceHolder
				.setFormat(PixelFormat.TRANSPARENT);
		VideoCaptureDevInfo.CreateVideoCaptureDevInfo()
				.updateCameraOrientation(Surface.ROTATION_0);
		Message m = Message.obtain(mVideoHandler, REQUEST_OPEN_OR_CLOSE_DEVICE,
				1, 0, udc);
		mVideoHandler.sendMessageDelayed(m, 300);
		udc.setShowing(true);
	}

	private void adjustLayout() {
		int maxWidth = 2;
		int marginTop = 0;
		int marginBottom = 0;
		int marginLeft = 0;
		int marginRight = 0;
		int size = mCurrentShowedSV.size();
		int rows = size / maxWidth + (size % maxWidth == 0 ? 0 : 1);
		int cols = size > 1 ? maxWidth : size;
		if (size == 0) {
			V2Log.e(" No surface to show");
			return;
		}

		Rect outR = new Rect();
		mVideoLayout.getDrawingRect(outR);
		int[] po = new int[2];
		mVideoLayout.getLocationInWindow(po);
		mVideoLayout.measure(View.MeasureSpec.UNSPECIFIED,
				View.MeasureSpec.UNSPECIFIED);
		// First extra from layout parameter. Because if doc layout request
		// fixed position,
		// will set layout width parameter. At here measure doesn't work with
		// layoutparameter
		int containerW = mVideoLayout.getLayoutParams().width;
		if (containerW <= 0) {
			containerW = mVideoLayout.getWidth();
		}
		int containerH = outR.bottom - outR.top;

		int normalW = containerW / cols, normalH = normalW / 16 * 9;
		if (normalH * rows > containerH) {
			normalH = containerH / rows;
			normalW = normalH / 9 * 16;
		}

		int fixedWidth = normalW;
		int fixedHeight = normalH;

		marginTop = marginBottom = Math.abs(containerH - fixedHeight * rows) / 2;
		marginLeft = marginRight = Math.abs(containerW - fixedWidth * cols) / 2;

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
					((ViewGroup) sw.getView().getParent()).removeView(sw
							.getView());
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
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mVideoLayout.removeAllViews();
		quit();
		finish();
	}

	@Override
	public void finish() {
		super.finish();
		// this.overridePendingTransition(R.animator.nonam_scale_null,
		// R.animator.nonam_scale_center_100_0);
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

		cb.unRegisterPermissionUpdateListener(mVideoHandler,
				NOTIFY_USER_PERMISSION_UPDATED, null);
		cb.removeRegisterOfKickedConfListener(mVideoHandler,
				NOTIFICATION_KICKED, null);
		cb.removeAttendeeListener(this.mVideoHandler, ATTENDEE_LISTENER, null);

		cb.unRegisterVideoMixerListener(mVideoHandler, VIDEO_MIX_NOTIFICATION,
				null);

		ds.unRegisterNewDocNotification(mVideoHandler, NEW_DOC_NOTIFICATION,
				null);
		ds.unRegisterDocDisplayNotification(mVideoHandler,
				DOC_DOWNLOADED_NOTIFICATION, null);
		ds.unRegisterDocClosedNotification(mVideoHandler,
				DOC_CLOSED_NOTIFICATION, null);
		ds.unRegisterDocPageAddedNotification(mVideoHandler,
				DOC_PAGE_ADDED_NOTIFICATION, null);
		ds.unRegisterPageCanvasUpdateNotification(mVideoHandler,
				DOC_PAGE_CANVAS_NOTIFICATION, null);
		cb.removeSyncDesktopListener(mVideoHandler, DESKTOP_SYNC_NOTIFICATION,
				null);
		if (mDocContainer != null) {
			// clean document bitmap cache
			mDocContainer.cleanCache();
		}
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

		VideoCaptureDevInfo.CreateVideoCaptureDevInfo().reverseCamera();

		cb.updateCameraParameters(new CameraConfiguration(""), null);
		return;
	}

	/**
	 * user quit conference, however positive or negative
	 */
	private void quit() {
		if (mWaitingDialog != null) {
			mWaitingDialog.dismiss();
		}
		for (SurfaceViewW sw : this.mCurrentShowedSV) {
			Message.obtain(mVideoHandler, REQUEST_OPEN_OR_CLOSE_DEVICE, 0, 0,
					sw.udc).sendToTarget();
			// mService.requestCloseVideoDevice(mGroupId, sw.udc,
			// Message.obtain(
			// mVideoHandler, REQUEST_CLOSE_DEVICE_RESPONSE));
		}

		// close local camera
		Message.obtain(
				mVideoHandler,
				REQUEST_OPEN_OR_CLOSE_DEVICE,
				0,
				0,
				new UserDeviceConfig(GlobalHolder.getInstance()
						.getCurrentUserId(), "", null)).sendToTarget();
		VideoRecorder.VideoPreviewSurfaceHolder = null;
		mAttendeeList.clear();
		mCurrentShowedSV.clear();
		Message.obtain(this.mVideoHandler, REQUEST_EXIT_CONF, this.mGroupId)
				.sendToTarget();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showQuitDialog();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void doApplyOrReleaseSpeak() {
		if (isSpeaking) {
			cb.applyForReleasePermission(ConferencePermission.SPEAKING, null);
			isSpeaking = false;
		} else {
			cb.applyForControlPermission(ConferencePermission.SPEAKING, null);
			isSpeaking = true;
		}
	}

	/**
	 * Handle event which new user entered conference
	 * 
	 * @param user
	 */
	private void doHandleNewUserEntered(User user) {
		if (user == null) {
			return;
		}
		Attendee a = findAttendee(user.getmUserId());
		if (a == null) {
			a = new Attendee(user);
			this.mAttendeeList.add(a);
		} else {
			a.setUser(user);
		}

		List<UserDeviceConfig> ld = GlobalHolder.getInstance()
				.getAttendeeDevice(a.getAttId());
		if (ld == null || ld.size() <= 0) {
			V2Log.w(" No available device config for user:" + user.getmUserId()
					+ "  name:" + user.getName());
		}
		for (UserDeviceConfig udc : ld) {
			a.addDevice(udc);
		}

		if (mAttendeeContainer != null) {
			mAttendeeContainer.updateEnteredAttendee(a);
		}
		Toast.makeText(mContext, user.getName() + "进入会议室! ", Toast.LENGTH_SHORT)
				.show();
	}

	private void requestEnterConf() {
		mWaitingDialog = ProgressDialog.show(
				mContext,
				"",
				mContext.getResources().getString(
						R.string.requesting_enter_conference), true);
		Message.obtain(mVideoHandler, REQUEST_ENTER_CONF,
				Long.valueOf(mGroupId)).sendToTarget();
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
			// User do exist video device
			if (a.getmDevices() != null) {
				for (UserDeviceConfig udc : a.getmDevices()) {
					if (udc != null && udc.isShowing()) {
						showOrCloseAttendeeVideo(udc);
					}
				}
			}
			a.setJoined(false);
			if (mAttendeeContainer != null) {
				mAttendeeContainer.updateExitedAttendee(a);
			}
		}

	}

	private boolean checkVideoExceedMaminum() {
		return mCurrentShowedSV.size() >= 4;
	}

	/**
	 * show remote user video
	 * 
	 * @param udc
	 */
	private void showOrCloseAttendeeVideo(UserDeviceConfig udc) {
		if (udc == null) {
			V2Log.e(" can't not open or close device");
			return;
		}
		// if already opened attendee's video, switch action to close
		if (udc.isShowing()) {
			cb.requestCloseVideoDevice(conf, udc, new Registrant(mVideoHandler,
					REQUEST_CLOSE_DEVICE_RESPONSE, null));

			for (SurfaceViewW sw : mCurrentShowedSV) {
				if (sw.udc == udc) {
					mCurrentShowedSV.remove(sw);
					mVideoLayout.removeView(sw.getView());
					sw.rl.removeAllViews();
					break;
				}
			}

			udc.setShowing(false);
			udc.doClose();
			adjustLayout();
		} else {
			if (checkVideoExceedMaminum()) {
				Toast.makeText(mContext,
						R.string.error_exceed_support_video_count,
						Toast.LENGTH_SHORT).show();
				return;
			}
			VideoPlayer vp = new VideoPlayer();
			udc.setSVHolder(new SurfaceView(this));
			vp.SetSurface(udc.getSVHolder().getHolder());
			udc.setVp(vp);

			//
			if (udc.getBelongsAttendee() instanceof AttendeeMixedDevice) {
				mCurrentShowedSV
				.add(new MixedSurfaceViewW(((AttendeeMixedDevice)udc.getBelongsAttendee()).getMV(), udc));
			} else {
				mCurrentShowedSV
				.add(new SurfaceViewW(udc.getBelongsAttendee(), udc));
				
			}
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
			if (uid == a.getAttId()) {
				at = a;
				break;
			}
		}
		return at;
	}

	class SubViewListener implements VideoDocLayout.DocListener,
			VideoAttendeeListLayout.VideoAttendeeActionListener,
			VideoMsgChattingLayout.ChattingListener {

		@Override
		public void requestSendMsg(VMessage vm) {
			vm.mGroupId = mGroupId;
			vm.setToUser(new User(0));
			vm.setUser(GlobalHolder.getInstance().getCurrentUser());
			vm.setMsgCode(VMessage.VMESSAGE_CODE_CONF);
			cs.sendVMessage(vm, null);

		}

		@Override
		public void requestChattingViewFixedLayout(View v) {
			calculateLayoutParma(v, 1);
			adjustLayout();
		}

		@Override
		public void requestChattingViewFloatLayout(View v) {
			calculateLayoutParma(v, 2);
			adjustLayout();
		}

		@Override
		public void OnAttendeeClicked(Attendee at, UserDeviceConfig udc) {
			if (udc == null || at == null) {
				return;
			}
			if (at.getAttId() == GlobalHolder.getInstance().getCurrentUserId()) {

			} else {
				showOrCloseAttendeeVideo(udc);
			}
		}

		@Override
		public void requestAttendeeViewFixedLayout(View v) {
			calculateLayoutParma(v, 1);
			adjustLayout();
		}

		@Override
		public void requestAttendeeViewFloatLayout(View v) {
			calculateLayoutParma(v, 2);
			adjustLayout();
		}

		@Override
		public void updateDoc(V2Doc doc, Page p) {

		}

		@Override
		public void requestDocViewFixedLayout(View v) {
			// If current doc layout size is full screen, then ignore
			// reques fixed size reqeust.
			if (mDocContainer.isFullScreenSize()) {
				return;
			}
			calculateLayoutParma(v, 1);
			adjustLayout();

		}

		@Override
		public void requestDocViewFloatLayout(View v) {
			// If current doc layout size is full screen, then ignore
			// reques fixed size reqeust.
			if (mDocContainer.isFullScreenSize()) {
				return;
			}
			calculateLayoutParma(v, 2);
			adjustLayout();
		}

		@Override
		public void requestDocViewFillParent(View v) {
			mVideoLayout.setVisibility(View.GONE);
			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);

			int[] location = new int[2];
			v.getLocationInWindow(location);

			ViewGroup.LayoutParams vl = mDocContainer.getLayoutParams();
			vl.width = dm.widthPixels - location[0];
			//
			v.setLayoutParams(vl);
			Animation anim = new ScaleAnimation(0.5F, 1.0F, 1.0F, 1.0F,
					Animation.ABSOLUTE, 1.0F, Animation.ABSOLUTE, 1.0F);
			anim.setFillAfter(true);
			anim.setDuration(400);
			v.startAnimation(anim);

			// Update local video layout params make sure local video
			// can float at right and bottom
			// Because layout mess up if mVideoLayout gone
			RelativeLayout.LayoutParams localRL = (RelativeLayout.LayoutParams) localSurfaceViewLy
					.getLayoutParams();
			localRL.addRule(RelativeLayout.ALIGN_BOTTOM, v.getId());
			localSurfaceViewLy.setLayoutParams(localRL);

			// make sure local is in front of any view
			localSurfaceViewLy.bringToFront();
		}

		@Override
		public void requestDocViewRestore(View v) {
			mVideoLayout.setVisibility(View.VISIBLE);

			final ViewGroup.LayoutParams vl = mDocContainer.getLayoutParams();
			int oriWidth = vl.width;
			vl.width = (int) (mVideoLayoutMain.getWidth() * 0.5 - mMenuButtonContainer
					.getWidth());
			// Animation anim = new ScaleAnimation(1.0F, 0.5F,
			// 1.0F,1.0F, 1.0F,1.0F);
			// anim.setFillAfter(true);
			// anim.setDuration(400);
			// v.startAnimation(anim);
			v.setLayoutParams(vl);

			// Update local video layout params make sure local video
			// can float at right and bottom
			RelativeLayout.LayoutParams localRL = (RelativeLayout.LayoutParams) localSurfaceViewLy
					.getLayoutParams();
			localRL.addRule(RelativeLayout.ALIGN_BOTTOM, mVideoLayout.getId());
			localSurfaceViewLy.setLayoutParams(localRL);
		}

		/**
		 * 
		 * @param flag
		 *            1 request fixed 2 request float
		 */
		private void calculateLayoutParma(View v, int flag) {
			int id = v.getId();
			// request float
			if (flag == 2) {
				// set video layout layout to right of in_meeting_menu_layout
				id = R.id.in_meeting_menu_layout;
			}
			RelativeLayout.LayoutParams rl = (RelativeLayout.LayoutParams) mVideoLayout
					.getLayoutParams();
			rl.addRule(RelativeLayout.RIGHT_OF, id);
			int[] location = new int[2];
			v.getLocationInWindow(location);
			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
			rl.width = dm.widthPixels - location[0];
			// if flag is request fixed
			// video layout width = screen's width - requested view's width
			if (flag == 1) {
				rl.width -= v.getMeasuredWidth();
			}
			mVideoLayout.setLayoutParams(rl);

		}

	}

	class MixerWrapper {
		String id;
		MixVideo mix;
		MixVideoLayout layout;
		AttendeeMixedDevice amd;

		public MixerWrapper(String id, MixVideo mix, MixVideoLayout layout) {
			super();
			this.id = id;
			this.mix = mix;
			this.layout = layout;
			amd = new AttendeeMixedDevice(mix);
		}

	}

	class SurfaceViewW {

		Attendee at;
		UserDeviceConfig udc;
		int layId;
		RelativeLayout rl;

		public SurfaceViewW() {

		}

		public SurfaceViewW(Attendee at, UserDeviceConfig udc) {
			this.at = at;
			this.udc = udc;
		}

		public View getView() {
			if (rl == null) {
				rl = new RelativeLayout(mContext);
				rl.setPadding(1, 1, 1, 1);
				rl.setBackgroundColor(Color.rgb(143, 144, 144));
				// FIXME make sure hash code is unique.
				layId = (int) udc.hashCode();

				if (udc.getSVHolder() == null) {
					udc.setSVHolder(new SurfaceView(mContext));
				}
				rl.addView(udc.getSVHolder(), new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT,
						RelativeLayout.LayoutParams.MATCH_PARENT));
				TextView tv = new TextView(mContext);
				tv.setText(at.getAttName());
				tv.setBackgroundColor(Color.rgb(138, 138, 138));
				tv.setPadding(10, 10, 10, 10);
				tv.setTextSize(20);
				RelativeLayout.LayoutParams tvrl = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.WRAP_CONTENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);
				tvrl.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				tvrl.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				rl.addView(tv, tvrl);
				rl.setId(layId);
			}
			return rl;
		}
	}

	class MixedSurfaceViewW extends SurfaceViewW {

		MixVideo at;

		public MixedSurfaceViewW(MixVideo at) {
			this.at = at;
			this.udc = new UserDeviceConfig(0, at.getId(), null);
		}
		
		public MixedSurfaceViewW(MixVideo at, UserDeviceConfig udc) {
			this.at = at;
			this.udc = udc;
		}

		@Override
		public View getView() {
			if (rl == null) {
				rl = new RelativeLayout(mContext);
				rl.setPadding(1, 1, 1, 1);
				rl.setBackgroundColor(Color.rgb(143, 144, 144));
				// FIXME make sure hash code is unique.
				layId = (int) udc.hashCode();

				if (udc.getSVHolder() == null) {
					udc.setSVHolder(new SurfaceView(mContext));
				}
				rl.addView(udc.getSVHolder(), new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT,
						RelativeLayout.LayoutParams.MATCH_PARENT));
				TextView tv = new TextView(mContext);
				// FIXME use name
				tv.setText("混平");
				tv.setBackgroundColor(Color.rgb(138, 138, 138));
				tv.setPadding(10, 10, 10, 10);
				tv.setTextSize(20);
				RelativeLayout.LayoutParams tvrl = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.WRAP_CONTENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);
				tvrl.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				tvrl.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				rl.addView(tv, tvrl);
				rl.setId(layId);
			}
			return rl;
		}
	}

	class VideoHandler extends Handler {

		Map<String, V2Doc.PageArray> prLegacy = new HashMap<String, V2Doc.PageArray>();

		@Override
		public synchronized void handleMessage(Message msg) {

			switch (msg.what) {
			case ONLY_SHOW_LOCAL_VIDEO:
				showOrCloseLocalSurViewOnly();
				break;
			case APPLY_OR_RELEASE_SPEAK:
				doApplyOrReleaseSpeak();
				break;
			case REQUEST_OPEN_DEVICE_RESPONSE:
			case REQUEST_CLOSE_DEVICE_RESPONSE:
				break;
			case CONF_USER_DEVICE_EVENT:
				// recordUserDevice((ConfUserDeviceInfo) msg.obj);
				break;
			case USER_DELETE_GROUP:
				GroupUserObject ro = (GroupUserObject) msg.obj;
				Attendee a = new Attendee(GlobalHolder.getInstance().getUser(
						ro.getmUserId()));
				mAttendeeList.remove(a);
				if (mAttendeeContainer != null) {
					mAttendeeContainer.removeAttendee(a);
				}
				break;
			case GROUP_ADD_USER:
				GroupUserObject ro1 = (GroupUserObject) msg.obj;
				Attendee a1 = new Attendee(GlobalHolder.getInstance().getUser(
						ro1.getmUserId()));
				mAttendeeList.add(a1);
				if (mAttendeeContainer != null) {
					mAttendeeContainer.addAttendee(a1);
				}
				break;
			case ATTENDEE_LISTENER:
				if (msg.arg1 == 1) {
					doHandleNewUserEntered((User) msg.obj);
				} else {
					doHandleUserExited((User) msg.obj);
				}
				break;
			case REQUEST_OPEN_OR_CLOSE_DEVICE:
				if (msg.arg1 == 0) {
					cb.requestCloseVideoDevice(conf,
							(UserDeviceConfig) msg.obj, new Registrant(
									mVideoHandler,
									REQUEST_CLOSE_DEVICE_RESPONSE, null));
				} else if (msg.arg1 == 1) {
					cb.requestOpenVideoDevice(conf, (UserDeviceConfig) msg.obj,
							new Registrant(mVideoHandler,
									REQUEST_OPEN_DEVICE_RESPONSE, null));
				}
				break;
			case NOTIFICATION_KICKED: {
				int r = msg.arg1;
				int resource = R.string.conversations_kick_notification;
				// FIXME use error code for user deleted conf
				if (r == 204) {
					resource = R.string.confs_is_deleted_notification;
				}
				Toast.makeText(mContext, resource, Toast.LENGTH_LONG).show();
				// Do quit action
				quit();
				finish();
			}
				break;
			case REQUEST_ENTER_CONF:
				cb.requestEnterConference(new Conference((Long) msg.obj),
						new Registrant(this, REQUEST_ENTER_CONF_RESPONSE, null));
				break;
			case REQUEST_ENTER_CONF_RESPONSE:
				JNIResponse recr = (JNIResponse) msg.obj;
				if (recr.getResult() == JNIResponse.Result.SUCCESS) {

				} else if (recr.getResult() == RequestEnterConfResponse.Result.TIME_OUT) {
					Toast.makeText(mContext,
							R.string.error_request_enter_conference_time_out,
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(mContext,
							R.string.error_request_enter_conference,
							Toast.LENGTH_SHORT).show();
				}

				if (mWaitingDialog != null && mWaitingDialog.isShowing()) {
					mWaitingDialog.dismiss();

				}
				break;
			case REQUEST_EXIT_CONF:
				cb.requestExitConference(new Conference((Long) msg.obj), null);
				break;
			// user permission updated
			case NOTIFY_USER_PERMISSION_UPDATED:
				PermissionUpdateIndication ind = (PermissionUpdateIndication) msg.obj;
				if (mAttendeeContainer != null) {
					Attendee pa = new Attendee(GlobalHolder.getInstance()
							.getUser(ind.getUid()));
					mAttendeeContainer.updateAttendeeSpeakingState(pa,
							ConferencePermission.SPEAKING,
							PermissionState.fromInt(ind.getState()));
				}
				if (ind.getUid() == GlobalHolder.getInstance()
						.getCurrentUserId()) {
					if (PermissionState.fromInt(ind.getState()) == PermissionState.GRANTED) {
						mSpeakerIV.setImageResource(R.drawable.speaking_button);
					} else {
						mSpeakerIV.setImageResource(R.drawable.mute_button);
					}
				}
				break;
			case NEW_DOC_NOTIFICATION:
				V2Doc vd = (V2Doc) ((DocumentService.AsyncResult) (msg.obj))
						.getResult();
				synchronized (mDocs) {
					mDocs.put(vd.getId(), vd);
					// check weather received doc before
					if (prLegacy.containsKey(vd.getId())) {
						V2Doc.PageArray vpr = prLegacy.get(vd.getId());
						for (V2Doc.Page p : vpr.getPr()) {
							V2Doc.Page existP = vd.findPage(p.getNo());
							if (existP == null) {
								vd.addPage(p);
							}
						}
						prLegacy.remove(vd.getId());
					}

					if (mDocContainer != null) {
						mDocContainer.addDoc(vd);
					}
				}
				break;
			case DOC_PAGE_NOTIFICATION:
				V2Doc.PageArray vpr = (V2Doc.PageArray) ((DocumentService.AsyncResult) (msg.obj))
						.getResult();
				V2Doc vc = mDocs.get(vpr.getDocId());
				// If doesn't receive doc yet, record page array first for
				// further use.
				if (vc == null) {
					prLegacy.put(vpr.getDocId(), vpr);
					break;
				}
				for (V2Doc.Page p : vpr.getPr()) {
					V2Doc.Page existP = vc.findPage(p.getNo());
					if (existP == null) {
						vc.addPage(p);
					}
				}
				if (mDocContainer != null) {
					mDocContainer.updateLayoutPageInformation();
					mDocContainer.updatePageButton();
				}
				break;
			case DOC_PAGE_ADDED_NOTIFICATION:
				V2Doc.Page vpp = (V2Doc.Page) ((DocumentService.AsyncResult) (msg.obj))
						.getResult();
				if (vpp != null) {
					V2Doc v2d = mDocs.get(vpp.getDocId());
					v2d.addPage(vpp);
				}
				if (mDocContainer != null) {
					mDocContainer.updateLayoutPageInformation();
					mDocContainer.updatePageButton();
				}
				break;
			case DOC_PAGE_ACTIVITE_NOTIFICATION:
				V2Doc.Page vpa = (V2Doc.Page) ((DocumentService.AsyncResult) (msg.obj))
						.getResult();
				if (vpa != null) {
					V2Doc v2d = mDocs.get(vpa.getDocId());
					if (v2d != null) {
						v2d.setActivatePageNo(vpa.getNo());
						if (mDocContainer != null) {
							mDocContainer.updateCurrentDoc(v2d);

						}
						mCurrentActivateDoc = v2d;
					}
				}

				break;

			case DOC_DOWNLOADED_NOTIFICATION:
				V2Doc.Page vp = (V2Doc.Page) ((DocumentService.AsyncResult) (msg.obj))
						.getResult();
				V2Doc cache = mDocs.get(vp.getDocId());
				Page ppC = cache.findPage(vp.getNo());
				if (ppC == null) {
					cache.addPage(vp);
				} else {
					ppC.setFilePath(vp.getFilePath());
				}
				// try to update current doc's bitmap
				if (mDocContainer != null
						&& vp.getDocId().equals(mCurrentActivateDoc.getId())
						&& vp.getNo() == cache.getActivatePageNo()) {
					mDocContainer.updateCurrentDoc(mCurrentActivateDoc);
				}
				break;

			case DOC_CLOSED_NOTIFICATION:
				V2Doc removedDoc = (V2Doc) ((DocumentService.AsyncResult) (msg.obj))
						.getResult();
				synchronized (mDocs) {
					removedDoc = mDocs.get(removedDoc.getId());
					if (removedDoc != null) {
						if (mDocContainer != null) {
							mDocContainer.closeDoc(removedDoc);
						}
						mDocs.remove(removedDoc.getId());
					}
				}
				break;

			case DOC_PAGE_CANVAS_NOTIFICATION:
				V2ShapeMeta shape = (V2ShapeMeta) ((DocumentService.AsyncResult) (msg.obj))
						.getResult();
				synchronized (mDocs) {
					V2Doc ca = mDocs.get(shape.getDocId());
					V2Doc.Page caVp = ca.findPage(shape.getPageNo());
					if (caVp != null) {
						caVp.addMeta(shape);
					} else {
						V2Log.e(" didn't find page for canvas"
								+ shape.getPageNo());
					}
					if (mDocContainer != null
							&& caVp.getDocId().equals(
									mCurrentActivateDoc.getId())
							&& caVp.getNo() == mCurrentActivateDoc
									.getActivatePageNo()) {
						mDocContainer.drawShape(caVp.getVsMeta());
					}
				}
				break;
			case DESKTOP_SYNC_NOTIFICATION:
				int sync = msg.arg1;
				if (mDocContainer != null) {
					if (sync == 1) {
						mDocContainer.updateSyncStatus(true);
					} else {
						mDocContainer.updateSyncStatus(false);
					}
				}
				break;

			case VIDEO_MIX_NOTIFICATION:
				// create mixed video
				if (msg.arg1 == 1) {
					MixVideo mv = (MixVideo) msg.obj;

					MixerWrapper mw = new MixerWrapper(mv.getId(), mv,
							new MixVideoLayout(mContext, mv));
					synchronized (mMixerWrapper) {
						//If exist, do not add again
						if (mMixerWrapper.containsKey(mv.getId())) {
							break;
						}
						mMixerWrapper.put(mv.getId(),mw);
					}
					// Notify attendee list mixed video is created
					if (mAttendeeContainer != null) {
						mAttendeeContainer
								.updateEnteredAttendee(new AttendeeMixedDevice(
										mv));
					}

					// destroy mixed video
				} else if (msg.arg1 == 2) {
					MixVideo mv = (MixVideo) msg.obj;
					MixerWrapper mw = null;
					synchronized (mMixerWrapper) {
						mw = mMixerWrapper.remove(mv.getId());
					}
					// Notify attendee list remove mixed video device
					if (mAttendeeContainer != null && mw != null) {
						mAttendeeContainer.updateExitedAttendee(mw.amd);
					}
					// TODO close all device

					// add mixed video device
				} /*
				 * else if (msg.arg1 == 3) { MixVideo.MixVideoDevice mv =
				 * (MixVideo.MixVideoDevice) msg.obj; MixVideo mix =
				 * mMixerWrapper.get(mv.getMx().getId()).mix; if (mix == null) {
				 * V2Log.e(" Doesn't cache mix: " + mv.getMx().getId()); } else
				 * { mix.addDevice(mv.getUdc(), mv.getPos()); } //remove mixed
				 * video device } else if (msg.arg1 == 4) {
				 * MixVideo.MixVideoDevice mv = (MixVideo.MixVideoDevice)
				 * msg.obj; MixVideo mix =
				 * mMixerWrapper.get(mv.getMx().getId()).mix; if (mix == null) {
				 * V2Log.e(" Doesn't cache mix: " + mv.getMx().getId()); } else
				 * { MixVideo.MixVideoDevice cacheMVD = mix.removeDevice(mv);
				 * //TODO close device } }
				 */
				break;
			}
		}

	}

}
