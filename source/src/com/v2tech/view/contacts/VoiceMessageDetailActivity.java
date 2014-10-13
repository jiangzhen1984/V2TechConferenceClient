package com.v2tech.view.contacts;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.DataBaseContext;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.DateUtil;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.bo.ConversationNotificationObject;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.view.conversation.P2PConversation;
import com.v2tech.vo.AudioVideoMessageBean;
import com.v2tech.vo.AudioVideoMessageBean.ChildMessageBean;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.User;
import com.v2tech.vo.UserDeviceConfig;
import com.v2tech.vo.VideoBean;

public class VoiceMessageDetailActivity extends Activity implements
		OnClickListener {

	public static final String TAG = "VoiceMessageDetailActivity";
	private LinearLayout videoCall;
	private LinearLayout vioceCall;
	private TextView clearRecord;
	private TextView returnBack;
	private TextView userName;
	private TextView userRemark;
	private ImageView userIcon;
	private ListView mListView;
	private ArrayList<ChildMessageBean> mListItem;
	private ChildMessageBean childBean;
	private VoiceDetailBaseAdapter adapter;
	private long remoteID;
	private VoiceDetailReceiverBroadcast receiver;
	private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_specifitem_voicedetail);
		mContext = this;
		findview();
		initReceiver();
		init();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	private void findview() {
		videoCall = (LinearLayout) findViewById(R.id.specific_voiceDetail_video_call_bottom_button);
		vioceCall = (LinearLayout) findViewById(R.id.specific_voiceDetail_vioce_call_bottom_button);
		mListView = (ListView) findViewById(R.id.specific_voiceDetail_listview);
		clearRecord = (TextView) findViewById(R.id.specific_voiceDetail_clearAll);
		returnBack = (TextView) findViewById(R.id.specific_title_return);
		userName = (TextView) findViewById(R.id.specific_voiceDetail_name);
		userRemark = (TextView) findViewById(R.id.specific_voiceDetail_remark);
		userIcon = (ImageView) findViewById(R.id.specific_voice_headIcon);

		returnBack.setOnClickListener(this);
		clearRecord.setOnClickListener(this);
		videoCall.setOnClickListener(this);
		vioceCall.setOnClickListener(this);
	}

	private void initReceiver() {
		receiver = new VoiceDetailReceiverBroadcast();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(P2PConversation.P2P_BROADCAST_MEDIA_UPDATE);
		intentFilter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		registerReceiver(receiver, intentFilter);
	}

	private void init() {

		remoteID = getIntent().getLongExtra("remoteUserID", -1l);
		if (remoteID == -1l)
			Toast.makeText(getApplicationContext(), "获取用户信息失败", Toast.LENGTH_SHORT).show();

		BitmapManager.getInstance().registerBitmapChangedListener(
				this.bitmapChangedListener);
		
		User user = GlobalHolder.getInstance().getUser(remoteID);
		userName.setText(user.getName());
		userRemark.setText(user.getSignature());
		User remoteUser = GlobalHolder.getInstance().getUser(remoteID);
		if (remoteUser.getAvatarBitmap() != null) {
			userIcon.setImageBitmap(remoteUser.getAvatarBitmap());
		}

		mListItem = getIntent().getParcelableArrayListExtra("messages");
		adapter = new VoiceDetailBaseAdapter();
		mListView.setAdapter(adapter);
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		Intent intent = new Intent(mContext,
				VoiceMessageActivity.class);
		startActivity(intent);
		finish();
	}

	class VoiceDetailBaseAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mListItem.size();
		}

		@Override
		public Object getItem(int position) {
			return mListItem.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder holder = null;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = View.inflate(VoiceMessageDetailActivity.this,
						R.layout.activity_specifitem_voicedetail_adapter, null);
				holder.directionIcon = (ImageView) convertView
						.findViewById(R.id.specific_voiceDetail_adapter_direction);
				holder.state = (TextView) convertView
						.findViewById(R.id.specific_voiceDetail_adapter_state);
				holder.holdTime = (TextView) convertView
						.findViewById(R.id.specific_voiceDetail_adapter_holdTime);
				holder.saveTime = (TextView) convertView
						.findViewById(R.id.specific_voiceDetail_saveTime);
				convertView.setTag(holder);
			} else
				holder = (ViewHolder) convertView.getTag();

			childBean = mListItem.get(position);
			long time = childBean.childHoldingTime;
			String holdTime = time <= 0 ? "" : DateUtil.getDates(time);
			holder.holdTime.setText(holdTime);
			holder.saveTime.setText(DateUtil
					.getStringDate(childBean.childSaveDate));
			if (childBean.childISCallOut == AudioVideoMessageBean.STATE_CALL_OUT) {
				holder.directionIcon
						.setImageResource(R.drawable.vs_voice_callout);
				holder.state.setTextColor(Color.BLUE);
				holder.holdTime.setTextColor(Color.BLUE);
				if (childBean.childMediaType == AudioVideoMessageBean.TYPE_AUDIO)
					holder.state.setText("[音频]已拨出");
				else
					holder.state.setText("[视频]已拨出");
			} else {
				if (childBean.childMediaState == AudioVideoMessageBean.STATE_CALL_OUT) {
					holder.directionIcon
							.setImageResource(R.drawable.vs_voice_listener);
					holder.state.setTextColor(Color.GREEN);
					holder.holdTime.setTextColor(Color.GREEN);
					if (childBean.childMediaType == AudioVideoMessageBean.TYPE_AUDIO)
						holder.state.setText("[音频]已接听");
					else
						holder.state.setText("[视频]已接听");
				} else {
					holder.directionIcon
							.setImageResource(R.drawable.vs_voice_nolistener);
					holder.state.setTextColor(Color.RED);
					holder.holdTime.setTextColor(Color.RED);
					if (childBean.childMediaType == AudioVideoMessageBean.TYPE_AUDIO)
						holder.state.setText("[音频]未接听");
					else
						holder.state.setText("[视频]未接听");
				}
			}
			return convertView;
		}
	}

	class ViewHolder {

		public TextView state;
		public TextView holdTime;
		public TextView saveTime;
		public ImageView directionIcon;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.specific_title_return:
			Intent intent = new Intent(this, VoiceMessageActivity.class);
			startActivity(intent);
			finish();
			mListItem.clear();
			break;
		case R.id.specific_voiceDetail_clearAll:
			mListItem.clear();
			adapter.notifyDataSetChanged();
			Uri url = ContentDescriptor.HistoriesMedia.CONTENT_URI;
			String where = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_REMOTE_USER_ID
					+ "= ?";
			String[] selectionArgs = new String[] { String.valueOf(remoteID) };
			int result = getContentResolver().delete(url, where, selectionArgs);
			Toast.makeText(getApplicationContext(), "本次共删除" + result + "条记录", Toast.LENGTH_SHORT)
					.show();
			break;
		case R.id.specific_voiceDetail_video_call_bottom_button:
			Intent iv = new Intent();
			iv.addCategory(PublicIntent.DEFAULT_CATEGORY);
			iv.setAction(PublicIntent.START_P2P_CONVERSACTION_ACTIVITY);
			iv.putExtra("uid", remoteID);
			iv.putExtra("is_coming_call", false);
			iv.putExtra("voice", false);
			List<UserDeviceConfig> list = GlobalHolder.getInstance()
					.getAttendeeDevice(remoteID);
			if (list != null && list.size() > 0) {
				iv.putExtra("device", list.get(0).getDeviceID());
			} else {
				iv.putExtra("device", "");
			}
			startActivity(iv);
			break;
		case R.id.specific_voiceDetail_vioce_call_bottom_button:
			Intent voice = new Intent();
			voice.addCategory(PublicIntent.DEFAULT_CATEGORY);
			voice.setAction(PublicIntent.START_P2P_CONVERSACTION_ACTIVITY);
			voice.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			voice.putExtra("uid", remoteID);
			voice.putExtra("is_coming_call", false);
			voice.putExtra("voice", true);
			startActivity(voice);
			break;
		}
	}

	class VoiceDetailReceiverBroadcast extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			long receiveRemoteID = intent.getLongExtra("remoteID", -1l);
			if (remoteID == -1l) {
				Log.e(TAG, "get remoteID is -1 ... update failed!!");
				return;
			}
			
			if(remoteID != receiveRemoteID)
				return;

			String selections = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_REMOTE_USER_ID
					+ "= ? ";
			String[] selectionArgs = new String[] { String.valueOf(remoteID) };
			VideoBean newestMediaMessage = MessageLoader.getNewestMediaMessage(
					VoiceMessageDetailActivity.this, selections, selectionArgs);
			if (newestMediaMessage == null) {
				Log.e(TAG, "get newest remoteID " + remoteID
						+ " --> VideoBean is NULL ... update failed!!");
				return;
			}
			ChildMessageBean newChild = new ChildMessageBean();
			newChild.childMediaType = newestMediaMessage.mediaType;
			newChild.childReadState = newestMediaMessage.readSatate;
			newChild.childMediaState = newestMediaMessage.mediaState;
			newChild.childHoldingTime = newestMediaMessage.endDate
					- newestMediaMessage.startDate;
			newChild.childSaveDate = newestMediaMessage.startDate;
			if (newestMediaMessage.formUserID != GlobalHolder.getInstance()
					.getCurrentUserId())
				newChild.childISCallOut = AudioVideoMessageBean.STATE_CALL_IN;
			else
				newChild.childISCallOut = AudioVideoMessageBean.STATE_CALL_OUT;

			mListItem.add(0 , newChild);
			adapter.notifyDataSetChanged();
			
			//update conversation state
			Intent i = new Intent(PublicIntent.REQUEST_UPDATE_CONVERSATION);
	        i.addCategory(PublicIntent.DEFAULT_CATEGORY);
	    	ConversationNotificationObject obj = new ConversationNotificationObject(Conversation.TYPE_CONTACT,
	    			-1);
	        obj.setMsgID(0);
	        i.putExtra("obj", obj);
	        i.putExtra("isFresh", false);
	        i.putExtra("isDelete", false);
	        mContext.sendBroadcast(i);	
	        
	        //update database state
	        ContentValues values = new ContentValues();
			values.put(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_READ_STATE , AudioVideoMessageBean.STATE_READED);
			String where = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_READ_STATE + "= ? and " +
					ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_REMOTE_USER_ID + "= ?";
			String[] args = new String[]{ String.valueOf(0) , String.valueOf(remoteID)};
			DataBaseContext con = new DataBaseContext(VoiceMessageDetailActivity.this);
			con.getContentResolver().update(ContentDescriptor.HistoriesMedia.CONTENT_URI, 
					values, where, args);
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
		BitmapManager.getInstance().unRegisterBitmapChangedListener(bitmapChangedListener);
	}
	
	private BitmapManager.BitmapChangedListener bitmapChangedListener = new BitmapManager.BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			Bitmap avatarBitmap = user.getAvatarBitmap();
			if (avatarBitmap != null && !avatarBitmap.isRecycled()) {
				userIcon.setImageBitmap(avatarBitmap);
			}
		}
	};
}
