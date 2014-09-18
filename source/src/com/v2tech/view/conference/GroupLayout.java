package com.v2tech.view.conference;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.MessageUtil;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.vo.ConferenceConversation;
import com.v2tech.vo.ContactConversation;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.ConversationFirendAuthentication;
import com.v2tech.vo.CrowdConversation;
import com.v2tech.vo.VMessage;

public class GroupLayout extends LinearLayout {

	private static final String TAG = "GroupLayout";

	private Conversation mConv;

	private ImageView mGroupIV;
	private TextView mGroupNameTV;
	private TextView mGroupOwnerTV;
	private TextView mGroupDateTV;
	private ImageView mNotificatorIV;

	private Handler hand = new Handler();

	public GroupLayout(Context context, AttributeSet attrs, Conversation group) {
		super(context, attrs);
		this.mConv = group;
		init();
	}

	public GroupLayout(Context context, Conversation group) {
		super(context);
		this.mConv = group;
		init();
	}

	private void init() {
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.conversation_view, null, false);

		mGroupIV = (ImageView) view
				.findViewById(R.id.group_list_conference_image_view);
		mGroupNameTV = (TextView) view
				.findViewById(R.id.group_list_conference_title_tv);
		mGroupOwnerTV = (TextView) view
				.findViewById(R.id.group_list_conference_owner_tv);
		mGroupDateTV = (TextView) view
				.findViewById(R.id.gourp_list_conference_create_time_tv);
		mNotificatorIV = (ImageView) view
				.findViewById(R.id.group_list_conference_notificator);
		mNotificatorIV.bringToFront();

		mGroupNameTV.setText(this.mConv.getName());
		switch (mConv.getType()) {
		case Conversation.TYPE_CONTACT:
			hand.post(queryMessageRunnable);
			Bitmap bm = ((ContactConversation) this.mConv).getAvatar();
			if (bm != null)
				mGroupIV.setImageBitmap(bm);
			else
				mGroupIV.setImageResource(R.drawable.avatar);
			break;
		case Conversation.TYPE_CONFERNECE:
			if (((ConferenceConversation) mConv).getGroup().getOwnerUser()
					.getmUserId() != GlobalHolder.getInstance()
					.getCurrentUserId())
				mGroupIV.setImageResource(R.drawable.conference_icon);
			break;
		case Conversation.TYPE_GROUP:
			mGroupIV.setImageResource(R.drawable.chat_group_icon);
			break;
		case Conversation.TYPE_VOICE_MESSAGE:
			mGroupIV.setImageResource(R.drawable.vs_message_voice);
			mGroupNameTV.setText("语音消息");
			break;
		case Conversation.TYPE_VERIFICATION_MESSAGE:
			mGroupIV.setImageResource(R.drawable.vs_message_verification);
			mGroupNameTV.setText("验证消息");
			break;
		default:
			throw new RuntimeException("the invalid conversation type :"
					+ mConv.getType());
		}

		mGroupOwnerTV.setText(mConv.getMsg());
		if (mConv.getType() == Conversation.TYPE_CONTACT) {
			V2Log.e(TAG, mConv.getDateLong() + "------");
			mGroupDateTV.setText(mConv.getDateLong());
		} else
			mGroupDateTV.setText(mConv.getDate());
		addView(view);
	}

	public long getGroupId() {
		return mConv.getExtId();
	}

	public void update(CharSequence content, String date, boolean flag) {
		if (flag) {
			mNotificatorIV.setVisibility(View.VISIBLE);
		} else {
			mNotificatorIV.setVisibility(View.GONE);
		}
		if (content != null)
			mGroupOwnerTV.setText(content);

		switch (mConv.getType()) {
			case Conversation.TYPE_CONTACT:
				mGroupDateTV.setText(mConv.getDateLong());
				break;
			default:
				mGroupDateTV.setText(date);
				break;
		}
	}

	private Runnable queryMessageRunnable = new Runnable() {

		@Override
		public void run() {

			if (getContext() == null) {
				return;
			}
			VMessage vm = MessageLoader.getNewestMessage(getContext(),
					GlobalHolder.getInstance().getCurrentUserId(),
					mConv.getExtId());
			if (vm == null) {
				return;
			}
			CharSequence builder = MessageUtil.getMixedConversationContent(
					getContext(), vm);
			mConv.setDate(vm.getFullDateStr());
			if (builder != null) {
				mConv.setMsg(builder);
			}

			// UPdate UI
			update();

		}

	};

	public void updateNotificator(boolean flag) {
		if (flag) {
			mNotificatorIV.setVisibility(View.VISIBLE);
		} else {
			mNotificatorIV.setVisibility(View.GONE);
		}
	}

	public void updateGroupOwner(String name) {
		mGroupNameTV.setText(name);
	}

	public void updateContent(String content) {
		mGroupOwnerTV.setText("创建人:" + content);
	}

	public void update() {
		switch (mConv.getType()) {
		case Conversation.TYPE_VERIFICATION_MESSAGE:
			ConversationFirendAuthentication firend = (ConversationFirendAuthentication) mConv;
			mGroupOwnerTV.setText(firend.getMsg());
			break;
		case Conversation.TYPE_CONTACT:
			ContactConversation contact = (ContactConversation) mConv;
			mGroupNameTV.setText(contact.getName());
			mGroupOwnerTV.setText(contact.getMsg());
			mGroupDateTV.setText(contact.getDateLong());
			break;
		case Conversation.TYPE_GROUP:
			CrowdConversation crowd = (CrowdConversation) mConv;
			mGroupNameTV.setText(crowd.getName());
			mGroupOwnerTV.setText(crowd.getMsg());
			mGroupDateTV.setText(crowd.getDate());
			break;
		case Conversation.TYPE_CONFERNECE:

			break;

		default:
			break;
		}
		// mGroupNameTV.setText(this.mConv.getName());
		// mGroupOwnerTV.setText(mConv.getMsg());
		// // mGroupDateTV.setText(mConv.getDate());
		// if(this.mConv.getType() == Conversation.TYPE_CONTACT){
		// mGroupDateTV.setText(mConv.getDateLong());
		// V2Log.e(TAG, mConv.getDateLong() + "update---");
		// }
		// else
		// mGroupDateTV.setText(mConv.getDate());
	}

	public void updateIcon(Bitmap bitmap) {
		if (bitmap != null && !bitmap.isRecycled()) {
			mGroupIV.setImageBitmap(bitmap);
		} else {
			mGroupIV.setImageResource(R.drawable.avatar);
		}
	}

}
