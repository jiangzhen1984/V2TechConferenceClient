package com.bizcom.vc.activity.crow;

import java.util.ArrayList;
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
import com.bizcom.vo.DiscussionGroup;
import com.bizcom.vo.Group;
import com.bizcom.vo.Group.GroupType;
import com.bizcom.vo.User;
import com.v2tech.R;

public class GroupMemberActivity extends Activity {

	public static final int GROUP_MEMBER_TYPE_CROWD = 0;
	public static final int GROUP_MEMBER_TYPE_DISCUSSION = 1;

	private Context mContext;
	private int activityType;

	private Group memberGroup;
	private List<User> mMembers;
	private List<User> deleteMemberList;
	private ListView mMembersContainer;
	private TextView mInvitationButton;
	private View mReturnButton;
	private MembersAdapter adapter;

	private LocalReceiver localReceiver;
	private V2CrowdGroupRequest service;

	private boolean isInDeleteMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.crowd_members_activity);
		mContext = this;
		activityType = getIntent().getIntExtra("memberType", 0);
		initView();
		setListener();
		initReceiver();
		init();
	}

	@Override
	protected void onDestroy() {
		deleteMemberList.clear();
		mMembers.clear();
		if (activityType == GROUP_MEMBER_TYPE_CROWD) {
			this.unregisterReceiver(localReceiver);
		} 
		service.clearCalledBack();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		if (isInDeleteMode) {
			isInDeleteMode = false;
			mInvitationButton.setText(R.string.crowd_members_invitation);
			for (int i = 0; i < deleteMemberList.size(); i++) {
				User user = deleteMemberList.get(i);
				int index = mMembers.indexOf(user);
				if (index != -1) {
					User search = mMembers.get(index);
					search.isShowDelete = false;
				}
			}
			deleteMemberList.clear();
			adapter.notifyDataSetChanged();
			return;
		}
		super.onBackPressed();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != Activity.RESULT_CANCELED) {
			mMembers.clear();
			mMembers = memberGroup.getUsers();
			adapter.notifyDataSetChanged();
		}
	}

	private void init() {

		BitmapManager.getInstance().registerBitmapChangedListener(listener);
		service = new V2CrowdGroupRequest();
		deleteMemberList = new ArrayList<User>();

		long cid = getIntent().getLongExtra("cid", 0);
		if (activityType == GROUP_MEMBER_TYPE_CROWD) {
			memberGroup = GlobalHolder.getInstance().getGroupById(
					GroupType.CHATING.intValue(), cid);
			if (memberGroup.getOwnerUser().getmUserId() != GlobalHolder
					.getInstance().getCurrentUserId()) {
				mInvitationButton.setVisibility(View.INVISIBLE);
			}
		} else {
			memberGroup = GlobalHolder.getInstance().getGroupById(
					GroupType.DISCUSSION.intValue(), cid);
		}

		mMembers = memberGroup.getUsers();

		if (activityType == GROUP_MEMBER_TYPE_DISCUSSION) {
			sortMembers();
		}
		adapter = new MembersAdapter();
		mMembersContainer.setAdapter(adapter);
	}

	private void initReceiver() {
		if (activityType == GROUP_MEMBER_TYPE_CROWD) {
			localReceiver = new LocalReceiver();
			IntentFilter filter = new IntentFilter();
			filter.addAction(JNIService.JNI_BROADCAST_KICED_CROWD);
			filter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_REMOVED);
			filter.addAction(JNIService.JNI_BROADCAST_GROUP_USER_ADDED);
			filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			this.registerReceiver(localReceiver, filter);
		}
	}

	private void initView() {
		mMembersContainer = (ListView) findViewById(R.id.crowd_members_list);
		mInvitationButton = (TextView) findViewById(R.id.crowd_members_invitation_button);
		mReturnButton = findViewById(R.id.crowd_members_return_button);
	}

	private void setListener() {
		mMembersContainer.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

			}
		});
		mMembersContainer
				.setOnItemLongClickListener(new OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> parent,
							View view, int position, long id) {
						if (memberGroup.getOwnerUser().getmUserId() != GlobalHolder
								.getInstance().getCurrentUserId()) {
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
				});

		mReturnButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		mInvitationButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isInDeleteMode) {
					onBackPressed();
				} else {
					if (!GlobalHolder.getInstance().isServerConnected()) {
						Toast.makeText(mContext,
								R.string.error_discussion_no_network,
								Toast.LENGTH_SHORT).show();
					} else {
						Intent i = null;
						if (activityType == GROUP_MEMBER_TYPE_CROWD) {
							i = new Intent(
									PublicIntent.START_GROUP_CREATE_ACTIVITY);
							i.addCategory(PublicIntent.DEFAULT_CATEGORY);
							i.putExtra("cid", memberGroup.getmGId());
							i.putExtra("mode", true);
							startActivity(i);
						} else {
							i = new Intent(
									PublicIntent.START_DISCUSSION_BOARD_CREATE_ACTIVITY);
							i.addCategory(PublicIntent.DEFAULT_CATEGORY);
							i.putExtra("cid", memberGroup.getmGId());
							i.putExtra("mode", true);
							startActivityForResult(i, 100);
						}
					}
				}
			}
		});
	}

	private void updateMembersChange() {
		if (activityType == GROUP_MEMBER_TYPE_CROWD) {
			memberGroup = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
					GroupType.CHATING.intValue(),
					getIntent().getLongExtra("cid", 0));
		} else {
			memberGroup = (DiscussionGroup) GlobalHolder.getInstance().getGroupById(
					GroupType.DISCUSSION.intValue(),
					getIntent().getLongExtra("cid", 0));
		}
		mMembers = memberGroup.getUsers();
		adapter.notifyDataSetChanged();
	}

	private void sortMembers() {
		long ownerID = memberGroup.getOwnerUser().getmUserId();
		long loginUserID = GlobalHolder.getInstance().getCurrentUserId();
		User loginUser = GlobalHolder.getInstance().getCurrentUser();
		int ownerPos = -1;
		int loginPos = -1;
		boolean isExistCreater = false;

		for (int i = 0; i < mMembers.size(); i++) {

			if (ownerPos != -1 && loginPos != -1)
				break;

			if (ownerID == mMembers.get(i).getmUserId()) {
				ownerPos = i;
			} else if (loginUserID == mMembers.get(i).getmUserId()) {
				loginPos = i;
			}
		}

		if (ownerPos != -1) {
			isExistCreater = true;
			User user = mMembers.get(ownerPos);
			mMembers.remove(user);
			mMembers.add(0, user);
		}

		if (loginPos != -1) {
			mMembers.remove(loginUser);
			if (isExistCreater) {
				mMembers.add(1, loginUser);
			} else {
				if (loginPos != 0) {
					mMembers.add(0, loginUser);
				}
			}
		}
	}

	public ClickListener memberClick = new ClickListener() {

		@Override
		public void removeMember(User user) {
			service.removeMember(memberGroup, user, null);
			mMembers.remove(user);
			deleteMemberList.remove(user);
			adapter.notifyDataSetChanged();
		}

		@Override
		public void changeDeletedMembers(boolean isAdd, User user) {
			if (isAdd) {
				deleteMemberList.add(user);
			} else {
				deleteMemberList.remove(user);
			}
		}
	};

	class LocalReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(JNIService.JNI_BROADCAST_KICED_CROWD)) {
				GroupUserObject obj = intent.getParcelableExtra("group");
				if (obj == null) {
					V2Log.e("GroupMemberActivity",
							"Received the broadcast to quit the crowd group , but crowd id is wroing... ");
					return;
				}
				if (obj.getmGroupId() == memberGroup.getmGId()) {
					finish();
				}
			} else if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_GROUP_USER_REMOVED)) {
				updateMembersChange();
			} else if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_GROUP_USER_ADDED)) {
				// GroupUserObject guo = (GroupUserObject)
				// intent.getExtras().get(
				// "obj");
				// if (guo == null)
				// return;
				//
				// if(crowd.getmGId() == guo.getmGroupId()){
				// User addUser =
				// GlobalHolder.getInstance().getUser(guo.getmUserId());
				// mMembers.add(addUser);
				// adapter.notifyDataSetChanged();
				// }
				updateMembersChange();
			}
		}
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
						mMembers.get(position), memberGroup.getOwnerUser());
			}
			return convertView;
		}
	}
}
