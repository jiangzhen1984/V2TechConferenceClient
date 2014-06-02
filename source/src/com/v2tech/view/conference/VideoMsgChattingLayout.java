package com.v2tech.view.conference;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.vo.VMessage;

public class VideoMsgChattingLayout extends LinearLayout {

	private View rootView;

	private ChattingListener listener;
	private ScrollView mScroller;
	private LinearLayout mMsgContainer;
	private View mSendButton;
	private TextView mContentTV;
	private View mPinButton;

	public interface ChattingListener {
		public void requestSendMsg(VMessage vm);

		public void requestChattingViewFixedLayout(View v);

		public void requestChattingViewFloatLayout(View v);
	};

	public VideoMsgChattingLayout(Context context) {
		super(context);
		initLayout();
	}

	public VideoMsgChattingLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initLayout();
	}

	public VideoMsgChattingLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		initLayout();
	}

	private void initLayout() {
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.video_msg_chatting_layout, null, false);

		mPinButton = view.findViewById(R.id.video_msg_chatting_pin_button);
		mPinButton.setOnClickListener(mRequestFixedListener);

		this.addView(view, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));

		this.mScroller = (ScrollView) view
				.findViewById(R.id.video_msg_container_scroller);
		this.mMsgContainer = (LinearLayout) view
				.findViewById(R.id.video_msg_container);
		this.mContentTV = (TextView) view
				.findViewById(R.id.video_msg_chatting_layout_msg_content);
		this.mSendButton = view
				.findViewById(R.id.video_msg_chatting_layout_send_button);
		mSendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (listener != null) {
					if (mContentTV.getText() == null
							|| mContentTV.getText().toString().trim().isEmpty()) {
						return;
					}
					VMessage vm = new VMessage(GlobalHolder.getInstance()
							.getCurrentUser(), null, mContentTV.getText()
							.toString());
					addNewMessage(vm);
					listener.requestSendMsg(vm);
					mContentTV.setText("");
				}
			}

		});

		rootView = this;
	}

	public void setListener(ChattingListener listener) {
		this.listener = listener;
	}

	public void requestScrollToNewMessage() {
		if (mMsgContainer.getChildCount() <= 0) {
			return;
		}
		this.mScroller.post(new Runnable() {

			@Override
			public void run() {
				mScroller.scrollTo(
						0,
						mMsgContainer.getChildAt(
								mMsgContainer.getChildCount() - 1).getBottom());
			}

		});
	}

	public void addNewMessage(VMessage vm) {
		TextView tvSender = new TextView(this.getContext());
		tvSender.setText(vm.getUser().getName() + " " + vm.getDateTimeStr());
		tvSender.setTextColor(getContext().getResources().getColor(
				R.color.conference_msg_content_color));
		tvSender.setPadding(15, 5, 15, 5);
		tvSender.setTextSize(14);

		this.mMsgContainer.addView(tvSender);

		TextView tv = new TextView(this.getContext());
		tv.setText(vm.getText());
		tv.setPadding(15, 5, 15, 5);
		tv.setTextColor(getContext().getResources().getColor(
				R.color.conference_msg_content_color));
		tv.setTextSize(14);
		this.mMsgContainer.addView(tv);
		this.mScroller.post(new Runnable() {

			@Override
			public void run() {
				mScroller.scrollTo(
						0,
						mMsgContainer.getChildAt(
								mMsgContainer.getChildCount() - 1).getBottom());
			}

		});
	}

	/**
	 * Used to manually request FloatLayout, Because when this layout will hide,
	 * call this function to inform interface
	 */
	public void requestFloatLayout() {
		if ("float".equals(mPinButton.getTag())) {
			return;
		}
		if (this.listener != null) {
			this.listener.requestChattingViewFloatLayout(rootView);
		}

		mPinButton.setTag("float");
		((ImageView) mPinButton)
				.setImageResource(R.drawable.pin_button_selector);
	}

	private OnClickListener mRequestFixedListener = new OnClickListener() {

		@Override
		public void onClick(View view) {

			if (view.getTag().equals("float")) {
				if (listener != null) {
					listener.requestChattingViewFixedLayout(rootView);
				}
			} else {
				if (listener != null) {
					listener.requestChattingViewFloatLayout(rootView);
				}
			}

			if (view.getTag().equals("float")) {
				view.setTag("fix");
				((ImageView) view)
						.setImageResource(R.drawable.pin_fixed_button_selector);
			} else {
				view.setTag("float");
				((ImageView) view)
						.setImageResource(R.drawable.pin_button_selector);
			}
		}

	};
}
