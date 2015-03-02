package com.bizcom.vc.activity.contacts;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bizcom.request.V2ContactsRequest;
import com.bizcom.request.jni.GroupServiceJNIResponse;
import com.bizcom.request.util.MessageListener;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vo.ContactGroup;
import com.bizcom.vo.Group;
import com.bizcom.vo.NetworkStateCode;
import com.bizcom.vo.User;
import com.bizcom.vo.Group.GroupType;
import com.v2tech.R;

public class SelectJionGroupActivity extends Activity {

	private static final int UPDATE_USER_GROUP_DONE = 1;
	public static final int SELECT_GROUP_RESPONSE_CODE_DONE = 0;
	public static final int SELECT_GROUP_RESPONSE_CODE_CANCEL = 1;

	private Context mContext;
	private RadioGroup mGroupListLy;
	private View mRadioGroupLy;
	private V2ContactsRequest contactService = new V2ContactsRequest();

	private STATE state = STATE.NONE;
	private boolean changed;
	private long originGroupId;
	private long userId;
	private Toast mToast;
	// 值为"addFriend"时是从加好友跳转而来，其他值为更改分组跳转而来。
	private String from;
	private TextView mReturnButton;
	private LocalReceiver localReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		originGroupId = getIntent().getLongExtra("gid", 0);
		userId = getIntent().getLongExtra("uid", 0);
		from = getIntent().getStringExtra("from");
		mContext = this;
		setContentView(R.layout.activity_contacts_update_group);
		mGroupListLy = (RadioGroup) findViewById(R.id.contact_update_group_list);
		mRadioGroupLy = findViewById(R.id.contact_update_group_list_layout);
		mReturnButton = (TextView)findViewById(R.id.contact_update_return_button);
		mReturnButton.setOnClickListener(mReturnButtonListener);
		mRadioGroupLy.setOnTouchListener(mRadioGroupLyListener);
		if (from != null && from.equals("addFriend")) {
			((TextView) mReturnButton)
					.setText(R.string.contacts_update_group_add_friends);
		} else {
			((TextView) mReturnButton)
					.setText(R.string.contacts_update_group_back);
		}
		initReceiver();
		// build radio button first
		buildList();
		mGroupListLy.setOnCheckedChangeListener(mGroupChangedListener);
		overridePendingTransition(R.anim.left_in, R.anim.left_out);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		initRadioGroup();
	}
	
	private void initRadioGroup() {
		if (!GlobalHolder.getInstance().isServerConnected()) {
			for (int i = 0; i < mGroupListLy.getChildCount(); i++) {
				if (mGroupListLy.getChildAt(i) instanceof RadioButton) {
					mGroupListLy.getChildAt(i).setClickable(false);
				} 
			}		
		}
	}

	private void initReceiver() {
		localReceiver = new LocalReceiver(); 
		IntentFilter filter = new IntentFilter();
		filter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		this.registerReceiver(localReceiver, filter);
	}

	private void buildList() {
		
		List<Group> friendGroup = GlobalHolder.getInstance().getGroup(
				GroupType.CONTACT.intValue());
		for (int i = 0; i < friendGroup.size(); i++) {
			Group g = friendGroup.get(i);

			RadioButton rb = (RadioButton) LayoutInflater.from(mContext)
					.inflate(R.layout.common_radio_right, null);
			rb.setText(g.getName());
			rb.setTag(g);
			rb.setTextColor(mContext.getResources().getColor(
					R.color.activiy_contact_detail_item_color));
			int margin = (int) mContext.getResources().getDimension(
					R.dimen.contact_detail_2_item_margin_horizontal);
			rb.setPadding(margin, 0, margin, 0);

			rb.setId((int) g.getmGId());

			long id = rb.getId();

			LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			mGroupListLy.addView(rb, ll);

			LinearLayout line = new LinearLayout(mContext);
			line.setBackgroundColor(Color.rgb(206, 206, 206));
			mGroupListLy.addView(line, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, 1));
			if (g.getmGId() == originGroupId) {
				rb.toggle();
			}
		}

		if ((from != null) && from.equals("addFriend")) {
			int i1 = (int) getIntent().getLongExtra("groupID", -1);
			if (i1 != -1) {
				mGroupListLy.check(i1);
			}
		}
	}

	@Override
	public void finish() {
		if ((from != null) && from.equals("addFriend")) {
		} else {
			if (changed) {
				Intent i = new Intent(
						PublicIntent.BROADCAST_REQUEST_UPDATE_CONTACTS_GROUP);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				mContext.sendBroadcast(i);
			}
		}
		super.finish();
		overridePendingTransition(R.anim.right_in, R.anim.right_out);
	}
	
	

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(localReceiver);
		contactService.clearCalledBack();
	}

	@Override
	public void onBackPressed() {
		if ((from != null) && from.equals("addFriend")) {
			setResult(SELECT_GROUP_RESPONSE_CODE_CANCEL, null);
		}
		super.onBackPressed();
	}
	
	private OnClickListener mReturnButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();
		}
		
	};
	
	private OnTouchListener mRadioGroupLyListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (!GlobalHolder.getInstance().isServerConnected()) {
				Toast.makeText(mContext,
						R.string.error_discussion_no_network,
						Toast.LENGTH_SHORT).show();
			}
			return false;
		}

		
	};

	private RadioGroup.OnCheckedChangeListener mGroupChangedListener = new RadioGroup.OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(RadioGroup rg, int id) {
			if ((from != null) && from.equals("addFriend")) {
				if (!GlobalHolder.getInstance().isServerConnected()) {
					Toast.makeText(mContext, R.string.error_local_connect_to_server, Toast.LENGTH_SHORT).show();
				}
				else{
					RadioButton rb = (RadioButton) rg.findViewById(id);
					Group group = (Group) rb.getTag();
					Intent intent = new Intent();
					intent.putExtra("groupName", group.getName());
					intent.putExtra("groupID", group.getmGId());
					setResult(SELECT_GROUP_RESPONSE_CODE_DONE, intent);
				}
				finish();
			} else {
				synchronized (state) {
					if (!GlobalHolder.getInstance().isServerConnected()) 
						Toast.makeText(mContext, R.string.error_local_connect_to_server, Toast.LENGTH_SHORT).show();
						
					if (state == STATE.UPDATING) {
						if (mToast == null) {
							mToast = Toast
									.makeText(
											mContext,
											R.string.activiy_contact_update_group_error_msg_in_progess,
											Toast.LENGTH_SHORT);
						}
						mToast.cancel();
						mToast.show();
						return;
					}
					state = STATE.UPDATING;
				}
				Group srcGroup = GlobalHolder.getInstance().getGroupById(
						Group.GroupType.CONTACT.intValue(), originGroupId);
				// update group id to new group
				originGroupId = ((Group) rg.findViewById(id).getTag())
						.getmGId();
				Group desGroup = GlobalHolder.getInstance().getGroupById(
						Group.GroupType.CONTACT.intValue(), originGroupId);

				User user = GlobalHolder.getInstance()
						.getUser(userId);
				contactService.updateUserGroup((ContactGroup) desGroup,
						(ContactGroup) srcGroup, user, new MessageListener(mLocalHandler,
								UPDATE_USER_GROUP_DONE, new LocalObject(user, srcGroup, desGroup)));

			}
		}

	};
	
	class LocalReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION)) {
				NetworkStateCode code = (NetworkStateCode) intent.getExtras()
						.get("state");
				if (code == NetworkStateCode.CONNECTED_ERROR) {
					for (int i = 0; i < mGroupListLy.getChildCount(); i++) {
						if (mGroupListLy.getChildAt(i) instanceof RadioButton) {
							mGroupListLy.getChildAt(i).setClickable(false);
						} 
					}
				} else {
					for (int i = 0; i < mGroupListLy.getChildCount(); i++) {
						if (mGroupListLy.getChildAt(i) instanceof RadioButton) {
							mGroupListLy.getChildAt(i).setClickable(true);
						} 
					}
				}
			}
		}
	}

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if ((from != null) && from.equals("addFriend")) {
			} else {
				switch (msg.what) {
				case UPDATE_USER_GROUP_DONE:
					synchronized (state) {
						state = STATE.NONE;
					}
					LocalObject lo = (LocalObject) ((GroupServiceJNIResponse)msg.obj).callerObject;
					//Send broadcast for indicate contact group update
					Intent i = new Intent(PublicIntent.BROADCAST_CONTACT_GROUP_UPDATED_NOTIFICATION);
					i.addCategory(PublicIntent.DEFAULT_CATEGORY);
					i.putExtra("userId", lo.user.getmUserId());
					i.putExtra("srcGroupId", lo.src.getmGId());
					i.putExtra("destGroupId", lo.dest.getmGId());
					
					mContext.sendBroadcast(i);
					
					//Set result to parent
					Intent intent = new Intent();
					Group g = GlobalHolder.getInstance().getGroupById(GroupType.CONTACT.intValue(), lo.dest.getmGId());
					intent.putExtra("groupName", g.getName());
					intent.putExtra("groupID", lo.dest.getmGId());
					setResult(SELECT_GROUP_RESPONSE_CODE_DONE, intent);
					
					finish();
					break;
				}
			}
		}

	};
	
	class LocalObject {
		User user;
		Group src;
		Group dest;
		
		public LocalObject(User user, Group src, Group dest) {
			super();
			this.user = user;
			this.src = src;
			this.dest = dest;
		}
		
	}

	enum STATE {
		NONE, UPDATING
	}

}
