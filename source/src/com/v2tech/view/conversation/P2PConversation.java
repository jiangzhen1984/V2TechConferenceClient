package com.v2tech.view.conversation;

import java.util.List;
import java.util.UUID;

import v2av.VideoCaptureDevInfo;
import v2av.VideoCaptureDevInfo.VideoCaptureDevice;
import v2av.VideoPlayer;
import v2av.VideoRecorder;
import v2av.VideoSize;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.ChatService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestChatServiceResponse;
import com.v2tech.util.GlobalConfig;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.vo.AudioVideoMessageBean;
import com.v2tech.vo.CameraConfiguration;
import com.v2tech.vo.NetworkStateCode;
import com.v2tech.vo.User;
import com.v2tech.vo.UserChattingObject;
import com.v2tech.vo.UserDeviceConfig;
import com.v2tech.vo.VideoBean;

public class P2PConversation extends Activity implements
		VideoConversationListener {

	public static final String TAG = "P2PConversation";
	private static final int UPDATE_TIME = 1;
	private static final int OPEN_REMOTE_VIDEO = 2;
	private static final int QUIT = 3;
	private static final int HANG_UP_NOTIFICATION = 4;
	private static final int CALL_RESPONSE = 5;
	private static final int VIDEO_CONECTED = 6;

	private static final int HAND_UP_REASON_REMOTE_REJECT = 1;
	private static final int HAND_UP_REASON_NO_NETWORK = 2;

	private static final String SURFACE_HOLDER_TAG_LOCAL = "local";
	private static final String SURFACE_HOLDER_TAG_REMOTE = "remote";

	public static final String P2P_BROADCAST_MEDIA_UPDATE = "com.v2tech.p2p.broadcast.media_update";

	private Context mContext;
	private ChatService chatService = new ChatService();
	private UserChattingObject uad;
	private LocalHandler mLocalHandler = new LocalHandler(
			Looper.getMainLooper());
	private BroadcastReceiver receiver = new LocalReceiver();

	private long mTimeLine = 0;

	private TextView mTimerTV;

	private View mRejectButton;
	private View mAcceptButton;
	private View mAudioOnlyButton;

	// For video conversation
	private View cameraButton;
	private View videoMuteButton;
	private View videoHangUpButton;
	private View mReverseCameraButton;

	// Video call view
	private SurfaceView mLcalSurface;
	private SurfaceView mRemoteSurface;

	// R.id.fragment_conversation_connected_video_local_layout
	private RelativeLayout mLcalSurfaceLayout;

	private boolean mlocalSurfaceCreated = false;

	private MediaPlayer mPlayer;

	private int displayRotation = 0;

	private boolean isOpenedRemote;
	private boolean isStoped;
	private boolean isOpenedLocal;
	private VideoBean currentVideoBean;
	private long startTime;

	private VideoSize defaultCameraCaptureSize = new VideoSize(176, 144);
	private boolean displayWidthIsLonger = false;
	private boolean isRejected;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		displayWidthIsLonger = verifyDisplayWidthIsLonger();
		displayRotation = getDisplayRotation();

		mContext = this;
		startTime = GlobalConfig.getGlobalServerTime();
		uad = buildObject();
		initReceiver();
		currentVideoBean = new VideoBean();
		chatService.registerCancelledListener(mLocalHandler,
				HANG_UP_NOTIFICATION, null);
		chatService.registerVideoChatConnectedListener(mLocalHandler,
				VIDEO_CONECTED, null);
		if (uad.isIncoming()) {
			currentVideoBean.mediaState = AudioVideoMessageBean.STATE_NO_ANSWER_CALL;
			currentVideoBean.readSatate = AudioVideoMessageBean.STATE_UNREAD;
			currentVideoBean.formUserID = uad.getUser().getmUserId();
			currentVideoBean.remoteUserID = uad.getUser().getmUserId();
			currentVideoBean.toUserID = GlobalHolder.getInstance()
					.getCurrentUserId();
			if (uad.isAudioType()) {
				currentVideoBean.mediaChatID = uad.getSzSessionID();
				currentVideoBean.mediaType = AudioVideoMessageBean.TYPE_AUDIO;
				setContentView(R.layout.fragment_conversation_incoming_audio_call);
				mRejectButton = findViewById(R.id.conversation_fragment_voice_reject_button);
				mAcceptButton = findViewById(R.id.conversation_fragment_voice_accept_button);
				TextView tv = (TextView) findViewById(R.id.conversation_fragment_audio_incoming_call_name);
				if (tv != null) {
					tv.setText(uad.getUser().getName());
				}
			} else if (uad.isVideoType()) {
				currentVideoBean.mediaChatID = uad.getSzSessionID();
				currentVideoBean.mediaType = AudioVideoMessageBean.TYPE_VIDEO;
				setContentView(R.layout.fragment_conversation_incoming_video_call);
				mRejectButton = findViewById(R.id.conversation_fragment_video_reject_button);
				mAcceptButton = findViewById(R.id.conversation_fragment_video_accept_button);
				mAudioOnlyButton = findViewById(R.id.conversation_fragment_voice_accept_only_button);
				TextView tv = (TextView) findViewById(R.id.conversation_fragment_video_invitation_name);
				if (tv != null) {
					tv.setText(uad.getUser().getName());
				}
			}
			mRejectButton.setOnClickListener(rejectListener);
			mAcceptButton.setOnClickListener(acceptListener);
			if (mAudioOnlyButton != null) {
				mAudioOnlyButton.setOnClickListener(acceptVoicOnlyListener); 
			}
			// start time out monitor
			mLocalHandler.postDelayed(timeOutMonitor, 1000 * 60);
		} else {
			String uuid = UUID.randomUUID().toString();
			currentVideoBean.mediaState = AudioVideoMessageBean.STATE_ANSWER_CALL;
			currentVideoBean.readSatate = AudioVideoMessageBean.STATE_READED;
			currentVideoBean.formUserID = GlobalHolder.getInstance()
					.getCurrentUserId();
			currentVideoBean.toUserID = uad.getUser().getmUserId();
			currentVideoBean.remoteUserID = uad.getUser().getmUserId();
			if (uad.isAudioType()) {
				currentVideoBean.mediaType = AudioVideoMessageBean.TYPE_AUDIO;
				uad.setSzSessionID("AudioChat" + uuid);
				currentVideoBean.mediaChatID = "AudioChat" + uuid;
				setContentView(R.layout.fragment_conversation_outing_audio);
			} else if (uad.isVideoType()) {
				currentVideoBean.mediaType = AudioVideoMessageBean.TYPE_VIDEO;
				uad.setSzSessionID(uuid);
				currentVideoBean.mediaChatID = uuid;
				setContentView(R.layout.fragment_conversation_outing_video);
			}

		}

		initButtons();

		initViews();

		if (!uad.isIncoming()) {
			chatService.inviteUserChat(uad, new Registrant(mLocalHandler,
					CALL_RESPONSE, null));
			if (uad.isVideoType()) {
				// Update view
				TextView tv = (TextView) findViewById(R.id.conversation_fragment_connected_title_text);
				tv.setText(tv.getText().toString()
						.replace("[]", uad.getUser().getName()));
			} else {
				mTimerTV.setText(R.string.conversation_waiting);
				TextView nameTV = (TextView) findViewById(R.id.conversation_fragment_connected_name);
				nameTV.setText(uad.getUser().getName());
				// Update mute button to disable
				setMuteButtonDisable(true);
			}
			//Start waiting voice
			playRingToneOuting();
		}

		// initialize phone state listener
		initTelephonyManagerListener();
		// Update global state
		setGlobalState(true);

	}

	public boolean verifyDisplayWidthIsLonger() {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		return displayMetrics.widthPixels > displayMetrics.heightPixels;
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

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	public UserChattingObject getObject() {
		if (uad == null) {
			buildObject();
		}
		return uad;
	}

	@Override
	protected void onStart() {
		super.onStart();
		isStoped = false;
		if (uad.isIncoming() && !uad.isConnected()) {
			playRingToneIncoming();
		}
		if (uad.isConnected()) {
			// Resume audio
			chatService.suspendOrResumeAudio(true);
		}
		// keep screen on
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

	}

	@Override
	protected void onStop() {
		super.onStop();
		stopRingTone();
		if (uad.isConnected()) {
			// Resume audio
			chatService.suspendOrResumeAudio(false);
			this.closeRemoteVideo();
		}
		this.closeLocalCamera();
		isStoped = true;
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		setGlobalState(false);
		mContext.unregisterReceiver(receiver);
		chatService.removeRegisterCancelledListener(mLocalHandler,
				HANG_UP_NOTIFICATION, null);

		chatService.removeVideoChatConnectedistener(mLocalHandler,
				VIDEO_CONECTED, null);

		chatService.clearCalledBack();
	}

	@Override
	public void openLocalCamera() {
		if (isStoped) {
			V2Log.w(" Do not open remote in stopping in Local");
			return;
		}
		if (isOpenedLocal) {
			V2Log.w("Local camera is opened");
			return;
		}
		isOpenedLocal = true;
		V2Log.e("open local holder"
				+ getSurfaceHolder(SURFACE_HOLDER_TAG_LOCAL));
		VideoRecorder.VideoPreviewSurfaceHolder = getSurfaceHolder(SURFACE_HOLDER_TAG_LOCAL);
		VideoRecorder.DisplayRotation = displayRotation;
		VideoCaptureDevInfo.CreateVideoCaptureDevInfo()
				.SetCapParams(defaultCameraCaptureSize.width,
						defaultCameraCaptureSize.height);
		UserChattingObject selfUCD = new UserChattingObject(GlobalHolder
				.getInstance().getCurrentUser(), 0, "");
		chatService.requestOpenVideoDevice(selfUCD.getUdc(), null);
	}

	@Override
	public void reverseLocalCamera() {
		closeLocalCamera();
		boolean flag = VideoCaptureDevInfo.CreateVideoCaptureDevInfo()
				.reverseCamera();
		if (flag) {
			chatService.updateCameraParameters(new CameraConfiguration(""),
					null);
		}
		openLocalCamera();
	}

	public VideoSize getCurrentCameraCaptureSureSize() {
		VideoCaptureDevice cameraDevice = VideoCaptureDevInfo
				.CreateVideoCaptureDevInfo().GetCurrDevice();
		return cameraDevice.GetSrcSizeByEncSize(defaultCameraCaptureSize.width,
				defaultCameraCaptureSize.height);

	}

	@Override
	public void closeLocalCamera() {
		UserChattingObject selfUCD = new UserChattingObject(GlobalHolder
				.getInstance().getCurrentUser(), 0, "");
		chatService.requestCloseVideoDevice(selfUCD.getUdc(), null);
		isOpenedLocal = false;
	}

	@Override
	public void onBackPressed() {
		showConfirmDialog();
	}

	private void initTelephonyManagerListener() {
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(new PhoneStateListener() {

			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				if (state == TelephonyManager.CALL_STATE_OFFHOOK
						|| state == TelephonyManager.CALL_STATE_RINGING) {
					V2Log.e(TAG, "the onCallStateChanged was called ....");
					hangUp();
				}
			}

		}, PhoneStateListener.LISTEN_CALL_STATE);
	}

	private SurfaceHolder getSurfaceHolder(String type) {
		SurfaceHolder localHolder = null;
		if (mLcalSurface.getTag().equals(type)) {
			localHolder = mLcalSurface.getHolder();
		} else {
			localHolder = mRemoteSurface.getHolder();
		}
		return localHolder;

	}

	

	private void playRingToneOuting() {
		mPlayer = MediaPlayer.create(mContext, R.raw.outing_ring_tone_1);
		mPlayer.setLooping(true);
		mPlayer.start();
	}
	
	private void  stopRingToneOuting() {
		if (mPlayer != null) {
			mPlayer.release();
		}
	}
	
	private void playRingToneIncoming() {
		mPlayer = MediaPlayer.create(mContext,
				RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
		mPlayer.start();
	}

	private void stopRingTone() {
		if (mPlayer != null) {
			mPlayer.release();
		}
	}

	private void setGlobalState(boolean flag) {
		long uid = flag ? uad.getUser().getmUserId() : 0;
		if (uad.isAudioType()) {
			GlobalHolder.getInstance().setAudioState(flag, uid);
		} else if (uad.isVideoType()) {
			GlobalHolder.getInstance().setVideoState(flag, uid);
			// If clear video state, we must clear voice connect state too.
			if (!flag) {
				GlobalHolder.getInstance().setVoiceConnectedState(false);
			}
		}
	}

	private void initButtons() {
		// For video button
		cameraButton = findViewById(R.id.conversation_fragment_connected_video_camera_button);
		if (cameraButton != null) {
			cameraButton.setOnClickListener(mCloseOrOpenCameraButtonListener);
		}
		videoHangUpButton = findViewById(R.id.conversation_fragment_connected_video_hang_up_button);
		if (videoHangUpButton != null) {
			videoHangUpButton.setOnClickListener(mHangUpButtonListener);
		}

		videoMuteButton = findViewById(R.id.conversation_fragment_connected_video_mute_button);
		if (videoMuteButton != null) {
			videoMuteButton.setOnClickListener(mMuteButtonListener);
		}

		// For audio Button
		View audioSpeakerButton = findViewById(R.id.conversation_fragment_connected_speaker_button);
		if (audioSpeakerButton != null) {
			audioSpeakerButton.setOnClickListener(mAudioSpeakerButtonListener);
		}
		View audioHangUpButton = findViewById(R.id.conversation_fragment_connected_hang_up_button);
		if (audioHangUpButton != null) {
			audioHangUpButton.setOnClickListener(mHangUpButtonListener);
		}

		View audioMuteButton = findViewById(R.id.conversation_fragment_connected_audio_mute_button);
		if (audioMuteButton != null) {
			audioMuteButton.setOnClickListener(mMuteButtonListener);
		}
	}

	private void setMuteButtonDisable(boolean flag) {
		View audioMuteButton = findViewById(R.id.conversation_fragment_connected_audio_mute_button);
		TextView audioMuteButtonText = (TextView) findViewById(R.id.conversation_fragment_connected_audio_mute_text);
		ImageView audioMuteButtonImage = (ImageView) findViewById(R.id.conversation_fragment_connected_audio_mute_image);

		if (flag) {
			if (audioMuteButton != null) {
				audioMuteButton.setEnabled(false);
				audioMuteButton
						.setBackgroundResource(R.drawable.conversation_framgent_gray_button_bg_pressed);
			}

			if (audioMuteButtonText != null) {
				audioMuteButtonText
						.setTextColor(mContext
								.getResources()
								.getColor(
										R.color.fragment_conversation_disable_text_color));
			}

			if (audioMuteButtonImage != null) {
				audioMuteButtonImage
						.setImageResource(R.drawable.conversation_connected_mute_button_gray);
			}
		} else {
			if (audioMuteButton != null) {
				audioMuteButton.setEnabled(true);
				audioMuteButton
						.setBackgroundResource(R.drawable.conversation_fragment_gray_button_selector);
			}

			if (audioMuteButtonText != null) {
				audioMuteButtonText
						.setTextColor(mContext
								.getResources()
								.getColor(
										R.color.fragment_conversation_connected_gray_text_color));
			}

			if (audioMuteButtonImage != null) {
				audioMuteButtonImage
						.setImageResource(R.drawable.message_voice_mute);
			}
		}
	}

	/**
	 * FIXME optimze code
	 */
	private void disableAllButtons() {

		/*
		 * View cameraButtonBG =
		 * findViewById(R.id.conversation_fragment_connected_video_camera_button
		 * ); if (cameraButtonBG != null) {
		 * cameraButtonBG.setBackgroundResource(
		 * R.drawable.conversation_framgent_gray_button_bg_pressed); }
		 */
		// For video button
		View cameraButton = findViewById(R.id.conversation_fragment_connected_video_camera_button);
		if (cameraButton != null) {
			cameraButton.setEnabled(false);
			cameraButton
					.setBackgroundResource(R.drawable.conversation_framgent_gray_button_bg_pressed);
		}
		View hangUpButton = findViewById(R.id.conversation_fragment_connected_video_hang_up_button);
		if (hangUpButton != null) {
			hangUpButton.setEnabled(false);
			hangUpButton
					.setBackgroundResource(R.drawable.conversation_framgent_gray_button_bg_pressed);
		}

		View muteButton = findViewById(R.id.conversation_fragment_connected_video_mute_button);
		if (muteButton != null) {
			muteButton.setEnabled(false);
			muteButton
					.setBackgroundResource(R.drawable.conversation_framgent_gray_button_bg_pressed);
		}

		int grayColor = mContext.getResources().getColor(
				R.color.fragment_conversation_disable_text_color);
		TextView cameraButtonText = (TextView) findViewById(R.id.conversation_fragment_connected_open_or_close_camera_text);
		if (cameraButtonText != null) {
			cameraButtonText.setTextColor(grayColor);
		}

		TextView hangUpButtonText = (TextView) findViewById(R.id.conversation_fragment_connected_video_hang_up_button_text);
		if (hangUpButtonText != null) {
			hangUpButtonText.setTextColor(grayColor);
		}

		TextView muteButtonText = (TextView) findViewById(R.id.conversation_fragment_connected_video_mute_text);
		if (muteButtonText != null) {
			muteButtonText.setTextColor(grayColor);
		}

		ImageView cameraButtonImage = (ImageView) findViewById(R.id.conversation_fragment_connected_open_or_close_camera_image);
		if (cameraButtonImage != null) {
			cameraButtonImage
					.setImageResource(R.drawable.conversation_connected_camera_button_gray);
		}

		ImageView hangUpButtonImage = (ImageView) findViewById(R.id.conversation_fragment_connected_video_hang_up_button_image);
		if (hangUpButtonImage != null) {
			hangUpButtonImage
					.setImageResource(R.drawable.conversation_connected_hang_up_button_gray);
		}

		ImageView muteButtonImage = (ImageView) findViewById(R.id.conversation_fragment_connected_video_mute_image);
		if (muteButtonImage != null) {
			muteButtonImage
					.setImageResource(R.drawable.conversation_connected_mute_button_gray);
		}
		// ---------------
		// For audio Button
		View audioSpeakerButton = findViewById(R.id.conversation_fragment_connected_speaker_button);
		if (audioSpeakerButton != null) {
			audioSpeakerButton.setEnabled(false);
			audioSpeakerButton
					.setBackgroundResource(R.drawable.conversation_framgent_gray_button_bg_pressed);
		}
		View audioHangUpButton = findViewById(R.id.conversation_fragment_connected_hang_up_button);
		if (audioHangUpButton != null) {
			audioHangUpButton.setEnabled(false);
			audioHangUpButton
					.setBackgroundResource(R.drawable.conversation_framgent_gray_button_bg_pressed);
		}

		View audioMuteButton = findViewById(R.id.conversation_fragment_connected_audio_mute_button);
		if (audioMuteButton != null) {
			audioMuteButton.setEnabled(false);
			audioMuteButton
					.setBackgroundResource(R.drawable.conversation_framgent_gray_button_bg_pressed);
		}

		TextView audioSpeakerButtonText = (TextView) findViewById(R.id.conversation_fragment_connected_speaker_text);
		if (audioSpeakerButtonText != null) {
			audioSpeakerButtonText.setTextColor(grayColor);
		}

		TextView audioHangUpButtonText = (TextView) findViewById(R.id.conversation_fragment_connected_audio_hang_up_button_text);
		if (audioHangUpButtonText != null) {
			audioHangUpButtonText.setTextColor(grayColor);
		}

		TextView audioMuteButtonText = (TextView) findViewById(R.id.conversation_fragment_connected_audio_mute_text);
		if (audioMuteButtonText != null) {
			audioMuteButtonText.setTextColor(grayColor);
		}

		ImageView audioSpeakerButtonImage = (ImageView) findViewById(R.id.conversation_fragment_connected_speaker_image);
		if (audioSpeakerButtonImage != null) {
			audioSpeakerButtonImage
					.setImageResource(R.drawable.conversation_connected_speaker_phone_button_gray);
		}

		ImageView audioHangUpButtonImage = (ImageView) findViewById(R.id.conversation_fragment_connected_audio_hang_up_button_image);
		if (audioHangUpButtonImage != null) {
			audioHangUpButtonImage
					.setImageResource(R.drawable.conversation_connected_hang_up_button_gray);
		}
		// --------
		ImageView audioMuteButtonImage = (ImageView) findViewById(R.id.conversation_fragment_connected_audio_mute_image);
		if (audioMuteButtonImage != null) {
			audioMuteButtonImage
					.setImageResource(R.drawable.conversation_connected_mute_button_gray);
		}

		// incoming audio call title
		TextView incomingCallTitle = (TextView) findViewById(R.id.fragment_conversation_audio_incoming_call_title);
		if (incomingCallTitle != null) {
			incomingCallTitle.setText(R.string.conversation_end);
		}

		// Incoming video call title
		TextView incomingVideoCallTitle = (TextView) findViewById(R.id.fragment_conversation_video_title);
		if (incomingVideoCallTitle != null) {
			incomingVideoCallTitle.setText(R.string.conversation_end);
		}

		// outing call
		if (mRejectButton != null) {
			mRejectButton
					.setBackgroundResource(R.drawable.conversation_framgent_gray_button_bg_pressed);
			((TextView) mRejectButton).setTextColor(grayColor);
		}
		// ---------------
		if (mAcceptButton != null) {
			mAcceptButton
					.setBackgroundResource(R.drawable.conversation_framgent_gray_button_bg_pressed);
			((TextView) mAcceptButton).setTextColor(grayColor);
		}
		if (mAudioOnlyButton != null) {
			mAudioOnlyButton
					.setBackgroundResource(R.drawable.conversation_framgent_gray_button_bg_pressed);
			((TextView) mAudioOnlyButton).setTextColor(grayColor);
		}

		TextView outingVideoCallTitle = (TextView) findViewById(R.id.conversation_fragment_video_outing_waiting_text);
		if (outingVideoCallTitle != null) {
			outingVideoCallTitle.setText(R.string.conversation_end);
		}

		// If is incoming layout, no mTimerTV view
		if (mTimerTV != null) {
			mTimerTV.setText(R.string.conversation_end);
		}
		// set -1 to stop update time timer
		mTimeLine = -1;
	}

	private void initViews() {
		// Initialize user avatar
		ImageView avatarIV = null;
		if (uad.isIncoming() && !uad.isConnected()) {
			if (uad.isAudioType()) {
				avatarIV = (ImageView) findViewById(R.id.conversation_fragment_audio_incoming_call_avatar);
			} else if (uad.isVideoType()) {
				avatarIV = (ImageView) findViewById(R.id.conversation_fragment_video_avatar);
			}
		} else if (uad.isAudioType()) {
			avatarIV = (ImageView) findViewById(R.id.conversation_fragment_voice_avatar);
		} else if (!uad.isIncoming() && !uad.isConnected()) {
			avatarIV = (ImageView) findViewById(R.id.conversation_fragment_video_outing_call_avatar);
		}

		if (uad.getUser().getAvatarBitmap() != null && avatarIV != null) {
			avatarIV.setImageBitmap(uad.getUser().getAvatarBitmap());
		}

		if (uad.isVideoType() && uad.isIncoming() && uad.isConnected()) {
			mLcalSurface = (SurfaceView) findViewById(R.id.fragment_conversation_connected_video_local_surface);
			mLcalSurfaceLayout = (RelativeLayout) findViewById(R.id.fragment_conversation_connected_video_local_layout);
			mRemoteSurface = (SurfaceView) findViewById(R.id.fragment_conversation_connected_video_remote_surface);
			ViewGroup.LayoutParams localLp = mLcalSurfaceLayout
					.getLayoutParams();
			adjustLocalVideoLyoutParams(localLp);

		} else if (uad.isVideoType() && !uad.isIncoming()) {
			mRemoteSurface = (SurfaceView) findViewById(R.id.fragment_conversation_connected_video_remote_surface);
			mLcalSurface = (SurfaceView) findViewById(R.id.fragment_conversation_connected_video_local_surface);
			mLcalSurfaceLayout = (RelativeLayout) findViewById(R.id.fragment_conversation_connected_video_local_layout);
			callOutLocalSurfaceChangeBig();
		}

		if (mRemoteSurface != null) {
			mRemoteSurface.setTag(SURFACE_HOLDER_TAG_REMOTE);
			mRemoteSurface.getHolder().addCallback(mRemoteVideoHolder);
		}

		if (mLcalSurface != null) {
			mLcalSurface.setTag(SURFACE_HOLDER_TAG_LOCAL);
			mLcalSurface.setZOrderMediaOverlay(true);
			mLcalSurface.getHolder().addCallback(mLocalCameraHolder);
		}

		if (uad.isAudioType()) {
			mTimerTV = (TextView) findViewById(R.id.fragment_conversation_connected_duration);
		} else if (uad.isVideoType()) {
			mTimerTV = (TextView) findViewById(R.id.conversation_fragment_connected_video_duration);
		}

		bringButtonsToFront();

		View cameraButton = findViewById(R.id.conversation_fragment_connected_video_camera_button);
		if (cameraButton != null) {
			if (!uad.isIncoming() && !uad.isConnected()) {
				cameraButton.setVisibility(View.GONE);
			} else {
				cameraButton.setVisibility(View.VISIBLE);
			}
		}

		View muteButton = findViewById(R.id.conversation_fragment_connected_video_mute_button);
		if (muteButton != null) {
			if (!uad.isIncoming() && !uad.isConnected()) {
				muteButton.setVisibility(View.GONE);
			} else {
				muteButton.setVisibility(View.VISIBLE);
			}
		}

		if (uad.isVideoType()) {
			if (!uad.isConnected() && !uad.isIncoming()) {
				findViewById(R.id.conversation_fragment_connected_title_text)
						.setVisibility(View.GONE);
				findViewById(
						R.id.conversation_fragment_connected_video_duration)
						.setVisibility(View.GONE);
				findViewById(
						R.id.conversation_fragment_outing_video_card_container)
						.setVisibility(View.VISIBLE);

				((TextView) findViewById(R.id.conversation_fragment_video_outing_call_name))
						.setText(uad.getUser().getName());
			} else if (uad.isConnected()) {
				findViewById(R.id.conversation_fragment_connected_title_text)
						.setVisibility(View.VISIBLE);
				findViewById(
						R.id.conversation_fragment_connected_video_duration)
						.setVisibility(View.VISIBLE);
				findViewById(
						R.id.conversation_fragment_outing_video_card_container)
						.setVisibility(View.GONE);
			}
		}

	}

	private void adjustLocalVideoLyoutParams(ViewGroup.LayoutParams localLp) {
		if (localLp != null) {
			VideoSize size = getCurrentCameraCaptureSureSize();
			if (displayWidthIsLonger) {
				localLp.height = localLp.width * size.height / size.width;
			} else {
				localLp.height = localLp.width * size.width / size.height;
			}
			mLcalSurfaceLayout.setLayoutParams(localLp);
		}
	}

	private void updateViewForVideoAcceptance() {
		View cameraButton = findViewById(R.id.conversation_fragment_connected_video_camera_button);
		if (cameraButton != null) {
			if (!uad.isIncoming() && !uad.isConnected()) {
				cameraButton.setVisibility(View.GONE);
			} else {
				cameraButton.setVisibility(View.VISIBLE);
			}
		}

		View muteButton = findViewById(R.id.conversation_fragment_connected_video_mute_button);
		if (muteButton != null) {
			if (!uad.isIncoming() && !uad.isConnected()) {
				muteButton.setVisibility(View.GONE);
			} else {
				muteButton.setVisibility(View.VISIBLE);
			}
		}

		if (!uad.isIncoming() && uad.isConnected()) {
			findViewById(R.id.conversation_fragment_connected_title_text)
					.setVisibility(View.VISIBLE);
			findViewById(R.id.conversation_fragment_connected_video_duration)
					.setVisibility(View.VISIBLE);
			findViewById(R.id.conversation_fragment_outing_video_card_container)
					.setVisibility(View.GONE);
		}

		mReverseCameraButton = findViewById(R.id.fragment_conversation_reverse_camera_button);
		mReverseCameraButton.setVisibility(View.VISIBLE);
		mReverseCameraButton.bringToFront();
		mReverseCameraButton.setOnClickListener(surfaceViewListener);

	}

	private void showConfirmDialog() {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.fragment_conversation_quit_dialog);
		dialog.getWindow().setBackgroundDrawable(
				new ColorDrawable(android.graphics.Color.TRANSPARENT));

		if (getObject().isAudioType()) {
			((TextView) dialog
					.findViewById(R.id.fragment_conversation_quit_dialog_content))
					.setText(R.string.conversation_quit_dialog_audio_text);
		}
		dialog.findViewById(R.id.fragment_conversation_IMWCancelButton)
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View view) {
						dialog.dismiss();
					}

				});

		dialog.findViewById(R.id.fragment_conversation_IMWQuitButton)
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View view) {
						hangUp();
						dialog.dismiss();
					}

				});
		dialog.show();
	}

	private UserChattingObject buildObject() {
		long uid = getIntent().getExtras().getLong("uid");
		boolean mIsInComingCall = getIntent().getExtras().getBoolean(
				"is_coming_call");
		boolean mIsVoiceCall = getIntent().getExtras().getBoolean("voice");
		String deviceId = getIntent().getExtras().getString("device");
		String sessionID = getIntent().getExtras().getString("sessionID");
		User u = GlobalHolder.getInstance().getUser(uid);

		int flag = mIsVoiceCall ? UserChattingObject.VOICE_CALL
				: UserChattingObject.VIDEO_CALL;
		if (mIsInComingCall) {
			flag |= UserChattingObject.INCOMING_CALL;
		} else {
			flag |= UserChattingObject.OUTING_CALL;
		}
		uad = new UserChattingObject(u, flag, deviceId);
		uad.setSzSessionID(sessionID);
		return uad;
	}

	private void updateTimer() {
		if (mTimerTV == null) {
			V2Log.e("No timer text view");
			return;
		}
		int hour = (int) mTimeLine / 3600;

		int minute = (int) (mTimeLine - (hour * 3600)) / 60;

		int second = (int) mTimeLine - (hour * 3600 + minute * 60);
		mTimerTV.setText((hour < 10 ? "0" + hour : hour) + ":"
				+ (minute < 10 ? "0" + minute : minute) + ":"
				+ (second < 10 ? "0" + second : second));
	}

	private void openRemoteVideo() {
		if (isStoped) {
			V2Log.w(" Do not open remote in stopping ");
			return;
		}
		if (isOpenedRemote) {
			V2Log.w(" Remote device already opened ");
			return;
		}
		if (!isOpenedLocal) {
			mLocalHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					openRemoteVideo();
				}

			}, 1000);
		}
		VideoPlayer.DisplayRotation = displayRotation;
		VideoPlayer vp = uad.getVp();
		if (vp == null) {
			vp = new VideoPlayer();
			uad.setVp(vp);
		}
		if (uad.getDeviceId() == null || uad.getDeviceId().isEmpty()) {
			List<UserDeviceConfig> udcList = GlobalHolder.getInstance()
					.getAttendeeDevice(uad.getUser().getmUserId());
			if (udcList != null && udcList.size() > 0) {
				uad.setDeviceId(udcList.get(0).getDeviceID());
			}
		}
		if (uad.getDeviceId() == null || uad.getDeviceId().isEmpty()) {
			V2Log.e("No P2P remote device Id");
			return;
		}
		isOpenedRemote = true;
		vp.SetSurface(getSurfaceHolder(SURFACE_HOLDER_TAG_REMOTE));
		chatService.requestOpenVideoDevice(uad.getUdc(), null);
	}

	private void closeRemoteVideo() {
		if (uad.getVp() != null && uad.getDeviceId() != null) {
			chatService.requestCloseVideoDevice(uad.getUdc(), null);
		}
		isOpenedRemote = false;
	}

	ViewGroup.LayoutParams smallP = null;

	private void callOutLocalSurfaceChangeBig() {
		ViewGroup.LayoutParams backLP = mRemoteSurface.getLayoutParams();
		smallP = mLcalSurfaceLayout.getLayoutParams();
		mLcalSurfaceLayout.setLayoutParams(backLP);
		mLcalSurface.setBackgroundResource(0);
	}

	private void callOutLocalSurfaceChangeSmall() {
		adjustLocalVideoLyoutParams(smallP);
		mLcalSurface.setBackgroundResource(R.drawable.local_video_bg);
	}

	private void exchangeSurfaceHolder() {
		ViewGroup.LayoutParams backLP = mRemoteSurface.getLayoutParams();
		ViewGroup.LayoutParams smallP = mLcalSurface.getLayoutParams();

		if (backLP.width == ViewGroup.LayoutParams.FILL_PARENT
				|| backLP.width == ViewGroup.LayoutParams.MATCH_PARENT) {
			mLcalSurface.setZOrderMediaOverlay(false);
			mLcalSurface.setZOrderOnTop(false);
			mLcalSurface.setOnClickListener(null);
			mRemoteSurface.setZOrderMediaOverlay(true);
			mRemoteSurface.setZOrderOnTop(true);
			mRemoteSurface.setOnClickListener(surfaceViewListener);
			mRemoteSurface.bringToFront();

		} else {
			mLcalSurface.bringToFront();
			mLcalSurface.setZOrderMediaOverlay(true);
			mLcalSurface.setZOrderOnTop(true);
			mLcalSurface.setOnClickListener(surfaceViewListener);
			mRemoteSurface.setZOrderMediaOverlay(false);
			mRemoteSurface.setZOrderOnTop(false);
			mRemoteSurface.setOnClickListener(null);
		}

		mLcalSurface.setLayoutParams(smallP);
		mRemoteSurface.setLayoutParams(backLP);

		bringButtonsToFront();

		findViewById(R.id.fragment_conversation_connected_video_container)
				.invalidate();
	}

	private void bringButtonsToFront() {
		if (mTimerTV != null) {
			mTimerTV.bringToFront();
		}
		View v = findViewById(R.id.conversation_fragment_connected_title_text);
		if (v != null) {
			v.bringToFront();
		}
		v = findViewById(R.id.fragment_conversation_connected_video_button_container);
		if (v != null) {
			v.bringToFront();
		}

		if (mReverseCameraButton != null) {
			mReverseCameraButton.bringToFront();
		}

		v = findViewById(R.id.conversation_fragment_outing_video_card_container);
		if (v != null) {
			v.bringToFront();
		}
	}

	private void initReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		filter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		filter.addAction(PublicIntent.FINISH_APPLICATION);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_USER_PRESENT);
		filter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		this.registerReceiver(receiver, filter);

		IntentFilter strickFliter = new IntentFilter();
		strickFliter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		strickFliter.addAction(JNIService.JNI_BROADCAST_VIDEO_CALL_CLOSED);
		Intent i = this.registerReceiver(null, strickFliter);
		// means exist close broadcast, need to finish this activity
		if (i != null) {
			V2Log.e(TAG , "initReceiver invoking ..hangUp() ");
			removeStickyBroadcast(i);
			hangUp();
		}
	}

	private void quit() {
		Log.e(TAG, currentVideoBean.mediaChatID + "");
		if (currentVideoBean.startDate == 0)
			currentVideoBean.startDate = startTime;
		MessageBuilder.saveMediaChatHistories(mContext, currentVideoBean);
		sendUpdateBroadcast();
		GlobalConfig.isVideoConversationOpen = false;
		finish();
	}

	private void sendUpdateBroadcast() {
		Intent intent = new Intent();
		intent.setAction(P2P_BROADCAST_MEDIA_UPDATE);
		intent.addCategory(PublicIntent.DEFAULT_CATEGORY);
		intent.putExtra("remoteID", currentVideoBean.remoteUserID);
		sendBroadcast(intent);
	}

	private void hangUp() {
		// Stop ring tone
		stopRingTone();
		if (uad.isVideoType()) {
			closeRemoteVideo();
		}
		closeLocalCamera();
		chatService.cancelChattingCall(uad, null);
		V2Log.e(TAG, "the hangUp() invoking HANG_UP_NOTIFICATION");
		Message.obtain(mLocalHandler, HANG_UP_NOTIFICATION).sendToTarget();
	}

	private OnClickListener surfaceViewListener = new OnClickListener() {

		@Override
		public void onClick(final View view) {
			// view.setEnabled(false);
			// exchangeSurfaceHolder();
			// mLocalHandler.postDelayed(new Runnable() {
			//
			// @Override
			// public void run() {
			// view.setEnabled(true);
			// }
			//
			// }, 500);
			view.setEnabled(false);
			reverseLocalCamera();
			mLocalHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					view.setEnabled(true);
				}

			}, 500);
		}

	};

	private OnClickListener rejectListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			chatService.refuseChatting(uad, null);
			// Remove timer
			mLocalHandler.removeCallbacks(timeOutMonitor);
			isRejected = true;
			hangUp();
		}

	};

	private OnClickListener acceptListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// Stop ring tone
			stopRingTone();
			// set state to connected
			uad.setConnected(true);
			chatService.acceptChatting(uad, null);
			// Remove timer
			mLocalHandler.removeCallbacks(timeOutMonitor);

			if (uad.isAudioType()) {
				setContentView(R.layout.fragment_conversation_outing_audio);
			} else if (uad.isVideoType()) {
				setContentView(R.layout.fragment_conversation_outing_video);
			}
			// Need to re-initialize button, because layout changed
			initButtons();

			initViews();
			if (uad.isVideoType()) {
				TextView tv = (TextView) findViewById(R.id.conversation_fragment_connected_title_text);
				tv.setText(tv.getText().toString()
						.replace("[]", uad.getUser().getName()));

				mReverseCameraButton = findViewById(R.id.fragment_conversation_reverse_camera_button);
				mReverseCameraButton.setVisibility(View.VISIBLE);
				mReverseCameraButton.setOnClickListener(surfaceViewListener);

			} else {
				TextView nameTV = (TextView) findViewById(R.id.conversation_fragment_connected_name);
				nameTV.setText(uad.getUser().getName());
			}
			currentVideoBean.startDate = GlobalConfig.getGlobalServerTime();
			V2Log.e(TAG, "get startDate is :" + currentVideoBean.startDate);
			currentVideoBean.mediaState = AudioVideoMessageBean.STATE_ANSWER_CALL;
			currentVideoBean.readSatate = AudioVideoMessageBean.STATE_READED;
			// Start to time
			Message.obtain(mLocalHandler, UPDATE_TIME).sendToTarget();
		}
	};

	private OnClickListener acceptVoicOnlyListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// Stop ring tone
			stopRingTone();

			uad.setConnected(true);
			chatService.acceptChatting(uad, null);
			// Remove timer
			mLocalHandler.removeCallbacks(timeOutMonitor);
			// do not open local video
			openRemoteVideo();
			// Start to time
			Message.obtain(mLocalHandler, UPDATE_TIME).sendToTarget();

		}

	};

	private OnClickListener mCloseOrOpenCameraButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {

			if (mLcalSurface.getTag().equals(SURFACE_HOLDER_TAG_REMOTE)) {
				exchangeSurfaceHolder();
			}

			if (mLcalSurface.getVisibility() == View.GONE) {
				mLcalSurface.setVisibility(View.VISIBLE);
				mReverseCameraButton.setVisibility(View.VISIBLE);
			} else {
				mLcalSurface.setVisibility(View.GONE);
				mReverseCameraButton.setVisibility(View.GONE);
			}

			int drawId = R.drawable.conversation_connected_camera_button_pressed;
			int color = R.color.fragment_conversation_connected_pressed_text_color;
			TextView cameraText = (TextView) findViewById(R.id.conversation_fragment_connected_open_or_close_camera_text);
			ImageView cameraImage = (ImageView) findViewById(R.id.conversation_fragment_connected_open_or_close_camera_image);

			if (view.getTag() == null || view.getTag().equals("close")) {
				view.setTag("open");
				drawId = R.drawable.conversation_connected_camera_button_pressed;
				color = R.color.fragment_conversation_connected_pressed_text_color;
				if (cameraText != null) {
					cameraText.setText(R.string.conversation_open_video_text);
				}
				closeLocalCamera();
			} else {
				view.setTag("close");
				drawId = R.drawable.conversation_connected_camera_button;
				color = R.color.fragment_conversation_connected_gray_text_color;
				cameraText.setText(R.string.conversation_close_video_text);
				openLocalCamera();
			}

			if (cameraImage != null) {
				cameraImage.setImageResource(drawId);
			}
			if (cameraText != null) {
				cameraText
						.setTextColor(mContext.getResources().getColor(color));
			}

		}

	};

	private OnClickListener mAudioSpeakerButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			AudioManager audioManager;
			audioManager = (AudioManager) mContext
					.getSystemService(Context.AUDIO_SERVICE);
			audioManager.setMode(AudioManager.MODE_NORMAL);

			int drawId = R.drawable.message_voice_lounder_pressed;
			int color = R.color.fragment_conversation_connected_pressed_text_color;

			if (view.getTag() == null || view.getTag().equals("earphone")) {
				audioManager.setSpeakerphoneOn(true);
				view.setTag("speakerphone");
				drawId = R.drawable.message_voice_lounder_pressed;
				color = R.color.fragment_conversation_connected_pressed_text_color;
			} else {
				audioManager.setSpeakerphoneOn(false);
				view.setTag("earphone");
				drawId = R.drawable.message_voice_lounder;
				color = R.color.fragment_conversation_connected_gray_text_color;
			}

			TextView speakerPhoneText = (TextView) findViewById(R.id.conversation_fragment_connected_speaker_text);
			ImageView speakerPhoneImage = (ImageView) findViewById(R.id.conversation_fragment_connected_speaker_image);

			if (speakerPhoneImage != null) {
				speakerPhoneImage.setImageResource(drawId);
			}
			if (speakerPhoneText != null) {
				speakerPhoneText.setTextColor(mContext.getResources().getColor(
						color));
			}

		}

	};

	private OnClickListener mMuteButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			uad.setMute(!uad.isMute());
			chatService.muteChatting(uad, null);
			int drawId = R.drawable.message_voice_mute_pressed;
			int color = R.color.fragment_conversation_connected_pressed_text_color;

			if (view.getTag() == null || view.getTag().equals("mute")) {
				view.setTag("speaking");
				drawId = R.drawable.message_voice_mute_pressed;
				color = R.color.fragment_conversation_connected_pressed_text_color;
			} else {
				view.setTag("mute");

				drawId = R.drawable.message_voice_mute;
				color = R.color.fragment_conversation_connected_gray_text_color;
			}

			TextView speakerOrMuteText = null;
			if (uad.isAudioType()) {
				speakerOrMuteText = (TextView) findViewById(R.id.conversation_fragment_connected_audio_mute_text);
			} else if (uad.isVideoType()) {
				speakerOrMuteText = (TextView) findViewById(R.id.conversation_fragment_connected_video_mute_text);
			}
			ImageView speakerOrMuteImage = null;
			if (uad.isAudioType()) {
				speakerOrMuteImage = (ImageView) findViewById(R.id.conversation_fragment_connected_audio_mute_image);
			} else if (uad.isVideoType()) {
				speakerOrMuteImage = (ImageView) findViewById(R.id.conversation_fragment_connected_video_mute_image);
			}

			if (speakerOrMuteImage != null) {
				speakerOrMuteImage.setImageResource(drawId);
			}
			if (speakerOrMuteText != null) {
				speakerOrMuteText.setTextColor(mContext.getResources()
						.getColor(color));
			}

		}

	};

	private OnClickListener mHangUpButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			isRejected = true;
			hangUp();
		}

	};

	private Runnable timeOutMonitor = new Runnable() {

		@Override
		public void run() {
			chatService.cancelChattingCall(uad, null);
			V2Log.e(TAG, "the timeOutMonitor invoking HANG_UP_NOTIFICATION");
			Message.obtain(mLocalHandler, HANG_UP_NOTIFICATION).sendToTarget();
		}

	};

	private SurfaceHolder.Callback mRemoteVideoHolder = new SurfaceHolder.Callback() {

		@Override
		public void surfaceChanged(SurfaceHolder holder, int flag, int width,
				int height) {
			if (uad.getVp() != null) {
				uad.getVp().SetViewSize(width, height);
			}
			if (!isOpenedRemote && uad.isConnected()) {
				openRemoteVideo();
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			if (uad.isConnected()) {
				V2Log.e("Create new holder " + holder);
				openRemoteVideo();
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			if (uad.isConnected() && !isOpenedRemote) {
				closeRemoteVideo();
			}
		}

	};

	private SurfaceHolder.Callback mLocalCameraHolder = new SurfaceHolder.Callback() {

		@Override
		public void surfaceChanged(SurfaceHolder holder, int flag, int width,
				int height) {
			if (!isStoped && !isOpenedLocal) {
				openLocalCamera();
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			if (isStoped) {
				return;
			}
			if (isOpenedLocal) {
				closeLocalCamera();
			}
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

			// when conversation is connected or during outing call
			if (uad.isConnected() || (!uad.isConnected() && !uad.isIncoming())) {
				V2Log.e("Create new holder " + holder);
				Log.i("wzl", "打开本地");
				closeLocalCamera();
				openLocalCamera();
			}
			mlocalSurfaceCreated = true;
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			mlocalSurfaceCreated = false;
			if (isOpenedLocal) {
				closeLocalCamera();
			}
		}

	};

	class LocalReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION
					.equals(action)) {
				NetworkStateCode code = (NetworkStateCode) intent.getExtras()
						.get("state");
				if (code != NetworkStateCode.CONNECTED) {
					V2Log.e(TAG,
							"JNI_BROADCAST_CONNECT_STATE_NOTIFICATION 调用了 HANG_UP_NOTIFICATION");
					Message.obtain(mLocalHandler, HANG_UP_NOTIFICATION,
							Integer.valueOf(HAND_UP_REASON_NO_NETWORK))
							.sendToTarget();
				}
			} else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
			} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
				if (uad.isVideoType()
						&& (uad.isConnected() && uad.isIncoming())) {
					closeLocalCamera();
					if (uad.isConnected()) {
						closeRemoteVideo();
					}
				}
			} else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
				if (uad.isVideoType()
						&& (uad.isConnected() && uad.isIncoming())) {
					openLocalCamera();
					if (uad.isConnected()) {
						openRemoteVideo();
					}
				}
			}
		}

	}

	private Object mLocal = new Object();
	private boolean inProgress = false;

	class LocalHandler extends Handler {

		public LocalHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_TIME:
				if (mTimeLine != -1) {
					mTimeLine++;
					updateTimer();
					Message m = Message.obtain(mLocalHandler, UPDATE_TIME);
					mLocalHandler.sendMessageDelayed(m, 1000);
				}
				break;
			case OPEN_REMOTE_VIDEO:
				openRemoteVideo();
				break;
			case QUIT:
				quit();
				break;
			case HANG_UP_NOTIFICATION:
				synchronized (mLocal) {
					V2Log.e(TAG, "the HANG_UP_NOTIFICATION was called ....");
					if (inProgress) {
						break;
					}

					if (uad.isIncoming() && isRejected == false) 
						currentVideoBean.readSatate = AudioVideoMessageBean.STATE_UNREAD;
					else
						currentVideoBean.readSatate = AudioVideoMessageBean.STATE_READED;
					
					if (currentVideoBean.startDate != 0)
						currentVideoBean.endDate = GlobalConfig.getGlobalServerTime();
					
					V2Log.e(TAG, "get endDate is :" + currentVideoBean.endDate);
					inProgress = true;
					Message timeoutMessage = Message.obtain(this, QUIT);
					this.sendMessageDelayed(timeoutMessage, 2000);
					// temptag09221
					disableAllButtons();
					closeLocalCamera();
				}

				break;
			case CALL_RESPONSE:
				JNIResponse resp = (JNIResponse) msg.obj;
				if (resp.getResult() == JNIResponse.Result.SUCCESS) {
					currentVideoBean.readSatate = AudioVideoMessageBean.STATE_READED;
					RequestChatServiceResponse rcsr = (RequestChatServiceResponse) resp;
					if (rcsr.getCode() == RequestChatServiceResponse.REJCTED) {
						V2Log.e(TAG, "CALL_RESPONSE 调用了 HANG_UP_NOTIFICATION");
						Message.obtain(this, HANG_UP_NOTIFICATION)
								.sendToTarget();
						currentVideoBean.mediaState = AudioVideoMessageBean.STATE_NO_ANSWER_CALL;
					} else if (rcsr.getCode() == RequestChatServiceResponse.ACCEPTED) {
						uad.setConnected(true);
						// Send audio invitation
						// Do not need to modify any values. because this API
						// will handler this case
						chatService.inviteUserChat(uad, null);
						// Notice do not open remote video at here
						// because we must open remote video after get video
						// connected event
						if (uad.isVideoType()) {
							uad.setDeviceId(rcsr.getDeviceID());
							if (!uad.isIncoming()) {
								// exchangeSurfaceHolder();
								callOutLocalSurfaceChangeSmall();
							}
							// openRemoteVideo();
							// updateViewForVideoAcceptance();
						} else {
							// set mute button to enable
							setMuteButtonDisable(false);
						}
						// Start to time
						Message.obtain(mLocalHandler, UPDATE_TIME)
								.sendToTarget();
						currentVideoBean.mediaState = AudioVideoMessageBean.STATE_ANSWER_CALL;
						currentVideoBean.startDate = GlobalConfig.getGlobalServerTime();
						V2Log.e(TAG, "get startDate is :" + currentVideoBean.startDate);
						// Remove timer
						mLocalHandler.removeCallbacks(timeOutMonitor);
					} else {
						V2Log.e(" indicator is null can not open audio UI ");
					}
				}
				//Stop waiting voice
				stopRingToneOuting();
				break;

			case VIDEO_CONECTED:
				if (uad.isVideoType()) {
					openRemoteVideo();
					updateViewForVideoAcceptance();
				}
				break;
			}
		}

	}

}
