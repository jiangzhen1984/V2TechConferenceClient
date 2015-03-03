package com.bizcom.vc.activity.conference;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
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

import com.V2.jni.util.V2Log;
import com.bizcom.util.MessageUtil;
import com.bizcom.vc.adapter.CommonAdapter;
import com.bizcom.vc.adapter.VMessageDataAndViewWrapper;
import com.bizcom.vc.adapter.CommonAdapter.CommonAdapterItemDateAndViewWrapper;
import com.bizcom.vc.listener.CommonCallBack;
import com.bizcom.vc.listener.CommonCallBack.CommonNotifyChatInterToReplace;
import com.bizcom.vc.widget.cus.PasteEditText;
import com.bizcom.vo.ConferenceGroup;
import com.bizcom.vo.VMessage;
import com.v2tech.R;

public class LeftMessageChattingLayout extends LinearLayout implements
		CommonNotifyChatInterToReplace {

	private ConferenceGroup conf;
	private ChattingListener listener;
	private List<CommonAdapterItemDateAndViewWrapper> ItemDataAndViewWrapperList;
	private CommonAdapter adapter;
	private Context mContext;

	private View rootView;
	private ListView mMsgListView;
	private View mSendButton;
	private PasteEditText mInputMessageET;
	private View mPinButton;
	private OnKeyListener mInputMessageETOnKeyListener = new InputMessageETOnKeyListener();
	private OnClickListener mPinButtonOnClickListener = new PinButtonOnClickListener();
	private CommonAdapter.CommonAdapterGetViewListener mListViewAdapterGetViewListener = new ListViewAdapterGetViewListener();
	private SendButtonOnClickListener mSendButtonOnClickListener = new SendButtonOnClickListener();
	private MsgListViewOnTouchListener mMsgListViewOnTouchListener = new MsgListViewOnTouchListener();

	public interface ChattingListener {
		public void requestSendMsg(VMessage vm);

		public void requestChattingViewFixedLayout(View v);

		public void requestChattingViewFloatLayout(View v);
	};

	public LeftMessageChattingLayout(Context context, ConferenceGroup conf) {
		super(context);
		initLayout();
		initData();
		this.conf = conf;
		mContext = context;
		CommonCallBack.getInstance().setNotifyChatInterToReplace(this);
	}

	private void initLayout() {
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.video_msg_chatting_layout, null, false);

		mPinButton = view.findViewById(R.id.video_msg_chatting_pin_button);
		mPinButton.setOnClickListener(mPinButtonOnClickListener);

		this.addView(view, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));

		this.mMsgListView = (ListView) view
				.findViewById(R.id.video_msg_container);
		this.mMsgListView.setOnTouchListener(mMsgListViewOnTouchListener);
		this.mInputMessageET = (PasteEditText) view
				.findViewById(R.id.video_msg_chatting_layout_msg_content);
		this.mInputMessageET.setOnKeyListener(mInputMessageETOnKeyListener);
		this.mSendButton = view
				.findViewById(R.id.video_msg_chatting_layout_send_button);
		this.mSendButton.setOnClickListener(mSendButtonOnClickListener);

		rootView = this;
	}

	private void initData() {
		ItemDataAndViewWrapperList = new ArrayList<CommonAdapterItemDateAndViewWrapper>();
		adapter = new CommonAdapter(ItemDataAndViewWrapperList,
				mListViewAdapterGetViewListener);
		mMsgListView.setAdapter(adapter);
	}

	public void setListener(ChattingListener listener) {
		this.listener = listener;
	}

	public void requestScrollToNewMessage() {
		if (ItemDataAndViewWrapperList.size() <= 0) {
			return;
		}
		mMsgListView.setSelection(ItemDataAndViewWrapperList.size() - 1);
	}

	public void addNewMessage(VMessage vm) {
		ItemDataAndViewWrapperList.add(new VMessageDataAndViewWrapper(vm));
		adapter.notifyDataSetChanged();
		requestScrollToNewMessage();
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
		String str = (String) mPinButton.getTag();
		if (str == null || str.equals("float")) {
			return false;
		} else {
			return true;
		}
	}

	private class MsgListViewOnTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View view, MotionEvent arg1) {
			InputMethodManager imm = (InputMethodManager) mContext
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mInputMessageET.getWindowToken(),
					InputMethodManager.RESULT_UNCHANGED_SHOWN);
			if (mMsgListView == view) {
				if (imm != null) {
					imm.hideSoftInputFromWindow(
							mInputMessageET.getWindowToken(), 0);
				}
			}
			return false;
		}
	}

	private class SendButtonOnClickListener implements OnClickListener {

		@Override
		public void onClick(View view) {
			if (listener != null) {
				if (mInputMessageET.getText() == null
						|| mInputMessageET.getText().toString().trim()
								.isEmpty()) {
					return;
				}
				VMessage vm = MessageUtil.buildChatMessage(mContext,
						mInputMessageET, conf.getGroupType().intValue(),
						conf.getmGId(), null);
				addNewMessage(vm);
				listener.requestSendMsg(vm);
			}
		}
	}

	private class InputMessageETOnKeyListener implements OnKeyListener {

		@Override
		public boolean onKey(View view, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				InputMethodManager imm = (InputMethodManager) getContext()
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
			}
			return false;
		}

	};

	private class PinButtonOnClickListener implements OnClickListener {

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

	private class ListViewAdapterGetViewListener implements
			CommonAdapter.CommonAdapterGetViewListener {

		@Override
		public View getView(CommonAdapterItemDateAndViewWrapper wr,
				View convertView, ViewGroup vg) {

			VMessage vm = (VMessage) wr.getItemObject();
			if (convertView == null) {
				ConferenceMessageBodyView mv = new ConferenceMessageBodyView(
						getContext(), vm);
				mv.setConf(conf);
				convertView = mv;
			} else {
				((ConferenceMessageBodyView) convertView).updateView(vm);
			}
			return convertView;
		}
	};

	@Override
	public void notifyChatInterToReplace(VMessage vm) {
		if(ItemDataAndViewWrapperList == null)
			return ;
		
		for (int i = 0; i < ItemDataAndViewWrapperList.size(); i++) {
			VMessageDataAndViewWrapper wrapper = (VMessageDataAndViewWrapper) ItemDataAndViewWrapperList
					.get(i);
			VMessage replaced = (VMessage) wrapper.getItemObject();
			if (replaced.getUUID().equals(vm.getUUID())) {
				V2Log.e("binaryReplace", "LeftMessageChattingLayout -- "
						+ "Recevice Binary data from server , and replaced wait! id is : " + vm.getmXmlDatas());
				replaced.setImageItems(vm.getImageItems());
				((Activity) mContext).runOnUiThread(new Runnable() {

					@Override
					public void run() {
						adapter.notifyDataSetChanged();
					}
				});
				break;
			}
		}
	}
}
