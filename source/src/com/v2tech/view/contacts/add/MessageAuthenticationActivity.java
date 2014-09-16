package com.v2tech.view.contacts.add;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.view.contacts.ContactDetail;
import com.v2tech.view.contacts.ContactDetail2;
import com.v2tech.vo.AddFriendHistorieNode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

public class MessageAuthenticationActivity extends Activity {
	public static final String tableName = "AddFriendHistories";
	// R.id.message_authentication
	private ListView lvMessageAuthentication;
	// R.id.message_back
	private TextView tvMessageBack;
	// R.id.rb_friend_authentication
	private RadioButton rbFriendAuthentication;
	// R.id.rb_group_authentication
	private RadioButton rbGroupAuthentication;

	private List<MessageAuthenticationData> messageAuthenticationDataList = new ArrayList<MessageAuthenticationActivity.MessageAuthenticationData>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message_authentication);
		tvMessageBack = (TextView) findViewById(R.id.message_back);
		tvMessageBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onBackPressed();
			}
		});

		rbFriendAuthentication = (RadioButton) findViewById(R.id.rb_friend_authentication);
		rbFriendAuthentication
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean arg1) {
						if (arg1) {
							rbFriendAuthentication.setTextColor(Color.rgb(255,
									255, 255));
						} else {
							rbFriendAuthentication.setTextColor(getResources()
									.getColor(R.color.button_text_color));
						}

					}
				});

		rbGroupAuthentication = (RadioButton) findViewById(R.id.rb_group_authentication);
		rbGroupAuthentication
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean arg1) {
						if (arg1) {
							rbGroupAuthentication.setTextColor(Color.rgb(255,
									255, 255));
						} else {
							rbGroupAuthentication.setTextColor(getResources()
									.getColor(R.color.button_text_color));
						}

					}
				});

		// 获得listview
		lvMessageAuthentication = (ListView) findViewById(R.id.message_authentication);
		// 生成数据
		// 查数据库生成数据

		Iterator<AddFriendHistorieNode> iterator = GlobalHolder.getInstance().addFriendHistorieList
				.iterator();

		// String sql="select * from "+tableName;
		// Cursor cr=AddFriendHistroysHandler.select(getApplicationContext(),
		// sql, new String[]{});

		while (iterator.hasNext()) {
			AddFriendHistorieNode tempNode = iterator.next();
			// // _id integer primary key AUTOINCREMENT,0
			// // OwnerUserID bigint,1
			// // SaveDate bigint,2
			// // FromUserID bigint,3
			// // OwnerAuthType bigint,4
			// // ToUserID bigint, 5
			// // RemoteUserID bigint, 6
			// // ApplyReason nvarchar(4000),7
			// // RefuseReason nvarchar(4000), 8
			// // AddState bigint ,9
			// // ReadState bigint);10
			// tempNode.ownerUserID=cr.getLong(1);
			// tempNode.saveDate=cr.getLong(2);
			// tempNode.fromUserID=cr.getLong(3);
			// tempNode.ownerAuthType=cr.getLong(4);
			// tempNode.toUserID=cr.getLong(5);
			// tempNode.remoteUserID=cr.getLong(6);
			// tempNode.applyReason=cr.getString(7);
			// tempNode.refuseReason=cr.getString(8);
			// tempNode.addState=cr.getLong(9);
			// tempNode.readState=cr.getLong(10);

			MessageAuthenticationData tempData = new MessageAuthenticationData();
			tempData.remoteUserID = tempNode.remoteUserID;
			tempData.name = GlobalHolder.getInstance()
					.getUser(tempData.remoteUserID).getName();

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
			messageAuthenticationDataList.add(tempData);
		}

		// 建立adapter
		MessageAuthenticationListViewAdapter adapter = new MessageAuthenticationListViewAdapter(
				this, R.layout.message_authentication_listview_item,
				messageAuthenticationDataList);
		// 设置adapter
		lvMessageAuthentication.setAdapter(adapter);

		lvMessageAuthentication
				.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
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
							intent.setClass(MessageAuthenticationActivity.this,
									ContactDetail2.class);
							break;
						case 0:
						case 1:
						case 2:
						case 3:
						case 4:
						case 6:
							intent.setClass(MessageAuthenticationActivity.this,
									ContactDetail.class);
							break;
						}

						MessageAuthenticationActivity.this
								.startActivity(intent);
					}
				});

	}

	class MessageAuthenticationData {
		public MessageAuthenticationData() {
		}

		public MessageAuthenticationData(Drawable dheadImage, String name,
				String authenticationMessage, int state) {
			this.dheadImage = dheadImage;
			this.name = name;
			this.authenticationMessage = authenticationMessage;
			this.state = state;
		}

		Drawable dheadImage;
		String name;
		String authenticationMessage;
		// 别人加我：允许任何人：0已添加您为好友，需要验证：1未处理，2已同意，3已拒绝
		// 我加别人：允许认识人：4成为好友，需要验证：5等待验证，4被同意（成为好友），6被拒绝
		int state;// 允许时候人时，我加被人：0成为好友。需要验证时，被邀请，1未处理，2已同意，3已拒绝。邀请人，4等待验证，5被同意(成为好友)，6被拒绝。允许时候人时：7别人加我(已添加您为好友)
		// -----------------
		long remoteUserID;
	}

	// 验证消息adapter
	class MessageAuthenticationListViewAdapter extends BaseAdapter {
		// R.layout.message_authentication_listview_item
		// MessageAuthenticationData
		private int layoutId;
		private List<MessageAuthenticationData> list;
		private LayoutInflater layoutInflater;
		private Context context;

		public MessageAuthenticationListViewAdapter(Context context,
				int layoutId, List<MessageAuthenticationData> list) {
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
		}

		@Override
		public View getView(int arg0, View arg1, ViewGroup arg2) {
			ViewTag viewTag;
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
				arg1.setTag(viewTag);
			} else {
				viewTag = (ViewTag) arg1.getTag();
			}

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

			return arg1;
		}

	}

}
