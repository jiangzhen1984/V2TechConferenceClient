package com.v2tech.view.contacts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.logic.Group;

public class ContactGroupView extends LinearLayout {

	private Group mGroup;

	private TextView mGroupNameTV;
	private TextView mGroupStsTV;
	private OnClickListener callback;

	public ContactGroupView(Context context, Group g, OnClickListener callbackListener) {
		super(context);
		this.callback = callbackListener;
		initData(g);
	}

	public void initData(Group g) {
		if (g == null || g.getmGId() <= 0) {
			throw new RuntimeException("Invalid Group data");
		}
		this.mGroup = g;

		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.contacts_group_view, null, false);

		mGroupNameTV = (TextView)view.findViewById(R.id.contact_group_name);
		mGroupNameTV.setText(g.getName());
		mGroupStsTV = (TextView)view.findViewById(R.id.contact_group_online_statist);
		mGroupStsTV.setText("");
		this.addView(view, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
	}

	
	public Group getGroup() {
		return this.mGroup;
	}

}
