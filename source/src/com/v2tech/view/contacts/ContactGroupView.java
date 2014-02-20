package com.v2tech.view.contacts;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.logic.Group;
import com.v2tech.logic.User;

public class ContactGroupView extends LinearLayout {

	private Group mGroup;

	private ImageView mGroupIndicatorIV;
	private TextView mGroupNameTV;
	private TextView mGroupStsTV;
	private OnClickListener callback;

	private int mOnLineCounts;
	private int AllCounts;
	private Boolean isCalAllUserCounts = false;

	private Handler innerHandler = new Handler();

	public ContactGroupView(Context context, Group g,
			OnClickListener callbackListener) {
		super(context);
		this.callback = callbackListener;
		initData(g);
	}

	public void initData(Group g) {
		if (g == null || g.getmGId() <= 0) {
			throw new RuntimeException("Invalid Group data");
		}
		this.mGroup = g;
		mOnLineCounts = 0;

		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.contacts_group_view, null, false);

		RelativeLayout contentContainer = (RelativeLayout) view
				.findViewById(R.id.contact_group_view_root);

		mGroupIndicatorIV = (ImageView) view
				.findViewById(R.id.contacts_group_arrow_right);
		mGroupNameTV = (TextView) view.findViewById(R.id.contact_group_name);
		mGroupNameTV.setText(g.getName());
		mGroupStsTV = (TextView) view
				.findViewById(R.id.contact_group_online_statist);
		mGroupStsTV.setText("");

		if (this.callback != null) {
			this.setOnClickListener(this.callback);
		}

		updateUserStatus();

		contentContainer.setPadding(g.getLevel() * 35,
				contentContainer.getPaddingTop(),
				contentContainer.getPaddingRight(),
				contentContainer.getPaddingRight());

		this.addView(view, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
	}

	private void calAllCounts(Group g) {
		for (User u : g.getUsers()) {
			if (u.getmStatus() == User.Status.ONLINE) {
				mOnLineCounts++;
			}
		}
		AllCounts += g.getUsers().size();
		for (Group subG : g.getChildGroup()) {
			calAllCounts(subG);
		}

	}

	public Group getGroup() {
		return this.mGroup;
	}

	private void startCalculUsers() {
		innerHandler.post(new Runnable() {

			@Override
			public void run() {
				synchronized (isCalAllUserCounts) {
					if (!isCalAllUserCounts) {
						calAllCounts(mGroup);
						isCalAllUserCounts = true;
					}
				}
				mGroupStsTV.setText(mOnLineCounts + " / " + AllCounts);
			}

		});
	}

	public void doExpandedOrCollapse() {
		if (mGroupIndicatorIV.getTag() == null
				|| mGroupIndicatorIV.getTag().equals("collapse")) {
			mGroupIndicatorIV.setImageResource(R.drawable.arrow_down_gray);
			mGroupIndicatorIV.setTag("expanded");
		} else {
			mGroupIndicatorIV.setImageResource(R.drawable.arrow_right_gray);
			mGroupIndicatorIV.setTag("collapse");
		}

	}

	public void updateUserStatus() {
		innerHandler.post(new Runnable() {

			@Override
			public void run() {
				mGroupStsTV.setText(mGroup.getOnlineUserCount() + " / "
						+ mGroup.getUserCount());

			}

		});

	}
}
