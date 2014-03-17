package com.v2tech.view.conference;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.logic.ContactConversation;
import com.v2tech.logic.Conversation;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.VMessage;
import com.v2tech.view.PublicIntent;

public class GroupLayout extends LinearLayout {

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
		View view = LayoutInflater.from(super.getContext()).inflate(
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

		if (this.mConv instanceof ContactConversation) {
			hand.post(queryMessageRunnable);
			Bitmap bm = ((ContactConversation) this.mConv).getAvatar();
			if (bm != null) {
				mGroupIV.setImageBitmap(bm);
			} else {
				mGroupIV.setImageResource(R.drawable.avatar);
			}
			this.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {

					Intent i = new Intent(
							PublicIntent.START_CONVERSACTION_ACTIVITY);
					i.putExtra("user1id", GlobalHolder.getInstance()
							.getCurrentUserId());
					i.putExtra("user2id", mConv.getExtId());
					i.putExtra("user2Name", mConv.getName());
					i.addCategory(PublicIntent.DEFAULT_CATEGORY);
					getContext().startActivity(i);
					mNotificatorIV.setVisibility(View.GONE);
				}

			});
		}
		if (this.mConv.getNotiFlag() == Conversation.NOTIFICATION) {
			mNotificatorIV.setVisibility(View.VISIBLE);
		}
		mGroupNameTV.setText(this.mConv.getName());
		mGroupOwnerTV.setText(mConv.getMsg());
		mGroupDateTV.setText(mConv.getDate());
		addView(view);
	}

	public long getGroupId() {
		return mConv.getExtId();
	}

	
	public void update(String content, String date, boolean flag) {
		if (flag) {
			mNotificatorIV.setVisibility(View.VISIBLE);
		} else {
			mNotificatorIV.setVisibility(View.GONE);
		}

		if (this.mConv instanceof ContactConversation) {
			if (content != null) {
				((ContactConversation) mConv).setMsg(content);
			}
			if (date != null) {
				((ContactConversation) mConv).setDate(date);
			}
		}
		if (content != null) {
			mGroupOwnerTV.setText(content);
		}
		if (date != null) {
			mGroupDateTV.setText(date);
		}
	}

	private Runnable queryMessageRunnable = new Runnable() {

		@Override
		public void run() {

			if (getContext() == null) {
				return;
			}
			String selection = "(("
					+ ContentDescriptor.Messages.Cols.FROM_USER_ID + "=? and "
					+ ContentDescriptor.Messages.Cols.TO_USER_ID + "=? ) or "
					+ "(" + ContentDescriptor.Messages.Cols.FROM_USER_ID
					+ "=? and " + ContentDescriptor.Messages.Cols.TO_USER_ID
					+ "=? )) and  (" + ContentDescriptor.Messages.Cols.MSG_TYPE
					+ "=? )";
			String[] args = new String[] {
					GlobalHolder.getInstance().getCurrentUserId() + "",
					mConv.getExtId() + "", mConv.getExtId() + "",
					GlobalHolder.getInstance().getCurrentUserId() + "",
					VMessage.MessageType.TEXT.getIntValue() + "" };

			Cursor cur = getContext().getContentResolver().query(
					ContentDescriptor.Messages.CONTENT_URI,
					ContentDescriptor.Messages.Cols.ALL_CLOS,
					selection,
					args,
					ContentDescriptor.Messages.Cols.SEND_TIME + " desc "
							+ " limit " + 1 + " offset " + 0);
			if (cur.getCount() == 0) {
				return;
			}
			if (cur.moveToNext()) {
				String content = cur.getString(5);
				String dateString = cur.getString(7);
				mGroupOwnerTV.setText(content);
				mGroupDateTV.setText(dateString);
				((ContactConversation) mConv).setMsg(content);
			}
			cur.close();
		}

	};

	public void updateGroupOwner(String name) {
		mGroupNameTV.setText(name);
	}

	public void updateIcon(Bitmap bitmap) {
		if (bitmap != null && !bitmap.isRecycled()) {
			mGroupIV.setImageBitmap(bitmap);
		}
	}

}
