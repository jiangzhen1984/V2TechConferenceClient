package com.bizcom.vc.activity.crow;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.bizcom.bo.GroupUserObject;
import com.bizcom.db.provider.VerificationProvider;
import com.bizcom.request.V2CrowdGroupRequest;
import com.bizcom.request.jni.CreateCrowdResponse;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.request.util.MessageListener;
import com.bizcom.vc.activity.BaseCreateActivity;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vc.widget.MultilevelListView.ItemData;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.CrowdGroup.AuthType;
import com.bizcom.vo.Group;
import com.bizcom.vo.Group.GroupType;
import com.bizcom.vo.User;
import com.bizcom.vo.VMessageQualificationApplicationCrowd;
import com.v2tech.R;

/**
 * Intent parameters:<br>
 * mode : true means in invitation mode, otherwise in create mode
 * 
 * @author 28851274
 * 
 */
public class CrowdCreateActivity extends BaseCreateActivity {

	private static final int CREATE_GROUP_MESSAGE = 4;
	private static final int UPDATE_CROWD_RESPONSE = 5;
	private static final int END_CREATE_OPERATOR = 9;

	private static final int SEND_SERVER_TYPE_INVITE = 10;
	private static final int SEND_SERVER_TYPE_CREATE = 11;

	private LocalHandler mLocalHandler = new LocalHandler();

	private Spinner mRuleSpinner;
	private EditText mCrowdTitleET;

	private List<Group> mGroupList;
	private CrowdGroup crowd;
	private V2CrowdGroupRequest cg = new V2CrowdGroupRequest();

	private boolean isInInvitationMode = false;

	private ProgressDialog mWaitingDialog;
	private ProgressDialog mCreateWaitingDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.initCreateType(BaseCreateActivity.CREATE_LAYOUT_TYPE_CROWD);
		super.onCreate(savedInstanceState);

		isInInvitationMode = getIntent().getBooleanExtra("mode", false);
		if (isInInvitationMode) {
			findViewById(R.id.ws_common_create_custom_content_ly)
					.setVisibility(View.GONE);
			titleContentTV.setText(R.string.group_invitation_title);
		}
	}

	@Override
	protected void init() {
		mCrowdTitleET = (EditText) findViewById(R.id.ws_common_create_edit_name_et);
		mRuleSpinner = (Spinner) findViewById(R.id.group_create_group_rule);
		loadCache();

		String hint = getResources().getString(
				R.string.crowd_create_name_input_hint);
		SpannableStringBuilder style = new SpannableStringBuilder(hint);
		style.setSpan(new ForegroundColorSpan(Color.rgb(194, 194, 194)), 0, // common_gray_color_c2
				hint.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		mCrowdTitleET.setHint(style);
		// init spinner
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext,
				R.layout.crowd_create_activity_spinner_item);
		String level[] = getResources().getStringArray(R.array.crowd_rules);
		for (int i = 0; i < level.length; i++) {
			adapter.add(level[i]);
		}
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mRuleSpinner.setAdapter(adapter);
		mRuleSpinner.setSelection(0);

		mCrowdTitleET = (EditText) findViewById(R.id.ws_common_create_edit_name_et);
		new LoadContactsAT().execute();
	}

	/**
	 * For invite new member action
	 */
	private void loadCache() {
		crowd = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.CHATING.intValue(),
				getIntent().getLongExtra("cid", 0));
		if (crowd != null) {
			mRuleSpinner.setEnabled(false);
			mCrowdTitleET.setEnabled(false);
			if (crowd.getAuthType() == AuthType.ALLOW_ALL) {
				mRuleSpinner.setSelection(0);
			} else if (crowd.getAuthType() == AuthType.QULIFICATION) {
				mRuleSpinner.setSelection(1);
			} else if (crowd.getAuthType() == AuthType.NEVER) {
				mRuleSpinner.setSelection(2);
			}
			mCrowdTitleET.setText(crowd.getName());
		}
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
			Toast.makeText(mContext, R.string.error_local_connect_to_server,
					Toast.LENGTH_SHORT).show();
			return;
		}

		String title = mCrowdTitleET.getText().toString();
		if (title == null || title.trim().isEmpty()) {
			mCrowdTitleET
					.setError(getString(R.string.error_crowd_title_required));
			mCrowdTitleET.requestFocus();
			return;
		}

		if (crowd != null) {
			synchronized (CrowdCreateActivity.class) {
				List<User> removeUsers = new ArrayList<User>();
				List<User> users = crowd.getUsers();
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

				mCreateWaitingDialog = ProgressDialog.show(
						mContext,
						"",
						mContext.getResources().getString(
								R.string.notification_watiing_process), true);
				sendServer(SEND_SERVER_TYPE_INVITE);
			}
		} else {
			crowd = new CrowdGroup(0, mCrowdTitleET.getText().toString(),
					GlobalHolder.getInstance().getCurrentUser(), new Date(
							GlobalConfig.getGlobalServerTime()));
			int pos = mRuleSpinner.getSelectedItemPosition();
			// Position match with R.array.crowd_rules
			if (pos == 0) {
				crowd.setAuthType(CrowdGroup.AuthType.ALLOW_ALL);
			} else if (pos == 1) {
				crowd.setAuthType(CrowdGroup.AuthType.QULIFICATION);
			} else if (pos == 2) {
				crowd.setAuthType(CrowdGroup.AuthType.NEVER);
			}

			mCreateWaitingDialog = ProgressDialog.show(
					mContext,
					"",
					mContext.getResources().getString(
							R.string.notification_watiing_process), true);
			sendServer(SEND_SERVER_TYPE_CREATE);
		}
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
				for (User user : userList) {
					saveQualication(user);
				}
			}
		}).start();
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
			startSelectGroup(mLocalHandler, cb, (Group) obj);
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		setResult(Activity.RESULT_CANCELED, null);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// call clear to remove callback from JNI.
		cg.clearCalledBack();
	}

	// public boolean isScreenLarge() {
	// final int screenSize = getResources().getConfiguration().screenLayout
	// & Configuration.SCREENLAYOUT_SIZE_MASK;
	// return screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE
	// || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE;
	// }

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

	}

	@Override
	protected void addAttendee(User u) {
		if (u.isCurrentLoggedInUser()) {
			return;
		}
		super.addAttendee(u);
	}

	private void saveQualication(User user) {

		if (crowd == null)
			crowd = new CrowdGroup(0, "", null);

		long waitMessageExist = VerificationProvider
				.queryCrowdInviteWaitingQualMessageById(user.getmUserId() , crowd == null ? 0 : crowd.getmGId());
		if (waitMessageExist != -1) {
			V2Log.d("CrowdCreateActivity  --> Save VMessageQualification Cache Object failed , "
					+ "Because already exist in database...groupID is : "
					+ crowd == null ? "creater"
					: crowd.getmGId() + "--" + user == null ? "user" : user
							.getmUserId() + "");
			return;
		}

		VMessageQualificationApplicationCrowd crowdQuion = new VMessageQualificationApplicationCrowd(
				crowd, user);
		Uri uri = VerificationProvider.saveQualicationMessage(crowdQuion, true);
		if (uri != null) {
			V2Log.d("CrowdCreateActivity  --> Save VMessageQualification Cache Object Successfully , "
					+ "groupID is : "
					+ crowd.getmGId()
					+ " userID is : "
					+ user.getmUserId()
					+ ""
					+ " database id is : "
					+ Long.parseLong(uri.getLastPathSegment())
					+ " URI is : "
					+ uri.toString());
		} else {
			V2Log.d("CrowdCreateActivity  --> Save VMessageQualification Cache Object failed , "
					+ "the Uri is null...groupID is : "
					+ crowd.getmGId()
					+ " userID is : " + user.getmUserId());
		}
	}

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
					CrowdGroup cg = (CrowdGroup) recr.callerObject;
					long id = ((CreateCrowdResponse) recr).getGroupId();
					cg.setGId(id);
					cg.setCreateDate(new Date());

					// add group to global cache
					GlobalHolder.getInstance().addGroupToList(
							GroupType.CHATING.intValue(), cg);
					// Add self to list
					cg.addUserToGroup(GlobalHolder.getInstance()
							.getCurrentUser());
					// send broadcast to inform new crowd notification
					Intent i = new Intent();
					i.setAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
					i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
					i.putExtra("group", new GroupUserObject(
							V2GlobalConstants.GROUP_TYPE_CROWD, id, -1));
					mContext.sendBroadcast(i);

					Intent crowdIntent = new Intent(
							PublicIntent.SHOW_CROWD_DETAIL_ACTIVITY);
					crowdIntent.addCategory(PublicIntent.DEFAULT_CATEGORY);
					crowdIntent.putExtra("cid", id);
					mContext.startActivity(crowdIntent);

					// finish current activity
					finish();
				} else if (recr.getResult() == JNIResponse.Result.TIME_OUT) {
					cg.setmPendingCrowdId(0);
					V2Log.e("CrowdCreateActivity CREATE_GROUP_MESSAGE --> create crowd group failed.. time out!!");
					mErrorNotification.setVisibility(View.VISIBLE);
					mErrorNotification
							.setText(R.string.crowd_create_activity_time_out_error_info);
				} else {
					V2Log.e("CrowdCreateActivity CREATE_GROUP_MESSAGE --> create crowd group failed.. ERROR CODE IS : "
							+ recr.getResult().name());
					mErrorNotification.setVisibility(View.VISIBLE);
					mErrorNotification
							.setText(R.string.crowd_create_activity_error_info);
				}
			}
				break;
			case UPDATE_CROWD_RESPONSE:
				// finish current activity
				finish();
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
					cg.createCrowdGroup(crowd, userList, new MessageListener(
							mLocalHandler, CREATE_GROUP_MESSAGE, crowd));
				} else {
					cg.inviteMember(crowd, userList, new MessageListener(
							mLocalHandler, UPDATE_CROWD_RESPONSE, crowd));
				}
				break;
			}
		}

	}

}
