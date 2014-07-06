package com.v2tech.view.conversation;

import java.util.List;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.MessageUtil;
import com.v2tech.util.V2Log;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFaceItem;
import com.v2tech.vo.VMessageFileItem;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VMessageTextItem;

public class MessageBodyView extends LinearLayout {

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

	private ClickListener callback;

	public interface ClickListener {
		public void onMessageClicked(VMessage v);

		public void requestPlayAudio(View v, VMessage vm, VMessageAudioItem vai);

		public void requestStopAudio(View v, VMessage vm, VMessageAudioItem vai);

		public void requestDownloadFile(View v, VMessage vm,
				VMessageFileItem vfi);

		public void requestPauseDownloadingFile(View v, VMessage vm,
				VMessageFileItem vfi);

		public void requestResumeDownloadingFile(View v, VMessage vm,
				VMessageAudioItem vfi);

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
			failedIcon.setVisibility(View.GONE);
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
			failedIcon.setVisibility(View.GONE);
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
			populateAudioMessage(audioItems);
			return;
		}

		List<VMessageFileItem> fileItems = mMsg.getFileItems();
		if (fileItems.size() > 0) {
			populateFileItem(fileItems);
			return;
		}

		TextView et = new TextView(this.getContext());
		et.setOnLongClickListener(messageLongClickListener);
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
		ImageView micIv = new ImageView(getContext());
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
			micIv.setImageResource(R.drawable.voice_message_mic_icon_selector);
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
					callback.requestPlayAudio(view, mMsg, item);
					item.setPlaying(true);
					updateUnreadFlag(false);
				}
			}

		});
		mContentContainer.addView(audioRoot, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

	}

	private void populateFileItem(List<VMessageFileItem> fileItems) {
		final VMessageFileItem item = fileItems.get(0);
		View fileRootView = LayoutInflater.from(getContext()).inflate(
				R.layout.message_body_file_item, null, false);
		TextView fileName = (TextView) fileRootView
				.findViewById(R.id.message_body_file_item_file_name);
		fileName.setText(item.getFileName());

		TextView fileSize = (TextView) fileRootView
				.findViewById(R.id.message_body_file_item_file_size);
		fileSize.setText(item.getFileSizeStr());

		TextView state = (TextView) fileRootView
				.findViewById(R.id.message_body_file_item_state);
		String strState = "";
		boolean showProgressLayout = false;
		if (item.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING) {
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_downloading)
					.toString();
			showProgressLayout = true;
		} else if (item.getState() == VMessageAbstractItem.STATE_FILE_SENDING) {
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_sending)
					.toString();
			showProgressLayout = true;
		} else if (item.getState() == VMessageAbstractItem.STATE_FILE_PAUSED) {
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_pause)
					.toString();
			showProgressLayout = true;
		} else if (item.getState() == VMessageAbstractItem.STATE_FILE_SENT_FALIED) {
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_sent_failed)
					.toString();
		} else if (item.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED) {
			strState = getContext()
					.getResources()
					.getText(R.string.contact_message_file_item_download_failed)
					.toString();
		} else if (item.getState() == VMessageAbstractItem.STATE_FILE_MISS_DOWNLOAD) {
			strState = getContext().getResources()
					.getText(R.string.contact_message_file_item_miss_download)
					.toString();
		}

		if (showProgressLayout) {
			fileRootView.findViewById(
					R.id.message_body_file_item_progress_layout).setVisibility(
					View.VISIBLE);

			TextView progress = (TextView) fileRootView
					.findViewById(R.id.message_body_file_item_progress_size);
			progress.setText(item.getDownloadedSize() + "/"
					+ item.getFileSizeStr());

			TextView speed = (TextView) fileRootView
					.findViewById(R.id.message_body_file_item_progress_speed);
			speed.setText(item.getSpeed() + "K");

		} else {
			fileRootView.findViewById(
					R.id.message_body_file_item_progress_layout).setVisibility(
					View.INVISIBLE);
		}

		state.setText(strState);

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
				if (pw == null) {
					LayoutInflater inflater = (LayoutInflater) getContext()
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					View view = inflater.inflate(
							R.layout.message_selected_pop_up_window, null);

					pw = new PopupWindow(view,
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT, true);
					pw.setBackgroundDrawable(new ColorDrawable(
							Color.TRANSPARENT));
					pw.setFocusable(true);
					pw.setTouchable(true);
					pw.setOutsideTouchable(true);

					TextView tv = (TextView) view
							.findViewById(R.id.contact_message_pop_up_item_copy);
					tv.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View view) {

							ClipboardManager clipboard = (ClipboardManager) getContext()
									.getSystemService(Context.CLIPBOARD_SERVICE);
							ClipData clip = ClipData.newPlainText(
									"label",
									MessageUtil
											.getMixedConversationCopyedContent(mMsg));

							clipboard.setPrimaryClip(clip);
							pw.dismiss();

							Toast.makeText(getContext(),
									R.string.contact_message_copy_message,
									Toast.LENGTH_SHORT).show();
						}

					});

				}
				int offsetX = (anchor.getMeasuredWidth() - 50) / 2;
				int offsetY = -(anchor.getMeasuredHeight() + 60);

				int[] location = new int[2];
				anchor.getLocationInWindow(location);
				if (location[1] <= 0) {
					Rect r = new Rect();
					anchor.getDrawingRect(r);
					Rect r1 = new Rect();
					anchor.getGlobalVisibleRect(r1);
					pw.showAtLocation((View) anchor.getParent(),
							Gravity.NO_GRAVITY, r1.left + (r.right - r.left)
									/ 2, r1.top);
				} else {
					pw.showAsDropDown(anchor, offsetX, offsetY);
				}
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

	public void startVoiceAnimation() {

	}

	public void stopVoiceAnimation() {

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

	private OnLongClickListener messageLongClickListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View anchor) {
			showPopupWindow(anchor);
			return false;
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

	class LoadTask extends AsyncTask<ImageView, Void, ImageView[]> {

		@Override
		protected ImageView[] doInBackground(ImageView... vms) {
			// Only has one element
			for (ImageView vm : vms) {
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
