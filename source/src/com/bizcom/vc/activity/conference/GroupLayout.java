package com.bizcom.vc.activity.conference;

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

import com.V2.jni.util.V2Log;
import com.bizcom.util.MessageUtil;
import com.bizcom.vc.activity.conversation.MessageLoader;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vo.ConferenceConversation;
import com.bizcom.vo.ContactConversation;
import com.bizcom.vo.Conversation;
import com.bizcom.vo.CrowdConversation;
import com.bizcom.vo.DiscussionConversation;
import com.bizcom.vo.Group;
import com.bizcom.vo.User;
import com.bizcom.vo.VMessage;
import com.v2tech.R;

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
			initNickName();
			break;
		case Conversation.TYPE_CONFERNECE:
			if (((ConferenceConversation) mConv).getGroup().getOwnerUser()
					.getmUserId() != GlobalHolder.getInstance()
					.getCurrentUserId())
				mGroupIV.setImageResource(R.drawable.conference_icon);
			else
				mGroupIV.setImageResource(R.drawable.conference_icon_self);
			mGroupNameTV.setText(mConv.getName());
			break;
		case V2GlobalConstants.GROUP_TYPE_DEPARTMENT:
			mGroupIV.setImageResource(R.drawable.chat_group_icon);
			mGroupNameTV.setText(mConv.getName());
			mGroupDateTV.setVisibility(View.INVISIBLE);
			break;
		case V2GlobalConstants.GROUP_TYPE_DISCUSSION:
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
			mGroupDateTV.setVisibility(View.INVISIBLE);
			mGroupNameTV.setText(mConv.getName());
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
		mGroupOwnerTV.setText(getResources().getString(
				R.string.vo_conference_crowd_conversation_creation)
				+ mConv.getMsg());
		mGroupDateTV.setText(mConv.getDate());
		addView(view);
	}

	private void initNickName() {

		User currentUser = null;
		if (mConv.getType() == V2GlobalConstants.GROUP_TYPE_USER) {
			ContactConversation con = (ContactConversation) mConv;
			currentUser = con.getU();
		}

		if (currentUser != null) {
			boolean isFriend = GlobalHolder.getInstance().isFriend(currentUser);
			String nickName = currentUser.getNickName();
			if (isFriend && !TextUtils.isEmpty(nickName))
				mGroupNameTV.setText(nickName);
			else
				mGroupNameTV.setText(mConv.getName());
		} else {
			V2Log.e(TAG,
					"updateName ---> get current user is null or id is -1 , please check conversation user is exist");
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
			ContactConversation contact = (ContactConversation) mConv;
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
			mConv.setDate(vm.getStandFormatDate());
			if (builder != null) {
				mConv.setMsg(builder);
			}

			// UPdate UI
			update();

		}

	};

	public void updateConversationNotificator(boolean flag) {
		if (flag) {
			mNotificatorIV.setVisibility(View.VISIBLE);
		} else {
			mNotificatorIV.setVisibility(View.GONE);
		}
	}

	public void updateUserContent(String content) {
		if (mConv.getType() == V2GlobalConstants.GROUP_TYPE_USER)
			mGroupOwnerTV.setText(content);
	}

	public void updateGroupContent(Group group) {
		if (mConv.getType() == V2GlobalConstants.GROUP_TYPE_DISCUSSION)
			return;

		User currentUser = null;
		if (mConv.getType() == V2GlobalConstants.GROUP_TYPE_CROWD) {
			CrowdConversation crowd = (CrowdConversation) mConv;
			currentUser = crowd.getGroup().getOwnerUser();
		} else if (mConv.getType() == V2GlobalConstants.GROUP_TYPE_CONFERENCE) {
			ConferenceConversation conf = (ConferenceConversation) mConv;
			currentUser = conf.getGroup().getOwnerUser();
		}

		boolean isFriend = GlobalHolder.getInstance().isFriend(currentUser);
		String nickName = currentUser.getNickName();

		if (isFriend && !TextUtils.isEmpty(nickName)) {
			V2Log.d(TAG, "updateName ---> Update NickName successfully!");
			mGroupOwnerTV.setText(R.string.conference_groupLayout_creation
					+ nickName);
		} else {
			mGroupOwnerTV.setText(R.string.conference_groupLayout_creation
					+ currentUser.getName());
		}
	}

	public void updateGroupName(String name) {
		mConNameTV.setText(name);
		mGroupNameTV.setText(name);
	}

	public void update() {

		if (mConv.getType() == V2GlobalConstants.GROUP_TYPE_USER)
			initNickName();
		else {
			mConNameTV.setText(mConv.getName());
			mGroupNameTV.setText(mConv.getName());
		}
		mGroupOwnerTV.setText(getResources().getString(
				R.string.vo_conference_crowd_conversation_creation)
				+ mConv.getMsg());
		mGroupDateTV.setText(mConv.getDate());
	}

	public void updateIcon(Bitmap bitmap) {
		if (bitmap != null && !bitmap.isRecycled()) {
			mGroupIV.setImageBitmap(bitmap);
		} else {
			mGroupIV.setImageResource(R.drawable.avatar);
		}
	}

	public void updateCrowdLayout() {
		mGroupDateTV.setVisibility(View.VISIBLE);
	}

	public void updateDiscussionLayout(boolean isShowContactLayout) {
		if (isShowContactLayout) {
			mGroupNameTV.setVisibility(View.VISIBLE);
			mGroupOwnerTV.setVisibility(View.VISIBLE);
			mGroupDateTV.setVisibility(View.VISIBLE);
			mConNameTV.setVisibility(View.INVISIBLE);
		} else {
			mGroupNameTV.setVisibility(View.INVISIBLE);
			mGroupOwnerTV.setVisibility(View.INVISIBLE);
			mGroupDateTV.setVisibility(View.INVISIBLE);
			mConNameTV.setVisibility(View.VISIBLE);
		}
	}

	public Conversation getmConv() {
		return mConv;
	}
}
