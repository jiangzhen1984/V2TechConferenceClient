package com.v2tech.view.contacts;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.logic.Group;
import com.v2tech.logic.User;
import com.v2tech.util.V2Log;

public class ContactGroupView extends LinearLayout {

	private Group mGroup;

	private ImageView mPhotoIV;
	private TextView mGroupNameTV;
	private TextView mGroupStsTV;
	private boolean isShowingChild;
	private LinearLayout mChildContainer;
	private boolean isChildLoaded;

	public ContactGroupView(Context context, Group g) {
		super(context);
		initData(g);
	}

	public void initData(Group g) {
		long l1 = System.currentTimeMillis();
		if (g == null || g.getmGId() <= 0) {
			throw new RuntimeException("Invalid Group data");
		}
		this.mGroup = g;

		isShowingChild = false;
		isChildLoaded = false;
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

				isShowingChild = !isShowingChild;
				showChild(isShowingChild);
			}

		});
		
		this.addView(view, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		V2Log.e( System.currentTimeMillis()  -l1 +"");
	}

	private void showChild(boolean visable) {
		if (!isChildLoaded) {
			List<Group> lg = this.mGroup.getChildGroup();
			LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			for (Group g : lg) {
				mChildContainer
						.addView(new ContactGroupView(getContext(), g), ll);
			}
			// TODO if group doesn't load user data yet, should load child data
			// and show progress
			List<User> lu = this.mGroup.getUsers();
			for (User u : lu) {
				mChildContainer
						.addView(new ContactUserView(getContext(), u), ll);
			}
			isChildLoaded = true;
		}
		if (visable) {
			// TODO add animation
			mChildContainer.setVisibility(View.VISIBLE);
		} else {
			mChildContainer.setVisibility(View.GONE);
		}
	}

}
