package com.v2tech.view.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.vo.Group;

public class ContactGroupView extends LinearLayout {

	private Group mGroup;

	private ImageView mGroupIndicatorIV;
	private TextView mGroupNameTV;
	private TextView mGroupStsTV;
	private OnClickListener callback;

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

		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.contacts_group_view, null, false);

		View contentContainer =  view
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


	public Group getGroup() {
		return this.mGroup;
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
		mGroupStsTV.setText(mGroup.getOnlineUserCount() + " / "
				+ mGroup.getUserCount());
	}
}
