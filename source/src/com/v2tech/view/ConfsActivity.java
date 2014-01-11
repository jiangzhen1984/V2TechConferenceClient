package com.v2tech.view;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.Group;
import com.v2tech.util.V2Log;

public class ConfsActivity extends Activity {

	
	private Context mContext;
	
	private LinearLayout mGroupContainer;
	
	private Handler mHandler = new ConfsHandler();
	
	private static final int NOTIFY_CONFS_LIST = 1;
	
	private List<Group> mList;
	
	 
	private ImageView mSettingButtonIV;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.activity_confs);
		mGroupContainer = (LinearLayout)findViewById(R.id.group_list_container);
		mSettingButtonIV =(ImageView)findViewById(R.id.group_list_setting);
		mSettingButtonIV.setOnClickListener(mSettingButtonListener);
		mList = GlobalHolder.getInstance().getList();
		addGroupList(mList);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.v2tech.group_changed");
		filter.addCategory("com.v2tech");
		this.registerReceiver(mGroupChnageListener, filter);		
	}
	
	
	private OnClickListener mSettingButtonListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			mContext.startActivity(new Intent(mContext, SettingActivity.class));
			overridePendingTransition(R.animator.down_in,R.animator.down_out);
			finish();
		}
		
	};
	
	
	private BroadcastReceiver mGroupChnageListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			nodifyGroupListChange(GlobalHolder.getInstance().getList());
		}
		
	};


	public void nodifyGroupListChange(List<Group> list) {		
		Message.obtain(mHandler, NOTIFY_CONFS_LIST, list).sendToTarget();
	}
	
	
	private void addGroupList(List<Group> list) {
		if (list == null || list.size()<=0) {
			V2Log.w(" group list is null");
			return;
		}
		mGroupContainer.removeAllViews();
		for (Group g : list) {
			final GroupLayout gp = new GroupLayout(mContext, g);
			gp.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent i = new  Intent(mContext, VideoActivity.class);
					i.putExtra("gId", gp.getGroupId());
					startActivityForResult(i, 0);
				}
				
			});
			mGroupContainer.addView(gp);
		}
		
	}
	
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != 0) {
			Toast.makeText(mContext, data.getExtras().getString("error_msg"), Toast.LENGTH_LONG).show();
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
