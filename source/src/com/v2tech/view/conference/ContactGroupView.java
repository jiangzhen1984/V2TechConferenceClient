package com.v2tech.view.conference;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
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
	private OnClickListener checkBoxCallbackListener;
	private CheckBox mCheckBox;

	public ContactGroupView(Context context, Group g,
			OnClickListener callbackListener) {
		super(context);
		this.callback = callbackListener;
		initData(g);
	}
	
	
	public ContactGroupView(Context context, Group g,
			OnClickListener callbackListener, OnClickListener checkBoxCallbackListener) {
		super(context);
		this.callback = callbackListener;
		this.checkBoxCallbackListener = checkBoxCallbackListener;
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

		if (this.callback != null) {
			this.setOnClickListener(this.callback);
		}
		
		if (this.checkBoxCallbackListener  != null) {
			mCheckBox= (CheckBox)view.findViewById(R.id.conf_create_contact_group_view_ck);
			//mCheckBox.setVisibility(View.VISIBLE);
			mCheckBox.setChecked(false);
			mCheckBox.setTag(g);
		}

		updateUserStatus();

		contentContainer.setPadding((g.getLevel() -1) * 35,
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
//		V2Log.e(mGroup.getOnlineUserCount() + " / "
//				+ mGroup.getUserCount());
		mGroupStsTV.setText(mGroup.getOnlineUserCount() + " / "
				+ mGroup.getUserCount());
	}
	
	
	public void updateChecked() {
		this.mCheckBox.setChecked(!this.mCheckBox.isChecked());
	}
	
	public boolean isChecked() {
		return this.mCheckBox.isChecked();
	}
}
