package com.v2tech.view.contacts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.logic.Group;

public class ContactGroupView extends LinearLayout {

	private Group mGroup;

	private ImageView mPhotoIV;
	private TextView mGroupNameTV;
	private TextView mGroupStsTV;
	private boolean isShowingChild;
	private LinearLayout mChildContainer;
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

		isShowingChild = false;
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.contacts_group_view, null, false);

		mPhotoIV = (ImageView) view.findViewById(R.id.contact_group_arrow);
		mGroupNameTV = (TextView)view.findViewById(R.id.contact_group_name);
		mGroupNameTV.setText(g.getName());
		mGroupStsTV = (TextView)view.findViewById(R.id.contact_group_online_statist);
		//TODO get group staticist information
		mGroupStsTV.setText("");

		mChildContainer = (LinearLayout) view
				.findViewById(R.id.child_container);
		mChildContainer.setVisibility(View.GONE);

		this.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (isShowingChild) {
					mPhotoIV.setImageResource(R.drawable.arrow_right_gray);
				} else {
					mPhotoIV.setImageResource(R.drawable.arrow_down_gray);
				}
				if (callback != null) {
					callback.onClick(view);
				}
				isShowingChild = !isShowingChild;
			}

		});
		
		this.addView(view, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
	}


	public boolean isShowedChild() {
		return isShowingChild;
	}
	
	public Group getGroup() {
		return this.mGroup;
	}

}
