package com.v2tech.view;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.V2.jni.GroupRequest;
import com.v2tech.R;
import com.v2tech.logic.GlobalHolder;

public class ConfsActivity extends Activity {

	
	private Context mContext;
	
	private GroupRequest mGR = GroupRequest.getInstance(this);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.activity_confs);
		mGR.getGroupInfo(3, GlobalHolder.getLoggedUserId());
		mGR.getGroupInfo(1, GlobalHolder.getLoggedUserId());
		mGR.getGroupInfo(2, GlobalHolder.getLoggedUserId());
	}

	
	
}
