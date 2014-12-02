package com.v2tech.view.conversation;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.ind.GroupAddUserJNIObject;
import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.db.DataBaseContext;
import com.v2tech.db.V2TechDBHelper;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.BitmapManager.BitmapChangedListener;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.util.ProgressUtils;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.bo.ConversationNotificationObject;
import com.v2tech.view.contacts.ContactDetail;
import com.v2tech.view.contacts.ContactDetail2;
import com.v2tech.view.contacts.add.AddFriendHistroysHandler;
import com.v2tech.view.contacts.add.FriendManagementActivity;
import com.v2tech.view.group.CrowdApplicantDetailActivity;
import com.v2tech.vo.AddFriendHistorieNode;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.ConversationFirendAuthenticationData.VerificationMessageType;
import com.v2tech.vo.Crowd;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VMessageQualification.QualificationState;
import com.v2tech.vo.VMessageQualification.ReadState;
import com.v2tech.vo.VMessageQualification.Type;
import com.v2tech.vo.VMessageQualificationApplicationCrowd;
import com.v2tech.vo.VMessageQualificationInvitationCrowd;

/**
 * FIXME should combine two types of message, use one adapter for two.
 *
 * @author jiangzhen
 *
 */
public class MessageAuthenticationActivity extends Activity {

	private final static String TAG = "MessageAuthenticationActivity";
	private final static int ACCEPT_INVITATION_DONE = 1;
	private final static int ACCEPT_APPLY_DONE = 5;
	private final static int PROMPT_TYPE_FRIEND = 2;
	private final static int PROMPT_TYPE_GROUP = 3;
	private final static int AUTHENTICATION_RESULT = 4;

	public static final String tableName = "AddFriendHistories";
	// R.id.message_authentication
	private ListView lvMessageAuthentication;
	// R.id.message_back
	private TextView tvMessageBack;
	// R.id.tv_complete_right
	private TextView tvCompleteRight;
	// R.id.rb_friend_authentication
	private RadioButton rbFriendAuthentication;
	// R.id.rb_group_authentication
	private RadioButton rbGroupAuthentication;

	private ImageView ivFriendAuthenticationPrompt;
	private ImageView ivGroupAuthenticationPrompt;

	private FriendMessageAdapter firendAdapter;
	private GroupMessageAdapter groupAdapter;
	private List<FriendMAData> friendMADataList = new ArrayList<FriendMAData>();
	private List<ListItemWrapper> mMessageList;

	private FriendAuthenticationBroadcastReceiver friendAuthenticationBroadcastReceiver;
	private CrowdAuthenticationBroadcastReceiver mCrowdAuthenticationBroadcastReceiver;

	private boolean isFriendAuthentication;
	private boolean isFriendInDeleteMode = false;
	private boolean isGroupInDeleteMode = false;
	private int currentRadioType; // 当前所在的是哪个RadioButton界面
	private ListItemWrapper waitingQualification = null; // 当前所在的是哪个RadioButton界面
	private CrowdGroupService crowdService;
	private Resources res;

	private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.message_authentication);
		connectView();
		bindViewEnvent();
		crowdService = new CrowdGroupService();
		res = getResources();
		initReceiver();
		BitmapManager.getInstance().registerBitmapChangedListener(listener);

		currentRadioType = PROMPT_TYPE_FRIEND;

		isFriendAuthentication = getIntent().getBooleanExtra(
				"isFriendActivity", true);
		if (isFriendAuthentication) {
			MessageLoader.updateVerificationMessageReadState(VerificationMessageType.CONTACT_TYPE
					 , ReadState.READ , null , null);
			changeMessageAuthenticationListView();
		} else {
			MessageLoader.updateVerificationMessageReadState(VerificationMessageType.CROWD_TYPE
					 , ReadState.READ , null , null);
			rbGroupAuthentication.setChecked(true);
		}
        requestUpdateConversation();
		boolean showCrwodNotification = getIntent().getBooleanExtra(
				"isCrowdShowNotification", false);
		if (showCrwodNotification && isFriendAuthentication)
			updateTabPrompt(PROMPT_TYPE_GROUP, true);

		boolean showFriendNotification = getIntent().getBooleanExtra(
				"isFriendShowNotification", false);
		if (showFriendNotification && !isFriendAuthentication)
			updateTabPrompt(PROMPT_TYPE_FRIEND, true);

	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	private void connectView() {
		tvMessageBack = (TextView) findViewById(R.id.message_back);
		tvCompleteRight = (TextView) findViewById(R.id.tv_complete_right);
		rbFriendAuthentication = (RadioButton) findViewById(R.id.rb_friend_authentication);
		rbGroupAuthentication = (RadioButton) findViewById(R.id.rb_group_authentication);
		lvMessageAuthentication = (ListView) findViewById(R.id.message_authentication);
		ivFriendAuthenticationPrompt = (ImageView) findViewById(R.id.rb_friend_authentication_prompt);
		ivGroupAuthenticationPrompt = (ImageView) findViewById(R.id.rb_group_authentication_prompt);
	}

    public void requestUpdateConversation(){
        Intent i = new Intent(PublicIntent.REQUEST_UPDATE_CONVERSATION);
        i.addCategory(PublicIntent.DEFAULT_CATEGORY);
        i.putExtra("isFresh", false);
        mContext.sendBroadcast(i);
    }

	private BitmapChangedListener listener = new BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			if(user == null)
				return ;

			for (FriendMAData data : friendMADataList) {
				if(data.remoteUserID == user.getmUserId()){
					data.dheadImage.recycle();
					data.dheadImage = null;
					data.dheadImage = bm;
					if(firendAdapter != null)
						firendAdapter.notifyDataSetChanged();
				}
			}
		}
	};

	private void bindViewEnvent() {
		tvMessageBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onBackPressed();
			}
		});
		tvCompleteRight.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				exitDeleteMode();
			}
		});
		rbFriendAuthentication
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean arg1) {
						if (arg1) {

							currentRadioType = PROMPT_TYPE_FRIEND;
							rbFriendAuthentication.setTextColor(Color.rgb(255,
									255, 255));
							rbGroupAuthentication.setTextColor(getResources()
									.getColor(R.color.button_text_color));
							isFriendAuthentication = true;
							changeMessageAuthenticationListView();
							MessageLoader.updateVerificationMessageReadState(VerificationMessageType.CONTACT_TYPE
									 , ReadState.READ , null , null);
							updateTabPrompt(PROMPT_TYPE_FRIEND, false);
						}

					}
				});
		rbGroupAuthentication
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean arg1) {
						if (arg1) {
							currentRadioType = PROMPT_TYPE_GROUP;
							rbGroupAuthentication.setTextColor(Color.rgb(255,
									255, 255));
							rbFriendAuthentication.setTextColor(getResources()
									.getColor(R.color.button_text_color));
							isFriendAuthentication = false;
							changeMessageAuthenticationListView();
							updateTabPrompt(PROMPT_TYPE_GROUP, false);
							MessageLoader.updateVerificationMessageReadState(VerificationMessageType.CROWD_TYPE
									 , ReadState.READ , null , null);
						}
					}
				});

		lvMessageAuthentication
				.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						if (isFriendAuthentication) {
							FriendMAData data = friendMADataList.get(position);
							Intent intent = new Intent();
							intent.putExtra("remoteUserID", data.remoteUserID);
							intent.putExtra("authenticationMessage",
									data.authenticationMessage);
							intent.putExtra("state", data.state);
							intent.putExtra("fromActivity",
									"MessageAuthenticationActivity");
							// 别人加我：允许任何人：0已添加您为好友，需要验证：1未处理，2已同意，3已拒绝
							// 我加别人：允许认识人：4你们已成为了好友，需要验证：5等待对方验证，4被同意（你们已成为了好友），6拒绝了你为好友
							switch (data.state) {
							case 5:
								intent.setClass(
										MessageAuthenticationActivity.this,
										ContactDetail2.class);
								break;
							case 0:
							case 1:
							case 2:
							case 3:
							case 4:
							case 6:
								intent.setClass(
										MessageAuthenticationActivity.this,
										ContactDetail.class);
								break;
							}

							MessageAuthenticationActivity.this
									.startActivity(intent);
						} else {
							if (isGroupInDeleteMode) {
								Toast.makeText(
										mContext,
										R.string.crowd_message_deletion_mode_quit_required,
										Toast.LENGTH_SHORT).show();
								return;
							}
							VMessageQualification msg = (VMessageQualification) mMessageList
									.get(position).obj;
							if (msg.getType() == VMessageQualification.Type.CROWD_INVITATION) {
								startCrowdInvitationDetail(msg);
							} else if (msg.getType() == VMessageQualification.Type.CROWD_APPLICATION) {
								VMessageQualificationApplicationCrowd amsg = (VMessageQualificationApplicationCrowd) msg;
								Intent i = new Intent();
								i.setClass(MessageAuthenticationActivity.this,
										CrowdApplicantDetailActivity.class);
								i.putExtra("cid", amsg.getCrowdGroup()
										.getmGId());
								i.putExtra("aid", amsg.getApplicant()
										.getmUserId());
								i.putExtra("mid", amsg.getId());
								startActivityForResult(i, AUTHENTICATION_RESULT);
							}
						}
					}
				});

		lvMessageAuthentication
				.setOnItemLongClickListener(new OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> arg0,
							View view, int position, long arg3) {
						if (!isFriendInDeleteMode || !isGroupInDeleteMode) {
							enternDeleteMode(position);
							return true;
						}
						return false;
					}

				});

	}

	private void enternDeleteMode(int position) {
		if (isFriendAuthentication && !isFriendInDeleteMode) {
			isFriendInDeleteMode = true;
			firendAdapter.notifyDataSetChanged();
			tvMessageBack.setVisibility(View.INVISIBLE);
			tvCompleteRight.setVisibility(View.VISIBLE);
			rbGroupAuthentication.setEnabled(false);
			rbFriendAuthentication.setEnabled(false);
		}

		if (!isFriendAuthentication && !isGroupInDeleteMode) {
			isGroupInDeleteMode = true;
			mMessageList.get(position).showLeftDeleteButton = true;
			tvMessageBack.setVisibility(View.INVISIBLE);
			tvCompleteRight.setVisibility(View.VISIBLE);
			groupAdapter.notifyDataSetChanged();
			rbGroupAuthentication.setEnabled(false);
			rbFriendAuthentication.setEnabled(false);
		}
	}

	private void exitDeleteMode() {
		if (isFriendAuthentication && isFriendInDeleteMode) {
			isFriendInDeleteMode = false;
			firendAdapter.notifyDataSetChanged();
			tvMessageBack.setVisibility(View.VISIBLE);
			tvCompleteRight.setVisibility(View.INVISIBLE);
			rbGroupAuthentication.setEnabled(true);
			rbFriendAuthentication.setEnabled(true);
		}

		if (!isFriendAuthentication && isGroupInDeleteMode) {
			isGroupInDeleteMode = false;
			tvMessageBack.setVisibility(View.VISIBLE);
			tvCompleteRight.setVisibility(View.GONE);
			groupAdapter.notifyDataSetChanged();
			rbGroupAuthentication.setEnabled(true);
			rbFriendAuthentication.setEnabled(true);
		}

	}

	/**
	 * FIXME optimze code
	 */
	private void changeMessageAuthenticationListView() {
		if (isFriendAuthentication) {
			if (firendAdapter == null) {
				firendAdapter = new FriendMessageAdapter(this, friendMADataList);
			}
			lvMessageAuthentication.setAdapter(firendAdapter);
			loadFriendMessage();
		} else {

			mMessageList = new ArrayList<ListItemWrapper>();

			if (groupAdapter == null) {
				groupAdapter = new GroupMessageAdapter();
			}

			mMessageList.clear();
			loadGroupMessage();
		}
	}

	private void loadGroupMessage() {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				List<VMessageQualification> list = MessageBuilder
						.queryQualMessageList(mContext, GlobalHolder
								.getInstance().getCurrentUser());
				if (list == null) {
					return null;
				}

				for (int i = 0; i < list.size(); i++) {
					VMessageQualification qualification = list.get(i);
					mMessageList.add(new ListItemWrapper(qualification));
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				lvMessageAuthentication.setAdapter(groupAdapter);
			}

		}.execute();
	}

	private void updateTabPrompt(int type, boolean showPrompt) {

		switch (type) {
		case PROMPT_TYPE_FRIEND:
			if (showPrompt)
				ivFriendAuthenticationPrompt.setVisibility(View.VISIBLE);
			else
				ivFriendAuthenticationPrompt.setVisibility(View.INVISIBLE);
			break;
		case PROMPT_TYPE_GROUP:
			if (showPrompt)
				ivGroupAuthenticationPrompt.setVisibility(View.VISIBLE);
			else
				ivGroupAuthenticationPrompt.setVisibility(View.INVISIBLE);
			break;
		}
	}

	void initReceiver() {
		friendAuthenticationBroadcastReceiver = new FriendAuthenticationBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(JNIService.JNI_BROADCAST_CONTACTS_AUTHENTICATION);
		intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
		registerReceiver(friendAuthenticationBroadcastReceiver, intentFilter);

		mCrowdAuthenticationBroadcastReceiver = new CrowdAuthenticationBroadcastReceiver();
		intentFilter = new IntentFilter();
		intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		intentFilter
				.addAction(JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE);
		intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_JOIN_FAILED);
		intentFilter.addAction(JNIService.JNI_BROADCAST_KICED_CROWD);
		intentFilter.addAction(PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION);
		intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
		registerReceiver(mCrowdAuthenticationBroadcastReceiver, intentFilter);

	}

	void unInitReceiver() {
		if (friendAuthenticationBroadcastReceiver != null) {
			unregisterReceiver(friendAuthenticationBroadcastReceiver);
		}
		if (mCrowdAuthenticationBroadcastReceiver != null) {
			unregisterReceiver(mCrowdAuthenticationBroadcastReceiver);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (resultCode) {
		case AUTHENTICATION_RESULT:
			if (data != null) {
				long id = data.getLongExtra("qualificationID", -1l);
				QualificationState state = (QualificationState) data
						.getSerializableExtra("qualState");
				if (id != -1l) {
					boolean isFresh = false;
					for (ListItemWrapper wrapper : mMessageList) {
						VMessageQualification message = (VMessageQualification) wrapper.obj;
						if (message.getId() == id) {
                            if(state == QualificationState.INVALID){
                                mMessageList.remove(wrapper);
                                MessageBuilder.deleteQualMessage(mContext , message.getId());
                            }
                            else
							    message.setQualState(state);
							groupAdapter.notifyDataSetChanged();
							isFresh = true;
							break;
						}
					}

					if(!isFresh)
						V2Log.e(TAG , "Update QualificationState failed ... because no search it in mMessageList "
								+ "id is : " + id);
				}
				else
					V2Log.e(TAG , "Update QualificationState failed ... because id is -1");
			}
			break;
		}
	}

	@Override
	protected void onDestroy() {
		unInitReceiver();
		BitmapManager.getInstance().unRegisterBitmapChangedListener(listener);
		crowdService.clearCalledBack();
		checkVerificationMessage();
		super.onDestroy();
	}

	private void checkVerificationMessage() {
		Cursor cursor = null;
		try {
			VMessageQualification nestQualification = MessageLoader
					.getNewestCrowdVerificationMessage();
			String sql = "select * from " + AddFriendHistroysHandler.tableName
					+ " order by SaveDate desc limit 1";
			cursor = AddFriendHistroysHandler.select(mContext, sql,
					new String[] {});
			if ((cursor == null || !cursor.moveToNext())
					&& nestQualification == null) {
				notificateConversationUpdate();
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	/**
	 * 通知ConversationTabFragment 更新会话列表
	 */
	private void notificateConversationUpdate() {

		Intent i = new Intent(PublicIntent.REQUEST_UPDATE_CONVERSATION);
		i.addCategory(PublicIntent.DEFAULT_CATEGORY);
		ConversationNotificationObject obj = new ConversationNotificationObject(
				Conversation.TYPE_CONTACT, -2);
		i.putExtra("obj", obj);
		i.putExtra("isFresh", false);
		i.putExtra("isDelete", true);
		mContext.sendBroadcast(i);
	}

	private void loadFriendMessage() {
		new AsyncTask<Void, Void, Void>() {
			List<FriendMAData> tempMessageAuthenticationDataList = new ArrayList<FriendMAData>();

			@Override
			protected Void doInBackground(Void... arg0) {
				tempMessageAuthenticationDataList.clear();
				// 把所有的改为已读
				String sql = "update " + tableName
						+ " set ReadState=1 where ReadState=0";
				AddFriendHistroysHandler.update(getApplicationContext(), sql);

				sql = "select * from " + tableName + " order by SaveDate asc";
				Cursor cr = AddFriendHistroysHandler.select(
						getApplicationContext(), sql, new String[] {});
				if (cr.moveToLast()) {
					do {

						AddFriendHistorieNode tempNode = new AddFriendHistorieNode();
						// _id integer primary key AUTOINCREMENT,0
						// OwnerUserID bigint,1
						// SaveDate bigint,2
						// FromUserID bigint,3
						// OwnerAuthType bigint,4
						// ToUserID bigint, 5
						// RemoteUserID bigint, 6
						// ApplyReason nvarchar(4000),7
						// RefuseReason nvarchar(4000), 8
						// AddState bigint ,9
						// ReadState bigint);10
						tempNode.ownerUserID = cr.getLong(1);
						tempNode.saveDate = cr.getLong(2);
						tempNode.fromUserID = cr.getLong(3);
						tempNode.ownerAuthType = cr.getLong(4);
						tempNode.toUserID = cr.getLong(5);
						tempNode.remoteUserID = cr.getLong(6);
						tempNode.applyReason = cr.getString(7);
						tempNode.refuseReason = cr.getString(8);
						tempNode.addState = cr.getLong(9);
						tempNode.readState = cr.getLong(10);

						FriendMAData tempData = new FriendMAData();

						tempData.remoteUserID = tempNode.remoteUserID;
						User user = GlobalHolder.getInstance().getUser(
								tempData.remoteUserID);
						tempData.dheadImage = user.getAvatarBitmap();
						tempData.name = user.getName();

						tempData.dbRecordIndex = cr.getLong(0);
						// 别人加我：允许任何人：0已添加您为好友，需要验证：1未处理，2已同意，3已拒绝
						// 我加别人：允许认识人：4你们已成为了好友，需要验证：5等待对方验证，4被同意（你们已成为了好友），6拒绝了你为好友
						if ((tempNode.fromUserID == tempNode.remoteUserID)
								&& (tempNode.ownerAuthType == 0)) {// 别人加我允许任何人
							tempData.state = 0;
						} else if ((tempNode.fromUserID == tempNode.remoteUserID)
								&& (tempNode.ownerAuthType == 1)
								&& (tempNode.addState == 0)) {// 别人加我未处理
							tempData.state = 1;
							tempData.authenticationMessage = tempNode.applyReason;
						} else if ((tempNode.fromUserID == tempNode.remoteUserID)
								&& (tempNode.ownerAuthType == 1)
								&& (tempNode.addState == 1)) {// 别人加我已同意
							tempData.state = 2;
							tempData.authenticationMessage = tempNode.applyReason;
						} else if ((tempNode.fromUserID == tempNode.remoteUserID)
								&& (tempNode.ownerAuthType == 1)
								&& (tempNode.addState == 2)) {// 别人加我已拒绝
							tempData.state = 3;
							tempData.authenticationMessage = tempNode.refuseReason;
						} else if ((tempNode.fromUserID == tempNode.ownerUserID)
								&& (tempNode.addState == 0)) {// 我加别人等待验证
							tempData.state = 5;
						} else if ((tempNode.fromUserID == tempNode.ownerUserID)
								&& (tempNode.addState == 1)) {// 我加别人已被同意或我加别人不需验证
							tempData.state = 4;
						} else if ((tempNode.fromUserID == tempNode.ownerUserID)
								&& (tempNode.addState == 2)) {// 我加别人已被拒绝
							tempData.state = 6;
							tempData.authenticationMessage = tempNode.refuseReason;
						}
						tempMessageAuthenticationDataList.add(tempData);
					} while (cr.moveToPrevious());
				}
				cr.close();
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				if (firendAdapter != null) {
					friendMADataList.clear();
					friendMADataList.addAll(tempMessageAuthenticationDataList);
					firendAdapter.notifyDataSetChanged();
				}
			}

		}.execute();

	}

	private void startCrowdInvitationDetail(VMessageQualification msg) {
		VMessageQualificationInvitationCrowd imsg = (VMessageQualificationInvitationCrowd) msg;
		Intent i = new Intent();
		if (imsg.getQualState() == QualificationState.BE_ACCEPTED) {
			if (GlobalHolder.getInstance().getGroupById(
					V2GlobalEnum.GROUP_TYPE_CROWD,
					imsg.getCrowdGroup().getmGId()) != null) {
				i.putExtra("obj",
						new ConversationNotificationObject(
								Conversation.TYPE_GROUP, imsg.getCrowdGroup()
										.getmGId()));
				i.setAction(PublicIntent.START_CONVERSACTION_ACTIVITY);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				startActivity(i);
			} else {
				V2Log.e("startCrowdInvitationDetail --> start ConversationActivity failed ... crowd group isn't exist!");
			}
		} else {
            CrowdGroup group = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
                    V2GlobalEnum.GROUP_TYPE_CROWD,
                    imsg.getCrowdGroup().getmGId());
            Crowd crowd;
            if(group != null){
                crowd = new Crowd(group.getmGId(), group.getOwnerUser(), group.getName(),
                        group.getBrief());
                crowd.setAnnounce(group.getAnnouncement());
                crowd.setAuth(group.getAuthType().intValue());
            }
            else {
                CrowdGroup crowdGroup = imsg.getCrowdGroup();
                crowd = new Crowd(crowdGroup.getmGId(), crowdGroup.getOwnerUser(), crowdGroup
                        .getName(), crowdGroup.getBrief());
                crowd.setAnnounce(crowdGroup.getAnnouncement());
                crowd.setAuth(crowdGroup.getAuthType().intValue());
            }

			if (imsg.getQualState() == QualificationState.BE_REJECT) {
				i.setAction(PublicIntent.SHOW_CROWD_APPLICATION_ACTIVITY);
				crowd.setAuth(CrowdGroup.AuthType.QULIFICATION.intValue());
				i.putExtra("crowd", crowd);
				// set disable authentication
				i.putExtra("authdisable", false);
				i.putExtra("stored", true);
                startActivityForResult(i , AUTHENTICATION_RESULT);
			} else {
				i.setAction(JNIService.JNI_BROADCAST_CROWD_INVATITION);
				i.addCategory(JNIService.JNI_ACTIVITY_CATEGROY);
				i.putExtra("crowd", crowd);
				startActivityForResult(i, AUTHENTICATION_RESULT);
			}
		}
	}

	@Override
	public void onBackPressed() {
		if (isFriendInDeleteMode || isGroupInDeleteMode) {
			exitDeleteMode();
		} else {
			setResult(16);
			super.onBackPressed();
		}

	}

	class FriendMAData {
		public FriendMAData() {
		}

		Bitmap dheadImage;
		String name;
		String authenticationMessage;
		// 别人加我：允许任何人：0已添加您为好友，需要验证：1未处理，2已同意，3已拒绝
		// 我加别人：允许认识人：4成为好友，需要验证：5等待验证，4被同意（成为好友），6被拒绝
		int state;
		long remoteUserID;
		// 对应数据库中id
		long dbRecordIndex;
	}

	// 验证消息adapter
	class FriendMessageAdapter extends BaseAdapter {
		class ViewTag {
			// R.id.head_image
			ImageView ivHeadImage;
			// R.id.name
			TextView tvName;
			// R.id.authentication_message
			TextView tvAuthenticationMessage;
			// R.id.access
			Button bAccess;
			// R.id.access_or_no
			TextView tvAccessOrNo;
			// R.id.b_suer_delete
			Button bSuerDelete;
			// R.id.ib_delete
			ImageButton ibDelete;
		}

		private final int layoutId = R.layout.message_authentication_listview_item;
		private LayoutInflater layoutInflater;
		private List<FriendMAData> list;
		private Context context;

		public FriendMessageAdapter(Context context, List<FriendMAData> list) {
			layoutInflater = LayoutInflater.from(context);
			this.list = list;
			this.context = context;
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int arg0) {
			return list.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int arg0, View arg1, ViewGroup arg2) {
			final int position = arg0;
			final ViewTag viewTag;
			if (arg1 == null) {
				arg1 = layoutInflater.inflate(layoutId, null);
				viewTag = new ViewTag();
				viewTag.ivHeadImage = (ImageView) arg1
						.findViewById(R.id.head_image);
				viewTag.tvName = (TextView) arg1.findViewById(R.id.name);
				viewTag.tvAuthenticationMessage = (TextView) arg1
						.findViewById(R.id.authentication_message);
				viewTag.bAccess = (Button) arg1.findViewById(R.id.access);
				viewTag.tvAccessOrNo = (TextView) arg1
						.findViewById(R.id.access_or_no);
				viewTag.ibDelete = (ImageButton) arg1
						.findViewById(R.id.ib_delete);
				viewTag.bSuerDelete = (Button) arg1
						.findViewById(R.id.b_suer_delete);
				arg1.setTag(viewTag);
			} else {
				viewTag = (ViewTag) arg1.getTag();
			}

			if (arg0 >= list.size())
				return arg1;
			final FriendMAData data = list.get(arg0);
            if(data == null){
                V2Log.e(TAG , "Get FriendMAData Object is null ... please check!");
                return arg1;
            }

            boolean isGetAvatar = false;
            if (data.dheadImage != null) {
                if (!data.dheadImage.isRecycled())
                    viewTag.ivHeadImage.setImageBitmap(data.dheadImage);
                else
                    isGetAvatar = true;
            } else
                isGetAvatar = true;

            if(isGetAvatar == true) {
                User remoteUser = GlobalHolder.getInstance().getUser(data.remoteUserID);
                if (remoteUser != null)
                    viewTag.ivHeadImage.setImageBitmap(remoteUser.getAvatarBitmap());
                else
                    viewTag.ivHeadImage.setImageResource(R.drawable.avatar);
            }
			viewTag.tvName.setText(data.name);

			// 别人加我：允许任何人：0已添加您为好友，需要验证：1未处理，2已同意，3已拒绝
			// 我加别人：允许认识人：4你们已成为了好友，需要验证：5等待对方验证，4被同意（你们已成为了好友），6拒绝了你为好友
			switch (data.state) {
			case 0:
				viewTag.tvAuthenticationMessage.setText("已添加您为好友");
				viewTag.bAccess.setVisibility(View.GONE);
				viewTag.tvAccessOrNo.setVisibility(View.GONE);
				break;
			case 1:
				viewTag.tvAuthenticationMessage
						.setText(data.authenticationMessage);
				viewTag.bAccess.setVisibility(View.VISIBLE);
				viewTag.tvAccessOrNo.setVisibility(View.GONE);
				break;
			case 2:
				viewTag.tvAuthenticationMessage
						.setText(data.authenticationMessage);
				viewTag.bAccess.setVisibility(View.GONE);
				viewTag.tvAccessOrNo.setVisibility(View.VISIBLE);
				viewTag.tvAccessOrNo.setText("已同意");
				break;
			case 3:
				viewTag.tvAuthenticationMessage
						.setText(data.authenticationMessage);
				viewTag.bAccess.setVisibility(View.GONE);
				viewTag.tvAccessOrNo.setVisibility(View.VISIBLE);
				viewTag.tvAccessOrNo.setText("已拒绝");
				break;
			case 4:// 你们已成为了好友
				viewTag.tvAuthenticationMessage.setText("你们已成为了好友");
				viewTag.bAccess.setVisibility(View.GONE);
				viewTag.tvAccessOrNo.setVisibility(View.GONE);
				break;
			case 5:// 等待对方验证
				viewTag.tvAuthenticationMessage.setText("等待对方验证");
				viewTag.bAccess.setVisibility(View.GONE);
				viewTag.tvAccessOrNo.setVisibility(View.GONE);
				break;
			case 6:// 6拒绝了你为好友
				viewTag.tvAuthenticationMessage.setText("拒绝了你为好友");
				viewTag.bAccess.setVisibility(View.GONE);
				viewTag.tvAccessOrNo.setVisibility(View.GONE);
				break;
			default:
				viewTag.tvAuthenticationMessage.setText("你们已成为了好友");
				viewTag.bAccess.setVisibility(View.GONE);
				viewTag.tvAccessOrNo.setVisibility(View.GONE);
				break;

			}
			viewTag.bAccess.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {

					Intent intent = new Intent(context,
							FriendManagementActivity.class);
					intent.putExtra("remoteUserID", data.remoteUserID);
					intent.putExtra("cause", "access_friend_authentication");
					context.startActivity(intent);
				}
			});

			viewTag.ibDelete.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					if (viewTag.bSuerDelete.getVisibility() == View.VISIBLE) {
						viewTag.bSuerDelete.setVisibility(View.GONE);
						if (data.state == 1)
							viewTag.bAccess.setVisibility(View.VISIBLE);
						else
							viewTag.tvAccessOrNo.setVisibility(View.VISIBLE);
					} else {
						viewTag.bSuerDelete.setVisibility(View.VISIBLE);
						viewTag.bAccess.setVisibility(View.GONE);
						viewTag.tvAccessOrNo.setVisibility(View.GONE);
					}
				}
			});

			viewTag.bSuerDelete.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					// 库中删除
					String sql = "delete from " + tableName + " where _id="
							+ list.get(position).dbRecordIndex;
					V2TechDBHelper dbh = new V2TechDBHelper(
							new DataBaseContext(getApplicationContext()));
					SQLiteDatabase db = dbh.getWritableDatabase();
					db.execSQL(sql);
					// data中删除
					list.remove(position);
					FriendMessageAdapter.this.notifyDataSetChanged();
				}
			});

			viewTag.bSuerDelete.setVisibility(View.GONE);
			if (isFriendInDeleteMode) {
				viewTag.ibDelete.setVisibility(View.VISIBLE);
				// viewTag.bAccess.setVisibility(View.GONE);
				// viewTag.tvAccessOrNo.setVisibility(View.GONE);
				viewTag.bAccess.setEnabled(false);
			} else {
				viewTag.ibDelete.setVisibility(View.GONE);
				viewTag.bAccess.setEnabled(true);
			}

			return arg1;
		}
	}

	class ListItemWrapper {
		Object obj;
		boolean showLeftDeleteButton;
		boolean showDeleteButton;

		public ListItemWrapper(Object obj, boolean showLeftDeleteButton,
				boolean showDeleteButton) {
			super();
			this.obj = obj;
			this.showLeftDeleteButton = showLeftDeleteButton;
			this.showDeleteButton = showDeleteButton;
		}

		public ListItemWrapper(Object obj) {
			this(obj, false, false);
		}

	}

	class GroupMessageAdapter extends BaseAdapter {

		class ViewItem {
			View mDeleteHintButton;
			ImageView mMsgBanneriv;
			TextView mNameTV;
			TextView mContentTV;
			TextView mRejectContentTV;
			TextView mRes;
			View mAcceptButton;
			View mDeleteButton;
		}

		class AcceptedButtonTag {
			ViewItem item;
			ListItemWrapper wrapper;
		}

		private LayoutInflater layoutInflater;

		public GroupMessageAdapter() {
			layoutInflater = LayoutInflater.from(mContext);
		}

		@Override
		public int getCount() {
			return mMessageList.size();
		}

		@Override
		public Object getItem(int position) {
			return mMessageList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return ((VMessageQualification) mMessageList.get(position).obj)
					.getId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewItem item = null;
			if (convertView == null) {
				convertView = layoutInflater.inflate(
						R.layout.qualification_message_adapter_item, null);
				item = new ViewItem();
				item.mDeleteHintButton = convertView
						.findViewById(R.id.qualification_msg_delete_left_button);
				item.mRejectContentTV = (TextView) convertView
						.findViewById(R.id.qualification_msg_content_reject);
				item.mDeleteHintButton
						.setOnClickListener(mDeleteLeftButtonListener);

				item.mMsgBanneriv = (ImageView) convertView
						.findViewById(R.id.qualification_msg_image_view);
				item.mNameTV = (TextView) convertView
						.findViewById(R.id.qualification_msg_name);
				item.mContentTV = (TextView) convertView
						.findViewById(R.id.qualification_msg_content);
				item.mRes = (TextView) convertView
						.findViewById(R.id.qualification_msg_res);
				item.mAcceptButton = convertView
						.findViewById(R.id.qualification_msgconfirm_button);
				item.mAcceptButton.setOnClickListener(mAcceptButtonListener);
				item.mDeleteButton = convertView
						.findViewById(R.id.qualification_msg_delete_button);
				item.mDeleteButton.setOnClickListener(mDeleteButtonListener);
				convertView.setTag(item);
			} else {
				item = (ViewItem) convertView.getTag();
			}
			updateViewItem(mMessageList.get(position), item);
			convertView.invalidate();

			return convertView;
		}

		private void updateViewItem(ListItemWrapper wrapper, ViewItem item) {
			if (wrapper == null || wrapper.obj == null || item == null) {
				return;
			}
			AcceptedButtonTag tag = null;
			if (item.mAcceptButton.getTag() == null) {
				tag = new AcceptedButtonTag();
				item.mAcceptButton.setTag(tag);
			} else {
				tag = (AcceptedButtonTag) item.mAcceptButton.getTag();
			}
			VMessageQualification msg = (VMessageQualification) wrapper.obj;
			tag.item = item;
			tag.wrapper = wrapper;

			item.mDeleteButton.setTag(wrapper);
			item.mDeleteHintButton.setTag(wrapper);

			// to be invited
			if (msg.getType() == VMessageQualification.Type.CROWD_INVITATION) {
				VMessageQualificationInvitationCrowd vqic = (VMessageQualificationInvitationCrowd) msg;
				CrowdGroup cg = vqic.getCrowdGroup();
				item.mMsgBanneriv.setImageResource(R.drawable.chat_group_icon);
				item.mNameTV.setText(cg.getName());
				if (vqic.getInvitationUser() != null)
					item.mContentTV
							.setText(vqic.getInvitationUser().getName()
									+ mContext
											.getText(R.string.crowd_invitation_content));

				updateInviteMessageView(item, vqic);
				// If current user is invitor
			} else if (msg.getType() == VMessageQualification.Type.CROWD_APPLICATION) {
				VMessageQualificationApplicationCrowd vqac = (VMessageQualificationApplicationCrowd) msg;
				if (vqac.getApplicant().getAvatarBitmap() != null) {
					item.mMsgBanneriv.setImageBitmap(vqac.getApplicant()
							.getAvatarBitmap());
				} else {
					item.mMsgBanneriv.setImageResource(R.drawable.avatar);
				}
				item.mNameTV.setText(vqac.getApplicant().getName());

				updateApplyMessageView(item, vqac);
			}

			if (isGroupInDeleteMode) {
				item.mDeleteHintButton.setVisibility(View.VISIBLE);
			} else {
				item.mDeleteHintButton.setVisibility(View.GONE);
				wrapper.showDeleteButton = false;
			}

			// Show delete button and hide commoand button
			if (wrapper.showDeleteButton && isGroupInDeleteMode) {
				item.mDeleteButton.setVisibility(View.VISIBLE);
				item.mRes.setVisibility(View.INVISIBLE);
				item.mAcceptButton.setVisibility(View.INVISIBLE);
			} else {
				item.mDeleteButton.setVisibility(View.INVISIBLE);
				if (msg.getType() == VMessageQualification.Type.CROWD_APPLICATION)
					updateApplyMessageView(item,
							(VMessageQualificationApplicationCrowd) msg);
				else
					updateInviteMessageView(item,
							(VMessageQualificationInvitationCrowd) msg);
			}
		}

		private void updateApplyMessageView(ViewItem item,
				VMessageQualificationApplicationCrowd vqac) {
			if (vqac.getQualState() == QualificationState.ACCEPTED) {
				item.mRes.setVisibility(View.VISIBLE);
				item.mAcceptButton.setVisibility(View.INVISIBLE);
                item.mRejectContentTV.setVisibility(View.GONE);
				item.mRes.setText(R.string.crowd_invitation_accepted);
				item.mContentTV.setText(res
						.getString(R.string.crowd_invitation_apply_join)
						+ vqac.getCrowdGroup().getName()
						+ res.getString(R.string.crowd_Authentication_crowd));
			} else if (vqac.getQualState() == QualificationState.REJECT) {
				item.mRes.setVisibility(View.VISIBLE);
				item.mAcceptButton.setVisibility(View.INVISIBLE);
                item.mRejectContentTV.setVisibility(View.GONE);
				item.mRes.setText(R.string.crowd_invitation_rejected);
				item.mContentTV.setText(res
						.getString(R.string.crowd_invitation_apply_join)
						+ vqac.getCrowdGroup().getName()
						+ res.getString(R.string.crowd_Authentication_crowd));
			} else if (vqac.getQualState() == QualificationState.BE_REJECT) {
				item.mRes.setVisibility(View.INVISIBLE);
				item.mAcceptButton.setVisibility(View.INVISIBLE);
				item.mRejectContentTV.setVisibility(View.GONE);
				item.mRejectContentTV.setText(vqac.getApplyReason());
				item.mContentTV.setText(res
						.getString(R.string.crowd_invitation_reject_join)
						+ vqac.getCrowdGroup().getName()
						+ res.getString(R.string.crowd_Authentication_crowd));
			} else if (vqac.getQualState() == QualificationState.BE_ACCEPTED) {
				item.mRes.setVisibility(View.INVISIBLE);
				item.mAcceptButton.setVisibility(View.INVISIBLE);
                item.mRejectContentTV.setVisibility(View.GONE);
				item.mContentTV.setText(res
						.getString(R.string.crowd_invitation_accept_join)
						+ vqac.getCrowdGroup().getName()
						+ res.getString(R.string.crowd_Authentication_crowd));
			} else if (vqac.getQualState() == QualificationState.WAITING) {
				item.mRes.setVisibility(View.INVISIBLE);
                item.mRejectContentTV.setVisibility(View.GONE);
				item.mAcceptButton.setVisibility(View.VISIBLE);
				item.mContentTV.setText(res
						.getString(R.string.crowd_invitation_apply_join)
						+ vqac.getCrowdGroup().getName()
						+ res.getString(R.string.crowd_Authentication_crowd));
			}
		}

		private void updateInviteMessageView(ViewItem item,
				VMessageQualificationInvitationCrowd vqic) {
			if (vqic.getQualState() == QualificationState.ACCEPTED) {
				item.mRes.setVisibility(View.VISIBLE);
				item.mAcceptButton.setVisibility(View.INVISIBLE);
				 item.mRejectContentTV.setVisibility(View.GONE);
				item.mRes.setText(R.string.crowd_invitation_accepted);
			} else if (vqic.getQualState() == QualificationState.REJECT) {
				item.mRes.setVisibility(View.VISIBLE);
				item.mAcceptButton.setVisibility(View.INVISIBLE);
				item.mRejectContentTV.setVisibility(View.GONE);
				item.mRes.setText(R.string.crowd_invitation_rejected);
			} else if (vqic.getQualState() == QualificationState.BE_ACCEPTED) {
				item.mRes.setVisibility(View.INVISIBLE);
				item.mAcceptButton.setVisibility(View.INVISIBLE);
				 item.mRejectContentTV.setVisibility(View.GONE);
				item.mContentTV.setText(R.string.crowd_invitation_accepted_notes);
			} else if ((vqic.getQualState() == QualificationState.BE_REJECT)
					|| (vqic.getQualState() == QualificationState.WAITING_FOR_APPLY)) {
				item.mRes.setVisibility(View.INVISIBLE);
				item.mAcceptButton.setVisibility(View.INVISIBLE);
				item.mRejectContentTV.setVisibility(View.VISIBLE);
				item.mRejectContentTV.setText(vqic.getRejectReason());
				item.mContentTV.setText(R.string.crowd_invitation_rejected_notes);
			} else if (vqic.getQualState() == QualificationState.INVALID){
                item.mRes.setVisibility(View.VISIBLE);
                item.mAcceptButton.setVisibility(View.INVISIBLE);
                item.mRejectContentTV.setVisibility(View.GONE);
                item.mRes.setText(R.string.crowd_invitation_invalid_notes);
            }else {
				item.mRes.setVisibility(View.INVISIBLE);
				item.mRejectContentTV.setVisibility(View.GONE);
				item.mAcceptButton.setVisibility(View.VISIBLE);
			}
		}

		private OnClickListener mDeleteLeftButtonListener = new OnClickListener() {

			@Override
			public void onClick(View v) {

				ListItemWrapper wrapper = (ListItemWrapper) v.getTag();
				if (wrapper.showDeleteButton)
					wrapper.showDeleteButton = false;
				else
					wrapper.showDeleteButton = true;

				v.setTag(wrapper);
				groupAdapter.notifyDataSetChanged();
			}

		};

		private OnClickListener mDeleteButtonListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				ListItemWrapper wrapper = (ListItemWrapper) v.getTag();
				if (wrapper == null) {
					return;
				}
				// remove from list
				VMessageQualification msg = (VMessageQualification) wrapper.obj;
				for (int i = 0; i < mMessageList.size(); i++) {
					if (msg.getId() == ((VMessageQualification) mMessageList
							.get(i).obj).getId()) {
						mMessageList.remove(i);
						break;
					}
				}
				// delete message from database
				MessageBuilder.deleteQualMessage(mContext, msg.getId());

				groupAdapter.notifyDataSetChanged();
			}

		};

		private OnClickListener mAcceptButtonListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isGroupInDeleteMode) {
					Toast.makeText(mContext,
							R.string.crowd_message_deletion_mode_quit_required,
							Toast.LENGTH_SHORT).show();
					return;
				}
				AcceptedButtonTag tag = (AcceptedButtonTag) v.getTag();
				VMessageQualification msg = (VMessageQualification) tag.wrapper.obj;
				waitingQualification = tag.wrapper;
				// msg.setQualState(VMessageQualification.QualificationState.ACCEPTED);
				VMessageQualification message = MessageBuilder
						.queryQualMessageById(mContext, msg.getId());

				if (msg.getType() == VMessageQualification.Type.CROWD_INVITATION) {
					CrowdGroup cg = ((VMessageQualificationInvitationCrowd) msg)
							.getCrowdGroup();
					if (message.getQualState().intValue() != msg.getQualState()
							.intValue()) {
						Intent intent = new Intent();
						intent.setAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
						intent.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
						intent.putExtra("crowd", cg.getmGId());
						sendBroadcast(intent);
						updateViewItem(tag.wrapper, tag.item);
						msg.setReadState(ReadState.READ);
						MessageBuilder.updateQualicationMessage(mContext, msg);
//						startCrowdInvitationDetail(msg);
					} else {
						// Add crowd group to cache, we can't handle this after
						// done
						// because group information come before done event.
						GlobalHolder.getInstance().addGroupToList(
								GroupType.CHATING.intValue(), cg);

						Crowd crowd = new Crowd(cg.getmGId(),
								cg.getOwnerUser(), cg.getName(), cg.getBrief());
						crowdService.acceptInvitation(crowd,
								new MessageListener(mLocalHandler,
										ACCEPT_INVITATION_DONE, cg));
						ProgressUtils
								.showNormalWithHintProgress(mContext, true)
								.initTimeOut();
					}

				} else if (msg.getType() == VMessageQualification.Type.CROWD_APPLICATION) {
					VMessageQualificationApplicationCrowd vqac = ((VMessageQualificationApplicationCrowd) msg);
					Group group = GlobalHolder.getInstance().getGroupById(
							vqac.getCrowdGroup().getGroupType().intValue(),
							vqac.getCrowdGroup().getmGId());
					if (group != null) {
						if (message.getQualState().intValue() != msg
								.getQualState().intValue()) {
							msg.setQualState(QualificationState.ACCEPTED);
							msg.setReadState(ReadState.READ);
							updateViewItem(tag.wrapper, tag.item);
							MessageBuilder.updateQualicationMessage(mContext,
									msg);
						} else {
							crowdService.acceptApplication(
									vqac.getCrowdGroup(), vqac.getApplicant(),
									new MessageListener(mLocalHandler,
											ACCEPT_APPLY_DONE, group));
							ProgressUtils.showNormalWithHintProgress(mContext,
									true).initTimeOut();
						}
					} else {
						handleFailedDone();
						return;
					}
				}
			}

		};

	}

	private void handleAcceptDone(Type type, long groupID, long userID) {
		if (type == null) {
			return;
		}

		VMessageQualification currentMessage = null;
		VMessageQualification vq = MessageBuilder.queryQualMessageByCrowdId(
				mContext, userID, groupID);
		for (ListItemWrapper wrapper : mMessageList) {
			VMessageQualification message = (VMessageQualification) wrapper.obj;
			if (vq.getId() == message.getId()) {
				currentMessage = message;
				message.setQualState(QualificationState.ACCEPTED);
				message.setReadState(ReadState.READ);
				groupAdapter.notifyDataSetChanged();
				break;
			}
		}

//		if (type == Type.CROWD_INVITATION) {
//			Intent i = new Intent();
//			i.setAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
//			i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
//			i.putExtra("crowd", groupID);
//			sendBroadcast(i);
//			startCrowdInvitationDetail(currentMessage);
//		}

	}

	private void handleFailedDone() {
		if (waitingQualification != null) {
			VMessageQualificationInvitationCrowd vm = (VMessageQualificationInvitationCrowd) waitingQualification.obj;
			Toast.makeText(mContext, vm.getCrowdGroup().getName() + res.getString(R.string.crowd_Authentication_hit) ,
					Toast.LENGTH_SHORT).show();
//            vm.setQualState(QualificationState.INVALID);
//            MessageBuilder.updateQualicationMessage(mContext , vm);
            mMessageList.remove(waitingQualification);
            waitingQualification = null;
            MessageBuilder.deleteQualMessage(mContext, vm.getId());
            groupAdapter.notifyDataSetChanged();
        }
	}

	class CrowdAuthenticationBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent intent) {
			if (JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE
					.equals(intent.getAction())) {
				long msgId = intent.getLongExtra("msgId", 0);
				VMessageQualification msg = MessageBuilder
						.queryQualMessageById(mContext, msgId);
				boolean isAdd = true;
				ListItemWrapper removedWrapper = null;
				if (msg != null && mMessageList != null) {

					for (ListItemWrapper wrapper : mMessageList) {
						VMessageQualification message = (VMessageQualification) wrapper.obj;
						if (message.getId() == msg.getId()) {
							removedWrapper = wrapper;
							isAdd = false;
							break;
						}
					}

					if (!isAdd)
						mMessageList.remove(removedWrapper);
					mMessageList.add(0, new ListItemWrapper(msg));

					groupAdapter.notifyDataSetChanged();
					// 当用户在当前界面时，就不显示红点了
					if (currentRadioType != PROMPT_TYPE_GROUP)
						updateTabPrompt(PROMPT_TYPE_GROUP, true);

					msg.setReadState(ReadState.READ);
					MessageBuilder.updateQualicationMessage(mContext, msg);
				}
				// Cancel next broadcast
				this.abortBroadcast();
			}
			else if (JNIService.JNI_BROADCAST_GROUP_JOIN_FAILED.equals(intent
					.getAction())) {
				ProgressUtils.showNormalWithHintProgress(mContext, false);
				handleFailedDone();
			} else if (PublicIntent.BROADCAST_CROWD_DELETED_NOTIFICATION
                    .equals(intent.getAction())
                    || intent.getAction().equals(
                    JNIService.JNI_BROADCAST_KICED_CROWD)) {
                long cid = intent.getLongExtra("crowd", -1l);
                if (cid == -1l) {
                    V2Log.e(TAG,
                            "Received the broadcast to quit the crowd group , but crowd id is wroing... ");
                    return;
                }
				
                int location = -1;
				for (int i = 0 ; i < mMessageList.size() ; i++) {
					VMessageQualification message = (VMessageQualification) mMessageList.get(i).obj;
					if(message.getType() == Type.CROWD_APPLICATION){
						VMessageQualificationApplicationCrowd apply = (VMessageQualificationApplicationCrowd) message;
						if(apply.getCrowdGroup() != null){
							if(apply.getCrowdGroup().getmGId() == cid){
								location = i;
								break;
							}
						}
						else
							V2Log.e(TAG, "Remove the crowd qualification message failed! get crowd group is null"
									+ "crowd id is " + cid + " and message id is : " + apply.getId());
					}
					else{
						VMessageQualificationInvitationCrowd invite = (VMessageQualificationInvitationCrowd) message;
						if(invite.getCrowdGroup() != null){
							if(invite.getCrowdGroup().getmGId() == cid){
								location = i;
								break;
							}
						}
						else
							V2Log.e(TAG, "Remove the crowd qualification message failed! get crowd group is null"
									+ "crowd id is " + cid + " and message id is : " + invite.getId());
					}
				}
				
				if(location != -1){
					mMessageList.remove(location);
					groupAdapter.notifyDataSetChanged();
					V2Log.e(TAG, "Remove the crowd qualification message successful! crowd id is : " + cid);
				}
//				if(!isFriendAuthentication)
//					changeMessageAuthenticationListView();
//				rbGroupAuthentication.setChecked(true);
            }
		}
	}

	class FriendAuthenticationBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if (arg1.getAction().equals(
					JNIService.JNI_BROADCAST_CONTACTS_AUTHENTICATION)) {
				loadFriendMessage();
				if (currentRadioType != PROMPT_TYPE_FRIEND)
					updateTabPrompt(PROMPT_TYPE_FRIEND, true);
			}
			// Cancel next broadcast
			this.abortBroadcast();
		}

	}

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ACCEPT_INVITATION_DONE:
				JNIResponse jni = (JNIResponse) msg.obj;
				if (jni.getResult().ordinal() == JNIResponse.Result.SUCCESS
						.ordinal()) {
					CrowdGroup crowd = (CrowdGroup) jni.callerObject;
					handleAcceptDone(Type.CROWD_INVITATION, crowd.getmGId(),
							crowd.getOwnerUser().getmUserId());
				}
				break;
			case ACCEPT_APPLY_DONE:
				JNIResponse applyJni = (JNIResponse) msg.obj;
				if (applyJni.getResult().ordinal() == JNIResponse.Result.SUCCESS
						.ordinal()) {
					GroupAddUserJNIObject obj = (GroupAddUserJNIObject) applyJni.resObj;
					handleAcceptDone(Type.CROWD_APPLICATION, obj.getGroupID(),
							obj.getUserID());
				}
				break;
			}
			ProgressUtils.showNormalWithHintProgress(mContext, false);
		}

	};
}
