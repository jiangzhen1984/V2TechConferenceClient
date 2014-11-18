package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import v2av.CaptureCapability;
import v2av.VideoCaptureDevInfo;
import v2av.VideoPlayer;
import v2av.VideoRecorder;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils.TruncateAt;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.AsyncResult;
import com.v2tech.service.ChatService;
import com.v2tech.service.ConferencMessageSyncService;
import com.v2tech.service.ConferenceService;
import com.v2tech.service.DocumentService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.service.jni.PermissionUpdateIndication;
import com.v2tech.util.DensityUtils;
import com.v2tech.util.GlobalState;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.bo.GroupUserObject;
import com.v2tech.view.bo.UserStatusObject;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.vo.Attendee;
import com.v2tech.vo.AttendeeMixedDevice;
import com.v2tech.vo.CameraConfiguration;
import com.v2tech.vo.Conference;
import com.v2tech.vo.ConferenceGroup;
import com.v2tech.vo.ConferencePermission;
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

	private static final int TAG_SUB_WINDOW_STATE_FIXED = 0x1;
	private static final int TAG_SUB_WINDOW_STATE_FLOAT = 0x0;
	private static final int TAG_SUB_WINDOW_STATE_FULL_SCRREN = 0x10;
	private static final int TAG_SUB_WINDOW_STATE_RESTORED = 0x00;

	private static final int ONLY_SHOW_LOCAL_VIDEO = 1;
	private static final int REQUEST_OPEN_DEVICE_RESPONSE = 4;
	private static final int REQUEST_CLOSE_DEVICE_RESPONSE = 5;
	private static final int REQUEST_OPEN_OR_CLOSE_DEVICE = 6;
	private static final int NOTIFICATION_KICKED = 7;
	private static final int NOTIFY_USER_PERMISSION_UPDATED = 11;

	private static final int ATTENDEE_DEVICE_LISTENER = 20;
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

	private static final int TAG_CLOSE_DEVICE = 0;

	private static final int TAG_OPEN_DEVICE = 1;

	public static final String JNI_EVENT_VIDEO_CATEGORY = "com.v2tech.conf_video_event";
	public static final String JNI_EVENT_VIDEO_CATEGORY_OPEN_VIDEO_EVENT_ACTION = "com.v2tech.conf_video_event.open_video_event";
	private static final String TAG = "VideoActivityV2";

	private boolean isSpeaking;

	private Handler mVideoHandler = new VideoHandler();

	private Context mContext;
	private List<SurfaceViewW> mCurrentShowedSV;

	private RelativeLayout mRootContainer;

	private FrameLayout mContentLayoutMain;
	private FrameLayout mVideoLayout;
	private FrameLayout mSubWindowLayout;

	private TextView mGroupNameTV;
	private ImageView mSettingIV;
	private ImageView mQuitIV;
	private ImageView mSpeakerIV;
	private PopupWindow mSettingWindow;
	private Dialog mQuitDialog;
	private VideoInvitionAttendeeLayout mInvitionContainer;
	private VideoMsgChattingLayout mMessageContainer;
	private VideoAttendeeListLayout mAttendeeContainer;
	private VideoDocLayout mDocContainer;

	private ImageView mMenuButton;
	private View mMenuButtonContainer;
	private View mMenuSparationLine;

	private View mMenuInviteAttendeeButton;
	private View mMenuMessageButton;
	private View mMenuAttendeeButton;
	private View mMenuDocButton;
	private View mConverseLocalCameraButton;
	private View mMenuButtonGroup[];

	private View localSurfaceViewLy;
	private SurfaceView mLocalSurface;

	private Conference conf;
	private ConferenceGroup cg;

	private ConferenceService cb;
	private DocumentService ds;
	private ChatService cs = new ChatService();

	private Set<Attendee> mAttendeeList = new HashSet<Attendee>();

	private Map<String, V2Doc> mDocs = new HashMap<String, V2Doc>();
	private String mCurrentActivateDocId = null;

	private SubViewListener subViewListener = new SubViewListener();

	private List<VMessage> mPendingMessageList;

	private List<PermissionUpdateIndication> mPendingPermissionUpdateList;

	private Toast mToast;

	private DisplayMetrics dm;

	private boolean mServiceBound = false;
	private boolean mLocalHolderIsCreate = false;

	private int mVideoMaxCols = 2;

	private int mContentWidth = -1;
	private int mContentHeight = -1;

	private AudioManager audioManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_in_metting);
		mContext = this;
		dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		this.mRootContainer = (RelativeLayout) findViewById(R.id.video_layout_root);
		this.mContentLayoutMain = (FrameLayout) findViewById(R.id.in_meeting_content_main);
		// 用于隐藏输入法
		mContentLayoutMain.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imm != null && v != null) {
					imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				}
				return false;
			}
		});
		this.mCurrentShowedSV = new ArrayList<SurfaceViewW>();

		this.mVideoLayout = new FrameLayout(this);
		mContentLayoutMain.addView(this.mVideoLayout);
		this.mSubWindowLayout = new FrameLayout(this);
		mSubWindowLayout.setVisibility(View.GONE);
		mContentLayoutMain.addView(this.mSubWindowLayout);

		// setting button
		this.mSettingIV = (ImageView) findViewById(R.id.in_meeting_setting_iv);
		this.mSettingIV.setOnClickListener(mShowSettingListener);
		// TODO hide for temp release
		this.mSettingIV.setVisibility(View.INVISIBLE);

		// request exit button
		this.mQuitIV = (ImageView) findViewById(R.id.in_meeting_log_out_iv);
		this.mQuitIV.setOnClickListener(mShowQuitWindowListener);
		// request speak or mute button
		this.mSpeakerIV = (ImageView) findViewById(R.id.speaker_iv);
		this.mSpeakerIV.setOnClickListener(mApplySpeakerListener);
		// conference name text view
		this.mGroupNameTV = (TextView) findViewById(R.id.in_meeting_name);

		// local camera surface view
		this.mLocalSurface = (SurfaceView) findViewById(R.id.local_surface_view);
		mLocalSurface.getHolder().addCallback(mLocalCameraHolder);
		mLocalSurface.setZOrderMediaOverlay(true);

		this.localSurfaceViewLy = findViewById(R.id.local_surface_view_ly);
		localSurfaceViewLy.setOnTouchListener(mLocalCameraDragListener);

		// show menu list(Include show message layout, show attendee list layout
		// show document layout) button
		mMenuButton = (ImageView) findViewById(R.id.in_meeting_menu_button);
		mMenuButton.setOnClickListener(mMenuButtonListener);
		mMenuButtonContainer = findViewById(R.id.in_meeting_menu_layout);
		mMenuSparationLine = findViewById(R.id.in_meeting_video_separation_line0);

		// show message layout button
		mMenuMessageButton = findViewById(R.id.in_meeting_menu_show_msg_button);
		mMenuMessageButton.setTag("msg");
		mMenuMessageButton.setOnClickListener(mMenuShowButtonListener);
		// show attendee list layout button
		mMenuAttendeeButton = findViewById(R.id.in_meeting_menu_show_attendees_button);
		mMenuAttendeeButton.setTag("attendee");
		mMenuAttendeeButton.setOnClickListener(mMenuShowButtonListener);

		// show document display button
		mMenuDocButton = findViewById(R.id.in_meeting_menu_show_doc_button);
		mMenuDocButton.setTag("doc");
		mMenuDocButton.setOnClickListener(mMenuShowButtonListener);

		// show invition button
		mMenuInviteAttendeeButton = findViewById(R.id.in_meeting_menu_show_invition_attendees_button);
		mMenuInviteAttendeeButton.setTag("invition");
		mMenuInviteAttendeeButton.setOnClickListener(mMenuShowButtonListener);

		mConverseLocalCameraButton = findViewById(R.id.converse_camera_button);
		mConverseLocalCameraButton.setOnClickListener(mConverseCameraListener);

		mMenuButtonGroup = new View[] { mMenuInviteAttendeeButton,
				mMenuMessageButton, mMenuAttendeeButton, mMenuDocButton };
		// Initialize broadcast receiver
		initConfsListener();
		// Initialize conference object and show local camera
		init();

		// make sure local is in front of any view
		localSurfaceViewLy.bringToFront();

		// Start animation
		this.overridePendingTransition(R.animator.nonam_scale_center_0_100,
				R.animator.nonam_scale_null);

		bindService(new Intent(mContext, ConferencMessageSyncService.class),
				mLocalServiceConnection, Context.BIND_AUTO_CREATE);

		audioManager = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);

		// Broadcast for user joined conference, to inform that quit P2P
		// conversation
		broadcastForJoined();
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (mServiceBound) {
			suspendOrResume(true);
		} else {
			// Set audio use speaker phone
			updateAudioSpeaker(true);
		}
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.cancel(PublicIntent.VIDEO_NOTIFICATION_ID);
		// keep screen on
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Adjust content layout
		adjustContentLayout();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Conference newConf = (Conference) intent.getExtras().get("conf");
		if (newConf != null && newConf.getId() != conf.getId()) {
			// Pop up dialog to exit conference
			showQuitDialog(mContext
					.getText(R.string.in_meeting_quit_text_for_new).toString()
					.replace("[]", conf.getName()));
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		mContentLayoutMain.measure(View.MeasureSpec.EXACTLY,
				View.MeasureSpec.EXACTLY);
		this.mContentWidth = -1;
		this.mContentHeight = -1;
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
		filter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);

		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_USER_PRESENT);

		mContext.registerReceiver(mConfUserChangeReceiver, filter);

	}

	private void init() {
		conf = (Conference) this.getIntent().getExtras().get("conf");

		cg = (ConferenceGroup) GlobalHolder.getInstance().getGroupById(
				V2GlobalEnum.GROUP_TYPE_CONFERENCE, conf.getId());
		if (cg == null) {
			V2Log.e(" doesn't receive group information  yet");
			return;
		}
		mGroupNameTV.setText(cg.getName());

		Group confGroup = GlobalHolder.getInstance()
				.findGroupById(conf.getId());
		// load conference attendee list
		if (confGroup != null) {
			List<User> l = confGroup.getUsers();
			for (User u : l) {
				mAttendeeList.add(new Attendee(u));
			}
		}
		V2Log.i(" Conference size:" + mAttendeeList.size());

		mPendingMessageList = new ArrayList<VMessage>();

		mPendingPermissionUpdateList = new ArrayList<PermissionUpdateIndication>();
		// Set default speaking state if current user is owner, then should
		// apply speaking default;
		isSpeaking = (conf.getCreator() == GlobalHolder.getInstance()
				.getCurrentUserId() || conf.getChairman() == GlobalHolder
				.getInstance().getCurrentUserId()) ? true : false;
	}

	/**
	 * Broadcast for user joined conference, to inform that quit P2P
	 * conversation
	 */
	private void broadcastForJoined() {
		Intent i = new Intent(
				PublicIntent.BROADCAST_JOINED_CONFERENCE_NOTIFICATION);
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);

		i.putExtra("confid", conf.getId());
		sendBroadcast(i);
	}

	/**
	 * Update speaker icon and state
	 * 
	 * @param flag
	 */
	private void updateSpeakerState(boolean flag) {
		isSpeaking = flag;
		// set flag to speaking icon
		if (!flag) {
			mSpeakerIV.setImageResource(R.drawable.mute_button);
		} else {
			mSpeakerIV.setImageResource(R.drawable.speaking_button);
		}
	}

	private void showOrHideSubWindow(View content) {
		View currentChild = null;
		if (mSubWindowLayout.getChildCount() > 0) {
			currentChild = mSubWindowLayout.getChildAt(0);
		}

		int visible = View.VISIBLE;
		// Update content
		if (currentChild != content) {
			mSubWindowLayout.removeAllViews();
			FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(
					FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.MATCH_PARENT);
			fl.leftMargin = 0;
			fl.topMargin = 0;
			mSubWindowLayout.addView(content, fl);
			// If content is different, always visible
			visible = View.VISIBLE;
		} else {
			// Otherwise check current visibility state.
			if (mSubWindowLayout.getVisibility() == View.VISIBLE) {
				visible = View.GONE;
			} else if (mSubWindowLayout.getVisibility() == View.GONE) {
				visible = View.VISIBLE;
			}
		}

		// Show or hide sub window with animation
		Animation animation = null;
		if (visible == View.GONE) {
			animation = new ScaleAnimation(1.0F, 0.0f, 1.0F, 1.0f,
					Animation.RELATIVE_TO_SELF, 0.0f,
					Animation.RELATIVE_TO_SELF, 1.0f);
		} else if (visible == View.VISIBLE) {
			animation = new ScaleAnimation(0.0F, 1.0f, 1.0F, 1.0f,
					Animation.RELATIVE_TO_SELF, 0.0f,
					Animation.RELATIVE_TO_SELF, 1.0f);

		}
		animation.setDuration(400);
		mSubWindowLayout.setVisibility(visible);
		mSubWindowLayout.startAnimation(animation);
		adjustContentLayout();
	}

	private View initMsgLayout() {
		if (mMessageContainer != null) {
			return mMessageContainer;
		}
		mMessageContainer = new VideoMsgChattingLayout(this, cg);
		mMessageContainer.setListener(subViewListener);

		// Populate message
		for (int i = 0; i < mPendingMessageList.size(); i++) {
			mMessageContainer.addNewMessage(mPendingMessageList.get(i));
		}
		mMessageContainer.requestScrollToNewMessage();
		mPendingMessageList.clear();
		return mMessageContainer;
	}

	private View initAttendeeContainer() {
		if (mAttendeeContainer == null) {
			mAttendeeContainer = new VideoAttendeeListLayout(conf, this);
			mAttendeeContainer.setAttendsList(this.mAttendeeList);

			mAttendeeContainer.bringToFront();
			mAttendeeContainer.setListener(subViewListener);
			// Initialize speaking
			if (isSpeaking) {

				Attendee pa = null;
				for (Attendee att : mAttendeeList) {
					if (att.getAttId() == GlobalHolder.getInstance()
							.getCurrentUser().getmUserId()) {
						pa = att;
					}
				}

				if (pa == null) {
					pa = new Attendee(GlobalHolder.getInstance()
							.getCurrentUser());
					mAttendeeList.add(pa);
				}
				pa.setSpeakingState(true);
				mAttendeeContainer.updateAttendeeSpeakingState(pa);
			}
			// Update pending attendee state
			for (PermissionUpdateIndication ind : mPendingPermissionUpdateList) {
				updateAttendeePermissionStateIcon(ind);
			}
			mPendingPermissionUpdateList.clear();

		} else {
			mAttendeeContainer.updateStatist();
			return mAttendeeContainer;
		}
		return mAttendeeContainer;
	}

	private View initDocLayout() {

		if (mDocContainer == null) {
			mDocContainer = new VideoDocLayout(this, mDocs,
					mCurrentActivateDocId);
			mDocContainer.setListener(subViewListener);
			Group g = GlobalHolder.getInstance().findGroupById(conf.getId());
			if (g != null && g instanceof ConferenceGroup) {
				mDocContainer.updateSyncStatus(((ConferenceGroup) g).isSyn());
			}

		} else
			mDocContainer.updateCurrentDoc();
		return mDocContainer;
	}

	private View initInvitionContainer() {
		if (mInvitionContainer == null) {
			mInvitionContainer = new VideoInvitionAttendeeLayout(this, conf);
			mInvitionContainer.setListener(subViewListener);

		}
		return mInvitionContainer;
	}

	private void adjustContentLayout() {
		int width, height;
		int marginLeft = 0;

		// Calculate offset
		if (mMenuButtonContainer.getVisibility() == View.VISIBLE) {
			if (mMenuButtonContainer.getMeasuredWidth() == 0) {
				mMenuButtonContainer.measure(View.MeasureSpec.UNSPECIFIED,
						View.MeasureSpec.UNSPECIFIED);
			}
			marginLeft = mMenuButtonContainer.getMeasuredWidth();
		}

		if (mSubWindowLayout.getVisibility() == View.VISIBLE) {

			FrameLayout.LayoutParams fl = (FrameLayout.LayoutParams) mSubWindowLayout
					.getLayoutParams();
			if (mContentLayoutMain.getMeasuredWidth() == 0
					|| mContentWidth == -1 || mContentHeight == -1) {
				mContentLayoutMain.measure(View.MeasureSpec.UNSPECIFIED,
						View.MeasureSpec.UNSPECIFIED);
			}

			if (mContentWidth == -1) {
				mContentWidth = mContentLayoutMain.getWidth();
			}
			if (mContentHeight == -1) {
				mContentHeight = mContentLayoutMain.getHeight();
			}

			int flag = getSubViewWindowState();
			// If sub window request full screen
			if ((flag & TAG_SUB_WINDOW_STATE_FULL_SCRREN) == TAG_SUB_WINDOW_STATE_FULL_SCRREN) {
				width = (mContentWidth - marginLeft);
			} else {
				width = (mContentWidth - marginLeft) / 2;
			}

			height = mContentHeight;

			if (fl == null) {
				fl = new FrameLayout.LayoutParams(width, height);
			} else {
				fl.width = width;
				fl.height = height;
			}
			fl.leftMargin = marginLeft;

			mContentLayoutMain.updateViewLayout(mSubWindowLayout, fl);

			// Update left offset for video layout
			if ((flag & TAG_SUB_WINDOW_STATE_FIXED) == TAG_SUB_WINDOW_STATE_FIXED) {
				marginLeft += width;
			}
		}

		// Update width and height for video layout
		FrameLayout.LayoutParams fl = (FrameLayout.LayoutParams) mVideoLayout
				.getLayoutParams();
		width = mContentWidth - marginLeft;
		height = mContentHeight;
		if (fl == null) {
			fl = new FrameLayout.LayoutParams(width, height);
		}

		if (fl.leftMargin == marginLeft && fl.width == width
				&& fl.height == height) {

		} else {
			fl.width = width;
			fl.height = height;

			fl.leftMargin = marginLeft;
			mContentLayoutMain.updateViewLayout(mVideoLayout, fl);
			//
			this.adjustVideoLayout();
		}

		// make sure local is in front of any view
		localSurfaceViewLy.bringToFront();
	}

	/**
	 * Call this before {@link #adjustContentLayout}
	 */
	private void requestSubViewFixed() {
		Integer flag = (Integer) mSubWindowLayout.getTag();
		if (flag == null) {
			mSubWindowLayout
					.setTag(Integer.valueOf(TAG_SUB_WINDOW_STATE_FIXED));
		} else {
			mSubWindowLayout.setTag(Integer.valueOf(flag.intValue()
					| TAG_SUB_WINDOW_STATE_FIXED));
		}

	}

	/**
	 * Call this before {@link #adjustContentLayout}
	 */
	private void requestSubViewFloat() {
		Integer flag = (Integer) mSubWindowLayout.getTag();
		if (flag == null) {
			mSubWindowLayout
					.setTag(Integer.valueOf(TAG_SUB_WINDOW_STATE_FLOAT));
		} else {
			mSubWindowLayout.setTag(Integer.valueOf(flag.intValue()
					& TAG_SUB_WINDOW_STATE_FLOAT));
		}
	}

	/**
	 * Call this before {@link #adjustContentLayout}
	 */
	private void requestSubViewFillScreen() {
		Integer flag = (Integer) mSubWindowLayout.getTag();
		if (flag == null) {
			mSubWindowLayout.setTag(Integer
					.valueOf(TAG_SUB_WINDOW_STATE_FULL_SCRREN));
		} else {
			mSubWindowLayout.setTag(Integer.valueOf(flag.intValue()
					| TAG_SUB_WINDOW_STATE_FULL_SCRREN));
		}
	}

	/**
	 * Call this before {@link #adjustContentLayout}
	 */
	private void requestSubViewRestore() {
		Integer flag = (Integer) mSubWindowLayout.getTag();
		if (flag == null) {
			mSubWindowLayout.setTag(Integer
					.valueOf(TAG_SUB_WINDOW_STATE_RESTORED));
		} else {
			mSubWindowLayout.setTag(Integer.valueOf(flag.intValue()
					& TAG_SUB_WINDOW_STATE_RESTORED));
		}
	}

	private int getSubViewWindowState() {
		Integer flag = (Integer) mSubWindowLayout.getTag();
		if (flag == null) {
			return 0;
		} else {
			return flag.intValue();
		}
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
				((ImageView) view)
						.setImageResource(R.drawable.video_menu_button);
				// If menu layout is visible, hide line of below at menu button
				mMenuSparationLine.setVisibility(View.GONE);

			} else {
				// Do not hide other window
				// showOrHidenAttendeeContainer(View.GONE);
				// showOrHidenMsgContainer(View.GONE);
				// showOrHidenDocContainer(View.GONE);
				Animation animation = AnimationUtils.loadAnimation(mContext,
						R.animator.nonam_scale_y_100_0);
				animation.setDuration(400);
				mMenuButtonContainer.startAnimation(animation);
				mMenuButtonContainer.setVisibility(View.GONE);
				((ImageView) view)
						.setImageResource(R.drawable.video_menu_button_pressed);

				// If menu layout is invisible, show line of below at menu
				// button
				mMenuSparationLine.setVisibility(View.VISIBLE);
			}

			adjustContentLayout();
		}

	};

	private OnClickListener mMenuShowButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			View content = null;
			// Call cancel full screen request first.
			// because we can't make sure user press invitation layout or not,
			// we have to restore first.
			requestSubViewRestore();
			if (v.getTag().equals("msg")) {
				content = initMsgLayout();
				// If last state is fixed
				if (mMessageContainer.getWindowSizeState()) {
					requestSubViewFixed();
				}

			} else if (v.getTag().equals("attendee")) {
				content = initAttendeeContainer();
				// If last state is fixed
				if (mAttendeeContainer.getWindowSizeState()) {
					requestSubViewFixed();
				}

			} else if (v.getTag().equals("doc")) {
				content = initDocLayout();

				if (mDocContainer.isFullScreenSize()) {
					requestSubViewFillScreen();
				} else {
					requestSubViewFixed();
				}

			} else if (v.getTag().equals("invition")) {
				if (cg.isCanInvitation()) {
					// Make sure invitation layout fill full screen
					requestSubViewFillScreen();
					content = initInvitionContainer();

				} else {
					Toast.makeText(mContext,
							R.string.error_no_permission_to_invitation,
							Toast.LENGTH_SHORT).show();
				}

			}

			// Update button background
			for (View button : mMenuButtonGroup) {
				int c = 0;
				if (button == v) {
					c = mContext.getResources().getColor(
							R.color.confs_common_bg);
				} else {
					c = Color.rgb(255, 255, 255);
				}

				button.setBackgroundColor(c);
			}

			if (content != null) {
				showOrHideSubWindow(content);
			}

			// Make sure local camera is first front of all
			localSurfaceViewLy.bringToFront();
		}

	};

	private OnClickListener mApplySpeakerListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			doApplyOrReleaseSpeak(!isSpeaking);
			// Make sure update start after send request,
			// because update state will update isSpeaking value
			updateSpeakerState(!isSpeaking);
		}
	};

	private OnClickListener mShowQuitWindowListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			showQuitDialog(mContext.getText(R.string.in_meeting_quit_text)
					.toString());
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

				int height = (int) (dm.heightPixels * 0.5);
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

				SeekBar sbar = (SeekBar) view
						.findViewById(R.id.camera_setting_quality);
				sbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					@Override
					public void onProgressChanged(SeekBar sb, int v,
							boolean flag) {

						VideoCaptureDevInfo.VideoCaptureDevice vcd = VideoCaptureDevInfo
								.CreateVideoCaptureDevInfo().GetDevice(
										VideoCaptureDevInfo
												.CreateVideoCaptureDevInfo()
												.GetDefaultDevName());
						if (v >= vcd.capabilites.size()) {
							return;
						}

						int width = 176;
						int height = 144;
						int bitrate = 256000;
						int fps = 15;

						int index = 0;
						for (CaptureCapability cp : vcd.capabilites) {
							if (index == v) {
								width = cp.width;
								height = cp.height;
							}
							index++;
						}

						// // close local camera
						Message.obtain(
								mVideoHandler,
								REQUEST_OPEN_OR_CLOSE_DEVICE,
								0,
								0,
								new UserDeviceConfig(
										V2GlobalEnum.GROUP_TYPE_CONFERENCE,
										conf.getId(), GlobalHolder
												.getInstance()
												.getCurrentUserId(), "", null))
								.sendToTarget();
						//

						VideoCaptureDevInfo.CreateVideoCaptureDevInfo()
								.SetCapParams(width, height, bitrate, fps,
										ImageFormat.NV21);

						mVideoHandler.postDelayed(new Runnable() {

							@Override
							public void run() {
								closeLocalCamera();
								showLocalSurViewOnly();
							}

						}, 500);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						seekBar.getVerticalScrollbarPosition();
						System.out.println("===");

					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						System.out.println("===");
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
				long mid = intent.getLongExtra("mid", 0);
				VMessage vm = MessageLoader.loadGroupMessageById(mContext,
						V2GlobalEnum.GROUP_TYPE_CONFERENCE,
						intent.getLongExtra("groupID", 0), mid);
				if (mMessageContainer != null) {
					mMessageContainer.addNewMessage(vm);
				} else {
					mPendingMessageList.add(vm);
				}
				V2Log.e(TAG, "JNI_BROADCAST_NEW_CONF_MESSAGE is coming");
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {
				long confID = intent.getLongExtra("gid", 0);
				if (confID == conf.getId()) {
					Group confGroup = GlobalHolder.getInstance().findGroupById(
							conf.getId());
					// load conference attendee list
					List<Attendee> list = new ArrayList<Attendee>();
					if (confGroup != null) {
						List<User> l = confGroup.getUsers();
						for (User u : l) {
							Attendee at = new Attendee(u);
							boolean bt = mAttendeeList.add(at);
							if (bt) {
								list.add(at);
							}
						}
					}
					if (mAttendeeContainer != null) {
						mAttendeeContainer.addNewAttendee(list);
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
				if (code == NetworkStateCode.CONNECTED_ERROR) {
					finish();
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
				UserStatusObject us = (UserStatusObject) intent.getExtras()
						.get("status");
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
				finish();
			} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {

			} else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {

			}

		}

	};

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
	private void showLocalSurViewOnly() {
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
			udc = new UserDeviceConfig(V2GlobalEnum.GROUP_TYPE_CONFERENCE,
					conf.getId(), atd.getAttId(), "", null);
			// Make sure current user device is enable
			udc.setEnable(true);
			atd.addDevice(udc);
		}

		// layout must before open device
		// showOrCloseAttendeeVideo(udc);
		udc.setSVHolder(mLocalSurface);
		VideoRecorder.VideoPreviewSurfaceHolder = udc.getSVHolder().getHolder();
		VideoRecorder.VideoPreviewSurfaceHolder
				.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		// VideoCaptureDevInfo.CreateVideoCaptureDevInfo()
		// .updateCameraOrientation(Surface.ROTATION_0);
		VideoRecorder.DisplayRotation = getDisplayRotation();

		openLocalCamera();
		udc.setShowing(true);
	}

	private int getDisplayRotation() {
		if (Build.VERSION.SDK_INT > 7) {
			int rotation = getWindowManager().getDefaultDisplay().getRotation();
			switch (rotation) {
			case Surface.ROTATION_0:
				return 0;
			case Surface.ROTATION_90:
				return 90;
			case Surface.ROTATION_180:
				return 180;
			case Surface.ROTATION_270:
				return 270;
			}
		}

		return 0;
	}

	private void closeLocalCamera() {
		try {
			Message.obtain(
					mVideoHandler,
					REQUEST_OPEN_OR_CLOSE_DEVICE,
					0,
					0,
					new UserDeviceConfig(V2GlobalEnum.GROUP_TYPE_CONFERENCE,
							conf.getId(), GlobalHolder.getInstance()
									.getCurrentUserId(), "", null))
					.sendToTarget();
		} catch (Exception e) {

		}
	}

	private void openLocalCamera() {
		Message.obtain(
				mVideoHandler,
				REQUEST_OPEN_OR_CLOSE_DEVICE,
				1,
				0,
				new UserDeviceConfig(V2GlobalEnum.GROUP_TYPE_CONFERENCE, conf
						.getId(),
						GlobalHolder.getInstance().getCurrentUserId(), "", null))
				.sendToTarget();
	}

	private void adjustVideoLayout() {
		int marginTop = 0;
		int marginLeft = 0;
		int size = mCurrentShowedSV.size();
		int rows = size / mVideoMaxCols + (size % mVideoMaxCols == 0 ? 0 : 1);
		int cols = size > 1 ? mVideoMaxCols : size;
		if (size == 0) {
			V2Log.e(" No remote device need to show size:" + size);
			return;
		}

		Rect outR = new Rect();
		mVideoLayout.getDrawingRect(outR);
		int[] po = new int[2];
		mVideoLayout.getLocationInWindow(po);
		mVideoLayout
				.measure(View.MeasureSpec.EXACTLY, View.MeasureSpec.EXACTLY);
		// First extra from layout parameter. Because if doc layout request
		// fixed position,
		// will set layout width parameter. At here measure doesn't work with
		// layoutparameter
		int containerW = mVideoLayout.getLayoutParams().width;
		if (containerW <= 0) {
			containerW = mVideoLayout.getWidth();
		}
		int containerH = outR.bottom - outR.top;

		int normalW = containerW / cols, normalH = normalW / 4 * 3;
		if (normalH * rows > containerH) {
			normalH = containerH / rows;
			normalW = normalH / 3 * 4;
		}

		int fixedWidth = normalW;
		int fixedHeight = normalH;
		fixedWidth -= fixedWidth % 16;
		fixedHeight -= fixedHeight % 16;

		marginTop = Math.abs(containerH - fixedHeight * rows) / 2;
		marginLeft = Math.abs(containerW - fixedWidth * cols) / 2;

		int index = 0;
		for (SurfaceViewW sw : mCurrentShowedSV) {
			View v = mVideoLayout.findViewById(sw.getView().getId());
			FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
					fixedWidth, fixedHeight);
			int row = index / mVideoMaxCols;
			int column = index % mVideoMaxCols;
			p.leftMargin = marginLeft + column * fixedWidth;
			p.topMargin = marginTop + row * fixedHeight;
			if (v != null) {
				mVideoLayout.updateViewLayout(v, p);
			} else {
				if (sw.getView().getParent() != null) {
					((ViewGroup) sw.getView().getParent()).removeView(sw
							.getView());
				}
				mVideoLayout.addView(sw.getView(), p);
			}

			index++;
		}

		// make sure local camera is first of all
		localSurfaceViewLy.bringToFront();
	}

	@Override
	protected void onStop() {
		super.onStop();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (mServiceBound) {
			suspendOrResume(false);
		} else {
			updateAudioSpeaker(false);
		}
	}

	@Override
	public void finish() {
		super.finish();
		// this.overridePendingTransition(R.animator.nonam_scale_null,
		// R.animator.nonam_scale_center_100_0);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		moveTaskToBack(true);
	}

	@Override
	protected void onDestroy() {
		quit();
		mContext.unregisterReceiver(mConfUserChangeReceiver);
		super.onDestroy();
		mAttendeeList.clear();
		if (mCurrentShowedSV != null) {
			mCurrentShowedSV.clear();
		}
		mVideoLayout.removeAllViews();
		if (mDocContainer != null) {
			// clean document bitmap cache
			mDocContainer.cleanCache();
		}

		if (mServiceBound) {
			cb.unRegisterPermissionUpdateListener(mVideoHandler,
					NOTIFY_USER_PERMISSION_UPDATED, null);
			cb.removeRegisterOfKickedConfListener(mVideoHandler,
					NOTIFICATION_KICKED, null);
			cb.removeAttendeeListener(this.mVideoHandler, ATTENDEE_LISTENER,
					null);

			cb.unRegisterVideoMixerListener(mVideoHandler,
					VIDEO_MIX_NOTIFICATION, null);

			ds.unRegisterNewDocNotification(mVideoHandler,
					NEW_DOC_NOTIFICATION, null);
			ds.unRegisterDocDisplayNotification(mVideoHandler,
					DOC_DOWNLOADED_NOTIFICATION, null);
			ds.unRegisterDocClosedNotification(mVideoHandler,
					DOC_CLOSED_NOTIFICATION, null);
			ds.unRegisterDocPageAddedNotification(mVideoHandler,
					DOC_PAGE_ADDED_NOTIFICATION, null);
			ds.unRegisterPageCanvasUpdateNotification(mVideoHandler,
					DOC_PAGE_CANVAS_NOTIFICATION, null);
			cb.removeSyncDesktopListener(mVideoHandler,
					DESKTOP_SYNC_NOTIFICATION, null);
			cb.removeAttendeeDeviceListener(mVideoHandler,
					ATTENDEE_DEVICE_LISTENER, null);

			unbindService(mLocalServiceConnection);
		}
		// call clear function from all service
		ds.clearCalledBack();
		cb.clearCalledBack();
		cs.clearCalledBack();

		mContext.stopService(new Intent(mContext,
				ConferencMessageSyncService.class));
		// clear current meeting state
		GlobalHolder.getInstance().setMeetingState(false, 0);
		// clear messages
		// MessageLoader.deleteGroupMessage(mContext,
		// V2GlobalEnum.GROUP_TYPE_CONFERENCE , conf.getId());
		mVideoHandler = null;

		audioManager.setMode(AudioManager.MODE_NORMAL);
		audioManager.setSpeakerphoneOn(false);
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
		moveTaskToBack(true);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	/**
	 * Update speaker flag according headset state
	 * 
	 * @param flag
	 *            true means start, false means on stop
	 */
	private void updateAudioSpeaker(boolean flag) {
		audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

		GlobalState gs = GlobalHolder.getInstance().getGlobalState();
		if (gs.isBluetoothHeadsetPluged() || gs.isWiredHeadsetPluged()) {
			audioManager.setSpeakerphoneOn(false);
		} else {
			audioManager.setSpeakerphoneOn(true);
		}

	}

	private void showQuitDialog(String content) {
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
					finish();
				}

			});
			mQuitDialog = d;
		}

		TextView v = (TextView) mQuitDialog
				.findViewById(R.id.in_meeting_quit_window_content);
		v.setText(content);

		mQuitDialog.show();
	}

	/**
	 * reverse local camera from back to front or from front to back
	 */
	private void doReverseCamera() {

		boolean flag = VideoCaptureDevInfo.CreateVideoCaptureDevInfo()
				.reverseCamera();
		if (flag) {
			cb.updateCameraParameters(new CameraConfiguration(""), null);
		}
		return;
	}

	/**
	 * Use to suspend or resume current conference's all video device.<br>
	 * If current activity do stop then suspend, current activity do start then
	 * resume.
	 */
	private void suspendOrResume(boolean resume) {
		if (resume) {

			updateAllRemoteDevice(TAG_OPEN_DEVICE);

			adjustVideoLayout();

			// Send speaking status
			doApplyOrReleaseSpeak(isSpeaking);
			// Make sure update start after send request,
			// because update state will update isSpeaking value
			updateSpeakerState(isSpeaking);
			// Resume audio
			cb.updateAudio(true);

			// close local camera
			openLocalCamera();

		} else {
			updateAllRemoteDevice(TAG_CLOSE_DEVICE);

			closeLocalCamera();

			mVideoLayout.removeAllViews();
			// suspend audio
			cb.updateAudio(false);

			// close local camera
			closeLocalCamera();
		}
		updateAudioSpeaker(resume);
	}

	private void updateAllRemoteDevice(int tag) {
		for (SurfaceViewW sw : this.mCurrentShowedSV) {
			if (TAG_OPEN_DEVICE == tag) {
				sw.observer.setValid(true);
				sw.observer.open();
			} else {
				sw.observer.setValid(false);
				sw.observer.close();
			}
		}
	}

	/**
	 * user quit conference, however positive or negative
	 */
	private void quit() {
		Intent i = new Intent();
		i.putExtra("gid", conf.getId());
		setResult(0, i);

		// if bound, then conference service is initialized. Otherwise not.
		if (mServiceBound) {
			updateAllRemoteDevice(TAG_CLOSE_DEVICE);

			// close local camera
			closeLocalCamera();
		}
		VideoRecorder.VideoPreviewSurfaceHolder = null;
		mAttendeeList.clear();
		mCurrentShowedSV.clear();
	}

	@Override
	public void onBackPressed() {
		showQuitDialog(mContext.getText(R.string.in_meeting_quit_text)
				.toString());
	}

	private void doApplyOrReleaseSpeak(boolean flag) {
		if (!flag) {
			cb.applyForReleasePermission(ConferencePermission.SPEAKING, null);
		} else {
			cb.applyForControlPermission(ConferencePermission.SPEAKING, null);
		}
	}

	/**
	 * Handle event which new user entered conference
	 * 
	 * @param user
	 */
	private void doHandleNewUserEntered(Attendee att) {
		if (att == null) {
			return;
		}

		att.setJoined(true);
		if (conf.getChairman() == att.getAttId()) {
			att.setChairMan(true);
		}

		List<UserDeviceConfig> devs = att.getmDevices();
		if (devs == null || devs.size() <= 0) {
			List<UserDeviceConfig> ld = GlobalHolder.getInstance()
					.getAttendeeDevice(att.getAttId());
			att.setmDevices(ld);
		}

		if (mAttendeeContainer != null) {
			mAttendeeContainer.updateEnteredAttendee(att);
		}

		showToastNotification(att.getAttName()
				+ mContext.getText(R.string.conf_notification_joined_meeting));
	}

	private void showToastNotification(String text) {
		if (mToast != null) {
			mToast.cancel();
		}
		mToast = Toast.makeText(mContext, text, Toast.LENGTH_SHORT);
		mToast.show();
	}

	/**
	 * Handle event which user exited conference
	 * 
	 * @param user
	 */
	private void doHandleUserExited(Attendee att) {
		if (att == null) {
			V2Log.e("Attendee is null");
			return;
		}

		boolean layoutChanged = false;
		for (int i = 0; i < mCurrentShowedSV.size(); i++) {
			SurfaceViewW svw = mCurrentShowedSV.get(i);
			if (att.getAttId() == svw.at.getAttId()) {
				svw.observer.close();
				svw.udc.setShowing(false);
				mCurrentShowedSV.remove(svw);
				mVideoLayout.removeView(svw.getView());
				svw.rl.removeAllViews();
				i--;
				layoutChanged = true;
			}
		}

		// adjust layout if we closed video
		if (layoutChanged) {
			adjustVideoLayout();
		}

		if (conf.getChairman() == att.getAttId()) {
			att.setChairMan(true);
		}
		att.setJoined(false);
		if (mAttendeeContainer != null) {
			mAttendeeContainer.updateExitedAttendee(att);
		}

		// Clean user device
		showToastNotification(att.getAttName()
				+ mContext.getText(R.string.conf_notification_quited_meeting));

	}

	private boolean checkVideoExceedMaminum() {
		return mCurrentShowedSV.size() >= 4;
	}

	/**
	 * show remote user video
	 * 
	 * @param udc
	 */
	private boolean showOrCloseAttendeeVideo(UserDeviceConfig udc) {
		if (udc == null) {
			V2Log.e(" can't not open or close device");
			return false;
		}
		// if already opened attendee's video, switch action to close
		if (udc.isShowing()) {
			for (SurfaceViewW sw : mCurrentShowedSV) {
				if (sw.udc == udc) {
					sw.observer.close();
					mCurrentShowedSV.remove(sw);
					mVideoLayout.removeView(sw.getView());
					sw.rl.removeAllViews();
					break;
				}
			}
			udc.setShowing(false);
			udc.doClose();
			adjustVideoLayout();
			return false;
		} else {
			if (checkVideoExceedMaminum()) {
				Toast.makeText(mContext,
						R.string.error_exceed_support_video_count,
						Toast.LENGTH_SHORT).show();
				return false;
			}
			VideoPlayer vp = new VideoPlayer();
			udc.setSVHolder(new SurfaceView(this));
			udc.setVp(vp);
			SurfaceHolderObserver observer = new SurfaceHolderObserver(cg, cs,
					udc);
			SurfaceViewW sw = new SurfaceViewW(udc.getBelongsAttendee(), udc,
					observer);
			mCurrentShowedSV.add(sw);

			// Do adjust layout first, then request open device.
			// otherwise can't show video
			adjustVideoLayout();
			// Request open device
			sw.observer.open();

			udc.setShowing(true);
			return true;
		}

	}

	/**
	 * Update attendee state
	 * 
	 * @param ind
	 */
	private boolean updateAttendeePermissionStateIcon(
			PermissionUpdateIndication ind) {
		Attendee pa = null;
		for (Attendee att : mAttendeeList) {
			if (att.getAttId() == ind.getUid()) {
				pa = att;
			}
		}

		if (pa != null && mAttendeeContainer != null) {
			ConferencePermission cp = ConferencePermission.fromInt(ind
					.getType());
			PermissionState ps = PermissionState.fromInt(ind.getState());
			if (cp == ConferencePermission.SPEAKING
					&& ps == PermissionState.GRANTED) {
				pa.setSpeakingState(true);
			} else {
				pa.setSpeakingState(false);
			}
			mAttendeeContainer.updateAttendeeSpeakingState(pa);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 
	 * @param at
	 * @param devices
	 */
	private void updateAttendeeDevice(Attendee at,
			List<UserDeviceConfig> devices) {
		boolean layoutChanged = false;

		boolean closeFlag = false;
		for (int i = 0; i < mCurrentShowedSV.size(); i++) {
			SurfaceViewW svw = mCurrentShowedSV.get(i);
			if (at.getAttId() != svw.at.getAttId()) {
				closeFlag = false;
				continue;
			} else {
				closeFlag = true;
			}

			for (int j = 0; j < devices.size(); j++) {
				UserDeviceConfig ud = devices.get(j);
				// If remote user disable local camera device which
				// local user already opened
				if (svw.udc.getDeviceID().equals(ud.getDeviceID())) {
					if (!ud.isEnable()) {
						closeFlag = true;
					} else {
						closeFlag = false;
						// Update already opened device
						devices.set(j, svw.udc);
					}
				}

			}
			// Need to close video
			if (closeFlag) {
				layoutChanged = true;
				svw.observer.close();
				mCurrentShowedSV.remove(i);
				mVideoLayout.removeView(svw.getView());
				i--;
			}

		}

		if (mAttendeeContainer != null) {
			mAttendeeContainer.resetAttendeeDevices(at, devices);
		}
		// adjust layout if we closed video
		if (layoutChanged) {
			adjustVideoLayout();
		}
	}

	/**
	 * 
	 * @param res
	 * @param opt
	 */
	private void updateDocNotification(AsyncResult res, int opt) {
		V2Doc doc = null;
		V2Doc.Page page = null;
		V2Doc.PageArray pageArray = null;
		V2ShapeMeta shape = null;
		if (opt == NEW_DOC_NOTIFICATION) {
			doc = (V2Doc) res.getResult();
			V2Doc cacheDoc = mDocs.get(doc.getId());
			if (cacheDoc == null) {
				mDocs.put(doc.getId(), doc);
			} else {
				cacheDoc.updateDoc(doc);
				doc = cacheDoc;
			}
		} else if (opt == DOC_CLOSED_NOTIFICATION) {
			doc = (V2Doc) res.getResult();
			// Notice need to use cache document
			// because cache document object is different from JNI's callback
			doc = mDocs.remove(doc.getId());
		}

		String docId = null;
		switch (opt) {
		case NEW_DOC_NOTIFICATION:
			doc = (V2Doc) res.getResult();
			docId = doc.getId();
			break;
		case DOC_CLOSED_NOTIFICATION:
			docId = doc.getId();
			break;
		case DOC_PAGE_NOTIFICATION:
			pageArray = (V2Doc.PageArray) res.getResult();
			docId = pageArray.getDocId();
			break;
		case DOC_PAGE_ADDED_NOTIFICATION:
		case DOC_DOWNLOADED_NOTIFICATION:
		case DOC_PAGE_ACTIVITE_NOTIFICATION:
			page = (V2Doc.Page) res.getResult();
			docId = page.getDocId();
			// Record current activate Id;
			mCurrentActivateDocId = docId;
			break;
		case DOC_PAGE_CANVAS_NOTIFICATION:
			shape = (V2ShapeMeta) res.getResult();
			docId = shape.getDocId();
			break;
		default:
			V2Log.e("Unknow doc operation:" + opt);
			return;
		}
		;

		if (doc == null) {
			doc = mDocs.get(docId);
			// Put fake doc, because page events before doc event;
			if (doc == null) {
				doc = new V2Doc(docId, null, null, 0, null);
				mDocs.put(docId, doc);
			}
			// If doc is not null, means new doc event or doc close event. need
			// to update cache doc or not.
			// If doc page event before new doc event, need to update cache
			// update
		} else if (NEW_DOC_NOTIFICATION == opt) {
			V2Doc cache = mDocs.get(docId);
			if (cache != doc) {
				cache.updateDoc(doc);
			}
		}

		if (pageArray != null) {
			doc.updatePageArray(pageArray);
		}
		if (page != null) {
			doc.addPage(page);
		}

		if (opt == DOC_PAGE_CANVAS_NOTIFICATION) {
			page = doc.getPage(shape.getPageNo());
			if (page == null) {
				page = new Page(shape.getPageNo(), docId, null);
				page.addMeta(shape);
				doc.addPage(page);
			} else {
				page.addMeta(shape);
			}
		} else if (opt == DOC_PAGE_ACTIVITE_NOTIFICATION) {
			doc.setActivatePageNo(page.getNo());
		}

		// Update UI
		if (mDocContainer != null) {
			switch (opt) {
			case NEW_DOC_NOTIFICATION:
				mDocContainer.addDoc(doc);
				break;
			case DOC_CLOSED_NOTIFICATION:
				mDocContainer.closeDoc(doc);
				break;
			case DOC_PAGE_NOTIFICATION:
			case DOC_PAGE_ADDED_NOTIFICATION:
				mDocContainer.updateLayoutPageInformation();
				mDocContainer.updatePageButton();
				break;
			case DOC_PAGE_ACTIVITE_NOTIFICATION:
				doc.setActivatePageNo(page.getNo());
			case DOC_DOWNLOADED_NOTIFICATION:
				mDocContainer.updateCurrentDoc(doc);
				break;
			case DOC_PAGE_CANVAS_NOTIFICATION:
				mDocContainer.drawShape(page.getDocId(), page.getNo(), shape);
				break;
			}
			;
		}

	}

	private ServiceConnection mLocalServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName cname, IBinder binder) {
			V2Log.i(" Service bound in video meeting");
			mServiceBound = true;
			ConferencMessageSyncService cms = ((ConferencMessageSyncService.LocalBinder) binder)
					.getService();
			cb = cms.getConferenceService();
			ds = cms.getDocService();

			// register listener for conference service
			cb.registerPermissionUpdateListener(mVideoHandler,
					NOTIFY_USER_PERMISSION_UPDATED, null);
			cb.registerAttendeeListener(mVideoHandler, ATTENDEE_LISTENER, null);
			cb.registerKickedConfListener(mVideoHandler, NOTIFICATION_KICKED,
					null);
			cb.registerSyncDesktopListener(mVideoHandler,
					DESKTOP_SYNC_NOTIFICATION, null);

			cb.registerVideoMixerListener(mVideoHandler,
					VIDEO_MIX_NOTIFICATION, null);

			cb.registerAttendeeDeviceListener(mVideoHandler,
					ATTENDEE_DEVICE_LISTENER, null);

			// Register listen to document notification
			ds.registerNewDocNotification(mVideoHandler, NEW_DOC_NOTIFICATION,
					null);
			ds.registerDocDisplayNotification(mVideoHandler,
					DOC_DOWNLOADED_NOTIFICATION, null);
			ds.registerdocPageActiveNotification(mVideoHandler,
					DOC_PAGE_ACTIVITE_NOTIFICATION, null);
			ds.registerDocPageNotification(mVideoHandler,
					DOC_PAGE_NOTIFICATION, null);
			ds.registerDocClosedNotification(mVideoHandler,
					DOC_CLOSED_NOTIFICATION, null);
			ds.registerDocPageAddedNotification(mVideoHandler,
					DOC_PAGE_ADDED_NOTIFICATION, null);
			ds.registerPageCanvasUpdateNotification(mVideoHandler,
					DOC_PAGE_CANVAS_NOTIFICATION, null);

			Message.obtain(mVideoHandler, ONLY_SHOW_LOCAL_VIDEO).sendToTarget();

			suspendOrResume(true);

		}

		@Override
		public void onServiceDisconnected(ComponentName cname) {
			mServiceBound = false;
		}

	};

	private SurfaceHolder.Callback mLocalCameraHolder = new SurfaceHolder.Callback() {

		@Override
		public void surfaceChanged(SurfaceHolder holder, int arg1, int arg2,
				int arg3) {
			mLocalHolderIsCreate = true;
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			V2Log.e("Create new holder " + holder);
			mLocalHolderIsCreate = true;
			Message.obtain(mVideoHandler, ONLY_SHOW_LOCAL_VIDEO).sendToTarget();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			closeLocalCamera();
			mLocalHolderIsCreate = false;
		}

	};

	private OnTouchListener mLocalCameraDragListener = new OnTouchListener() {

		private long lastPressedTime = 0;
		int lastX;
		int lastY;

		@Override
		public boolean onTouch(View view, MotionEvent event) {
			int action = event.getAction();
			if (action == MotionEvent.ACTION_DOWN) {
				lastX = (int) event.getRawX();
				lastY = (int) event.getRawY();
			} else if (action == MotionEvent.ACTION_MOVE) {
				updateParameters(view, event);
				lastX = (int) event.getRawX();
				lastY = (int) event.getRawY();

			} else if (action == MotionEvent.ACTION_UP) {
				long currTime = System.currentTimeMillis();
				if (currTime - lastPressedTime < 400) {
					zoom(view);
					lastPressedTime = 0;
				} else {
					lastPressedTime = System.currentTimeMillis();
				}
			}
			return true;
		}

		private void zoom(View view) {
			int width = 0;
			int height = 0;
			RelativeLayout.LayoutParams vl = (RelativeLayout.LayoutParams) view
					.getLayoutParams();
			if (view.getTag() == null || view.getTag().toString().equals("out")) {
				width = vl.width / 2;
				view.setTag("in");
			} else {
				width = vl.width * 2;
				view.setTag("out");
				int[] location = new int[2];
				view.getLocationInWindow(location);

				// If zoom out
				if (location[0] < vl.width + 16) {
					vl.rightMargin -= (vl.width + 16 - location[0]);
				}
				height = width / 4 * 3;
				if (location[1] < height + 16) {
					vl.bottomMargin -= (height + 16 - location[1]);
				}

			}

			width -= width % 16;
			height = width / 4 * 3;
			height -= height % 16;

			vl.width = width;
			vl.height = height;

			view.setLayoutParams(vl);

		}

		private void updateParameters(View view, MotionEvent event) {
			RelativeLayout.LayoutParams rl = (RelativeLayout.LayoutParams) view
					.getLayoutParams();
			Rect r = new Rect();
			mRootContainer.getDrawingRect(r);

			int[] pos = new int[2];
			mRootContainer.getLocationInWindow(pos);

			rl.bottomMargin -= (event.getRawY() - lastY);
			rl.rightMargin -= (event.getRawX() - lastX);
			if (rl.bottomMargin < 0) {
				rl.bottomMargin = 0;
			}
			if (rl.rightMargin < 0) {
				rl.rightMargin = 0;
			}
			if ((r.right - r.left - view.getWidth()) < rl.rightMargin) {
				rl.rightMargin = r.right - r.left - view.getWidth();
			}

			if ((r.bottom - r.top) - (rl.bottomMargin + view.getHeight()) <= 5) {
				rl.bottomMargin = r.bottom - r.top - view.getHeight() - 5;
			}

			((ViewGroup) view.getParent()).updateViewLayout(view, rl);
			// make sure draging view is first front of all
			view.bringToFront();
		}
	};

	class SubViewListener implements VideoDocLayout.DocListener,
			VideoAttendeeListLayout.VideoAttendeeActionListener,
			VideoMsgChattingLayout.ChattingListener,
			VideoInvitionAttendeeLayout.Listener {

		@Override
		public void requestSendMsg(VMessage vm) {
			// vm.setGroupId(conf.getId());
			// vm.setToUser(new User(0));
			// vm.setFromUser(GlobalHolder.getInstance().getCurrentUser());
			// vm.setMsgCode(V2GlobalEnum.GROUP_TYPE_CONFERENCE);
			cs.sendVMessage(vm, null);

		}

		@Override
		public void requestChattingViewFixedLayout(View v) {
			requestSubViewFixed();
			adjustContentLayout();
		}

		@Override
		public void requestChattingViewFloatLayout(View v) {
			requestSubViewFloat();
			adjustContentLayout();
		}

		@Override
		public void OnAttendeeDragged(Attendee at, UserDeviceConfig udc, int x,
				int y) {
			if (at == null || udc == null) {
				return;
			}
			if (at.getAttId() == GlobalHolder.getInstance().getCurrentUserId()
					|| !at.isJoined()) {
				return;
			}

			for (SurfaceViewW sw : mCurrentShowedSV) {
				if (sw.udc.getDeviceID().equals(udc.getDeviceID())) {
					return;
				}
			}

			for (SurfaceViewW sw : mCurrentShowedSV) {
				int[] lo = new int[2];
				Rect r = new Rect();
				sw.getView().getDrawingRect(r);
				sw.getView().getLocationOnScreen(lo);
				r.left = lo[0];
				r.right = lo[0] + r.right;
				r.top = lo[1];
				r.bottom = lo[1] + r.bottom;
				if (r.contains(x, y)) {
					boolean flag = showOrCloseAttendeeVideo(sw.udc);
					// update opened video view background
					mAttendeeContainer.updateCurrentSelectedBg(flag, sw.at,
							sw.udc);
					sw.at = at;
					sw.udc = udc;
					// update new opened video view background
					flag = showOrCloseAttendeeVideo(udc);
					mAttendeeContainer.updateCurrentSelectedBg(flag, at, udc);

					return;
				}
			}

			OnAttendeeClicked(at, udc);
			if (udc.isShowing()) {
				mAttendeeContainer.updateCurrentSelectedBg(true, at, udc);
			}
		}

		@Override
		public void OnAttendeeClicked(Attendee at, UserDeviceConfig udc) {
			if (udc == null || at == null) {
				return;
			}
			if (at.getAttId() == GlobalHolder.getInstance().getCurrentUserId()
					|| !at.isJoined()) {

			} else {
				showOrCloseAttendeeVideo(udc);
			}
		}

		@Override
		public void requestAttendeeViewFixedLayout(View v) {
			requestSubViewFixed();
			adjustContentLayout();
		}

		@Override
		public void requestAttendeeViewFloatLayout(View v) {
			requestSubViewFloat();
			adjustContentLayout();
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
			requestSubViewFixed();
			adjustContentLayout();

		}

		@Override
		public void requestDocViewFloatLayout(View v) {
			// If current doc layout size is full screen, then ignore
			// reques fixed size reqeust.
			if (mDocContainer.isFullScreenSize()) {
				return;
			}
			requestSubViewFloat();
			adjustContentLayout();
		}

		@Override
		public void requestDocViewFillParent(View v) {
			requestSubViewFillScreen();
			adjustContentLayout();
		}

		@Override
		public void requestDocViewRestore(View v) {
			requestSubViewRestore();
			// Request fixed size for doc layout
			requestSubViewFixed();
			adjustContentLayout();

		}

		@Override
		public void requestInvitation(Conference conf, List<User> l) {
			if (l == null || l.size() <= 0) {
				Toast.makeText(mContext, R.string.warning_no_attendee_selected,
						Toast.LENGTH_SHORT).show();
				return;
			}
			// ignore call back;
			cb.inviteAttendee(conf, l, null);
			// Hide invitation layout
			showOrHideSubWindow(initInvitionContainer());
		}

	}

	class SurfaceViewW {

		Attendee at;
		UserDeviceConfig udc;
		int layId;
		RelativeLayout rl;
		SurfaceHolderObserver observer;

		public SurfaceViewW() {

		}

		public SurfaceViewW(Attendee at, UserDeviceConfig udc,
				SurfaceHolderObserver observer) {
			this.at = at;
			this.udc = udc;
			this.observer = observer;
			this.udc.getSVHolder().getHolder().addCallback(observer);
		}

		public View getView() {
			if (rl == null) {
				rl = new RelativeLayout(mContext);
				rl.setPadding(1, 1, 1, 1);
				rl.setBackgroundColor(Color.rgb(143, 144, 144));
				layId = (int) udc.hashCode();

				if (udc.getSVHolder() == null) {
					udc.setSVHolder(new SurfaceView(mContext));
				}
				rl.addView(udc.getSVHolder(), new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT,
						RelativeLayout.LayoutParams.MATCH_PARENT));
				TextView tv = new TextView(mContext);
				tv.setText(at.getAttName());
				int widthDP = DensityUtils.dip2px(mContext, 80);
				tv.setMaxWidth(widthDP);
				tv.setEllipsize(TruncateAt.END);
				tv.setSingleLine();
				tv.setBackgroundColor(Color.rgb(138, 138, 138));
				tv.setPadding(10, 10, 10, 10);
				tv.setTextSize(14);
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

		@Override
		public synchronized void handleMessage(Message msg) {

			switch (msg.what) {
			case ONLY_SHOW_LOCAL_VIDEO:
				// Make sure open local camera after service bound and holder
				// created
				if (mLocalHolderIsCreate && mServiceBound) {
					showLocalSurViewOnly();
				}
				break;
			case REQUEST_OPEN_DEVICE_RESPONSE:
			case REQUEST_CLOSE_DEVICE_RESPONSE:
				break;
			case CONF_USER_DEVICE_EVENT:
				// recordUserDevice((ConfUserDeviceInfo) msg.obj);
				break;
			case USER_DELETE_GROUP: {
				GroupUserObject ro = (GroupUserObject) msg.obj;
				Attendee a = new Attendee(GlobalHolder.getInstance().getUser(
						ro.getmUserId()));
				mAttendeeList.remove(a);
				if (mAttendeeContainer != null) {
					mAttendeeContainer.removeAttendee(a);
				}
			}
				break;
			case GROUP_ADD_USER:
				GroupUserObject ro1 = (GroupUserObject) msg.obj;
				if (ro1.getmType() == V2GlobalEnum.GROUP_TYPE_CONFERENCE) { // CONFGROUP
					Attendee a1 = new Attendee(GlobalHolder.getInstance()
							.getUser(ro1.getmUserId()));
					mAttendeeList.add(a1);
					if (mAttendeeContainer != null) {
						mAttendeeContainer.addNewAttendee(a1);
					}
				}
				break;
			case ATTENDEE_DEVICE_LISTENER: {
				// need to update user device when remote user disable device
				Object[] obj = (Object[]) (((AsyncResult) msg.obj).getResult());
				List<UserDeviceConfig> list = (List<UserDeviceConfig>) obj[1];
				Long uid = (Long) obj[0];
				Attendee at = findAttendee(uid);
				if (at != null) {
					if (at.getmDevices() == null) {
						at.setmDevices(list);
					}
					updateAttendeeDevice(at, list);
				}
			}
				break;
			case ATTENDEE_LISTENER:
				User ut = (User) (((AsyncResult) msg.obj).getResult());
				Attendee at = findAttendee(ut.getmUserId());
				if (msg.arg1 == 1) {
					// for non-register user construct temp attendee
					if (at == null) {
						at = new Attendee(ut);
						mAttendeeList.add(at);
					}
					V2Log.d(TAG, "Successful receiver the 参会人加入的回调");
					doHandleNewUserEntered(at);
				} else {
					V2Log.d(TAG, "Successful receiver the 参会人退出的回调");
					doHandleUserExited(at);
				}
				break;
			case REQUEST_OPEN_OR_CLOSE_DEVICE:
				if (msg.arg1 == TAG_CLOSE_DEVICE) {
					cb.requestCloseVideoDevice(cg, (UserDeviceConfig) msg.obj,
							new MessageListener(mVideoHandler,
									REQUEST_CLOSE_DEVICE_RESPONSE, null));
				} else if (msg.arg1 == TAG_OPEN_DEVICE) {
					cb.requestOpenVideoDevice(cg, (UserDeviceConfig) msg.obj,
							new MessageListener(mVideoHandler,
									REQUEST_OPEN_DEVICE_RESPONSE, null));
				}
				break;
			case NOTIFICATION_KICKED: {
				int r = msg.arg1;
				int resource = R.string.conversations_kick_notification;
				if (r == V2GlobalEnum.CONF_CODE_DELETED) {
					resource = R.string.confs_is_deleted_notification;
				}
				Toast.makeText(mContext, resource, Toast.LENGTH_LONG).show();
				// Do quit action
				quit();
				finish();
			}
				break;

			// user permission updated
			case NOTIFY_USER_PERMISSION_UPDATED:
				PermissionUpdateIndication ind = (PermissionUpdateIndication) (((AsyncResult) msg.obj)
						.getResult());
				if (!updateAttendeePermissionStateIcon(ind)) {
					mPendingPermissionUpdateList.add(ind);
				}
				if (ind.getUid() == GlobalHolder.getInstance()
						.getCurrentUserId()) {

					updateSpeakerState(PermissionState.fromInt(ind.getState()) == PermissionState.GRANTED
							&& ConferencePermission.SPEAKING.intValue() == ind
									.getType());
				}
				break;
			case NEW_DOC_NOTIFICATION:
			case DOC_PAGE_NOTIFICATION:
			case DOC_PAGE_ADDED_NOTIFICATION:
			case DOC_PAGE_ACTIVITE_NOTIFICATION:
			case DOC_DOWNLOADED_NOTIFICATION:
			case DOC_CLOSED_NOTIFICATION:
			case DOC_PAGE_CANVAS_NOTIFICATION:
				updateDocNotification((AsyncResult) (msg.obj), msg.what);
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
				V2Log.e(TAG,
						"successful receive mix video callback. type is : "
								+ msg.arg1);
				if (msg.arg1 == 1) {
					MixVideo mv = (MixVideo) (((AsyncResult) msg.obj)
							.getResult());
					AttendeeMixedDevice amd = new AttendeeMixedDevice(mv);
					mAttendeeList.add(amd);

					// Notify attendee list mixed video is created
					if (mAttendeeContainer != null) {
						V2Log.e(TAG,
								"VIDEO_MIX_NOTIFICATION 被调用 , 成功添加混合视频  update---");
						mAttendeeContainer.updateEnteredAttendee(amd);
					}

					// destroy mixed video
				} else if (msg.arg1 == 2) {
					// MixVideo mv = (MixVideo) msg.obj;
					MixVideo mv = (MixVideo) (((AsyncResult) msg.obj)
							.getResult());
					AttendeeMixedDevice amd = new AttendeeMixedDevice(mv);
					// Remove from attendee list, because chairman closed mixed
					// video device
					mAttendeeList.remove(amd);
					// Notify attendee list remove mixed video device
					if (mAttendeeContainer != null) {
						V2Log.e(TAG,
								"VIDEO_MIX_NOTIFICATION 被调用 , 成功移除混合视频  update---");
						mAttendeeContainer.updateExitedAttendee(amd);
					}
					UserDeviceConfig mixedUDC = amd.getDefaultDevice();
					// Close opened mixed video
					for (SurfaceViewW sw : mCurrentShowedSV) {
						if (sw.udc.getDeviceID().equals(mixedUDC.getDeviceID())) {
							sw.observer.close();
							mCurrentShowedSV.remove(sw);
							mVideoLayout.removeView(sw.getView());
							sw.rl.removeAllViews();
							adjustVideoLayout();
							break;
						}
					}

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

	// @Override
	// public boolean dispatchTouchEvent(MotionEvent ev) {
	// View v = getCurrentFocus();
	// if(v == null){
	// return super.dispatchTouchEvent(ev);
	// }
	//
	// InputMethodManager imm = (InputMethodManager)
	// getSystemService(Context.INPUT_METHOD_SERVICE);
	// if (imm != null && v != null) {
	// imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	// }
	// V2techSearchContentProvider.closedDataBase();
	// return super.dispatchTouchEvent(ev);
	// }

}
