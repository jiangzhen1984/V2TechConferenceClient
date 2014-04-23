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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.logic.Attendee;
import com.v2tech.logic.CameraConfiguration;
import com.v2tech.logic.Conference;
import com.v2tech.logic.ConferencePermission;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.Group;
import com.v2tech.logic.Group.GroupType;
import com.v2tech.logic.Registrant;
import com.v2tech.logic.User;
import com.v2tech.logic.UserDeviceConfig;
import com.v2tech.logic.VMessage;
import com.v2tech.logic.jni.JNIResponse;
import com.v2tech.logic.jni.RequestEnterConfResponse;
import com.v2tech.service.ChatService;
import com.v2tech.service.ConferenceService;
import com.v2tech.service.DocumentService;
import com.v2tech.util.V2Log;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.conference.VideoMsgChattingLayout.ChattingListener;
import com.v2tech.view.vo.V2Doc;
import com.v2tech.view.vo.V2Doc.Page;

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

	private static final int ATTENDEE_LISTENER = 21;
	private static final int CONF_USER_DEVICE_EVENT = 23;

	private static final int NEW_DOC_NOTIFICATION = 50;
	private static final int DOC_PAGE_NOTIFICATION = 51;
	private static final int DOC_PAGE_ACTIVITE_NOTIFICATION = 52;
	private static final int DOC_PAGE_ADDED_NOTIFICATION = 53;
	private static final int DOC_DOWNLOADED_NOTIFICATION = 54;
	private static final int DOC_CLOSED_NOTIFICATION = 55;

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
	
	private SurfaceView mLocalSurface;

	private Conference conf;

	private ConferenceService cb = new ConferenceService();

	private ChatService cs = new ChatService();

	private Long mGroupId;
	private Set<Attendee> mAttendeeList = new HashSet<Attendee>();
	private DocumentService ds = new DocumentService();

	private Map<String, V2Doc> mDocs = new HashMap<String, V2Doc>();
	private V2Doc mCurrentActivateDoc = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(R.layout.activity_in_metting);
		mContext = this;
		this.mVideoLayoutMain = (RelativeLayout) findViewById(R.id.video_layout_root);
		this.mCurrentShowedSV = new ArrayList<SurfaceViewW>();
		this.mVideoLayout = (RelativeLayout) findViewById(R.id.in_metting_video_main);
		this.mSettingIV = (ImageView) findViewById(R.id.in_meeting_setting_iv);
		this.mSettingIV.setOnClickListener(mShowSettingListener);
		// this.mListUserIV = (ImageView)
		// findViewById(R.id.in_meeting_show_attendee_iv);
		// this.mListUserIV.setOnClickListener(mShowConfUsersListener);
		this.mQuitIV = (ImageView) findViewById(R.id.in_meeting_log_out_iv);
		this.mQuitIV.setOnClickListener(mShowQuitWindowListener);
		this.mSpeakerIV = (ImageView) findViewById(R.id.speaker_iv);
		this.mSpeakerIV.setOnClickListener(mApplySpeakerListener);
		this.mGroupNameTV = (TextView) findViewById(R.id.in_meeting_name);
		this.mMenuLine = (LinearLayout) findViewById(R.id.in_meeting_menu_separation_line);
		this.mVideoLine = (LinearLayout) findViewById(R.id.in_meeting_video_separation_line1);
		this.mLocalSurface =(SurfaceView) findViewById(R.id.local_surface_view);

		mMenuButton = (ImageView) findViewById(R.id.in_meeting_menu_button);
		mMenuButton.setOnClickListener(mMenuButtonListener);
		mMenuButtonContainer = (LinearLayout) findViewById(R.id.in_meeting_menu_layout);

		mMenuMessageButton = findViewById(R.id.in_meeting_menu_show_msg_button);
		mMenuMessageButton.setTag("msg");
		mMenuMessageButton.setOnClickListener(mMenuShowButtonListener);

		mMenuAttendeeButton = findViewById(R.id.in_meeting_menu_show_attendees_button);
		mMenuAttendeeButton.setTag("attendee");
		mMenuAttendeeButton.setOnClickListener(mMenuShowButtonListener);

		mMenuDocButton = findViewById(R.id.in_meeting_menu_show_doc_button);
		mMenuDocButton.setTag("doc");
		mMenuDocButton.setOnClickListener(mMenuShowButtonListener);

		// mAttendeeArrowIV.setVisibility(View.INVISIBLE);
		initConfsListener();
		init();
		registerOrRemoveListener(true);

		// initMenuFeature();

		// listen callback
		ds.registerNewDocNotification(mVideoHandler, NEW_DOC_NOTIFICATION, null);
		ds.registerDocDisplayNotification(mVideoHandler,
				DOC_DOWNLOADED_NOTIFICATION, null);
		ds.registerdocPageActiveNotification(mVideoHandler,
				DOC_PAGE_ACTIVITE_NOTIFICATION, null);
		ds.registerDocPageNotification(mVideoHandler, DOC_PAGE_NOTIFICATION,
				null);
		ds.registerDocClosedNotification(mVideoHandler,
				DOC_CLOSED_NOTIFICATION, null);
		ds.registerDocPageAddedNotification(mVideoHandler, DOC_PAGE_ADDED_NOTIFICATION, null);
	}

	private OnClickListener mMenuButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (mMenuButtonContainer.getVisibility() == View.GONE) {
				Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
						mContext, R.animator.right_in);
				tabBlockHolderAnimation.setDuration(1000);
				mMenuButtonContainer.startAnimation(tabBlockHolderAnimation);
				mMenuButtonContainer.setVisibility(View.VISIBLE);
				
			} else {
				
				showOrHidenAttendeeContainer(View.GONE);
				showOrHidenMsgContainer(View.GONE);
				showOrHidenDocContainer(View.GONE);
				Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
						mContext, R.animator.left_out);
				tabBlockHolderAnimation.setDuration(1000);
				mMenuButtonContainer.startAnimation(tabBlockHolderAnimation);
				mMenuButtonContainer.setVisibility(View.GONE);
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
			}
		}

	};

	@Override
	protected void onStart() {
		super.onStart();
		requestEnterConf();
		// adjustLayout();
		Message.obtain(mVideoHandler, ONLY_SHOW_LOCAL_VIDEO).sendToTarget();
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.cancel(PublicIntent.VIDEO_NOTIFICATION_ID);
	}

	private void initConfsListener() {
		IntentFilter filter = new IntentFilter();
		filter.addCategory(JNI_EVENT_VIDEO_CATEGORY);
		filter.addAction(JNI_EVENT_VIDEO_CATEGORY_OPEN_VIDEO_EVENT_ACTION);
		filter.addAction(JNIService.JNI_BROADCAST_NEW_CONF_MESSAGE);
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		mContext.registerReceiver(mConfUserChangeReceiver, filter);

		cb.registerAttendeeListener(this.mVideoHandler, ATTENDEE_LISTENER, null);
	}

	private void registerOrRemoveListener(boolean flag) {
		if (flag) {
			cb.registerKickedConfListener(mVideoHandler, NOTIFICATION_KICKED,
					null);
		} else {
			cb.removeRegisterOfKickedConfListener(mVideoHandler,
					NOTIFICATION_KICKED, null);
		}
	}

	private void init() {
		mGroupId = this.getIntent().getLongExtra("gid", 0);
		if (mGroupId <= 0) {
			Toast.makeText(this, R.string.error_in_meeting_invalid_gid,
					Toast.LENGTH_LONG).show();
			return;
		}

		conf = new Conference(mGroupId);
		Group g = GlobalHolder.getInstance().getGroupById(GroupType.CONFERENCE,
				mGroupId);
		mGroupNameTV.setText(g.getName());
	}

	private void showOrHidenMsgContainer(int visible) {
		if (mMessageContainer == null) {
			mMessageContainer = new VideoMsgChattingLayout(this);
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
					400, mVideoLayout.getMeasuredHeight());
			lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			lp.addRule(RelativeLayout.RIGHT_OF, mMenuLine.getId());
			lp.addRule(RelativeLayout.BELOW, mVideoLine.getId());
			mVideoLayoutMain.addView(mMessageContainer, lp);
			mMessageContainer.bringToFront();
			mMessageContainer.setVisibility(View.GONE);
			mMessageContainer.setListener(new ChattingListener() {

				@Override
				public void requestSendMsg(VMessage vm) {
					vm.mGroupId = mGroupId;
					vm.setToUser(new User(0));
					vm.setUser(GlobalHolder.getInstance().getCurrentUser());
					vm.setMsgCode(VMessage.VMESSAGE_CODE_CONF);
					cs.sendVMessage(vm, null);
				}

			});
		}
		if (visible == View.GONE && View.GONE == mMessageContainer.getVisibility()) {
			return;
		}
		
		if (visible == View.GONE
				|| visible == mMessageContainer.getVisibility()) {
			mMessageContainer.setVisibility(View.GONE);
			mMenuMessageButton.setBackgroundColor(Color.rgb(255, 255, 255));

			Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
					this, R.animator.right_out);
			tabBlockHolderAnimation.setDuration(1000);
			mMessageContainer.startAnimation(tabBlockHolderAnimation);

		} else {
			mMessageContainer.setVisibility(View.VISIBLE);
			mMenuMessageButton.setBackgroundColor(mContext.getResources()
					.getColor(R.color.confs_common_bg));

			Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
					this, R.animator.left_in);
			tabBlockHolderAnimation.setDuration(1000);
			mMessageContainer.startAnimation(tabBlockHolderAnimation);
			mMessageContainer.requestScrollToNewMessage();
			
		}
	}

	private void showOrHidenAttendeeContainer(int visible) {
		if (mAttendeeContainer == null) {
			mAttendeeContainer = new VideoAttendeeListLayout(this);
			Group conf = GlobalHolder.getInstance().getGroupById(
					Group.GroupType.CONFERENCE, this.mGroupId);
			List<User> l = new ArrayList<User>(conf.getUsers());
			Set<Attendee> al = new HashSet<Attendee>();

			for (Attendee t : this.mAttendeeList) {
				al.add(t);
			}

			for (User u : l) {
				al.add(new Attendee(u));
			}
			mAttendeeContainer.setAttendsList(al);
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
					400, mVideoLayout.getMeasuredHeight());
			lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			lp.addRule(RelativeLayout.RIGHT_OF, mMenuLine.getId());
			lp.addRule(RelativeLayout.BELOW, mVideoLine.getId());
			mVideoLayoutMain.addView(mAttendeeContainer, lp);
			mAttendeeContainer.bringToFront();
			mAttendeeContainer.setVisibility(View.GONE);
			mAttendeeContainer
					.setListener(new VideoAttendeeListLayout.VideoAttendeeActionListener() {

						@Override
						public void OnAttendeeClicked(Attendee at,
								UserDeviceConfig udc) {
							if (udc == null || at == null) {
								return;
							}
							if (at.getUser().getmUserId() == GlobalHolder.getInstance().getCurrentUserId()) {
								
							} else {
								showOrCloseAttendeeVideo(udc);
							}
						}

					});
		}
		
		//View is hidded, do not need to hide again
		if (visible == View.GONE && View.GONE == mAttendeeContainer.getVisibility()) {
			return;
		}
		if (visible == View.GONE
				|| visible == mAttendeeContainer.getVisibility()) {
			mAttendeeContainer.setVisibility(View.GONE);
			mMenuAttendeeButton.setBackgroundColor(Color.rgb(255, 255, 255));

			Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
					this, R.animator.right_out);
			tabBlockHolderAnimation.setDuration(1000);
			mAttendeeContainer.startAnimation(tabBlockHolderAnimation);
		} else {
			mAttendeeContainer.setVisibility(View.VISIBLE);
			mMenuAttendeeButton.setBackgroundColor(mContext.getResources()
					.getColor(R.color.confs_common_bg));
			// set animation when visible
			Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
					this, R.animator.left_in);
			tabBlockHolderAnimation.setDuration(1000);
			mAttendeeContainer.startAnimation(tabBlockHolderAnimation);
		}
	}

	private void showOrHidenDocContainer(int visible) {
		if (mDocContainer == null) {
			mDocContainer = new VideoDocLayout(this);

			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
					800, mVideoLayout.getMeasuredHeight());
			lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			lp.addRule(RelativeLayout.RIGHT_OF, mMenuLine.getId());
			lp.addRule(RelativeLayout.BELOW, mVideoLine.getId());
			mVideoLayoutMain.addView(mDocContainer, lp);

			mDocContainer.bringToFront();
			mDocContainer.setVisibility(View.GONE);
			synchronized (mDocs) {
				for (Entry<String, V2Doc> e : mDocs.entrySet()) {
					mDocContainer.addDoc(e.getValue());
				}
			}
			mDocContainer.updateCurrentDoc(mCurrentActivateDoc);

		}
		//View is hidded, do not need to hide again
		if (visible == View.GONE && View.GONE == mDocContainer.getVisibility()) {
			return;
		}
				
		if (visible == View.GONE || visible == mDocContainer.getVisibility()) {
			mDocContainer.setVisibility(View.GONE);
			mMenuDocButton.setBackgroundColor(Color.rgb(255, 255, 255));

			Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
					this, R.animator.right_out);
			tabBlockHolderAnimation.setDuration(1000);
			mDocContainer.startAnimation(tabBlockHolderAnimation);
		} else {
			mDocContainer.setVisibility(View.VISIBLE);
			mMenuDocButton.setBackgroundColor(mContext.getResources().getColor(
					R.color.confs_common_bg));

			// set animation when visible
			Animation tabBlockHolderAnimation = AnimationUtils.loadAnimation(
					this, R.animator.left_in);
			tabBlockHolderAnimation.setDuration(1000);
			mDocContainer.startAnimation(tabBlockHolderAnimation);
		}
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

		if (selfInAttendeeList == false) {
			atd = new Attendee(GlobalHolder.getInstance().getCurrentUser(),
					true, false);
			mAttendeeList.add(atd);
		}

		udc = atd.getDefaultDevice();
		// Add default device
		if (udc == null) {
			udc = new UserDeviceConfig(atd.getUser().getmUserId(), "", null);
			atd.addDevice(udc);
		}

		// layout must before open device
		//showOrCloseAttendeeVideo(udc);
		
		udc.setSVHolder(mLocalSurface);
		VideoRecorder.VideoPreviewSurfaceHolder = udc.getSVHolder()
				.getHolder();
		VideoRecorder.VideoPreviewSurfaceHolder
				.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		VideoRecorder.VideoPreviewSurfaceHolder
				.setFormat(PixelFormat.TRANSPARENT);
		VideoCaptureDevInfo.CreateVideoCaptureDevInfo()
				.updateCameraOrientation(Surface.ROTATION_0);
		Message m = Message.obtain(mVideoHandler,
				REQUEST_OPEN_OR_CLOSE_DEVICE, 1, 0, udc);
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
		int containerW = outR.right - outR.left;
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
		mVideoLayout.removeAllViews();
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
		registerOrRemoveListener(false);
		cb.removeAttendeeListener(this.mVideoHandler, ATTENDEE_LISTENER, null);
		ds.unRegisterNewDocNotification(mVideoHandler, NEW_DOC_NOTIFICATION,
				null);
		ds.unRegisterDocDisplayNotification(mVideoHandler,
				DOC_DOWNLOADED_NOTIFICATION, null);
		ds.unRegisterDocClosedNotification(mVideoHandler,
				DOC_CLOSED_NOTIFICATION, null);
		ds.unRegisterDocPageAddedNotification(mVideoHandler, DOC_PAGE_ADDED_NOTIFICATION, null);
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

		for (SurfaceViewW sw : this.mCurrentShowedSV) {
			if (sw.udc.getBelongsAttendee().isSelf()) {

				VideoCaptureDevInfo.CreateVideoCaptureDevInfo().reverseCamera();

				cb.updateCameraParameters(new CameraConfiguration(""), null);
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
		Attendee a = new Attendee(user);
		this.mAttendeeList.add(a);
		List<UserDeviceConfig> ld = GlobalHolder.getInstance()
				.getAttendeeDevice(a.getUser().getmUserId());
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
			this.mAttendeeList.remove(a);
		}
		if (mAttendeeContainer != null) {
			mAttendeeContainer.updateExitedAttendee(a);
		}

	}

	private boolean checkVideoExceedMaminum() {
		return mCurrentShowedSV.size() >= 4;
	}

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
				VideoCaptureDevInfo.CreateVideoCaptureDevInfo()
						.updateCameraOrientation(Surface.ROTATION_0);
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
				// FIXME make sure hash code is unique.
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
			case NOTIFICATION_KICKED:
				Toast.makeText(mContext,
						R.string.conversations_kick_notification,
						Toast.LENGTH_LONG).show();
				quit();
				finish();
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

					// FIXME ?????
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
						mDocContainer.closeDoc(removedDoc);
						mDocs.remove(removedDoc.getId());
					}
				}
				break;

			}
		}

	}

}
