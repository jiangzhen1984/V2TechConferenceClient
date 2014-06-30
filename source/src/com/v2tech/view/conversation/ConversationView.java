package com.v2tech.view.conversation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.ChatService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.MessageUtil;
import com.v2tech.util.V2Log;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.adapter.VMessageAdater;
import com.v2tech.view.contacts.ContactDetail;
import com.v2tech.view.widget.CommonAdapter;
import com.v2tech.view.widget.CommonAdapter.CommonAdapterItemWrapper;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFaceItem;
import com.v2tech.vo.VMessageTextItem;

public class ConversationView extends Activity {

	private final int START_LOAD_MESSAGE = 1;
	private final int LOAD_MESSAGE = 2;
	private final int END_LOAD_MESSAGE = 3;
	private final int SEND_MESSAGE = 4;
	private final int SEND_MESSAGE_DONE = 5;
	private final int QUERY_NEW_MESSAGE = 6;

	private final int BATCH_COUNT = 10;

	private int offset = 0;

	private long user1Id;

	private long user2Id;

	private long groupId;

	private String user2Name;

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

	private ImageView mAudioSpeakerIV;

	private View mShowContactDetailButton;

	private View mButtonRecordAudio;

	private MediaRecorder mRecorder = null;
	private MediaPlayer mPlayer;

	private MessageReceiver receiver = new MessageReceiver();

	private ChatService mChat = new ChatService();

	private User local;
	private User remote;

	private Conversation mCurrentConv;

	private ListView mMessagesContainer;

	private LinearLayout mFaceLayout;
	private View mSmileIconButton;

	private CommonAdapter adapter;

	private List<CommonAdapterItemWrapper> messageArray = new ArrayList<CommonAdapterItemWrapper>();

	private boolean isStopped;

	private int currentItemPos = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.activity_contact_message);

		lh = new LocalHandler();

		mMessagesContainer = (ListView) findViewById(R.id.conversation_message_list);
		adapter = new CommonAdapter(messageArray, mConvertListener);
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

		mAudioSpeakerIV = (ImageView) findViewById(R.id.contact_message_speaker);
		mAudioSpeakerIV.setOnClickListener(mMessageTypeSwitchListener);

		mButtonRecordAudio = findViewById(R.id.message_button_audio_record);
		mButtonRecordAudio.setOnTouchListener(mButtonHolderListener);

		mAdditionFeatureContainer = findViewById(R.id.contact_message_sub_feature_ly);

		mUserTitleTV = (TextView) findViewById(R.id.message_user_title);

		mFaceLayout = (LinearLayout) findViewById(R.id.contact_message_face_item_ly);

		user1Id = this.getIntent().getLongExtra("user1id", 0);
		user2Id = this.getIntent().getLongExtra("user2id", 0);
		groupId = this.getIntent().getLongExtra("gid", 0);
		user2Name = this.getIntent().getStringExtra("user2Name");

		local = GlobalHolder.getInstance().getUser(user1Id);
		remote = GlobalHolder.getInstance().getUser(user2Id);
		if (remote != null && remote.getName() != null) {
			user2Name = remote.getName();
		}

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

		if (groupId != 0) {
			mCurrentConv = GlobalHolder.getInstance().findConversationByType(
					Conversation.TYPE_GROUP, groupId);
		} else {
			mCurrentConv = GlobalHolder.getInstance().findConversationByType(
					Conversation.TYPE_CONTACT, user2Id);
		}
		//Register listener for avatar changed
		BitmapManager.getInstance().registerBitmapChangedListener(avatarChangedListener);

		IntentFilter filter = new IntentFilter();
		filter.addAction(JNIService.JNI_BROADCAST_NEW_MESSAGE);
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		registerReceiver(receiver, filter);

		if (mCurrentConv != null) {
			notificateConversationUpdate();
		}
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

		if (user1Id == 0 || user2Id == 0) {
			Toast.makeText(this, R.string.error_contact_messag_invalid_user_id,
					Toast.LENGTH_SHORT).show();
		}
		mUserTitleTV.setText(user2Name);

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.cancel(PublicIntent.MESSAGE_NOTIFICATION_ID);
		GlobalHolder.getInstance().CURRENT_CONVERSATION = mCurrentConv;
		GlobalHolder.getInstance().CURRENT_ID = user2Id;
		isStopped = false;
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
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	protected void onStop() {
		super.onStop();
		isStopped = true;
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
		BitmapManager.getInstance().registerBitmapChangedListener(avatarChangedListener);
		
		stopPlaying();
		releasePlayer();
		this.unregisterReceiver(receiver);
		cleanCache();
		GlobalHolder.getInstance().CURRENT_CONVERSATION = null;
		GlobalHolder.getInstance().CURRENT_ID = 0;
		V2Log.e("conversation view exited");
	}

	private void scrollToBottom() {
		scrollToPos(messageArray.size() - 1);
	}

	private void scrollToPos(final int pos) {
		if (pos < 0 || pos >= messageArray.size()) {
			return;
		}
		mMessagesContainer.post(new Runnable() {

			@Override
			public void run() {
				mMessagesContainer.setSelection(pos);

			}

		});

	}

	private synchronized boolean startPlaying(String fileName) {
		if (mPlayer != null && mPlayer.isPlaying()) {
			mPlayer.release();
		}
		mPlayer = new MediaPlayer();
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setVolume(1.0f, 1.0f);
		mPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO auto play next
			}

		});
		mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
				return false;
			}

		});
		try {
			if (mPlayer.isPlaying()) {
				mPlayer.stop();
			}
			mPlayer.setDataSource(fileName);
			mPlayer.prepare();
			mPlayer.start();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void stopPlaying() {
		if (mPlayer != null) {
			mPlayer.stop();
		}
	}

	private void releasePlayer() {
		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}
	}

	private Dialog mVoiceDialog = null;
	private ImageView mVolume;
	private View mSpeakingLayout;
	private View mPreparedCancelLayout;

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
		}

		if (mVoiceDialog.isShowing()) {
			mVoiceDialog.dismiss();
		} else {
			updateCancelSendVoiceMsgNotification(false);

			mVoiceDialog.show();
		}
	}

	private void updateCancelSendVoiceMsgNotification(boolean flag) {
		if (flag) {
			if (mSpeakingLayout != null) {
				mSpeakingLayout.setVisibility(View.GONE);
			}

			if (mPreparedCancelLayout != null) {
				mPreparedCancelLayout.setVisibility(View.VISIBLE);
			}
		} else {
			if (mSpeakingLayout != null) {
				mSpeakingLayout.setVisibility(View.VISIBLE);
			}
			if (mPreparedCancelLayout != null) {
				mPreparedCancelLayout.setVisibility(View.GONE);
			}
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
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
		mRecorder.setOutputFile(filePath);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

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
			VMessage vm = (VMessage) messageArray.get(before).getItemObject();
			vm.recycleAllImageMessage();
		}

		while (++after < size) {
			VMessage vm = (VMessage) messageArray.get(after).getItemObject();
			vm.recycleAllImageMessage();
		}
	}

	private void cleanCache() {
		messageArray.clear();
	}

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

	private OnTouchListener mButtonHolderListener = new OnTouchListener() {
		private String fileName = null;
		private long starttime = 0;

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
				starttime = System.currentTimeMillis();
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				boolean flag = false;
				Rect r = new Rect();
				view.getDrawingRect(r);
				// check if touch position out of button than cancel send voice
				// message
				if (r.contains((int) event.getX(), (int) event.getY())) {
					flag = false;
				} else {
					flag = true;
				}
				updateCancelSendVoiceMsgNotification(flag);
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				stopRecording();

				Rect r = new Rect();
				view.getDrawingRect(r);
				// check if touch position out of button than cancel send voice
				// message
				if (r.contains((int) event.getX(), (int) event.getY())) {
					// send
					VMessage vm = new VMessage(groupId, local, remote);
					int seconds = (int) ((System.currentTimeMillis() - starttime) / 1000) + 1;
					new VMessageAudioItem(vm, fileName, seconds);
					// Send message to server
					sendMessageToRemote(vm);
				} else {
					Toast.makeText(mContext,
							R.string.contact_message_message_cancelled,
							Toast.LENGTH_SHORT).show();
					File f = new File(fileName);
					f.deleteOnExit();
				}
				starttime = 0;
				fileName = null;
				lh.removeCallbacks(mUpdateMicStatusTimer);
				showOrCloseVoiceDialog();
			}
			return false;
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
			} else {
				mMoreFeatureIV.setImageResource(R.drawable.message_plus);
				mMoreFeatureIV.setTag("plus");
				mAdditionFeatureContainer.setVisibility(View.GONE);
			}
		}

	};

	private static final int SELECT_PICTURE = 1;

	private OnClickListener selectImageButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			startActivityForResult(
					Intent.createChooser(intent, "Select Picture"),
					SELECT_PICTURE);

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

			Drawable drawable = ((ImageView) (smile)).getDrawable();
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
					drawable.getIntrinsicHeight());

			String emoji = GlobalConfig
					.getEmojiStr(GlobalConfig.GLOBAL_FACE_ARRAY[Integer
							.parseInt(smile.getTag().toString())]);
			mMessageET.append(emoji);
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
		if (requestCode == SELECT_PICTURE) {
			if (resultCode == RESULT_OK) {
				Uri selectedImage = data.getData();
				String[] filePathColumn = { MediaStore.Images.Media.DATA };

				Cursor cursor = getContentResolver().query(selectedImage,
						filePathColumn, null, null, null);
				if (cursor == null) {
					Toast.makeText(mContext,
							R.string.error_contact_messag_invalid_image_path,
							Toast.LENGTH_SHORT).show();
					return;
				}
				cursor.moveToFirst();

				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				String filePath = cursor.getString(columnIndex);
				cursor.close();
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
			}
		}
	}

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
							new VMessageTextItem(vm, strTextContent);
						}

						int ind = GlobalConfig.getDrawableIndexByEmoji(str
								.subSequence(emojiStart, end).toString());
						if (ind > 0) {
							// new face item and add list
							new VMessageFaceItem(vm, ind);
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
				if (index == len - 1 && strStart < index) {
					String strTextContent = str.substring(strStart, index);
					new VMessageTextItem(vm, strTextContent);
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
		if (groupId == 0) {
			i.putExtra("extId", user2Id);
			i.putExtra("type", Conversation.TYPE_CONTACT);
		} else {
			i.putExtra("type", Conversation.TYPE_GROUP);
			i.putExtra("extId", groupId);
		}
		i.putExtra("noti", false);
		mContext.sendBroadcast(i);
	}

	private void addMessageToContainer(VMessage msg) {
		// make offset
		offset++;

		messageArray.add(new VMessageAdater(msg));
		adapter.notifyDataSetChanged();
		scrollToBottom();
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
				cleanRangeBitmapCache(first - 5, first + allVisibleCount
						+ BATCH_COUNT);
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
				startPlaying(vai.getAudioFilePath());
			}
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

	private void queryAndAddMessage(final int msgId) {

		VMessage m = MessageLoader.loadMessageById(mContext, msgId);
		if (m == null || m.getFromUser().getmUserId() != this.user2Id
				|| m.getGroupId() != this.groupId) {
			return;
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
			mContext.startActivity(i);
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
			return convertView;
		}
	};
	
	
	private BitmapManager.BitmapChangedListener avatarChangedListener = new BitmapManager.BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			if (user.getmUserId() == local.getmUserId() || user.getmUserId() == remote.getmUserId()) {
				adapter.notifyDataSetInvalidated();
			}
			
		}
		
	};

	private Runnable mUpdateMicStatusTimer = new Runnable() {
		public void run() {
			int ratio = mRecorder.getMaxAmplitude() / 600;
			int db = 0;// 分贝
			if (ratio > 1)
				db = (int) (20 * Math.log10(ratio));
			System.out.println("分贝值：" + db + "     " + Math.log10(ratio));
			updateVoiceVolume(db / 4);

			lh.postDelayed(mUpdateMicStatusTimer, 200);
		}
	};

	class MessageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (JNIService.JNI_BROADCAST_NEW_MESSAGE.equals(intent.getAction())) {
				Message.obtain(lh, QUERY_NEW_MESSAGE,
						intent.getExtras().get("mid")).sendToTarget();
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
					for (int i = 0; i < array.size(); i++) {
						messageArray.add(0, new VMessageAdater(array.get(i)));
					}
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
				scrollToPos(currentItemPos);
				adapter.notifyDataSetChanged();
				isLoading = false;
				break;
			case SEND_MESSAGE:

				mChat.sendVMessage((VMessage) msg.obj, new Registrant(this,
						SEND_MESSAGE_DONE, null));
				break;
			case QUERY_NEW_MESSAGE:
				if (msg.obj == null || "".equals(msg.obj.toString())) {
					break;
				}
				queryAndAddMessage(Integer.parseInt(msg.obj.toString()));
				break;
			}
		}

	}

}
