package com.v2tech.view.conversation;

import v2av.VideoPlayer;
import v2av.VideoRecorder;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.ChatService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.vo.User;
import com.v2tech.vo.UserChattingObject;

public class P2PConversation extends Activity implements 
		VideoConversationListener {

	
	private static final int UPDATE_TIME = 1;
	private static final int OPEN_REMOTE_VIDEO = 2;
	private static final int TIME_OUT = 3;
	
	
	
	
	
	private ChatService chatService = new ChatService();

	private UserChattingObject uad;
	
	private LocalHandler mLocalHandler = new LocalHandler(
			Looper.getMainLooper());
	
	
	private long mTimeLine = 0;
	
	
	private TextView mTimerTV;
	
	private View mRejectButton;
	private View mAcceptButton;
	private View mAudioOnlyButton;
	
	//Video call view
	private SurfaceView mLocalSurface;
	private SurfaceView mRemoteSurface;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		uad = buildObject();
		if (uad.isIncoming()) {
			if (uad.isAudioType()) {
				setContentView(R.layout.fragment_conversation_incoming_audio_call);
				mRejectButton = findViewById(R.id.conversation_fragment_voice_reject_button);
				mAcceptButton = findViewById(R.id.conversation_fragment_voice_accept_button);
			} else if (uad.isVideoType()) {
				setContentView(R.layout.fragment_conversation_incoming_video_call);
				mRejectButton = findViewById(R.id.conversation_fragment_video_reject_button);
				mAcceptButton = findViewById(R.id.conversation_fragment_voice_accept_button);
				mAudioOnlyButton = findViewById(R.id.conversation_fragment_voice_accept_only_button);
			}
			mRejectButton.setOnClickListener(rejectListener);
			mAcceptButton.setOnClickListener(acceptListener);
			if(mAudioOnlyButton != null) {
				mAudioOnlyButton.setOnClickListener(acceptVoicOnlyListener);
			}
		} else {
			if (uad.isAudioType()) {
				setContentView(R.layout.fragment_conversation_outing_audio);
			} else if (uad.isVideoType()) {
				setContentView(R.layout.fragment_conversation_outing_video);
			}
		}
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
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	@Override
	public void openLocalCamera() {
		VideoRecorder.VideoPreviewSurfaceHolder = mLocalSurface.getHolder();
		VideoRecorder.VideoPreviewSurfaceHolder
				.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		VideoRecorder.VideoPreviewSurfaceHolder
				.setFormat(PixelFormat.TRANSPARENT);
		mLocalSurface.setZOrderOnTop(true);
		chatService.openVideoDevice(uad, null);
	}

	@Override
	public void reverseLocalCamera() {

	}

	@Override
	public void closeLocalCamera() {
		chatService.closeVideoDevice(uad, null);
	}

	@Override
	public void onBackPressed() {
		showConfirmDialog();
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
						finish();
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
		User u = GlobalHolder.getInstance().getUser(uid);

		int flag = mIsVoiceCall ? UserChattingObject.VOICE_CALL
				: UserChattingObject.VIDEO_CALL;
		if (mIsInComingCall) {
			flag |= UserChattingObject.INCOMING_CALL;
		} else {
			flag |= UserChattingObject.OUTING_CALL;
		}
		uad = new UserChattingObject(u, flag, deviceId);
		return uad;
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
	
	
	private void quit() {
		finish();
	}
	
	
	
	

	private OnClickListener rejectListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			chatService.refuseChatting(uad, null);
			//Remove timer
			mLocalHandler.removeCallbacks(timeOutMonitor);
			quit();
		}

	};


	private OnClickListener acceptListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			chatService.acceptChatting(uad, null);
			//Remove timer
			mLocalHandler.removeCallbacks(timeOutMonitor);
		}
	};

	
	
	private OnClickListener acceptVoicOnlyListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			chatService.acceptChatting(uad, null);
			//Remove timer
			mLocalHandler.removeCallbacks(timeOutMonitor);
			//TODO do not open video
		}

	};
	
	
	
	
	
	private Runnable timeOutMonitor = new Runnable() {

		@Override
		public void run() {
			chatService.cancelChattingCall(uad, null);
			Message.obtain(mLocalHandler, TIME_OUT).sendToTarget();
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
			case OPEN_REMOTE_VIDEO:
				openRemoteVideo();
				break;
			case TIME_OUT:
				quit();
				break;
			}
		}

	}

}
