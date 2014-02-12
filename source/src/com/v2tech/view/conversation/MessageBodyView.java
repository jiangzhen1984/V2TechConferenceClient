package com.v2tech.view.conversation;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.logic.Message;

public class MessageBodyView extends LinearLayout {

	private Message mMsg;

	private ImageView mHeadIcon;
	private LinearLayout mContentContainer;
	private ImageView mArrowIV;
	private TextView timeTV;

	private View rootView;

	private boolean isShowTime;

	private LinearLayout mLocalMessageContainter;

	private LinearLayout mRemoteMessageContainter;

	private TextView contextTV;

	private Handler localHandler;
	private Runnable popupWindowListener = null;

	public MessageBodyView(Context context, Message m) {
		this(context, m, false);
	}

	public MessageBodyView(Context context, Message m, boolean isShowTime) {
		super(context);
		this.mMsg = m;

		rootView = LayoutInflater.from(context).inflate(R.layout.message_body,
				null, false);
		this.isShowTime = isShowTime;
		this.localHandler = new Handler();
		initData();
	}

	private void initData() {
		timeTV = (TextView) rootView.findViewById(R.id.message_body_time_text);
		if (isShowTime && mMsg.getDate() != null) {
			timeTV.setText(mMsg.getDateTimeStr());
		} else {
			timeTV.setVisibility(View.GONE);
		}

		mLocalMessageContainter = (LinearLayout) rootView
				.findViewById(R.id.message_body_local_user_ly);

		mRemoteMessageContainter = (LinearLayout) rootView
				.findViewById(R.id.message_body_remote_ly);

		if (mMsg.isLocal()) {
			mHeadIcon = (ImageView) rootView
					.findViewById(R.id.conversation_message_body_icon_local);
			mContentContainer = (LinearLayout) rootView
					.findViewById(R.id.messag_body_content_ly_local);
			mArrowIV = (ImageView) rootView
					.findViewById(R.id.message_body_arrow_left);
			mArrowIV.bringToFront();
			mLocalMessageContainter.setVisibility(View.VISIBLE);
			mRemoteMessageContainter.setVisibility(View.GONE);
		} else {
			mHeadIcon = (ImageView) rootView
					.findViewById(R.id.conversation_message_body_icon_remote);
			mContentContainer = (LinearLayout) rootView
					.findViewById(R.id.messag_body_content_ly_remote);
			mArrowIV = (ImageView) rootView
					.findViewById(R.id.message_body_arrow_right);
			mArrowIV.bringToFront();
			mLocalMessageContainter.setVisibility(View.GONE);
			mRemoteMessageContainter.setVisibility(View.VISIBLE);
		}

		if (mMsg.getType() == Message.MessageType.TEXT) {
			contextTV = new TextView(this.getContext());
			contextTV.setText(mMsg.getText());
			mContentContainer.addView(contextTV, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));
		}

		LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		this.addView(rootView, ll);

		mContentContainer.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(final View anchor, MotionEvent mv) {

				if (mv.getAction() == MotionEvent.ACTION_DOWN) {
					updateSelectedBg(true);
					showPopupWindow(anchor);
				} else if (mv.getAction() == MotionEvent.ACTION_UP) {
					updateSelectedBg(false);
					cancelShowPopUpWindow();
				}
				return true;
			}

		});
	}

	private void updateSelectedBg(boolean selected) {
		if (selected) {
			if (mMsg.isLocal()) {
				mContentContainer
						.setBackgroundResource(R.drawable.message_body_bg_selected);
				mArrowIV.setImageResource(R.drawable.message_body_arrow_left_selected);
			}
		} else {
			if (mMsg.isLocal()) {
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
				LayoutInflater inflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View view = inflater.inflate(
						R.layout.message_selected_pop_up_window, null);
				
				final PopupWindow pw = new PopupWindow(view,
						ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.WRAP_CONTENT, true);
				pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
				pw.setFocusable(true);
				pw.setTouchable(true);
				pw.setOutsideTouchable(true);

				
				TextView tv = (TextView) view
						.findViewById(R.id.contact_message_pop_up_item_copy);
				tv.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						ClipboardManager clipboard = (ClipboardManager) getContext()
								.getSystemService(Context.CLIPBOARD_SERVICE);
						ClipData clip = ClipData.newPlainText("label",
								mMsg.getText());
						clipboard.setPrimaryClip(clip);
						pw.dismiss();
					}

				});
				
				
				int offsetX = (anchor.getMeasuredWidth() - 50) / 2;
				int offsetY = -(anchor.getMeasuredHeight() + 60) ;
				pw.showAsDropDown(anchor, offsetX,  offsetY);
				popupWindowListener = null;
				updateSelectedBg(false);
			}

		};

		localHandler.postDelayed(popupWindowListener, 800);

	}

	private void cancelShowPopUpWindow() {
		if (popupWindowListener != null) {
			localHandler.removeCallbacks(popupWindowListener);
		}
	}

	public Message getItem() {
		return this.mMsg;
	}

	public void updateMessage(Message msg) {
		this.mMsg = msg;
		mContentContainer.removeAllViews();
		if (mMsg.isLocal()) {
			mArrowIV.bringToFront();
			mLocalMessageContainter.setVisibility(View.VISIBLE);
			mRemoteMessageContainter.setVisibility(View.GONE);
			mContentContainer = (LinearLayout) rootView
					.findViewById(R.id.messag_body_content_ly_local);
		} else {
			mArrowIV.bringToFront();
			mContentContainer = (LinearLayout) rootView
					.findViewById(R.id.messag_body_content_ly_remote);
			mLocalMessageContainter.setVisibility(View.GONE);
			mRemoteMessageContainter.setVisibility(View.VISIBLE);
		}

		if (mMsg.getType() == Message.MessageType.TEXT) {
			contextTV.setText(mMsg.getText());
			mContentContainer.addView(contextTV, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));
		}
	}

}
