package com.v2tech.view.conversation;

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
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.DensityUtils;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.MessageUtil;
import com.v2tech.util.SPUtil;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFaceItem;
import com.v2tech.vo.VMessageFileItem;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VMessageLinkTextItem;
import com.v2tech.vo.VMessageTextItem;

public class MessageBodyView extends LinearLayout {

	private static final String TAG = "MessageBodyView";
	private static final int PICTURE = 1;
	private static final int WORD = 2;
	private static final int EXCEL = 3;
	private static final int PDF = 4;
	private static final int PPT = 5;
	private static final int ZIP = 6;
	private static final int VIS = 7;
	private static final int VIDEO = 8;
	private static final int SOUND = 9;
	private static final int OTHER = 10;
	private static final int MESSAGE_TYPE_TEXT = 11;
	private static final int MESSAGE_TYPE_NON_TEXT = 12;

	private int messageType = MESSAGE_TYPE_TEXT;
	private VMessage mMsg;
	private ImageView mHeadIcon;
	private LinearLayout mContentContainer;
	private ImageView mArrowIV;
	private TextView timeTV;
	private TextView seconds;
	private View failedIcon;
	private View unReadIcon;

	private View rootView;

	private boolean isShowTime;

	private LinearLayout mLocalMessageContainter;

	private LinearLayout mRemoteMessageContainter;

	private Handler localHandler;
	private Runnable popupWindowListener = null;
	private PopupWindow pw;

	private long lastUpdateTime;

	private ClickListener callback;
	private ImageView micIv;
	private int width = 0;

	public interface ClickListener {
		public void onMessageClicked(VMessage v);

		public void reSendMessageClicked(VMessage v);

		public void requestDelMessage(VMessage v);

		public void requestPlayAudio(View v, VMessage vm, VMessageAudioItem vai);

		public void requestStopAudio(View v, VMessage vm, VMessageAudioItem vai);

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

	public MessageBodyView(Context context, VMessage m) {
		this(context, m, false);
	}

	public MessageBodyView(Context context, VMessage m, boolean isShowTime) {
		super(context);
		this.mMsg = m;
		rootView = LayoutInflater.from(context).inflate(R.layout.message_body,
				null, false);
		this.isShowTime = isShowTime;
		this.localHandler = new Handler();
		initView();
		initData();
	}

	private void initView() {
		if (rootView == null) {
			V2Log.e(" root view is Null can not initialize");
			return;
		}
		timeTV = (TextView) rootView.findViewById(R.id.message_body_time_text);

		mLocalMessageContainter = (LinearLayout) rootView
				.findViewById(R.id.message_body_left_user_ly);

		mRemoteMessageContainter = (LinearLayout) rootView
				.findViewById(R.id.message_body_remote_ly);

		LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		this.addView(rootView, ll);
	}

	private void initData() {
		if (isShowTime && mMsg.getDate() != null) {
			timeTV.setText(mMsg.getDateTimeStr());
		} else {
			timeTV.setVisibility(View.GONE);
		}

		if (!mMsg.isLocal()) {
			mHeadIcon = (ImageView) rootView
					.findViewById(R.id.conversation_message_body_icon_left);
			// if (mMsg.getToUser() != null && mMsg.getToUser().isDirty()) {
			// User toUser =
			// GlobalHolder.getInstance().getUser(mMsg.getToUser().getmUserId());
			// mMsg.setToUser(toUser);
			// }
			// if (mMsg.getToUser() != null
			// && mMsg.getToUser().getAvatarBitmap() != null) {
			// mHeadIcon.setImageBitmap(mMsg.getToUser().getAvatarBitmap());
			// }
			mContentContainer = (LinearLayout) rootView
					.findViewById(R.id.messag_body_content_ly_left);
			mArrowIV = (ImageView) rootView
					.findViewById(R.id.message_body_arrow_left);
			mArrowIV.bringToFront();
			mLocalMessageContainter.setVisibility(View.VISIBLE);
			mRemoteMessageContainter.setVisibility(View.GONE);
			seconds = (TextView) rootView
					.findViewById(R.id.message_body_video_item_second_left);

			failedIcon = rootView
					.findViewById(R.id.message_body_failed_item_left);
			unReadIcon = rootView
					.findViewById(R.id.message_body_unread_icon_left);
			failedIcon.setVisibility(View.INVISIBLE);
			unReadIcon.setVisibility(View.GONE);
			seconds.setVisibility(View.GONE);
		} else {
			mHeadIcon = (ImageView) rootView
					.findViewById(R.id.conversation_message_body_icon_right);
			mContentContainer = (LinearLayout) rootView
					.findViewById(R.id.messag_body_content_ly_right);
			mArrowIV = (ImageView) rootView
					.findViewById(R.id.message_body_arrow_right);
			mArrowIV.bringToFront();
			mLocalMessageContainter.setVisibility(View.GONE);
			mRemoteMessageContainter.setVisibility(View.VISIBLE);
			seconds = (TextView) rootView
					.findViewById(R.id.message_body_video_item_second_right);
			failedIcon = rootView
					.findViewById(R.id.message_body_failed_item_right);

			unReadIcon = rootView
					.findViewById(R.id.message_body_unread_icon_right);
			unReadIcon.setVisibility(View.GONE);
			failedIcon.setVisibility(View.INVISIBLE);
			seconds.setVisibility(View.GONE);
		}

		if (mMsg.getFromUser() != null && mMsg.getFromUser().isDirty()) {
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

		if (mMsg.getFromUser() != null
				&& mMsg.getFromUser().getAvatarBitmap() != null) {
			mHeadIcon.setImageBitmap(mMsg.getFromUser().getAvatarBitmap());
		}

		populateMessage();

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

		if (mMsg.getState() == VMessage.STATE_SENT_FAILED) {
			failedIcon.setVisibility(View.VISIBLE);
		}

		TextView et = new TextView(this.getContext());
		mContentContainer.setOnLongClickListener(messageLongClickListener);
		et.setOnClickListener(imageMessageClickListener);
		et.setBackgroundColor(Color.TRANSPARENT);
		et.setSelected(false);
		LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		mContentContainer.addView(et, ll);
		List<VMessageAbstractItem> items = mMsg.getItems();
		for (int i = 0; items != null && i < items.size(); i++) {
			VMessageAbstractItem item = items.get(i);
			// Add new layout for new line
			if (item.isNewLine() && et.length() != 0) {
				et.append("\n"); 
			}
			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_TEXT) {
				et.append(((VMessageTextItem) item).getText());
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FACE) {
				Drawable dr = this
						.getResources()
						.getDrawable(
								GlobalConfig.GLOBAL_FACE_ARRAY[((VMessageFaceItem) item)
										.getIndex()]);
				dr.setBounds(0, 0, dr.getIntrinsicWidth(),
						dr.getIntrinsicHeight());

				et.append(".");

				SpannableStringBuilder builder = new SpannableStringBuilder(
						et.getText());
				ImageSpan is = new ImageSpan(dr);
				builder.setSpan(is, et.getText().length() - 1, et.getText()
						.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				et.setText(builder);
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_IMAGE) {
				messageType = MESSAGE_TYPE_NON_TEXT;
				Drawable dr = new BitmapDrawable(this.getContext()
						.getResources(),
						((VMessageImageItem) item).getCompressedBitmap());
				dr.setBounds(0, 0, dr.getIntrinsicWidth(),
						dr.getIntrinsicHeight());
				et.append(".");
				SpannableStringBuilder builder = new SpannableStringBuilder(
						et.getText());
				ImageSpan is = new ImageSpan(dr);
				builder.setSpan(is, et.getText().length() - 1, et.getText()
						.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				et.setText(builder);
				// AudioItem only has one item
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_LINK_TEXT) {
				et.append(((VMessageLinkTextItem) item).getText());
			}

			if (item.getState() == VMessageAbstractItem.STATE_SENT_FALIED) {
				failedIcon.setVisibility(View.VISIBLE);
			}

		}

		mContentContainer.setTag(this.mMsg);

	}

	/**
	 * Now only handle one audio message
	 * 
	 * @param audioItems
	 */
	private void populateAudioMessage(List<VMessageAudioItem> audioItems) {
		final VMessageAudioItem item = audioItems.get(0);
		if (item.getState() == VMessageAbstractItem.STATE_UNREAD) {
			unReadIcon.setVisibility(View.VISIBLE);
		} else if (item.getState() == VMessageAbstractItem.STATE_SENT_FALIED) {
			failedIcon.setVisibility(View.VISIBLE);
		}

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
			micIv.setImageResource(R.drawable.voice_message_mic_icon_selector);
			audioRoot.addView(micIv, micIvLy);

			tvIvLy.addRule(RelativeLayout.RIGHT_OF, micIv.getId());
			audioRoot.addView(tv, tvIvLy);
		}

		audioRoot.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (item.isPlaying()) {
					callback.requestStopAudio(view, mMsg, item);
				} else {
					callback.requestStopOtherAudio(mMsg);
					callback.requestPlayAudio(view, mMsg, item);
					updateUnreadFlag(false);
				}
			}

		});

		audioRoot.setOnLongClickListener(messageLongClickListener);

		mContentContainer.addView(audioRoot, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

	}

	private void populateFileItem(List<VMessageFileItem> fileItems) {
		final VMessageFileItem item = fileItems.get(0);
		View fileRootView = LayoutInflater.from(getContext()).inflate(
				R.layout.message_body_file_item, null, false);
		ImageView fileIcon = (ImageView) fileRootView
				.findViewById(R.id.message_body_file_item_icon_ly);
		TextView fileName = (TextView) fileRootView
				.findViewById(R.id.message_body_file_item_file_name);
		fileName.setText(item.getFileName());

		adapterFileIcon(item.getFileType(), fileIcon);

		TextView fileSize = (TextView) fileRootView
				.findViewById(R.id.message_body_file_item_file_size);
		fileSize.setText(item.getFileSizeStr());

		updateFileItemView(item, fileRootView);
		fileRootView.setOnClickListener(fileMessageItemClickListener);
		fileRootView.setOnLongClickListener(messageLongClickListener);

		fileRootView.setTag(item);
		mContentContainer.addView(fileRootView, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

	}

	private void adapterFileIcon(int fileType, ImageView fileIcon) {

		switch (fileType) {
		case PICTURE:
			fileIcon.setBackgroundResource(R.drawable.selectfile_type_picture);
			break;
		case WORD:
			fileIcon.setBackgroundResource(R.drawable.selectfile_type_word);
			break;
		case EXCEL:
			fileIcon.setBackgroundResource(R.drawable.selectfile_type_excel);
			break;
		case PDF:
			fileIcon.setBackgroundResource(R.drawable.selectfile_type_pdf);
			break;
		case PPT:
			fileIcon.setBackgroundResource(R.drawable.selectfile_type_ppt);
			break;
		case ZIP:
			fileIcon.setBackgroundResource(R.drawable.selectfile_type_zip);
			break;
		case VIS:
			fileIcon.setBackgroundResource(R.drawable.selectfile_type_viso);
			break;
		case VIDEO:
			fileIcon.setBackgroundResource(R.drawable.selectfile_type_video);
			break;
		case SOUND:
			fileIcon.setBackgroundResource(R.drawable.selectfile_type_sound);
			break;
		case OTHER:
			fileIcon.setBackgroundResource(R.drawable.selectfile_type_ohter);
			break;
		}
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

				int width = DensityUtils.dip2px(getContext(), 180);
				int height = DensityUtils.dip2px(getContext(), 80);
				if (pw == null) {
					LayoutInflater inflater = (LayoutInflater) getContext()
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					View popWindow = inflater.inflate(
							R.layout.message_selected_pop_up_window, null);
					// FIXME should not hard code 140 50
					pw = new PopupWindow(popWindow, width, height, true);
					pw.setBackgroundDrawable(new ColorDrawable(
							Color.TRANSPARENT));
					pw.setFocusable(true);
					pw.setTouchable(true);
					pw.setOutsideTouchable(true);

					TextView tvResend = (TextView) popWindow
							.findViewById(R.id.contact_message_pop_up_item_resend);
					tvResend.setOnClickListener(mResendButtonListener);

					if (messageType == MESSAGE_TYPE_TEXT) {
						TextView tv = (TextView) popWindow
								.findViewById(R.id.contact_message_pop_up_item_copy);
						tv.setVisibility(View.VISIBLE);
						tv.setOnClickListener(mCopyButtonListener);
					}

					TextView deleteText = (TextView) popWindow
							.findViewById(R.id.contact_message_pop_up_item_delete);
					deleteText.setOnClickListener(mDeleteButtonListener);

				}

				if (failedIcon.getVisibility() == View.INVISIBLE) {
					pw.getContentView()
							.findViewById(
									R.id.contact_message_pop_up_item_resend)
							.setVisibility(View.GONE);
					if (messageType == MESSAGE_TYPE_TEXT) {
						pw.getContentView()
								.findViewById(
										R.id.contact_message_pop_up_item_copy)
								.setVisibility(View.VISIBLE);
					}
				} else {
					// List<VMessageAbstractItem> items = mMsg.getItems();
					// for (VMessageAbstractItem vMessageAbstractItem : items) {
					// if(VMessageAbstractItem.ITEM_TYPE_TEXT ==
					// vMessageAbstractItem.getType() ||
					// VMessageAbstractItem.ITEM_TYPE_FACE ==
					// vMessageAbstractItem.getType()){
					// pw.getContentView()
					// .findViewById(
					// R.id.contact_message_pop_up_item_resend)
					// .setVisibility(View.VISIBLE);
					// }
					// else{
					//
					// pw.getContentView()
					// .findViewById(
					// R.id.contact_message_pop_up_item_redownload)
					// .setVisibility(View.VISIBLE);
					// }
					// }
					if (mMsg.isLocal()) {
						pw.getContentView()
								.findViewById(
										R.id.contact_message_pop_up_item_resend)
								.setVisibility(View.VISIBLE);
					} else {
						pw.getContentView()
								.findViewById(
										R.id.contact_message_pop_up_item_redownload)
								.setVisibility(View.VISIBLE);
					}
					pw.getContentView()
							.findViewById(R.id.contact_message_pop_up_item_copy)
							.setVisibility(View.GONE);
				}

				int viewWidth = anchor.getMeasuredWidth();
				// int viewHeight = anchor.getMeasuredHeight();
				int offsetX = (viewWidth - width) / 2;
				// int offsetY = (viewHeight + height);

				int[] location = new int[2];
				anchor.getLocationInWindow(location);
				// if (location[1] <= 0) {
				Rect r = new Rect();
				anchor.getDrawingRect(r);
				Rect r1 = new Rect();
				anchor.getGlobalVisibleRect(r1);
				int offsetXLocation = r1.left + offsetX;
				int offsetYLocation = r1.top - (height / 2);
				pw.showAtLocation((View) anchor.getParent(),
						Gravity.NO_GRAVITY, offsetXLocation, offsetYLocation);
				// } else {
				// pw.showAsDropDown((View) anchor.getParent(), offsetX,
				// offsetY);
				// }
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

	public void updateView(VMessage vm, boolean showTime) {
		isShowTime = showTime;
		updateView(vm);
	}

	public void updateUnreadFlag(boolean flag) {
		if (!flag) {
			this.unReadIcon.setVisibility(View.GONE);
		} else {
			this.unReadIcon.setVisibility(View.VISIBLE);
		}
	}

	public void updateFailedFlag(boolean flag) {
		if (!flag) {
			this.failedIcon.setVisibility(View.INVISIBLE);
		} else {
			this.failedIcon.setVisibility(View.VISIBLE);
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
		if (this.mMsg == vm) {
			return;
		}
		if (mContentContainer != null) {
			mContentContainer.removeAllViews();
		}
		this.mMsg = vm;
		initData();
	}

	public void updateView(VMessageFileItem vfi) {
		if (vfi == null || vfi.getVm() != mMsg) {
			return;
		}

		View fileRootView = mContentContainer.getChildAt(0);
		updateFileItemView(vfi, fileRootView);

	}

	/**
	 * Use to user update avatar or re-connect network
	 * 
	 * @param bmp
	 */
	public void updateHeadIcon(Bitmap bmp) {
		mHeadIcon.setImageBitmap(bmp);
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
			progress.setText(vfi.getDownloadSizeStr() + "/"
					+ vfi.getFileSizeStr());

			TextView speed = (TextView) rootView
					.findViewById(R.id.message_body_file_item_progress_speed);
			if (lastUpdateTime == 0 || vfi.getSpeed() == 0) {
				vfi.setSpeed(100.0F);
				lastUpdateTime = System.currentTimeMillis();
			} else {
				long sec = (System.currentTimeMillis() - lastUpdateTime) / 1000;
				vfi.setSpeed(sec == 0 ? 0 : (vfi.getDownloadedSize() / sec));
			}
			speed.setText(vfi.getSpeedStr());
			float percent = (float) ((double) vfi.getDownloadedSize() / (double) vfi
					.getFileSize());

			final ViewGroup progressC = (ViewGroup) rootView
					.findViewById(R.id.message_body_file_item_progress_state_ly);
			ViewTreeObserver viewTreeObserver = progressC.getViewTreeObserver();
			viewTreeObserver.addOnPreDrawListener(new OnPreDrawListener() {
				
				@Override
				public boolean onPreDraw() {
					if(width == 0){
						width = progressC.getMeasuredWidth();
						V2Log.d(TAG, "total width：" + width);
					}
					return true;
				}
			});
//			progressC.measure(View.MeasureSpec.makeMeasureSpec(0,
//					View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
//					.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
//			width = progressC.getMeasuredWidth();
			View iv = rootView
					.findViewById(R.id.message_body_file_item_progress_state);
			android.view.ViewGroup.LayoutParams params = iv.getLayoutParams();
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
		if (vfi.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING) {
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_downloading)
					.toString();
			actionButton.setImageResource(R.drawable.message_file_pause_button);
			showProgressLayout = true;
		} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_SENDING) {
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
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_sent_failed)
					.toString();
			// Show failed icon
			failedIcon.setVisibility(View.VISIBLE);
		} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED) {
			strState = getContext()
					.getResources()
					.getText(R.string.contact_message_file_item_download_failed)
					.toString();
			// Show failed icon
			failedIcon.setVisibility(View.VISIBLE);
		} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_MISS_DOWNLOAD) {
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_miss_download)
					.toString();
		} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_UNDOWNLOAD) {
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_miss_download)
					.toString();
		} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_SENT) {
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_sent)
					.toString();
		} else if (vfi.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADED) {
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_downloaded)
					.toString();
		}

		view.setText(strState);
		return showProgressLayout;
	}

	private OnLongClickListener messageLongClickListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View anchor) {
			showPopupWindow(anchor);
			return false;
		}

	};

	public VMessage getMsg() {
		return this.mMsg;
	}

	public void updateAvatar(Bitmap bmp) {
		mHeadIcon.setImageBitmap(bmp);
	}

	private OnClickListener fileMessageItemClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			VMessageFileItem item = (VMessageFileItem) view.getTag();

			if (callback != null) {
				if (item.getState() == VMessageFileItem.STATE_FILE_UNDOWNLOAD) {
					callback.requestDownloadFile(view, item.getVm(), item);
				} else if (item.getState() == VMessageFileItem.STATE_FILE_SENDING) {
					callback.requestPauseTransFile(view, item.getVm(), item);
				} else if (item.getState() == VMessageFileItem.STATE_FILE_PAUSED_SENDING) {
					callback.requestResumeTransFile(view, item.getVm(), item);
				} else if (item.getState() == VMessageFileItem.STATE_FILE_DOWNLOADING) {
					callback.requestPauseDownloadFile(view, item.getVm(), item);
				} else if (item.getState() == VMessageFileItem.STATE_FILE_PAUSED_DOWNLOADING) {
					callback.requestResumeDownloadFile(view, item.getVm(), item);
				}
			}

			updateView(item);
		}

	};

	private OnClickListener imageMessageClickListener = new OnClickListener() {

		@Override
		public void onClick(View anchor) {
			if (callback != null) {
				// VMessage vm =(VMessage) anchor.getTag();
				List<VMessageImageItem> vl = mMsg.getImageItems();
				if (vl != null && vl.size() > 0) {
					callback.onMessageClicked(mMsg);
				}

				List<VMessageAudioItem> al = mMsg.getAudioItems();
				if (al != null && al.size() > 0) {
					VMessageAudioItem audioItem = al.get(0);
					if (audioItem.isPlaying()) {
						callback.requestStopAudio(anchor, mMsg, audioItem);
					} else {
						callback.requestPlayAudio(anchor, mMsg, audioItem);
						audioItem.setPlaying(true);
					}
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

			Toast.makeText(getContext(), R.string.contact_message_copy_message,
					Toast.LENGTH_SHORT).show();
		}

	};

	private OnClickListener mResendButtonListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			if (SPUtil.checkCurrentAviNetwork(getContext())) {
				if (callback != null) {
					View fileRootView = mContentContainer.getChildAt(0);
					failedIcon.setVisibility(View.INVISIBLE);

					if(mMsg.isLocal()){
						callback.reSendMessageClicked(mMsg);
					}
					else{
						if (mMsg.getItems().size() > 0
								&& mMsg.getItems().get(0).getType() == VMessageFileItem.ITEM_TYPE_FILE) {
							VMessageFileItem fileItem = (VMessageFileItem) mMsg
									.getItems().get(0);
							if (fileItem.getState() != VMessageAbstractItem.STATE_FILE_SENDING) {
								callback.requestDownloadFile(fileRootView, mMsg,
										fileItem);
							}
							updateFileItemView(fileItem, fileRootView);
						}
					}
				}
			} else {
				Toast.makeText(getContext(), "网络连接不可用，请稍候再试",
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

}
