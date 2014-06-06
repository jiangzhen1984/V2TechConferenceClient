package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.adapter.VMessageAdater;
import com.v2tech.view.widget.CommonAdapter;
import com.v2tech.view.widget.CommonAdapter.CommonAdapterItemWrapper;
import com.v2tech.vo.VMessage;

public class VideoMsgChattingLayout extends LinearLayout {

	private View rootView;
	private ListView mMsgContainer;
	private ChattingListener listener;
	private View mSendButton;
	private TextView mContentTV;
	private View mPinButton;
	private List<CommonAdapterItemWrapper> messageArray;
	private CommonAdapter adapter;

	public interface ChattingListener {
		public void requestSendMsg(VMessage vm);

		public void requestChattingViewFixedLayout(View v);

		public void requestChattingViewFloatLayout(View v);
	};

	public VideoMsgChattingLayout(Context context) {
		super(context);
		initLayout();
		initData();
	}


	private void initLayout() {
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.video_msg_chatting_layout, null, false);

		mPinButton = view.findViewById(R.id.video_msg_chatting_pin_button);
		mPinButton.setOnClickListener(mRequestFixedListener);

		this.addView(view, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));

		this.mMsgContainer = (ListView) view
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

	
	private void initData() {
		messageArray = new ArrayList<CommonAdapterItemWrapper>();
		adapter = new CommonAdapter(messageArray, mConvertListener);
		mMsgContainer.setAdapter(adapter);
	}
	
	
	public void setListener(ChattingListener listener) {
		this.listener = listener;
	}

	public void requestScrollToNewMessage() {
		if (messageArray.size() <= 0) {
			return;
		}
		mMsgContainer.setSelection(messageArray.size() - 1);
	}

	public void addNewMessage(VMessage vm) {
		messageArray.add(new VMessageAdater(vm));
		adapter.notifyDataSetChanged();
		requestScrollToNewMessage();
	}

	
	public void clean() {
		
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
	
	
	
	private CommonAdapter.ViewConvertListener mConvertListener = new CommonAdapter.ViewConvertListener() {

		@Override
		public View converView(CommonAdapterItemWrapper wr, View convertView,
				ViewGroup vg) {
			
			VMessage vm = (VMessage) wr.getItemObject();
			if (convertView == null) {
				ConferenceMessageBodyView mv = new ConferenceMessageBodyView(getContext(), vm);
				convertView = mv;
			} else {
				((ConferenceMessageBodyView) convertView).updateView(vm);
			}
			return convertView;
		}
	};
}
