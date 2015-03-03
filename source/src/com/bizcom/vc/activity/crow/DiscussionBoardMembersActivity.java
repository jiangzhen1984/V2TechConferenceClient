package com.bizcom.vc.activity.crow;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import com.bizcom.request.V2CrowdGroupRequest;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.widget.cus.GroupMemberView;
import com.bizcom.vc.widget.cus.GroupMemberView.ClickListener;
import com.bizcom.vo.DiscussionGroup;
import com.bizcom.vo.Group.GroupType;
import com.bizcom.vo.User;
import com.v2tech.R;

/**
 * 
 * <ul>
 * Intent key: cid : discussion board id
 * </ul>
 * 
 * @see PublicIntent#SHOW_DISCUSSION_BOARD_MEMBERS_ACTIVITY
 * @author 28851274
 * 
 */
public class DiscussionBoardMembersActivity extends Activity {

	private Context mContext;

	private ListView mMembersContainer;
	private MembersAdapter adapter;
	private TextView mInvitationButton;
	private View mReturnButton;

	private List<User> mMembers;
	private DiscussionGroup crowd;

	private boolean isInDeleteMode;
	private V2CrowdGroupRequest service;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.discussion_board_members_activity);
		mContext = this;
		mMembersContainer = (ListView) findViewById(R.id.discussion_board_members_list);
		mMembersContainer.setOnItemClickListener(itemListener);
		mMembersContainer.setOnItemLongClickListener(itemLongListener);

		mInvitationButton = (TextView) findViewById(R.id.discussion_board_members_invitation_button);
		mInvitationButton.setOnClickListener(mInvitationButtonListener);

		mReturnButton = findViewById(R.id.discussion_board_members_return_button);
		mReturnButton.setOnClickListener(mReturnListener);

		crowd = (DiscussionGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.DISCUSSION.intValue(),
				getIntent().getLongExtra("cid", 0));
		mMembers = crowd.getUsers();
		sortMembers();
		adapter = new MembersAdapter();
		mMembersContainer.setAdapter(adapter);
		overridePendingTransition(R.anim.left_in, R.anim.left_out);
		service = new V2CrowdGroupRequest();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
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
			mInvitationButton
					.setText(R.string.discussion_board_members_invitation_button_text);
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
			mMembers = crowd.getUsers();
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.right_in, R.anim.right_out);
	}

	private void sortMembers() {
		long ownerID = crowd.getOwnerUser().getmUserId();
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
						.setText(R.string.discussion_board_members_deletion_mode_quit_button);
				adapter.notifyDataSetChanged();
				return true;
			}
			return false;
		}

	};

	private OnClickListener mInvitationButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (!GlobalHolder.getInstance().isServerConnected()) {
				Toast.makeText(DiscussionBoardMembersActivity.this,
						R.string.error_discussion_no_network,
						Toast.LENGTH_SHORT).show();
				return;
			}

			if (isInDeleteMode) {
				isInDeleteMode = false;
				mInvitationButton
						.setText(R.string.discussion_board_members_invitation_button_text);
				for (User user : mMembers) {
					if (user.isShowDelete == true)
						user.isShowDelete = false;
				}
				adapter.notifyDataSetChanged();
			} else {
				// start CrowdCreateActivity
				Intent i = new Intent(
						PublicIntent.START_DISCUSSION_BOARD_CREATE_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.putExtra("cid", crowd.getmGId());
				i.putExtra("mode", true);
				startActivityForResult(i, 100);
			}
		}

	};

	private OnClickListener mReturnListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();
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
