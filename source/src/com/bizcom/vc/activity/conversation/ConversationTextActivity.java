package com.bizcom.vc.activity.conversation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
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

import com.V2.jni.ind.FileJNIObject;
import com.V2.jni.util.V2Log;
import com.bizcom.bo.ConversationNotificationObject;
import com.bizcom.bo.GroupUserObject;
import com.bizcom.bo.MessageObject;
import com.bizcom.request.V2ChatRequest;
import com.bizcom.request.V2CrowdGroupRequest;
import com.bizcom.request.jni.FileTransStatusIndication;
import com.bizcom.request.jni.FileTransStatusIndication.FileTransProgressStatusIndication;
import com.bizcom.request.jni.JNIResponse.Result;
import com.bizcom.request.util.AsyncResult;
import com.bizcom.request.util.BitmapManager;
import com.bizcom.request.util.FileOperationEnum;
import com.bizcom.util.FileUitls;
import com.bizcom.util.LocalSharedPreferencesStorage;
import com.bizcom.util.MessageUtil;
import com.bizcom.vc.activity.contacts.ContactDetail;
import com.bizcom.vc.activity.crow.CrowdDetailActivity;
import com.bizcom.vc.activity.crow.CrowdFilesActivity.CrowdFileActivityType;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.MainApplication;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vc.listener.CommonCallBack;
import com.bizcom.vc.listener.CommonCallBack.CommonNotifyChatInterToReplace;
import com.bizcom.vc.listener.CommonCallBack.CommonUpdateCrowdFileStateInterface;
import com.bizcom.vc.listener.CommonCallBack.CommonUpdateMessageBodyPopupWindowInterface;
import com.bizcom.vc.listener.CommonCallBack.CrowdFileExeType;
import com.bizcom.vc.service.FileService;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vo.Conversation;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.DiscussionGroup;
import com.bizcom.vo.FileInfoBean;
import com.bizcom.vo.Group;
import com.bizcom.vo.Group.GroupType;
import com.bizcom.vo.NetworkStateCode;
import com.bizcom.vo.OrgGroup;
import com.bizcom.vo.User;
import com.bizcom.vo.UserDeviceConfig;
import com.bizcom.vo.VMessage;
import com.bizcom.vo.VMessageAbstractItem;
import com.bizcom.vo.VMessageAudioItem;
import com.bizcom.vo.VMessageFileItem;
import com.bizcom.vo.VMessageFileItem.FileType;
import com.bizcom.vo.VMessageImageItem;
import com.bizcom.vo.VMessageLinkTextItem;
import com.v2tech.R;

public class ConversationTextActivity extends Activity implements
		CommonUpdateMessageBodyPopupWindowInterface,
		CommonUpdateCrowdFileStateInterface, CommonNotifyChatInterToReplace {

	private static final String TAG = "ConversationTextActivity";
	private static final int SEND_MESSAGE_SUCCESS = 0;
	private static final int VOICE_DIALOG_FLAG_RECORDING = 1;
	private static final int VOICE_DIALOG_FLAG_CANCEL = 2;
	private static final int VOICE_DIALOG_FLAG_WARING_FOR_TIME_TOO_SHORT = 3;

	private final int START_LOAD_MESSAGE = 1;
	private final int LOAD_MESSAGE = 2;
	private final int END_LOAD_MESSAGE = 3;
	private final int SEND_MESSAGE = 4;
	private final int SEND_MESSAGE_ERROR = 6;
	private final int PLAY_NEXT_UNREAD_MESSAGE = 7;
	private final int REQUEST_DEL_MESSAGE = 8;
	private final int ADAPTER_NOTIFY = 9;
	private final int FILE_STATUS_LISTENER = 20;
	private final int RECORD_STATUS_LISTENER = 21;

	private final int BATCH_COUNT = 10;
	private final int INTERVAL_TIME = 60000;

	/**
	 * for activity result
	 */
	private static final int SELECT_PICTURE_CODE = 100;
	protected static final int RECEIVE_SELECTED_FILE = 1000;

	private int offset = 0;
	private long currentLoginUserID = 0;
	private long remoteChatUserID = 0;
	private long remoteGroupID = 0;

	private User currentLoginUser;
	private User remoteChatUser;
	private int currentConversationViewType;

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
	// private ArrayAACPlayer mAACPlayer = null;

	// // Use local AAC encoder;
	// private V2Encoder mAacEncoder;

	private MediaPlayer mediaPlayer = null;

	private MessageReceiver receiver = new MessageReceiver();

	private V2ChatRequest mChat = new V2ChatRequest();
	private V2CrowdGroupRequest mGroupChat = new V2CrowdGroupRequest();

	private ListView mMessagesContainer;

	private LinearLayout mFaceLayout;
	private RelativeLayout mToolLayout;

	private MessageAdapter adapter;

	private List<VMessage> messageArray;

	private boolean isStopped;

	private int currentItemPos = 0;

	private ArrayList<FileInfoBean> mCheckedList;

	private ConversationNotificationObject cov = null;

	private View root; // createVideoDialog的View
	private boolean isReLoading; // 用于onNewIntent判断是否需要重新加载界面聊天数据
	private boolean sendFile; // 用于从个人信息中传递过来的文件，只发送一次

	/**
	 * Record Audio item flags
	 */
	private VMessage playingAudioMessage;
	private MessageBodyView playingAudioBodyView;
	private MessageBodyView showingPopupWindow;
	private boolean stopOhterAudio;
	private boolean isCreate;
	private long lastIntervalTime;

	/**
	 * executeConversationCreate only called once
	 */
	private boolean isFirstCall = true;

	/**
	 * 
	 */
	private boolean isFinishActivity;

	/**
	 * 当群文件有新的文件上传，或在上传界面更改了状态，则需要返回聊天界面时滚动到底部
	 */
	private boolean isScrollButtom;

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

		initExtraObject();
		initBroadcast();

		CommonCallBack.getInstance().setMessageBodyPopup(this);
		CommonCallBack.getInstance().setCrowdFileState(this);
		CommonCallBack.getInstance().setNotifyChatInterToReplace(this);
		// Register listener for avatar changed
		BitmapManager.getInstance().registerBitmapChangedListener(
				avatarChangedListener);
		mChat.registerFileTransStatusListener(this.lh, FILE_STATUS_LISTENER,
				null);
		mChat.registerP2PRecordResponseListener(this.lh,
				RECORD_STATUS_LISTENER, null);
		mGroupChat.registerFileTransStatusListener(this.lh,
				FILE_STATUS_LISTENER, null);
		// Start animation
		// this.overridePendingTransition(R.anim.nonam_scale_center_0_100,
		// R.anim.nonam_scale_null);
		// initalize vioce function that showing dialog
		createVideoDialog();
		// request ConversationTabFragment to update
		requestUpdateTabFragment();
		isCreate = true;
	}

	private void requestUpdateTabFragment() {
		Intent i = new Intent(PublicIntent.REQUEST_UPDATE_CONVERSATION);
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);
		long conversationID;
		if (currentConversationViewType == V2GlobalConstants.GROUP_TYPE_USER)
			conversationID = remoteChatUserID;
		else
			conversationID = remoteGroupID;
		i.putExtra("obj", new ConversationNotificationObject(
				Conversation.TYPE_CONTACT, conversationID, false));
		mContext.sendBroadcast(i);
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
		filter.addAction(JNIService.JNI_BROADCAST_MESSAGE_SENT_RESULT);
		filter.addAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
		filter.addAction(PublicIntent.BROADCAST_CROWD_QUIT_NOTIFICATION);
		filter.addAction(PublicIntent.BROADCAST_DISCUSSION_QUIT_NOTIFICATION);
		filter.addAction(PublicIntent.BROADCAST_CROWD_FILE_ACTIVITY_SEND_NOTIFICATION);
		filter.addAction(JNIService.BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION);
		filter.addAction(JNIService.JNI_BROADCAST_KICED_CROWD);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_UPDATED);
		filter.addAction(JNIService.JNI_BROADCAST_FILE_STATUS_ERROR_NOTIFICATION);
		filter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		registerReceiver(receiver, filter);
	}

	/**
	 * 初始化控件与监听器
	 */
	private void initModuleAndListener() {
		messageArray = new ArrayList<VMessage>();

		mMessagesContainer = (ListView) findViewById(R.id.conversation_message_list);
		adapter = new MessageAdapter();
		mMessagesContainer.setAdapter(adapter);
		mMessagesContainer.setOnTouchListener(mHiddenOnTouchListener);
		mMessagesContainer.setOnScrollListener(scrollListener);

		mSendButtonTV = findViewById(R.id.message_send);
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
		setRecordAudioTouchListener();

		mButtonCreateMetting = findViewById(R.id.contact_message_create_metting_button_layout);
		mButtonCreateMetting.setOnClickListener(mButtonCreateMettingListener);

		mAdditionFeatureContainer = findViewById(R.id.contact_message_sub_feature_ly);

		mUserTitleTV = (TextView) findViewById(R.id.message_user_title);

		mFaceLayout = (LinearLayout) findViewById(R.id.contact_message_face_item_ly);
		mToolLayout = (RelativeLayout) findViewById(R.id.contact_message_sub_feature_ly_inner);
	}

	private void initExtraObject() {

		Bundle bundle = this.getIntent().getExtras();
		if (bundle != null) {
			cov = (ConversationNotificationObject) bundle.get("obj");
			if (cov == null)
				cov = getIntent().getParcelableExtra("obj");
		}

		if (bundle == null || cov == null) {
			V2Log.e(TAG,
					"start activity was woring , please check given ConversationNotificationObject");
			cov = new ConversationNotificationObject(Conversation.TYPE_CONTACT,
					1);
		}

		// this.getIntent().getBooleanExtra("fromContactDetail", false);
		initConversationInfos();
	}

	private void initConversationInfos() {
		currentLoginUserID = GlobalHolder.getInstance().getCurrentUserId();
		currentLoginUser = GlobalHolder.getInstance().getUser(
				currentLoginUserID);
		if (cov.getConversationType() == Conversation.TYPE_CONTACT) {
			currentConversationViewType = V2GlobalConstants.GROUP_TYPE_USER;
			remoteChatUserID = cov.getExtId();
			remoteChatUser = GlobalHolder.getInstance().getUser(
					remoteChatUserID);
			if (remoteChatUser == null) {
				remoteChatUser = new User(remoteChatUserID);
			}

			mUserTitleTV.setText(remoteChatUser.getDisplayName());
			mButtonCreateMetting.setVisibility(View.GONE);
			mVideoCallButton.setVisibility(View.VISIBLE);
			mAudioCallButton.setVisibility(View.VISIBLE);
			mShowContactDetailButton.setVisibility(View.VISIBLE);
			mButtonCreateMetting.setVisibility(View.GONE);
			mShowCrowdDetailButton.setVisibility(View.GONE);
			mButtonCreateMetting.setVisibility(View.GONE);
		} else if (cov.getConversationType() == Conversation.TYPE_GROUP) {
			currentConversationViewType = V2GlobalConstants.GROUP_TYPE_CROWD;
			remoteGroupID = cov.getExtId();
			Group group = GlobalHolder.getInstance()
					.getGroupById(remoteGroupID);
			if (group == null) {
				group = new CrowdGroup(remoteGroupID, "", null);
			}
			mVideoCallButton.setVisibility(View.GONE);
			mAudioCallButton.setVisibility(View.GONE);
			mShowContactDetailButton.setVisibility(View.GONE);
			mButtonCreateMetting.setVisibility(View.VISIBLE);
			mShowCrowdDetailButton.setVisibility(View.VISIBLE);
			mUserTitleTV.setText(group.getName());
		} else if (cov.getConversationType() == V2GlobalConstants.GROUP_TYPE_DEPARTMENT
				|| cov.getConversationType() == V2GlobalConstants.GROUP_TYPE_DISCUSSION) {
			if (cov.getConversationType() == V2GlobalConstants.GROUP_TYPE_DEPARTMENT) {
				mShowContactDetailButton.setVisibility(View.GONE);
				mShowCrowdDetailButton.setVisibility(View.GONE);
				currentConversationViewType = V2GlobalConstants.GROUP_TYPE_DEPARTMENT;
				OrgGroup departmentGroup = (OrgGroup) GlobalHolder
						.getInstance().getGroupById(
								V2GlobalConstants.GROUP_TYPE_DEPARTMENT,
								cov.getExtId());
				mUserTitleTV.setText(departmentGroup.getName());
			} else {
				mShowContactDetailButton.setVisibility(View.GONE);
				mShowCrowdDetailButton.setVisibility(View.VISIBLE);
				currentConversationViewType = V2GlobalConstants.GROUP_TYPE_DISCUSSION;
				DiscussionGroup discussionGroup = (DiscussionGroup) GlobalHolder
						.getInstance().getGroupById(
								V2GlobalConstants.GROUP_TYPE_DISCUSSION,
								cov.getExtId());
				mUserTitleTV.setText(discussionGroup.getName());
			}
			remoteGroupID = cov.getExtId();
			mVideoCallButton.setVisibility(View.GONE);
			mAudioCallButton.setVisibility(View.GONE);
			mSelectFileButtonIV.setVisibility(View.GONE);
			mButtonCreateMetting.setVisibility(View.VISIBLE);
		}
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
		V2Log.d(TAG, "entry onStart....");
		if (!mIsInited && !mLoadedAllMessages) {
			android.os.Message m = android.os.Message.obtain(lh,
					START_LOAD_MESSAGE);
			lh.sendMessageDelayed(m, 500);
		}

		// 当群文件有新的文件上传，或在上传界面更改了状态，则需要返回聊天界面时滚动到底部
		if (isScrollButtom) {
			scrollToBottom();
			isScrollButtom = false;
		}

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.cancel(PublicIntent.MESSAGE_NOTIFICATION_ID);
		chanageAudioFlag();
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
	public void onBackPressed() {
		V2Log.d(TAG, "entry onBackPressed");
		checkMessageEmpty();
		// this.overridePendingTransition(R.anim.nonam_scale_null,
		// R.anim.nonam_scale_center_100_0);
		super.onBackPressed();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		V2Log.e(TAG, "entry onDestroy....");
		finishWork();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		V2Log.d(TAG, "entry onNewIntent....");
		ConversationNotificationObject tempCov = intent
				.getParcelableExtra("obj");
		if (tempCov != null)
			cov = tempCov;
		else
			cov = new ConversationNotificationObject(Conversation.TYPE_CONTACT,
					1);
		if (!isReLoading) {
			V2Log.d(TAG, "entry onNewIntent , reloading chating datas...");
			remoteChatUserID = 0;
			remoteGroupID = 0;
			remoteChatUser = null;
			mIsInited = false;
			mLoadedAllMessages = false;
			currentItemPos = 0;
			offset = 0;
			messageArray.clear();
			adapter.notifyDataSetChanged();
			initConversationInfos();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SELECT_PICTURE_CODE && data != null) {
			String filePath = data.getStringExtra("checkedImage");
			if (filePath == null) {
				Toast.makeText(mContext,
						R.string.error_contact_messag_invalid_image_path,
						Toast.LENGTH_SHORT).show();
				return;
			}
			VMessage vim = MessageBuilder.buildImageMessage(
					cov.getConversationType(), remoteGroupID, currentLoginUser,
					remoteChatUser, filePath);
			// Send message to server
			sendMessageToRemote(vim);
		} else if (requestCode == RECEIVE_SELECTED_FILE) {
			if (data != null) {
				if (isFinishActivity) {
					onBackPressed();
				}

				mCheckedList = data.getParcelableArrayListExtra("checkedFiles");
				if (mCheckedList == null || mCheckedList.size() <= 0)
					return;

				switch (currentConversationViewType) {
				case V2GlobalConstants.GROUP_TYPE_CROWD:

					for (FileInfoBean bean : mCheckedList) {
						if (bean == null || TextUtils.isEmpty(bean.filePath))
							continue;

						VMessage vm = MessageBuilder.buildFileMessage(
								cov.getConversationType(), remoteGroupID,
								currentLoginUser, remoteChatUser, bean);
						bean.fileUUID = vm.getFileItems().get(0).getUuid();
						// // Save message
						vm.setmXmlDatas(vm.toXml());
						vm.setDate(new Date(GlobalConfig.getGlobalServerTime()));

						MessageBuilder.saveMessage(this, vm);
						MessageBuilder.saveFileVMessage(this, vm);

						addMessageToContainer(vm);

						GlobalHolder.getInstance().changeGlobleTransFileMember(
								V2GlobalConstants.FILE_TRANS_SENDING, mContext,
								true, remoteGroupID,
								"ConversationP2PTextActivity onActivity crowd");
					}

					Intent intent = new Intent(this, FileService.class);
					intent.putExtra("gid", remoteGroupID);
					intent.putParcelableArrayListExtra("uploads", mCheckedList);
					startService(intent);
					break;
				case V2GlobalConstants.GROUP_TYPE_USER:
					startSendMoreFile();
					mCheckedList = null;
					break;
				}
			}
		}
	}

	private void chanageAudioFlag() {
		isStopped = false;
		// recover record all flag
		starttime = 0;
		lastTime = 0;
		realRecoding = false;
		cannelRecoding = false;
		isDown = false;
		mButtonRecordAudio
				.setText(R.string.contact_message_button_send_audio_msg);
	}

	private void checkMessageEmpty() {
		boolean isDelete = false;
		if (currentConversationViewType == V2GlobalConstants.GROUP_TYPE_USER) {
			isDelete = MessageLoader.getNewestMessage(mContext,
					currentLoginUserID, remoteChatUserID) == null ? true
					: false;
		} else if (currentConversationViewType == V2GlobalConstants.GROUP_TYPE_CROWD) {
			isDelete = MessageLoader.getNewestGroupMessage(mContext,
					V2GlobalConstants.GROUP_TYPE_CROWD, remoteGroupID) == null ? true
					: false;
		} else if (currentConversationViewType == V2GlobalConstants.GROUP_TYPE_DEPARTMENT) {
			isDelete = MessageLoader.getNewestGroupMessage(mContext,
					V2GlobalConstants.GROUP_TYPE_DEPARTMENT, remoteGroupID) == null ? true
					: false;
		} else if (currentConversationViewType == V2GlobalConstants.GROUP_TYPE_DISCUSSION) {
			isDelete = MessageLoader.getNewestGroupMessage(mContext,
					V2GlobalConstants.GROUP_TYPE_DISCUSSION, remoteGroupID) == null ? true
					: false;
		}

		Intent i = new Intent();
		i.putExtra("groupType", currentConversationViewType);
		i.putExtra("groupID", remoteGroupID);
		i.putExtra("remoteUserID", remoteChatUserID);
		i.putExtra("isDelete", isDelete);
		setResult(V2GlobalConstants.REQUEST_CONVERSATION_TEXT_RETURN, i);
	}

	private void finishWork() {
		messageArray = null;
		mCheckedList = null;
		unregisterReceiver(receiver);
		GlobalConfig.isConversationOpen = false;
		BitmapManager.getInstance().unRegisterBitmapChangedListener(
				avatarChangedListener);
		mChat.removeRegisterFileTransStatusListener(this.lh,
				FILE_STATUS_LISTENER, null);
		mChat.removeP2PRecordResponseListener(this.lh, RECORD_STATUS_LISTENER,
				null);
		mGroupChat.unRegisterFileTransStatusListener(this.lh,
				FILE_STATUS_LISTENER, null);
		stopAudioPlaying();
		releasePlayer();
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
		V2Log.d(TAG, "currentItemPos:--" + currentItemPos);
		if (pos < 0) {
			V2Log.d(TAG, "没有加载到数据 :" + pos);
			return;
		}

		if (pos >= messageArray.size()) {
			V2Log.d(TAG, "参数不合法或没有加载到数据 :" + pos);
			return;
		}

		if (isCreate) {
			isCreate = false;
			mMessagesContainer.setSelection(pos);
		} else {
			adapter.notifyDataSetChanged();
			if ((LastFistItem >= messageArray.size() || LastFistItem < 0)) {
				// //
				// 次为了解决setSelection无效的问题，虽然能解决，但会造成界面卡顿。直接setSelection而不notifyDataSetChanged即可
				mMessagesContainer.post(new Runnable() {

					@Override
					public void run() {
						mMessagesContainer.setSelection(pos);
					}

				});
			} else {
				mMessagesContainer.setSelectionFromTop(LastFistItem,
						LastFistItemOffset);
			}
		}
	}

	private String recordingFileID;
	private long starttime = 0; // 记录真正开始录音时的开始时间
	private long lastTime = 0; // 记录每次录音时的当前时间(毫秒值) ， 用于判断用户点击录音的频率
	private boolean realRecoding;
	private boolean cannelRecoding;
	private boolean timeOutRecording;
	private boolean breakRecord;
	private boolean isDown;

	private long recordTimes = 0;
	private boolean successRecord;
	private int count = 11;
	private Timer mTimer;

	private void setRecordAudioTouchListener() {
		mButtonRecordAudio.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					isDown = true;
					cannelRecoding = false;
					stopCurrentAudioPlaying();
					showOrCloseVoiceDialog();
					long currentTime = System.currentTimeMillis();
					lh.postDelayed(preparedRecoding, 250);
					if (currentTime - lastTime < 250) {
						V2Log.d(TAG, "间隔太短，取消录音");
					}
					lastTime = currentTime;
					break;
				case MotionEvent.ACTION_MOVE:
					Rect r = new Rect();
					view.getDrawingRect(r);
					// check if touch position out of button than cancel send
					// voice
					// message
					if (isDown) {
						if (timeOutRecording) {
							mButtonRecordAudio
									.setText(R.string.contact_message_button_send_audio_msg);
						} else {
							if (r.contains((int) event.getX(),
									(int) event.getY())) {
								updateCancelSendVoiceMsgNotification(VOICE_DIALOG_FLAG_RECORDING);
							} else {
								updateCancelSendVoiceMsgNotification(VOICE_DIALOG_FLAG_CANCEL);
							}
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
						// 计算录音时间
						long seconds = (System.currentTimeMillis() - starttime);
						// recover all flag 复原标记位
						lastTime = 0;
						starttime = 0;
						realRecoding = false;
						// Remove timer
						// lh.removeCallbacks(mUpdateMicStatusTimer);
						lh.removeCallbacks(timeOutMonitor);
						// recover button show state
						mButtonRecordAudio
								.setText(R.string.contact_message_button_send_audio_msg);
						// check if touch position out of button than cancel
						// send voice message
						Rect rect = new Rect();
						view.getDrawingRect(rect);
						if (rect.contains((int) event.getX(),
								(int) event.getY())
								&& seconds > 1500) {
							successRecord = true;
							recordTimes = seconds / 1000;
						} else {
							if (seconds < 1500) {
								updateCancelSendVoiceMsgNotification(VOICE_DIALOG_FLAG_WARING_FOR_TIME_TOO_SHORT);
							} else {
								Toast.makeText(
										mContext,
										R.string.contact_message_message_cancelled,
										Toast.LENGTH_SHORT).show();
							}
						}

						// stop recording and sending
						V2Log.w("AudioRequest",
								"invoking stop recording! id is : "
										+ recordingFileID);
						stopRecording(recordingFileID);
						recordingFileID = null;

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
						} else {
							breakRecord = false;
							mButtonRecordAudio
									.setText(R.string.contact_message_button_send_audio_msg);
						}
					}
					break;
				}
				return true;
			}
		});
	}

	/**
	 * FIXME If want to support lower 4.0 for ACC should code by self
	 * 
	 * @param filePath
	 * @return
	 */
	private boolean startReocrding(String filePath) {
		// AAC Record version one
		// mAacEncoder = new AACEncoder(filePath);
		// mAacEncoder.start();
		// return true;

		// AAC Record version two
		// mRecorder = new MediaRecorder();
		// mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		// mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
		// mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		// mRecorder.setOutputFile(filePath);
		// mRecorder.setMaxDuration(60000);
		// mRecorder.setAudioSamplingRate(44100);
		// mRecorder.setAudioChannels(2);
		// try {
		// mRecorder.prepare();
		// mRecorder.start();
		// } catch (IOException e) {
		// V2Log.e(" can not prepare media recorder ");
		// return false;
		// } catch (IllegalStateException e) {
		// V2Log.e(" can not prepare media recorder ");
		// return false;
		// } catch (Exception e) {
		// V2Log.e("error , can not prepare media recorder ");
		// return false;
		// }

		// MP3
		mChat.startAudioRecord(filePath);
		return true;

	}

	private void stopRecording(String fileID) {
		// mAacEncoder.stop();
		// //FIXME should delay some millicseconds
		// if (mAacEncoder.getState() != MediaState.NORMAL
		// && mAacEncoder.getState() != MediaState.STOPPED) {
		// V2Log.e("=========recording file error " +mAacEncoder.getState());
		//
		// }
		// mAacEncoder.release();

		// try {
		// // Ignore stop fialed exception
		// mRecorder.stop();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// mRecorder.reset();
		// mRecorder.release();
		// mRecorder = null;

		mChat.stopAudioRecord(fileID);
	}

	/**
	 * 异常终止录音
	 */
	private void breakRecording() {

		if (mRecorder != null && realRecoding) {
			breakRecord = true;
			lastTime = 0;
			starttime = 0;
			realRecoding = false;
			// Hide voice dialog
			showOrCloseVoiceDialog();
			stopRecording(recordingFileID);
			recordingFileID = null;
			starttime = 0;
			// lh.removeCallbacks(mUpdateMicStatusTimer);
			lh.removeCallbacks(timeOutMonitor);
			lh.removeCallbacks(mUpdateSurplusTime);
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

	private Runnable preparedRecoding = new Runnable() {
		@Override
		public void run() {

			if (!cannelRecoding) {
				realRecoding = true;
				recordingFileID = UUID.randomUUID().toString();
				// GlobalConfig.getGlobalAudioPath() + "/" +
				// UUID.randomUUID().toString() + ".mp3";
				boolean resultReocrding = startReocrding(recordingFileID);
				if (resultReocrding) {
					starttime = System.currentTimeMillis();
					// Start update db for voice
					// lh.postDelayed(mUpdateMicStatusTimer, 200);
					// Start timer
					lh.postDelayed(timeOutMonitor, 59 * 1000);
					// start timer for prompt surplus time
					lh.postDelayed(mUpdateSurplusTime, 48 * 1000);
				} else
					breakRecording();
			}
		}
	};

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
			stopRecording(recordingFileID);
			// send
			timeOutRecording = true;
			realRecoding = false;
			successRecord = true;
			recordTimes = 60;
			recordingFileID = null;
			starttime = 0;
			// lh.removeCallbacks(mUpdateMicStatusTimer);
			lh.removeCallbacks(mUpdateSurplusTime);
			showOrCloseVoiceDialog();
		}
	};

	private boolean playNextUnreadMessage() {
		boolean found = false;
		if (playingAudioMessage == null)
			return false;
		for (int i = 0; i < messageArray.size(); i++) {
			VMessage vm = messageArray.get(i);
			if (vm.getUUID().equals(playingAudioMessage.getUUID())) {
				found = true;
				continue;
			}

			if (found) {
				List<VMessageAudioItem> items = vm.getAudioItems();
				if (items.size() > 0) {
					VMessageAudioItem audio = items.get(0);
					if (audio.getReadState() == VMessageAbstractItem.STATE_UNREAD) {
						V2Log.d(TAG,
								"start palying next aduio item , id is : "
										+ vm.getId()
										+ " and index in collections is : " + i
										+ " collections size is : "
										+ messageArray.size());
						if (i != 0)
							mMessagesContainer.setSelection(i);
						audio.setStartPlay(true);
						Message.obtain(lh, ADAPTER_NOTIFY).sendToTarget();
						return true;
					}
				}
			}
		}
		return false;
	}

	private InputStream currentPlayedStream;
	private TextView tips;

	private synchronized boolean startAudioPlaying(String fileName) {
		// AACPlayer Version One
		// mAACPlayer = new AACPlayer(fileName);
		// try {
		// if (currentPlayedStream != null) {
		// currentPlayedStream.close();
		// }
		// // currentPlayedStream = new FileInputStream(new File(fileName));
		// mAACPlayer.play();
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		// AACPlayer Version Two
		// mAACPlayer = new ArrayAACPlayer(
		// ArrayDecoder.create(Decoder.DECODER_FAAD2), mAACPlayerCallback,
		// AACPlayer.DEFAULT_AUDIO_BUFFER_CAPACITY_MS,
		// AACPlayer.DEFAULT_DECODE_BUFFER_CAPACITY_MS);
		// try {
		// if (currentPlayedStream != null) {
		// currentPlayedStream.close();
		// }
		// // currentPlayedStream = new FileInputStream(new File(fileName));
		// mAACPlayer.playAsync(fileName);
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		try {
			if (mediaPlayer == null) {
				mediaPlayer = new MediaPlayer();
				initMediaPlayerListener(mediaPlayer);
			}
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setDataSource(fileName);
			mediaPlayer.prepareAsync();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	private void stopAudioPlaying() {
		// if (mAACPlayer != null) {
		// mAACPlayer.stop();
		// mAACPlayer = null;
		// }
		// if (currentPlayedStream != null) {
		// try {
		// currentPlayedStream.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }

		if (mediaPlayer != null) {
			playingAudioBodyView.stopVoiceAnimation();
			playingAudioMessage.getAudioItems().get(0).setPlaying(false);
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}

	private void releasePlayer() {
		// if (mAACPlayer != null) {
		// // mAACPlayer.release();
		// mAACPlayer.stop();
		// mAACPlayer = null;
		// }

		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}

	private void stopCurrentAudioPlaying() {
		if (playingAudioMessage != null
				&& playingAudioMessage.getAudioItems().size() > 0) {
			playingAudioMessage.getAudioItems().get(0).setPlaying(false);
			stopAudioPlaying();
		}
	}

	private void initMediaPlayerListener(MediaPlayer mediaPlayer) {
		mediaPlayer.setOnErrorListener(new OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				V2Log.e(TAG, "playing wroing!");
				mp.reset();
				playingAudioBodyView.stopVoiceAnimation();
				playingAudioMessage.getAudioItems().get(0).setPlaying(false);
				return false;
			}
		});

		mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

			@Override
			public void onPrepared(MediaPlayer mp) {
				mp.start();
				playingAudioBodyView.startVoiceAnimation();
				playingAudioMessage.getAudioItems().get(0).setPlaying(true);
			}
		});

		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				releasePlayer();
				playingAudioBodyView.stopVoiceAnimation();
				playingAudioMessage.getAudioItems().get(0).setPlaying(false);
				Message.obtain(lh, PLAY_NEXT_UNREAD_MESSAGE).sendToTarget();
			}
		});
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

	// private void cleanRangeBitmapCache(int before, int after) {
	// int size = messageArray.size();
	// if (size < after && before < 0) {
	// return;
	// }
	//
	// while (--before >= 0) {
	// VMessageAdater ItemWrapper = (VMessageAdater) messageArray
	// .get(before);
	// VMessage vm = (VMessage) ItemWrapper.getItemObject();
	// ItemWrapper.setView(null);
	// vm.recycleAllImageMessage();
	// }
	//
	// while (++after < size) {
	// VMessageAdater ItemWrapper = (VMessageAdater) messageArray
	// .get(after);
	// VMessage vm = (VMessage) ItemWrapper.getItemObject();
	// ItemWrapper.setView(null);
	// vm.recycleAllImageMessage();
	// }
	// }

	private void startVoiceCall() {
		if (!GlobalHolder.getInstance().isServerConnected()) {
			Toast.makeText(mContext, R.string.error_local_connect_to_server,
					Toast.LENGTH_SHORT).show();
			return;
		}

		Intent iv = new Intent();
		iv.addCategory(PublicIntent.DEFAULT_CATEGORY);
		iv.setAction(PublicIntent.START_P2P_CONVERSACTION_ACTIVITY);
		iv.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		iv.putExtra("uid", remoteChatUserID);
		iv.putExtra("is_coming_call", false);
		iv.putExtra("voice", true);
		mContext.startActivity(iv);
	}

	private void startVideoCall() {
		if (!GlobalHolder.getInstance().isServerConnected()) {
			Toast.makeText(mContext, R.string.error_local_connect_to_server,
					Toast.LENGTH_SHORT).show();
			return;
		}

		Intent iv = new Intent();
		iv.addCategory(PublicIntent.DEFAULT_CATEGORY);
		iv.setAction(PublicIntent.START_P2P_CONVERSACTION_ACTIVITY);
		iv.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		iv.putExtra("uid", remoteChatUserID);
		iv.putExtra("is_coming_call", false);
		iv.putExtra("voice", false);
		UserDeviceConfig udc = GlobalHolder.getInstance().getUserDefaultDevice(
				remoteChatUserID);
		if (udc != null) {
			iv.putExtra("device", udc.getDeviceID());
		} else {
			iv.putExtra("device", "");
		}
		mContext.startActivity(iv);
	}

	// private PlayerCallback mAACPlayerCallback = new PlayerCallback() {
	//
	// @Override
	// public void playerStarted() {
	// playingAudioMessage.getAudioItems().get(0).setPlaying(true);
	// runOnUiThread(new Runnable() {
	//
	// @Override
	// public void run() {
	// playingAudioBodyView.startVoiceAnimation();
	// }
	// });
	// V2Log.i(TAG, "更改正在播放的audioItem标识为true , id is ："
	// + playingAudioMessage.getId());
	// }
	//
	// @Override
	// public void playerPCMFeedBuffer(boolean isPlaying,
	// int audioBufferSizeMs, int audioBufferCapacityMs) {
	// V2Log.e(audioBufferSizeMs + "  " + audioBufferCapacityMs);
	// if (audioBufferSizeMs == audioBufferCapacityMs) {
	//
	// }
	// }
	//
	// @Override
	// public synchronized void playerStopped(int perf) {
	//
	// if (stopOhterAudio) {
	// stopOhterAudio = false;
	// return;
	// }
	//
	// if (playingAudioMessage != null
	// && playingAudioMessage.getAudioItems().size() > 0) {
	// playingAudioMessage.getAudioItems().get(0).setPlaying(false);
	// runOnUiThread(new Runnable() {
	//
	// @Override
	// public void run() {
	// playingAudioBodyView.stopVoiceAnimation();
	// }
	// });
	// V2Log.i(TAG, "更改正在播放的audioItem标识为false , id is ："
	// + playingAudioMessage.getId() + " playing over!");
	// }
	// // Call in main thread
	// Message.obtain(lh, PLAY_NEXT_UNREAD_MESSAGE).sendToTarget();
	//
	// }
	//
	// @Override
	// public void playerException(Throwable t) {
	// playerStopped(0);
	// if (playingAudioMessage != null) {
	// playingAudioMessage.getAudioItems().get(0).setPlaying(false);
	// }
	// }
	//
	// };

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
			Intent intent = new Intent(ConversationTextActivity.this,
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
			if (LocalSharedPreferencesStorage.checkCurrentAviNetwork(mContext)) {
				Intent intent = null;
				if (currentConversationViewType == V2GlobalConstants.GROUP_TYPE_USER) {
					intent = new Intent(ConversationTextActivity.this,
							ConversationSelectFileEntry.class);
					intent.putExtra("uid", remoteChatUserID);
				} else if (currentConversationViewType == V2GlobalConstants.GROUP_TYPE_CROWD) {
					intent = new Intent(ConversationTextActivity.this,
							ConversationSelectFile.class);
					intent.putExtra("type", "crowdFile");
					intent.putExtra("uid", remoteGroupID);
				} else {
					return;
				}
				startActivityForResult(intent, RECEIVE_SELECTED_FILE);
			} else {

				Toast.makeText(mContext,
						R.string.common_networkIsDisconnection_retryPlease,
						Toast.LENGTH_SHORT).show();
			}
		}
	};

	private View.OnClickListener mVideoCallButtonListener = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if (!LocalSharedPreferencesStorage.checkCurrentAviNetwork(mContext)) {
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
			i.putExtra("gid", remoteGroupID);
			startActivity(i);
		}
	};

	private TextWatcher mPasteWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable edit) {
			mMessageET.removeTextChangedListener(this);
			MessageUtil.buildChatPasteMessageContent(mContext, mMessageET);
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

	private void doSendMessage() {
		VMessage vm = null;
		if (currentConversationViewType == V2GlobalConstants.GROUP_TYPE_USER)
			vm = MessageUtil.buildChatMessage(mContext, mMessageET,
					currentConversationViewType, remoteGroupID, remoteChatUser);
		else
			vm = MessageUtil.buildChatMessage(mContext, mMessageET,
					currentConversationViewType, remoteGroupID, null);
		if (vm != null)
			sendMessageToRemote(vm);
	}

	private void sendMessageToRemote(VMessage vm) {
		sendMessageToRemote(vm, true);
	}

	/**
	 * send chat message to remote
	 * 
	 * @param vm
	 * @param isFresh
	 *            是否通知ConversationTabFragment更新
	 */
	private void sendMessageToRemote(VMessage vm, boolean isFresh) {
		// // Save message
		vm.setmXmlDatas(vm.toXml());
		vm.setState(VMessageAbstractItem.STATE_NORMAL);
		vm.isBeginSendingAnima = true;

		MessageBuilder.saveMessage(this, vm);
		MessageBuilder.saveFileVMessage(this, vm);
		MessageBuilder.saveBinaryVMessage(this, vm);

		addMessageToContainer(vm);
		Message.obtain(lh, SEND_MESSAGE, vm).sendToTarget();
		// 如果从个人资料界面发送消息，需要回调界面创建该会话
		if (isFirstCall) {
			V2Log.d(TAG,
					"sendMessageToRemote --> need to create new conversation!");
			isFirstCall = false;
			CommonCallBack.getInstance().executeConversationCreate(
					currentConversationViewType, remoteGroupID,
					remoteChatUserID);
		}
	}

	private void addMessageToContainer(final VMessage msg) {
		// make offset
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				offset++;
				judgeShouldShowTime(msg);
				messageArray.add(msg);
				if (msg.getFileItems().size() > 0)
					msg.isBeginSendingAnima = false;
				Message.obtain(lh, ADAPTER_NOTIFY).sendToTarget();
				scrollToBottom();
			}
		});
	}

	private void startSendMoreFile() {
		for (int i = 0; i < mCheckedList.size(); i++) {
			GlobalHolder.getInstance().changeGlobleTransFileMember(
					V2GlobalConstants.FILE_TRANS_SENDING, mContext, true,
					remoteChatUserID, "ConversationP2PTextActivity onActivity");
			sendSelectedFile(mCheckedList.get(i));
		}
	}

	private int LastFistItem;
	private int LastFistItemOffset;
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
				LastFistItem = mMessagesContainer.getFirstVisiblePosition();
				V2Log.e(TAG, "First visible position is : " + LastFistItem);
				View v = mMessagesContainer.getChildAt(0);
				LastFistItemOffset = (v == null) ? 0 : v.getTop();
				// Do not clean image message state when loading message
			} else {
				// if (lastFirst != first) {
				// cleanRangeBitmapCache(first - 5, first + allVisibleCount
				// + BATCH_COUNT);
				// }
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

	private MessageBodyView.ClickListener listener = new MessageBodyView.ClickListener() {

		@Override
		public void onMessageClicked(VMessage v) {
			List<VMessageImageItem> imageItems = v.getImageItems();
			VMessageImageItem imageItem;
			if (imageItems != null && imageItems.size() > 0) {
				imageItem = v.getImageItems().get(0);
				Intent i = new Intent();
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.setAction(PublicIntent.START_VIDEO_IMAGE_GALLERY);
				i.putExtra("uid1", currentLoginUserID);
				i.putExtra("uid2", remoteChatUserID);
				i.putExtra("cid", v.getUUID());
				if (imageItem != null)
					i.putExtra("imageID", imageItem.getUuid());
				// type 0: is not group image view
				// type 1: group image view
				i.putExtra("type", currentConversationViewType);
				i.putExtra("gid", remoteGroupID);
				mContext.startActivity(i);
				return;
			}

			List<VMessageLinkTextItem> linkItems = v.getLinkItems();
			VMessageLinkTextItem linkItem = null;
			if (linkItems != null && linkItems.size() > 0) {
				linkItem = linkItems.get(0);
				Intent intent = new Intent();
				intent.setAction("android.intent.action.VIEW");
				Uri content_url = Uri.parse("http://" + linkItem.getUrl());
				intent.setData(content_url);
				startActivity(intent);
				return;
			}
		}

		public void onCrowdFileMessageClicked(CrowdFileActivityType type) {
			Intent i = new Intent(PublicIntent.START_CROWD_FILES_ACTIVITY);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.putExtra("cid", remoteGroupID);
			i.putExtra("crowdFileActivityType", type);
			startActivity(i);
		};

		@Override
		public void requestPlayAudio(MessageBodyView view, VMessage vm,
				VMessageAudioItem vai) {
			if (vai != null && vai.getAudioFilePath() != null) {
				playingAudioMessage = vm;
				playingAudioBodyView = view;
				for (int i = 0; i < messageArray.size(); i++) {
					VMessage tempVm = messageArray.get(i);
					if (tempVm.getUUID().equals(playingAudioMessage.getUUID())) {
						vai.setPlaying(true);
						tempVm.getAudioItems()
								.get(0)
								.setReadState(VMessageAbstractItem.STATE_READED);
					}
				}
				V2Log.i(TAG, "start play , currentPlayingAudio id is : "
						+ playingAudioMessage.getId());
				startAudioPlaying(vai.getAudioFilePath());
			}
		}

		@Override
		public void reSendMessageClicked(VMessage v) {
			v.setState(VMessageAbstractItem.STATE_NORMAL);
			v.setResendMessage(true);
			List<VMessageAbstractItem> items = v.getItems();
			for (int i = 0; i < items.size(); i++) {
				VMessageAbstractItem item = items.get(i);
				if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FILE) {
					item.setState(VMessageAbstractItem.STATE_FILE_SENDING);
					MessageLoader.updateFileItemState(mContext,
							(VMessageFileItem) item);
				} else
					item.setState(VMessageAbstractItem.STATE_NORMAL);
			}
			int update = MessageLoader.updateChatMessageState(mContext, v);
			if (update <= 0)
				V2Log.e(TAG,
						"Update chatMessage state failed...message uuid is : "
								+ v.getUUID());
			Message.obtain(lh, SEND_MESSAGE, v).sendToTarget();
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
					case VMessageAbstractItem.STATE_FILE_PAUSED_SENDING:
						if (currentConversationViewType == V2GlobalConstants.GROUP_TYPE_USER)
							GlobalHolder
									.getInstance()
									.changeGlobleTransFileMember(
											V2GlobalConstants.FILE_TRANS_SENDING,
											mContext, false, remoteChatUserID,
											"ConversationP2PText REQUEST_DEL_MESSAGE");
						else
							GlobalHolder
									.getInstance()
									.changeGlobleTransFileMember(
											V2GlobalConstants.FILE_TRANS_SENDING,
											mContext, false, remoteGroupID,
											"ConversationP2PText REQUEST_DEL_MESSAGE");
						if (item.getState() == VMessageAbstractItem.STATE_FILE_SENDING) {
							mChat.updateFileOperation(vfi,
									FileOperationEnum.OPERATION_CANCEL_SENDING,
									null);
						}
						break;
					case VMessageAbstractItem.STATE_FILE_DOWNLOADING:
					case VMessageAbstractItem.STATE_FILE_PAUSED_DOWNLOADING:
						if (currentConversationViewType == V2GlobalConstants.GROUP_TYPE_USER)
							GlobalHolder
									.getInstance()
									.changeGlobleTransFileMember(
											V2GlobalConstants.FILE_TRANS_DOWNLOADING,
											mContext, false, remoteChatUserID,
											"ConversationP2PText REQUEST_DEL_MESSAGE");
						else
							GlobalHolder
									.getInstance()
									.changeGlobleTransFileMember(
											V2GlobalConstants.FILE_TRANS_DOWNLOADING,
											mContext, false, remoteGroupID,
											"ConversationP2PText REQUEST_DEL_MESSAGE");
						if (item.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING) {
							mChat.updateFileOperation(
									vfi,
									FileOperationEnum.OPERATION_CANCEL_DOWNLOADING,
									null);
						}
						break;
					default:
						break;
					}
				}
			}
			Message.obtain(lh, REQUEST_DEL_MESSAGE, v).sendToTarget();
		}

		@Override
		public void requestStopAudio(VMessage vm, VMessageAudioItem vai) {
			V2Log.i(TAG,
					"request current playing audioItem 停止 , id is ："
							+ vm.getId());
			vai.setPlaying(false);
			stopAudioPlaying();
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
			if (playingAudioMessage == null
					|| playingAudioMessage.getId() == vm.getId()) {
				return;
			}
			V2Log.i(TAG, "停止了当前正在播放的currentPlayingAudio:--"
					+ playingAudioMessage.getId());
			playingAudioBodyView.stopVoiceAnimation();
			stopOhterAudio = true;
			stopCurrentAudioPlaying();
		}
	};

	private List<VMessage> loadMessages() {

		List<VMessage> array = null;
		switch (currentConversationViewType) {
		case V2GlobalConstants.GROUP_TYPE_USER: {
			array = MessageLoader.loadMessageByPage(mContext,
					Conversation.TYPE_CONTACT, currentLoginUserID,
					remoteChatUserID, BATCH_COUNT, offset);
			if (array != null) {
				for (int i = 0; i < array.size(); i++) {
					if (array.get(i).getFileItems().size() > 0) {
						VMessageFileItem vMessageFileItem = array.get(i)
								.getFileItems().get(0);
						File check = new File(vMessageFileItem.getFilePath());
						if (!check.isFile()
								&& !check.exists()
								&& vMessageFileItem.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADED) {
							vMessageFileItem
									.setState(VMessageAbstractItem.STATE_FILE_UNDOWNLOAD);
							MessageLoader.updateFileItemState(mContext,
									vMessageFileItem);
						}
					}
				}
			}
		}
			break;
		case V2GlobalConstants.GROUP_TYPE_CROWD:
			array = MessageLoader
					.loadGroupMessageByPage(mContext, Conversation.TYPE_GROUP,
							remoteGroupID, BATCH_COUNT, offset);
			break;
		case V2GlobalConstants.GROUP_TYPE_DEPARTMENT:
			array = MessageLoader.loadGroupMessageByPage(mContext,
					V2GlobalConstants.GROUP_TYPE_DEPARTMENT, remoteGroupID,
					BATCH_COUNT, offset);
			break;
		case V2GlobalConstants.GROUP_TYPE_DISCUSSION:
			array = MessageLoader.loadGroupMessageByPage(mContext,
					V2GlobalConstants.GROUP_TYPE_DISCUSSION, remoteGroupID,
					BATCH_COUNT, offset);
			break;
		default:
			break;
		}

		if (array != null) {
			// FIXME Don't delete these codes
			// boolean isExist;
			// if (currentConversationViewType == V2GlobalEnum.GROUP_TYPE_USER)
			// isExist = ConversationProvider
			// .queryUserConversation(remoteChatUserID);
			// else
			// isExist = ConversationProvider.queryGroupConversation(
			// currentConversationViewType, remoteGroupID);
			// if (!isExist) {
			// boolean flag = MessageLoader.deleteMessageByID(mContext,
			// currentConversationViewType, remoteGroupID,
			// remoteChatUserID, true);
			// if (flag) {
			// if (currentConversationViewType == V2GlobalEnum.GROUP_TYPE_USER)
			// MessageLoader.isTableExist(mContext,
			// currentConversationViewType, remoteGroupID,
			// remoteChatUserID, MessageLoader.CONTACT_TYPE);
			// else
			// MessageLoader.isTableExist(mContext,
			// currentConversationViewType, remoteGroupID,
			// remoteChatUserID, MessageLoader.CROWD_TYPE);
			// }
			// array.clear();
			// return array;
			// }
			offset += array.size();
			mIsInited = true;
		}
		return array;
	}

	private boolean pending = false;

	private boolean queryAndAddMessage(final int msgId) {

		VMessage m;
		if (currentConversationViewType == V2GlobalConstants.GROUP_TYPE_USER)
			m = MessageLoader.loadUserMessageById(mContext, remoteChatUserID,
					msgId);
		else
			m = MessageLoader.loadGroupMessageById(mContext,
					currentConversationViewType, remoteGroupID, msgId);
		if (m == null
				|| (m.getFromUser().getmUserId() != this.remoteChatUserID && m
						.getGroupId() == 0)
				|| (m.getGroupId() != this.remoteGroupID)) {
			return false;
		}

		// 使当前正在显示popupWindow消失
		if (showingPopupWindow != null)
			showingPopupWindow.dissmisPopupWindow();
		judgeShouldShowTime(m);
		messageArray.add(m);
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
			VMessage message = messageArray.get(i);
			if (vm.getId() == message.getId()) {
				deleteMessage(message, i);
				return true;
			} else {
				V2Log.d(TAG, "删除聊天消息失败,没有从集合中找到该消息 id : " + vm.getId());
			}
		}
		return false;
	}

	private void deleteMessage(VMessage vm, int location) {
		messageArray.remove(location);
		boolean isDeleteOther = true;
		// if (currentConversationViewType == V2GlobalEnum.GROUP_TYPE_CROWD
		// && vm.getFileItems().size() > 0
		// && vm.getFromUser().getmUserId() == GlobalHolder.getInstance()
		// .getCurrentUserId()
		// && vm.getFileItems().get(0).getState() ==
		// VMessageAbstractItem.STATE_FILE_SENDING) {
		// isDeleteOther = false;
		// }
		MessageLoader.deleteMessage(mContext, vm, isDeleteOther);
		List<VMessage> messagePages = MessageLoader.loadGroupMessageByPage(
				mContext, Conversation.TYPE_GROUP, remoteGroupID, 1,
				messageArray.size());
		if (messagePages != null && messagePages.size() > 0) {
			messageArray.add(0, messagePages.get(0));
		}
		V2Log.d(TAG, "现在集合长度：" + messageArray.size());
	}

	/**
	 * 用于判断指定的消息VMessage对象是否应该显示时间状态
	 * 
	 * @param message
	 */
	private void judgeShouldShowTime(VMessage message) {
		if (lastIntervalTime != 0) {
			if (GlobalConfig.getGlobalServerTime() - lastIntervalTime <= INTERVAL_TIME) {
				message.setShowTime(false);
			} else {
				message.setShowTime(true);
				lastIntervalTime = GlobalConfig.getGlobalServerTime();
			}
		} else {
			message.setShowTime(true);
			lastIntervalTime = GlobalConfig.getGlobalServerTime();
		}
	}

	private void updateFileProgressView(String uuid, long tranedSize,
			int progressType) {
		if (messageArray != null) {
			for (int i = 0; i < messageArray.size(); i++) {
				VMessage vm = messageArray.get(i);
				if (vm.getFileItems().size() > 0) {
					VMessageFileItem item = vm.getFileItems().get(0);
					if (item.getUuid().equals(uuid)) {
						VMessageFileItem vfi = ((VMessageFileItem) item);
						if (currentConversationViewType == V2GlobalConstants.GROUP_TYPE_CROWD
								&& vfi.getState() == VMessageAbstractItem.STATE_FILE_SENT) {
							V2Log.e("test",
									"VMessageAbstractItem.STATE_FILE_SENT！！");
							return;
						} else {
							switch (progressType) {
							case FileTransStatusIndication.IND_TYPE_PROGRESS_END:
								if (vfi.getState() == VMessageAbstractItem.STATE_FILE_SENDING) {
									vfi.setState(VMessageAbstractItem.STATE_FILE_SENT);
								} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING) {
									vfi.setState(VMessageAbstractItem.STATE_FILE_DOWNLOADED);
								}
								break;
							}
							vfi.setDownloadedSize(tranedSize);
							Message.obtain(lh, ADAPTER_NOTIFY).sendToTarget();
						}
					}
				} else if (vm.getAudioItems().size() > 0) {
					VMessageAudioItem item = vm.getAudioItems().get(0);
					if (item.getUuid().equals(uuid)) {
						Message.obtain(lh, ADAPTER_NOTIFY).sendToTarget();
					}
				}
			}
		}
	}

	/**
	 * update all sending or downloading file state to failed..
	 */
	public void executeUpdateFileState() {

		if (messageArray == null) {
			V2Log.e(TAG,
					"executeUpdateFileState is failed ... because messageArray is null");
			return;
		}

		for (int i = 0; i < messageArray.size(); i++) {
			VMessage vm = messageArray.get(i);
			if (vm.getFileItems().size() > 0) {
				List<VMessageFileItem> fileItems = vm.getFileItems();
				for (int j = 0; j < fileItems.size(); j++) {
					VMessageFileItem item = fileItems.get(j);
					switch (item.getState()) {
					case VMessageAbstractItem.STATE_FILE_DOWNLOADING:
					case VMessageAbstractItem.STATE_FILE_PAUSED_DOWNLOADING:
						item.setState(VMessageFileItem.STATE_FILE_DOWNLOADED_FALIED);
						break;
					case VMessageAbstractItem.STATE_FILE_SENDING:
					case VMessageAbstractItem.STATE_FILE_PAUSED_SENDING:
						item.setState(VMessageFileItem.STATE_FILE_SENT_FALIED);
						break;
					default:
						break;
					}
					if (!isStopped)
						Message.obtain(lh, ADAPTER_NOTIFY).sendToTarget();
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
			i.putExtra("uid", remoteChatUserID);
			i.putExtra("obj", cov);
			i.putExtra("fromPlace", "ConversationView");
			i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivityForResult(i, RECEIVE_SELECTED_FILE);
		}

	};

	private OnClickListener mShowCrowdDetailListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			Group g = GlobalHolder.getInstance().getGroupById(cov.getExtId());
			if (g.getGroupType() == GroupType.CHATING) {
				Intent i = new Intent(mContext, CrowdDetailActivity.class);
				i.putExtra("cid", cov.getExtId());
				startActivity(i);
			} else if (g.getGroupType() == GroupType.DISCUSSION) {
				Intent i = new Intent();
				i.setAction(PublicIntent.SHOW_DISCUSSION_BOARD_DETAIL_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.putExtra("cid", cov.getExtId());
				startActivity(i);
			}
		}

	};

	private BitmapManager.BitmapChangedListener avatarChangedListener = new BitmapManager.BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			if (user == null) {
				return;
			}

			for (int i = 0; i < messageArray.size(); i++) {
				VMessage vMessage = messageArray.get(i);
				if (vMessage.getFromUser() != null
						&& vMessage.getFromUser().getmUserId() == user
								.getmUserId()) {
					vMessage.isUpdateAvatar = true;
				}
			}
			Message.obtain(lh, ADAPTER_NOTIFY).sendToTarget();
		}
	};

	// private Runnable mUpdateMicStatusTimer = new Runnable() {
	// public void run() {
	// int ratio = mRecorder.getMaxAmplitude() / 600;
	// int db = 0;// 分贝
	// if (ratio > 1)
	// db = (int) (20 * Math.log10(ratio));
	//
	// // int db = (int)mAacEncoder.getDB();
	// updateVoiceVolume(db / 4);
	//
	// lh.postDelayed(mUpdateMicStatusTimer, 200);
	// }
	// };

	class MessageAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return messageArray.size();
		}

		@Override
		public Object getItem(int pos) {
			return messageArray.get(pos);
		}

		@Override
		public long getItemId(int pos) {
			return 0;
		}

		@Override
		public View getView(int pos, View convertView, ViewGroup v) {
			MessageBodyView mv = null;
			VMessage vm = messageArray.get(pos);
			if (convertView == null) {
				mv = new MessageBodyView(mContext, vm, vm.isShowTime());
				mv.setCallback(listener);
				convertView = mv;
			} else {
				List<VMessageFileItem> fileItems = vm.getFileItems();
				if (fileItems.size() > 0) {
					adapterFileIcon(fileItems);
				}

				Boolean isPlay = false;
				VMessageAudioItem vMessageAudioItem = null;
				List<VMessageAudioItem> audioItems = vm.getAudioItems();
				if (audioItems.size() > 0 && audioItems.get(0).isStartPlay()) {
					vMessageAudioItem = audioItems.get(0);
					vMessageAudioItem.setStartPlay(false);
					vMessageAudioItem
							.setReadState(VMessageAbstractItem.STATE_READED);
					MessageLoader.updateBinaryAudioItem(vMessageAudioItem);
					isPlay = true;
				}

				mv = (MessageBodyView) convertView;
				mv.updateView(vm);
				vm.isUpdateAvatar = false;

				if (isPlay)
					mv.getCallback()
							.requestPlayAudio(mv, vm, vMessageAudioItem);
			}
			return convertView;
		}
	}

	class MessageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (JNIService.JNI_BROADCAST_NEW_MESSAGE.equals(intent.getAction())) {
				MessageObject msgObj = intent.getParcelableExtra("msgObj");
				int groupType = msgObj.groupType;
				long groupID = msgObj.remoteGroupID;
				long remoteID = msgObj.rempteUserID;
				long msgID = msgObj.messageColsID;
				if (currentConversationViewType == groupType) {
					switch (currentConversationViewType) {
					case V2GlobalConstants.GROUP_TYPE_CROWD:
					case V2GlobalConstants.GROUP_TYPE_DEPARTMENT:
					case V2GlobalConstants.GROUP_TYPE_DISCUSSION:
						isReLoading = groupID == remoteGroupID;
						break;
					case V2GlobalConstants.GROUP_TYPE_USER:
						isReLoading = remoteID == remoteChatUserID;
						break;
					}
				} else
					isReLoading = false;
				// 用于onNewIntent判断是否需要重新加载界面聊天数据，以及是否阻断广播 , true 后台
				boolean isAppBack = ((MainApplication) mContext
						.getApplicationContext()).isRunningBackgound();
				if (isReLoading && !isLoading) {
					boolean result = queryAndAddMessage((int) msgID);
					if (result) {
						offset += 1;
						if (!isAppBack) {
							CommonCallBack.getInstance()
									.executeNotifyCrowdDetailActivity();
						}
					}
				}
			} else if (JNIService.JNI_BROADCAST_MESSAGE_SENT_RESULT
					.equals(intent.getAction())) {
				int result = intent.getExtras().getInt("errorCode");
				String uuid = intent.getExtras().getString("uuid");
				for (int i = 0; i < messageArray.size(); i++) {
					VMessage vm = messageArray.get(i);
					if (vm.getUUID().equals(uuid)) {
						vm.isBeginSendingAnima = false;
						if (result == SEND_MESSAGE_SUCCESS) {
							vm.setState(VMessageAbstractItem.STATE_SENT_SUCCESS);
							vm.isShowFailed = false;
						} else {
							vm.isShowFailed = true;
							vm.setState(VMessageAbstractItem.STATE_SENT_FALIED);
						}

						List<VMessageAbstractItem> items = vm.getItems();
						for (int j = 0; j < items.size(); j++) {
							VMessageAbstractItem item = items.get(j);
							if (uuid.equals(item.getUuid())) {
								if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FILE) {
									if (result == SEND_MESSAGE_SUCCESS)
										item.setState(VMessageAbstractItem.STATE_FILE_SENT);
									else
										item.setState(VMessageAbstractItem.STATE_FILE_SENT_FALIED);
								} else {
									if (result == SEND_MESSAGE_SUCCESS)
										item.setState(VMessageAbstractItem.STATE_SENT_SUCCESS);
									else
										item.setState(VMessageAbstractItem.STATE_SENT_FALIED);
								}
								break;
							}
						}

						if (vm.isResendMessage()
								&& result == SEND_MESSAGE_SUCCESS) {
							VMessage rsendVm = messageArray.get(i);
							rsendVm.isUpdateDate = true;
							messageArray.remove(rsendVm);
							messageArray.add(rsendVm);
							scrollToBottom();
						}
						Message.obtain(lh, ADAPTER_NOTIFY).sendToTarget();
						break;
					}
				}
				// handler kicked event
			} else if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_KICED_CROWD)) {
				GroupUserObject obj = intent.getParcelableExtra("group");
				if (obj == null) {
					V2Log.e(TAG,
							"Received the broadcast to quit the crowd group , but crowd id is wroing... ");
					return;
				}

				if (obj.getmGroupId() == remoteGroupID) {
					onBackPressed();
				}
			} else if ((PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION
					.equals(intent.getAction()))
					|| (PublicIntent.BROADCAST_CROWD_QUIT_NOTIFICATION
							.equals(intent.getAction()))
					|| PublicIntent.BROADCAST_DISCUSSION_QUIT_NOTIFICATION
							.equals(intent.getAction())) {
				finish();
			} else if ((JNIService.BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION
					.equals(intent.getAction()))) {
				if (currentConversationViewType == V2GlobalConstants.GROUP_TYPE_CROWD) {
					ArrayList<FileJNIObject> list = intent
							.getParcelableArrayListExtra("fileJniObjects");
					long groupID = intent.getLongExtra("groupID", -1l);
					if (list == null || list.size() <= 0 || groupID == -1l) {
						V2Log.e("ConversationView : May receive new group files failed.. get empty collection");
						return;
					}
					// 自己上传文件不提示
					if (list.get(0).user.mId == currentLoginUserID
							|| groupID != remoteGroupID)
						return;
					for (FileJNIObject fileJNIObject : list) {
						User user = GlobalHolder.getInstance().getUser(
								list.get(0).user.mId);
						VMessage vm = new VMessage(cov.getConversationType(),
								remoteGroupID, user, null, new Date(
										GlobalConfig.getGlobalServerTime()));
						vm.setUUID(fileJNIObject.vMessageID);
						VMessageFileItem item = new VMessageFileItem(vm,
								fileJNIObject.fileName,
								VMessageFileItem.STATE_FILE_SENT,
								fileJNIObject.fileId);
						item.setFileSize(fileJNIObject.fileSize);
						item.setUuid(fileJNIObject.fileId);
						addMessageToContainer(vm);
					}
				}
			} else if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_GROUP_USER_REMOVED)) {
				GroupUserObject obj = (GroupUserObject) intent.getExtras().get(
						"obj");
				if (obj == null) {
					V2Log.e(TAG,
							"JNI_BROADCAST_GROUP_USER_REMOVED --> Update Conversation failed that the user removed ... given GroupUserObject is null");
					return;
				}

				isFinishActivity = true;
			} else if (intent
					.getAction()
					.equals(PublicIntent.BROADCAST_CROWD_FILE_ACTIVITY_SEND_NOTIFICATION)) {
				String fileID = intent.getStringExtra("fileID");
				int exeType = intent.getIntExtra("exeType", -1);
				for (int i = 0; i < messageArray.size(); i++) {
					VMessage tempVm = messageArray.get(i);
					if (tempVm.getFileItems().size() > 0) {
						VMessageFileItem vMessageFileItem = tempVm
								.getFileItems().get(0);
						if (vMessageFileItem.getUuid().equals(fileID)) {
							if (exeType == VMessageAbstractItem.STATE_FILE_SENDING)
								vMessageFileItem
										.setState(VMessageAbstractItem.STATE_FILE_SENDING);
							else
								vMessageFileItem
										.setState(VMessageAbstractItem.STATE_FILE_SENT_FALIED);
							tempVm.isShowFailed = false;
							isScrollButtom = true;
							break;
						}
					}
				}
				Message.obtain(lh, ADAPTER_NOTIFY).sendToTarget();
			} else if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_FILE_STATUS_ERROR_NOTIFICATION)) {
				String fileID = intent.getStringExtra("fileID");
				int transType = intent.getIntExtra("transType", -1);
				if (fileID == null || transType == -1)
					return;

				for (int i = 0; i < messageArray.size(); i++) {
					VMessage vm = (VMessage) messageArray.get(i);
					if (vm.getFileItems().size() > 0) {
						VMessageFileItem vfi = vm.getFileItems().get(0);
						if (vfi.getUuid().equals(fileID)) {
							switch (transType) {
							case V2GlobalConstants.FILE_TRANS_SENDING:
								vfi.setDownloadedSize(0);
								vfi.setState(VMessageFileItem.STATE_FILE_SENT_FALIED);
								break;
							case V2GlobalConstants.FILE_TRANS_DOWNLOADING:
								vfi.setDownloadedSize(0);
								vfi.setState(VMessageFileItem.STATE_FILE_DOWNLOADED_FALIED);
								break;
							default:
								break;
							}
							break;
						}
					}
				}
				Message.obtain(lh, ADAPTER_NOTIFY).sendToTarget();
			} else if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION)) {
				NetworkStateCode code = (NetworkStateCode) intent.getExtras()
						.get("state");
				if (code != NetworkStateCode.CONNECTED) {
					for (int i = 0; i < messageArray.size(); i++) {
						VMessage tempVm = messageArray.get(i);
						if (tempVm.getFileItems().size() > 0) {
							VMessageFileItem vMessageFileItem = tempVm
									.getFileItems().get(0);
							if (vMessageFileItem.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING
									|| vMessageFileItem.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_DOWNLOADING) {
								vMessageFileItem
										.setState(VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED);
							}

							if (vMessageFileItem.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_SENDING
									|| vMessageFileItem.getState() == VMessageAbstractItem.STATE_FILE_SENDING) {
								vMessageFileItem
										.setState(VMessageAbstractItem.STATE_FILE_SENT_FALIED);
							}
							Message.obtain(lh, ADAPTER_NOTIFY).sendToTarget();
						}
					}
				}
			} else if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_GROUP_UPDATED)) {
				if (cov.getConversationType() == V2GlobalConstants.GROUP_TYPE_DEPARTMENT) {
					OrgGroup departmentGroup = (OrgGroup) GlobalHolder
							.getInstance().getGroupById(
									V2GlobalConstants.GROUP_TYPE_DEPARTMENT,
									cov.getExtId());
					mUserTitleTV.setText(departmentGroup.getName());
				} else if (currentConversationViewType == V2GlobalConstants.GROUP_TYPE_DISCUSSION) {
					DiscussionGroup discussionGroup = (DiscussionGroup) GlobalHolder
							.getInstance().getGroupById(
									V2GlobalConstants.GROUP_TYPE_DISCUSSION,
									cov.getExtId());
					mUserTitleTV.setText(discussionGroup.getName());
				} else if (currentConversationViewType == V2GlobalConstants.GROUP_TYPE_CROWD) {
					CrowdGroup crowdGroup = (CrowdGroup) GlobalHolder
							.getInstance().getGroupById(
									V2GlobalConstants.GROUP_TYPE_CROWD,
									cov.getExtId());
					mUserTitleTV.setText(crowdGroup.getName());
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
				if (array == null) {
					return;
				}

				int loadSize = array.size();
				if (loadSize > 0 && messageArray == null) {
					messageArray = new ArrayList<VMessage>();
				}

				for (int i = 0; i < array.size(); i++) {
					VMessage vm = array.get(i);
					// 群文件处理,如果是远端用户上传的文件，则强制更改状态为已上传，因为群中远端文件只有一种状态
					if (vm.getFileItems().size() > 0
							&& vm.getMsgCode() == V2GlobalConstants.GROUP_TYPE_CROWD) {
						VMessageFileItem fileItem = vm.getFileItems().get(0);
						// 如果该文件时其他人上传的，则在下载的时候，强制将聊天界面的状态改成已上传
						if (vm.getFromUser().getmUserId() != GlobalHolder
								.getInstance().getCurrentUserId()
								&& fileItem.getState() != VMessageAbstractItem.STATE_FILE_SENT) {
							fileItem.setState(VMessageAbstractItem.STATE_FILE_SENT);
						} else { // 如果该文件是自己上传的，则在下载的时候，强制将聊天界面的状态改成已上传
							if (fileItem.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED
									|| fileItem.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_DOWNLOADING
									|| fileItem.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADED
									|| fileItem.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING) {
								fileItem.setState(VMessageAbstractItem.STATE_FILE_SENT);
							}
						}
					}
					messageArray.add(0, vm);

					// 设置VMessage是否应该显示时间
					if (lastIntervalTime == 0) {
						lastIntervalTime = vm.getmDateLong();
						vm.setShowTime(true);
					} else {

						long currentMsgTime = vm.getmDateLong();
						if (lastIntervalTime - currentMsgTime < INTERVAL_TIME) {
							vm.setShowTime(false);
						} else {
							vm.setShowTime(true);
							lastIntervalTime = vm.getmDateLong();
						}
					}
				}

				LastFistItem = LastFistItem + loadSize;
				currentItemPos = loadSize - 1;
				if (currentItemPos == -1)
					currentItemPos = 0;
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
				scrollToPos(currentItemPos);
				isLoading = false;
				// 处理从个人信息传递过来的文件
				if (!sendFile) {
					sendFile = true;
					initSendFile();
				}
				break;
			case SEND_MESSAGE:
				VMessage sendMessage = (VMessage) msg.obj;
				mChat.sendVMessage(sendMessage, null);
				break;
			case SEND_MESSAGE_ERROR:
				break;
			case PLAY_NEXT_UNREAD_MESSAGE:
				boolean flag = playNextUnreadMessage();
				// To last message
				if (!flag) {
					playingAudioMessage = null;
				}
				break;
			case REQUEST_DEL_MESSAGE:
				VMessage message = (VMessage) msg.obj;
				removeMessage(message);
				adapter.notifyDataSetChanged();
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
				}
				break;
			case RECORD_STATUS_LISTENER:
				int result = msg.arg1;
				int recordType = msg.arg2;
				String fileID = (String) (((AsyncResult) msg.obj).getResult());
				if (result == Result.SUCCESS.value()) {
					if (recordType == V2GlobalConstants.RECORD_TYPE_START) {
						V2Log.d(TAG, "the file start recording , id is : "
								+ fileID);
					} else {
						String filePath = GlobalConfig.getGlobalAudioPath()
								+ "/" + fileID + ".mp3";
						if (successRecord && !isStopped) {
							V2Log.d(TAG,
									"the record file sending successfully! id is : "
											+ fileID);
							VMessage vm = MessageBuilder.buildAudioMessage(
									cov.getConversationType(), remoteGroupID,
									currentLoginUser, remoteChatUser, filePath,
									(int) recordTimes);
							// Send message to server
							sendMessageToRemote(vm);
						} else {
							// delete audio file
							File f = new File(filePath);
							if (f.exists())
								f.delete();
						}
						successRecord = false;
						recordTimes = 0;
					}
				} else {
					if (recordType == V2GlobalConstants.RECORD_TYPE_START) {
						V2Log.e(TAG, "record failed! error code is : " + result);
						breakRecording();
						String filePath = GlobalConfig.getGlobalAudioPath()
								+ "/" + fileID + ".mp3";
						File f = new File(filePath);
						if (f.exists())
							f.delete();
					} else {
						recordTimes = 0;
						successRecord = false;
					}
				}
				break;
			case ADAPTER_NOTIFY:
				adapter.notifyDataSetChanged();
				break;
			}
		}

	}

	/**
	 * get selected file path to send remote.
	 */
	public void sendSelectedFile(FileInfoBean bean) {

		if (bean == null || TextUtils.isEmpty(bean.filePath))
			return;

		VMessage vim = MessageBuilder.buildFileMessage(
				cov.getConversationType(), remoteGroupID, currentLoginUser,
				remoteChatUser, bean);
		sendMessageToRemote(vim);
	}

	public void adapterFileIcon(List<VMessageFileItem> fileItems) {

		for (VMessageFileItem vMessageFileItem : fileItems) {
			String fileName = vMessageFileItem.getFileName();
			FileType fileType = FileUitls.getFileType(fileName);
			vMessageFileItem.setFileType(fileType);
		}
	}

	@Override
	public void updateMessageBodyPopupWindow(MessageBodyView view) {
		showingPopupWindow = view;
	}

	@Override
	public void updateCrowdFileState(String fileID, VMessage vm,
			CrowdFileExeType type) {

		switch (type) {
		case ADD_FILE:
			if (messageArray == null)
				messageArray = new ArrayList<VMessage>();
			addMessageToContainer(vm);
			isScrollButtom = true;
			break;
		case DELETE_FILE:
			if (vm != null) {
				for (int i = 0; i < messageArray.size(); i++) {
					VMessage message = messageArray.get(i);
					if (message.getFileItems().size() > 0
							&& message.getFileItems().get(0).getUuid()
									.equals(fileID)) {
						deleteMessage(message, i);
						break;
					} else {
						V2Log.d(TAG, "删除聊天消息失败,没有从集合中找到该消息 id : " + vm.getId());
					}
				}
				Message.obtain(lh, ADAPTER_NOTIFY).sendToTarget();
				isScrollButtom = true;
			}
			break;
		case UPDATE_FILE:
			for (int i = 0; i < messageArray.size(); i++) {
				VMessage tempVm = messageArray.get(i);
				if (tempVm.getFileItems().size() > 0) {
					VMessageFileItem vMessageFileItem = tempVm.getFileItems()
							.get(0);
					if (vMessageFileItem.getUuid().equals(fileID)) {
						VMessageFileItem transItem = vm.getFileItems().get(0);
						vMessageFileItem.setState(transItem.getState());
						break;
					}
				}
			}
			Message.obtain(lh, ADAPTER_NOTIFY).sendToTarget();
			break;
		default:
			break;
		}
	}

	@Override
	public void notifyChatInterToReplace(final VMessage vm) {

		if (messageArray == null)
			return;

		for (int i = 0; i < messageArray.size(); i++) {
			VMessage replaced = messageArray.get(i);
			if (replaced.getUUID().equals(vm.getUUID())) {
				V2Log.e("binaryReplace",
						"ConversationTextActivity -- "
								+ "Recevice Binary data from server , and replaced wait! id is : "
								+ vm.getmXmlDatas());
				replaced.setImageItems(vm.getImageItems());
				Message.obtain(lh, ADAPTER_NOTIFY).sendToTarget();
				break;
			}
		}

	};
}
