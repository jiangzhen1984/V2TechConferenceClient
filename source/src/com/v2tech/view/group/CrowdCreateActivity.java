package com.v2tech.view.group;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.service.jni.CreateCrowdResponse;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.SPUtil;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.adapter.CreateConfOrCrowdAdapter;
import com.v2tech.view.conversation.MessageBuilder;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.view.widget.GroupListView;
import com.v2tech.view.widget.GroupListView.Item;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.CrowdGroup.AuthType;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.VMessageQualification.ReadState;
import com.v2tech.vo.VMessageQualification.Type;
import com.v2tech.vo.NetworkStateCode;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessageQualificationApplicationCrowd;
import com.v2tech.vo.VMessageQualificationInvitationCrowd;

/**
 * Intent parameters:<br>
 * mode :  true means in invitation mode, otherwise in create mode
 * @author 28851274
 *
 */
public class CrowdCreateActivity extends Activity {

	private static final int UPDATE_ATTENDEES = 2;
	private static final int CREATE_GROUP_MESSAGE = 4;
	private static final int UPDATE_CROWD_RESPONSE = 5;
	private static final int START_GROUP_SELECT = 6;
	private static final int DOING_SELECT_GROUP = 7;
	private static final int END_GROUP_SELECT = 8;
	
	
	private static final int OP_ADD_ALL_GROUP_USER = 1;
	private static final int OP_DEL_ALL_GROUP_USER = 0;

	private static final int PAD_LAYOUT = 1;
	private static final int PHONE_LAYOUT = 0;
	

	private Context mContext;
	private LocalHandler mLocalHandler = new LocalHandler();
	private CreateConfOrCrowdAdapter mAdapter;

	private EditText searchedTextET;
	private GroupListView mContactsContainer;
	private AdapterView<ListAdapter> mAttendeeContainer;

	private TextView mGroupTitle;
	private TextView mGroupConfirmButton;
	private TextView mReturnButton;
	private EditText mGroupTitleET;
	private Spinner mRuleSpinner;
	private LinearLayout mErrorNotificationLayout;
	private TextView mErrorNotification;

	private List<Group> mGroupList;
	private CrowdGroup crowd;
	private CrowdGroupService cg = new CrowdGroupService();

	// Used to save current selected user
	private Set<User> mUserList = new HashSet<User>();
	private List<User> mUserListArray = new ArrayList<User>();

	private int landLayout = PAD_LAYOUT;
	
	private boolean isInInvitationMode = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isScreenLarge()) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		setContentView(R.layout.crowd_create_activity);
				
		mGroupList = new ArrayList<Group>();
		mContext = this;
		mContactsContainer = (GroupListView) findViewById(R.id.group_create_contacts_list);
		mContactsContainer.setListener(listViewListener);
		mContactsContainer.setShowedCheckedBox(true);
		mContactsContainer.setTextFilterEnabled(true);
		mContactsContainer.setIgnoreCurrentUser(true);

		
		mAttendeeContainer = (AdapterView<ListAdapter>) findViewById(R.id.crowd_create_list_view);
		mAttendeeContainer.setOnItemClickListener(mItemClickedListener);
		landLayout = mAttendeeContainer.getTag().equals("vertical") ? PAD_LAYOUT
				: PHONE_LAYOUT;
		mAdapter = new CreateConfOrCrowdAdapter(mContext , mUserListArray , landLayout);
		mAttendeeContainer.setAdapter(mAdapter);

		mGroupTitle = (TextView) findViewById(R.id.ws_common_activity_title_content);
		mGroupTitle.setText(R.string.crowd_create_activity_title);
		
		mGroupConfirmButton = (TextView) findViewById(R.id.ws_common_activity_title_right_button);
		mGroupConfirmButton.setText(R.string.common_confirm_name);
		mGroupConfirmButton.setOnClickListener(confirmButtonListener);
		//�����Զ�����ɫ��hint��ʾ����
		mGroupTitleET = (EditText) findViewById(R.id.group_create_group_name);
		String hint = getResources().getString(R.string.crowd_create_name_input_hint);
		SpannableStringBuilder style = new SpannableStringBuilder(hint);
		style.setSpan(new ForegroundColorSpan(Color.rgb(194, 194, 194)), 0,  //common_gray_color_c2
				hint.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		mGroupTitleET.setHint(style);
		//�����Զ�����ɫ�ʹ�С��Spinner��ʾ����
		mRuleSpinner = (Spinner) findViewById(R.id.group_create_group_rule);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, R.layout.crowd_create_activity_spinner_item);
		String level[] = getResources().getStringArray(R.array.crowd_rules);
		for (int i = 0; i < level.length; i++) {
			adapter.add(level[i]);
		}
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mRuleSpinner.setAdapter(adapter);
		mRuleSpinner.setSelection(0);

		new LoadContactsAT().execute();

		searchedTextET = (EditText) findViewById(R.id.contacts_search);

		mErrorNotificationLayout = (LinearLayout) findViewById(R.id.group_create_error_notification);
		mErrorNotification = (TextView) findViewById(R.id.group_create_error_notification_hints);
		mReturnButton = (TextView) findViewById(R.id.ws_common_activity_title_left_button);
		mReturnButton.setText(R.string.common_return_name);
		mReturnButton.setOnClickListener(mReturnListener);
		loadCache();
		
		isInInvitationMode = getIntent().getBooleanExtra("mode", false);
		if (isInInvitationMode) {
			findViewById(R.id.group_create_input_box).setVisibility(View.GONE);;
			mGroupTitle.setText(R.string.group_invitation_title);
		}
		
		boolean checkCurrentAviNetwork = SPUtil.checkCurrentAviNetwork(mContext);
		if(!checkCurrentAviNetwork){
			mErrorNotificationLayout.setVisibility(View.VISIBLE);
			mErrorNotification
					.setText(R.string.error_create_conference_failed_no_network);
		}
		else{
			mErrorNotificationLayout.setVisibility(View.GONE);
		}
		initReceiver();
	}
	
	IntentFilter intentFilter;
	LocalBroadcastReceiver receiver = new LocalBroadcastReceiver();

	private void initReceiver() {
		intentFilter = new IntentFilter();
		intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		intentFilter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		intentFilter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		this.registerReceiver(receiver, intentFilter);
	}

	class LocalBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION.equals(intent
					.getAction())) {
				NetworkStateCode code = (NetworkStateCode) intent.getExtras()
						.get("state");
				if (code != NetworkStateCode.CONNECTED) {
					mErrorNotificationLayout.setVisibility(View.VISIBLE);
					mErrorNotification
							.setText(R.string.error_create_conference_failed_no_network);
				} else {
					mErrorNotificationLayout.setVisibility(View.GONE);
				}
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// call clear to remove callback from JNI.
		cg.clearCalledBack();
		this.unregisterReceiver(receiver);
	}

	@Override
	protected void onStart() {
		super.onStart();
		searchedTextET.addTextChangedListener(textChangedListener);
	}

	@Override
	protected void onStop() {
		super.onStop();
		searchedTextET.removeTextChangedListener(textChangedListener);
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

	/**
	 * For invite new member action
	 */
	private void loadCache() {
		crowd = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.CHATING.intValue(),
				getIntent().getLongExtra("cid", 0));
		if (crowd != null) {
			mRuleSpinner.setEnabled(false);
			if (crowd.getAuthType() == AuthType.ALLOW_ALL) {
				mRuleSpinner.setSelection(0);
			} else if (crowd.getAuthType() == AuthType.QULIFICATION) {
				mRuleSpinner.setSelection(1);
			} else if (crowd.getAuthType() == AuthType.NEVER) {
				mRuleSpinner.setSelection(2);
			}
			mGroupTitleET.setEnabled(false);
			mGroupTitleET.setText(crowd.getName());
		}
	}

	private void updateUserToAttendList(final User u) {
		if (u == null) {
			return;
		}
		boolean remove = false;
		for (User tu : mUserList) {
			if (tu.getmUserId() == u.getmUserId()) {
				mUserList.remove(tu);
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

	private void removeAttendee(User u) {
		mUserList.remove(u);
		mUserListArray.remove(u);
		mAdapter.notifyDataSetChanged();
	}

	private void addAttendee(User u) {
		if (u.isCurrentLoggedInUser()) {
			return;
		}
		boolean ret = mUserList.add(u);
		if (!ret) {
			return;
		}
		
		mUserListArray.add(u);
		mAdapter.notifyDataSetChanged();
	}
	
	
	private ProgressDialog mWaitingDialog;

	private void selectGroup(Group selectGroup, boolean addOrRemove) {
		List<Group> subGroups = selectGroup.getChildGroup();
		for (int i = 0; i < subGroups.size(); i++) {
			selectGroup(subGroups.get(i), addOrRemove);
		}
		List<User> list = selectGroup.getUsers();
		for (int i = 0; i < list.size(); i++) {
			if (addOrRemove) {
				addAttendee(list.get(i));
			} else {
				removeAttendee(list.get(i));
			}
		}
	}



	
	private OnItemClickListener mItemClickedListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			User user = mUserListArray.get(position);
			mContactsContainer.updateCheckItem(user, false);
			Message.obtain(mLocalHandler, UPDATE_ATTENDEES, user).sendToTarget();
		}
		
	};

	private TextWatcher textChangedListener = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {
			if (s.toString().isEmpty()) {
				mContactsContainer.clearTextFilter();
			} else {
				mContactsContainer.setFilterText(s.toString());
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {

		}

	};

	private GroupListView.GroupListViewListener listViewListener = new GroupListView.GroupListViewListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id, Item item) {
			return false;
		}

		@Override
		public void onItemClicked(AdapterView<?> parent, View view,
				int position, long id, Item item) {
			
		}

		public void onCheckboxClicked(View view, Item item) {
			CheckBox cb = (CheckBox)view;
			Object obj = item.getObject();
			if (obj instanceof User) {
				User user = (User) obj;
				mContactsContainer.updateCheckItem((User) obj,
						cb.isChecked());
				Message.obtain(mLocalHandler, UPDATE_ATTENDEES, (User) obj)
						.sendToTarget();
				
				Set<Group> belongsGroup = user.getBelongsGroup();
				for (Group group : belongsGroup) {
					List<User> users = group.getUsers();
					mContactsContainer.checkBelongGroupAllChecked(group , users);
				}
			} else if (obj instanceof Group) {
				Message.obtain(
						mLocalHandler,
						START_GROUP_SELECT,
						cb.isChecked() ? OP_ADD_ALL_GROUP_USER
								: OP_DEL_ALL_GROUP_USER, 0, (Group) obj)
						.sendToTarget();
				mContactsContainer.updateCheckItem((Group) obj, cb.isChecked());
			}
		}

	};
	
	
	private ProgressDialog mCreateWaitingDialog;

	private OnClickListener confirmButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
            mErrorNotificationLayout.setVisibility(View.GONE);

			String title = mGroupTitleET.getText().toString();
			if (title == null || title.trim().isEmpty()) {
				mGroupTitleET
						.setError(getString(R.string.error_crowd_title_required));
				mGroupTitleET.requestFocus();
				return;
			}

			if (crowd != null) {
				List<User> users = crowd.getUsers();
				Iterator<User> iterator = mUserList.iterator();
				while (iterator.hasNext()) {
					User checkUser = iterator.next();
					for (User user : users) {
						if(user.getmUserId() == checkUser.getmUserId()){
							mUserList.remove(checkUser);
							break;
						}
					}
				}
				
				List<User> newMembers = new ArrayList<User>(mUserList);
				for (User user : newMembers) {
					saveQualication(user);
				}
				cg.inviteMember(crowd, newMembers, new MessageListener(
						mLocalHandler, UPDATE_CROWD_RESPONSE, crowd));
			} else {
				CrowdGroup crowd = new CrowdGroup(0, mGroupTitleET.getText()
						.toString(), GlobalHolder.getInstance()
						.getCurrentUser(), new Date());
				int pos = mRuleSpinner.getSelectedItemPosition();
				// Position match with R.array.crowd_rules
				if (pos == 0) {
					crowd.setAuthType(CrowdGroup.AuthType.ALLOW_ALL);
				} else if (pos == 1) {
					crowd.setAuthType(CrowdGroup.AuthType.QULIFICATION);
				} else if (pos == 2) {
					crowd.setAuthType(CrowdGroup.AuthType.NEVER);
				}

				List<User> userList = new ArrayList<User>(mUserList);
				if (mCreateWaitingDialog != null && mCreateWaitingDialog.isShowing()) {
					mCreateWaitingDialog.dismiss();
				}
				mCreateWaitingDialog = ProgressDialog.show(
						mContext,
						"",
						mContext.getResources().getString(
								R.string.notification_watiing_process), true);
				
				//Do not add userList to crowd, because this just invitation.
				cg.createCrowdGroup(crowd, userList, new MessageListener(mLocalHandler,
						CREATE_GROUP_MESSAGE, crowd));
//				view.setEnabled(false);
			}
		}

	};
	
	private void saveQualication(User user){

//        User applicant = GlobalHolder.getInstance().getUser(crowd.getOwnerUser().getmUserId());
//        if (applicant == null)
//            applicant = new User(uid);
		VMessageQualificationApplicationCrowd crowdQuion = new VMessageQualificationApplicationCrowd(
				crowd, user);
		crowdQuion.setReadState(ReadState.UNREAD);
		Uri uri = MessageBuilder.saveQualicationMessage(crowdQuion , true);
		if (uri != null){
			V2Log.e("MessageBuilder updateQualicationMessageState --> Save VMessageQualification Object Successfully , "
					+ "the Uri is null...groupID is : " + crowd.getmGId() + " userID is : " + user.getmUserId());
		}
		else{
			V2Log.e("MessageBuilder updateQualicationMessageState --> Save VMessageQualification Object failed , "
					+ "the Uri is null...groupID is : " + crowd.getmGId() + " userID is : " + user.getmUserId());
		}
	
	}

	private OnClickListener mReturnListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();
		}

	};

	class LoadContactsAT extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			mGroupList.clear();
			mGroupList.addAll(GlobalHolder.getInstance().getGroup(
					GroupType.CONTACT.intValue()));
			mGroupList.addAll(GlobalHolder.getInstance().getGroup(
					GroupType.ORG.intValue()));
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mContactsContainer.setGroupList(mGroupList);
		}

	};
	
	class LocalHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_ATTENDEES:
				updateUserToAttendList((User) msg.obj);
				break;
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
					//Add self to list
					cg.addUserToGroup(GlobalHolder.getInstance().getCurrentUser());
					// send broadcast to inform new crowd notification
					Intent i = new Intent();
					i.setAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
					i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
					i.putExtra("crowd", id);
					mContext.sendBroadcast(i);

					Intent crowdIntent = new Intent(
							PublicIntent.SHOW_CROWD_DETAIL_ACTIVITY);
					crowdIntent.addCategory(PublicIntent.DEFAULT_CATEGORY);
					crowdIntent.putExtra("cid", id);
					mContext.startActivity(crowdIntent);
					
					// finish current activity
					finish();
				} else if(recr.getResult() == JNIResponse.Result.TIME_OUT){
                   cg.setmPendingCrowdId(0);
                   V2Log.e("CrowdCreateActivity CREATE_GROUP_MESSAGE --> create crowd group failed.. time out!!");
                   mErrorNotificationLayout.setVisibility(View.VISIBLE);
                   mErrorNotification.setText(R.string.crowd_create_activity_error_info);
                } else {
                    V2Log.e("CrowdCreateActivity CREATE_GROUP_MESSAGE --> create crowd group failed.. ERROR CODE IS : " +
                            recr.getResult().name());
					mErrorNotificationLayout.setVisibility(View.VISIBLE);
					mErrorNotification.setText(R.string.crowd_create_activity_error_info);
				}
			}
				break;
			case UPDATE_CROWD_RESPONSE:
				//Do not add user to list because user doesn't accept invitation yet
				
//				if (crowd.getAuthType() == AuthType.ALLOW_ALL) {
//					crowd.addUserToGroup(mUserList);
//				}
				// finish current activity
				finish();
				break;
				
			case START_GROUP_SELECT: {
				mWaitingDialog = ProgressDialog.show(
						mContext,
						"",
						mContext.getResources().getString(
								R.string.notification_watiing_process), true);
				Message.obtain(this, DOING_SELECT_GROUP, msg.arg1, msg.arg2,
						msg.obj).sendToTarget();
				break;
			}
			case DOING_SELECT_GROUP:
				selectGroup((Group) msg.obj,
						msg.arg1 == OP_ADD_ALL_GROUP_USER ? true : false);
				Message.obtain(this, END_GROUP_SELECT).sendToTarget();
				break;
			case END_GROUP_SELECT:
				mWaitingDialog.dismiss();
				mWaitingDialog = null;
				break;
			}
		}

	}

}
