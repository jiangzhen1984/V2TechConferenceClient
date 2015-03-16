package com.bizcom.vc.activity.conversation;

import java.util.Date;
import java.util.List;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.bizcom.util.AlgorithmUtil;
import com.bizcom.util.FileUitls;
import com.bizcom.util.MessageUtil;
import com.bizcom.util.LocalSharedPreferencesStorage;
import com.bizcom.vc.activity.crow.CrowdFilesActivity.CrowdFileActivityType;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vc.listener.CommonCallBack;
import com.bizcom.vc.widget.cus.ChatTextView;
import com.bizcom.vo.FileDownLoadBean;
import com.bizcom.vo.User;
import com.bizcom.vo.VMessage;
import com.bizcom.vo.VMessageAbstractItem;
import com.bizcom.vo.VMessageAudioItem;
import com.bizcom.vo.VMessageFaceItem;
import com.bizcom.vo.VMessageFileItem;
import com.bizcom.vo.VMessageImageItem;
import com.bizcom.vo.VMessageLinkTextItem;
import com.bizcom.vo.VMessageTextItem;
import com.v2tech.R;

public class MessageBodyView extends LinearLayout {

	private static final String TAG = "MessageBodyView";
	private static final int MESSAGE_TYPE_TEXT = 11;
	private static final int MESSAGE_TYPE_NON_TEXT = 12;

	private static final int SENDING_CIRCLE_RATE = 600;

	private int messageType = MESSAGE_TYPE_TEXT;
	private VMessage mMsg;
	private ImageView mHeadIcon;
	private LinearLayout mContentContainer;
	private ImageView mArrowIV;
	private TextView timeTV;
	private TextView seconds;
	private TextView name;
	private View failedIcon;
	private View unReadIcon;
	private View sendingIcon;

	private View rootView;

	/**
	 * The flag decide that Whether should to display the Time View
	 */
	private boolean isShowTime;

	private boolean isSending;

	private LinearLayout mLocalMessageContainter;

	private LinearLayout mRemoteMessageContainter;

	private Handler localHandler;
	private Runnable popupWindowListener = null;

	private long lastUpdateTime;

	private ClickListener callback;
	private ImageView micIv;

	private int width = 0;
	private int popupWindowWidth;
	private int popupWindowHeight;
	private PopupWindow pw;
	private RelativeLayout popWindow;
	private TextView pwReDownloadTV;
	private TextView pwResendTV;
	private TextView pwCopyTV;
	private TextView pwDeleteTV;

	private RotateAnimation anima;
	private MessageBodyType bodyType;

	public MessageBodyView(Context context, VMessage m, boolean isShowTime) {
		super(context);
		initMessageBody(context, m, isShowTime);
	}

	public void initMessageBody(Context context, VMessage m, boolean isShowTime) {
		if (m == null) {
			V2Log.e(TAG, "Given VMessage Object is null!");
			return;
		}

		this.mMsg = m;
		this.isShowTime = isShowTime;
		this.localHandler = new Handler();

		initView();
		populateMessage();
		initPopupWindow();

		if (mMsg.isBeginSendingAnima) {
			updateSendingAnima(true);
		} else {
			updateSendingAnima(false);
		}
	}

	private void initView() {
		removeAllViews();
		if (mMsg.getMsgCode() == V2GlobalConstants.GROUP_TYPE_USER) {
			this.bodyType = MessageBodyType.SINGLE_USER_TYPE;
			rootView = LayoutInflater.from(getContext()).inflate(
					R.layout.message_body, null, false);
		} else {
			this.bodyType = MessageBodyType.GROUP_TYPE;
			rootView = LayoutInflater.from(getContext()).inflate(
					R.layout.crowd_message_body, null, false);
		}

		LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		this.addView(rootView, ll);

		anima = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		anima.setDuration(SENDING_CIRCLE_RATE);
		anima.setRepeatCount(RotateAnimation.INFINITE);

		timeTV = (TextView) rootView.findViewById(R.id.message_body_time_text);

		mLocalMessageContainter = (LinearLayout) rootView
				.findViewById(R.id.message_body_left_user_ly);
		mRemoteMessageContainter = (LinearLayout) rootView
				.findViewById(R.id.message_body_remote_ly);

		if (!mMsg.isLocal()) {
			mHeadIcon = (ImageView) rootView
					.findViewById(R.id.conversation_message_body_icon_left);
			mContentContainer = (LinearLayout) rootView
					.findViewById(R.id.messag_body_content_ly_left);
			mArrowIV = (ImageView) rootView
					.findViewById(R.id.message_body_arrow_left);
			seconds = (TextView) rootView
					.findViewById(R.id.message_body_video_item_second_left);
			failedIcon = rootView
					.findViewById(R.id.message_body_failed_item_left);
			unReadIcon = rootView
					.findViewById(R.id.message_body_unread_icon_left);
			sendingIcon = rootView
					.findViewById(R.id.message_body_sending_icon_left);
			mLocalMessageContainter.setVisibility(View.VISIBLE);
			mRemoteMessageContainter.setVisibility(View.INVISIBLE);

			if (mMsg.getAudioItems().size() > 0)
				updateSendingAnima(true);

		} else {
			mHeadIcon = (ImageView) rootView
					.findViewById(R.id.conversation_message_body_icon_right);
			mContentContainer = (LinearLayout) rootView
					.findViewById(R.id.messag_body_content_ly_right);
			mArrowIV = (ImageView) rootView
					.findViewById(R.id.message_body_arrow_right);
			seconds = (TextView) rootView
					.findViewById(R.id.message_body_video_item_second_right);
			failedIcon = rootView
					.findViewById(R.id.message_body_failed_item_right);
			unReadIcon = rootView
					.findViewById(R.id.message_body_unread_icon_right);
			sendingIcon = rootView
					.findViewById(R.id.message_body_sending_icon_right);
			mLocalMessageContainter.setVisibility(View.INVISIBLE);
			mRemoteMessageContainter.setVisibility(View.VISIBLE);

			if (mMsg.getFileItems().size() <= 0)
				updateSendingAnima(true);
		}

		updateChatUser();

		failedIcon.setVisibility(View.INVISIBLE);
		unReadIcon.setVisibility(View.GONE);
		sendingIcon.setVisibility(View.GONE);
		seconds.setVisibility(View.GONE);
		mArrowIV.bringToFront();

		if (isShowTime && mMsg.getDate() != null) {
			timeTV.setVisibility(View.VISIBLE);
			timeTV.setText(mMsg.getStringDate());
		} else {
			timeTV.setVisibility(View.GONE);
		}

		if (mMsg.getFromUser() != null && mMsg.getFromUser().isFromService()) {
			User fromUser = GlobalHolder.getInstance().getUser(
					mMsg.getFromUser().getmUserId());
			if (fromUser != null) {
				mMsg.setFromUser(fromUser);
			} else {
				V2Log.e(" MessageBody doesn't receve user["
						+ mMsg.getFromUser().getmUserId()
						+ "] information from server");
			}
		}

		if (mMsg.getFromUser() != null) {
			mHeadIcon.setImageBitmap(mMsg.getFromUser().getAvatarBitmap());
		}
		mContentContainer.setTag(this.mMsg);
		initListener();
	}

	public void initTextView() {

		ChatTextView contentView = new ChatTextView(getContext());
		contentView.setOnClickListener(messageClickListener);
		contentView.setBackgroundColor(Color.TRANSPARENT);
		contentView.setTextColor(Color.BLACK);
		contentView.setOnLongClickListener(messageLongClickListener);
		contentView.setOnTouchListener(touchListener);
		contentView.setSelected(false);

		List<VMessageAbstractItem> items = mMsg.getItems();
		if (mMsg.isAutoReply()) {
			contentView.append(getResources().getString(
					R.string.contact_message_auto_reply));
		}

		for (int i = 0; items != null && i < items.size(); i++) {
			VMessageAbstractItem item = items.get(i);
			// Add new layout for new line
			if (item.isNewLine() && contentView.length() != 0
					&& !mMsg.isAutoReply()) {
				contentView.append("\n");
			}

			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_TEXT) {
				contentView.append(((VMessageTextItem) item).getText());
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FACE) {
				Drawable dr = this
						.getResources()
						.getDrawable(
								GlobalConfig.GLOBAL_FACE_ARRAY[((VMessageFaceItem) item)
										.getIndex()]);
				dr.setBounds(0, 0, dr.getIntrinsicWidth(),
						dr.getIntrinsicHeight());

				contentView.append(".");

				SpannableStringBuilder builder = new SpannableStringBuilder(
						contentView.getText());
				ImageSpan is = new ImageSpan(dr);
				builder.setSpan(is, contentView.getText().length() - 1,
						contentView.getText().length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				contentView.setText(builder);
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_IMAGE) {
				messageType = MESSAGE_TYPE_NON_TEXT;
				Drawable dr = new BitmapDrawable(this.getContext()
						.getResources(),
						((VMessageImageItem) item).getCompressedBitmap());
				dr.setBounds(0, 0, dr.getIntrinsicWidth(),
						dr.getIntrinsicHeight());
				contentView.append(".");
				SpannableStringBuilder builder = new SpannableStringBuilder(
						contentView.getText());
				ImageSpan is = new ImageSpan(dr);
				builder.setSpan(is, contentView.getText().length() - 1,
						contentView.getText().length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				contentView.setText(builder);
				// AudioItem only has one item
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_LINK_TEXT) {
				String linkText = ((VMessageLinkTextItem) item).getText();
				SpannableStringBuilder style = new SpannableStringBuilder(
						((VMessageLinkTextItem) item).getText());
				style.setSpan(new ForegroundColorSpan(Color.BLUE), 0,
						linkText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				style.setSpan(new UnderlineSpan(), 0, linkText.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				contentView.append(style);
			}
		}

		LinearLayout.LayoutParams contentLayout = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		mContentContainer.addView(contentView, contentLayout);
	}

	private void initListener() {
		mContentContainer.setOnLongClickListener(messageLongClickListener);
		mContentContainer.setOnTouchListener(touchListener);
		mContentContainer.setOnClickListener(messageClickListener);
	}

	private void initPopupWindow() {
		LayoutInflater inflater = (LayoutInflater) getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		popWindow = (RelativeLayout) inflater.inflate(
				R.layout.message_selected_pop_up_window, null);
		pw = new PopupWindow(popWindow, LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT, true);
		pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		pw.setFocusable(true);
		pw.setTouchable(true);
		pw.setOutsideTouchable(true);
		// ViewTreeObserver viewTreeObserver =
		// popupContent.getViewTreeObserver();
		// viewTreeObserver.addOnPreDrawListener(new OnPreDrawListener() {
		//
		// @Override
		// public boolean onPreDraw() {
		// if(popupWindowWidth == 0)
		// popupWindowWidth = popupContent.getMeasuredWidth();
		//
		// if(popupWindowHeight == 0)
		// popupWindowHeight = popupContent.getMeasuredHeight();
		// V2Log.e(TAG, "addOnPreDrawListener 被回调");
		// return true;
		// }
		// });
		pwResendTV = (TextView) popWindow
				.findViewById(R.id.contact_message_pop_up_item_resend);
		pwResendTV.setOnClickListener(mResendButtonListener);

		pwReDownloadTV = (TextView) popWindow
				.findViewById(R.id.contact_message_pop_up_item_redownload);
		pwReDownloadTV.setOnClickListener(mResendButtonListener);

		if (messageType == MESSAGE_TYPE_TEXT) {
			pwCopyTV = (TextView) popWindow
					.findViewById(R.id.contact_message_pop_up_item_copy);
			pwCopyTV.setVisibility(View.VISIBLE);
			pwCopyTV.setOnClickListener(mCopyButtonListener);
		}

		pwDeleteTV = (TextView) popWindow
				.findViewById(R.id.contact_message_pop_up_item_delete);
		pwDeleteTV.setOnClickListener(mDeleteButtonListener);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		popWindow.getChildAt(0).measure(MeasureSpec.UNSPECIFIED,
				MeasureSpec.UNSPECIFIED);
		popupWindowHeight = popWindow.getChildAt(0).getMeasuredHeight();
		popupWindowWidth = popWindow.getChildAt(0).getMeasuredWidth();
	}

	private void populateMessage() {
		List<VMessageAudioItem> audioItems = mMsg.getAudioItems();
		if (audioItems.size() > 0) {
			messageType = MESSAGE_TYPE_NON_TEXT;
			populateAudioMessage(audioItems);
			return;
		}

		List<VMessageFileItem> fileItems = mMsg.getFileItems();
		if (fileItems.size() > 0) {
			messageType = MESSAGE_TYPE_NON_TEXT;
			populateFileItem(fileItems);
			return;
		}

		if (mMsg.getState() == VMessageAbstractItem.STATE_SENT_FALIED) {
			failedIcon.setVisibility(View.VISIBLE);
		} else {
			failedIcon.setVisibility(View.GONE);
		}

		initTextView();
	}

	/**
	 * Now only handle one audio message
	 * 
	 * @param audioItems
	 */
	private void populateAudioMessage(List<VMessageAudioItem> audioItems) {
		final VMessageAudioItem item = audioItems.get(0);
		if (item.getReadState() == VMessageAbstractItem.STATE_UNREAD)
			unReadIcon.setVisibility(View.VISIBLE);
		else
			unReadIcon.setVisibility(View.INVISIBLE);

		if (item.getState() == VMessageAbstractItem.STATE_SENT_FALIED)
			failedIcon.setVisibility(View.VISIBLE);
		else
			failedIcon.setVisibility(View.INVISIBLE);

		seconds.setVisibility(View.VISIBLE);
		seconds.setText(item.getSeconds() + "''");

		RelativeLayout audioRoot = new RelativeLayout(getContext());
		micIv = new ImageView(getContext());
		micIv.setId(micIv.hashCode());

		RelativeLayout.LayoutParams micIvLy = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);

		TextView tv = new TextView(getContext());
		tv.setId(tv.hashCode());
		for (int in = 0; in < item.getSeconds() && in < 40; in++) {
			tv.append(" ");
		}

		RelativeLayout.LayoutParams tvIvLy = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);

		if (mMsg.isLocal()) {
			micIv.setImageResource(R.drawable.voice_message_mic_icon_self_selector);
			audioRoot.addView(tv, tvIvLy);

			micIvLy.addRule(RelativeLayout.RIGHT_OF, tv.getId());
			audioRoot.addView(micIv, micIvLy);

		} else {
			micIvLy.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			micIvLy.addRule(RelativeLayout.CENTER_IN_PARENT);
			micIv.setImageResource(R.drawable.voice_message_mic_icon_selector);
			audioRoot.addView(micIv, micIvLy);

			tvIvLy.addRule(RelativeLayout.RIGHT_OF, micIv.getId());
			audioRoot.addView(tv, tvIvLy);
		}

		audioRoot.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (item != null && !TextUtils.isEmpty(item.getAudioFilePath())) {
					if (item.isPlaying()) {
						callback.requestStopAudio(mMsg, item);
					} else {
						callback.requestStopOtherAudio(mMsg);
						callback.requestPlayAudio(MessageBodyView.this, mMsg,
								item);
						updateUnreadFlag(false, item);
					}
				}
			}

		});

		audioRoot.setOnLongClickListener(messageLongClickListener);
		audioRoot.setOnTouchListener(touchListener);
		mContentContainer.addView(audioRoot, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

	}

	private void populateFileItem(List<VMessageFileItem> fileItems) {
		VMessageFileItem item = fileItems.get(0);
		View fileRootView = LayoutInflater.from(getContext()).inflate(
				R.layout.message_body_file_item, null, false);
		ImageView fileIcon = (ImageView) fileRootView
				.findViewById(R.id.message_body_file_item_icon_ly);
		TextView fileName = (TextView) fileRootView
				.findViewById(R.id.message_body_file_item_file_name);
		fileName.setText(item.getFileName());

		fileIcon.setBackgroundResource(FileUitls.adapterFileIcon(item
				.getFileType()));

		TextView fileSize = (TextView) fileRootView
				.findViewById(R.id.message_body_file_item_file_size);
		fileSize.setText(item.getFileSizeStr());

		updateFileItemView(item, fileRootView);
		fileRootView.setOnClickListener(fileMessageItemClickListener);
		fileRootView.setOnLongClickListener(messageLongClickListener);
		fileRootView.setOnTouchListener(touchListener);

		fileRootView.setTag(item);
		mContentContainer.setGravity(Gravity.CENTER_VERTICAL);
		mContentContainer.addView(fileRootView, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

	}

	private void updateSelectedBg(boolean selected) {
		if (selected) {
			if (!mMsg.isLocal()) {
				mContentContainer
						.setBackgroundResource(R.drawable.message_body_bg_selected);
				mArrowIV.setImageResource(R.drawable.message_body_arrow_left_selected);
			}
		} else {
			if (!mMsg.isLocal()) {
				mContentContainer
						.setBackgroundResource(R.drawable.message_body_bg);
				mArrowIV.setImageResource(R.drawable.message_body_arrow_left);
			}
		}
	}

	private void showPopupWindow(final View anchor) {

		popupWindowListener = new Runnable() {

			@Override
			public void run() {
				if (!anchor.isShown()) {
					return;
				}

				if (messageType == MESSAGE_TYPE_TEXT)
					pwCopyTV.setVisibility(View.VISIBLE);

				if (failedIcon.getVisibility() == View.VISIBLE) {
					if (mMsg.isLocal()) {
						pwResendTV.setVisibility(View.VISIBLE);
					}
					// else {
					// pwReDownloadTV.setVisibility(View.VISIBLE);
					// }
				} else {
					pwResendTV.setVisibility(View.GONE);
				}

				// arrow.measure(MeasureSpec.UNSPECIFIED,
				// MeasureSpec.UNSPECIFIED);
				// int arrowWidth = arrow.getMeasuredWidth();

				int viewWidth = anchor.getMeasuredWidth();

				int[] location = new int[2];
				anchor.getLocationInWindow(location);

				int left = location[0];
				int top = location[1];

				if (left == 0 || top == 0) {
					Rect rect = new Rect();
					anchor.getGlobalVisibleRect(rect);
					left = rect.left;
					top = rect.top;
				}

				// int offsetX = left + (viewWidth / 2) - (popupWindowWidth /
				// 2);
				// int offsetY = top - popupWindowHeight;

				int offsetX = rawX - (popupWindowWidth / 2);
				int offsetY = rawY - popupWindowHeight;

				if (offsetY < 0)
					offsetY = Math.abs(offsetY);
				pw.showAtLocation((View) anchor.getParent(),
						Gravity.NO_GRAVITY, offsetX, offsetY);

				updateSelectedBg(false);
				popupWindowListener = null;
			}

		};

		localHandler.postDelayed(popupWindowListener, 200);

	}

	public VMessage getItem() {
		return this.mMsg;
	}

	public void setCallback(ClickListener cl) {
		this.callback = cl;
	}

	public ClickListener getCallback() {
		return callback;
	}

	public void updateUnreadFlag(boolean flag, VMessageAudioItem item) {
		if (!flag) {
			item.setState(VMessageAbstractItem.STATE_READED);
			this.unReadIcon.setVisibility(View.GONE);
		} else {
			item.setState(VMessageAbstractItem.STATE_UNREAD);
			this.unReadIcon.setVisibility(View.VISIBLE);
		}

		MessageLoader.updateBinaryAudioItem(item);
	}

	public void updateFailedFlag(boolean flag) {
		if (!flag) {
			this.failedIcon.setVisibility(View.INVISIBLE);
		} else {
			this.failedIcon.setVisibility(View.VISIBLE);
		}
	}

	public void updateSendingAnima(boolean isBegin) {
		if (!isBegin) {
			sendingIcon.setVisibility(View.GONE);
			sendingIcon.clearAnimation();
			isSending = false;
		} else {
			sendingIcon.setVisibility(View.VISIBLE);
			sendingIcon.startAnimation(anima);
			isSending = true;
		}
	}

	public void startVoiceAnimation() {

		if (mMsg.isLocal())
			micIv.setImageResource(R.drawable.conversation_local_speaking);
		else
			micIv.setImageResource(R.drawable.conversation_remote_speaking);

		AnimationDrawable drawable = (AnimationDrawable) micIv.getDrawable();
		drawable.start();
	}

	public void stopVoiceAnimation() {

		if (mMsg.isLocal())
			micIv.setImageResource(R.drawable.conversation_local_speaking);
		else
			micIv.setImageResource(R.drawable.conversation_remote_speaking);

		AnimationDrawable drawable = (AnimationDrawable) micIv.getDrawable();
		drawable.stop();

		if (mMsg.isLocal())
			micIv.setImageResource(R.drawable.voice_message_mic_icon_self_selector);
		else
			micIv.setImageResource(R.drawable.voice_message_mic_icon_selector);
	}

	public void updateView(VMessage vm) {
		if (vm == null) {
			V2Log.e("Can't not update data vm is null");
			return;
		}

		long oldUserID = this.mMsg.getFromUser().getmUserId();
		String olderUuid = this.mMsg.getUUID();
		boolean isLocal = this.mMsg.isLocal();
		this.mMsg = vm;
		this.isShowTime = vm.isShowTime();
		if (!olderUuid.equals(vm.getUUID()) && isLocal != vm.isLocal()) {
			initView();
		} else {
			if (oldUserID != this.mMsg.getFromUser().getmUserId()) {
				updateChatUser();
			}
			
			if ((mMsg.isUpdateAvatar && 
					oldUserID == this.mMsg.getFromUser().getmUserId()) || 
					oldUserID != this.mMsg.getFromUser().getmUserId())
				updateAvatar(mMsg.getFromUser().getAvatarBitmap());

			if (isShowTime && mMsg.getDate() != null) {
				timeTV.setVisibility(View.VISIBLE);
				timeTV.setText(mMsg.getStringDate());
			} else {
				timeTV.setVisibility(View.GONE);
			}

			if (mMsg.isBeginSendingAnima) {
				updateSendingAnima(true);
			} else {
				updateSendingAnima(false);
			}

			if (mMsg.isShowFailed) {
				updateFailedFlag(true);
			} else {
				updateFailedFlag(false);
			}

			if (mMsg.isUpdateDate) {
				updateDate();
				mMsg.isUpdateDate = false;
			}

			unReadIcon.setVisibility(View.GONE);
			seconds.setVisibility(View.GONE);
			mContentContainer.setTag(this.mMsg);
		}

		mContentContainer.removeAllViews();
		populateMessage();
	}

	public void updateView(VMessageFileItem vfi) {
		if (vfi == null || mMsg.getFileItems().size() < 0
				|| !vfi.getUuid().equals(mMsg.getFileItems().get(0).getUuid())) {
			return;
		}

		View fileRootView = mContentContainer.getChildAt(0);
		updateFileItemView(vfi, fileRootView);

	}

	public void updateDate() {
		mMsg.setDate(new Date(GlobalConfig.getGlobalServerTime()));
		if (isShowTime && mMsg.getDate() != null) {
			timeTV.setVisibility(View.VISIBLE);
			timeTV.setText(mMsg.getStringDate());
		} else {
			timeTV.setVisibility(View.GONE);
		}
	}

	private void updateChatUser() {
		if (!mMsg.isLocal()) {
			User fromUser = mMsg.getFromUser();
			if (bodyType == MessageBodyType.GROUP_TYPE) {
				name = (TextView) rootView
						.findViewById(R.id.message_body_person_name_left);
				if (fromUser != null) {
					name.setText(fromUser.getDisplayName());
				}
			}

		} else {

			User localUser = GlobalHolder.getInstance().getCurrentUser();
			if (bodyType == MessageBodyType.GROUP_TYPE) {
				name = (TextView) rootView
						.findViewById(R.id.message_body_person_name_right);
				if (localUser != null)
					name.setText(localUser.getDisplayName());
			}

		}
	}

	/**
	 * Use to user update avatar or re-connect network
	 * 
	 * @param bmp
	 */
	public void updateHeadIcon(Bitmap bmp) {
		mHeadIcon.setImageBitmap(bmp);
	}

	public void dissmisPopupWindow() {
		if (pw.isShowing())
			pw.dismiss();
	}

	private void updateFileItemView(VMessageFileItem vfi, View rootView) {
		TextView state = (TextView) rootView
				.findViewById(R.id.message_body_file_item_state);
		ImageView button = (ImageView) rootView
				.findViewById(R.id.message_body_file_item_progress_action_button);
		boolean showProgressLayout = updateFileItemStateText(vfi, state, button);

		if (showProgressLayout) {
			rootView.findViewById(R.id.message_body_file_item_progress_layout)
					.setVisibility(View.VISIBLE);
			TextView progress = (TextView) rootView
					.findViewById(R.id.message_body_file_item_progress_size);
			TextView speed = (TextView) rootView
					.findViewById(R.id.message_body_file_item_progress_speed);
			final View iv = rootView
					.findViewById(R.id.message_body_file_item_progress_state);
			final android.view.ViewGroup.LayoutParams params = iv
					.getLayoutParams();
			FileDownLoadBean bean = GlobalHolder.getInstance().globleFileProgress
					.get(vfi.getUuid());
			if (bean != null) {
				vfi.setDownloadedSize(bean.currentLoadSize);
			}
			// 设置 已下载/文件大小 显示状态
			progress.setText(vfi.getDownloadSizeStr() + "/"
					+ vfi.getFileSizeStr());
			// 设置速度
			if (vfi.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_SENDING
					|| vfi.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_DOWNLOADING) {
				speed.setText("0kb/s");
			} else {
				if (bean != null) {
					V2Log.e(TAG, "lastLoadTime : " + bean.lastLoadTime
							+ " lastLoadSize : " + bean.lastLoadSize
							+ " currentLoadSize : " + bean.currentLoadSize);
					lastUpdateTime = bean.lastLoadTime;
					vfi.setDownloadedSize(bean.currentLoadSize);
					long sec = (System.currentTimeMillis() - lastUpdateTime);
					long size = vfi.getDownloadedSize() - bean.lastLoadSize;
					vfi.setSpeed((size / sec) * 1000);
					speed.setText(vfi.getSpeedStr());
				} else {
					lastUpdateTime = System.currentTimeMillis();
					speed.setText("0kb/s");
				}
			}
			// 设置进度
			final float percent = (float) ((double) vfi.getDownloadedSize() / (double) vfi
					.getFileSize());
			final ViewGroup progressC = (ViewGroup) rootView
					.findViewById(R.id.message_body_file_item_progress_state_ly);
			ViewTreeObserver viewTreeObserver = progressC.getViewTreeObserver();
			viewTreeObserver.addOnPreDrawListener(new OnPreDrawListener() {

				@Override
				public boolean onPreDraw() {
					if (width == 0) {
						width = progressC.getMeasuredWidth();
						V2Log.d(TAG, "total width：" + width);
						params.width = (int) (width * percent);
						iv.setLayoutParams(params);
					}
					return true;
				}
			});
			// progressC.measure(View.MeasureSpec.makeMeasureSpec(0,
			// View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
			// .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
			// width = progressC.getMeasuredWidth();
			params.width = (int) (width * percent);
			iv.setLayoutParams(params);

		} else {
			rootView.findViewById(R.id.message_body_file_item_progress_layout)
					.setVisibility(View.GONE);
		}
	}

	private boolean updateFileItemStateText(VMessageFileItem vfi,
			TextView view, ImageView actionButton) {
		String strState = "";
		boolean showProgressLayout = false;
		if (vfi.getVm().getMsgCode() == V2GlobalConstants.GROUP_TYPE_CROWD)
			actionButton.setVisibility(View.GONE);
		else
			actionButton.setVisibility(View.VISIBLE);
		if (vfi.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING) {
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_downloading)
					.toString();
			actionButton.setImageResource(R.drawable.message_file_pause_button);
			showProgressLayout = true;
		} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_SENDING) {
			// 区分上传与发送
			if (MessageBodyType.GROUP_TYPE == bodyType)
				strState = getContext().getResources()
						.getText(R.string.contact_message_file_item_uploading)
						.toString();
			else
				strState = getContext().getResources()
						.getText(R.string.contact_message_file_item_sending)
						.toString();
			actionButton.setImageResource(R.drawable.message_file_pause_button);
			showProgressLayout = true;
		} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_SENDING
				|| vfi.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_DOWNLOADING) {
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_pause)
					.toString();
			actionButton
					.setImageResource(R.drawable.message_file_download_button);
			showProgressLayout = true;
		} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_SENT_FALIED) {
			// 区分上传与发送
			if (MessageBodyType.GROUP_TYPE == bodyType)
				strState = getContext()
						.getResources()
						.getText(
								R.string.contact_message_file_item_upload_failed)
						.toString();
			else
				strState = getContext()
						.getResources()
						.getText(R.string.contact_message_file_item_sent_failed)
						.toString();
			// Show failed icon
			failedIcon.setVisibility(View.VISIBLE);
			actionButton.setVisibility(View.GONE);
		} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED) {
			// 区分上传与发送
			if (MessageBodyType.GROUP_TYPE == bodyType)
				strState = getContext().getResources()
						.getText(R.string.contact_message_file_item_uploaded)
						.toString();
			else {
				strState = getContext()
						.getResources()
						.getText(
								R.string.contact_message_file_item_download_failed)
						.toString();
				// Show failed icon
				failedIcon.setVisibility(View.VISIBLE);
				actionButton.setVisibility(View.GONE);
			}
		} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_MISS_DOWNLOAD) {
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_miss_download)
					.toString();
			actionButton.setVisibility(View.GONE);
		} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_UNDOWNLOAD) {
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_miss_download)
					.toString();
			actionButton.setVisibility(View.GONE);
		} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_SENT) {
			// 区分上传与发送
			if (MessageBodyType.GROUP_TYPE == bodyType)
				strState = getContext().getResources()
						.getText(R.string.contact_message_file_item_uploaded)
						.toString();
			else
				strState = getContext().getResources()
						.getText(R.string.contact_message_file_item_sent)
						.toString();
			actionButton.setVisibility(View.GONE);
		} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADED) {
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_downloaded)
					.toString();
			actionButton.setVisibility(View.GONE);
		}

		view.setText(strState);
		return showProgressLayout;
	}

	private OnLongClickListener messageLongClickListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View anchor) {
			CommonCallBack.getInstance().executeUpdatePopupWindowState(
					MessageBodyView.this);
			showPopupWindow(mContentContainer);
			return false;
		}

	};

	private int rawX;
	private int rawY;
	private OnTouchListener touchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				V2Log.e(TAG,
						"x :" + event.getRawX() + " y : " + event.getRawY());
				rawX = (int) event.getRawX();
				rawY = (int) event.getRawY();
			}
			return false;
		}
	};

	public VMessage getMsg() {
		return this.mMsg;
	}

	public void updateAvatar(Bitmap bmp) {
		mHeadIcon.setImageBitmap(bmp);
	}

	public boolean isSending() {
		return isSending;
	}

	private OnClickListener fileMessageItemClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if(AlgorithmUtil.isFastClick())
				return ;
			
			VMessageFileItem item = (VMessageFileItem) view.getTag();

			if (callback != null) {
				if (mMsg.getMsgCode() == V2GlobalConstants.GROUP_TYPE_CROWD) {
					if (item.getState() == VMessageAbstractItem.STATE_FILE_SENDING
							|| item.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_SENDING
							|| item.getState() == VMessageAbstractItem.STATE_FILE_SENT_FALIED)
						callback.onCrowdFileMessageClicked(CrowdFileActivityType.CROWD_FILE_UPLOING_ACTIVITY);
					else
						callback.onCrowdFileMessageClicked(CrowdFileActivityType.CROWD_FILE_ACTIVITY);
				} else {
					if (item.getState() == VMessageFileItem.STATE_FILE_DOWNLOADED) {
						FileUitls.openFile(item.getFilePath());
					} else {
						if (item.getState() == VMessageFileItem.STATE_FILE_UNDOWNLOAD) {
							if (GlobalHolder.getInstance().mFailedFiles
									.contains(item.getUuid())) {
								item.setState(VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED);
								GlobalHolder.getInstance().mFailedFiles
										.remove(item.getUuid());
							} else {
								long key;
								if (mMsg.getMsgCode() == V2GlobalConstants.GROUP_TYPE_USER)
									key = mMsg.getToUser().getmUserId();
								else
									key = mMsg.getGroupId();
								boolean flag = GlobalHolder
										.getInstance()
										.changeGlobleTransFileMember(
												V2GlobalConstants.FILE_TRANS_DOWNLOADING,
												getContext(), true, key,
												"MessageBodyView fileMessageItemClickListener");
								if (!flag)
									return;
								callback.requestDownloadFile(view,
										item.getVm(), item);
								item.setState(VMessageFileItem.STATE_FILE_DOWNLOADING);
							}
						} else if (item.getState() == VMessageFileItem.STATE_FILE_SENDING) {
							callback.requestPauseTransFile(view, item.getVm(),
									item);
							item.setState(VMessageFileItem.STATE_FILE_PAUSED_SENDING);
						} else if (item.getState() == VMessageFileItem.STATE_FILE_PAUSED_SENDING) {
							callback.requestResumeTransFile(view, item.getVm(),
									item);
							item.setState(VMessageFileItem.STATE_FILE_SENDING);
						} else if (item.getState() == VMessageFileItem.STATE_FILE_DOWNLOADING) {
							callback.requestPauseDownloadFile(view,
									item.getVm(), item);
							item.setState(VMessageFileItem.STATE_FILE_PAUSED_DOWNLOADING);
						} else if (item.getState() == VMessageFileItem.STATE_FILE_PAUSED_DOWNLOADING) {
							callback.requestResumeDownloadFile(view,
									item.getVm(), item);
							item.setState(VMessageFileItem.STATE_FILE_DOWNLOADING);
						}
						MessageLoader.updateFileItemState(getContext(), item);
						updateView(item);
					}
				}
			}
		}

	};

	private OnClickListener messageClickListener = new OnClickListener() {

		@Override
		public void onClick(View anchor) {
			if (callback != null) {
				// VMessage vm =(VMessage) anchor.getTag();
				List<VMessageImageItem> vl = mMsg.getImageItems();
				if (vl != null && vl.size() > 0) {
					callback.onMessageClicked(mMsg);
					return;
				}

				List<VMessageAudioItem> al = mMsg.getAudioItems();
				if (al != null && al.size() > 0) {
					VMessageAudioItem audioItem = al.get(0);
					if (audioItem.isPlaying()) {
						callback.requestStopAudio(mMsg, audioItem);
					} else {
						callback.requestPlayAudio(MessageBodyView.this, mMsg,
								audioItem);
						audioItem.setPlaying(true);
					}
					return;
				}

				List<VMessageLinkTextItem> linkItems = mMsg.getLinkItems();
				if (linkItems != null && linkItems.size() > 0) {
					callback.onMessageClicked(mMsg);
					return;
				}
			}
		}

	};

	private OnClickListener mDeleteButtonListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			if (callback != null) {
				callback.requestDelMessage(mMsg);
			}
			pw.dismiss();
		}

	};

	private OnClickListener mCopyButtonListener = new OnClickListener() {
		@Override
		public void onClick(View view) {

			ClipboardManager clipboard = (ClipboardManager) getContext()
					.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("label",
					MessageUtil.getMixedConversationCopyedContent(mMsg));

			clipboard.setPrimaryClip(clip);
			pw.dismiss();
		}

	};

	private OnClickListener mResendButtonListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			if (LocalSharedPreferencesStorage.checkCurrentAviNetwork(getContext())) {
				if (callback != null) {
					View fileRootView = mContentContainer.getChildAt(0);
					if (mMsg.isLocal()) {
						if (mMsg.getItems().size() > 0
								&& mMsg.getItems().get(0).getType() == VMessageFileItem.ITEM_TYPE_FILE) {
							long key;
							if (mMsg.getMsgCode() == V2GlobalConstants.GROUP_TYPE_USER)
								key = mMsg.getToUser().getmUserId();
							else
								key = mMsg.getGroupId();
							boolean flag = GlobalHolder
									.getInstance()
									.changeGlobleTransFileMember(
											V2GlobalConstants.FILE_TRANS_SENDING,
											getContext(), true, key,
											"MessageBodyView mResendButtonListener");
							if (!flag)
								return;
						}

						failedIcon.setVisibility(View.INVISIBLE);
						callback.reSendMessageClicked(mMsg);

						if (mMsg.getItems().size() > 0
								&& mMsg.getItems().get(0).getType() == VMessageFileItem.ITEM_TYPE_FILE) {
							VMessageFileItem fileItem = (VMessageFileItem) mMsg
									.getItems().get(0);
							lastUpdateTime = 0;
							fileItem.setDownloadedSize(0);
							updateFileItemView(fileItem, fileRootView);
						}
					} else {
						if (mMsg.getItems().size() > 0
								&& mMsg.getItems().get(0).getType() == VMessageFileItem.ITEM_TYPE_FILE) {
							failedIcon.setVisibility(View.INVISIBLE);
							mMsg.setState(VMessageAbstractItem.STATE_NORMAL);
							VMessageFileItem fileItem = (VMessageFileItem) mMsg
									.getItems().get(0);
							callback.requestDownloadFile(fileRootView, mMsg,
									fileItem);
							fileItem.setState(VMessageAbstractItem.STATE_FILE_DOWNLOADING);
							fileItem.setDownloadedSize(0);
							updateFailedFlag(false);
							updateFileItemView(fileItem, fileRootView);
							MessageLoader.updateFileItemState(getContext(),
									fileItem);
							MessageLoader.updateChatMessageState(getContext(),
									mMsg);
						}
					}
				}
			} else {
				Toast.makeText(
						getContext(),
						R.string.conversation_message_bodyView_network_connection_not_availabl,
						Toast.LENGTH_SHORT).show();
			}
			pw.dismiss();
		}

	};

	class LoadTask extends AsyncTask<ImageView, Void, ImageView[]> {

		@Override
		protected ImageView[] doInBackground(ImageView... vms) {
			// Only has one element
			for (ImageView vm : vms) {
				Bitmap bm = ((VMessageImageItem) vm.getTag())
						.getCompressedBitmap();
				if (bm == null || bm.isRecycled()) {
					((VMessageImageItem) vm.getTag()).recycleAll();
				}
				((VMessageImageItem) vm.getTag()).getCompressedBitmap();
			}
			return vms;
		}

		@Override
		protected void onPostExecute(ImageView[] result) {
			// Only has one element
			ImageView vm = result[0];
			// If loaded vm is not same member's, means current view has changed
			// message, ignore this result
			VMessageImageItem item = ((VMessageImageItem) vm.getTag());
			if (item.getVm() != mMsg) {
				return;
			}
			Bitmap bm = item.getCompressedBitmap();
			vm.setImageBitmap(bm);
		}

	}

	public interface ClickListener {
		public void onMessageClicked(VMessage v);

		public void onCrowdFileMessageClicked(CrowdFileActivityType openType);

		public void reSendMessageClicked(VMessage v);

		public void requestDelMessage(VMessage v);

		public void requestPlayAudio(MessageBodyView view, VMessage vm,
				VMessageAudioItem vai);

		public void requestStopAudio(VMessage vm, VMessageAudioItem vai);

		public void requestDownloadFile(View v, VMessage vm,
				VMessageFileItem vfi);

		public void requestPauseDownloadFile(View v, VMessage vm,
				VMessageFileItem vfi);

		public void requestResumeDownloadFile(View v, VMessage vm,
				VMessageFileItem vfi);

		public void requestPauseTransFile(View v, VMessage vm,
				VMessageFileItem vfi);

		public void requestResumeTransFile(View v, VMessage vm,
				VMessageFileItem vfi);

		public void requestStopOtherAudio(VMessage vm);
	}

	public VMessage getmMsg() {
		return mMsg;
	}

	public void setmMsg(VMessage mMsg) {
		this.mMsg = mMsg;
	}
}

enum MessageBodyType {

	SINGLE_USER_TYPE(0), GROUP_TYPE(1), UNKNOWN(2);
	private int type;

	private MessageBodyType(int type) {
		this.type = type;
	}

	public static MessageBodyType fromInt(int code) {
		switch (code) {
		case 0:
			return SINGLE_USER_TYPE;
		case 1:
			return GROUP_TYPE;
		default:
			return UNKNOWN;

		}
	}

	public int intValue() {
		return type;
	}
}
