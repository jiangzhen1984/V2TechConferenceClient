package com.v2tech.view.conference;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.MessageUtil;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.vo.ConferenceConversation;
import com.v2tech.vo.ContactConversation;
import com.v2tech.vo.ContactGroup;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.ConversationFirendAuthenticationData;
import com.v2tech.vo.CrowdConversation;
import com.v2tech.vo.Group;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;

public class GroupLayout extends LinearLayout {

	private static final String TAG = "GroupLayout";
	
	private Conversation mConv;

	private ImageView mGroupIV;
	private TextView mGroupNameTV;
	private TextView mGroupOwnerTV;
	private TextView mGroupDateTV;
	private ImageView mNotificatorIV;
	private TextView mChatGroupNameTV;
	private RelativeLayout mChatGroupLayout;

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
		
		mChatGroupNameTV = (TextView) view
				.findViewById(R.id.group_list_chat_title_tv);
		mChatGroupLayout = (RelativeLayout) view
				.findViewById(R.id.group_list_conference_title_layout);
		mNotificatorIV.bringToFront();

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
		case V2GlobalEnum.GROUP_TYPE_DEPARTMENT:
			mGroupIV.setImageResource(R.drawable.chat_group_icon);
			break;
		case Conversation.TYPE_GROUP:
			mGroupIV.setImageResource(R.drawable.chat_group_icon);
			break;
		case Conversation.TYPE_VOICE_MESSAGE:
			mGroupIV.setImageResource(R.drawable.vs_message_voice);
			break;
		case Conversation.TYPE_VERIFICATION_MESSAGE:
			mGroupIV.setImageResource(R.drawable.vs_message_verification);
			break;
		default:
			throw new RuntimeException("the invalid conversation type :"
					+ mConv.getType());
		}

		updateName();
		mChatGroupNameTV.setText(mConv.getName());
		mGroupOwnerTV.setText(mConv.getMsg());
		mGroupDateTV.setText(mConv.getDate());
		addView(view);
	}
	
	private void updateName() {
		
		if(mConv.getType() == Conversation.TYPE_CONTACT){
			ContactConversation con = (ContactConversation) mConv;
			User currentUser = con.getU();
			if(currentUser != null && con.getUserID() != -1){
				boolean isFriend = GlobalHolder.getInstance().isFriend(currentUser);
				String nickName = currentUser.getNickName();
				if(isFriend && !TextUtils.isEmpty(nickName))
					mGroupNameTV.setText(nickName);
				else
					mGroupNameTV.setText(mConv.getName());
			}
			else{
				V2Log.e(TAG, "updateName ---> get current user is null or id is -1 , please check conversation user is exist");
			}
		}
		else
			mGroupNameTV.setText(mConv.getName());
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
				ContactConversation contact = (ContactConversation)mConv;
				contact.setDateLong(date);
				mGroupDateTV.setText(contact.getDate());
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
		mChatGroupNameTV.setText(name);
	}

	public void updateContent(String content) {
		mGroupOwnerTV.setText("创建人:" + content);
	}

	public void update() {
		
		updateName();
		mChatGroupNameTV.setText(mConv.getName());
		mGroupOwnerTV.setText(mConv.getMsg());
		mGroupDateTV.setText(mConv.getDate());
	}

	public void updateIcon(Bitmap bitmap) {
		if (bitmap != null && !bitmap.isRecycled()) {
			mGroupIV.setImageBitmap(bitmap);
		} else {
			mGroupIV.setImageResource(R.drawable.avatar);
		}
	}
	
	public void updateCrowdLayout(){
		
		mChatGroupNameTV.setVisibility(View.VISIBLE);
		mChatGroupLayout.setVisibility(View.INVISIBLE);
		mGroupDateTV.setVisibility(View.INVISIBLE);
	}

}
