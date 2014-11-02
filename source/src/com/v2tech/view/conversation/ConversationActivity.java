
package com.v2tech.view.conversation;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
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
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.ind.FileJNIObject;
import com.V2.jni.util.V2Log;
import com.spoledge.aacplayer.AACPlayer;
import com.spoledge.aacplayer.ArrayAACPlayer;
import com.spoledge.aacplayer.ArrayDecoder;
import com.spoledge.aacplayer.Decoder;
import com.spoledge.aacplayer.PlayerCallback;
import com.v2tech.R;
import com.v2tech.media.V2Encoder;
import com.v2tech.service.AsyncResult;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.ChatService;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.FileOperationEnum;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.service.jni.FileDownLoadErrorIndication;
import com.v2tech.service.jni.FileTransCannelIndication;
import com.v2tech.service.jni.FileTransStatusIndication;
import com.v2tech.service.jni.FileTransStatusIndication.FileTransErrorIndication;
import com.v2tech.service.jni.FileTransStatusIndication.FileTransProgressStatusIndication;
import com.v2tech.service.jni.JNIResponse.Result;
import com.v2tech.service.jni.RequestChatServiceResponse;
import com.v2tech.service.jni.RequestSendMessageResponse;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.MessageUtil;
import com.v2tech.util.SPUtil;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.adapter.VMessageAdater;
import com.v2tech.view.bo.ConversationNotificationObject;
import com.v2tech.view.contacts.ContactDetail;
import com.v2tech.view.group.CrowdDetailActivity;
import com.v2tech.view.widget.CommonAdapter;
import com.v2tech.view.widget.CommonAdapter.CommonAdapterItemWrapper;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.FileInfoBean;
import com.v2tech.vo.NetworkStateCode;
import com.v2tech.vo.OrgGroup;
import com.v2tech.vo.User;
import com.v2tech.vo.UserDeviceConfig;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFaceItem;
import com.v2tech.vo.VMessageFileItem;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VMessageLinkTextItem;
import com.v2tech.vo.VMessageTextItem;

public class ConversationActivity extends Activity {

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

	private static final int VOICE_DIALOG_FLAG_RECORDING = 1;
	private static final int VOICE_DIALOG_FLAG_CANCEL = 2;
	private static final int VOICE_DIALOG_FLAG_WARING_FOR_TIME_TOO_SHORT = 3;
	private static final String TAG = "ConversationActivity";
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

	private View mSendButtonTV;

	private TextView mReturnButtonTV;

	private EditText mMessageET;

	private TextView mUserTitleTV;

	private ImageView mMoreFeatureIV;

	private View mAdditionFeatureContainer;

	private View mSmileIconButton;
	private View mSelectImageButtonIV;
	private View mSelectFileButtonIV;
	private View mVideoCallButton;
	private View mAudioCallButton;
	private View mButtonCreateMetting;

	private ImageView mAudioSpeakerIV;

	private View mShowContactDetailButton;
	private View mShowCrowdDetailButton;

	private Button mButtonRecordAudio;

	private MediaRecorder mRecorder = null;
	private ArrayAACPlayer mAACPlayer = null;
	
	//Use local AAC encoder;
	private V2Encoder mAacEncoder;

	private MessageReceiver receiver = new MessageReceiver();

	private ChatService mChat = new ChatService();
	private CrowdGroupService mGroupChat = new CrowdGroupService();

	private User local;
	private User remote;
	private int currentConversationViewType;

	private ListView mMessagesContainer;

	private LinearLayout mFaceLayout;
	private RelativeLayout mToolLayout;

	private MessageAdapter adapter;

	private List<CommonAdapterItemWrapper> messageArray = new ArrayList<CommonAdapterItemWrapper>();

	private boolean isStopped;

	private int currentItemPos = 0;

	private ArrayList<FileInfoBean> mCheckedList;

	private ConversationNotificationObject cov = null;

	private View root; // createVideoDialog的View
	private SparseArray<VMessage> messageAllID;
	private long lastMessageBodyShowTime = 0;
	private long intervalTime = 15000; // 显示消息时间状态的间隔时间
	private boolean isReLoading; // 用于onNewIntent判断是否需要重新加载界面聊天数据
	private boolean sendFile; // 用于从个人信息中传递过来的文件，只发送一次

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.activity_contact_message);
		GlobalConfig.isConversationOpen = true;
		lh = new LocalHandler();
		initModuleAndListener();
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
		// TODO for group conversation
		initExtraObject();
		initBroadcast();
		// Register listener for avatar changed
		BitmapManager.getInstance().registerBitmapChangedListener(
				avatarChangedListener);
		mChat.registerFileTransStatusListener(this.lh, FILE_STATUS_LISTENER,
				null);
		mGroupChat.registerFileTransStatusListener(this.lh, FILE_STATUS_LISTENER,
				null);
		notificateConversationUpdate(false, cov.getExtId());
		// Start animation
		this.overridePendingTransition(R.animator.nonam_scale_center_0_100,
				R.animator.nonam_scale_null);
		// initalize vioce function that showing dialog
		createVideoDialog();
	}

	/**
	 * 注册广播
	 */
	private void initBroadcast() {
		IntentFilter filter = new IntentFilter();
		// receiver the new message broadcast
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		filter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
		filter.addAction(JNIService.JNI_BROADCAST_NEW_MESSAGE);
		filter.addAction(JNIService.JNI_BROADCAST_MESSAGE_SENT_FAILED);
		filter.addAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
		filter.addAction(PublicIntent.BROADCAST_CROWD_QUIT_NOTIFICATION);
		filter.addAction(JNIService.BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION);
		filter.addAction(JNIService.JNI_BROADCAST_KICED_CROWD);
		filter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		registerReceiver(receiver, filter);
	}

	/**
	 * 初始化控件与监听器
	 */
	private void initModuleAndListener() {
		mMessagesContainer = (ListView) findViewById(R.id.conversation_message_list);
		adapter = new MessageAdapter();
		mMessagesContainer.setAdapter(adapter);
		mMessagesContainer.setOnTouchListener(mHiddenOnTouchListener);
		mMessagesContainer.setOnScrollListener(scrollListener);
		// mMessagesContainer
		// .setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

		mSendButtonTV = findViewById(R.id.message_send);
		// mSendButtonTV.setOnClickListener(sendMessageListener);
		mSendButtonTV.setOnTouchListener(sendMessageButtonListener);

		mShowContactDetailButton = findViewById(R.id.contact_detail_button);
		mShowContactDetailButton.setOnClickListener(mShowContactDetailListener);

		mShowCrowdDetailButton = findViewById(R.id.contact_crowd_detail_button);
		mShowCrowdDetailButton.setOnClickListener(mShowCrowdDetailListener);

		mMessageET = (EditText) findViewById(R.id.message_text);
		mMessageET.addTextChangedListener(mPasteWatcher);

		mReturnButtonTV = (TextView) findViewById(R.id.contact_detail_return_button);
		mReturnButtonTV.setOnTouchListener(mHiddenOnTouchListener);

		mMoreFeatureIV = (ImageView) findViewById(R.id.contact_message_plus);
		mMoreFeatureIV.setOnClickListener(moreFeatureButtonListenr);

		mSmileIconButton = findViewById(R.id.message_smile_icon_layout);
		mSmileIconButton.setOnClickListener(mSmileIconListener);

		mSelectImageButtonIV = findViewById(R.id.contact_message_send_image_button_layout);
		mSelectImageButtonIV.setOnClickListener(selectImageButtonListener);

		mSelectFileButtonIV = findViewById(R.id.contact_message_send_file_button_layout);
		mSelectFileButtonIV.setOnClickListener(mfileSelectionButtonListener);

		mVideoCallButton = findViewById(R.id.contact_message_video_call_button_layout);
		mVideoCallButton.setOnClickListener(mVideoCallButtonListener);

		mAudioCallButton = findViewById(R.id.contact_message_audio_call_button_layout);
		mAudioCallButton.setOnClickListener(mAudioCallButtonListener);

		mAudioSpeakerIV = (ImageView) findViewById(R.id.contact_message_speaker);
		mAudioSpeakerIV.setOnClickListener(mMessageTypeSwitchListener);

		mButtonRecordAudio = (Button) findViewById(R.id.message_button_audio_record);
		mButtonRecordAudio.setOnTouchListener(mButtonHolderListener);

		mButtonCreateMetting = findViewById(R.id.contact_message_create_metting_button_layout);
		mButtonCreateMetting.setOnClickListener(mButtonCreateMettingListener);

		mAdditionFeatureContainer = findViewById(R.id.contact_message_sub_feature_ly);

		mUserTitleTV = (TextView) findViewById(R.id.message_user_title);

		mFaceLayout = (LinearLayout) findViewById(R.id.contact_message_face_item_ly);
		mToolLayout = (RelativeLayout) findViewById(R.id.contact_message_sub_feature_ly_inner);
	}

	/**
	 * 用于接收从个人信息传递过来的文件
	 */
	private void initSendFile() {
		mCheckedList = this.getIntent().getParcelableArrayListExtra(
				"checkedFiles");
		if (mCheckedList != null && mCheckedList.size() > 0) {
			startSendMoreFile();
		}
	}

	private Dialog mVoiceDialog = null;
	private ImageView mVolume;
	private View mSpeakingLayout;
	private View mPreparedCancelLayout;
	private View mWarningLayout;

	/**
	 * 初始化录音留言对话框
	 */
	public void createVideoDialog() {
		mVoiceDialog = new Dialog(mContext, R.style.MessageVoiceDialog);
		mVoiceDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		LayoutInflater flater = LayoutInflater.from(mContext);
		root = flater.inflate(R.layout.message_voice_dialog, null);
		mVoiceDialog.setContentView(root);
		mVolume = (ImageView) root
				.findViewById(R.id.message_voice_dialog_voice_volume);
		mSpeakingLayout = root
				.findViewById(R.id.message_voice_dialog_listening_container);
		mPreparedCancelLayout = root
				.findViewById(R.id.message_voice_dialog_cancel_container);
		mWarningLayout = root
				.findViewById(R.id.message_voice_dialog_warning_container);
		mVoiceDialog.setCancelable(true);
		mVoiceDialog.setCanceledOnTouchOutside(false);
		mVoiceDialog.setOwnerActivity(this);
		mVoiceDialog.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface arg0, int arg1, KeyEvent arg2) {
				if (arg1 == KeyEvent.KEYCODE_BACK) {
					breakRecording();
					return true;
				}
				return false;
			}
		});
		mVoiceDialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				mButtonRecordAudio
						.setText(R.string.contact_message_button_send_audio_msg);
			}
		});
		root.setVisibility(View.INVISIBLE);
	}

	@Override
	protected void onStart() {
		super.onStart();
		V2Log.e(TAG, "entry onStart....");
		if (!mIsInited && !mLoadedAllMessages) {
			android.os.Message m = android.os.Message.obtain(lh,
					START_LOAD_MESSAGE);
			lh.sendMessageDelayed(m, 500);
		}

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.cancel(PublicIntent.MESSAGE_NOTIFICATION_ID);
		isStopped = false;
		// recover record all flag
		starttime = 0;
		lastTime = 0;
		realRecoding = false;
		cannelRecoding = false;
		mButtonRecordAudio
				.setText(R.string.contact_message_button_send_audio_msg);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (pending) {
			pending = false;
			scrollToBottom();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		V2Log.d(TAG, "entry onPause");
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		V2Log.d(TAG, "entry onBackPressed");
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "entry onStop....");
		isStopped = true;
		breakRecording();
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
		V2Log.e(TAG, "entry onDestroy....");
		this.unregisterReceiver(receiver);
		GlobalConfig.isConversationOpen = false;
		ConversationUpdateFileState.getInstance().executeUpdate(messageArray);
		checkMessageEmpty();
		finishWork();
	}

	private void checkMessageEmpty() {
		
		boolean isDelete = false;
		ConversationNotificationObject obj = null;
		if (currentConversationViewType == V2GlobalEnum.GROUP_TYPE_USER){
			isDelete = MessageLoader.getNewestMessage(mContext, user1Id, user2Id) == null ? true : false;
			obj = new ConversationNotificationObject(Conversation.TYPE_CONTACT,
					user2Id);
		}
		else if(currentConversationViewType == V2GlobalEnum.GROUP_TYPE_CROWD){
			isDelete = MessageLoader.getNewestGroupMessage(mContext, V2GlobalEnum.GROUP_TYPE_CROWD, groupId) == null ? true : false;
			obj = new ConversationNotificationObject(Conversation.TYPE_CONTACT,
					groupId);
		}
		
		if(isDelete){
			Intent i = new Intent(PublicIntent.REQUEST_UPDATE_CONVERSATION);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.putExtra("obj", obj);
			i.putExtra("isFresh", false);
			i.putExtra("isDelete", true);
			mContext.sendBroadcast(i);
		}
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
		mGroupChat.unRegisterFileTransStatusListener(this.lh,
				FILE_STATUS_LISTENER, null);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		V2Log.e(TAG, "entry onNewIntent....");
		ConversationNotificationObject tempCov = intent
				.getParcelableExtra("obj");
		if (tempCov != null)
			cov = tempCov;
		else
			cov = new ConversationNotificationObject(Conversation.TYPE_CONTACT,
					1);
		if (!isReLoading) {
			V2Log.e(TAG, "entry onNewIntent , reloading chating datas...");
			user2Id = 0;
			groupId = 0;
			remote = null;
			initConversationInfos();
			mIsInited = false;
			mLoadedAllMessages = false;
			currentItemPos = 0;
			offset = 0;
			notificateConversationUpdate(false, cov.getExtId(), false);
		}
	}

	private void initExtraObject() {

		Bundle bundle = this.getIntent().getExtras();
		if (bundle != null) {
			cov = (ConversationNotificationObject) bundle.get("obj");
			if (cov == null)
				cov = getIntent().getParcelableExtra("obj");
		} 
			
		if (bundle == null || cov == null){
			V2Log.e(TAG, "start activity was woring , please check given ConversationNotificationObject");
			cov = new ConversationNotificationObject(Conversation.TYPE_CONTACT,
					1);
		}
		
		messageAllID = new SparseArray<VMessage>();
		initConversationInfos();
	}

	private void initConversationInfos() {
		messageArray.clear();
		messageAllID.clear();
		user1Id = GlobalHolder.getInstance().getCurrentUserId();
		local = GlobalHolder.getInstance().getUser(user1Id);
		if (cov.getType() == Conversation.TYPE_CONTACT) {
			currentConversationViewType = V2GlobalEnum.GROUP_TYPE_USER;
			user2Id = cov.getExtId();
			remote = GlobalHolder.getInstance().getUser(user2Id);
			if (remote == null) {
				remote = new User(user2Id);
			}
			
			if(!TextUtils.isEmpty(remote.getNickName()))
				mUserTitleTV.setText(remote.getNickName());
			else
				mUserTitleTV.setText(remote.getName());
			mButtonCreateMetting.setVisibility(View.GONE);
			mVideoCallButton.setVisibility(View.VISIBLE);
			mAudioCallButton.setVisibility(View.VISIBLE);
			mShowContactDetailButton.setVisibility(View.VISIBLE);
			mButtonCreateMetting.setVisibility(View.GONE);
			mShowCrowdDetailButton.setVisibility(View.GONE);
			mButtonCreateMetting.setVisibility(View.GONE);
		} else if (cov.getType() == Conversation.TYPE_GROUP) {
			currentConversationViewType = V2GlobalEnum.GROUP_TYPE_CROWD;
			groupId = cov.getExtId();
			CrowdGroup crowdGroup = (CrowdGroup) GlobalHolder.getInstance()
					.getGroupById(V2GlobalEnum.GROUP_TYPE_CROWD, groupId);
			mVideoCallButton.setVisibility(View.GONE);
			mAudioCallButton.setVisibility(View.GONE);
			mShowContactDetailButton.setVisibility(View.GONE);
			mButtonCreateMetting.setVisibility(View.VISIBLE);
			mShowCrowdDetailButton.setVisibility(View.VISIBLE);
			mUserTitleTV.setText(crowdGroup.getName());
		} else if (cov.getType() == V2GlobalEnum.GROUP_TYPE_DEPARTMENT) {
			currentConversationViewType = V2GlobalEnum.GROUP_TYPE_DEPARTMENT;
			groupId = cov.getExtId();
			OrgGroup departmentGroup = (OrgGroup) GlobalHolder.getInstance()
					.getGroupById(V2GlobalEnum.GROUP_TYPE_DEPARTMENT, groupId);
			mVideoCallButton.setVisibility(View.GONE);
			mAudioCallButton.setVisibility(View.GONE);
			mShowContactDetailButton.setVisibility(View.GONE);
			mSelectFileButtonIV.setVisibility(View.GONE);
			mButtonCreateMetting.setVisibility(View.VISIBLE);
			mUserTitleTV.setText(departmentGroup.getName());
		}
	}

	private void scrollToBottom() {
		mMessagesContainer.post(new Runnable() {

			@Override
			public void run() {
				mMessagesContainer.setSelection(messageArray.size() - 1);
			}

		});
	}

	private void scrollToPos(final int pos) {
		if (pos < 0 || pos >= messageArray.size()) {
			V2Log.d(TAG, "参数pos不合法 :" + pos);
			return;
		}
		mMessagesContainer.setSelection(pos);
		// 次为了解决setSelection无效的问题，虽然能解决，但会造成界面卡顿。直接setSelection而不notifyDataSetChanged即可
		// mMessagesContainer.post(new Runnable() {
		//
		// @Override
		// public void run() {
		// mMessagesContainer.setSelection(pos);
		//
		// }
		//
		// });

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
					VMessageAudioItem vMessageAudioItem = items.get(0);
					listener.requestPlayAudio(null, vm, vMessageAudioItem);
					((MessageBodyView) wrapper.getView()).updateUnreadFlag(
							false, vMessageAudioItem);
					((MessageBodyView) wrapper.getView()).startVoiceAnimation();
					return true;
				}
			}
		}
		return false;
	}

	private InputStream currentPlayedStream;
	private TextView tips;

	private synchronized boolean startPlaying(String fileName) {

//		mAACPlayer = new AACPlayer(fileName);
//		try {
//			if (currentPlayedStream != null) {
//				currentPlayedStream.close();
//			}
//			// currentPlayedStream = new FileInputStream(new File(fileName));
//			mAACPlayer.play();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
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
			//mAACPlayer.release();
			mAACPlayer.stop();
			mAACPlayer = null;
		}
	}

	private void showOrCloseVoiceDialog() {
		if (mVoiceDialog.isShowing()) {
			mVoiceDialog.dismiss();
			mButtonRecordAudio
					.setText(R.string.contact_message_button_send_audio_msg);
		} else {
			tips = (TextView) mSpeakingLayout
					.findViewById(R.id.message_voice_dialog_listening_container_tips);
			tips.setText(R.string.contact_message_voice_dialog_text);
			mVoiceDialog.show();
			root.setVisibility(View.VISIBLE);
			updateCancelSendVoiceMsgNotification(VOICE_DIALOG_FLAG_RECORDING);
		}
	}

	private void updateCancelSendVoiceMsgNotification(int flag) {
		if (flag == VOICE_DIALOG_FLAG_CANCEL) { // 松开手指，取消发送
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
				// tips = (TextView) mSpeakingLayout
				// .findViewById(R.id.message_voice_dialog_listening_container_tips);
				// tips.setText(R.string.contact_message_voice_dialog_text);

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
			int resId;
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

//		mAacEncoder = new AACEncoder(filePath);
//		mAacEncoder.start();
//		return true;
		
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		mRecorder.setOutputFile(filePath);
		mRecorder.setMaxDuration(60000);
		mRecorder.setAudioSamplingRate(44100);
		mRecorder.setAudioChannels(2);
		try {
			mRecorder.prepare();
			mRecorder.start();
		} catch (IOException e) {
			V2Log.e(" can not prepare media recorder ");
			return false;
		} catch (IllegalStateException e) {
			V2Log.e(" can not prepare media recorder ");
			return false;
		} catch (Exception e) {
			V2Log.e("error , can not prepare media recorder ");
			return false;
		}
		return true;
		
	}

	private void stopRecording() {
//		mAacEncoder.stop();
//		//FIXME should delay some millicseconds 
//		if (mAacEncoder.getState() != MediaState.NORMAL
//				&& mAacEncoder.getState() != MediaState.STOPPED) {
//			V2Log.e("=========recording file error " +mAacEncoder.getState());
//			
//		}
//		mAacEncoder.release();
		
		mRecorder.stop();
		mRecorder.reset();
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

	private void startVoiceCall() {
		Intent iv = new Intent();
		iv.addCategory(PublicIntent.DEFAULT_CATEGORY);
		iv.setAction(PublicIntent.START_P2P_CONVERSACTION_ACTIVITY);
		iv.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		iv.putExtra("uid", user2Id);
		iv.putExtra("is_coming_call", false);
		iv.putExtra("voice", true);
		mContext.startActivity(iv);
	}

	private void startVideoCall() {
		Intent iv = new Intent();
		iv.addCategory(PublicIntent.DEFAULT_CATEGORY);
		iv.setAction(PublicIntent.START_P2P_CONVERSACTION_ACTIVITY);
		iv.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		iv.putExtra("uid", user2Id);
		iv.putExtra("is_coming_call", false);
		iv.putExtra("voice", false);
		UserDeviceConfig udc = GlobalHolder.getInstance().getUserDefaultDevice(
				user2Id);
		if (udc != null) {
			iv.putExtra("device", udc.getDeviceID());
		} else {
			iv.putExtra("device", "");
		}
		mContext.startActivity(iv);
	}

	private PlayerCallback mAACPlayerCallback = new PlayerCallback() {

		@Override
		public void playerStarted() {
			currentPlayed.getAudioItems().get(0).setPlaying(true);
			runOnUiThread(new Runnable() {

				@Override
				public void run() {

					for (int i = 0; i < messageArray.size(); i++) {
						CommonAdapterItemWrapper wrapper = messageArray.get(i);
						VMessage vm = (VMessage) wrapper.getItemObject();
						if (vm == currentPlayed) {
							((MessageBodyView) wrapper.getView())
									.startVoiceAnimation();
						}
					}
				}
			});
			V2Log.i(TAG,
					"更改正在播放的audioItem标识为true , id is ：" + currentPlayed.getId());
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

			if (currentFlag) {
				currentFlag = false;
				return;
			}

			if (currentPlayed != null
					&& currentPlayed.getAudioItems().size() > 0) {
				currentPlayed.getAudioItems().get(0).setPlaying(false);
				runOnUiThread(new Runnable() {

					@Override
					public void run() {

						for (int i = 0; i < messageArray.size(); i++) {
							CommonAdapterItemWrapper wrapper = messageArray
									.get(i);
							VMessage vm = (VMessage) wrapper.getItemObject();
							if (vm == currentPlayed) {
								((MessageBodyView) wrapper.getView())
										.stopVoiceAnimation();
							}
						}
					}
				});
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
			if (action == MotionEvent.ACTION_UP) {
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
							mToolLayout.setVisibility(View.VISIBLE);
							mFaceLayout.setVisibility(View.GONE);
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

	private String fileName = null;
	private long starttime = 0; // 记录真正开始录音时的开始时间
	private long lastTime = 0; // 记录每次录音时的当前时间(毫秒值) ， 用于判断用户点击录音的频率
	private boolean realRecoding;
	private boolean cannelRecoding;
	private boolean timeOutRecording;
	private boolean breakRecord;
	private OnTouchListener mButtonHolderListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View view, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				cannelRecoding = false;
				showOrCloseVoiceDialog();
				stopCurrentAudioPlaying();
				long currentTime = System.currentTimeMillis();
				Log.e(TAG, (currentTime - lastTime) + "");
				lh.postDelayed(preparedRecoding, 250);
				if (currentTime - lastTime < 250) {
					V2Log.d(TAG, "间隔太短，取消录音");
				}
				lastTime = currentTime;
				break;
			case MotionEvent.ACTION_MOVE:
				Rect r = new Rect();
				view.getDrawingRect(r);
				// check if touch position out of button than cancel send voice
				// message
				if (timeOutRecording) {
					mButtonRecordAudio
							.setText(R.string.contact_message_button_send_audio_msg);
				} else {
					if (r.contains((int) event.getX(), (int) event.getY())) {
						updateCancelSendVoiceMsgNotification(VOICE_DIALOG_FLAG_RECORDING);
					} else {
						updateCancelSendVoiceMsgNotification(VOICE_DIALOG_FLAG_CANCEL);
					}
				}
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				// audio message send by 计时器
				if (timeOutRecording) {
					V2Log.d(TAG,
							"audio message send by 计时器，ignore the up event once");
					timeOutRecording = false;
					return true;
				}
				// entry normal process , stop recording state 进入正常结束流程
				if (realRecoding) {
					// stop recording
					stopRecording();
					// 计算录音时间
					long seconds = (System.currentTimeMillis() - starttime);
					// recover all flag 复原标记位
					lastTime = 0;
					starttime = 0;
					realRecoding = false;
					// Remove timer
					lh.removeCallbacks(mUpdateMicStatusTimer);
					lh.removeCallbacks(timeOutMonitor);
					// recover button show state
					mButtonRecordAudio
							.setText(R.string.contact_message_button_send_audio_msg);
					// check if touch position out of button than cancel send
					// voice message
					Rect rect = new Rect();
					view.getDrawingRect(rect);
					if (rect.contains((int) event.getX(), (int) event.getY())
							&& seconds > 1500) {
						// send
						VMessage vm = MessageBuilder.buildAudioMessage(
								cov.getType(), groupId, local, remote,
								fileName, (int) (seconds / 1000));
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
						// delete audio file
						File f = new File(fileName);
						f.delete();
					}
					fileName = null;
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

					if (mTimer != null) {
						V2Log.d(TAG, "时间没到，手动停止，恢复原状");
						mTimer.cancel();
						mTimer.purge();
						mTimer = null;
						count = 11;
					} else {
						lh.removeCallbacks(mUpdateSurplusTime);
					}

				} else { // beacuse click too much quick , stop recording..
					// 此判断是为了防止对话框叠加
					if (!breakRecord) {
						cannelRecoding = true;
						Log.d(TAG, "由于间隔太短，显示short对话框");
						lh.removeCallbacks(preparedRecoding);
						updateCancelSendVoiceMsgNotification(VOICE_DIALOG_FLAG_WARING_FOR_TIME_TOO_SHORT);
						showOrCloseVoiceDialog();
					} else
						breakRecord = false;
				}
				break;
			}
			return true;
		}
	};

	private Runnable preparedRecoding = new Runnable() {
		@Override
		public void run() {

			if (!cannelRecoding) {
				realRecoding = true;
				fileName = GlobalConfig.getGlobalAudioPath() + "/"
						+ UUID.randomUUID().toString() + ".aac";
				boolean resultReocrding = startReocrding(fileName);
				if (resultReocrding) {
					starttime = System.currentTimeMillis();
					// Start update db for voice
					lh.postDelayed(mUpdateMicStatusTimer, 200);
					// Start timer
					lh.postDelayed(timeOutMonitor, 59 * 1000);
					// start timer for prompt surplus time
					lh.postDelayed(mUpdateSurplusTime, 48 * 1000);
				} else
					breakRecording();
			}
		}
	};

	private int count = 11;
	private Timer mTimer;
	private Runnable mUpdateSurplusTime = new Runnable() {

		@Override
		public void run() {
			V2Log.d(TAG, "entry surplus time ...");
			mTimer = new Timer();
			mTimer.schedule(new TimerTask() {

				@Override
				public void run() {

					runOnUiThread(new Runnable() {

						public void run() {
							if (count == 0) {
								V2Log.d(TAG, "time is over.");
								mTimer.cancel();
								mTimer.purge();
								mTimer = null;
								count = 11;
								return;
							}
							String str = mContext.getText(
									R.string.contact_message_tips_rest_seconds)
									.toString();
							str = str.replace("[]", (count - 1) + "");
							tips.setText(str);
							count--;
						}
					});
				}
			}, 0, 1000);
		}
	};

	private Runnable timeOutMonitor = new Runnable() {

		@Override
		public void run() {
			stopRecording();
			// send
			timeOutRecording = true;
			realRecoding = false;
			VMessage vm = MessageBuilder.buildAudioMessage(cov.getType(),
					groupId, local, remote, fileName, 60);
			// Send message to server
			sendMessageToRemote(vm);

			starttime = 0;
			fileName = null;
			lh.removeCallbacks(mUpdateMicStatusTimer);
			lh.removeCallbacks(mUpdateSurplusTime);
			showOrCloseVoiceDialog();
		}
	};

	private Runnable initTouchTimeOut = new Runnable() {
		@Override
		public void run() {

			breakRecording();
		}
	};

	/**
	 * 异常终止录音
	 */
	private void breakRecording() {

		if (mRecorder != null && realRecoding == true) {
			breakRecord = true;
			lastTime = 0;
			starttime = 0;
			realRecoding = false;
			// Hide voice dialog
			showOrCloseVoiceDialog();
			stopRecording();
			starttime = 0;
			if (fileName != null) {
				File f = new File(fileName);
				f.delete();
				fileName = null;
			}
			lh.removeCallbacks(mUpdateMicStatusTimer);
			lh.removeCallbacks(timeOutMonitor);
		}
	}

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
				mToolLayout.setVisibility(View.VISIBLE);
				mFaceLayout.setVisibility(View.GONE);
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
			Intent intent = new Intent(ConversationActivity.this,
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
					iv.setPadding(20, 10, 0, 10);
					iv.setScaleType(ScaleType.FIT_XY);
					iv.setOnClickListener(mFaceSelectListener);
					mFaceLayout.addView(iv);
				}
			}
			if (mFaceLayout.getVisibility() == View.GONE) {
				mToolLayout.setVisibility(View.INVISIBLE);
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
				Intent intent = new Intent(ConversationActivity.this,
						ConversationSelectFileEntry.class);
				startActivityForResult(intent, RECEIVE_SELECTED_FILE);
			} else {

				Toast.makeText(mContext, "当前网络不可用，请稍候再试。", Toast.LENGTH_SHORT)
						.show();
			}
		}
	};

	private View.OnClickListener mVideoCallButtonListener = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if (!SPUtil.checkCurrentAviNetwork(mContext)) {
				Toast.makeText(mContext,
						R.string.conversation_no_network_notification,
						Toast.LENGTH_SHORT).show();
				return;
			}
			startVideoCall();
		}
	};

	private View.OnClickListener mAudioCallButtonListener = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			startVoiceCall();
		}
	};

	private View.OnClickListener mButtonCreateMettingListener = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			Intent i = new Intent(PublicIntent.START_CONFERENCE_CREATE_ACTIVITY);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.putExtra("gid", groupId);
			startActivity(i);
		}
	};

	private int flagCount = 0;
	private TextWatcher mPasteWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable edit) {
			String[] split = edit.toString().split("/:");
			for (String string : split) {
				if (string.contains(":")) {
					num++;
				}
			}
			if (num > 10 && split.length > 10) {
				Toast.makeText(mContext,
						R.string.error_contact_message_face_too_much,
						Toast.LENGTH_SHORT).show();
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < split.length; i++) {

					if (flagCount == 10) {
						flagCount = 0;
						break;
					}

					if (split[i].contains(":")) {
						flagCount++;
						sb.append("/:");
						if (flagCount == 10 && split[i].split(" ").length > 1) {
							sb.append(split[i].split(" ")[0]);
						} else
							sb.append(split[i]);
					} else {
						sb.append(split[i]);
					}

				}
				V2Log.e(TAG, "stringbuilder : " + sb.toString().trim());
				edit.clear();
				edit.append(sb.toString().trim());
				mMessageET.setSelection(sb.toString().trim().length());
				sb.delete(0, sb.length());
			}
			num = 0;

			mMessageET.removeTextChangedListener(this);
			int start = -1, end;
			int index = 0;
			V2Log.e(TAG, "输入的字符串：" + edit.toString());
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

		private int num;

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			// Editable edit = mMessageET.getText();
			// String str = edit.toString() + " ";
			// String str = arg0.toString() + " ";
			// String[] len = str.split("((/:){1}(.){1}(:/){1})");
			// String[] len = str.split("/:");
			// if (len.length > 10) {
			// Toast.makeText(mContext,
			// R.string.error_contact_message_face_too_much,
			// Toast.LENGTH_SHORT).show();
			// StringBuilder sb = new StringBuilder();
			// for (int i = 0; i < 10; i++) {
			// sb.append(len[i]);
			// }
			// mMessageET.setText(sb.toString().trim());
			// mMessageET.setSelection(sb.toString().trim().length());
			// return;
			// }
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
			VMessage vim = MessageBuilder.buildImageMessage(cov.getType(),
					groupId, local, remote, filePath);
			// Send message to server
			sendMessageToRemote(vim);
		} else if (requestCode == RECEIVE_SELECTED_FILE) {
			if (data != null) {
				mCheckedList = data.getParcelableArrayListExtra("checkedFiles");
				if (mCheckedList == null && mCheckedList.size() <= 0)
					return;

				switch (currentConversationViewType) {
				case V2GlobalEnum.GROUP_TYPE_CROWD:
					
					for (FileInfoBean bean : mCheckedList) {
						if (bean == null || TextUtils.isEmpty(bean.filePath))
							continue;
						
						VMessage vm = MessageBuilder.buildFileMessage(cov.getType(), groupId,
								local, remote, bean);
						
						// // Save message
						vm.setmXmlDatas(vm.toXml());
						vm.setDate(new Date(GlobalConfig.getGlobalServerTime()));

						MessageBuilder.saveMessage(this, vm);
						MessageBuilder.saveFileVMessage(this, vm);
						
						addMessageToContainer(vm);
						// send notification
						notificateConversationUpdate(true, vm.getId());
					}
					
					Intent intent = new Intent(this, FileService.class);
					intent.putExtra("gid", groupId);
					intent.putParcelableArrayListExtra("uploads", mCheckedList);
					startService(intent);
					break;
				case V2GlobalEnum.GROUP_TYPE_USER:
						startSendMoreFile();
						mCheckedList = null;
					break;
				}
			}
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
			Toast.makeText(mContext, "聊天信息不能为空", Toast.LENGTH_SHORT).show();
			return;
		}
		content = removeEmoji(content);
		if (remote == null) {
			remote = new User(user2Id);
		}
		// 如果user2Id为0，则说明为群组聊天
		VMessage vm = new VMessage(cov.getType(), this.groupId, local, remote,
				new Date(GlobalConfig.getGlobalServerTime()));
		String[] array = content.split("\n");
		for (int i = 0; i < array.length; i++) {
			String str = array[i];
			int len = str.length();
			if (str.length() <= 4) {
				VMessageAbstractItem vai = new VMessageTextItem(vm, str);
				vai.setNewLine(true);
				continue;
			}

			int emojiStart = -1, end, strStart = 0;
			int index = 0;
			Pattern pattern = Pattern
					.compile("(http://|https://|www\\.){1}[^\u4e00-\u9fa5\\s]*?\\.(com|net|cn|me|tw|fr|html){1}(/[^\u4e00-\u9fa5\\s]*){0,1}");
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
					}
				}

				int lastStart = 0;
				int lastEnd = 0;
				boolean firstMather = true;
				// check if exist last string
				if (index == len - 1 && strStart <= index) {
					String strTextContent = str.substring(strStart, len);
					Matcher matcher = pattern.matcher(strTextContent);
					while (matcher.find()) {
						String url = matcher.group(0);
						V2Log.e(TAG, "从文本内容检测到网址：" + url);
						// 检测网址前面是否有文本内容
						if (firstMather == true) {
							firstMather = false;
							if (matcher.start(0) != strStart) {

								VMessageTextItem vti = new VMessageTextItem(vm,
										strTextContent.substring(strStart,
												matcher.start(0)));
								// If strStart is 0 means string at new line
								if (strStart == 0) {
									vti.setNewLine(true);
								}
							}
							new VMessageLinkTextItem(vm, url, url);
						} else {
							if (matcher.start(0) != lastEnd) {
								VMessageTextItem vti = new VMessageTextItem(vm,
										strTextContent.substring(
												matcher.end(0) + 1, lastStart));
								// If strStart is 0 means string at new line
								if (matcher.end(0) + 1 == 0) {
									vti.setNewLine(true);
								}
							}
						}
						lastStart = matcher.start(0);
						lastEnd = matcher.end(0);
					}

					if (strTextContent.length() != lastEnd) {
						String lastText = strTextContent.substring(lastEnd,
								strTextContent.length());
						VMessageTextItem vti = new VMessageTextItem(vm,
								lastText);
						// If strStart is 0 means string at new line
						if (lastEnd == 0) {
							vti.setNewLine(true);
						}
					}
					strStart = index;
				}
				index++;
			}
		}
		mMessageET.setText("");
		// Send message to server
		sendMessageToRemote(vm);
	}
	
	private void sendMessageToRemote(VMessage vm) {
		sendMessageToRemote(vm, true);
	}
	

	/**
	 * send chat message to remote
	 * @param vm 
	 * @param isFresh 是否通知ConversationTabFragment更新
	 */
	private void sendMessageToRemote(VMessage vm, boolean isFresh) {
		// // Save message
		vm.setmXmlDatas(vm.toXml());
		vm.setDate(new Date(GlobalConfig.getGlobalServerTime()));

		MessageBuilder.saveMessage(this, vm);
		MessageBuilder.saveFileVMessage(this, vm);
		MessageBuilder.saveBinaryVMessage(this, vm);
		
		Message.obtain(lh, SEND_MESSAGE, vm).sendToTarget();
		addMessageToContainer(vm);
		// send notification
		notificateConversationUpdate(isFresh, vm.getId());
	}
	
	/**
	 * FIXME optimize code
	 * 去除IOS自带表情
	 * @param content
	 * @return
	 */
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

	private void notificateConversationUpdate(boolean isFresh, long msgID) {
		notificateConversationUpdate(isFresh, msgID, false);
	}

	/**
	 * 通知ConversationTabFragment 更新会话列表
	 * 
	 * @param isFresh
	 *            false msgID为null，但需要刷新会话列表；ture 正常刷新
	 * @param msgID
	 *            最新消息ID
	 */
	private void notificateConversationUpdate(boolean isFresh, long msgID,
			boolean isDeleteConversation) {

		Intent i = new Intent(PublicIntent.REQUEST_UPDATE_CONVERSATION);
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);
		ConversationNotificationObject obj = null;
		switch (currentConversationViewType) {
		case Conversation.TYPE_CONTACT:
			obj = new ConversationNotificationObject(Conversation.TYPE_CONTACT,
					user2Id);
			break;
		case Conversation.TYPE_GROUP:
			obj = new ConversationNotificationObject(Conversation.TYPE_GROUP,
					groupId);
			break;
		case Conversation.TYPE_DEPARTMENT:
			obj = new ConversationNotificationObject(
					Conversation.TYPE_DEPARTMENT, groupId);
			break;
		default:
			throw new RuntimeException(
					"ConversationView notificateConversationUpdate --> invaild currentConversationViewType : "
							+ currentConversationViewType);
		}
		obj.setMsgID(msgID);
		i.putExtra("obj", obj);
		i.putExtra("isFresh", isFresh);
		i.putExtra("isDelete", isDeleteConversation);
		mContext.sendBroadcast(i);
	}

	private void addMessageToContainer(VMessage msg) {
		// make offset
		offset++;

		messageArray.add(new VMessageAdater(msg));
		scrollToBottom();

		judgeShouldShowTime(msg);
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
			isUPScroll = first < lastFirst;
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
			List<VMessageImageItem> imageItems = v.getImageItems();
			VMessageImageItem imageItem = null;
			if (imageItems != null && imageItems.size() > 0) {
				imageItem = v.getImageItems().get(0);
				Intent i = new Intent();
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.setAction(PublicIntent.START_VIDEO_IMAGE_GALLERY);
				i.putExtra("uid1", user1Id);
				i.putExtra("uid2", user2Id);
				i.putExtra("cid", v.getUUID());
				if (imageItem != null)
					i.putExtra("imageID", imageItem.getUuid());
				// type 0: is not group image view
				// type 1: group image view
				i.putExtra("type", currentConversationViewType);
				i.putExtra("gid", groupId);
				mContext.startActivity(i);
				imageItem = null;
			}

			List<VMessageLinkTextItem> linkItems = v.getLinkItems();
			VMessageLinkTextItem linkItem = null;
			if (linkItems != null && linkItems.size() > 0) {
				linkItem = v.getLinkItems().get(0);
				Intent intent = new Intent();
				intent.setAction("android.intent.action.VIEW");
				Uri content_url = Uri.parse(linkItem.getUrl());
				intent.setData(content_url);
				startActivity(intent);
			}
		}

		@Override
		public void requestPlayAudio(View v, VMessage vm, VMessageAudioItem vai) {
			if (vai != null && vai.getAudioFilePath() != null) {
				currentPlayed = vm;
				V2Log.i(TAG, "start play , currentPlayingAudio id is : "
						+ currentPlayed.getId());
				startPlaying(vai.getAudioFilePath());
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
			notificateConversationUpdate(true, v.getId());
		}

		@Override
		public void requestDelMessage(VMessage v) {
			List<VMessageAbstractItem> items = v.getItems();
			for (int i = 0; i < items.size(); i++) {
				VMessageAbstractItem item = items.get(i);
				if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FILE) {
					VMessageFileItem vfi = (VMessageFileItem) item;

					switch (item.getState()) {
					case VMessageAbstractItem.STATE_FILE_SENDING:
						mChat.updateFileOperation(vfi,
								FileOperationEnum.OPERATION_CANCEL_SENDING,
								null);
						break;
					case VMessageAbstractItem.STATE_FILE_DOWNLOADED:
						mChat.updateFileOperation(vfi,
								FileOperationEnum.OPERATION_CANCEL_DOWNLOADING,
								null);
						break;
					default:
						break;
					}
				}
			}
			Message.obtain(lh, REQUEST_DEL_MESSAGE, v).sendToTarget();
		}

		@Override
		public void requestStopAudio(View v, VMessage vm, VMessageAudioItem vai) {
			V2Log.i(TAG,
					"request current playing audioItem 停止 , id is ："
							+ vai.getId());
			vai.setPlaying(false);
			stopPlaying();
		}

		@Override
		public void requestDownloadFile(View v, VMessage vm,
				VMessageFileItem vfi) {
			if (vfi == null) {
				return;
			}
			mChat.updateFileOperation(vfi,
					FileOperationEnum.OPERATION_START_DOWNLOAD, null);
		}

		@Override
		public void requestPauseTransFile(View v, VMessage vm,
				VMessageFileItem vfi) {
			if (vfi == null) {
				return;
			}
			mChat.updateFileOperation(vfi,
					FileOperationEnum.OPERATION_PAUSE_SENDING, null);
		}

		@Override
		public void requestResumeTransFile(View v, VMessage vm,
				VMessageFileItem vfi) {
			if (vfi == null) {
				return;
			}
			mChat.updateFileOperation(vfi,
					FileOperationEnum.OPERATION_RESUME_SEND, null);
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
		}

		@Override
		public void requestStopOtherAudio(VMessage vm) {
			if (currentPlayed == null || currentPlayed.getId() == vm.getId()) {
				return;
			}
			V2Log.i(TAG,
					"停止了当前正在播放的currentPlayingAudio:--" + currentPlayed.getId());
			currentFlag = true;
			stopCurrentAudioPlaying();
		}
	};

	protected void stopCurrentAudioPlaying() {

		if (currentPlayed != null && currentPlayed.getAudioItems().size() > 0) {
			currentPlayed.getAudioItems().get(0).setPlaying(false);
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					for (int i = 0; i < messageArray.size(); i++) {
						CommonAdapterItemWrapper wrapper = messageArray.get(i);
						VMessage vm = (VMessage) wrapper.getItemObject();
						if (vm == currentPlayed) {
							((MessageBodyView) wrapper.getView())
									.stopVoiceAnimation();
						}
					}
				}
			});
			stopPlaying();
		}
	}

	private List<VMessage> loadMessages() {

		List<VMessage> array = null;
		switch (currentConversationViewType) {
		case V2GlobalEnum.GROUP_TYPE_USER:
			array = MessageLoader.loadMessageByPage(mContext, user1Id, user2Id,
					BATCH_COUNT, offset, Conversation.TYPE_CONTACT);
			break;
		case V2GlobalEnum.GROUP_TYPE_CROWD:
			array = MessageLoader.loadGroupMessageByPage(mContext,
					Conversation.TYPE_GROUP, groupId, BATCH_COUNT, offset);
			break;
		case V2GlobalEnum.GROUP_TYPE_DEPARTMENT:
			array = MessageLoader.loadGroupMessageByPage(mContext,
					V2GlobalEnum.GROUP_TYPE_DEPARTMENT, groupId, BATCH_COUNT,
					offset);
			break;
		default:
			break;
		}

		if (array != null) {
			offset += array.size();
			mIsInited = true;
		}
		return array;
	}

	private boolean pending = false;

	private boolean queryAndAddMessage(final int msgId) {

		if (messageAllID.get(msgId) != null) {
			Log.e(TAG, "happen erupt , the message ：" + msgId
					+ "  already save in messageArray!");
			return false;
		}

		VMessage m;
		if (currentConversationViewType == V2GlobalEnum.GROUP_TYPE_USER)
			m = MessageLoader.loadUserMessageById(mContext, user2Id, msgId);
		else
			m = MessageLoader.loadGroupMessageById(mContext,
					currentConversationViewType, groupId, msgId);
		if (m == null
				|| (m.getFromUser().getmUserId() != this.user2Id && m
						.getGroupId() == 0) || (m.getGroupId() != this.groupId)) {
			return false;
		}
		
		judgeShouldShowTime(m);
		MessageBodyView mv = new MessageBodyView(this, m, m.isShowTime());
		mv.setCallback(listener);
		VMessageAdater adater = new VMessageAdater(m);
		adater.setView(mv);
		messageArray.add(adater);
		if (!isStopped)
			this.scrollToBottom();
		else
			pending = true;

		return true;
	}

	private boolean removeMessage(VMessage vm) {
		if (vm == null) {
			return false;
		}
		for (int i = 0; i < messageArray.size(); i++) {
			VMessageAdater va = (VMessageAdater) messageArray.get(i);
			VMessage message = (VMessage) va.getItemObject();
			if (vm.getId() == message.getId()) {
				if(message.getImageItems().size() >= 0)
					va.setView(null);
				messageArray.remove(i);
				MessageLoader.deleteMessage(mContext, vm);
				List<VMessage> messagePages = MessageLoader.loadGroupMessageByPage(mContext,
						Conversation.TYPE_GROUP, groupId, 1, messageArray.size());
				if(messagePages != null && messagePages.size() > 0)
					messageArray.add(0 , new VMessageAdater(messagePages.get(0)));
				V2Log.d(TAG, "现在集合长度：" + messageArray.size());
				return true;
			}
		}
		return false;
	}

	private void updateFileProgressView(String uuid, long tranedSize,
			int progressType) {
		for (int i = 0; i < messageArray.size(); i++) {
			VMessage vm = (VMessage) messageArray.get(i).getItemObject();
			if (vm.getItems().size() > 0) {
				VMessageAbstractItem item = vm.getItems().get(0);
				if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FILE
						&& item.getUuid().equals(uuid)) {
					VMessageFileItem vfi = ((VMessageFileItem) item);

					switch (progressType) {
					case FileTransStatusIndication.IND_TYPE_PROGRESS_END:
						if (vfi.getState() == VMessageAbstractItem.STATE_FILE_SENDING) {
							vfi.setState(VMessageAbstractItem.STATE_FILE_SENT);
						} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING) {
							vfi.setState(VMessageAbstractItem.STATE_FILE_DOWNLOADED);
						}
						// int updates = MessageBuilder.updateVMessageItem(this,
						// vfi);
						// Log.e(TAG, "updates success : " + updates);
						break;
					}

					vfi.setDownloadedSize(tranedSize);
					CommonAdapterItemWrapper common = messageArray.get(i);
					MessageBodyView mv = (MessageBodyView) common.getView();
					if (mv != null) {

						mv.updateView(vfi);
					} else {
						notificateConversationUpdate(true, vm.getId());
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

					int updates = MessageBuilder
							.updateVMessageItemToSentFalied(this, vm);
					Log.e(TAG, "updates success : " + updates);

					((MessageBodyView) messageArray.get(i).getView())
							.updateView(vfi);

				}
			}
		}
	}
	
	/**
	 * update all sending or downloading file state to failed..
	 */
	public void executeUpdateFileState(){
		
		if(messageArray == null){
			V2Log.e(TAG, "executeUpdateFileState is failed ... because messageArray is null");
			return ;
		}
			
		for (int i = 0; i < messageArray.size(); i++) {
			VMessage vm = (VMessage) messageArray.get(i)
					.getItemObject();
			if (vm.getFileItems().size() > 0) {
				List<VMessageFileItem> fileItems = vm.getFileItems();
				for (int j = 0; j < fileItems.size(); j++) {
					VMessageFileItem item = fileItems.get(j);
					switch (item.getState()) {
					case VMessageAbstractItem.STATE_FILE_DOWNLOADING:
					case VMessageAbstractItem.STATE_FILE_PAUSED_DOWNLOADING:
						item.setState(VMessageFileItem.STATE_FILE_DOWNLOADED_FALIED);
						MessageBuilder.updateVMessageItemToSentFalied(
								mContext, vm);
						break;
					case VMessageAbstractItem.STATE_FILE_SENDING:
					case VMessageAbstractItem.STATE_FILE_PAUSED_SENDING:
						item.setState(VMessageFileItem.STATE_FILE_SENT_FALIED);
						MessageBuilder.updateVMessageItemToSentFalied(
								mContext, vm);
						break;
					default:
						break;
					}
					
					if(!isStopped)
					((MessageBodyView) messageArray.get(i)
							.getView()).updateView(item);
				}
			}
		}
		adapter.notifyDataSetChanged();
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

	private OnClickListener mShowCrowdDetailListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			Intent i = new Intent(mContext, CrowdDetailActivity.class);
			i.putExtra("cid", cov.getExtId());
			startActivity(i);
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
				MessageBodyView mv = new MessageBodyView(mContext, vm,
						vm.isShowTime());
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
					|| (remote != null && user.getmUserId() == remote
							.getmUserId())) {
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

//			int db = (int)mAacEncoder.getDB();
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
				MessageBodyView mv = new MessageBodyView(mContext, vm,
						vm.isShowTime());
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
				int groupType = intent.getIntExtra("groupType", -1);
				long groupID = intent.getLongExtra("groupID", -1l);
				long remoteID = intent.getLongExtra("remoteUserID", -1l);
				if (currentConversationViewType == groupType) {
					switch (currentConversationViewType) {
					case V2GlobalEnum.GROUP_TYPE_CROWD:
					case V2GlobalEnum.GROUP_TYPE_DEPARTMENT:
						isReLoading = groupID == groupId;
						break;
					case V2GlobalEnum.GROUP_TYPE_USER:
						isReLoading = remoteID == user2Id;
						break;
					}
				} else
					isReLoading = false;
				// 用于onNewIntent判断是否需要重新加载界面聊天数据，以及是否阻断广播 , true 后台
				boolean isAppBack = GlobalConfig
						.isApplicationBackground(mContext);
				if (isReLoading) {
					boolean result = queryAndAddMessage((int) intent
							.getExtras().getLong("mid"));
					if (result) {
						offset += 1;
						if (!isAppBack) {
							// abort send down broadcast
							this.abortBroadcast();
						}
					}
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
								mdv.updateFailedFlag(true);
							}
							break;
						}

					}

				}
				// handler kicked event
			} else if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_KICED_CROWD)) {
				long crowdId = intent.getLongExtra("crowd", 0);
				if (crowdId == groupId) {
					finish();
				}
			} else if ((PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION
					.equals(intent.getAction()))
					|| (PublicIntent.BROADCAST_CROWD_QUIT_NOTIFICATION
							.equals(intent.getAction()))) {
				finish();
			} else if ((JNIService.BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION
					.equals(intent.getAction()))) {

				if (currentConversationViewType == V2GlobalEnum.GROUP_TYPE_CROWD) {
					ArrayList<FileJNIObject> list = intent
							.getParcelableArrayListExtra("fileJniObjects");
					long groupID = intent.getLongExtra("groupID", -1l);
					if (list == null || list.size() <= 0 || groupID == -1l) {
						V2Log.e("ConversationView : May receive new group files failed.. get empty collection");
						return;
					}
					// 自己上传文件不提示
					if (list.get(0).user.uid == user1Id || groupID != groupId)
						return;
					for (FileJNIObject fileJNIObject : list) {
						User user = GlobalHolder.getInstance().getUser(
								list.get(0).user.uid);
						VMessage vm = new VMessage(cov.getType(), groupId, user,
								null, new Date(GlobalConfig.getGlobalServerTime()));
						VMessageFileItem item = new VMessageFileItem(vm,
								fileJNIObject.fileName , fileJNIObject.fileType);
						item.setFileSize(fileJNIObject.fileSize);
						item.setUuid(fileJNIObject.fileId);
						item.setState(VMessageFileItem.STATE_FILE_SENT);
						addMessageToContainer(vm);
						// send notification
						notificateConversationUpdate(true, vm.getId());
					}
				}
			} else if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION)) {
				NetworkStateCode code = (NetworkStateCode) intent.getExtras()
						.get("state");
				if (code != NetworkStateCode.CONNECTED) {
					executeUpdateFileState();
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
				if (array != null) {
					V2Log.d(TAG, "获取的消息数量" + array.size());
					// 设置VMessage是否应该显示时间
					for (int i = 0; i < array.size(); i++) {
						long currentMessage = array.get(i).getmDateLong();

						if (i + 1 == array.size()) {
							array.get(i).setShowTime(true);
							break;
						}
						long lastMessage = array.get(i + 1).getmDateLong();
						if (currentMessage - lastMessage > intervalTime)
							array.get(i).setShowTime(true);
						else
							array.get(i).setShowTime(false);
					}
					for (int i = 0; i < array.size(); i++) {
						if (messageAllID.get((int) array.get(i).getId()) != null) {
							Log.e(TAG, "happen erupt , the message ："
									+ (int) array.get(i).getId()
									+ "  already save in messageArray!");
							continue;
						}
						messageAllID.append((int) array.get(i).getId(),
								array.get(i));
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
				scrollToPos(currentItemPos);
				isLoading = false;
				// 处理从个人信息传递过来的文件
				if (!sendFile) {
					sendFile = true;
					initSendFile();
				}
				break;
			case SEND_MESSAGE:
				mChat.sendVMessage((VMessage) msg.obj, new MessageListener(this,
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
				VMessage message = (VMessage) msg.obj;
				removeMessage(message);
				adapter.notifyDataSetChanged();
				// Update conversation
				notificateConversationUpdate(false, message.getId());
				break;
			case FILE_STATUS_LISTENER:
				FileTransStatusIndication ind = (FileTransStatusIndication) (((AsyncResult) msg.obj)
						.getResult());
				if (ind.indType == FileTransStatusIndication.IND_TYPE_PROGRESS) {
					FileTransProgressStatusIndication progress = (FileTransProgressStatusIndication) ind;
					updateFileProgressView(
							ind.uuid,
							((FileTransProgressStatusIndication) ind).nTranedSize,
							progress.progressType);
				} else if (ind.indType == FileTransStatusIndication.IND_TYPE_DOWNLOAD_ERR) {
				} else if (ind.indType == FileTransStatusIndication.IND_TYPE_TRANS_ERR) {

					FileTransErrorIndication transError = (FileTransErrorIndication) ind;
					for (int i = 0; i < messageArray.size(); i++) {
						VMessage vm = (VMessage) messageArray.get(i)
								.getItemObject();
						if (vm.getItems().size() > 0) {
							VMessageAbstractItem item = vm.getItems()
									.get(0);
							if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FILE
									&& item.getUuid().equals(ind.uuid)) {
								VMessageFileItem vfi = ((VMessageFileItem) item);
								switch (transError.nTransType) {
								case FileDownLoadErrorIndication.TYPE_SEND:
									if (transError.errorCode == 415) {
										Toast.makeText(
												getApplicationContext(),
												"亲，不可以发送0大小的文件，抱歉...",
												Toast.LENGTH_SHORT).show();
										vfi.setDownloadedSize(0);
										vfi.setState(VMessageFileItem.STATE_FILE_SENT_FALIED);
									} else {
										V2Log.e(TAG,
												"when sending the file --"
														+ vfi.getFileName()
														+ "-- , There is an error in the process of sending was happend...error code is :"
														+ transError.errorCode);
										vfi.setDownloadedSize(0);
										vfi.setState(VMessageFileItem.STATE_FILE_SENT_FALIED);
									}
									break;
								case FileDownLoadErrorIndication.TYPE_DOWNLOAD:
									V2Log.e(TAG,
											"when downloading the file --"
													+ vfi.getFileName()
													+ "-- , There is an error in the process of sending was happend...error code is :"
													+ transError.errorCode);
									vfi.setDownloadedSize(0);
									vfi.setState(VMessageFileItem.STATE_FILE_DOWNLOADED_FALIED);
									break;
								default:
									break;
								}
								
								if(!isStopped)
								((MessageBodyView) messageArray.get(i)
										.getView()).updateView(vfi);
							}
						}
					}
				} else if (ind.indType == FileTransStatusIndication.IND_TYPE_TRANS_CANNEL) {

					FileTransCannelIndication cannelError = (FileTransCannelIndication) ind;
					for (int i = 0; i < messageArray.size(); i++) {
						VMessage vm = (VMessage) messageArray.get(i)
								.getItemObject();
						if (vm.getItems().size() > 0) {
							VMessageAbstractItem item = vm.getItems()
									.get(0);
							if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FILE
									&& item.getUuid().equals(ind.uuid)) {
								VMessageFileItem vfi = ((VMessageFileItem) item);
								V2Log.e(TAG,
										"when downloading or sending the file --"
												+ vfi.getFileName()
												+ "-- , There is an error in the process of sending was happend...error code is :"
												+ cannelError.errorCode);
								// vfi.setDownloadedSize(0);
								// vfi.setState(VMessageFileItem.);
								// int updates = MessageBuilder
								// .updateVMessageItemToSentFalied(
								// ConversationView.this, vm);
								// Log.e(TAG, "updates success : " +
								// updates);
								// ((MessageBodyView) messageArray.get(i)
								// .getView()).updateView(vfi);
							}
						}
					}
				}
				break;
			case SEND_MESSAGE_DONE:
				if (msg.obj instanceof RequestChatServiceResponse) {
					RequestChatServiceResponse code = (RequestChatServiceResponse) msg.obj;
					if (code.getCode() != RequestChatServiceResponse.ACCEPTED) {
						V2Log.e(TAG,
								"send file failed....get the return code is :"
										+ code.getCode());
						String uuid = code.getUuid();
						if (uuid != null)
							updateFileTransErrorView(uuid);
					}
				} else if (msg.obj instanceof RequestSendMessageResponse) {
					RequestSendMessageResponse code = (RequestSendMessageResponse) msg.obj;
					int res = code.getResult().value();
					if (res != Result.SUCCESS.value())
						V2Log.e(TAG,
								"send chating failed....get the return code is :"
										+ res);
				}
				break;
			}
		}

	}
	
	private void startSendMoreFile() {
		for (int i = 0; i < mCheckedList.size(); i++) {
			sendSelectedFile(mCheckedList.get(i));
		}
	}

	/**
	 * get selected file path to send remote.
	 */
	public void sendSelectedFile(FileInfoBean bean) {

		if (bean == null || TextUtils.isEmpty(bean.filePath))
			return;

		VMessage vim = MessageBuilder.buildFileMessage(cov.getType(), groupId,
				local, remote, bean);
		sendMessageToRemote(vim);
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

	/**

	 * 用于判断指定的消息VMessage对象是否应该显示时间状态
	 * 
	 * @param message
	 */
	private void judgeShouldShowTime(VMessage message) {

		if (message.getmDateLong() - lastMessageBodyShowTime < intervalTime)
			message.setShowTime(false);
		else
			message.setShowTime(true);
		lastMessageBodyShowTime = message.getmDateLong();
	}

}
