package com.v2tech.view.conference;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.logic.ContactConversation;
import com.v2tech.logic.Conversation;

public class GroupLayout extends LinearLayout {


	private Conversation mConv;
	
	private ImageView mGroupIV;
	private TextView mGroupNameTV;
	private TextView mGroupOwnerTV;
	private TextView mGroupDateTV;
	private ImageView mNotificatorIV;
	
	
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
                R.layout.group_list_conference_view, null, false);
		
		mGroupIV = (ImageView) view.findViewById(R.id.group_list_conference_image_view);
		mGroupNameTV = (TextView) view.findViewById(R.id.group_list_conference_title_tv);
		mGroupOwnerTV = (TextView) view.findViewById(R.id.group_list_conference_owner_tv);
		mGroupDateTV = (TextView) view.findViewById(R.id.gourp_list_conference_create_time_tv);
		mNotificatorIV = (ImageView) view.findViewById(R.id.group_list_conference_notificator);
		
		if (this.mConv instanceof ContactConversation) {
			Bitmap bm = ((ContactConversation)this.mConv).getAvatar();
			if (bm != null) {
				mGroupIV.setImageBitmap(bm);
			} else {
				mGroupIV.setImageResource(R.drawable.avatar);
			}
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

	
}
