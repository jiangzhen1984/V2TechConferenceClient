package com.v2tech.view;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.LinearLayout;

import com.V2.jni.GroupRequest;
import com.v2tech.R;
import com.v2tech.logic.Group;
import com.v2tech.util.V2Log;

public class ConfsActivity extends Activity {

	
	private Context mContext;
	
	private LinearLayout mGroupContainer;
	
	private GroupRequest mGR = GroupRequest.getInstance(this);
	
	private Handler mHandler = new ConfsHandler();
	
	private static final int NOTIFY_CONFS_LIST = 1;
	
	private List<Group> mList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.activity_confs);
		mGroupContainer = (LinearLayout)findViewById(R.id.group_list_container);
		
		Object obj = getLastNonConfigurationInstance();
		if (obj != null) {
			mList = (List<Group>) obj;
			addGroupList(mList);
		}
		
	}
	
	
	
	
	


	@Override
	public Object onRetainNonConfigurationInstance() {
	    return mList;
	}



	public void nodifyGroupListChange(List<Group> list) {		
		Message.obtain(mHandler, NOTIFY_CONFS_LIST, list).sendToTarget();
	}
	
	
	private void addGroupList(List<Group> list) {
		if (list == null) {
			V2Log.w(" group list is null");
			return;
		}
		for (Group g : list) {
			mGroupContainer.addView(new GroupLayout(mContext, g));
		}
	}
	
	class ConfsHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NOTIFY_CONFS_LIST:
				if (msg.obj == null) {
					V2Log.w("no group object");
					return;
				}
				mList =(List<Group>)msg.obj;
				addGroupList(mList);
				break;
			}
		}
		
	}

	
	
}
