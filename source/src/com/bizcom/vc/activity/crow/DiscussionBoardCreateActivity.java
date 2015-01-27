package com.bizcom.vc.activity.crow;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.bizcom.bo.ConversationNotificationObject;
import com.bizcom.request.CrowdGroupService;
import com.bizcom.request.MessageListener;
import com.bizcom.request.jni.CreateDiscussionBoardResponse;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.vc.activity.BaseCreateActivity;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vc.widget.MultilevelListView.ItemData;
import com.bizcom.vo.Conversation;
import com.bizcom.vo.DiscussionGroup;
import com.bizcom.vo.Group;
import com.bizcom.vo.User;
import com.bizcom.vo.Group.GroupType;
import com.v2tech.R;

/**
 * Intent parameters:<br>
 * mode : true means in invitation mode, otherwise in create mode
 * 
 * @see PublicIntent#START_DISCUSSION_BOARD_CREATE_ACTIVITY
 * @author 28851274
 * 
 */
public class DiscussionBoardCreateActivity extends BaseCreateActivity {

	private static final int CREATE_GROUP_MESSAGE = 4;
	private static final int UPDATE_CROWD_RESPONSE = 5;
	private static final int START_GROUP_SELECT = 6;
	private static final int END_GROUP_SELECT = 8;

	private static final int OP_ADD_ALL_GROUP_USER = 1;
	private static final int OP_DEL_ALL_GROUP_USER = 0;

	private static final int SEND_SERVER_TYPE_INVITE = 10;
	private static final int SEND_SERVER_TYPE_CREATE = 11;
	private static final int END_CREATE_OPERATOR = 12;

	private LocalHandler mLocalHandler = new LocalHandler();

	private List<Group> mGroupList;
	private DiscussionGroup discussion;
	private CrowdGroupService cg = new CrowdGroupService();

	private boolean isInInvitationMode = false;

	private ProgressDialog mWaitingDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.initCreateType(BaseCreateActivity.CREATE_LAYOUT_TYPE_DISCUSSION);
		super.onCreate(savedInstanceState);

		isInInvitationMode = getIntent().getBooleanExtra("mode", false);
		if (isInInvitationMode) {
			titleContentTV.setText(R.string.discussion_create_invitation_title);
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		cg.clearCalledBack();
	}

	public boolean isScreenLarge() {
		final int screenSize = getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK;
		return screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE
				|| screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		setResult(Activity.RESULT_CANCELED, null);
	}

	@Override
	protected void init() {
		rightButtonTV.setClickable(false);
		rightButtonTV.setTextColor(getResources().getColor(
				R.color.conf_create_button_unclick_color));

		new LoadContactsAT().execute();
		loadCache();

	}

	@Override
	protected void setListener() {

	}

	@Override
	protected void leftButtonClickListener(View v) {
		onBackPressed();
	}

	@Override
	protected void rightButtonClickListener(View v) {
		if (!GlobalHolder.getInstance().isServerConnected()) {
			Toast.makeText(DiscussionBoardCreateActivity.this,
					R.string.error_discussion_no_network, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		rightButtonTV.setClickable(false);
		if (isInInvitationMode) {
			synchronized (DiscussionBoardCreateActivity.class) {
				List<User> removeUsers = new ArrayList<User>();
				List<User> users = discussion.getUsers();
				Iterator<User> iterator = mAttendeeList.iterator();
				while (iterator.hasNext()) {
					User checkUser = iterator.next();
					for (User user : users) {
						if (user.getmUserId() == checkUser.getmUserId()) {
							removeUsers.add(checkUser);
							break;
						}
					}
				}

				for (int i = 0; i < removeUsers.size(); i++) {
					mAttendeeList.remove(removeUsers.get(i));
				}

				List<User> newMembers = new ArrayList<User>(mAttendeeList);
				cg.inviteMember(discussion, newMembers, new MessageListener(
						mLocalHandler, UPDATE_CROWD_RESPONSE, discussion));

				mCreateWaitingDialog = ProgressDialog.show(
						mContext,
						"",
						mContext.getResources().getString(
								R.string.notification_watiing_process), true);
				sendServer(SEND_SERVER_TYPE_INVITE);
			}
		} else {
			if (mAttendeeList.size() <= 0) {
				Toast.makeText(mContext,
						R.string.error_discussion_require_members,
						Toast.LENGTH_SHORT).show();
				return;
			} else if (mAttendeeList.size() == 1) {
				if (mAttendeeList.iterator().next().getmUserId() == GlobalHolder
						.getInstance().getCurrentUserId()) {
					Toast.makeText(mContext,
							R.string.error_discussion_require_members,
							Toast.LENGTH_SHORT).show();
					return;
				}
			}
			discussion = new DiscussionGroup(0, "", GlobalHolder.getInstance()
					.getCurrentUser(), new Date());
			mCreateWaitingDialog = ProgressDialog.show(
					mContext,
					"",
					mContext.getResources().getString(
							R.string.notification_watiing_process), true);
			sendServer(SEND_SERVER_TYPE_CREATE);
		}
	}

	@Override
	protected void mAttendeeContainerItemClick(AdapterView<?> parent,
			View view, int position, long id) {
		User user = mAttendeeArrayList.get(position);
		mGroupListView.updateCheckItem(user, false);
		for (Group group : user.getBelongsGroup()) {
			List<User> users = group.getUsers();
			mGroupListView.checkBelongGroupAllChecked(group, users);
		}
		updateUserToAttendList(user);
	}

	@Override
	protected void mGroupListViewItemClick(AdapterView<?> parent, View view,
			int position, long id, ItemData item) {

	}

	@Override
	protected void mGroupListViewlongItemClick(AdapterView<?> parent,
			View view, int position, long id, ItemData item) {

	}

	@Override
	protected void mGroupListViewCheckBoxChecked(View view, ItemData item) {
		CheckBox cb = (CheckBox) view;
		Object obj = item.getObject();
		if (obj instanceof User) {
			User user = (User) obj;
			mGroupListView.updateCheckItem((User) obj, cb.isChecked());
			updateUserToAttendList(user);

			Set<Group> belongsGroup = user.getBelongsGroup();
			for (Group group : belongsGroup) {
				List<User> users = group.getUsers();
				mGroupListView.checkBelongGroupAllChecked(group, users);
			}
		} else if (obj instanceof Group) {
			Message.obtain(
					mLocalHandler,
					START_GROUP_SELECT,
					cb.isChecked() ? OP_ADD_ALL_GROUP_USER
							: OP_DEL_ALL_GROUP_USER, 0, (Group) obj)
					.sendToTarget();
			mGroupListView.updateCheckItem((Group) obj, cb.isChecked());
		}
	}

	/**
	 * For invite new member action
	 */
	private void loadCache() {
		discussion = (DiscussionGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.DISCUSSION.intValue(),
				getIntent().getLongExtra("cid", 0));
	}

	private void updateUserToAttendList(final User u) {
		if (u == null) {
			return;
		}
		boolean remove = false;
		for (User tu : mAttendeeList) {
			if (tu.getmUserId() == u.getmUserId()) {
				mAttendeeList.remove(tu);
				remove = true;
				break;
			}
		}

		if (remove) {
			removeAttendee(u);
		} else {
			addAttendee(u);
		}

		changeConfirmAble();
	}

	private void sendServer(final int type) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				List<User> userList = new ArrayList<User>(mAttendeeList);
				Message msg = Message.obtain(mLocalHandler,
						END_CREATE_OPERATOR, userList);
				msg.arg1 = type;
				msg.sendToTarget();
			}
		}).start();
	}

	private void changeConfirmAble() {
		if (mAttendeeList.size() > 0) {
			rightButtonTV.setClickable(true);
			rightButtonTV.setTextColor(getResources().getColor(
					R.color.conf_create_button_color));
		} else {
			rightButtonTV.setClickable(false);
			rightButtonTV.setTextColor(getResources().getColor(
					R.color.conf_create_button_unclick_color));
		}
	}


	@Override
	protected void addAttendee(User u) {
		if (u.isCurrentLoggedInUser()) {
			return;
		}
		super.addAttendee(u);
	}

	private ProgressDialog mCreateWaitingDialog;
	class LoadContactsAT extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			if (mGroupList == null)
				mGroupList = new ArrayList<Group>();
			else
				mGroupList.clear();
			mGroupList.addAll(GlobalHolder.getInstance().getGroup(
					GroupType.CONTACT.intValue()));
			mGroupList.addAll(GlobalHolder.getInstance().getGroup(
					GroupType.ORG.intValue()));
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mGroupListView.setGroupList(mGroupList);
		}

	};

	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CREATE_GROUP_MESSAGE: {
				mCreateWaitingDialog.dismiss();
				JNIResponse recr = (JNIResponse) msg.obj;
				if (recr.getResult() == JNIResponse.Result.SUCCESS) {
					DiscussionGroup cg = (DiscussionGroup) recr.callerObject;
					long id = ((CreateDiscussionBoardResponse) recr)
							.getGroupId();
					cg.setGId(id);
					cg.setCreateDate(new Date());

					// add group to global cache
					GlobalHolder.getInstance().addGroupToList(
							GroupType.DISCUSSION.intValue(), cg);
					// Add self to list
					cg.addUserToGroup(GlobalHolder.getInstance()
							.getCurrentUser());
					cg.addUserToGroup(mAttendeeList);

					Intent i = new Intent();
					i.setAction(JNIService.JNI_BROADCAST_NEW_DISCUSSION_NOTIFICATION);
					i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
					i.putExtra("gid", cg.getmGId());
					sendBroadcast(i);

					Intent crowdIntent = new Intent(
							PublicIntent.START_CONVERSACTION_ACTIVITY);
					crowdIntent.addCategory(PublicIntent.DEFAULT_CATEGORY);
					crowdIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
					crowdIntent.putExtra("obj",
							new ConversationNotificationObject(
									Conversation.TYPE_DISCUSSION, id));
					startActivity(crowdIntent);

					// finish current activity
					finish();
				} else if (recr.getResult() == JNIResponse.Result.TIME_OUT) {
					rightButtonTV.setClickable(true);
					cg.setmPendingCrowdId(0);
					V2Log.e("CrowdCreateActivity CREATE_GROUP_MESSAGE --> create discussion group failed.. time out!!");
				} else {
					rightButtonTV.setClickable(true);
					V2Log.e("CrowdCreateActivity CREATE_GROUP_MESSAGE --> create discussion group failed.. ERROR CODE IS : "
							+ recr.getResult().name());
				}
			}
				break;
			case UPDATE_CROWD_RESPONSE:
				discussion.addUserToGroup(mAttendeeList);
				setResult(Activity.RESULT_OK);
				finish();
				break;

			case START_GROUP_SELECT: {
				mWaitingDialog = ProgressDialog.show(
						mContext,
						"",
						mContext.getResources().getString(
								R.string.notification_watiing_process), true);
				selectGroup((Group) msg.obj,
						msg.arg1 == OP_ADD_ALL_GROUP_USER ? true : false);
				Message.obtain(this, END_GROUP_SELECT).sendToTarget();
				break;
			}
			case END_GROUP_SELECT:
				mWaitingDialog.dismiss();
				mWaitingDialog = null;
				changeConfirmAble();
				break;
			case END_CREATE_OPERATOR:
				if (mCreateWaitingDialog != null
						&& mCreateWaitingDialog.isShowing()) {
					mCreateWaitingDialog.dismiss();
				}

				List<User> userList = (List<User>) msg.obj;
				int opType = msg.arg1;
				if (opType == SEND_SERVER_TYPE_CREATE) {
					// Do not add userList to crowd, because this just
					// invitation.
					cg.createDiscussionBoard(discussion, userList,
							new MessageListener(mLocalHandler,
									CREATE_GROUP_MESSAGE, discussion));
				} else {
					cg.inviteMember(discussion, userList, new MessageListener(
							mLocalHandler, UPDATE_CROWD_RESPONSE, discussion));
				}
				break;
			}
		}

	}

}
