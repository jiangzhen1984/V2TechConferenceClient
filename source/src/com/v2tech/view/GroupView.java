package com.v2tech.view;

import android.content.Context;
import android.view.View;

import com.v2tech.logic.Group;

public class GroupView extends View {

	private Group mGroup;
	
	public GroupView(Context context, Group group) {
		super(context);
		this.mGroup = group;
	}
	
	
	
}
