package com.bizcom.vc.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.v2tech.R;

public class GroupListViewAdapterItem extends LinearLayout {

	public GroupListViewAdapterItem(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		init(null);
	}

	public GroupListViewAdapterItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(null);
	}

	public GroupListViewAdapterItem(Context context) {
		super(context);
		init(null);
	}

	public GroupListViewAdapterItem(Context context, ViewGroup root) {
		super(context);
		init(null);
	}

	private void init(ViewGroup root) {
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.group_list_view_adapter_item, root,
				root == null ? false : true);

		this.addView(view, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
	}
	
	

}
