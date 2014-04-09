package com.v2tech.view.conference;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.VMessage;

public class VideoMsgChattingLayout extends LinearLayout {
	
	private ChattingListener listener;
	
	private LinearLayout mMsgContainer;
	private ScrollView mScrollor;
	private View mSendButton;
	private TextView mContentTV;
	
	public interface ChattingListener {
		public void requestSendMsg(VMessage vm);
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

		this.addView(view, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));
		
		this.mMsgContainer = (LinearLayout)view.findViewById(R.id.video_msg_container);
		this.mScrollor = (ScrollView)view.findViewById(R.id.video_msg_container_scroller);
		this.mContentTV = (TextView)view.findViewById(R.id.video_msg_chatting_layout_msg_content);
		this.mSendButton = view.findViewById(R.id.video_msg_chatting_layout_send_button);
		mSendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (listener != null) {
					VMessage vm = new VMessage(GlobalHolder.getInstance().getCurrentUser(), null, mContentTV.getText().toString());
					addNewMessage(vm);
					listener.requestSendMsg(vm);
					mContentTV.setText("");
				}
			}
			
		});
	}
	
	
	
	public void setListener(ChattingListener listener) {
		this.listener = listener;
	}
	
	
	public void addNewMessage(VMessage vm) {
		TextView tvSender = new TextView(this.getContext());
		tvSender.setText(vm.getUser().getName()+" "+ vm.getDateTimeStr());
		tvSender.setTextColor(Color.rgb(0, 0, 255));
		tvSender.setPadding(15, 5, 15, 5);
		this.mMsgContainer.addView(tvSender);
		
		TextView tv = new TextView(this.getContext());
		tv.setText(vm.getText());
		tv.setPadding(15, 5, 15, 5);
		tv.setTextColor(Color.BLACK);
		this.mMsgContainer.addView(tv);
		this.mScrollor.post(new Runnable() {

			@Override
			public void run() {
				mScrollor.scrollTo(0, mMsgContainer.getChildAt(mMsgContainer.getChildCount() -1 ).getBottom());
			}
			
		});
	}
}
