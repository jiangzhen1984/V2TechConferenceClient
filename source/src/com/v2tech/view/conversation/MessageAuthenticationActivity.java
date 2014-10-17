package com.v2tech.view.conversation;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.db.DataBaseContext;
import com.v2tech.db.V2TechDBHelper;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.view.JNIService;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.contacts.ContactDetail;
import com.v2tech.view.contacts.ContactDetail2;
import com.v2tech.view.contacts.add.AddFriendHistroysHandler;
import com.v2tech.view.contacts.add.FriendManagementActivity;
import com.v2tech.view.group.CrowdApplicantDetailActivity;
import com.v2tech.vo.AddFriendHistorieNode;
import com.v2tech.vo.Crowd;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VMessageQualification.QualificationState;
import com.v2tech.vo.VMessageQualification.ReadState;
import com.v2tech.vo.VMessageQualificationApplicationCrowd;
import com.v2tech.vo.VMessageQualificationInvitationCrowd;

/**
 * FIXME should combine two types of message, use one adapter for two.
 * 
 * @author jiangzhen
 * 
 */
public class MessageAuthenticationActivity extends Activity {

	private final static int ACCEPT_INVITATION_DONE = 1;

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

	FriendMessageAdapter firendAdapter;
	private GroupMessageAdapter groupAdapter;
	FriendAuthenticationBroadcastReceiver friendAuthenticationBroadcastReceiver;
	private CrowdAuthenticationBroadcastReceiver mCrowdAuthenticationBroadcastReceiver;

	private boolean isFriendAuthentication = true;
	private boolean isGroupInDeleteMode = false;

	private CrowdGroupService crowdService;

	private Context mContext;

	private List<MessageAuthenticationData> messageAuthenticationDataList = new ArrayList<MessageAuthenticationActivity.MessageAuthenticationData>();
	private List<ListItemWrapper> mMessageList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.message_authentication);
		connectView();
		bindViewEnvent();
		crowdService = new CrowdGroupService();
		initReceiver();
	}

	@Override
	protected void onStart() {
		changeMessageAuthenticationListView();
		super.onStart();
	}

	private void connectView() {
		tvMessageBack = (TextView) findViewById(R.id.message_back);
		tvCompleteRight = (TextView) findViewById(R.id.tv_complete_right);
		rbFriendAuthentication = (RadioButton) findViewById(R.id.rb_friend_authentication);
		rbGroupAuthentication = (RadioButton) findViewById(R.id.rb_group_authentication);
		lvMessageAuthentication = (ListView) findViewById(R.id.message_authentication);
	}

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
				if (isGroupInDeleteMode) {
					isGroupInDeleteMode = false;
					tvCompleteRight.setVisibility(View.GONE);
					rbGroupAuthentication.setEnabled(true);
					rbFriendAuthentication.setEnabled(true);

					groupAdapter.notifyDataSetChanged();
				} else {
					firendAdapter.isDeleteMode = false;
					firendAdapter.notifyDataSetChanged();
					tvMessageBack.setVisibility(View.VISIBLE);
					tvCompleteRight.setVisibility(View.INVISIBLE);
				}
			}
		});
		rbFriendAuthentication
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean arg1) {
						if (arg1) {
							rbFriendAuthentication.setTextColor(Color.rgb(255,
									255, 255));
							rbGroupAuthentication.setTextColor(getResources()
									.getColor(R.color.button_text_color));
							isFriendAuthentication = true;
							changeMessageAuthenticationListView();
						}

					}
				});
		rbGroupAuthentication
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean arg1) {
						if (arg1) {
							rbGroupAuthentication.setTextColor(Color.rgb(255,
									255, 255));
							rbFriendAuthentication.setTextColor(getResources()
									.getColor(R.color.button_text_color));
							isFriendAuthentication = false;
							changeMessageAuthenticationListView();
						}
					}
				});

		lvMessageAuthentication
				.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						if (isFriendAuthentication) {
							MessageAuthenticationData data = messageAuthenticationDataList
									.get(position);
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
							VMessageQualification msg = (VMessageQualification) mMessageList
									.get(position).obj;
							if (msg.getType() == VMessageQualification.Type.CROWD_INVITATION) {
								VMessageQualificationInvitationCrowd imsg = (VMessageQualificationInvitationCrowd) msg;

								Crowd crowd = new Crowd(imsg.getCrowdGroup()
										.getmGId(), imsg.getCrowdGroup()
										.getOwnerUser(), imsg.getCrowdGroup()
										.getName(), imsg.getCrowdGroup()
										.getBrief());
								crowd.setAuth(imsg.getCrowdGroup()
										.getAuthType().intValue());
								Intent i = new Intent(
										JNIService.JNI_BROADCAST_CROWD_INVATITION);
								i.addCategory(JNIService.JNI_ACTIVITY_CATEGROY);
								i.putExtra("crowd", crowd);
								i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								mContext.startActivity(i);
							} else if (msg.getType() == VMessageQualification.Type.CROWD_APPLICATION) {
								Intent i = new Intent();
								i.setClass(MessageAuthenticationActivity.this,
										CrowdApplicantDetailActivity.class);
								i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								mContext.startActivity(i);

							}
						}
					}
				});

		lvMessageAuthentication
				.setOnItemLongClickListener(new OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> arg0,
							View view, int position, long arg3) {
						if (isFriendAuthentication) {
							if (firendAdapter.isDeleteMode == false) {
								firendAdapter.isDeleteMode = true;
								firendAdapter.notifyDataSetChanged();
								tvMessageBack.setVisibility(View.INVISIBLE);
								tvCompleteRight.setVisibility(View.VISIBLE);
							}
						} else {
							isGroupInDeleteMode = true;
							// Group item
							mMessageList.get(position).showLeftDeleteButton = true;
							tvCompleteRight.setVisibility(View.VISIBLE);
							groupAdapter.notifyDataSetChanged();
							rbGroupAuthentication.setEnabled(false);
							rbFriendAuthentication.setEnabled(false);
							return true;
						}
						return false;
					}
				});

	}

	/**
	 * FIXME optimze code
	 */
	private void changeMessageAuthenticationListView() {
		if (isFriendAuthentication) {
			if (firendAdapter == null) {
				firendAdapter = new FriendMessageAdapter(this,
						R.layout.message_authentication_listview_item,
						messageAuthenticationDataList);
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
				if(list != null){
					for (int i = 0; i < list.size(); i++) {
						VMessageQualification qualification = list.get(i);
						V2Log.e("MessageAuthenticationActivity loadGroupMessage --> load group message type :"
								+ qualification.getType().intValue()
								+ " id is : "
								+ qualification.getId());
						mMessageList.add(new ListItemWrapper(qualification));
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				lvMessageAuthentication.setAdapter(groupAdapter);
			}

		}.execute();
	}

	void initReceiver() {
		friendAuthenticationBroadcastReceiver = new FriendAuthenticationBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(JNIService.JNI_BROADCAST_FRIEND_AUTHENTICATION);
		intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
		registerReceiver(friendAuthenticationBroadcastReceiver, intentFilter);

		mCrowdAuthenticationBroadcastReceiver = new CrowdAuthenticationBroadcastReceiver();
		intentFilter = new IntentFilter();
		intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		intentFilter
				.addAction(JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE);
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
	protected void onDestroy() {
		unInitReceiver();
		crowdService.clearCalledBack();
		super.onDestroy();
	}

	private void loadFriendMessage() {
		new AsyncTask<Void, Void, Void>() {
			List<MessageAuthenticationData> tempMessageAuthenticationDataList = new ArrayList<MessageAuthenticationData>();

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

						MessageAuthenticationData tempData = new MessageAuthenticationData();
						tempData.remoteUserID = tempNode.remoteUserID;
						tempData.name = GlobalHolder.getInstance()
								.getUser(tempData.remoteUserID).getName();

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
					messageAuthenticationDataList.clear();
					messageAuthenticationDataList
							.addAll(tempMessageAuthenticationDataList);
					firendAdapter.notifyDataSetChanged();
				}
			}

		}.execute();

	}

	@Override
	public void onBackPressed() {
		if (firendAdapter.isDeleteMode) {
			firendAdapter.isDeleteMode = false;
			firendAdapter.notifyDataSetChanged();
			tvMessageBack.setVisibility(View.VISIBLE);
			tvCompleteRight.setVisibility(View.INVISIBLE);
		} else if (isGroupInDeleteMode && !isFriendAuthentication) {
			isGroupInDeleteMode = false;
			rbGroupAuthentication.setEnabled(true);
			rbFriendAuthentication.setEnabled(true);
			tvCompleteRight.setVisibility(View.INVISIBLE);
			this.groupAdapter.notifyDataSetChanged();
		} else {
			super.onBackPressed();
		}
	}

	class MessageAuthenticationData {
		public MessageAuthenticationData() {
		}

		Drawable dheadImage;
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
		// R.layout.message_authentication_listview_item
		// MessageAuthenticationData
		private int layoutId;
		private List<MessageAuthenticationData> list;
		private LayoutInflater layoutInflater;
		private Context context;
		public boolean isDeleteMode = false;

		public FriendMessageAdapter(Context context, int layoutId,
				List<MessageAuthenticationData> list) {
			layoutInflater = LayoutInflater.from(context);
			this.layoutId = layoutId;
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
			final MessageAuthenticationData data = list.get(arg0);

			// viewTag.ivHeadImage.setImageDrawable(data.dheadImage);
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
					} else {
						viewTag.bSuerDelete.setVisibility(View.VISIBLE);
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
			if (isDeleteMode) {
				viewTag.ibDelete.setVisibility(View.VISIBLE);
				viewTag.bAccess.setVisibility(View.GONE);
				viewTag.tvAccessOrNo.setVisibility(View.GONE);
			} else {
				viewTag.ibDelete.setVisibility(View.GONE);
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
				item.mContentTV.setText(vqic.getInvitationUser().getName()
						+ mContext.getText(R.string.crowd_invitation_content));
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
				item.mContentTV.setText(mContext
						.getText(R.string.crowd_applicant_content));

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
				item.mRes.setVisibility(View.GONE);
				item.mAcceptButton.setVisibility(View.GONE);
			} else {
				item.mDeleteButton.setVisibility(View.GONE);

				if (msg.getQualState() == VMessageQualification.QualificationState.WAITING) {
					item.mRes.setVisibility(View.GONE);
					item.mAcceptButton.setVisibility(View.VISIBLE);

				} else if (msg.getQualState() == VMessageQualification.QualificationState.ACCEPTED) {
					item.mRes.setVisibility(View.VISIBLE);
					item.mAcceptButton.setVisibility(View.GONE);
					item.mRes.setText(R.string.crowd_invitation_accepted);
				} else if (msg.getQualState() == VMessageQualification.QualificationState.REJECT) {
					item.mRes.setVisibility(View.VISIBLE);
					item.mAcceptButton.setVisibility(View.GONE);
					item.mRes.setText(R.string.crowd_invitation_rejected);
				}
			}
		}

		private OnClickListener mDeleteLeftButtonListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				ListItemWrapper wrapper = (ListItemWrapper) v.getTag();
				wrapper.showDeleteButton = true;
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
				AcceptedButtonTag tag = (AcceptedButtonTag) v.getTag();
				VMessageQualification msg = (VMessageQualification) tag.wrapper.obj;

				msg.setQualState(VMessageQualification.QualificationState.ACCEPTED);

				if (msg.getType() == VMessageQualification.Type.CROWD_INVITATION) {
					CrowdGroup cg = ((VMessageQualificationInvitationCrowd) msg)
							.getCrowdGroup();
					Crowd crowd = new Crowd(cg.getmGId(), cg.getOwnerUser(),
							cg.getName(), cg.getBrief());
					crowdService.acceptInvitation(crowd, new Registrant(
							mLocalHandler, ACCEPT_INVITATION_DONE, cg));

				} else if (msg.getType() == VMessageQualification.Type.CROWD_APPLICATION) {
					VMessageQualificationApplicationCrowd vqac = ((VMessageQualificationApplicationCrowd) msg);
					crowdService.acceptApplication(vqac.getCrowdGroup(),
							vqac.getApplicant(), null);
				}
				updateViewItem(tag.wrapper, tag.item);

				// Update message state to database;
				msg.setQualState(QualificationState.ACCEPTED);
				msg.setReadState(ReadState.READ);
				MessageBuilder.updateQualicationMessage(mContext, msg);
			}

		};

	}

	private void handleAcceptDone(CrowdGroup cg) {
		if (cg == null) {
			return;
		}
		VMessageQualification vq = MessageBuilder.queryQualMessageByCrowdId(
				mContext, GlobalHolder.getInstance().getCurrentUser(), cg);

		GlobalHolder.getInstance().addGroupToList(GroupType.CHATING.intValue(),
				cg);

		Intent i = new Intent();
		i.setAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
		i.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		i.putExtra("crowd", cg.getmGId());
		sendBroadcast(i);

		vq.setReadState(VMessageQualification.ReadState.READ);
		vq.setQualState(VMessageQualification.QualificationState.ACCEPTED);
		MessageBuilder.updateQualicationMessage(mContext, vq);

	}

	class CrowdAuthenticationBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent intent) {
			if (JNIService.JNI_BROADCAST_NEW_QUALIFICATION_MESSAGE
					.equals(intent.getAction())) {
				long msgId = intent.getLongExtra("msgId", 0);
				VMessageQualification msg = MessageBuilder
						.queryQualMessageById(mContext, msgId);
				if (msg != null) {
					mMessageList.add(0, new ListItemWrapper(msg));
					groupAdapter.notifyDataSetChanged();
				}
				// Cancel next broadcast
				this.abortBroadcast();
			}
		}
	}

	class FriendAuthenticationBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if (arg1.getAction().equals(
					JNIService.JNI_BROADCAST_FRIEND_AUTHENTICATION)) {
				loadFriendMessage();
			}
		}

	}

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ACCEPT_INVITATION_DONE:
				JNIResponse jni = (JNIResponse) msg.obj;
				handleAcceptDone((CrowdGroup) jni.callerObject);
				break;
			}
		}

	};

}
