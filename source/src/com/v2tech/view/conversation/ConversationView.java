package com.v2tech.view.conversation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.spoledge.aacplayer.AACPlayer;
import com.spoledge.aacplayer.ArrayAACPlayer;
import com.spoledge.aacplayer.ArrayDecoder;
import com.spoledge.aacplayer.Decoder;
import com.spoledge.aacplayer.PlayerCallback;
import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.service.AsyncResult;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.ChatService;
import com.v2tech.service.FileOperationEnum;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.jni.FileTransStatusIndication;
import com.v2tech.service.jni.FileTransStatusIndication.FileTransProgressStatusIndication;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.MessageUtil;
import com.v2tech.util.SPUtil;
import com.v2tech.util.V2Log;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.adapter.VMessageAdater;
import com.v2tech.view.bo.ConversationNotificationObject;
import com.v2tech.view.contacts.ContactDetail;
import com.v2tech.view.widget.CommonAdapter;
import com.v2tech.view.widget.CommonAdapter.CommonAdapterItemWrapper;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFaceItem;
import com.v2tech.vo.VMessageFileItem;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VMessageTextItem;

public class ConversationView extends Activity {

	private final int START_LOAD_MESSAGE = 1;
	private final int LOAD_MESSAGE = 2;
	private final int END_LOAD_MESSAGE = 3;
	private final int SEND_MESSAGE = 4;
	private final int SEND_MESSAGE_DONE = 5;
	private final int SEND_MESSAGE_ERROR = 6;
	private final int PLAY_NEXT_UNREAD_MESSAGE = 7;
	private final int REQUEST_DEL_MESSAGE = 8;
	private final int FILE_STATUS_LISTENER = 20;

	private final int BATCH_COUNT = 10;
	private static final int SELECT_PICTURE_CODE = 100;
	private static final int FILE_SELECT_CODE = 101;

	private static final int VOICE_DIALOG_FLAG_RECORDING = 1;
	private static final int VOICE_DIALOG_FLAG_CANCEL = 2;
	private static final int VOICE_DIALOG_FLAG_WARING_FOR_TIME_TOO_SHORT = 3;
	private static final String TAG = "ConversationView";
	protected static final int RECEIVE_SELECTED_FILE = 1000;

	private int offset = 0;

	private long user1Id;

	private long user2Id;

	private long groupId;

	private LocalHandler lh;

	private BackendHandler backEndHandler;

	private boolean isLoading;

	private boolean mLoadedAllMessages;

	private boolean mIsInited;

	private Context mContext;

	private TextView mSendButtonTV;

	private TextView mReturnButtonTV;

	private EditText mMessageET;

	private ImageView mLoadingImg;

	private TextView mUserTitleTV;

	private ImageView mMoreFeatureIV;

	private View mAdditionFeatureContainer;

	private ImageView mSelectImageButtonIV;
	private View mSelectFileButtonIV;

	private ImageView mAudioSpeakerIV;

	private View mShowContactDetailButton;

	private Button mButtonRecordAudio;

	private MediaRecorder mRecorder = null;
	private AACPlayer mAACPlayer = null;

	private MessageReceiver receiver = new MessageReceiver();

	private ChatService mChat = new ChatService();

	private User local;
	private User remote;

	private ListView mMessagesContainer;

	private LinearLayout mFaceLayout;
	private View mSmileIconButton;

	private MessageAdapter adapter;

	private List<CommonAdapterItemWrapper> messageArray = new ArrayList<CommonAdapterItemWrapper>();

	private boolean isStopped;

	private int currentItemPos = 0;

	private ArrayList<FileInfoBean> mCheckedList;

	private ConversationNotificationObject cov = null;

	private boolean reStart;
	private ImageView mVideoCallButton;
	private ImageView mAudioCallButton;
	private Bundle bundle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.activity_contact_message);

		GlobalConfig.isConversationOpen = true;

		lh = new LocalHandler();

		mMessagesContainer = (ListView) findViewById(R.id.conversation_message_list);
		adapter = new MessageAdapter();
		mMessagesContainer.setAdapter(adapter);
		mMessagesContainer.setOnTouchListener(mHiddenOnTouchListener);
		mMessagesContainer.setOnScrollListener(scrollListener);
		// mMessagesContainer
		// .setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

		mSendButtonTV = (TextView) findViewById(R.id.message_send);
		// mSendButtonTV.setOnClickListener(sendMessageListener);
		mSendButtonTV.setOnTouchListener(sendMessageButtonListener);

		mShowContactDetailButton = findViewById(R.id.contact_detail_button);
		mShowContactDetailButton.setOnClickListener(mShowContactDetailListener);

		mMessageET = (EditText) findViewById(R.id.message_text);
		mMessageET.addTextChangedListener(mPasteWatcher);

		mReturnButtonTV = (TextView) findViewById(R.id.contact_detail_return_button);
		mReturnButtonTV.setOnTouchListener(mHiddenOnTouchListener);

		mMoreFeatureIV = (ImageView) findViewById(R.id.contact_message_plus);
		mMoreFeatureIV.setOnClickListener(moreFeatureButtonListenr);

		mSmileIconButton = findViewById(R.id.message_smile_icon);
		mSmileIconButton.setOnClickListener(mSmileIconListener);

		mSelectImageButtonIV = (ImageView) findViewById(R.id.contact_message_send_image_button);
		mSelectImageButtonIV.setOnClickListener(selectImageButtonListener);

		mSelectFileButtonIV = findViewById(R.id.contact_message_send_file_button);
		mSelectFileButtonIV.setOnClickListener(mfileSelectionButtonListener);

		mVideoCallButton = (ImageView) findViewById(R.id.contact_message_video_call_button);
		mVideoCallButton.setOnClickListener(mVideoCallButtonListener);

		mAudioCallButton = (ImageView) findViewById(R.id.contact_message_audio_call_button);
		mAudioCallButton.setOnClickListener(mAudioCallButtonListener);

		mAudioSpeakerIV = (ImageView) findViewById(R.id.contact_message_speaker);
		mAudioSpeakerIV.setOnClickListener(mMessageTypeSwitchListener);

		mButtonRecordAudio = (Button) findViewById(R.id.message_button_audio_record);
		mButtonRecordAudio.setOnTouchListener(mButtonHolderListener);

		mAdditionFeatureContainer = findViewById(R.id.contact_message_sub_feature_ly);

		mUserTitleTV = (TextView) findViewById(R.id.message_user_title);

		mFaceLayout = (LinearLayout) findViewById(R.id.contact_message_face_item_ly);

		HandlerThread thread = new HandlerThread("back-end");
		thread.start();
		synchronized (thread) {
			while (!thread.isAlive()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			backEndHandler = new BackendHandler(thread.getLooper());
		}
		initExtraObject(savedInstanceState);

		// Register listener for avatar changed
		BitmapManager.getInstance().registerBitmapChangedListener(
				avatarChangedListener);

		IntentFilter filter = new IntentFilter();
		filter.addAction(JNIService.JNI_BROADCAST_NEW_MESSAGE);
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		filter.addAction(JNIService.JNI_BROADCAST_MESSAGE_SENT_FAILED);
		filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
		registerReceiver(receiver, filter);

		mChat.registerFileTransStatusListener(this.lh, FILE_STATUS_LISTENER,
				null);
		notificateConversationUpdate();

		// Start animation
		this.overridePendingTransition(R.animator.nonam_scale_center_0_100,
				R.animator.nonam_scale_null);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!mIsInited && !mLoadedAllMessages) {
			android.os.Message m = android.os.Message.obtain(lh,
					START_LOAD_MESSAGE);
			lh.sendMessageDelayed(m, 500);
		}

		mUserTitleTV.setText(remote.getName());

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.cancel(PublicIntent.MESSAGE_NOTIFICATION_ID);
		isStopped = false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (pending) {
			pending = false;
			scrollToBottom();
		}

		if (reStart == false) {

			mCheckedList = this.getIntent().getParcelableArrayListExtra(
					"checkedFiles");
			if (mCheckedList != null && mCheckedList.size() > 0) {
				startSendMoreFile();
			}
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	protected void onStop() {
		super.onStop();
		isStopped = true;
		reStart = true;

		voiceIsSentByTimer = true;
		if (mRecorder != null) {

			stopRecording();
			starttime = 0;
			fileName = null;
			File f = new File(fileName);
			f.deleteOnExit();
			lh.removeCallbacks(mUpdateMicStatusTimer);
			lh.removeCallbacks(timeOutMonitor);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mMessageET.getWindowToken(),
				InputMethodManager.HIDE_IMPLICIT_ONLY);
		return super.onTouchEvent(event);
	}

	@Override
	public void finish() {
		super.finish();
		this.overridePendingTransition(R.animator.nonam_scale_null,
				R.animator.nonam_scale_center_100_0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(receiver);
		GlobalConfig.isConversationOpen = false;
		finishWork();
	}

	private void finishWork() {
		BitmapManager.getInstance().unRegisterBitmapChangedListener(
				avatarChangedListener);

		stopPlaying();
		releasePlayer();
		cleanCache();
		V2Log.e("conversation view exited");
		mChat.removeRegisterFileTransStatusListener(this.lh,
				FILE_STATUS_LISTENER, null);
	}

	private void initExtraObject(Bundle savedInstanceState) {

		if (savedInstanceState != null) {

			bundle = savedInstanceState.getBundle("saveBundle");
		} else {

			bundle = this.getIntent().getExtras();
		}

		if (bundle != null) {
			cov = (ConversationNotificationObject) bundle.get("obj");
			if (cov == null) {
				cov = getIntent().getParcelableExtra("obj");
			}
		} else {
			cov = new ConversationNotificationObject(Conversation.TYPE_CONTACT,
					1);
		}
		user1Id = GlobalHolder.getInstance().getCurrentUserId();
		if (cov.getType().equals(Conversation.TYPE_CONTACT)) {
			user2Id = cov.getExtId();
		} else if (cov.getType().equals(Conversation.TYPE_GROUP)) {
			groupId = cov.getExtId();
		}

		local = GlobalHolder.getInstance().getUser(user1Id);
		remote = GlobalHolder.getInstance().getUser(user2Id);
		// If current conversation is group then user2 is null;
		if (remote == null) {
			remote = new User(user2Id);
		}
	}

	private void scrollToBottom() {
		scrollToPos(messageArray.size() - 1);
	}

	private void scrollToPos(final int pos) {
		if (pos < 0 || pos >= messageArray.size()) {
			V2Log.d(TAG, "参数pos不合法 :" + pos);
			return;
		}
		mMessagesContainer.post(new Runnable() {

			@Override
			public void run() {
				mMessagesContainer.setSelection(pos);

			}

		});

	}

	private VMessage currentPlayed;

	private boolean playNextUnreadMessage() {
		boolean found = false;
		for (int i = 0; i < messageArray.size(); i++) {
			CommonAdapterItemWrapper wrapper = messageArray.get(i);
			VMessage vm = (VMessage) wrapper.getItemObject();
			if (vm == currentPlayed) {
				found = true;
				continue;
			}

			if (found) {
				List<VMessageAudioItem> items = vm.getAudioItems();
				if (items.size() > 0
						&& items.get(0).getState() == VMessageAbstractItem.STATE_UNREAD) {
					this.scrollToPos(i);
					listener.requestPlayAudio(null, vm, items.get(0));
					((MessageBodyView) wrapper.getView())
							.updateUnreadFlag(false);
					((MessageBodyView) wrapper.getView()).startVoiceAnimation();
					return true;
				}
			}
		}
		return false;
	}

	private InputStream currentPlayedStream;

	private synchronized boolean startPlaying(String fileName) {

		mAACPlayer = new ArrayAACPlayer(
				ArrayDecoder.create(Decoder.DECODER_FAAD2), mAACPlayerCallback,
				AACPlayer.DEFAULT_AUDIO_BUFFER_CAPACITY_MS,
				AACPlayer.DEFAULT_DECODE_BUFFER_CAPACITY_MS);
		try {
			if (currentPlayedStream != null) {
				currentPlayedStream.close();
			}
			// currentPlayedStream = new FileInputStream(new File(fileName));
			mAACPlayer.playAsync(fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	private void stopPlaying() {
		if (mAACPlayer != null) {
			mAACPlayer.stop();
			mAACPlayer = null;
		}
		if (currentPlayedStream != null) {
			try {
				currentPlayedStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void releasePlayer() {
		if (mAACPlayer != null) {
			mAACPlayer.stop();
			mAACPlayer = null;
		}
	}

	private Dialog mVoiceDialog = null;
	private ImageView mVolume;
	private View mSpeakingLayout;
	private View mPreparedCancelLayout;
	private View mWarningLayout;

	private void showOrCloseVoiceDialog() {
		if (mVoiceDialog == null) {
			mVoiceDialog = new Dialog(mContext, R.style.MessageVoiceDialog);
			mVoiceDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			LayoutInflater flater = LayoutInflater.from(mContext);
			View root = flater.inflate(R.layout.message_voice_dialog, null);
			mVoiceDialog.setContentView(root);
			mVolume = (ImageView) root
					.findViewById(R.id.message_voice_dialog_voice_volume);
			mSpeakingLayout = root
					.findViewById(R.id.message_voice_dialog_listening_container);
			mPreparedCancelLayout = root
					.findViewById(R.id.message_voice_dialog_cancel_container);
			mWarningLayout = root
					.findViewById(R.id.message_voice_dialog_warning_container);
			mVoiceDialog.setCancelable(false);
		}

		if (mVoiceDialog.isShowing()) {
			mVoiceDialog.dismiss();
			mButtonRecordAudio
					.setText(R.string.contact_message_button_send_audio_msg);
		} else {
			updateCancelSendVoiceMsgNotification(VOICE_DIALOG_FLAG_RECORDING);

			mVoiceDialog.show();
		}
	}

	private void updateCancelSendVoiceMsgNotification(int flag) {
		if (flag == VOICE_DIALOG_FLAG_CANCEL) {
			if (mSpeakingLayout != null) {
				mSpeakingLayout.setVisibility(View.GONE);
			}

			if (mWarningLayout != null) {
				mWarningLayout.setVisibility(View.GONE);
			}

			if (mPreparedCancelLayout != null) {
				mPreparedCancelLayout.setVisibility(View.VISIBLE);
			}
			mButtonRecordAudio
					.setText(R.string.contact_message_button_up_to_cancel);
		} else if (flag == VOICE_DIALOG_FLAG_RECORDING) {
			if (mSpeakingLayout != null) {
				mSpeakingLayout.setVisibility(View.VISIBLE);
				int duration = (int) (System.currentTimeMillis() - starttime) / 1000;
				TextView tips = (TextView) mSpeakingLayout
						.findViewById(R.id.message_voice_dialog_listening_container_tips);
				if (duration > 50) {
					String str = mContext.getText(
							R.string.contact_message_tips_rest_seconds)
							.toString();
					str = str.replace("[]", (59 - duration) + "");
					tips.setText(str);
				} else {
					tips.setText(R.string.contact_message_voice_dialog_text);
				}

			}
			if (mPreparedCancelLayout != null) {
				mPreparedCancelLayout.setVisibility(View.GONE);
			}
			if (mWarningLayout != null) {
				mWarningLayout.setVisibility(View.GONE);
			}
			mButtonRecordAudio
					.setText(R.string.contact_message_button_up_to_send);
		} else if (flag == VOICE_DIALOG_FLAG_WARING_FOR_TIME_TOO_SHORT) {
			if (mSpeakingLayout != null) {
				mSpeakingLayout.setVisibility(View.GONE);
			}
			if (mPreparedCancelLayout != null) {
				mPreparedCancelLayout.setVisibility(View.GONE);
			}
			if (mWarningLayout != null) {
				mWarningLayout.setVisibility(View.VISIBLE);
			}
			mButtonRecordAudio
					.setText(R.string.contact_message_button_send_audio_msg);
		}

	}

	private void updateVoiceVolume(int vol) {
		if (mVolume != null) {
			int resId = R.drawable.message_voice_volume_1;
			switch (vol) {
			case 0:
			case 1:
				resId = R.drawable.message_voice_volume_1;
				break;
			case 2:
				resId = R.drawable.message_voice_volume_2;
				break;
			case 3:
				resId = R.drawable.message_voice_volume_3;
				break;
			case 4:
				resId = R.drawable.message_voice_volume_4;
				break;
			default:
				resId = R.drawable.message_voice_volume_4;
				break;
			}
			mVolume.setImageResource(resId);

		}
	}

	/**
	 * FIXME If want to support lower 4.0 for ACC should code by self
	 * 
	 * @param filePath
	 * @return
	 */
	private boolean startReocrding(String filePath) {

		mRecorder = new MediaRecorder();
		mRecorder.reset();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
		mRecorder.setOutputFile(filePath);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		mRecorder.setMaxDuration(60000);
		mRecorder.setAudioSamplingRate(44100);
		mRecorder.setAudioChannels(2);
		try {
			mRecorder.prepare();
		} catch (IOException e) {
			V2Log.e(" can not prepare media recorder ");
			return false;
		}

		mRecorder.start();
		return true;
	}

	private void stopRecording() {
		// mRecorder.stop();
		mRecorder.release();
		mRecorder = null;
	}

	private void cleanRangeBitmapCache(int before, int after) {
		int size = messageArray.size();
		if (size < after && before < 0) {
			return;
		}

		while (--before >= 0) {
			VMessageAdater ItemWrapper = (VMessageAdater) messageArray
					.get(before);
			VMessage vm = (VMessage) ItemWrapper.getItemObject();
			ItemWrapper.setView(null);
			vm.recycleAllImageMessage();
		}

		while (++after < size) {
			VMessageAdater ItemWrapper = (VMessageAdater) messageArray
					.get(after);
			VMessage vm = (VMessage) ItemWrapper.getItemObject();
			ItemWrapper.setView(null);
			vm.recycleAllImageMessage();
		}
	}

	private void cleanCache() {
		messageArray.clear();
	}

	private PlayerCallback mAACPlayerCallback = new PlayerCallback() {

		@Override
		public void playerStarted() {
			currentPlayed.getAudioItems().get(0).setPlaying(true);
			runOnUiThread(new Runnable(){

				@Override
				public void run() {
					
					for (int i = 0; i < messageArray.size(); i++) {
						CommonAdapterItemWrapper wrapper = messageArray.get(i);
						VMessage vm = (VMessage) wrapper.getItemObject();
						if (vm == currentPlayed) {
							((MessageBodyView)wrapper.getView()).startVoiceAnimation();
						}
					}
				}});
			V2Log.i(TAG, "设置当前audio正在播放标识 true：" + currentPlayed.getId() + "currentPlayed集合长度：" + currentPlayed.getAudioItems().size());
		}

		@Override
		public void playerPCMFeedBuffer(boolean isPlaying,
				int audioBufferSizeMs, int audioBufferCapacityMs) {
			V2Log.e(audioBufferSizeMs + "  " + audioBufferCapacityMs);
			if (audioBufferSizeMs == audioBufferCapacityMs) {

			}
		}

		@Override
		public synchronized void playerStopped(int perf) {
			
			if(currentFlag == true){
				currentFlag = false;
				return;
			}
			
			if (currentPlayed != null
					&& currentPlayed.getAudioItems().size() > 0) {
				currentPlayed.getAudioItems().get(0).setPlaying(false);
				runOnUiThread(new Runnable(){

					@Override
					public void run() {
						
						for (int i = 0; i < messageArray.size(); i++) {
							CommonAdapterItemWrapper wrapper = messageArray.get(i);
							VMessage vm = (VMessage) wrapper.getItemObject();
							if (vm == currentPlayed) {
								((MessageBodyView)wrapper.getView()).stopVoiceAnimation();
							}
						}
					}});
				V2Log.i(TAG, "设置当前audio正在播放标识 false：" + currentPlayed.getId());
			}
			// Call in main thread
			Message.obtain(lh, PLAY_NEXT_UNREAD_MESSAGE).sendToTarget();

		}

		@Override
		public void playerException(Throwable t) {
			playerStopped(0);
			if (currentPlayed != null) {
				currentPlayed.getAudioItems().get(0).setPlaying(false);
			}
		}

	};

	private OnTouchListener sendMessageButtonListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View anchor, MotionEvent mv) {
			int action = mv.getAction();
			if (action == MotionEvent.ACTION_DOWN
					|| action == MotionEvent.ACTION_HOVER_ENTER) {
			} else if (action == MotionEvent.ACTION_UP) {
				doSendMessage();
			}
			return true;
		}

	};

	private OnClickListener mMessageTypeSwitchListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			String tag = (String) view.getTag();
			if (tag != null) {
				if (tag.equals("speaker")) {
					view.setTag("keyboard");
					((ImageView) view)
							.setImageResource(R.drawable.message_keyboard);
					mButtonRecordAudio.setVisibility(View.VISIBLE);
					mMessageET.setVisibility(View.GONE);
					mSendButtonTV.setVisibility(View.GONE);

					if (mMoreFeatureIV.getTag() != null) {
						if (!mMoreFeatureIV.getTag().equals("plus")) {

							mMoreFeatureIV
									.setImageResource(R.drawable.message_plus);
							mMoreFeatureIV.setTag("plus");
							mAdditionFeatureContainer.setVisibility(View.GONE);
						}
					}

				} else if (tag.equals("keyboard")) {
					view.setTag("speaker");
					((ImageView) view)
							.setImageResource(R.drawable.speaking_button);
					mButtonRecordAudio.setVisibility(View.GONE);
					mMessageET.setVisibility(View.VISIBLE);
					mSendButtonTV.setVisibility(View.VISIBLE);
				}
			}
		}

	};

	private boolean voiceIsSentByTimer;
	private String fileName = null;
	private long starttime = 0;

	private OnTouchListener mButtonHolderListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View view, MotionEvent event) {

			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				showOrCloseVoiceDialog();
				fileName = GlobalConfig.getGlobalAudioPath() + "/"
						+ System.currentTimeMillis() + ".aac";
				if (!startReocrding(fileName)) {
					// hide dialog
					showOrCloseVoiceDialog();
				}
				// Start update db for voice
				lh.postDelayed(mUpdateMicStatusTimer, 200);
				// Start timer
				lh.postDelayed(timeOutMonitor, 59 * 1000);
				starttime = System.currentTimeMillis();
				voiceIsSentByTimer = false;
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				Rect r = new Rect();
				view.getDrawingRect(r);
				// check if touch position out of button than cancel send voice
				// message
				if (r.contains((int) event.getX(), (int) event.getY())) {
					updateCancelSendVoiceMsgNotification(VOICE_DIALOG_FLAG_RECORDING);
				} else {
					updateCancelSendVoiceMsgNotification(VOICE_DIALOG_FLAG_CANCEL);
				}

			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				if (voiceIsSentByTimer) {
					return false;
				}

				stopRecording();

				Rect r = new Rect();
				view.getDrawingRect(r);
				// check if touch position out of button than cancel send voice
				// message
				long seconds = (System.currentTimeMillis() - starttime);
				if (r.contains((int) event.getX(), (int) event.getY())
						&& seconds > 1500) {
					// send
					VMessage vm = new VMessage(groupId, local, remote);

					new VMessageAudioItem(vm, fileName, (int) (seconds / 1000));
					// Send message to server
					sendMessageToRemote(vm);
				} else {

					if (seconds < 1500) {
						updateCancelSendVoiceMsgNotification(VOICE_DIALOG_FLAG_WARING_FOR_TIME_TOO_SHORT);
					} else {
						Toast.makeText(mContext,
								R.string.contact_message_message_cancelled,
								Toast.LENGTH_SHORT).show();
					}
					File f = new File(fileName);
					f.deleteOnExit();
				}
				starttime = 0;
				fileName = null;
				lh.removeCallbacks(mUpdateMicStatusTimer);
				if (seconds < 1500) {
					// Send delay message for close dialog
					lh.postDelayed(new Runnable() {
						@Override
						public void run() {
							showOrCloseVoiceDialog();
						}

					}, 1000);
				} else {
					showOrCloseVoiceDialog();
				}
				// Remove timer
				lh.removeCallbacks(timeOutMonitor);

				mButtonRecordAudio
						.setText(R.string.contact_message_button_send_audio_msg);
			}
			return false;
		}

	};

	private Runnable timeOutMonitor = new Runnable() {

		@Override
		public void run() {
			voiceIsSentByTimer = true;
			stopRecording();
			// send
			VMessage vm = new VMessage(groupId, local, remote);
			int seconds = (int) ((System.currentTimeMillis() - starttime) / 1000) + 1;
			VMessageAudioItem vai = new VMessageAudioItem(vm, fileName, seconds);
			vai.setState(VMessageAbstractItem.STATE_NORMAL);
			// Send message to server
			sendMessageToRemote(vm);

			starttime = 0;
			fileName = null;
			lh.removeCallbacks(mUpdateMicStatusTimer);
			showOrCloseVoiceDialog();
		}

	};

	private OnClickListener moreFeatureButtonListenr = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mMoreFeatureIV.getTag() == null
					|| mMoreFeatureIV.getTag().equals("plus")) {
				mMoreFeatureIV.setImageResource(R.drawable.message_minus);
				mMoreFeatureIV.setTag("minus");
				mAdditionFeatureContainer.setVisibility(View.VISIBLE);
				if (!mMessageET.isShown()) {
					mAudioSpeakerIV.setTag("speaker");
					mAudioSpeakerIV
							.setImageResource(R.drawable.speaking_button);
					mButtonRecordAudio.setVisibility(View.GONE);
					mMessageET.setVisibility(View.VISIBLE);
					mSendButtonTV.setVisibility(View.VISIBLE);
				}
			} else {
				mMoreFeatureIV.setImageResource(R.drawable.message_plus);
				mMoreFeatureIV.setTag("plus");
				mAdditionFeatureContainer.setVisibility(View.GONE);
			}
		}

	};

	private OnClickListener selectImageButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// Intent intent = new Intent();
			// intent.setType("image/*");
			// intent.setAction(Intent.ACTION_GET_CONTENT);
			// startActivityForResult(
			// Intent.createChooser(intent, "Select Picture"),
			// SELECT_PICTURE_CODE);
			Intent intent = new Intent(ConversationView.this,
					ConversationSelectImage.class);
			startActivityForResult(intent, SELECT_PICTURE_CODE);
		}

	};

	private OnClickListener mSmileIconListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mFaceLayout.getChildCount() <= 0) {
				// init faceItem
				for (int i = 1; i < GlobalConfig.GLOBAL_FACE_ARRAY.length; i++) {
					ImageView iv = new ImageView(mContext);
					iv.setImageResource(GlobalConfig.GLOBAL_FACE_ARRAY[i]);
					iv.setTag(i + "");
					iv.setOnClickListener(mFaceSelectListener);
					mFaceLayout.addView(iv);
				}
			}
			if (mFaceLayout.getVisibility() == View.GONE) {
				mFaceLayout.setVisibility(View.VISIBLE);
			} else {
				mFaceLayout.setVisibility(View.GONE);
			}

		}

	};

	private OnClickListener mFaceSelectListener = new OnClickListener() {

		@Override
		public void onClick(View smile) {
			Editable et = mMessageET.getEditableText();
			String str = et.toString() + " ";
			String[] len = str.split("((/:){1}(.){1}(:/){1})");
			if (len.length > 10) {
				Toast.makeText(mContext,
						R.string.error_contact_message_face_too_much,
						Toast.LENGTH_SHORT).show();
				return;
			}
			Drawable drawable = ((ImageView) (smile)).getDrawable();
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
					drawable.getIntrinsicHeight());

			String emoji = GlobalConfig
					.getEmojiStr(GlobalConfig.GLOBAL_FACE_ARRAY[Integer
							.parseInt(smile.getTag().toString())]);
			mMessageET.append(emoji);
		}

	};

	private View.OnClickListener mfileSelectionButtonListener = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if (SPUtil.checkCurrentAviNetwork(mContext)) {
				Intent intent = new Intent(ConversationView.this,
						ConversationSelectFileEntry.class);
				startActivityForResult(intent, RECEIVE_SELECTED_FILE);
				// intent.putExtra("obj", cov);
				// startActivity(intent);
				// finishWork();
				// finish();
			} else {

				Toast.makeText(mContext, "当前网络不可用，请稍候再试。", Toast.LENGTH_SHORT)
						.show();
			}
		}
	};

	private View.OnClickListener mVideoCallButtonListener = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			// if (!SPUtil.checkCurrentAviNetwork(mContext)) {
			// Toast.makeText(mContext,
			// R.string.conversation_no_network_notification,
			// Toast.LENGTH_SHORT).show();
			// return;
			// }
			// startVideoCall();
		}
	};

	private View.OnClickListener mAudioCallButtonListener = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {

		}
	};

	private TextWatcher mPasteWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable edit) {
			mMessageET.removeTextChangedListener(this);
			int start = -1, end = -1;
			int index = 0;
			while (index < edit.length()) {
				if (edit.charAt(index) == '/' && index < edit.length() - 1
						&& edit.charAt(index + 1) == ':') {
					start = index;
					index += 2;
					continue;
				} else if (start != -1) {
					if (edit.charAt(index) == ':' && index < edit.length() - 1
							&& edit.charAt(index + 1) == '/') {
						end = index + 2;
						SpannableStringBuilder builder = new SpannableStringBuilder();

						int ind = GlobalConfig.getDrawableIndexByEmoji(edit
								.subSequence(start, end).toString());
						// replay emoji and clean
						if (ind > 0) {
							MessageUtil
									.appendSpan(
											builder,
											mContext.getResources()
													.getDrawable(
															GlobalConfig.GLOBAL_FACE_ARRAY[ind]),
											ind);
							edit.replace(start, end, builder);
						}
						index = start;
						start = -1;
						end = -1;
					}
				}
				index++;
			}

			mMessageET.addTextChangedListener(this);
		}

		@Override
		public void beforeTextChanged(CharSequence ch, int arg1, int arg2,
				int arg3) {
		}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {

		}

	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// if (requestCode == SELECT_PICTURE_CODE) {
		// Uri selectedImage = data.getData();
		// Uri selectedImage = Uri.parse(data
		// .getStringExtra("checkedImage"));
		// String[] filePathColumn = { MediaStore.Images.Media.DATA };
		//
		// Cursor cursor = getContentResolver().query(selectedImage,
		// filePathColumn, null, null, null);
		// if (cursor == null) {
		// Toast.makeText(mContext,
		// R.string.error_contact_messag_invalid_image_path,
		// Toast.LENGTH_SHORT).show();
		// return;
		// }
		// cursor.moveToFirst();
		//
		// int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
		// String filePath = cursor.getString(columnIndex);
		// cursor.close();
		// if (filePath == null) {
		// Toast.makeText(mContext,
		// R.string.error_contact_messag_invalid_image_path,
		// Toast.LENGTH_SHORT).show();
		// return;
		// }
		// VMessage vim = MessageBuilder.buildImageMessage(local, remote,
		// filePath);
		// // Send message to server
		// sendMessageToRemote(vim);
		// }
		if (requestCode == SELECT_PICTURE_CODE && data != null) {
			String filePath = data.getStringExtra("checkedImage");
			if (filePath == null) {
				Toast.makeText(mContext,
						R.string.error_contact_messag_invalid_image_path,
						Toast.LENGTH_SHORT).show();
				return;
			}
			VMessage vim = MessageBuilder.buildImageMessage(local, remote,
					filePath);
			// Send message to server
			sendMessageToRemote(vim);
		} else if (requestCode == RECEIVE_SELECTED_FILE) {
			if (data != null) {
				mCheckedList = data.getParcelableArrayListExtra("checkedFiles");
				if (mCheckedList != null && mCheckedList.size() > 0) {
					startSendMoreFile();
					mCheckedList = null;
				}
			}
		} else if (requestCode == 0) {

		}

	}

	// public void startVideoCall() {
	// Intent iv = new Intent();
	// iv.addCategory(PublicIntent.DEFAULT_CATEGORY);
	// iv.setAction(PublicIntent.START_P2P_CONVERSACTION_ACTIVITY);
	// iv.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	// iv.putExtra("uid", user2Id);
	// iv.putExtra("is_coming_call", false);
	// iv.putExtra("voice", false);
	// List<UserDeviceConfig> list = GlobalHolder.getInstance()
	// .getAttendeeDevice(user2Id);
	// if (list != null && list.size() > 0) {
	// iv.putExtra("device", list.get(0).getDeviceID());
	// } else {
	// iv.putExtra("device", "");
	// }
	// mContext.startActivity(iv);
	//
	// }

	private void doSendMessage() {
		String content = mMessageET.getEditableText().toString();
		if (content == null || content.equals("")) {
			return;
		}
		content = removeEmoji(content);
		if (remote == null) {
			remote = new User(user2Id);
		}

		VMessage vm = new VMessage(this.groupId, local, remote);

		String[] array = content.split("\n");
		for (int i = 0; i < array.length; i++) {
			String str = array[i];
			int len = str.length();
			if (str.length() <= 4) {
				VMessageAbstractItem vai = new VMessageTextItem(vm, str);
				vai.setNewLine(true);
				continue;
			}

			int emojiStart = -1, end = -1, strStart = 0;
			int index = 0;
			while (index < str.length()) {
				if (str.charAt(index) == '/' && index < len - 1
						&& str.charAt(index + 1) == ':') {
					emojiStart = index;
					index += 2;
					continue;
				} else if (emojiStart != -1) {
					// Found end flag of emoji
					if (str.charAt(index) == ':' && index < len - 1
							&& str.charAt(index + 1) == '/') {
						end = index + 2;

						// If emojiStart lesser than strStart,
						// mean there exist string before emoji
						if (strStart < emojiStart) {
							String strTextContent = str.substring(strStart,
									emojiStart);
							VMessageTextItem vti = new VMessageTextItem(vm,
									strTextContent);
							// If strStart is 0 means string at new line
							if (strStart == 0) {
								vti.setNewLine(true);
							}

						}

						int ind = GlobalConfig.getDrawableIndexByEmoji(str
								.subSequence(emojiStart, end).toString());
						if (ind > 0) {
							// new face item and add list
							VMessageFaceItem vfi = new VMessageFaceItem(vm, ind);
							// If emojiStart is 0 means emoji at new line
							if (emojiStart == 0) {
								vfi.setNewLine(true);
							}

						}
						// Assign end to index -1, do not assign end because
						// index will be ++
						index = end - 1;
						strStart = end;
						emojiStart = -1;
						end = -1;

					}
				}

				// check if exist last string
				if (index == len - 1 && strStart <= index) {
					String strTextContent = str.substring(strStart, len);
					VMessageTextItem vti = new VMessageTextItem(vm,
							strTextContent);
					// If strStart is 0 means string at new line
					if (strStart == 0) {
						vti.setNewLine(true);
					}

					strStart = index;
				}

				index++;
			}

		}

		List<VMessageAbstractItem> items = vm.getItems();
		for (VMessageAbstractItem item : items) {
			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_TEXT) {
				System.out.println(((VMessageTextItem) item).getText() + "    "
						+ item.isNewLine());
			} else {
				System.out.println(((VMessageFaceItem) item).getIndex()
						+ "    " + item.isNewLine());
			}
		}
		//
		mMessageET.setText("");
		// Send message to server
		sendMessageToRemote(vm);
	}

	private void sendMessageToRemote(VMessage vm) {
		// // Save message
		MessageBuilder.saveMessage(this, vm);

		Message.obtain(lh, SEND_MESSAGE, vm).sendToTarget();
		addMessageToContainer(vm);
		// send notification
		notificateConversationUpdate();
	}

	// FIXME optimize code
	private String removeEmoji(String content) {
		byte[] bys = new byte[] { -16, -97 };
		byte[] bc = content.getBytes();
		byte[] copy = new byte[bc.length];
		int j = 0;
		for (int i = 0; i < bc.length; i++) {
			if (i < bc.length - 2 && bys[0] == bc[i] && bys[1] == bc[i + 1]) {
				i += 3;
				continue;
			}
			copy[j] = bc[i];
			j++;
		}
		return new String(copy, 0, j);
	}

	private void notificateConversationUpdate() {
		Intent i = new Intent(PublicIntent.REQUEST_UPDATE_CONVERSATION);
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);
		ConversationNotificationObject obj = null;
		if (groupId == 0) {
			obj = new ConversationNotificationObject(Conversation.TYPE_CONTACT,
					user2Id);
		} else {
			obj = new ConversationNotificationObject(Conversation.TYPE_GROUP,
					groupId);
		}
		i.putExtra("obj", obj);
		mContext.sendBroadcast(i);
	}

	private void addMessageToContainer(VMessage msg) {
		// make offset
		offset++;

		messageArray.add(new VMessageAdater(msg));
		scrollToBottom();
		adapter.notifyDataSetChanged();
	}

	private OnScrollListener scrollListener = new OnScrollListener() {
		boolean scrolled = false;
		int lastFirst = 0;
		boolean isUPScroll = false;

		@Override
		public void onScroll(AbsListView view, int first, int allVisibleCount,
				int allCount) {
			if (!scrolled) {
				return;
			}

			if (first <= 2 && isUPScroll && !mLoadedAllMessages) {
				android.os.Message.obtain(lh, START_LOAD_MESSAGE)
						.sendToTarget();
				currentItemPos = first;
				// Do not clean image message state when loading message
			} else {
				if (lastFirst != first) {
					cleanRangeBitmapCache(first - 5, first + allVisibleCount
							+ BATCH_COUNT);
				}
			}
			// Calculate scrolled direction
			isUPScroll = first < lastFirst ? true : false;
			lastFirst = first;

		}

		@Override
		public void onScrollStateChanged(AbsListView av, int state) {
			scrolled = state != AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
		}

	};
	private boolean currentFlag;

	private MessageBodyView.ClickListener listener = new MessageBodyView.ClickListener() {

		@Override
		public void onMessageClicked(VMessage v) {
			Intent i = new Intent();
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.setAction(PublicIntent.START_VIDEO_IMAGE_GALLERY);
			i.putExtra("uid1", user1Id);
			i.putExtra("uid2", user2Id);
			i.putExtra("cid", v.getId());
			// type 0: is not group image view
			// type 1: group image view
			i.putExtra("type", groupId == 0 ? 0 : 1);
			mContext.startActivity(i);
		}

		@Override
		public void requestPlayAudio(View v, VMessage vm, VMessageAudioItem vai) {
			if (vai != null && vai.getAudioFilePath() != null) {
				currentPlayed = vm;
				V2Log.i(TAG, "currentPlayingAudio:--" + currentPlayed.getId());
				startPlaying(vai.getAudioFilePath());
				if (vai.getState() == VMessageAbstractItem.STATE_UNREAD) {
					vai.setState(VMessageAbstractItem.STATE_NORMAL);
					ContentValues cv = new ContentValues();
					cv.put(ContentDescriptor.MessageItems.Cols.STATE,
							vai.getState());
					mContext.getContentResolver().update(
							ContentDescriptor.MessageItems.CONTENT_URI, cv,
							ContentDescriptor.MessageItems.Cols.ID + " = ? ",
							new String[] { vai.getId() + "" });

				}
			}
		}

		@Override
		public void reSendMessageClicked(VMessage v) {
			v.setState(VMessage.STATE_UNREAD);
			List<VMessageAbstractItem> items = v.getItems();
			for (int i = 0; i < items.size(); i++) {
				VMessageAbstractItem item = items.get(i);
				if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FILE) {
					item.setState(VMessageAbstractItem.STATE_FILE_SENDING);
				} else {
					item.setState(VMessageAbstractItem.STATE_NORMAL);
				}
			}
			Message.obtain(lh, SEND_MESSAGE, v).sendToTarget();
		}

		@Override
		public void requestDelMessage(VMessage v) {
			Message.obtain(lh, REQUEST_DEL_MESSAGE, v).sendToTarget();
		}

		@Override
		public void requestStopAudio(View v, VMessage vm, VMessageAudioItem vai) {
			V2Log.i(TAG, "当前正在播放的音频item：" + vai.getId() + "停止");
			vai.setPlaying(false);
			stopPlaying();
		}

		@Override
		public void requestDownloadFile(View v, VMessage vm,
				VMessageFileItem vfi) {
			if (vfi == null) {
				return;
			}
			vfi.setState(VMessageFileItem.STATE_FILE_DOWNLOADING);

			mChat.updateFileOperation(vfi,
					FileOperationEnum.OPERATION_START_DOWNLOAD, null);
		}

		@Override
		public void requestPauseTransFile(View v, VMessage vm,
				VMessageFileItem vfi) {
			if (vfi == null) {
				return;
			}
			if (vfi.getState() == VMessageFileItem.STATE_FILE_DOWNLOADING) {
				mChat.updateFileOperation(vfi,
						FileOperationEnum.OPERATION_PAUSE_DOWNLOADING, null);
				vfi.setState(VMessageFileItem.STATE_FILE_PAUSED_DOWNLOADING);
			} else if (vfi.getState() == VMessageFileItem.STATE_FILE_SENDING) {
				mChat.updateFileOperation(vfi,
						FileOperationEnum.OPERATION_PAUSE_SENDING, null);
				vfi.setState(VMessageFileItem.STATE_FILE_PAUSED_SENDING);
			}

		}

		@Override
		public void requestResumeTransFile(View v, VMessage vm,
				VMessageFileItem vfi) {
			if (vfi == null) {
				return;
			}

			if (vfi.getState() == VMessageFileItem.STATE_FILE_PAUSED_DOWNLOADING) {
				mChat.updateFileOperation(vfi,
						FileOperationEnum.OPERATION_RESUME_DOWNLOAD, null);
				vfi.setState(VMessageFileItem.STATE_FILE_DOWNLOADING);
			} else if (vfi.getState() == VMessageFileItem.STATE_FILE_PAUSED_SENDING) {
				mChat.updateFileOperation(vfi,
						FileOperationEnum.OPERATION_RESUME_SEND, null);
				vfi.setState(VMessageFileItem.STATE_FILE_SENDING);
			}

		}

		@Override
		public void requestPauseDownloadFile(View v, VMessage vm,
				VMessageFileItem vfi) {
			if (vfi == null) {
				return;
			}

			mChat.updateFileOperation(vfi,
					FileOperationEnum.OPERATION_PAUSE_DOWNLOADING, null);
			vfi.setState(VMessageFileItem.STATE_FILE_PAUSED_DOWNLOADING);
		}

		@Override
		public void requestResumeDownloadFile(View v, VMessage vm,
				VMessageFileItem vfi) {
			if (vfi == null) {
				return;
			}
			mChat.updateFileOperation(vfi,
					FileOperationEnum.OPERATION_RESUME_DOWNLOAD, null);
			vfi.setState(VMessageFileItem.STATE_FILE_DOWNLOADING);
		}

		@Override
		public void requestStopOtherAudio(VMessage vm) {
			if(currentPlayed == null || currentPlayed.getId() == vm.getId()){
				return ;
			}
			V2Log.i(TAG, "停止了当前正在播放的currentPlayingAudio:--" + currentPlayed.getId());
			currentFlag = true;
			currentPlayed.getAudioItems().get(0).setPlaying(false);
			runOnUiThread(new Runnable(){

				@Override
				public void run() {
					for (int i = 0; i < messageArray.size(); i++) {
						CommonAdapterItemWrapper wrapper = messageArray.get(i);
						VMessage vm = (VMessage) wrapper.getItemObject();
						if (vm == currentPlayed) {
							((MessageBodyView)wrapper.getView()).stopVoiceAnimation();
						}
					}
				}
			});
			stopPlaying();
		}
	};

	private List<VMessage> loadMessages() {

		List<VMessage> array = null;
		if (this.groupId == 0) {
			array = MessageLoader.loadMessageByPage(mContext, user1Id, user2Id,
					BATCH_COUNT, offset);
		} else if (this.groupId != 0) {
			array = MessageLoader.loadGroupMessageByPage(mContext, groupId,
					BATCH_COUNT, offset);
		}

		if (array != null) {
			offset += array.size();
			mIsInited = true;
		}
		return array;
	}

	private boolean pending = false;

	private boolean queryAndAddMessage(final int msgId) {

		VMessage m = MessageLoader.loadMessageById(mContext, msgId);
		if (m == null || m.getFromUser().getmUserId() != this.user2Id
				|| m.getGroupId() != this.groupId) {
			return false;
		}
		MessageBodyView mv = new MessageBodyView(this, m, true);

		messageArray.add(new VMessageAdater(m));
		if (mv != null) {
			if (!isStopped) {
				this.scrollToBottom();
			} else {
				pending = true;
			}
		}
		return true;
	}

	private boolean removeMessage(VMessage vm) {
		if (vm == null) {
			return false;
		}
		for (int i = 0; i < messageArray.size(); i++) {
			VMessageAdater va = (VMessageAdater) messageArray.get(i);
			if (vm.getId() == ((VMessage) va.getItemObject()).getId()) {
				messageArray.remove(i);
				return true;
			}
		}
		return false;
	}

	private void updateFileProgressView(String uuid, long tranedSize) {
		for (int i = 0; i < messageArray.size(); i++) {
			VMessage vm = (VMessage) messageArray.get(i).getItemObject();
			if (vm.getItems().size() > 0) {
				VMessageAbstractItem item = vm.getItems().get(0);
				if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FILE
						&& item.getUuid().equals(uuid)) {
					VMessageFileItem vfi = ((VMessageFileItem) item);

					if (vfi.getFileSize() == tranedSize) {
						if (vfi.getState() == VMessageAbstractItem.STATE_FILE_SENDING) {
							vfi.setState(VMessageAbstractItem.STATE_FILE_SENT);
						} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING) {
							vfi.setState(VMessageAbstractItem.STATE_FILE_DOWNLOADED);
						}

						MessageBuilder.updateVMessageItem(this, vfi);
					}

					vfi.setDownloadedSize(tranedSize);
					CommonAdapterItemWrapper common = messageArray.get(i);
					MessageBodyView mv = (MessageBodyView) common.getView();
					if (mv != null) {

						mv.updateView(vfi);
					} else {
						notificateConversationUpdate();
					}
				}
			}
		}

	}

	private void updateFileTransErrorView(String uuid) {
		for (int i = 0; i < messageArray.size(); i++) {
			VMessage vm = (VMessage) messageArray.get(i).getItemObject();
			if (vm.getItems().size() > 0) {
				VMessageAbstractItem item = vm.getItems().get(0);
				if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FILE
						&& item.getUuid().equals(uuid)) {
					VMessageFileItem vfi = ((VMessageFileItem) item);
					vfi.setDownloadedSize(0);
					// If lesser than sending, means file is receive
					if (vfi.getState() < VMessageFileItem.STATE_FILE_SENDING) {
						vfi.setState(VMessageFileItem.STATE_FILE_DOWNLOADED_FALIED);
					} else {
						vfi.setState(VMessageFileItem.STATE_FILE_SENT_FALIED);
					}
					MessageBuilder.updateVMessageItem(this, vfi);

					((MessageBodyView) messageArray.get(i).getView())
							.updateView(vfi);

				}
			}
		}
	}

	private OnTouchListener mHiddenOnTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View view, MotionEvent arg1) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mMessageET.getWindowToken(),
					InputMethodManager.RESULT_UNCHANGED_SHOWN);
			if (mReturnButtonTV == view) {
				onBackPressed();
			}
			return false;
		}

	};

	private OnClickListener mShowContactDetailListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			Intent i = new Intent();
			i.setClass(mContext, ContactDetail.class);
			i.putExtra("uid", user2Id);
			i.putExtra("obj", cov);
			i.putExtra("fromPlace", "ConversationView");
			i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivityForResult(i, RECEIVE_SELECTED_FILE);
		}

	};

	private CommonAdapter.ViewConvertListener mConvertListener = new CommonAdapter.ViewConvertListener() {

		@Override
		public View converView(CommonAdapterItemWrapper wr, View convertView,
				ViewGroup vg) {
			if (wr == null) {
				return null;
			}
			VMessage vm = (VMessage) wr.getItemObject();
			if (convertView == null) {
				MessageBodyView mv = new MessageBodyView(mContext, vm, true);
				mv.setCallback(listener);
				convertView = mv;
			} else {
				((MessageBodyView) convertView).updateView(vm);
			}
			((VMessageAdater) wr).setView(convertView);
			return convertView;
		}
	};

	private BitmapManager.BitmapChangedListener avatarChangedListener = new BitmapManager.BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			if (user == null) {
				return;
			}
			if (user.getmUserId() == local.getmUserId()
					|| user.getmUserId() == remote.getmUserId()) {
				for (int i = 0; i < messageArray.size(); i++) {
					MessageBodyView mdv = (MessageBodyView) messageArray.get(i)
							.getView();
					// TODO need to figure out why it will be null
					// when re-connect network.
					if (mdv == null) {
						continue;
					}
					VMessage vm = mdv.getMsg();
					if (vm.getFromUser().getmUserId() == user.getmUserId()) {
						mdv.updateAvatar(bm);
					}
				}
			}

		}

	};

	private Runnable mUpdateMicStatusTimer = new Runnable() {
		public void run() {
			int ratio = mRecorder.getMaxAmplitude() / 600;
			int db = 0;// 分贝
			if (ratio > 1)
				db = (int) (20 * Math.log10(ratio));
			updateVoiceVolume(db / 4);

			lh.postDelayed(mUpdateMicStatusTimer, 200);
		}
	};

	class MessageAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return messageArray.size();
		}

		@Override
		public Object getItem(int pos) {
			return messageArray.get(pos).getItemObject();
		}

		@Override
		public long getItemId(int pos) {
			return messageArray.get(pos).getItemObject().hashCode();
		}

		@Override
		public View getView(int pos, View convertView, ViewGroup v) {
			CommonAdapterItemWrapper wrapper = messageArray.get(pos);
			if (wrapper.getView() == null) {
				VMessage vm = (VMessage) wrapper.getItemObject();
				List<VMessageFileItem> fileItems = vm.getFileItems();
				if (fileItems != null) {

					adapterFileIcon(fileItems);
				}
				MessageBodyView mv = new MessageBodyView(mContext, vm, true);
				mv.setCallback(listener);
				((VMessageAdater) wrapper).setView(mv);

			}
			return wrapper.getView();
		}

	}

	class MessageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (JNIService.JNI_BROADCAST_NEW_MESSAGE.equals(intent.getAction())) {
				boolean result = queryAndAddMessage((int) intent.getExtras()
						.getLong("mid"));
				if (result) {
					offset += 1;
					// abort send down broadcast
					this.abortBroadcast();
				}
			} else if (JNIService.JNI_BROADCAST_MESSAGE_SENT_FAILED
					.equals(intent.getAction())) {
				String uuid = intent.getExtras().getString("uuid");
				for (int i = 0; i < messageArray.size(); i++) {
					VMessage vm = (VMessage) messageArray.get(i)
							.getItemObject();
					if (vm.getUUID().equals(uuid)) {
						vm.setState(VMessage.STATE_SENT_FAILED);
						MessageBodyView mdv = ((MessageBodyView) messageArray
								.get(i).getView());
						if (mdv != null) {
							mdv.updateFailedFlag(true);
						}
						// TODO update database
						break;
					}
					List<VMessageAbstractItem> items = vm.getItems();
					for (int j = 0; j < items.size(); j++) {
						VMessageAbstractItem item = items.get(j);
						if (uuid.equals(item.getUuid())) {
							if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FILE) {
								item.setState(VMessageAbstractItem.STATE_FILE_SENT_FALIED);
							} else {
								item.setState(VMessageAbstractItem.STATE_SENT_FALIED);
							}
							MessageBodyView mdv = ((MessageBodyView) messageArray
									.get(i).getView());
							if (mdv != null) {
								// TODO update database
								mdv.updateFailedFlag(true);
							}
							break;
						}

					}

				}
			}
		}

	}

	class BackendHandler extends Handler {

		public BackendHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LOAD_MESSAGE:
				List<VMessage> array = loadMessages();
				V2Log.d(TAG, "获取的消息数量" + array.size());
				if (array != null) {
					for (int i = 0; i < array.size(); i++) {
						messageArray.add(0, new VMessageAdater(array.get(i)));
					}
					V2Log.d(TAG, "当前消息集合大小" + messageArray.size());
					currentItemPos += array.size() - 1;
				}
				android.os.Message.obtain(lh, END_LOAD_MESSAGE, array)
						.sendToTarget();
				break;
			}
		}

	}

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case START_LOAD_MESSAGE:
				if (isLoading) {
					break;
				}
				android.os.Message.obtain(backEndHandler, LOAD_MESSAGE)
						.sendToTarget();
				isLoading = true;
				break;
			case END_LOAD_MESSAGE:
				V2Log.d(TAG, "currentItemPos:--" + currentItemPos);
				adapter.notifyDataSetChanged();
				scrollToPos(currentItemPos);
				isLoading = false;
				break;
			case SEND_MESSAGE:
				mChat.sendVMessage((VMessage) msg.obj, new Registrant(this,
						SEND_MESSAGE_DONE, null));
				break;
			case SEND_MESSAGE_ERROR:
				break;
			case PLAY_NEXT_UNREAD_MESSAGE:
				boolean flag = playNextUnreadMessage();
				// To last message
				if (!flag) {
					currentPlayed = null;
				}
				break;
			case REQUEST_DEL_MESSAGE:
				removeMessage((VMessage) msg.obj);
				MessageLoader.deleteMessage(mContext, (VMessage) msg.obj);
				adapter.notifyDataSetChanged();
				break;
			case FILE_STATUS_LISTENER:
				FileTransStatusIndication ind = (FileTransStatusIndication) (((AsyncResult) msg.obj)
						.getResult());
				if (ind.indType == FileTransStatusIndication.IND_TYPE_PROGRESS) {
					updateFileProgressView(
							ind.uuid,
							((FileTransProgressStatusIndication) ind).nTranedSize);
				} else if (ind.indType == FileTransStatusIndication.IND_TYPE_TRANS_ERR) {
					updateFileTransErrorView(ind.uuid);
				}
				break;
			}

		}

	}

	/**
	 * get selected file path to send remote.
	 */
	public void sendSelectedFile(String selectPath, int fileType) {

		if (!TextUtils.isEmpty(selectPath)) {

			VMessage vim = MessageBuilder.buildFileMessage(local, remote,
					selectPath, fileType);
			VMessageFileItem vfi = vim.getFileItems().get(0);
			vfi.setState(VMessageFileItem.STATE_FILE_SENDING);
			sendMessageToRemote(vim);
		}
	}

	public void adapterFileIcon(List<VMessageFileItem> fileItems) {

		for (VMessageFileItem vMessageFileItem : fileItems) {
			fileName = vMessageFileItem.getFileName();
			if (fileName.endsWith(".jpg") || fileName.endsWith(".png")
					|| fileName.endsWith(".jpeg") || fileName.endsWith(".bmp")
					|| fileName.endsWith("gif")) {
				vMessageFileItem.setFileType(1); // PICTURE = 1

			} else if (fileName.endsWith(".doc")) {
				vMessageFileItem.setFileType(2); // WORD = 2
			} else if (fileName.endsWith(".xls")) {
				vMessageFileItem.setFileType(3); // EXCEL = 3
			} else if (fileName.endsWith(".pdf")) {
				vMessageFileItem.setFileType(4); // PDF = 4
			} else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
				vMessageFileItem.setFileType(5); // PPT = 5
			} else if (fileName.endsWith(".zip") || fileName.endsWith(".rar")) {
				vMessageFileItem.setFileType(6); // ZIP = 6
			} else if (fileName.endsWith(".vsd") || fileName.endsWith(".vss")
					|| fileName.endsWith(".vst") || fileName.endsWith(".vdx")) {
				vMessageFileItem.setFileType(7); // VIS = 7
			} else if (fileName.endsWith(".mp4") || fileName.endsWith(".rmvb")
					|| fileName.endsWith(".avi") || fileName.endsWith(".3gp")) {
				vMessageFileItem.setFileType(8); // VIDEO = 8
			} else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav")
					|| fileName.endsWith(".ape") || fileName.endsWith(".wmv")) {
				vMessageFileItem.setFileType(9); // SOUND = 9
			} else {
				vMessageFileItem.setFileType(10); // OTHER = 10
			}
		}
	}

	private void startSendMoreFile() {

		for (int i = 0; i < mCheckedList.size(); i++) {

			sendSelectedFile(mCheckedList.get(i).filePath,
					mCheckedList.get(i).fileType);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBundle("saveBundle", bundle);
	}

}
