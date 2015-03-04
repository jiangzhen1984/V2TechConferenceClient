package com.bizcom.vc.activity.crow;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.bizcom.bo.GroupUserObject;
import com.bizcom.request.V2CrowdGroupRequest;
import com.bizcom.request.util.BitmapManager;
import com.bizcom.request.util.BitmapManager.BitmapChangedListener;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vc.widget.cus.GroupMemberView;
import com.bizcom.vc.widget.cus.GroupMemberView.ClickListener;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.Group.GroupType;
import com.bizcom.vo.User;
import com.v2tech.R;

public class CrowdMembersActivity extends Activity {

	private Context mContext;

	private ListView mMembersContainer;
	private MembersAdapter adapter;
	private TextView mInvitationButton;
	private View mReturnButton;

	private List<User> mMembers;
	private CrowdGroup crowd;

	private boolean isInDeleteMode;
	private V2CrowdGroupRequest service;
	private LocalReceiver localReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.crowd_members_activity);
		mContext = this;
		mMembersContainer = (ListView) findViewById(R.id.crowd_members_list);
		mMembersContainer.setOnItemClickListener(itemListener);
		mMembersContainer.setOnItemLongClickListener(itemLongListener);

		mInvitationButton = (TextView) findViewById(R.id.crowd_members_invitation_button);
		mInvitationButton.setOnClickListener(mInvitationButtonListener);

		mReturnButton = findViewById(R.id.crowd_members_return_button);
		mReturnButton.setOnClickListener(mReturnListener);

		crowd = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.CHATING.intValue(),
				getIntent().getLongExtra("cid", 0));
		mMembers = crowd.getUsers();
		adapter = new MembersAdapter();
		mMembersContainer.setAdapter(adapter);
		overridePendingTransition(R.anim.left_in, R.anim.left_out);
		service = new V2CrowdGroupRequest();
		if (crowd.getOwnerUser().getmUserId() != GlobalHolder.getInstance()
				.getCurrentUserId()) {
			mInvitationButton.setVisibility(View.INVISIBLE);
		}

		BitmapManager.getInstance().registerBitmapChangedListener(listener);
		initReceiver();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(localReceiver);
		service.clearCalledBack();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		if (isInDeleteMode) {
			isInDeleteMode = false;
			mInvitationButton.setText(R.string.crowd_members_invitation);
			adapter.notifyDataSetChanged();
			return;
		}
		super.onBackPressed();
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.right_in, R.anim.right_out);
	}

	private BitmapChangedListener listener = new BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			if (user == null || bm == null)
				return;

			for (User member : mMembers) {
				if (member.getmUserId() == user.getmUserId()) {
					member.setAvatarBitmap(bm);
				}
			}
		}
	};

	private void initReceiver() {
		localReceiver = new LocalReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(JNIService.JNI_BROADCAST_KICED_CROWD);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);
		filter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_ADDED);
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		this.registerReceiver(localReceiver, filter);
	}

	private OnItemClickListener itemListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int pos,
				long id) {

		}

	};

	private OnItemLongClickListener itemLongListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int pos, long id) {
			if (crowd.getOwnerUser().getmUserId() != GlobalHolder.getInstance()
					.getCurrentUserId()) {
				return false;
			}
			if (!isInDeleteMode) {
				isInDeleteMode = true;
				mInvitationButton
						.setText(R.string.crowd_members_deletion_mode_quit_button);
				adapter.notifyDataSetChanged();
				return true;
			}
			return false;
		}

	};

	private OnClickListener mInvitationButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (isInDeleteMode) {
				isInDeleteMode = false;
				mInvitationButton.setText(R.string.crowd_members_invitation);
				adapter.notifyDataSetChanged();

			} else {
				if (!GlobalHolder.getInstance().isServerConnected()) {
					Toast.makeText(mContext,
							R.string.error_discussion_no_network,
							Toast.LENGTH_SHORT).show();
				} else {
					// start CrowdCreateActivity
					Intent i = new Intent(
							PublicIntent.START_GROUP_CREATE_ACTIVITY);
					i.addCategory(PublicIntent.DEFAULT_CATEGORY);
					i.putExtra("cid", crowd.getmGId());
					i.putExtra("mode", true);
					startActivity(i);
					onBackPressed();
				}
			}
		}

	};

	private OnClickListener mReturnListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();
		}

	};

	class LocalReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(JNIService.JNI_BROADCAST_KICED_CROWD)) {
				GroupUserObject obj = intent.getParcelableExtra("group");
				if (obj == null) {
					V2Log.e("CrowdFilesActivity",
							"Received the broadcast to quit the crowd group , but crowd id is wroing... ");
					return;
				}
				if (obj.getmGroupId() == crowd.getmGId()) {
					finish();
				}
			} else if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_GROUP_USER_REMOVED)) {
				updateMembersChange();
			} else if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_GROUP_USER_ADDED)) {
//				GroupUserObject guo = (GroupUserObject) intent.getExtras().get(
//						"obj");
//				if (guo == null) 
//					return;
//				
//				if(crowd.getmGId() == guo.getmGroupId()){
//					User addUser = GlobalHolder.getInstance().getUser(guo.getmUserId());
//					mMembers.add(addUser);
//					adapter.notifyDataSetChanged();
//				}
				updateMembersChange();
			}
		}
	}

	private void updateMembersChange() {
		crowd = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.CHATING.intValue(),
				getIntent().getLongExtra("cid", 0));
		mMembers = crowd.getUsers();
		adapter.notifyDataSetChanged();
	}

	class MembersAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mMembers.size();
		}

		@Override
		public Object getItem(int position) {
			return mMembers.get(position);
		}

		@Override
		public long getItemId(int position) {
			return mMembers.get(position).getmUserId();
		}

		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = new GroupMemberView(mContext,
						mMembers.get(position), memberClick);
			} else {
				((GroupMemberView) convertView).update(isInDeleteMode,
						mMembers.get(position), crowd.getOwnerUser());
			}
			return convertView;
		}

	}

	public ClickListener memberClick = new ClickListener() {

		@Override
		public void removeMember(User user) {
			service.removeMember(crowd, user, null);
			mMembers.remove(user);
			adapter.notifyDataSetChanged();
		}
	};

}
