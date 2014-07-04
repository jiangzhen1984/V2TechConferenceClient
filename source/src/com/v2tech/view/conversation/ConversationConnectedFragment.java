package com.v2tech.view.conversation;

import v2av.VideoPlayer;
import v2av.VideoRecorder;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.ChatService;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.vo.NetworkStateCode;
import com.v2tech.vo.UserChattingObject;

public class ConversationConnectedFragment extends Fragment {

	private static final int UPDATE_TIME = 1;
	private static final int CANCELLED_NOTIFICATION = 2;
	private static final int OPEN_REMOTE_VIDEO = 3;

	private View voiceLayout;
	private View videoLayout;

	private FrameLayout remoteSurfaceLayout;
	private SurfaceView mLocalSurface;
	private SurfaceView mRemoteSurface;
	private TextView mVideoTextTitle;
	private TextView mTimerTV;
	private View mVieoLocalCameraButton;
	private View mVideoCancelButton;
	private View mVideoMuteButton;

	private TextView mAudioSpeakerText;
	private ImageView mAudioSpeakerImage;
	private View mAudioSpeakerButton;
	private View mAudioCancelButton;
	private View mAudioMuteButton;
	private ImageView mAudioMuteImage;
	private TextView mAudioMuteText;

	private long mTimeLine = 0;

	private UserChattingObject uad;

	private TurnListener mIndicator;

	private LocalHandler mLocalHandler = new LocalHandler(
			Looper.getMainLooper());

	private VideoConversationListener call;

	private ChatService chatService = new ChatService();

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		call = (VideoConversationListener) activity;
		chatService.registerCancelledListener(mLocalHandler,
				CANCELLED_NOTIFICATION, null);

		mIndicator = (TurnListener) activity;

		uad = mIndicator.getObject();

		initReceiver();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_conversation_conntected,
				container, false);

		voiceLayout = v
				.findViewById(R.id.fragment_conversation_connected_voice_container);
		videoLayout = v
				.findViewById(R.id.fragment_conversation_connected_video_container);

		mVieoLocalCameraButton = v
				.findViewById(R.id.conversation_fragment_connected_video_camera_button);
		mVideoCancelButton = v
				.findViewById(R.id.conversation_fragment_connected_video_cancel_button);
		mVideoMuteButton = v
				.findViewById(R.id.conversation_fragment_connected_video_mute_button);
		mAudioSpeakerButton = v
				.findViewById(R.id.conversation_fragment_connected_speaker_button);
		mAudioCancelButton = v
				.findViewById(R.id.conversation_fragment_connected_cancel_button);
		mAudioMuteButton = v
				.findViewById(R.id.conversation_fragment_connected_mute_button);
		
		
		mAudioSpeakerImage= (ImageView)v
				.findViewById(R.id.conversation_fragment_connected_speaker_image);
		mAudioSpeakerText= (TextView)v
				.findViewById(R.id.conversation_fragment_connected_speaker_text);
		
		mAudioMuteImage= (ImageView)v
				.findViewById(R.id.conversation_fragment_connected_mute_image);
		mAudioMuteText= (TextView)v
				.findViewById(R.id.conversation_fragment_connected_mute_text);


		mVideoCancelButton.setOnClickListener(mCancelButtonListener);
		mVieoLocalCameraButton.setOnClickListener(mVideoCameraButtonListener);
		mVideoMuteButton.setOnClickListener(mMuteButtonListener);
		mAudioCancelButton.setOnClickListener(mCancelButtonListener);
		mAudioSpeakerButton.setOnClickListener(mAudioSpeakerButtonListener);
		mAudioMuteButton.setOnClickListener(mMuteButtonListener);

		if (uad.isVideoType()) {
			mTimerTV = (TextView) v
					.findViewById(R.id.conversation_fragment_connected_video_duration);
			mVideoTextTitle = (TextView) v
					.findViewById(R.id.conversation_fragment_connected_title_text);
			mLocalSurface = (SurfaceView) v
					.findViewById(R.id.fragment_conversation_connected_video_local_surface);

			mRemoteSurface = (SurfaceView) v
					.findViewById(R.id.fragment_conversation_connected_video_remote_surface);

			remoteSurfaceLayout = (FrameLayout) v
					.findViewById(R.id.fragment_conversation_connected_video_remote_surface_container);

			voiceLayout.setVisibility(View.GONE);
			videoLayout.setVisibility(View.VISIBLE);
			mVideoTextTitle.setText(mVideoTextTitle.getText().toString()
					.replace("[]", uad.getUser().getName()));
		} else if (uad.isAudioType()) {

			mTimerTV = (TextView) v
					.findViewById(R.id.fragment_conversation_connected_duration);
			((TextView) v
					.findViewById(R.id.conversation_fragment_connected_name))
					.setText(uad.getUser().getName());
			voiceLayout.setVisibility(View.VISIBLE);
			videoLayout.setVisibility(View.GONE);
		}

		Message.obtain(mLocalHandler, UPDATE_TIME).sendToTarget();
		return v;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (uad.isVideoType()) {
			VideoRecorder.VideoPreviewSurfaceHolder = mLocalSurface.getHolder();
			VideoRecorder.VideoPreviewSurfaceHolder
					.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			VideoRecorder.VideoPreviewSurfaceHolder
					.setFormat(PixelFormat.TRANSPARENT);
			call.openLocalCamera();
			mLocalSurface.setZOrderOnTop(true);
			mLocalSurface.bringToFront();

			Message m = Message.obtain(mLocalHandler, OPEN_REMOTE_VIDEO);
			mLocalHandler.sendMessageDelayed(m, 500);
		}
		
		uad.setMute(false);
		chatService.muteChatting(uad, null);
		
		
		AudioManager audioManager;
		audioManager = (AudioManager) getActivity().getSystemService(
				Context.AUDIO_SERVICE);
		audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
		audioManager.setSpeakerphoneOn(true);
	}

	@Override
	public void onStop() {
		super.onStop();
		if (uad.isVideoType()) {
			call.closeLocalCamera();
			closeRemoteVideo();
		}
		uad.setMute(true);
		chatService.muteChatting(uad, null);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		chatService.removeRegisterCancelledListener(mLocalHandler,
				CANCELLED_NOTIFICATION, null);
		getActivity().unregisterReceiver(receiver);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		chatService.cancelChattingCall(uad, null);
	}

	private BroadcastReceiver receiver = new LocalReceiver();

	private void initReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		filter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		filter.addAction(PublicIntent.FINISH_APPLICATION);
		filter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		getActivity().registerReceiver(receiver, filter);
	}

	class LocalReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION
					.equals(action)) {
				NetworkStateCode code = (NetworkStateCode) intent.getExtras()
						.get("state");
				if (code != NetworkStateCode.CONNECTED) {
					quit();
				}
			}
		}

	}

	public void quit() {
		if (getActivity() != null) {
			getActivity().finish();
		}
	}

	private void updateTimer() {
		int hour = (int) mTimeLine / 3600;

		int minute = (int) (mTimeLine - (hour * 3600)) / 60;

		int second = (int) mTimeLine - (hour * 3600 + minute * 60);
		mTimerTV.setText((hour < 10 ? "0" + hour : hour) + ":"
				+ (minute < 10 ? "0" + minute : minute) + ":"
				+ (second < 10 ? "0" + second : second));
	}

	private void openRemoteVideo() {
		if (uad.getVp() == null) {
			VideoPlayer vp = new VideoPlayer();
			vp.SetRotation(270);
			vp.SetSurface(mRemoteSurface.getHolder());
			uad.setVp(vp);
		}
		chatService.openVideoDevice(uad, null);
	}

	private void closeRemoteVideo() {
		if (uad.getVp() != null) {
			chatService.closeVideoDevice(uad, null);
			uad.setVp(null);
			mRemoteSurface.getHolder().setFormat(PixelFormat.TRANSPARENT);
		}

	}
	
	private OnClickListener mCancelButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			chatService.cancelChattingCall(uad, null);
			quit();
		}

	};

	private OnClickListener mVideoCameraButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
		}

	};

	private OnClickListener mAudioSpeakerButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			AudioManager audioManager;
			audioManager = (AudioManager) getActivity().getSystemService(
					Context.AUDIO_SERVICE);
			audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
			if (view.getTag() == null || view.getTag().equals("earphone")) {
				audioManager.setSpeakerphoneOn(true);
				view.setTag("speakerphone");
				mAudioSpeakerImage.setImageResource(R.drawable.message_voice_lounder_pressed);
				mAudioSpeakerText.setTextColor(getActivity().getResources().getColor(R.color.fragment_conversation_connected_pressed_text_color));
				view.setBackgroundResource(R.drawable.conversation_framgent_gray_button_bg_pressed);
			} else {
				audioManager.setSpeakerphoneOn(false);
				view.setTag("earphone");
				mAudioSpeakerImage.setImageResource(R.drawable.message_voice_lounder);
				mAudioSpeakerText.setTextColor(getActivity().getResources().getColor(R.color.fragment_conversation_connected_gray_text_color));
				view.setBackgroundResource(R.drawable.conversation_framgent_gray_button_bg);
			}

		}

	};

	private OnClickListener mMuteButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			AudioManager audioManager;
			audioManager = (AudioManager) getActivity().getSystemService(
					Context.AUDIO_SERVICE);
			audioManager.setMicrophoneMute(!audioManager.isMicrophoneMute());
			
			if (view.getTag() == null || view.getTag().equals("mute")) {
				view.setTag("speaking");
				mAudioMuteImage.setImageResource(R.drawable.message_voice_mute_pressed);
				mAudioMuteText.setTextColor(getActivity().getResources().getColor(R.color.fragment_conversation_connected_pressed_text_color));
				view.setBackgroundResource(R.drawable.conversation_framgent_gray_button_bg_pressed);
			} else {
				view.setTag("mute");
				mAudioMuteImage.setImageResource(R.drawable.message_voice_mute);
				mAudioMuteText.setTextColor(getActivity().getResources().getColor(R.color.fragment_conversation_connected_gray_text_color));
				view.setBackgroundResource(R.drawable.conversation_framgent_gray_button_bg);
			}

		}

	};

	class LocalHandler extends Handler {

		public LocalHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_TIME:
				mTimeLine++;
				updateTimer();
				Message m = Message.obtain(mLocalHandler, UPDATE_TIME);
				mLocalHandler.sendMessageDelayed(m, 1000);
				break;
			case CANCELLED_NOTIFICATION:
				quit();
				break;
			case OPEN_REMOTE_VIDEO:
				openRemoteVideo();
				break;
			}
		}

	}

}
