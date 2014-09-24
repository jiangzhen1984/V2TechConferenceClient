package com.v2tech.view.contacts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.DataBaseContext;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.DateUtil;
import com.v2tech.view.PublicIntent;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.view.conversation.P2PConversation;
import com.v2tech.vo.AudioVideoMessageBean;
import com.v2tech.vo.AudioVideoMessageBean.ChildMessageBean;
import com.v2tech.vo.User;
import com.v2tech.vo.UserDeviceConfig;
import com.v2tech.vo.VideoBean;

public class VoiceMessageActivity extends Activity {

	public static final String TAG = "VoiceMessageActivity";
	private TextView callBack;
	private TextView deleteOperator;
	private TextView cannelOperator;
	private RelativeLayout deleteLayout;
	private CheckBox selectedAll;
	private ListView mVoicesList;
	private VoiceBaseAdapter adapter;
	private List<AudioVideoMessageBean> mListItem;
	private Map<Integer, AudioVideoMessageBean> deleteList;
	private Context mContext;
	private ViewHolder holder = null;
	private VoiceReceiverBroadcast receiver;
	private boolean isVisibile;
	private boolean isEditing;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_specificitem_voice);
		mContext = this;
		findview();
		initReceiver();
		setListener();
		init();
	}

	private void findview() {
		mVoicesList = (ListView) findViewById(R.id.specific_voice_listview);
		callBack = (TextView) findViewById(R.id.specific_title_return);
		deleteLayout = (RelativeLayout) findViewById(R.id.specific_voice_delete);
		cannelOperator = (TextView) findViewById(R.id.specific_title_cannel);
		deleteOperator = (TextView) findViewById(R.id.specific_title_deleteAll);
		selectedAll = (CheckBox) findViewById(R.id.specific_voice_delete_all);
	}

	private void setListener() {

		callBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mListItem = null;
				deleteList = null;
				finish();
			}
		});

		mVoicesList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				if(isEditing){
					CheckBox selected = (CheckBox) view
							.findViewById(R.id.specific_voice_check);
					if(selected.isChecked()){
						mListItem.get(position).isCheck = false;
						selected.setChecked(false);
						deleteList.remove(position);
					}
					else{
						deleteList.put(position, mListItem.get(position));
						mListItem.get(position).isCheck = true;
						selected.setChecked(true);
					}
				}
				else{
					
					Intent intent = new Intent();
					intent.addCategory(PublicIntent.DEFAULT_CATEGORY);
					intent.setAction(PublicIntent.START_P2P_CONVERSACTION_ACTIVITY);
					AudioVideoMessageBean audioVideoMessageBean = mListItem.get(position);
					if (audioVideoMessageBean.mediaType == AudioVideoMessageBean.TYPE_AUDIO){
						intent.putExtra("uid", audioVideoMessageBean.remoteUserID);
						intent.putExtra("is_coming_call", false);
						intent.putExtra("voice", true);
					}
					else{
						intent.putExtra("uid", audioVideoMessageBean.remoteUserID);
						intent.putExtra("is_coming_call", false);
						intent.putExtra("voice", false);
						List<UserDeviceConfig> list = GlobalHolder.getInstance()
								.getAttendeeDevice(audioVideoMessageBean.remoteUserID);
						if (list != null && list.size() > 0) {
							intent.putExtra("device", list.get(0).getDeviceID());
						} else {
							intent.putExtra("device", "");
						}
					}
					startActivity(intent);
				}
			}
		});

		mVoicesList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				isEditing = true;
				deleteLayout.setVisibility(View.VISIBLE);
				deleteOperator.setVisibility(View.VISIBLE);
				cannelOperator.setVisibility(View.VISIBLE);
				cannelOperator.setClickable(true);

				callBack.setVisibility(View.INVISIBLE);
				callBack.setClickable(false);
				
				isVisibile = true;
				adapter.notifyDataSetChanged();
				return false;
			}
		});

		cannelOperator.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				isEditing = false;
				deleteLayout.setVisibility(View.GONE);
				deleteOperator.setVisibility(View.INVISIBLE);
				cannelOperator.setVisibility(View.INVISIBLE);
				cannelOperator.setClickable(false);

				callBack.setVisibility(View.VISIBLE);
				callBack.setClickable(true);
				
				isVisibile = false;
				adapter.notifyDataSetChanged();
			}
		});

		deleteOperator.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (deleteList.size() <= 0)
					return;

				for (Iterator<Entry<Integer, AudioVideoMessageBean>> i = deleteList
						.entrySet().iterator(); i.hasNext();) {
					Entry<Integer, AudioVideoMessageBean> entry = i.next();
					mListItem.remove(entry.getValue());
					AudioVideoMessageBean value = entry.getValue();
					String where = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_REMOTE_USER_ID
							+ " = ?";
					String[] selectionArgs = new String[] { String.valueOf(value.remoteUserID) };
					int delete = getContentResolver().delete(
							ContentDescriptor.HistoriesMedia.CONTENT_URI,
							where, selectionArgs);
					if (delete == 0)
						Log.e(TAG, "delete failed...");
				}
				adapter.notifyDataSetChanged();
			}
		});

		selectedAll.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {

				for (int i = 0; i < mListItem.size(); i++) {
					mListItem.get(i).isCheck = isChecked;
					deleteList.put(i, mListItem.get(i));
				}
				adapter.notifyDataSetChanged();
			}
		});
	}

	private void initReceiver() {
		receiver = new VoiceReceiverBroadcast();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(P2PConversation.P2P_BROADCAST_MEDIA_UPDATE);
		intentFilter.addCategory(PublicIntent.DEFAULT_CATEGORY);
		registerReceiver(receiver, intentFilter);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
		BitmapManager.getInstance().unRegisterBitmapChangedListener(bitmapChangedListener);
	}

	private void init() {
		// 异步去加载数据库
		new AsyncTask<Void, Void, List<AudioVideoMessageBean>>() {

			@Override
			protected List<AudioVideoMessageBean> doInBackground(Void... params) {

				return MessageLoader.loadAudioOrVideoHistoriesMessage(mContext,
						GlobalHolder.getInstance().getCurrentUserId(),
						AudioVideoMessageBean.TYPE_ALL);
			}

			@Override
			protected void onPostExecute(List<AudioVideoMessageBean> result) {
				super.onPostExecute(result);

				if (result == null) {
					Toast.makeText(mContext, "加载发生异常，请重新加载", Toast.LENGTH_SHORT)
							.show();
					return;
				}
				mListItem = result;
				adapter = new VoiceBaseAdapter();
				mVoicesList.setAdapter(adapter);
			}

		}.execute();

		deleteList = new HashMap<Integer, AudioVideoMessageBean>();
		BitmapManager.getInstance().registerBitmapChangedListener(
				this.bitmapChangedListener);
	}

	class VoiceBaseAdapter extends BaseAdapter {

		private AudioVideoMessageBean audioVideoMessageBean;

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
		public View getView(final int position, View convertView, ViewGroup parent) {

			if (convertView == null) {
				holder = new ViewHolder();
				convertView = View.inflate(VoiceMessageActivity.this,
						R.layout.activity_specificitem_voice_adapter, null);
				holder.voiceName = (TextView) convertView
						.findViewById(R.id.specific_voice_name);
				holder.unreadNumber = (TextView) convertView
						.findViewById(R.id.specific_voice_unreadNumber);
				holder.watchDetail = (TextView) convertView
						.findViewById(R.id.specific_voice_watchDetail);
				holder.voiceHoldingTime = (TextView) convertView
						.findViewById(R.id.specific_voice_times);
				holder.directionIcon = (ImageView) convertView
						.findViewById(R.id.specific_voice_direction_icon);
				holder.headIcon = (ImageView) convertView
						.findViewById(R.id.specific_voice_headIcon);
				holder.selected = (CheckBox) convertView
						.findViewById(R.id.specific_voice_check);
				holder.notifyIcon = (ImageView) convertView
						.findViewById(R.id.group_list_conference_notificator);
				convertView.setTag(holder);
			} else
				holder = (ViewHolder) convertView.getTag();

			audioVideoMessageBean = mListItem.get(position);
			User remoteUser = GlobalHolder.getInstance().getUser(audioVideoMessageBean.remoteUserID);
			if (remoteUser.getAvatarBitmap() != null) 
				holder.headIcon.setImageBitmap(remoteUser.getAvatarBitmap());
			else
				holder.headIcon.setImageResource(R.drawable.avatar);
			holder.voiceName.setText(audioVideoMessageBean.name);
			// 处理时间显示
			if (audioVideoMessageBean.holdingTime >= 0) {
				String time = DateUtil
						.getDates(audioVideoMessageBean.holdingTime);
				if (audioVideoMessageBean.mediaType == AudioVideoMessageBean.TYPE_AUDIO)
					holder.voiceHoldingTime.setText("[音频] " + time);
				else
					holder.voiceHoldingTime.setText("[视频] " + time);
			} else {
				if (audioVideoMessageBean.mediaType == AudioVideoMessageBean.TYPE_AUDIO)
					holder.voiceHoldingTime.setText("[音频] ");
				else
					holder.voiceHoldingTime.setText("[视频] ");
			}
			// 处理图标
			if (audioVideoMessageBean.isCallOut == AudioVideoMessageBean.STATE_CALL_OUT)
				holder.directionIcon
						.setImageResource(R.drawable.vs_voice_callout);
			else {
				// 处理未读数量与字体颜色
				if (audioVideoMessageBean.readState == AudioVideoMessageBean.STATE_UNREAD) {
					holder.unreadNumber.setVisibility(View.VISIBLE);
					holder.directionIcon
							.setImageResource(R.drawable.vs_voice_nolistener);
					holder.voiceName.setTextColor(Color.RED);
					holder.unreadNumber.setText(" ( "
							+ audioVideoMessageBean.callNumbers + " )");
					holder.unreadNumber.setTextColor(Color.RED);
				} else {
					holder.unreadNumber.setVisibility(View.GONE);
					holder.directionIcon
							.setImageResource(R.drawable.vs_voice_listener);
				}
			}
			
			if(isVisibile)
				holder.selected.setVisibility(View.VISIBLE);
			else
				holder.selected.setVisibility(View.GONE);
			
			if (mListItem.get(position).isCheck) {
				holder.selected.setChecked(true);
			} else {
				holder.selected.setChecked(false);
			}
				
			holder.watchDetail.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

					AudioVideoMessageBean bean = mListItem.get(position);
					ArrayList<ChildMessageBean> mChildBeans = bean.mChildBeans;
					Intent intent = new Intent(mContext,
							VoiceMessageDetailActivity.class);
					intent.putParcelableArrayListExtra("messages", mChildBeans);
					intent.putExtra("remoteUserID", bean.remoteUserID);
					startActivity(intent);

					holder.notifyIcon.setVisibility(View.INVISIBLE);
					bean.callNumbers = 0;
					
					ContentValues values = new ContentValues();
					values.put(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_READ_STATE , 1);
					String where = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_READ_STATE + "= ?";
					String[] selectionArgs = new String[]{ String.valueOf(0) };
					DataBaseContext context = new DataBaseContext(VoiceMessageActivity.this);
					context.getContentResolver().update(ContentDescriptor.HistoriesMedia.CONTENT_URI, 
							values, where, selectionArgs);
					
					finish();
					mListItem = null;
					deleteList = null;
				}
			});
			return convertView;
		}
	}

	class ViewHolder {

		public TextView voiceName;
		public TextView unreadNumber;
		public TextView watchDetail;
		public TextView voiceHoldingTime;
		public ImageView directionIcon;
		public ImageView headIcon;
		public CheckBox selected;
		public ImageView notifyIcon;
	}

	class VoiceReceiverBroadcast extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			long remoteID = intent.getLongExtra("remoteID", -1l);
			if (remoteID == -1l) {
				Log.e(TAG, "get remoteID is -1 ... update failed!!");
				return;
			}

			String selections = ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_REMOTE_USER_ID
					+ "= ? ";
			String[] selectionArgs = new String[] { String.valueOf(remoteID) };
			VideoBean newestMediaMessage = MessageLoader.getNewestMediaMessage(
					VoiceMessageActivity.this, selections, selectionArgs);
			if (newestMediaMessage == null) {
				Log.e(TAG, "get newest remoteID " + remoteID
						+ " --> VideoBean is NULL ... update failed!!");
				return;
			}

			for (AudioVideoMessageBean target : mListItem) {
				if (target.remoteUserID == remoteID) {
					target.holdingTime = newestMediaMessage.endDate
							- newestMediaMessage.startDate;
					target.mediaType = newestMediaMessage.mediaType;
					target.meidaState = newestMediaMessage.mediaState;
					target.readState = newestMediaMessage.readSatate;
					if (target.readState == AudioVideoMessageBean.STATE_UNREAD) {

						target.callNumbers += 1;
						holder.notifyIcon.setVisibility(View.VISIBLE);
					}
					adapter.notifyDataSetChanged();
					return;
				}
			}
		}
	}
	
	private BitmapManager.BitmapChangedListener bitmapChangedListener = new BitmapManager.BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			for (AudioVideoMessageBean bean : mListItem) {
				User remoteUser = GlobalHolder.getInstance().getUser(bean.remoteUserID);
				if (remoteUser.getAvatarBitmap() != null) {
					holder.headIcon.setImageBitmap(remoteUser.getAvatarBitmap());
				}
			}
		}
	};
}
