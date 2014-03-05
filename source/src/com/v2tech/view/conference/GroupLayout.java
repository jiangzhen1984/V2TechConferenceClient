package com.v2tech.view.conference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.logic.Group;

public class GroupLayout extends LinearLayout {


	private Group mGroup;
	
	private ImageView mGroupIV;
	private TextView mGroupNameTV;
	private TextView mGroupOwnerTV;
	private TextView mGroupDateTV;
	
	
	public GroupLayout(Context context, AttributeSet attrs, Group group) {
		super(context, attrs);
		this.mGroup = group;
		init();
	}

	public GroupLayout(Context context, Group group) {
		super(context);
		this.mGroup = group;
		init();
	}
	
	
	private void init() {
		View view = LayoutInflater.from(super.getContext()).inflate(
                R.layout.group_list_conference_view, null, false);
		
		mGroupIV = (ImageView) view.findViewById(R.id.group_list_conference_image_view);
		mGroupNameTV = (TextView) view.findViewById(R.id.group_list_conference_title_tv);
		mGroupOwnerTV = (TextView) view.findViewById(R.id.group_list_conference_owner_tv);
		mGroupDateTV = (TextView) view.findViewById(R.id.gourp_list_conference_create_time_tv);
		
		addView(view);
		mGroupNameTV.setText(this.mGroup.getName());
		mGroupOwnerTV.setText(this.mGroup.getOwnerUser() != null ? this.mGroup.getOwnerUser().getName() : (this.mGroup.getOwner() +""));
		mGroupDateTV.setText(this.mGroup.getCreateDate());
	}
	
	
	public long getGroupId() {
		return this.mGroup.getmGId();
	}

	
}
