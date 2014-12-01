package com.v2tech.view.conference;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.MessageUtil;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.vo.ConferenceConversation;
import com.v2tech.vo.ContactConversation;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.DiscussionConversation;
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
	
	/**
	 * The second layout , the discussion layout use it...
	 */
	private TextView mConNameTV;

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
		/**
		 * The second layout , the discussion layout use it...
		 */
		mConNameTV = (TextView) view
				.findViewById(R.id.ws_fragment_conversation_name);
		mNotificatorIV.bringToFront();

		switch (mConv.getType()) {
		case Conversation.TYPE_CONTACT:
			hand.post(queryMessageRunnable);
			Bitmap bm = ((ContactConversation) this.mConv).getAvatar();
			if (bm != null)
				mGroupIV.setImageBitmap(bm);
			else
				mGroupIV.setImageResource(R.drawable.avatar);
			updateContactName();
			break;
		case Conversation.TYPE_CONFERNECE:
			if (((ConferenceConversation) mConv).getGroup().getOwnerUser()
					.getmUserId() != GlobalHolder.getInstance()
					.getCurrentUserId())
				mGroupIV.setImageResource(R.drawable.conference_icon);
			break;
		case V2GlobalEnum.GROUP_TYPE_DEPARTMENT:
			mGroupIV.setImageResource(R.drawable.chat_group_icon);
			mGroupNameTV.setText(mConv.getName());
			mGroupDateTV.setVisibility(View.INVISIBLE);
			break;
		case V2GlobalEnum.GROUP_TYPE_DISCUSSION:
			mGroupIV.setImageResource(R.drawable.chat_group_discussion_icon);
			DiscussionConversation dis = (DiscussionConversation) mConv;
			mConNameTV.setText(dis.getName());
			
			mGroupNameTV.setVisibility(View.INVISIBLE);
			mGroupOwnerTV.setVisibility(View.INVISIBLE);
			mGroupDateTV.setVisibility(View.INVISIBLE);
			mConNameTV.setVisibility(View.VISIBLE);
			break;
		case Conversation.TYPE_GROUP:
			mGroupIV.setImageResource(R.drawable.chat_group_icon);
			mGroupNameTV.setText(mConv.getName());
			mGroupDateTV.setVisibility(View.INVISIBLE);
			break;
		case Conversation.TYPE_VOICE_MESSAGE:
			mGroupIV.setImageResource(R.drawable.vs_message_voice);
			mGroupNameTV.setText(mConv.getName());
			break;
		case Conversation.TYPE_VERIFICATION_MESSAGE:
			mGroupIV.setImageResource(R.drawable.vs_message_verification);
			mGroupNameTV.setText(mConv.getName());
			break;
		default:
			throw new RuntimeException("the invalid conversation type :"
					+ mConv.getType());
		}
		mGroupOwnerTV.setText(mConv.getMsg());
		mGroupDateTV.setText(mConv.getDate());
		addView(view);
	}
	
	private void updateContactName() {
		
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
		if(mConv.getType() == V2GlobalEnum.GROUP_TYPE_DISCUSSION)
			mConNameTV.setText(name);
		else
			mGroupNameTV.setText(name);
	}

	public void updateContent(String content) {
		if(mConv.getType() == V2GlobalEnum.GROUP_TYPE_USER)
			mGroupOwnerTV.setText(content);
		else{
			if(mConv.getType() != V2GlobalEnum.GROUP_TYPE_DISCUSSION)
			mGroupOwnerTV.setText("创建人:" + content);
		}
	}

	public void update() {
		
		if(mConv.getType() == V2GlobalEnum.GROUP_TYPE_USER)
			updateContactName();
		else{
//				DiscussionConversation dis = (DiscussionConversation) mConv;
				mConNameTV.setText(mConv.getName());
				mGroupNameTV.setText(mConv.getName());
		}
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
		mGroupDateTV.setVisibility(View.VISIBLE);
	}
	
	public void updateDiscussionLayout(){
		mGroupNameTV.setVisibility(View.VISIBLE);
		mGroupOwnerTV.setVisibility(View.VISIBLE);
		mGroupDateTV.setVisibility(View.VISIBLE);
		mConNameTV.setVisibility(View.INVISIBLE);
	}
}
