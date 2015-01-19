package com.v2tech.view.conference;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.v2tech.R;
import com.v2tech.util.MessageUtil;
import com.v2tech.view.adapter.VMessageAdater;
import com.v2tech.view.conference.ConferenceMessageBodyView.ActionListener;
import com.v2tech.view.cus.PasteEditText;
import com.v2tech.view.widget.CommonAdapter;
import com.v2tech.view.widget.CommonAdapter.CommonAdapterItemWrapper;
import com.v2tech.vo.ConferenceGroup;
import com.v2tech.vo.VMessage;

public class VideoMsgChattingLayout extends LinearLayout {

	private ConferenceGroup conf;
	private View rootView;
	private ListView mMsgContainer;
	private ChattingListener listener;
	private View mSendButton;
	private PasteEditText mContentTV;
	private View mPinButton;
	private List<CommonAdapterItemWrapper> messageArray;
	private CommonAdapter adapter;
	private Context mContext;
	private ActionListener actionLisener;
	
	public interface ChattingListener {
		public void requestSendMsg(VMessage vm);
		
		public void requestChattingViewFixedLayout(View v);

		public void requestChattingViewFloatLayout(View v);
	};

	public VideoMsgChattingLayout(Context context, ConferenceGroup conf) {
		super(context);
		initLayout();
		initData();
		setListeners();
		this.conf = conf;
		mContext = context;
	}

	public View getmSendButton() {
		return mSendButton;
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
		this.mContentTV = (PasteEditText) view
				.findViewById(R.id.video_msg_chatting_layout_msg_content);
		this.mSendButton = view
				.findViewById(R.id.video_msg_chatting_layout_send_button);
		mContentTV.setOnKeyListener(keyListener);
		rootView = this;
	}
	
	private void initData() {
		messageArray = new ArrayList<CommonAdapterItemWrapper>();
		adapter = new CommonAdapter(messageArray, mConvertListener);
		mMsgContainer.setAdapter(adapter);
	}
	
	private void setListeners() {
		this.mMsgContainer.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View view, MotionEvent arg1) {
				InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mContentTV.getWindowToken(),
						InputMethodManager.RESULT_UNCHANGED_SHOWN);
				if (mMsgContainer == view) {
					if (imm != null) {  
				        imm.hideSoftInputFromWindow(mContentTV.getWindowToken(), 0);  
				    }  
				}
				return false;
			}
		});

		mSendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (listener != null) {
					if (mContentTV.getText() == null
							|| mContentTV.getText().toString().trim().isEmpty()) {
						return;
					}
					VMessage vm = MessageUtil.buildChatMessage(mContext, mContentTV, conf.getGroupType().intValue(),
							conf.getmGId(), null);
					addNewMessage(vm);
					listener.requestSendMsg(vm);
				}
			}
		});
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
	
	public boolean getWindowSizeState() {
		String str =(String)mPinButton.getTag();
		if (str == null || str.equals("float")) {
			return false;
		} else {
			return true;
		}
	}
	
	
	private OnKeyListener keyListener = new OnKeyListener() {

		@Override
		public boolean onKey(View view, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
			}
			return false;
		}
		
	};

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
				mv.setConf(conf);
				convertView = mv;
			} else {
				((ConferenceMessageBodyView) convertView).updateView(vm);
			}
			return convertView;
		}
	};
}
