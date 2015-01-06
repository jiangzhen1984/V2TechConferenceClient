package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.DisplayMetrics;
import android.util.Log;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.ImRequest;
import com.V2.jni.V2GlobalEnum;
import com.V2.jni.util.V2Log;
import com.V2.jni.util.XmlAttributeExtractor;
import com.v2tech.R;
import com.v2tech.db.provider.VerificationProvider;
import com.v2tech.service.AsyncResult;
import com.v2tech.service.ChatService;
import com.v2tech.service.ConferencMessageSyncService;
import com.v2tech.service.ConferenceService;
import com.v2tech.service.DocumentService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.service.jni.PermissionRequestIndication;
import com.v2tech.service.jni.PermissionUpdateIndication;
import com.v2tech.util.DensityUtils;
import com.v2tech.view.ConversationsTabFragment;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.bo.GroupUserObject;
import com.v2tech.view.bo.UserStatusObject;
import com.v2tech.view.conversation.ConversationSelectImage;
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

public class ConferenceActivity extends Activity {
	private static final String TAG = "ConferenceActivity";
	// 左列表的显示模式标志
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
	private static final int NOTIFY_HOST_PERMISSION_REQUESTED = 12;

	private static final int ATTENDEE_DEVICE_LISTENER = 20;
	private static final int ATTENDEE_ENTER_OR_EXIT_LISTNER = 21;
	private static final int CONF_USER_DEVICE_EVENT = 23;
	private static final int USER_DELETE_GROUP = 24;
	private static final int GROUP_ADD_USER = 25;

	private static final int NEW_DOC_NOTIFICATION = 50;
	private static final int DOC_PAGE_LIST_NOTIFICATION = 51;
	private static final int DOC_TURN_PAGE_NOTIFICATION = 52;
	private static final int DOC_ADDED_ONE_PAGE_NOTIFICATION = 53;
	private static final int DOC_DOWNLOADE_COMPLETE_ONE_PAGE_NOTIFICATION = 54;
	private static final int DOC_CLOSED_NOTIFICATION = 55;

	private static final int DOC_PAGE_CANVAS_NOTIFICATION = 56;

	private static final int SYNC_STATE_NOTIFICATION = 57;
	private static final int VOICEACTIVATION_NOTIFICATION = 58;
	private static final int INVITATION_STATE_NOTIFICATION = 59;

	private static final int VIDEO_MIX_NOTIFICATION = 70;
	private static final int TAG_CLOSE_DEVICE = 0;
	private static final int TAG_OPEN_DEVICE = 1;

	private static final int FLAG_IS_SYNCING = 1;
	private static final int FLAG_NO_SYNC = 0;

	private static final int SUB_ACTIVITY_CODE_SHARE_DOC = 100;

	// private PermissionState mCurrentAttendeeLectureState =
	// PermissionState.NORMAL;

	private boolean isMuteCamera = false;

	private Handler mVideoHandler = new VideoHandler();

	private Context mContext;
	private List<SurfaceViewW> mCurrentShowedSV;

	private RelativeLayout mRootContainer;

	private FrameLayout mContentLayoutMain;
	private FrameLayout mVideoLayout;
	private FrameLayout mSubWindowLayout;

	private TextView mGroupNameTV;
	private ImageView mChairmanControl;
	private View mFeatureIV;
	private View mMsgNotification;
	private View mConfMsgRedDot;
	private ImageView mSpeakerIV;
	private ImageView mCameraIV;
	private PopupWindow mSettingWindow;
	private PopupWindow mChairControlWindow;
	private PopupWindow moreWindow;
	private TextView mRequestButtonName;
	private ImageView mRequestButtonImage;
	private VideoShowHostRequest mHostRequestWindow;

	private Dialog mQuitDialog;
	private Dialog mMuteCameraDialog;
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

	private ConferenceGroup cg;
	private Conference conf;

	private ConferenceService cb;
	private DocumentService ds;
	private ChatService cs = new ChatService();

	private Set<Attendee> mAttendeeList = new HashSet<Attendee>();
	private List<Attendee> mFastAttendeeList = new ArrayList<Attendee>();

	private Map<String, V2Doc> mDocs = new HashMap<String, V2Doc>();
	private String mCurrentLecturerActivateDocId = null;
	private V2Doc.Page mCurrentLecturerActivateDocPage = null;

	private SubViewListener subViewListener = new SubViewListener();

	private List<VMessage> mPendingMessageList;

	private List<PermissionUpdateIndication> mPendingPermissionUpdateList;

	private Set<User> mHostRequestUsers;

	private Toast mToast;

	private DisplayMetrics dm;

	private boolean mServiceBound = false;
	private boolean mLocalHolderIsCreate = false;
	private boolean isMoveTaskBack = true;

	private int mVideoMaxCols = 2;

	private int mContentWidth = -1;
	private int mContentHeight = -1;

	private AudioManager audioManager;
	boolean isBluetoothHeadsetConnected = false;
	private BluetoothAdapter blueadapter = BluetoothAdapter.getDefaultAdapter();

	private int arrowWidth = 0;

	private boolean isSyn = false;
	private boolean isVoiceActivation = false;
	private boolean canInvitation = false;

	private boolean hasUnreadChiremanControllMsg = false;
	// private boolean initConferenceDateFaile = false;

	// private boolean isSpeaking;

	private Attendee currentAttendee;

	private BroadcastReceiver mConfUserChangeReceiver = new ConfBroadcastReceiver();

	private boolean isFinish;

	private boolean currentAttendeeTurnedpage = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_in_metting);
		if (!initConferenceDate()) {

			isFinish = true;
			Intent i = new Intent();
			i.putExtra("gid", conf.getId());
			setResult(ConversationsTabFragment.CONFERENCE_ENTER_CODE, i);
			Toast.makeText(getApplicationContext(),
					R.string.confs_is_deleted_notification, Toast.LENGTH_LONG)
					.show();
			super.finish();
			return;
		}

		if (blueadapter != null
				&& BluetoothProfile.STATE_CONNECTED == blueadapter
						.getProfileConnectionState(BluetoothProfile.HEADSET)) {
			isBluetoothHeadsetConnected = true;
			Log.i(TAG, "蓝牙是连接的");
		}

		mContext = this;
		dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		this.mRootContainer = (RelativeLayout) findViewById(R.id.video_layout_root);
		this.mContentLayoutMain = (FrameLayout) findViewById(R.id.in_meeting_content_main);
		// 用于隐藏输入法
		mContentLayoutMain.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				hindSoftInput(v);
				return false;
			}
		});
		this.mCurrentShowedSV = new ArrayList<SurfaceViewW>();

		this.mSubWindowLayout = new FrameLayout(this);

		mSubWindowLayout.setVisibility(View.GONE);
		mContentLayoutMain.addView(this.mSubWindowLayout);

		this.mChairmanControl = (ImageView) findViewById(R.id.iv_chairman_control);
		this.mChairmanControl.setOnClickListener(onClickMChairmanControl);

		mMsgNotification = findViewById(R.id.host_request_msg_notificator);
		mMsgNotification.setVisibility(View.GONE);

		// request exit button
		this.mFeatureIV = findViewById(R.id.in_meeting_feature);
		this.mFeatureIV.setOnClickListener(mFeatureShowPopup);

		// request speak or mute button
		this.mSpeakerIV = (ImageView) findViewById(R.id.speaker_iv);
		this.mSpeakerIV.setOnClickListener(mApplySpeakerListener);

		this.mCameraIV = (ImageView) findViewById(R.id.iv_camera);
		this.mCameraIV.setOnClickListener(onClickMCameraIV);
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

		// show invition button
		mMenuInviteAttendeeButton = findViewById(R.id.in_meeting_menu_show_invition_attendees_button);
		mMenuInviteAttendeeButton.setTag("invition");
		mMenuInviteAttendeeButton.setOnClickListener(mMenuShowButtonListener);

		// show attendee list layout button
		mMenuAttendeeButton = findViewById(R.id.in_meeting_menu_show_attendees_button);
		mMenuAttendeeButton.setTag("attendee");
		mMenuAttendeeButton.setOnClickListener(mMenuShowButtonListener);

		// show message layout button
		mMenuMessageButton = findViewById(R.id.in_meeting_menu_show_msg_button);
		mMenuMessageButton.setTag("msg");
		mMenuMessageButton.setOnClickListener(mMenuShowButtonListener);

		// show document display button
		mMenuDocButton = findViewById(R.id.in_meeting_menu_show_doc_button);
		mMenuDocButton.setTag("doc");
		mMenuDocButton.setOnClickListener(mMenuShowButtonListener);

		mConverseLocalCameraButton = findViewById(R.id.converse_camera_button);
		mConverseLocalCameraButton.setOnClickListener(mConverseCameraListener);

		mMenuButtonGroup = new View[] { mMenuInviteAttendeeButton,
				mMenuMessageButton, mMenuAttendeeButton, mMenuDocButton };

		this.mVideoLayout = (FrameLayout) findViewById(R.id.video_layout);
		mGroupNameTV.setText(cg.getName());

		if (cg.getOwnerUser().getmUserId() != GlobalHolder.getInstance()
				.getCurrentUserId()) {
			mChairmanControl.setVisibility(View.INVISIBLE);
		} else {
			// // If current user is conference creatoer, than update control
			// // permission to granted
			// mCurrentAttendeeLectureState = PermissionState.GRANTED;
		}

		// Initialize broadcast receiver
		initBroadcastReceiver();

		// Start animation
		this.overridePendingTransition(R.animator.nonam_scale_center_0_100,
				R.animator.nonam_scale_null);

		bindService(new Intent(mContext, ConferencMessageSyncService.class),
				mLocalServiceConnection, Context.BIND_AUTO_CREATE);

		audioManager = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);
		isBluetoothHeadsetConnected = audioManager.isBluetoothA2dpOn();
		audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
		// Broadcast for user joined conference, to inform that quit P2P
		// conversation
		broadcastForJoined();
		showMuteCameraDialog();
	}

	@Override
	protected void onStart() {
		super.onStart();

//		if (audioManager != null) {
//			audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
//		}

		if (mServiceBound) {
			suspendOrResume(true);
		} else {

			headsetAndBluetoothHeadsetHandle();

			// // Set audio use speaker phone
			// updateAudioSpeaker(true);
		}
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.cancel(PublicIntent.VIDEO_NOTIFICATION_ID);
		// keep screen on
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		adjustContentLayout();
		// Adjust content layout
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

	private void initBroadcastReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(JNIService.JNI_BROADCAST_NEW_CONF_MESSAGE);
		filter.addAction(JNIService.JNI_BROADCAST_CONFERENCE_REMOVED);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_ADDED);
		filter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		filter.addAction(PublicIntent.PREPARE_FINISH_APPLICATION);
		filter.addAction(PublicIntent.NOTIFY_CONFERENCE_ACTIVITY);
		filter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		filter.addAction(JNIService.JNI_BROADCAST_USER_STATUS_NOTIFICATION);
		filter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_USER_PRESENT);
		filter.addAction(Intent.ACTION_HEADSET_PLUG);
		filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
		filter.addAction(JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_CLOSE_VIDEO);
		filter.addAction(JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_OPEN_VIDEO);
		filter.addAction(JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_CLOSE_VIDEO_TO_MOBILE);
		filter.addAction(JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_OPEN_VIDEO_TO_MOBILE);

		Intent i = mContext.registerReceiver(mConfUserChangeReceiver, filter);
		// means exist close broadcast, need to finish this activity
		// if (i != null) {
		// removeStickyBroadcast(i);
		// Toast.makeText(mContext, R.string.confs_is_deleted_notification,
		// Toast.LENGTH_LONG).show();
		// finish();
		// }
	}

	private boolean initConferenceDate() {
		conf = (Conference) this.getIntent().getExtras().get("conf");

		cg = (ConferenceGroup) GlobalHolder.getInstance().getGroupById(
				V2GlobalEnum.GROUP_TYPE_CONFERENCE, conf.getId());

		if (cg == null) {
			V2Log.e(" doesn't receive group information  yet");
			return false;
		}

		initAttendeeList(cg);

		if (currentAttendee == null) {
			currentAttendee = new Attendee(GlobalHolder.getInstance()
					.getCurrentUser());
			mAttendeeList.add(currentAttendee);
		}

		currentAttendee
				.setChairMan(GlobalHolder.getInstance()
						.findGroupById(conf.getId()).getOwnerUser()
						.getmUserId() == currentAttendee.getAttId());

		// 默认设置自己不是主讲，主讲会在入会后广播过来
		if (currentAttendee.isChairMan()) {
			currentAttendee.setLectureState(Attendee.LECTURE_STATE_GRANTED);
		} else {
			currentAttendee.setLectureState(Attendee.LECTURE_STATE_NOT);
		}
		// mCurrentAttendeeLectureState = PermissionState.NORMAL;

		currentAttendee.setSpeakingState(currentAttendee.isChairMan());

		mPendingMessageList = new ArrayList<VMessage>();

		mPendingPermissionUpdateList = new ArrayList<PermissionUpdateIndication>();

		mHostRequestUsers = new HashSet<User>();
		return true;
	}

	private void initAttendeeList(Group confGroup) {
		List<User> l = confGroup.getUsers();
		for (User u : l) {
			if (TextUtils.isEmpty(u.getName())
					&& GlobalHolder.getInstance().getGlobalState()
							.isGroupLoaded()) {
				V2Log.e(TAG, " User + " + u.getmUserId()
						+ " need to get user base infos");
				ImRequest.getInstance().getUserBaseInfo(u.getmUserId());
			}
			Attendee at = new Attendee(u);

			if (u.getmUserId() == GlobalHolder.getInstance().getCurrentUserId()) {
				if (currentAttendee == null) {
					currentAttendee = at;
					mAttendeeList.add(at);
				}
			} else {
				mAttendeeList.add(at);
			}
		}

		// if(l.size()==0){
		// mVideoHandler.postDelayed(new Runnable() {
		// @Override
		// public void run() {
		// initAttendeeList(cg);
		// }
		// }, 100);
		// }
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

		currentAttendee.setSpeakingState(flag);
		// set flag to speaking icon
		if (!flag) {
			mSpeakerIV.setImageResource(R.drawable.mute_button);
		} else {
			mSpeakerIV.setImageResource(R.drawable.speaking_button);
		}
	}

	private void showCurrentAttendeeLectureStateToast(
			PermissionState newLectureState, boolean reject) {
		int oldLectureState = currentAttendee.getLectureState();
		if (oldLectureState == Attendee.LECTURE_STATE_APPLYING) {
			if (newLectureState == PermissionState.GRANTED) {
				// 20141221 1 同意主讲申请
				Toast.makeText(mContext,
						R.string.confs_toast_get_control_permission,
						Toast.LENGTH_SHORT).show();
				// Apply speaking
				if (!currentAttendee.isSpeaking()) {
					doApplyOrReleaseSpeak(!currentAttendee.isSpeaking());
					// Make sure update start after send request,
					// because update state will update isSpeaking value
					updateSpeakerState(!currentAttendee.isSpeaking());
				}
			} else if (newLectureState == PermissionState.NORMAL) {
				if (reject) {
					// 20141221 1 拒绝主讲申请
					Toast.makeText(mContext,
							R.string.confs_toast_reject_control_permission,
							Toast.LENGTH_SHORT).show();
				} else {
					// 20141221 1 取消主讲申请
					Toast.makeText(mContext,
							R.string.confs_toast_cancel_control_permission,
							Toast.LENGTH_SHORT).show();

				}
			}

		} else if (oldLectureState == Attendee.LECTURE_STATE_GRANTED) {
			if (newLectureState == PermissionState.NORMAL) {
				// 主动释放主讲
				Toast.makeText(mContext,
						R.string.confs_toast_release_control_permission,
						Toast.LENGTH_SHORT).show();
			}
		} else if (oldLectureState == Attendee.LECTURE_STATE_NOT) {
			if (newLectureState == PermissionState.APPLYING) {// 申请中
				// 如果不是主席才提示
				if (!currentAttendee.isChairMan()) {
					Toast.makeText(mContext,
							R.string.confs_title_button_request_host_name,
							Toast.LENGTH_SHORT).show();
				}
			}
		}
		// mCurrentAttendeeLectureState = state;

	}

	private void updateMCameraIVState(boolean flag) {
		// set flag to speaking icon
		if (flag) {
			mCameraIV.setImageResource(R.drawable.confernce_camera);
		} else {
			mCameraIV.setImageResource(R.drawable.confernce_camera_mute);
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
			if (currentAttendee.isSpeaking()) {

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
				updateAttendeePermissionState(ind);
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
					mCurrentLecturerActivateDocId);
			mDocContainer.setListener(subViewListener);

			Group g = GlobalHolder.getInstance().findGroupById(conf.getId());
			if (g != null && g instanceof ConferenceGroup) {
				mDocContainer
						.updateSyncStatus(((ConferenceGroup) g).isSyn()
								&& (currentAttendee.getLectureState() != Attendee.LECTURE_STATE_GRANTED));
				mDocContainer.updateCurrentDoc();
			}

		} else {
			mDocContainer.updateCurrentDoc();
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

		width = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay().getWidth() - marginLeft;

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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SUB_ACTIVITY_CODE_SHARE_DOC
				&& resultCode != Activity.RESULT_CANCELED) {
			if (currentAttendee.getLectureState() == Attendee.LECTURE_STATE_NOT) {
				Toast.makeText(this, "主讲权已被取消，共享失败", Toast.LENGTH_LONG).show();
				return;
			}
			String filePath = data.getStringExtra("checkedImage");
			cb.shareDoc(conf, filePath, null);
			cb.modifyGroupLayout(conf);
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
			// 请求弹出的窗口为正常模式
			requestSubViewRestore();

			if (v.getTag().equals("invition")) {
				if (cg.isCanInvitation() || currentAttendee.isChairMan()) {
					// Make sure invitation layout fill full screen
					// 请求弹出的窗口为全屏模式
					requestSubViewFillScreen();
					content = initInvitionContainer();

				} else {
					Toast.makeText(mContext,
							R.string.error_no_permission_to_invitation,
							Toast.LENGTH_SHORT).show();
				}

			} else if (v.getTag().equals("attendee")) {
				content = initAttendeeContainer();
				// If last state is fixed
				if (mAttendeeContainer.getWindowSizeState()) {
					requestSubViewFixed();
				}

			} else if (v.getTag().equals("msg")) {
				content = initMsgLayout();
				// If last state is fixed
				if (mMessageContainer.getWindowSizeState()) {
					requestSubViewFixed();
				}

			} else if (v.getTag().equals("doc")) {
				content = initDocLayout();
				if (mDocContainer.isFullScreenSize()) {
					requestSubViewFillScreen();
				} else {
					requestSubViewFixed();
				}
				// If current user is chairman or has get host rights then show
				// shared doc button
				if (conf.getChairman() == GlobalHolder.getInstance()
						.getCurrentUserId()
						|| currentAttendee.getLectureState() == Attendee.LECTURE_STATE_GRANTED) {
					((VideoDocLayout) content).requestShowSharedButton(true);
				} else {
					((VideoDocLayout) content).requestShowSharedButton(false);
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
			doApplyOrReleaseSpeak(!currentAttendee.isSpeaking());
			// Make sure update start after send request,
			// because update state will update isSpeaking value
			updateSpeakerState(!currentAttendee.isSpeaking());
		}
	};

	private OnClickListener onClickMCameraIV = new OnClickListener() {
		@Override
		public void onClick(View view) {
			if (isMuteCamera) {
				isMuteCamera = false;
				cb.enableVideoDev("", true);
				updateMCameraIVState(true);
			} else {
				isMuteCamera = true;
				cb.enableVideoDev("", false);
				updateMCameraIVState(false);

			}

		}
	};

	private OnClickListener mShowQuitWindowListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			showQuitDialog(mContext.getText(R.string.in_meeting_quit_text)
					.toString());
		}
	};

	private OnClickListener mRequestHostListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			((ImageView) view
					.findViewById(R.id.conference_activity_request_host_iv))
					.setImageResource(R.drawable.logout_button);

			((TextView) view
					.findViewById(R.id.conference_activity_request_host_tv))
					.setText(R.string.confs_title_button_request_host_name);

			if ((currentAttendee.getLectureState() == Attendee.LECTURE_STATE_GRANTED || currentAttendee
					.getLectureState() == Attendee.LECTURE_STATE_APPLYING)) {
				// updateCurrentAttendeeLectureState(PermissionState.NORMAL,
				// false);
				cb.applyForReleasePermission(ConferencePermission.CONTROL, null);
				// 释放主讲
			} else {
				// updateCurrentAttendeeLectureState(PermissionState.APPLYING,
				// false);
				currentAttendee
						.setLectureState(Attendee.LECTURE_STATE_APPLYING);
				if (mAttendeeContainer != null) {
					mAttendeeContainer.updateDisplay();
				}
				currentAttendee
						.setLectureState(Attendee.LECTURE_STATE_APPLYING);
				cb.applyForControlPermission(ConferencePermission.CONTROL, null);
				// 申请主讲中
			}
		}
	};

	private OnClickListener mFeatureShowPopup = new OnClickListener() {
		@Override
		public void onClick(View anchor) {
			showMoreWindow(anchor);
		}

	};

	private void showMoreWindow(View anchor) {
		DisplayMetrics dm = new DisplayMetrics();
		((Activity) mContext).getWindowManager().getDefaultDisplay()
				.getMetrics(dm);
		if (moreWindow == null) {
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.conference_pop_up_window,
					null);

			layout.findViewById(R.id.conference_activity_request_host_button)
					.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							moreWindow.dismiss();
							mRequestHostListener.onClick(v);
						}

					});

			layout.findViewById(R.id.video_quality).setOnClickListener(
					new OnClickListener() {

						@Override
						public void onClick(View v) {
							moreWindow.dismiss();
							mShowSettingListener.onClick(v);
						}

					});

			layout.findViewById(R.id.conference_activity_logout_button)
					.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							moreWindow.dismiss();
							mShowQuitWindowListener.onClick(v);
						}

					});

			mRequestButtonName = (TextView) layout
					.findViewById(R.id.conference_activity_request_host_tv);
			mRequestButtonImage = (ImageView) layout
					.findViewById(R.id.conference_activity_request_host_iv);

			LinearLayout itemContainer = (LinearLayout) layout
					.findViewById(R.id.common_pop_window_container);

			itemContainer.measure(View.MeasureSpec.UNSPECIFIED,
					View.MeasureSpec.UNSPECIFIED);
			View arrow = layout.findViewById(R.id.common_pop_up_arrow_up);

			mRequestButtonName = (TextView) layout
					.findViewById(R.id.conference_activity_request_host_tv);
			mRequestButtonImage = (ImageView) layout
					.findViewById(R.id.conference_activity_request_host_iv);

			arrow.measure(View.MeasureSpec.UNSPECIFIED,
					View.MeasureSpec.UNSPECIFIED);

			int height = itemContainer.getMeasuredHeight()
					+ arrow.getMeasuredHeight();

			moreWindow = new PopupWindow(layout,
					ViewGroup.LayoutParams.WRAP_CONTENT, height, true);
			moreWindow.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss() {
					moreWindow.dismiss();
				}

			});
			moreWindow.setBackgroundDrawable(new ColorDrawable(
					Color.TRANSPARENT));
			moreWindow.setFocusable(true);
			moreWindow.setTouchable(true);
			moreWindow.setOutsideTouchable(true);

		}

		updateMoreWindowDisplay();

		int[] pos = new int[2];
		anchor.getLocationInWindow(pos);
		pos[1] += anchor.getMeasuredHeight() - anchor.getPaddingBottom();
		// calculate arrow offset
		View arrow = moreWindow.getContentView().findViewById(
				R.id.common_pop_up_arrow_up);
		arrow.bringToFront();

		RelativeLayout.LayoutParams arrowRL = (RelativeLayout.LayoutParams) arrow
				.getLayoutParams();
		arrowRL.rightMargin = dm.widthPixels - pos[0]
				- (anchor.getMeasuredWidth() / 2) - arrow.getMeasuredWidth();
		arrow.setLayoutParams(arrowRL);

		moreWindow.setAnimationStyle(R.style.TitleBarPopupWindowAnim);
		int marginRight = DensityUtils.dip2px(mContext, 5);
		moreWindow.showAtLocation(anchor, Gravity.RIGHT | Gravity.TOP,
				marginRight, pos[1]);
	}

	private void updateMoreWindowDisplay() {
		if (mRequestButtonName == null || mRequestButtonImage == null) {
			return;
		}

		if (currentAttendee.getLectureState() == Attendee.LECTURE_STATE_APPLYING) {
			mRequestButtonName.setTextColor(mContext.getResources().getColor(
					R.color.conference_host_requesting_text_color));
			mRequestButtonImage.setImageResource(R.drawable.host_requesting);
			mRequestButtonName
					.setText(R.string.confs_title_button_request_host_name);
		} else if (currentAttendee.getLectureState() == Attendee.LECTURE_STATE_GRANTED) {
			mRequestButtonName.setTextColor(mContext.getResources().getColor(
					R.color.conference_acquired_host_text_color));
			mRequestButtonImage.setImageResource(R.drawable.host_required);
			mRequestButtonName
					.setText(R.string.confs_title_button_release_host_name);
		} else if (currentAttendee.getLectureState() == Attendee.LECTURE_STATE_NOT) {
			mRequestButtonName.setTextColor(mContext.getResources().getColor(
					R.color.common_item_text_color_black));
			mRequestButtonImage.setImageResource(R.drawable.host_request);
			mRequestButtonName
					.setText(R.string.confs_title_button_request_host_name);
		}
	}

	private OnClickListener onClickMChairmanControl = new OnClickListener() {
		public void onClick(View v) {
			showChairmanControlPopWindow(v);
		}
	};

	private void showChairmanControlPopWindow(View v) {
		if (mChairControlWindow == null) {
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(
					R.layout.in_meeting_chairman_control_pop_up_window, null);

			ImageView arrow = (ImageView) view.findViewById(R.id.arrow);
			int widthSpec = View.MeasureSpec.makeMeasureSpec(0,
					View.MeasureSpec.UNSPECIFIED);
			int heightSpec = View.MeasureSpec.makeMeasureSpec(0,
					View.MeasureSpec.UNSPECIFIED);
			arrow.measure(widthSpec, heightSpec);
			arrowWidth = arrow.getMeasuredWidth();

			CheckBox slience = (CheckBox) view.findViewById(R.id.cb_slience);
			CheckBox invitation = (CheckBox) view
					.findViewById(R.id.cb_invitation);
			CheckBox conferenceMessage = (CheckBox) view
					.findViewById(R.id.conference_message);

			mConfMsgRedDot = view
					.findViewById(R.id.host_request_msg_notificator);
			slience.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					cb.muteConf();
					mChairControlWindow.dismiss();

				}
			});

			invitation
					.setOnCheckedChangeListener(new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							cb.updateConferenceAttribute(conf, cg.isSyn(),
									isChecked, null);
							mChairControlWindow.dismiss();
						}
					});

			conferenceMessage
					.setOnCheckedChangeListener(new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							mChairControlWindow.dismiss();
							// 您当前没有会议消息
							if (mHostRequestUsers != null
									&& mHostRequestUsers.size() > 0) {
								showConfMsgPopWindow();
							} else {
								Toast.makeText(ConferenceActivity.this,
										"您当前没有会议消息", Toast.LENGTH_SHORT).show();
							}
						}

					});

			// set
			mChairControlWindow = new PopupWindow(view,
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			mChairControlWindow.setBackgroundDrawable(new ColorDrawable(
					Color.TRANSPARENT));
			mChairControlWindow.setFocusable(true);
			mChairControlWindow.setTouchable(true);
			mChairControlWindow.setOutsideTouchable(true);

		}

		if (mConfMsgRedDot != null) {
			if (hasUnreadChiremanControllMsg) {
				mConfMsgRedDot.setVisibility(View.VISIBLE);
			} else {
				mConfMsgRedDot.setVisibility(View.GONE);
			}
		}

		int[] pos = new int[2];
		v.getLocationInWindow(pos);
		pos[0] += v.getMeasuredWidth() / 2 - 15 * dm.density - arrowWidth / 2;
		pos[1] += v.getMeasuredHeight();

		mChairControlWindow.setAnimationStyle(R.style.TitleBarPopupWindowAnim);
		mChairControlWindow.showAtLocation(v, Gravity.NO_GRAVITY, pos[0],
				pos[1]);
	}

	private void showConfMsgPopWindow() {
		if (mHostRequestWindow == null) {
			mHostRequestWindow = new VideoShowHostRequest(mContext,
					mHostRequestUsers, cb);
		} else {
			mHostRequestWindow.updateList(mHostRequestUsers);
		}

		mHostRequestWindow.showAtLocation(mRootContainer, Gravity.CENTER, 0, 0);
		mMsgNotification.setVisibility(View.GONE);
		hasUnreadChiremanControllMsg = false;
	}

	// 20141218 1
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
			}

			// calculate arrow offset
			mSettingWindow.showAtLocation(mRootContainer, Gravity.CENTER, 0, 0);
		}

	};

	class ConfBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (JNIService.JNI_BROADCAST_NEW_CONF_MESSAGE.equals(intent
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
					List<User> l = confGroup.getUsers();
					for (User u : l) {
						Attendee at = new Attendee(u);

						boolean contain = false;
						for (Attendee tempAt : mAttendeeList) {
							if (at.getAttId() == tempAt.getAttId()) {
								contain = true;
							}
						}

						boolean bt = false;
						if (!contain) {
							bt = mAttendeeList.add(at);
						}

						if (bt) {
							list.add(at);
						}
					}
					if (mAttendeeContainer != null) {
						mAttendeeContainer.addNewAttendee(list);
					}
				}

			} else if (JNIService.JNI_BROADCAST_GROUP_USER_REMOVED
					.equals(intent.getAction())) {

				GroupUserObject obj = intent.getParcelableExtra("obj");
				if (obj == null) {
					V2Log.e(TAG,
							"Received the broadcast to quit the conference group , but given GroupUserObject is null!");
					return;
				}

				if (obj.getmType() != V2GlobalEnum.GROUP_TYPE_CONFERENCE)
					return;

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
					Message.obtain(mVideoHandler,
							ATTENDEE_ENTER_OR_EXIT_LISTNER, 0, 0, user)
							.sendToTarget();
				}

			} else if (PublicIntent.PREPARE_FINISH_APPLICATION.equals(intent
					.getAction())) {
				// Listen quit request to make sure close all device
				finish();
			} else if (PublicIntent.NOTIFY_CONFERENCE_ACTIVITY.equals(intent
					.getAction())) {
				// from VideoMsgChattingLayout 聊天打开图片
				isMoveTaskBack = false;
			} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {

			} else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {

			} else if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {

				if (intent.hasExtra("state")) {
					int state = intent.getIntExtra("state", 0);
					if (state == 1) {
						V2Log.i(TAG, "插入耳机");
						headsetAndBluetoothHeadsetHandle();
					} else if (state == 0) {
						V2Log.i(TAG, "拔出耳机");
						headsetAndBluetoothHeadsetHandle();
					}
				}

			} else if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED
					.equals(intent.getAction())) {

				int state = intent
						.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);

				if (state == BluetoothProfile.STATE_CONNECTED) {
					isBluetoothHeadsetConnected = true;
					V2Log.i(TAG, "蓝牙耳机已连接");
					headsetAndBluetoothHeadsetHandle();
				} else if (state == BluetoothProfile.STATE_DISCONNECTED) {
					V2Log.i("TAG", "蓝牙耳机已断开");
					isBluetoothHeadsetConnected = false;
					headsetAndBluetoothHeadsetHandle();
				}

			} else if (JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_CLOSE_VIDEO
					.equals(intent.getAction())) {
				V2Log.d(V2Log.UI_BROADCAST,
						"CLASS = ConferenceActivity  BROADCAST = JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_CLOSE_VIDEO");

				// 传来的是设备ID
				String dstDeviceID = intent.getStringExtra("dstDeviceID");
				UserDeviceConfig userDeviceConfig = null;

				if (dstDeviceID == null) {
					return;
				}

				for (SurfaceViewW sw : mCurrentShowedSV) {

					if (dstDeviceID.equals(sw.udc.getDeviceID())) {
						userDeviceConfig = sw.udc;
						break;
					}

				}

				closeAttendeeVideo(userDeviceConfig);

			} else if (JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_OPEN_VIDEO
					.equals(intent.getAction())) {

				V2Log.d(V2Log.UI_BROADCAST,
						"CLASS = ConferenceActivity  BROADCAST = JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_OPEN_VIDEO");

				if (isSyn && isVoiceActivation) {
					String xml = intent.getStringExtra("xml");
					if (xml == null || xml.equals("")) {
						return;
					}
					// 获得两个参数在列表中查的

					String dstUserIDStr = XmlAttributeExtractor.extract(xml,
							" DstUserID='", "'");
					String dstDeviceID = XmlAttributeExtractor.extract(xml,
							" DstDeviceID='", "'");
					if (dstUserIDStr == null || dstDeviceID == null) {
						return;
					}

					long dstUserID = -1;
					try {
						dstUserID = Long.valueOf(dstUserIDStr);
					} catch (NumberFormatException e) {
						V2Log.e(V2Log.XML_ERROR,
								" CLASS =ConferenceActivity.BroadcastReceiver METHOD = onReceive()"
										+ "dstUserIDStr = " + dstUserIDStr
										+ "is not long");
						return;
					}

					UserDeviceConfig userDeviceConfig = null;
					for (Attendee attendee : mAttendeeList) {

						if (attendee.getAttId() == dstUserID) {
							if (attendee.isSelf()) {
								return;
							}
							List<UserDeviceConfig> list = attendee
									.getmDevices();
							if (list == null) {
								return;
							}

							boolean ret = false;
							for (UserDeviceConfig udc : list) {
								if (udc.getDeviceID().equals(dstDeviceID)) {
									userDeviceConfig = udc;
									ret = true;
									break;
								}
							}

							if (ret) {
								break;
							}
						}
					}

					if (userDeviceConfig == null) {
						return;
					}
					V2Log.i("20141210 1",
							"语音激励打开视频:" + userDeviceConfig.getDeviceID());
					openAttendeeVideo(userDeviceConfig);

				}
			} else if (JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_CLOSE_VIDEO_TO_MOBILE
					.equals(intent.getAction())) {

				V2Log.d(V2Log.UI_BROADCAST,
						"CLASS = ConferenceActivity  BROADCAST = JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_CLOSE_VIDEO_TO_MOBILE");
				// 传来的是设备ID
				String dstDeviceID = intent.getStringExtra("sDstMediaID");
				UserDeviceConfig userDeviceConfig = null;

				if (dstDeviceID == null) {
					return;
				}

				for (SurfaceViewW sw : mCurrentShowedSV) {

					if (dstDeviceID.equals(sw.udc.getDeviceID())) {
						userDeviceConfig = sw.udc;
						break;
					}

				}
				if (userDeviceConfig == null) {
					return;
				}
				V2Log.i("20141210 1",
						"同步关闭视频:" + userDeviceConfig.getDeviceID());
				closeAttendeeVideo(userDeviceConfig);

			} else if (JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_OPEN_VIDEO_TO_MOBILE
					.equals(intent.getAction())) {

				V2Log.d(V2Log.UI_BROADCAST,
						"CLASS = ConferenceActivity  BROADCAST = JNIService.JNI_BROADCAST_CONFERENCE_CONF_SYNC_OPEN_VIDEO_TO_MOBILE");
				if (isSyn) {
					String xml = intent.getStringExtra("sSyncVideoMsgXML");
					if (xml == null || xml.equals("")) {
						return;
					}
					// 获得两个参数在列表中查的

					while (true) {
						String dstUserIDStr = XmlAttributeExtractor.extract(
								xml, " DstUserID='", "'");

						String dstDeviceID = XmlAttributeExtractor.extract(xml,
								" DstDeviceID='", "'");

						if (dstUserIDStr == null || dstDeviceID == null) {
							return;
						}
						int indexDstUserID = xml.indexOf(" DstUserID='")
								+ " DstUserID='".length();
						int indexDstDeviceID = xml.indexOf(" DstDeviceID='")
								+ " DstDeviceID='".length();
						if (indexDstUserID > indexDstDeviceID) {
							xml = xml.substring(indexDstUserID);
						} else {
							xml = xml.substring(indexDstDeviceID);
						}

						long dstUserID = -1;
						try {
							dstUserID = Long.valueOf(dstUserIDStr);
						} catch (NumberFormatException e) {
							V2Log.e(V2Log.XML_ERROR,
									" CLASS =ConferenceActivity.BroadcastReceiver METHOD = onReceive()"
											+ "dstUserIDStr = " + dstUserIDStr
											+ "is not long");
							return;
						}

						UserDeviceConfig userDeviceConfig = null;
						for (Attendee attendee : mAttendeeList) {

							if (attendee.getAttId() == dstUserID) {
								if (attendee.isSelf()) {
									V2Log.i("20141210 1", "指给移动端打开视频:"
											+ "是自己跳过");
									return;
								}
								List<UserDeviceConfig> list = attendee
										.getmDevices();
								if (list == null) {
									return;
								}

								boolean ret = false;
								for (UserDeviceConfig udc : list) {
									if (udc.getDeviceID().equals(dstDeviceID)) {
										userDeviceConfig = udc;
										ret = true;
										break;
									}
								}

								if (ret) {
									break;
								}

							}

							// 混合视频的情况
							if (attendee.getType() == Attendee.TYPE_MIXED_VIDEO) {

								List<UserDeviceConfig> list = ((AttendeeMixedDevice) attendee)
										.getmDevices();

								if (list == null) {
									return;
								}

								boolean ret = false;
								for (UserDeviceConfig udc : list) {
									if (udc.getDeviceID().equals(dstDeviceID)) {
										userDeviceConfig = udc;
										ret = true;
										break;
									}
								}

								if (ret) {
									break;
								}
							}

						}
						if (userDeviceConfig == null) {
							return;
						}
						V2Log.i("20141210 1",
								"指给移动端打开视频:" + userDeviceConfig.getDeviceID());
						openAttendeeVideo(userDeviceConfig);
					}

				}

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
			// udc = new UserDeviceConfig(V2GlobalEnum.GROUP_TYPE_CONFERENCE,
			// conf.getId(), atd.getAttId(),
			// String.valueOf(atd.getAttId())+":Camera", null);
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

	private void hindSoftInput(View v) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null && v != null) {
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
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
				mVideoLayout.setVisibility(View.VISIBLE);
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
			headsetAndBluetoothHeadsetHandle();
			// updateAudioSpeaker(false);
		}

//		if (audioManager != null) {
//			audioManager.setMode(AudioManager.MODE_NORMAL);
//		}
	}

	@Override
	public void finish() {
		quit();
		super.finish();
		// this.overridePendingTransition(R.animator.nonam_scale_null,
		// R.animator.nonam_scale_center_100_0);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (isFinish) {
			super.onDestroy();
			return;
		}

		mContext.unregisterReceiver(mConfUserChangeReceiver);
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
			cb.removeAttendeeEnterOrExitListener(this.mVideoHandler,
					ATTENDEE_ENTER_OR_EXIT_LISTNER, null);

			cb.unRegisterPermissionUpdateListener(this.mVideoHandler,
					NOTIFY_HOST_PERMISSION_REQUESTED, null);

			cb.unRegisterVideoMixerListener(mVideoHandler,
					VIDEO_MIX_NOTIFICATION, null);

			ds.unRegisterNewDocNotification(mVideoHandler,
					NEW_DOC_NOTIFICATION, null);
			ds.unRegisterDocDisplayNotification(mVideoHandler,
					DOC_DOWNLOADE_COMPLETE_ONE_PAGE_NOTIFICATION, null);
			ds.unRegisterDocClosedNotification(mVideoHandler,
					DOC_CLOSED_NOTIFICATION, null);
			ds.unRegisterDocPageAddedNotification(mVideoHandler,
					DOC_ADDED_ONE_PAGE_NOTIFICATION, null);
			ds.unRegisterPageCanvasUpdateNotification(mVideoHandler,
					DOC_PAGE_CANVAS_NOTIFICATION, null);
			cb.removeSyncStateListener(mVideoHandler, SYNC_STATE_NOTIFICATION,
					null);
			cb.removeInvitationStateListener(mVideoHandler,
					INVITATION_STATE_NOTIFICATION, null);

			cb.removeVoiceActivationListener(mVideoHandler,
					VOICEACTIVATION_NOTIFICATION, null);
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
		if (audioManager != null) {
			// audioManager.setSpeakerphoneOn(false);
			audioManager.setMode(AudioManager.MODE_NORMAL);
		}
		super.onDestroy();
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

		if (!isMoveTaskBack) {
			isMoveTaskBack = true;
		} else {
			moveTaskToBack(true);
		}
	}

	/**
	 * Update speaker flag according headset state
	 * 
	 * @param flag
	 *            true means start, false means on stop
	 */
	// private void updateAudioSpeaker(boolean flag) {
	// audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
	//
	// GlobalState gs = GlobalHolder.getInstance().getGlobalState();
	// if (gs.isBluetoothHeadsetPluged() || gs.isWiredHeadsetPluged()) {
	// audioManager.setSpeakerphoneOn(false);
	// } else {
	// audioManager.setSpeakerphoneOn(true);
	// }
	//
	// }

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
					VerificationProvider.deleteCrowdVerificationMessage(conf
							.getId() , -1);
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

	private void showMuteCameraDialog() {
		if (mMuteCameraDialog == null) {

			final Dialog d = new Dialog(mContext, R.style.DialogStyle1);

			d.setContentView(R.layout.dialog_mode1);
			final Button cancelB = (Button) d
					.findViewById(R.id.IMWCancelButton);
			cancelB.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					d.dismiss();
				}

			});
			final Button quitB = (Button) d.findViewById(R.id.IMWOKButton);
			quitB.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					d.dismiss();
					isMuteCamera = true;
					cb.enableVideoDev("", false);
					updateMCameraIVState(false);
				}

			});
			mMuteCameraDialog = d;
		}

		TextView muteContent = (TextView) mMuteCameraDialog
				.findViewById(R.id.content);
		TextView muteConfirm = (TextView) mMuteCameraDialog
				.findViewById(R.id.IMWOKButton);
		TextView muteCancel = (TextView) mMuteCameraDialog
				.findViewById(R.id.IMWCancelButton);
		muteContent.setText(R.string.in_meeting_mute_hit_content);
		muteConfirm.setText(R.string.in_meeting_mute_button);
		muteCancel.setText(R.string.in_meeting_mute_cancel_button);
		mMuteCameraDialog.show();
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
			doApplyOrReleaseSpeak(currentAttendee.isSpeaking());
			// Make sure update start after send request,
			// because update state will update isSpeaking value
			updateSpeakerState(currentAttendee.isSpeaking());
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
		headsetAndBluetoothHeadsetHandle();
		// updateAudioSpeaker(resume);
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
		setResult(ConversationsTabFragment.CONFERENCE_ENTER_CODE, i);

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
	 * @param att
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
		// 暂不提示进入或退出
		// showToastNotification(att.getAttName()
		// + mContext.getText(R.string.conf_notification_joined_meeting));
	}

	private void showToastNotification(String text) {
		if (mToast != null) {
			mToast.cancel();
		}
		mToast = Toast.makeText(mContext, text, Toast.LENGTH_SHORT);
		mToast.show();
	}

	private void doUpdateSyncEvent(boolean flag, int type) {
		if (type == 0) {

		}
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

		// 咱不提示进入或退出
		// // Clean user device
		// showToastNotification(att.getAttName()
		// + mContext.getText(R.string.conf_notification_quited_meeting));

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
			return closeAttendeeVideo(udc);
		} else {
			return openAttendeeVideo(udc);
		}

	}

	public boolean openAttendeeVideo(UserDeviceConfig udc) {
		if (udc == null) {
			V2Log.e(" can't not open device");
			return false;
		}
		if (udc.isShowing()) {
			return true;
		}
		if (checkVideoExceedMaminum()) {
			Toast.makeText(mContext, R.string.error_exceed_support_video_count,
					Toast.LENGTH_SHORT).show();
			return false;
		}

		VideoPlayer vp = new VideoPlayer();
		udc.setSVHolder(new SurfaceView(this));
		udc.setVp(vp);
		if (udc.getBelongsAttendee() instanceof AttendeeMixedDevice) {
			vp.setLayout(((AttendeeMixedDevice) udc.getBelongsAttendee())
					.getMV().getType().toIntValue());
		}
		SurfaceHolderObserver observer = new SurfaceHolderObserver(cg, cs, udc);
		SurfaceViewW sw = new SurfaceViewW(udc.getBelongsAttendee(), udc,
				observer);
		mCurrentShowedSV.add(sw);

		// Do adjust layout first, then request open device.
		// otherwise can't show video
		adjustVideoLayout();
		// Request open device
		sw.observer.open();

		udc.setShowing(true);

		Log.i("20141220 2", "openAttendeeVideo() udc.id =" + udc.getDeviceID()
				+ " udc.isShowing =" + udc.isShowing());

		if (mAttendeeContainer != null) {
			mAttendeeContainer.updateDisplay();
		}

		return true;
	}

	public boolean closeAttendeeVideo(UserDeviceConfig udc) {
		if (udc == null) {
			V2Log.e(" can't not open or close device");
			return false;
		}
		// if already opened attendee's video, switch action to close
		if (udc.isShowing()) {
			for (SurfaceViewW sw : mCurrentShowedSV) {
				if (sw.udc == udc) {
					sw.observer.close();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mCurrentShowedSV.remove(sw);
					mVideoLayout.removeView(sw.getView());
					sw.rl.removeAllViews();
					break;
				}
			}
			udc.setShowing(false);
			udc.doClose();
			adjustVideoLayout();

			if (mAttendeeContainer != null) {
				mAttendeeContainer.updateDisplay();
			}
		}

		return true;
	}

	/**
	 * Update attendee state
	 * 
	 * @param ind
	 */
	private boolean updateAttendeePermissionState(PermissionUpdateIndication ind) {
		Attendee attendee = null;

		for (Attendee att : mAttendeeList) {
			if (att.getAttId() == ind.getUid()) {
				attendee = att;
			}
		}

		if (attendee != null) {

			ConferencePermission conferencePermission = ConferencePermission
					.fromInt(ind.getType());
			PermissionState permissionState = PermissionState.fromInt(ind
					.getState());

			if (conferencePermission == ConferencePermission.SPEAKING) {
				if (permissionState == PermissionState.GRANTED) {
					attendee.setSpeakingState(true);
				} else {
					attendee.setSpeakingState(false);
				}
			} else if (conferencePermission == ConferencePermission.CONTROL) {
				if (permissionState == PermissionState.GRANTED) {
					attendee.setLectureState(Attendee.LECTURE_STATE_GRANTED);
					// 取消自己的默认主讲
					if (currentAttendee.isChairMan()) {
						if (currentAttendee.getAttId() != attendee.getAttId()) {
							if (currentAttendee.getLectureState() == Attendee.LECTURE_STATE_GRANTED) {
								currentAttendee
										.setLectureState(Attendee.LECTURE_STATE_NOT);
							}
						}
					}

				} else if (permissionState == PermissionState.APPLYING) {
					attendee.setLectureState(Attendee.LECTURE_STATE_APPLYING);
				} else if (permissionState == PermissionState.NORMAL) {
					attendee.setLectureState(Attendee.LECTURE_STATE_NOT);
				}
			}

			if (mAttendeeContainer != null) {
				mAttendeeContainer.updateDisplay();
			}
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
			Log.i("20141220 1", "updateAttendeeDevice");
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
		V2Log.d(TAG, "opt : " + opt);
		V2Doc doc = null;
		V2Doc.Page page = null;
		V2Doc.Doc pageArray = null;
		V2ShapeMeta shape = null;
		if (opt == NEW_DOC_NOTIFICATION) {
			doc = (V2Doc) res.getResult();
			V2Doc cacheDoc = mDocs.get(doc.getId());
			if (cacheDoc == null) {
				mDocs.put(doc.getId(), doc);
			} else {
				cacheDoc.updateV2Doc(doc);
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
		case DOC_PAGE_LIST_NOTIFICATION:
			pageArray = (V2Doc.Doc) res.getResult();
			docId = pageArray.getDocId();
			break;
		case DOC_ADDED_ONE_PAGE_NOTIFICATION:
		case DOC_DOWNLOADE_COMPLETE_ONE_PAGE_NOTIFICATION:
		case DOC_TURN_PAGE_NOTIFICATION:// 上下翻页
			page = (V2Doc.Page) res.getResult();
			docId = page.getDocId();
			// Record current activate Id;
			mCurrentLecturerActivateDocId = docId;

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
				if (opt == DOC_DOWNLOADE_COMPLETE_ONE_PAGE_NOTIFICATION)
					V2Log.e(TAG, "Update Doc Page failed , 没有获取到文件路径");
			}
			// If doc is not null, means new doc event or doc close event. need
			// to update cache doc or not.
			// If doc page event before new doc event, need to update cache
			// update
		} else if (NEW_DOC_NOTIFICATION == opt) {
			V2Doc cache = mDocs.get(docId);
			if (cache != doc) {
				cache.updateV2Doc(doc);
			}
		}

		if (pageArray != null) {
			doc.updateDoc(pageArray);
		}
		if (page != null) {
			doc.addPage(page);
		}

		if (opt == DOC_PAGE_CANVAS_NOTIFICATION) {
			// page = doc.getPage(shape.getPageNo());
			// if (page == null) {
			// page = new Page(shape.getPageNo(), docId, null);
			// page.addMeta(shape);
			// doc.addPage(page);
			// } else {
			// page.addMeta(shape);
			// }
		} else if (opt == DOC_TURN_PAGE_NOTIFICATION) {
			if (isSyn) {
				doc.setActivatePageNo(page.getNo());
			}
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
			case DOC_PAGE_LIST_NOTIFICATION:
			case DOC_ADDED_ONE_PAGE_NOTIFICATION:
				mDocContainer.updateLayoutPageInformation();
				mDocContainer.updatePageButton();
				break;
			case DOC_TURN_PAGE_NOTIFICATION:
				if (isSyn) {
					Log.i("20141224 1", "翻页");
					doc.setActivatePageNo(page.getNo());
					mDocContainer.updateCurrentDoc();
				}
				break;
			case DOC_DOWNLOADE_COMPLETE_ONE_PAGE_NOTIFICATION:
				mDocContainer.updateCurrentDoc(doc);
				break;
			case DOC_PAGE_CANVAS_NOTIFICATION:
				mDocContainer.drawShape(page.getDocId(), page.getNo(), shape);
				break;
			}
		}

	}

	public void handleDocNotification(AsyncResult res, int opt) {

		V2Doc v2Doc = null;
		String docId = null;
		V2Doc.Doc doc = null;
		V2Doc.Page page = null;

		// V2ShapeMeta shape = null;
		switch (opt) {
		case NEW_DOC_NOTIFICATION:
			Log.i("20141229 1", "NEW_DOC_NOTIFICATION");
			v2Doc = (V2Doc) res.getResult();
			docId = v2Doc.getId();
			V2Doc cacheDoc = mDocs.get(docId);
			if (cacheDoc == null) {
				mDocs.put(docId, v2Doc);
			} else {
				cacheDoc.updateV2Doc(v2Doc);
				v2Doc = cacheDoc;
			}

			if (mDocContainer != null) {
				mDocContainer.addDoc(v2Doc);
			}

			break;
		case DOC_PAGE_LIST_NOTIFICATION:
			Log.i("20141229 1", "DOC_PAGE_LIST_NOTIFICATION");
			doc = (V2Doc.Doc) res.getResult();
			if (doc == null) {
				return;
			}

			docId = doc.getDocId();
			if (docId == null) {
				return;
			}

			v2Doc = mDocs.get(docId);
			if (v2Doc == null) {
				v2Doc = mDocs.get(docId);
				// Put fake doc, because page events before doc event;
				if (v2Doc == null) {
					v2Doc = new V2Doc(docId, null, null, 0, null);
					mDocs.put(docId, v2Doc);
				}
			}

			v2Doc.updateDoc(doc);

			if (mDocContainer != null) {
				mDocContainer.updateLayoutPageInformation();
				mDocContainer.updatePageButton();
			}

			break;

		case DOC_ADDED_ONE_PAGE_NOTIFICATION:
			Log.i("20141229 1", "DOC_ADDED_ONE_PAGE_NOTIFICATION");
			page = (V2Doc.Page) res.getResult();
			if (page == null) {
				return;
			}
			docId = page.getDocId();

			v2Doc = mDocs.get(docId);
			if (v2Doc == null) {
				v2Doc = mDocs.get(docId);
				if (v2Doc == null) {
					v2Doc = new V2Doc(docId, null, null, 0, null);
					mDocs.put(docId, v2Doc);
				}
			}
			// Record current activate Id;
			// mCurrentLecturerActivateDocId = docId;

			if (page != null) {
				v2Doc.addPage(page);
			}

			if (mDocContainer != null) {
				mDocContainer.updateLayoutPageInformation();
				mDocContainer.updatePageButton();
			}

			break;

		case DOC_DOWNLOADE_COMPLETE_ONE_PAGE_NOTIFICATION:
			Log.i("20141229 1", "DOC_DOWNLOADE_COMPLETE_ONE_PAGE_NOTIFICATION");
			page = (V2Doc.Page) res.getResult();
			if (page == null) {
				return;
			}

			docId = page.getDocId();

			v2Doc = mDocs.get(docId);
			if (v2Doc == null) {
				v2Doc = mDocs.get(docId);
				if (v2Doc == null) {
					v2Doc = new V2Doc(docId, null, null, 0, null);
					mDocs.put(docId, v2Doc);
				}

			}
			// Record current activate Id;
			// mCurrentLecturerActivateDocId = docId;

			v2Doc.addPage(page);

			if (page.equals(mCurrentLecturerActivateDocPage)) {
				if (mDocContainer != null) {
					// mDocContainer.updateCurrentDoc(v2Doc);
					mDocContainer.updateCurrentDoc();
				}
			}

			break;
		case DOC_TURN_PAGE_NOTIFICATION:// 上下翻页
			Log.i("20141229 1", "DOC_TURN_PAGE_NOTIFICATION");
			page = (V2Doc.Page) res.getResult();
			if (page == null) {
				return;
			}

			docId = page.getDocId();

			v2Doc = mDocs.get(docId);
			if (v2Doc == null) {
				v2Doc = mDocs.get(docId);
				if (v2Doc == null) {
					v2Doc = new V2Doc(docId, null, null, 0, null);
					mDocs.put(docId, v2Doc);
				}
			}

			mCurrentLecturerActivateDocId = docId;
			mCurrentLecturerActivateDocPage = page;

			if (page != null) {
				v2Doc.addPage(page);
			}

			if (mDocContainer != null) {
				if (isSyn || !isCurrentAttendeeTurnedpage()) {
					v2Doc.setActivatePageNo(page.getNo());
					mDocContainer.updateCurrentDoc(v2Doc);
					mDocContainer.updateCurrentDoc();
				}
			}

			break;
		case DOC_CLOSED_NOTIFICATION:
			Log.i("20141229 1", "DOC_CLOSED_NOTIFICATION");
			v2Doc = (V2Doc) res.getResult();
			if (v2Doc == null) {
				return;
			}

			if (mDocs.get(v2Doc.getId()) == null) {
				return;
			}

			v2Doc = mDocs.remove(v2Doc.getId());

			if (mDocContainer != null) {
				mDocContainer.closeDoc(v2Doc);
			}

			break;

		case DOC_PAGE_CANVAS_NOTIFICATION:
			// shape = (V2ShapeMeta) res.getResult();
			// docId = shape.getDocId();
			//
			// v2Doc = mDocs.get(docId);
			// if (v2Doc == null) {
			// return;
			// }
			//
			// page = v2Doc.getPage(shape.getPageNo());
			// if (page == null) {
			// page = new Page(shape.getPageNo(), docId, null);
			// page.addMeta(shape);
			// v2Doc.addPage(page);
			// } else {
			// page.addMeta(shape);
			// }

			break;
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
			cb.registerAttendeeEnterOrExitListener(mVideoHandler,
					ATTENDEE_ENTER_OR_EXIT_LISTNER, null);
			cb.registerKickedConfListener(mVideoHandler, NOTIFICATION_KICKED,
					null);
			cb.registerSyncStateListener(mVideoHandler,
					SYNC_STATE_NOTIFICATION, null);
			cb.registerInvitationStateListener(mVideoHandler,
					INVITATION_STATE_NOTIFICATION, null);
			cb.registerVoiceActivationListener(mVideoHandler,
					VOICEACTIVATION_NOTIFICATION, null);
			cb.registerVideoMixerListener(mVideoHandler,
					VIDEO_MIX_NOTIFICATION, null);

			cb.registerLectureRequestListener(mVideoHandler,
					NOTIFY_HOST_PERMISSION_REQUESTED, null);

			cb.registerAttendeeDeviceListener(mVideoHandler,
					ATTENDEE_DEVICE_LISTENER, null);

			// Register listen to document notification
			ds.registerNewDocNotification(mVideoHandler, NEW_DOC_NOTIFICATION,
					null);
			ds.registerDocPageListNotification(mVideoHandler,
					DOC_PAGE_LIST_NOTIFICATION, null);
			ds.registerDocDisplayNotification(mVideoHandler,
					DOC_DOWNLOADE_COMPLETE_ONE_PAGE_NOTIFICATION, null);

			ds.registerdocPageActiveNotification(mVideoHandler,
					DOC_TURN_PAGE_NOTIFICATION, null);
			ds.registerDocClosedNotification(mVideoHandler,
					DOC_CLOSED_NOTIFICATION, null);

			ds.registerDocPageAddedNotification(mVideoHandler,
					DOC_ADDED_ONE_PAGE_NOTIFICATION, null);
			ds.registerPageCanvasUpdateNotification(mVideoHandler,
					DOC_PAGE_CANVAS_NOTIFICATION, null);

			Message.obtain(mVideoHandler, ONLY_SHOW_LOCAL_VIDEO).sendToTarget();
			cb.notifyAllMessage(cg.getmGId());
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

			if (isSyn) {
				Toast.makeText(getApplicationContext(), "主席正在同步视频",
						Toast.LENGTH_LONG).show();
				Log.i("20141220 3", "主席正在同步视频"
						+ Thread.currentThread().getName());
				return;
			}

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
			// If current user is host
			boolean isLecturer = currentAttendee.getLectureState() == Attendee.LECTURE_STATE_GRANTED;
			ds.switchDoc(currentAttendee.getAttId(), doc, isLecturer, null);

		}

		@Override
		public void requestShareImageDoc(View v) {
			isMoveTaskBack = false;
			Intent intent = new Intent(mContext, ConversationSelectImage.class);
			startActivityForResult(intent, SUB_ACTIVITY_CODE_SHARE_DOC);
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
				GroupUserObject obj = (GroupUserObject) msg.obj;
				Attendee removed = null;
				Iterator<Attendee> iterator = mAttendeeList.iterator();
				while (iterator.hasNext()) {
					Attendee attendee = iterator.next();
					if (attendee.getAttId() == obj.getmUserId()) {
						removed = attendee;
						break;
					}
				}

				if (removed != null) {
					mAttendeeList.remove(removed);
					if (mAttendeeContainer != null) {
						mAttendeeContainer.removeAttendee(removed);
					}
				}
				// Attendee a = new Attendee(GlobalHolder.getInstance().getUser(
				// obj.getmUserId()));
			}
				break;
			case GROUP_ADD_USER:
				GroupUserObject ro1 = (GroupUserObject) msg.obj;
				if (ro1.getmType() == V2GlobalEnum.GROUP_TYPE_CONFERENCE) { // CONFGROUP

					boolean contain = false;
					for (Attendee attendee : mAttendeeList) {
						if (ro1.getmUserId() == attendee.getAttId()) {
							contain = true;
							break;
						}
					}

					if (contain) {
						return;
					}

					Attendee a1 = new Attendee(GlobalHolder.getInstance()
							.getUser(ro1.getmUserId()));
					mAttendeeList.add(a1);

					if (mAttendeeContainer != null) {
						mAttendeeContainer.addNewAttendee(a1);
					}
				}
				break;
			case ATTENDEE_DEVICE_LISTENER: {

				V2Log.d(V2Log.UI_MESSAGE,
						"CLASS = ConferenceActivity  MESSAGE = ATTENDEE_DEVICE_LISTENER");
				Log.i("20141220 1",
						"CLASS = ConferenceActivity  MESSAGE = ATTENDEE_DEVICE_LISTENER");
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
			case ATTENDEE_ENTER_OR_EXIT_LISTNER:
				User ut = (User) (((AsyncResult) msg.obj).getResult());
				Attendee at = findAttendee(ut.getmUserId());
				if (msg.arg1 == 1) {
					// for non-register user construct temp attendee
					if (at == null) {
						at = new Attendee(ut);
						mAttendeeList.add(at);
						mFastAttendeeList.add(at);
					} else {

						if (TextUtils.isEmpty(at.getAttName())) {
							User user = GlobalHolder.getInstance().getUser(
									at.getAttId());
							if (user != null)
								at.setUser(user);
							else
								V2Log.d(TAG,
										"Successful receiver the 参会人加入的回调 , but get newst user object "
												+ "from GlobleHolder is null!");
						}

					}

					V2Log.d(TAG, "Successful receiver the 参会人加入的回调");
					doHandleNewUserEntered(at);
				} else {
					V2Log.d(TAG, "Successful receiver the 参会人退出的回调");
					if (mFastAttendeeList.contains(at)) {
						at.isRmovedFromList = true;
						mFastAttendeeList.remove(at);
						mAttendeeList.remove(at);
					}
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
				finish();
			}
				break;

			case NOTIFY_HOST_PERMISSION_REQUESTED:
				PermissionRequestIndication rri = (PermissionRequestIndication) (((AsyncResult) msg.obj)
						.getResult());
				if (rri.getUid() != GlobalHolder.getInstance()
						.getCurrentUserId()) {
					mHostRequestUsers.add(GlobalHolder.getInstance().getUser(
							rri.getUid()));
					if (mHostRequestWindow == null
							|| !mHostRequestWindow.isShowing()) {
						mMsgNotification.setVisibility(View.VISIBLE);
						if (mConfMsgRedDot != null) {
							mConfMsgRedDot.setVisibility(View.VISIBLE);
						}

						hasUnreadChiremanControllMsg = true;
					}

					PermissionUpdateIndication pui = new PermissionUpdateIndication(
							rri.getUid(), rri.getType(), rri.getState());

					updateAttendeePermissionState(pui);

					if (moreWindow != null && moreWindow.isShowing()) {
						updateMoreWindowDisplay();
					}
				}

				if (mHostRequestWindow != null) {
					mHostRequestWindow.updateList(mHostRequestUsers);
				}

				break;
			// user permission updated
			case NOTIFY_USER_PERMISSION_UPDATED:
				PermissionUpdateIndication ind = (PermissionUpdateIndication) (((AsyncResult) msg.obj)
						.getResult());

				if (ind.getType() == ConferencePermission.CONTROL.intValue()) {
					if (ind.getState() == PermissionState.GRANTED.intValue()) {
					} else if (ind.getState() == PermissionState.APPLYING
							.intValue()) {
					} else if (ind.getState() == PermissionState.NORMAL
							.intValue()) {
						// 取消申请
						User user = null;
						for (User tempUser : mHostRequestUsers) {
							if (tempUser.getmUserId() == ind.getUid()) {
								user = tempUser;
								break;
							}
						}

						if (user != null) {
							mHostRequestUsers.remove(user);
						}

						if (mHostRequestWindow != null) {
							mHostRequestWindow.updateList(mHostRequestUsers);
						}

						if (mHostRequestUsers.size() == 0) {

							if (mHostRequestWindow != null
									&& mHostRequestWindow.isShowing()) {
								mHostRequestWindow.dismiss();
							}

							hasUnreadChiremanControllMsg = false;
							mMsgNotification.setVisibility(View.GONE);
							if (mConfMsgRedDot != null) {
								mConfMsgRedDot.setVisibility(View.GONE);
							}
						}

					}
				}

				// 更新自己的图标显示
				if (ind.getUid() == GlobalHolder.getInstance()
						.getCurrentUserId()) {
					if (ConferencePermission.CONTROL.intValue() == ind
							.getType()) {
						// 显示提示信息
						showCurrentAttendeeLectureStateToast(
								PermissionState.fromInt(ind.getState()), true);

					} else if (ConferencePermission.SPEAKING.intValue() == ind
							.getType()) {
						// 更新自己的发言图标
						updateSpeakerState(PermissionState.fromInt(ind
								.getState()) == PermissionState.GRANTED
								&& ConferencePermission.SPEAKING.intValue() == ind
										.getType());
					}
				}

				// 更新参会人列表里的主讲和发言状态
				if (!updateAttendeePermissionState(ind)
						&& mPendingPermissionUpdateList != null) {
					mPendingPermissionUpdateList.add(ind);
				}

				if (ind.getUid() == GlobalHolder.getInstance()
						.getCurrentUserId()) {
					if (ConferencePermission.CONTROL.intValue() == ind
							.getType()) {
						if (moreWindow != null && moreWindow.isShowing()) {
							updateMoreWindowDisplay();
						}

						if (mDocContainer != null) {
							if (currentAttendee.getLectureState() == Attendee.LECTURE_STATE_GRANTED) {
								mDocContainer.requestShowSharedButton(true);
							} else {
								mDocContainer.requestShowSharedButton(false);
							}
							mDocContainer
									.updateSyncStatus(isSyn
											&& (currentAttendee
													.getLectureState() != Attendee.LECTURE_STATE_GRANTED));
							mDocContainer.updateCurrentDoc();
						}
					}
				}

				break;
			case NEW_DOC_NOTIFICATION:
			case DOC_PAGE_LIST_NOTIFICATION:
			case DOC_ADDED_ONE_PAGE_NOTIFICATION:
			case DOC_TURN_PAGE_NOTIFICATION:
			case DOC_DOWNLOADE_COMPLETE_ONE_PAGE_NOTIFICATION:
			case DOC_CLOSED_NOTIFICATION:
			case DOC_PAGE_CANVAS_NOTIFICATION:
				handleDocNotification((AsyncResult) (msg.obj), msg.what);
				break;
			case INVITATION_STATE_NOTIFICATION:
				// 20141225 1
				canInvitation = msg.arg1 == 1 ? true : false;
				cg.setCanInvitation(canInvitation);

				if (currentAttendee.isChairMan()) {
					return;
				}

				if (!cg.isCanInvitation()) {
					View view = initInvitionContainer();
					if (mSubWindowLayout.getVisibility() == View.VISIBLE
							&& mSubWindowLayout.getChildAt(0) == view
							&& conf.getCreator() != GlobalHolder.getInstance()
									.getCurrentUserId()) {
						// close invitation view, remote forbbien invitation
						showOrHideSubWindow(view);
						Toast.makeText(mContext,
								R.string.error_no_permission_to_invitation,
								Toast.LENGTH_SHORT).show();
					}
					if (mMenuInviteAttendeeButton != null) {
						mMenuInviteAttendeeButton.setEnabled(false);
						((ImageView) mMenuInviteAttendeeButton)
								.setImageResource(R.drawable.video_menu_invite_attendee_button_disenable);
					}
				} else {
					if (mMenuInviteAttendeeButton != null) {
						mMenuInviteAttendeeButton.setEnabled(true);
						((ImageView) mMenuInviteAttendeeButton)
								.setImageResource(R.drawable.video_menu_invite_attendee_button);
					}
				}

				break;
			case SYNC_STATE_NOTIFICATION:
				V2Log.d(V2Log.UI_MESSAGE,
						"CLASS = ConferenceActivity  MESSAGE = DESKTOP_SYNC_NOTIFICATION");

				isSyn = msg.arg1 == 1 ? true : false;

				if (isSyn) {
					currentAttendeeTurnedpage = false;
				}
				if (mDocContainer != null) {

					mDocContainer
							.updateSyncStatus(isSyn
									&& (currentAttendee.getLectureState() != Attendee.LECTURE_STATE_GRANTED));
					mDocContainer.updateCurrentDoc();

				}

				cg.setSyn(isSyn);

				if (isSyn) {
					// 关闭所有打开的视频
					Object[] surfaceViewWArray = mCurrentShowedSV.toArray();
					for (int i = 0; i < surfaceViewWArray.length; i++) {
						closeAttendeeVideo(((SurfaceViewW) surfaceViewWArray[i]).udc);
					}
				}

				break;

			case VOICEACTIVATION_NOTIFICATION:
				// 语音激励开启与关闭
				V2Log.d(V2Log.UI_MESSAGE,
						"CLASS = ConferenceActivity  MESSAGE = VOICEACTIVATION_NOTIFICATION");
				isVoiceActivation = msg.arg1 == 1 ? true : false;

				if (!isVoiceActivation) {
					// 关闭所有打开的视频
					Object[] surfaceViewWArray = mCurrentShowedSV.toArray();
					for (int i = 0; i < surfaceViewWArray.length; i++) {
						closeAttendeeVideo(((SurfaceViewW) surfaceViewWArray[i]).udc);
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

	private void headsetAndBluetoothHeadsetHandle() {

		if (audioManager.isWiredHeadsetOn()) {
			audioManager.setSpeakerphoneOn(false);
			Log.i(TAG, "切换到了有线耳机");
		} else if (isBluetoothHeadsetConnected
				&& !audioManager.isBluetoothA2dpOn()) {
			// try {
			// Thread.sleep(500);
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
			audioManager.setSpeakerphoneOn(false);
			audioManager.startBluetoothSco();
			audioManager.setBluetoothScoOn(true);
			Log.i(TAG, "切换到SCO链路蓝牙耳机");
		} else if (isBluetoothHeadsetConnected
				&& audioManager.isBluetoothA2dpOn()) {
			audioManager.setSpeakerphoneOn(false);
			Log.i(TAG, "切换到了ACL链路的A2DP蓝牙耳机");
		} else {
			audioManager.setSpeakerphoneOn(true);
			Log.i(TAG, "切换到了外放");
		}

	}

	public boolean isCurrentAttendeeTurnedpage() {
		return currentAttendeeTurnedpage;
	}

	public void setCurrentAttendeeTurnedpage(boolean currentAttendeeTurnedpage) {
		this.currentAttendeeTurnedpage = currentAttendeeTurnedpage;
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
