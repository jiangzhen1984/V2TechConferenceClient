package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import v2av.CaptureCapability;
import v2av.VideoCaptureDevInfo;
import v2av.VideoPlayer;
import v2av.VideoRecorder;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils.TruncateAt;
import android.util.DisplayMetrics;
import android.view.Gravity;
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
import com.v2tech.R;
import com.v2tech.service.AsyncResult;
import com.v2tech.service.ChatService;
import com.v2tech.service.ConferencMessageSyncService;
import com.v2tech.service.ConferenceService;
import com.v2tech.service.DocumentService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.jni.PermissionUpdateIndication;
import com.v2tech.util.V2Log;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.bo.GroupUserObject;
import com.v2tech.view.bo.UserStatusObject;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.view.widget.MixVideoLayout;
import com.v2tech.vo.Attendee;
import com.v2tech.vo.AttendeeMixedDevice;
import com.v2tech.vo.CameraConfiguration;
import com.v2tech.vo.Conference;
import com.v2tech.vo.ConferenceGroup;
import com.v2tech.vo.ConferencePermission;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
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

	public static final String JNI_EVENT_VIDEO_CATEGORY = "com.v2tech.conf_video_event";
	public static final String JNI_EVENT_VIDEO_CATEGORY_OPEN_VIDEO_EVENT_ACTION = "com.v2tech.conf_video_event.open_video_event";

	private boolean isSpeaking;

	private Handler mVideoHandler = new VideoHandler();

	private Context mContext;
	private List<SurfaceViewW> mCurrentShowedSV;

	private RelativeLayout mRootContainer;

	private FrameLayout mContentLayoutMain;
	private RelativeLayout mVideoLayout;
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

	private ChatService cs = new ChatService();

	private Set<Attendee> mAttendeeList = new HashSet<Attendee>();
	private DocumentService ds;

	private Map<String, V2Doc> mDocs = new HashMap<String, V2Doc>();
	private V2Doc mCurrentActivateDoc = null;

	private Map<String, MixerWrapper> mMixerWrapper = new HashMap<String, MixerWrapper>();

	private SubViewListener subViewListener = new SubViewListener();

	private List<VMessage> mPendingMessageList;

	private List<PermissionUpdateIndication> mPendingPermissionUpdateList;

	private Toast mToast;

	private DisplayMetrics dm;

	private boolean mServiceBound = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(R.layout.activity_in_metting);
		mContext = this;
		dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		this.mRootContainer = (RelativeLayout) findViewById(R.id.video_layout_root);
		this.mContentLayoutMain = (FrameLayout) findViewById(R.id.in_meeting_content_main);
		this.mCurrentShowedSV = new ArrayList<SurfaceViewW>();

		this.mVideoLayout = new RelativeLayout(this);
		mContentLayoutMain.addView(this.mVideoLayout);
		this.mSubWindowLayout = new FrameLayout(this);
		mSubWindowLayout.setVisibility(View.GONE);
		mContentLayoutMain.addView(this.mSubWindowLayout);

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

		// local camera surface view
		this.mLocalSurface = (SurfaceView) findViewById(R.id.local_surface_view);

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

	}

	@Override
	protected void onStart() {
		super.onStart();

		if (mServiceBound) {
			suspendOrResume(true);
		}

		//Set audio use speaker phone
		updateAudioSpeaker(true);
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

	}

	private void init() {
		conf = (Conference) this.getIntent().getExtras().get("conf");

		cg = (ConferenceGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.CONFERENCE, conf.getId());
		if (cg == null) {
			V2Log.e(" doesn't receive group information  yet");
			return;
		}
		mGroupNameTV.setText(cg.getName());

		Group confGroup = GlobalHolder.getInstance()
				.findGroupById(conf.getId());
		// load conference attendee list
		if (confGroup != null) {
			List<User> l = new ArrayList<User>(confGroup.getUsers());
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
				.getCurrentUserId() || conf.getChairman() == GlobalHolder.getInstance()
						.getCurrentUserId() )? true : false;
	}

	/**
	 * Update speaker icon and state
	 * 
	 * @param flag
	 */
	private void updateSpeakerState(boolean flag) {
		isSpeaking = flag;
		// set flag to speaking icon
		if (flag) {
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
			synchronized (mMixerWrapper) {
				for (MixerWrapper mw : mMixerWrapper.values()) {
					mAttendeeContainer.updateEnteredAttendee(mw.amd);
				}
			}

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
				mAttendeeContainer.updateAttendeeSpeakingState(pa,
						ConferencePermission.SPEAKING, PermissionState.GRANTED);
			}
			// Update pending attendee state
			for (PermissionUpdateIndication ind : mPendingPermissionUpdateList) {
				updateAttendeePermissionStateIcon(ind);
			}
			mPendingPermissionUpdateList.clear();

		}

		return mAttendeeContainer;
	}

	private View initDocLayout() {

		if (mDocContainer == null) {
			mDocContainer = new VideoDocLayout(this);
			mDocContainer.setListener(subViewListener);
			synchronized (mDocs) {
				for (Entry<String, V2Doc> e : mDocs.entrySet()) {
					mDocContainer.addDoc(e.getValue());
				}
			}
			mDocContainer.updateCurrentDoc(mCurrentActivateDoc);
			Group g = GlobalHolder.getInstance().findGroupById(conf.getId());
			if (g != null && g instanceof ConferenceGroup) {
				mDocContainer.updateSyncStatus(((ConferenceGroup) g).isSyn());
			}

		}

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
		int width = 0, height = 0;
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
			if (mContentLayoutMain.getMeasuredWidth() == 0) {
				mContentLayoutMain.measure(View.MeasureSpec.UNSPECIFIED,
						View.MeasureSpec.UNSPECIFIED);
			}
			width = mContentLayoutMain.getMeasuredWidth();
			height = mContentLayoutMain.getMeasuredHeight();

			int flag = getSubViewWindowState();
			// If sub window request full screen
			if ((flag & TAG_SUB_WINDOW_STATE_FULL_SCRREN) == TAG_SUB_WINDOW_STATE_FULL_SCRREN) {
				width = (width - marginLeft);
			} else {
				width = (width - marginLeft) / 2;
			}

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

		FrameLayout.LayoutParams fl = (FrameLayout.LayoutParams) mVideoLayout
				.getLayoutParams();
		width = mContentLayoutMain.getMeasuredWidth() - marginLeft;
		height = mContentLayoutMain.getMeasuredHeight();
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
			this.adjustLayout();
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
								new UserDeviceConfig(GlobalHolder.getInstance()
										.getCurrentUserId(), "", null))
								.sendToTarget();
						//

						VideoCaptureDevInfo.CreateVideoCaptureDevInfo()
								.SetCapParams(width, height, bitrate, fps,
										ImageFormat.NV21);

						mVideoHandler.postDelayed(new Runnable() {

							@Override
							public void run() {
								showOrCloseLocalSurViewOnly();
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
				VMessage vm = MessageLoader.loadMessageById(mContext, mid);
				if (mMessageContainer != null) {
					mMessageContainer.addNewMessage(vm);
				} else {
					mPendingMessageList.add(vm);
				}
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {
				long confID = intent.getLongExtra("gid", 0);
				if (confID == conf.getId()) {
					Group confGroup = GlobalHolder.getInstance().findGroupById(
							conf.getId());
					// load conference attendee list
					if (confGroup != null) {
						List<User> l = new ArrayList<User>(confGroup.getUsers());
						for (User u : l) {
							Attendee at =new Attendee(u);
							boolean bt = mAttendeeList.add(at);
							if (bt && mAttendeeContainer != null) {
								mAttendeeContainer.addNewAttendee(at);
							}
						}
						if (mAttendeeContainer != null) {
							synchronized (mMixerWrapper) {
								for (MixerWrapper mw : mMixerWrapper.values()) {
									mAttendeeContainer
											.updateEnteredAttendee(mw.amd);
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
				// TODO show dialog
				NetworkStateCode code = (NetworkStateCode) intent.getExtras()
						.get("state");
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
				quit();
				finish();
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

		int normalW = containerW / cols, normalH = normalW / 4 * 3;
		if (normalH * rows > containerH) {
			normalH = containerH / rows;
			normalW = normalH / 3 * 4;
		}

		int fixedWidth = normalW;
		int fixedHeight = normalH;
		fixedWidth -= fixedWidth % 16;
		fixedHeight -= fixedHeight % 16;

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

		// make sure local camera is first of all
		localSurfaceViewLy.bringToFront();
	}

	@Override
	protected void onStop() {
		super.onStop();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (mServiceBound) {
			suspendOrResume(false);
		}
		updateAudioSpeaker(false);
	}

	@Override
	public void finish() {
		super.finish();
		// this.overridePendingTransition(R.animator.nonam_scale_null,
		// R.animator.nonam_scale_center_100_0);
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

		mContext.stopService(new Intent(mContext,
				ConferencMessageSyncService.class));
		// clear current meeting state
		GlobalHolder.getInstance().setMeetingState(false);
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
	
	
	

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	
	private void updateAudioSpeaker(boolean flag) {
		AudioManager audioManager;
		audioManager = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);
		audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
		audioManager.setSpeakerphoneOn(flag);
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
					Intent i = new Intent();
					i.putExtra("gid", conf.getId());
					setResult(0, i);
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
			for (SurfaceViewW sw : this.mCurrentShowedSV) {
				Message.obtain(mVideoHandler, REQUEST_OPEN_OR_CLOSE_DEVICE, 1,
						0, sw.udc).sendToTarget();
			}
			// Make sure local camera is first front of all
			Message.obtain(mVideoHandler, ONLY_SHOW_LOCAL_VIDEO).sendToTarget();
			adjustLayout();

			// Send speaking status
			doApplyOrReleaseSpeak(isSpeaking);
			// Make sure update start after send request,
			// because update state will update isSpeaking value
			updateSpeakerState(isSpeaking);

		} else {
			for (SurfaceViewW sw : this.mCurrentShowedSV) {
				Message.obtain(mVideoHandler, REQUEST_OPEN_OR_CLOSE_DEVICE, 0,
						0, sw.udc).sendToTarget();
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
			mVideoLayout.removeAllViews();
		}
	}

	/**
	 * user quit conference, however positive or negative
	 */
	private void quit() {
		// if bound, then conference service is initialized. Otherwise not.
		if (mServiceBound) {
			for (SurfaceViewW sw : this.mCurrentShowedSV) {
				Message.obtain(mVideoHandler, REQUEST_OPEN_OR_CLOSE_DEVICE, 0,
						0, sw.udc).sendToTarget();
				sw.udc.doClose();
			}

			// close local camera
			Message.obtain(
					mVideoHandler,
					REQUEST_OPEN_OR_CLOSE_DEVICE,
					0,
					0,
					new UserDeviceConfig(GlobalHolder.getInstance()
							.getCurrentUserId(), "", null)).sendToTarget();
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
		a.setJoined(true);
		if (conf.getChairman() == a.getAttId()) {
			a.setChairMan(true);
		}

		List<UserDeviceConfig> ld = GlobalHolder.getInstance()
				.getAttendeeDevice(a.getAttId());
		if (ld == null || ld.size() <= 0) {
			V2Log.w(" No available device config for user:" + user.getmUserId()
					+ "  name:" + user.getName());
		} else {
			for (UserDeviceConfig udc : ld) {
				a.addDevice(udc);
				V2Log.i("New attendee joined conference :" + a.getAttName()
						+ "   " + udc.getDeviceID());
			}
		}

		if (mAttendeeContainer != null) {
			mAttendeeContainer.updateEnteredAttendee(a);
		}

		showToastNotification(user.getName()
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
	private void doHandleUserExited(User user) {
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
			if (conf.getChairman() == a.getAttId()) {
				a.setChairMan(true);
			}
			a.setJoined(false);
			if (mAttendeeContainer != null) {
				mAttendeeContainer.updateExitedAttendee(a);
			}
		}

		// Clean user device
		GlobalHolder.getInstance().removeAttendeeDeviceCache(user.getmUserId());
		showToastNotification(user.getName()
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
			vp.SetSurface(udc.getSVHolder().getHolder());
			udc.setVp(vp);

			//
			if (udc.getBelongsAttendee() instanceof AttendeeMixedDevice) {
				mCurrentShowedSV.add(new MixedSurfaceViewW(
						((AttendeeMixedDevice) udc.getBelongsAttendee()), udc));
			} else {
				mCurrentShowedSV.add(new SurfaceViewW(udc.getBelongsAttendee(),
						udc));

			}
			// Do adjust layout first, then request open device.
			// otherwise can't show video
			adjustLayout();

			Message m = Message.obtain(mVideoHandler,
					REQUEST_OPEN_OR_CLOSE_DEVICE, 1, 0, udc);
			mVideoHandler.sendMessageDelayed(m, 300);
			udc.setShowing(true);
			return true;
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
			mAttendeeContainer.updateAttendeeSpeakingState(pa,
					ConferencePermission.fromInt(ind.getType()),
					PermissionState.fromInt(ind.getState()));
			return true;
		} else {
			return false;
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

			suspendOrResume(true);
		}

		@Override
		public void onServiceDisconnected(ComponentName cname) {
			mServiceBound = false;
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
			vm.setGroupId(conf.getId());
			vm.setToUser(new User(0));
			vm.setFromUser(GlobalHolder.getInstance().getCurrentUser());
			vm.setMsgCode(V2GlobalEnum.REQUEST_TYPE_CONF);
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
					// FIXME set id
					sw.getView().setId(udc.hashCode());
					// update new opened video view background
					flag = showOrCloseAttendeeVideo(udc);
					mAttendeeContainer.updateCurrentSelectedBg(flag, at, udc);

					return;
				}
			}

			OnAttendeeClicked(at, udc);
		}

		@Override
		public void OnAttendeeClicked(Attendee at, UserDeviceConfig udc) {
			if (udc == null || at == null) {
				return;
			}
			if (at.getAttId() == GlobalHolder.getInstance().getCurrentUserId()
					|| !at.isJoined()) {

			} else {
				boolean flag = showOrCloseAttendeeVideo(udc);
				mAttendeeContainer.updateCurrentSelectedBg(flag, at, udc);

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

	class MixerWrapper {
		String id;
		MixVideo mix;
		MixVideoLayout layout;
		AttendeeMixedDevice amd;

		public MixerWrapper(String id, MixVideo mix, MixVideoLayout layout,
				AttendeeMixedDevice amd) {
			super();
			this.id = id;
			this.mix = mix;
			this.layout = layout;
			this.amd = amd;
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
				tv.setMaxWidth(80);
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

	class MixedSurfaceViewW extends SurfaceViewW {

		AttendeeMixedDevice at;

		public MixedSurfaceViewW(AttendeeMixedDevice at) {
			this.at = at;
			this.udc = new UserDeviceConfig(0, at.getMV().getId(), null);
		}

		public MixedSurfaceViewW(AttendeeMixedDevice at, UserDeviceConfig udc) {
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
				tv.setText(at.getAttName());
				tv.setMaxWidth(80);
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

		Map<String, V2Doc.PageArray> prLegacy = new HashMap<String, V2Doc.PageArray>();

		@Override
		public synchronized void handleMessage(Message msg) {

			switch (msg.what) {
			case ONLY_SHOW_LOCAL_VIDEO:
				// make sure local view is first front of all;
				showOrCloseLocalSurViewOnly();
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
				Attendee a1 = new Attendee(GlobalHolder.getInstance().getUser(
						ro1.getmUserId()));
				mAttendeeList.add(a1);
				if (mAttendeeContainer != null) {
					mAttendeeContainer.addAttendee(a1);
				}
				break;
			case ATTENDEE_DEVICE_LISTENER: {
				List<UserDeviceConfig> list = (List<UserDeviceConfig>) (((AsyncResult) msg.obj)
						.getResult());
				for (UserDeviceConfig ud : list) {
					Attendee a = findAttendee(ud.getUserID());
					if (a == null) {
						continue;
					}
					a.addDevice(ud);
					// Update attendee device
					if (mAttendeeContainer != null) {
						mAttendeeContainer.updateEnteredAttendee(a);
					}
				}

			}
				break;
			case ATTENDEE_LISTENER:
				if (msg.arg1 == 1) {
					doHandleNewUserEntered((User) (((AsyncResult) msg.obj)
							.getResult()));
				} else {
					doHandleUserExited((User) (((AsyncResult) msg.obj)
							.getResult()));
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
				V2Doc vd = (V2Doc) ((AsyncResult) (msg.obj)).getResult();
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
				V2Doc.PageArray vpr = (V2Doc.PageArray) ((AsyncResult) (msg.obj))
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
				V2Doc.Page vpp = (V2Doc.Page) ((AsyncResult) (msg.obj))
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
				V2Doc.Page vpa = (V2Doc.Page) ((AsyncResult) (msg.obj))
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
				V2Doc.Page vp = (V2Doc.Page) ((AsyncResult) (msg.obj))
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
				V2Doc removedDoc = (V2Doc) ((AsyncResult) (msg.obj))
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
				V2ShapeMeta shape = (V2ShapeMeta) ((AsyncResult) (msg.obj))
						.getResult();
				synchronized (mDocs) {
					V2Doc ca = mDocs.get(shape.getDocId());
					//FIXME handle ca is null
					if (ca == null) {
						V2Log.e(" ERROR "+ shape.getDocId());
						break;
					}
					V2Doc.Page caVp = ca.findPage(shape.getPageNo());
					if (caVp != null) {
						caVp.addMeta(shape);
					} else {
						V2Log.i(" construct new page for canvas"
								+ shape.getPageNo());
						V2Doc.Page newPage = new V2Doc.Page(shape.getPageNo(), shape.getDocId(), null, null);
						newPage.addMeta(shape);
						caVp = newPage;
						ca.addPage(newPage);
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
					MixVideo mv = (MixVideo) (((AsyncResult) msg.obj)
							.getResult());

					MixerWrapper mw = new MixerWrapper(mv.getId(), mv,
							new MixVideoLayout(mContext, mv),
							new AttendeeMixedDevice(mv));
					synchronized (mMixerWrapper) {
						// If exist, do not add again
						if (mMixerWrapper.containsKey(mv.getId())) {
							break;
						}
						mMixerWrapper.put(mv.getId(), mw);
					}
					// Notify attendee list mixed video is created
					if (mAttendeeContainer != null) {
						mAttendeeContainer.updateEnteredAttendee(mw.amd);
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
					// Close opened mixed video
					if (mw != null && mw.amd.getDefaultDevice().isShowing()) {
						showOrCloseAttendeeVideo(mw.amd.getDefaultDevice());
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

}
