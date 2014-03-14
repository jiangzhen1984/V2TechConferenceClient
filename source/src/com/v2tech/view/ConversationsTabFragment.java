package com.v2tech.view;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.logic.AsynResult;
import com.v2tech.logic.Conference;
import com.v2tech.logic.ConferenceConversation;
import com.v2tech.logic.ContactConversation;
import com.v2tech.logic.Conversation;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.Group;
import com.v2tech.logic.NetworkStateCode;
import com.v2tech.logic.User;
import com.v2tech.logic.jni.JNIResponse;
import com.v2tech.logic.jni.RequestEnterConfResponse;
import com.v2tech.service.ConferenceService;
import com.v2tech.util.V2Log;
import com.v2tech.view.ContactsTabFragment.ContactsAdapter;
import com.v2tech.view.ContactsTabFragment.ListItem;
import com.v2tech.view.conference.GroupLayout;
import com.v2tech.view.conference.VideoActivityV2;

public class ConversationsTabFragment extends Fragment {

	private static final int FILL_CONFS_LIST = 2;
	private static final int REQUEST_ENTER_CONF = 3;
	private static final int REQUEST_ENTER_CONF_RESPONSE = 4;
	private static final int REQUEST_EXIT_CONF = 5;
	private static final int UPDATE_NEW_MESSAGE_NOTIFICATION = 6;
	private static final int NEW_MESSAGE_READED_NOTIFICATION = 7;
	private static final int UPDATE_USER_SIGN = 8;
	private static final int UPDATE_USER_AVATAR = 9;

	private static final int RETRY_COUNT = 10;

	private Tab1BroadcastReceiver receiver = new Tab1BroadcastReceiver();
	private IntentFilter intentFilter;

	private List<Group> mConferenceList;

	private EditText mSearchTextET;

	private ConfsHandler mHandler = new ConfsHandler();

	private ProgressDialog mWaitingDialog;

	private ImageView mLoadingImageIV;

	private long currentConfId;

	private boolean isLoaded = false;

	private View rootView;

	private List<ScrollItem> mItemList;

	private Context mContext;
	
	private LinearLayout networkNotificationContainer;
	
	private ListView mConversationsListView;
	
	private ConversationsAdapter adapter = new ConversationsAdapter();

	private ConferenceService cb = new ConferenceService();;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().registerReceiver(receiver, getIntentFilter());
		mItemList = new ArrayList<ScrollItem>();
		mContext = getActivity();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (rootView == null) {
			rootView = inflater.inflate(R.layout.tab_fragment_conversations,
					container, false);
			mConversationsListView = (ListView) rootView
					.findViewById(R.id.conversations_list_container);
			mConversationsListView.setAdapter(adapter);
			mLoadingImageIV = (ImageView) rootView
					.findViewById(R.id.conference_loading_icon);

			mSearchTextET = (EditText) rootView.findViewById(R.id.confs_search);
			networkNotificationContainer = (LinearLayout) rootView
					.findViewById(R.id.recent_conversation_network_nt);

			mSearchTextET.addTextChangedListener(new TextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {

				}

				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
				}

			});
		} else {
			((ViewGroup) rootView.getParent()).removeView(rootView);
		}
		return rootView;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(receiver);
		isLoaded = false;
		mItemList.clear();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	private IntentFilter getIntentFilter() {
		if (intentFilter == null) {
			intentFilter = new IntentFilter();
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_NOTIFICATION);
			intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			intentFilter.addAction(MainActivity.SERVICE_BOUNDED_EVENT);
			intentFilter.addAction(JNIService.JNI_BROADCAST_NEW_MESSAGE);
			intentFilter.addAction(PublicIntent.MESSAGE_READED_NOTIFICATION);
			intentFilter.addCategory(PublicIntent.DEFAULT_CATEGORY);
			intentFilter.addAction(PublicIntent.NEW_CONVERSATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_AVATAR_CHANGED_NOTIFICATION);
			intentFilter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);

		}
		return intentFilter;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!isLoaded) {
			Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Message.obtain(mHandler, REQUEST_EXIT_CONF, currentConfId)
				.sendToTarget();
	}

	private void populateConversation(List<Group> list) {
		if (list == null || list.size() <= 0) {
			V2Log.w(" group list is null");
			return;
		}

		for (final Group g : list) {
			Conversation cov = new ConferenceConversation(g);
			final GroupLayout gp = new GroupLayout(this.getActivity(), cov);
			gp.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
//					// TODO hidden request to enter conference feature
//					mWaitingDialog = ProgressDialog
//							.show(getActivity(),
//									"",
//									getActivity()
//											.getResources()
//											.getString(
//													R.string.requesting_enter_conference),
//									true);
//					currentConfId = g.getmGId();
//					Message.obtain(mHandler, REQUEST_ENTER_CONF,
//							Long.valueOf(g.getmGId())).sendToTarget();
				}

			});
			mItemList.add(0, new ScrollItem(cov, gp));
		}
		adapter.notifyDataSetChanged();

	}

	private void loadConversation() {

		Cursor mCur = getActivity().getContentResolver().query(
				ContentDescriptor.Conversation.CONTENT_URI,
				ContentDescriptor.Conversation.Cols.ALL_CLOS,
				ContentDescriptor.Conversation.Cols.TYPE + "=? and "
						+ ContentDescriptor.Conversation.Cols.OWNER + "=?",
				new String[] { Conversation.TYPE_CONTACT,
						GlobalHolder.getInstance().getCurrentUserId() + "" },
				null);

		while (mCur.moveToNext()) {
			Conversation cov = extractConversation(mCur);
			final GroupLayout gp = new GroupLayout(this.getActivity(), cov);
			mItemList.add(new ScrollItem(cov, gp));
			if (!GlobalHolder.getInstance().findConversation(cov)) {
				GlobalHolder.getInstance().addConversation(cov);
			}
		}
		mCur.close();
	}

	private Conversation extractConversation(Cursor cur) {
		long extId = cur.getLong(2);
		String name = cur.getString(3);
		int flag = cur.getInt(4);
		User u = GlobalHolder.getInstance().getUser(extId);
		if (u == null) {
			u = new User(extId);
			u.setName(name);
		}
		Conversation cov = new ContactConversation(u, flag);
		return cov;
	}
	
	private void updateConversation(long extId, String name) {
		ContentValues ct = new ContentValues();
		ct.put(ContentDescriptor.Conversation.Cols.EXT_NAME,
				name);
		getActivity().getContentResolver().update(
				ContentDescriptor.Conversation.CONTENT_URI,
				ct,
				ContentDescriptor.Conversation.Cols.EXT_ID + "=? and "
						+ ContentDescriptor.Conversation.Cols.TYPE
						+ "=?",
				new String[] { extId + "",
						Conversation.TYPE_CONTACT });
	}


	class ScrollItem {
		Conversation cov;
		View gp;

		public ScrollItem(Conversation g, View gp) {
			super();
			this.cov = g;
			this.gp = gp;
		}

	}
	
	
	
	class ConversationsAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mItemList.size();
		}

		@Override
		public Object getItem(int position) {
			ScrollItem item = mItemList.get(position);
			return item.cov;
		}

		@Override
		public long getItemId(int position) {
			return mItemList.get(position).cov.getExtId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return mItemList.get(position).gp;
		}

	}
	
	

	class Tab1BroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_GROUP_NOTIFICATION)) {
				Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
			} else if (JNIService.JNI_BROADCAST_NEW_MESSAGE.equals(intent
					.getAction())
					|| PublicIntent.NEW_CONVERSATION.equals(intent.getAction())) {
				Message.obtain(mHandler, UPDATE_NEW_MESSAGE_NOTIFICATION,
						intent.getExtras().getLong("fromuid")).sendToTarget();
			} else if (PublicIntent.MESSAGE_READED_NOTIFICATION.equals(intent
					.getAction())) {
				Message.obtain(mHandler, NEW_MESSAGE_READED_NOTIFICATION,
						intent.getExtras().getLong("fromuid")).sendToTarget();
			} else if (JNIService.JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE
					.equals(intent.getAction())) {
				Message.obtain(mHandler, UPDATE_USER_SIGN,
						intent.getExtras().get("uid")).sendToTarget();
			} else if (JNIService.JNI_BROADCAST_USER_AVATAR_CHANGED_NOTIFICATION
					.equals(intent.getAction())) {
				Object[] ar = new Object[] { intent.getExtras().get("uid"),
						intent.getExtras().get("avatar") };
				Message.obtain(mHandler, UPDATE_USER_AVATAR, ar).sendToTarget();
			} else if (JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION.equals(intent.getAction())) {
				NetworkStateCode code = (NetworkStateCode)intent.getExtras().get("state");
				if (code != NetworkStateCode.CONNECTED) {
					networkNotificationContainer.setVisibility(View.VISIBLE);
				} else {
					networkNotificationContainer.setVisibility(View.GONE);
				}
			}
		}

	}

	class ConfsHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FILL_CONFS_LIST:
				mConferenceList = GlobalHolder.getInstance().getGroup(
						Group.GroupType.CONFERENCE);
				// No server return send asynchronous message and waiting for
				// response
				if (mConferenceList != null) {
					if (!isLoaded) {
						((ViewGroup) mLoadingImageIV.getParent())
								.removeView(mLoadingImageIV);
						populateConversation(mConferenceList);
						isLoaded = true;
						loadConversation();
					}
				} else {
					if (msg.arg1 < RETRY_COUNT) {
						Message m = Message.obtain(this, FILL_CONFS_LIST,
								msg.arg1 + 1, 0);
						this.sendMessageDelayed(m, 1000);
					} else {
						if (!isLoaded) {
							isLoaded = true;
							((ViewGroup) mLoadingImageIV.getParent())
									.removeView(mLoadingImageIV);
							loadConversation();
						}
					}
				}

				break;
			case REQUEST_ENTER_CONF:
				cb.requestEnterConference(new Conference((Long) msg.obj),
						Message.obtain(this, REQUEST_ENTER_CONF_RESPONSE));
				break;
			case REQUEST_ENTER_CONF_RESPONSE:
				AsynResult ar = (AsynResult) msg.obj;
				if (ar.getState() == AsynResult.AsynState.SUCCESS) {
					RequestEnterConfResponse recr = (RequestEnterConfResponse) ar
							.getObject();
					if (recr.getResult() == JNIResponse.Result.SUCCESS) {
						Intent i = new Intent(getActivity(),
								VideoActivityV2.class);
						i.putExtra("gid", recr.getConferenceID());
						startActivityForResult(i, 0);
					} else {
						Toast.makeText(getActivity(),
								R.string.error_request_enter_conference,
								Toast.LENGTH_SHORT).show();
					}

				} else if (ar.getState() == AsynResult.AsynState.TIME_OUT) {
					Toast.makeText(getActivity(),
							R.string.error_request_enter_conference_time_out,
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getActivity(),
							R.string.error_request_enter_conference,
							Toast.LENGTH_SHORT).show();
				}
				if (mWaitingDialog != null) {
					mWaitingDialog.dismiss();
				}

				break;
			case REQUEST_EXIT_CONF:
				cb.requestExitConference(new Conference((Long) msg.obj), null);
				break;
			case UPDATE_NEW_MESSAGE_NOTIFICATION:
				long fromuid = (Long) msg.obj;
				boolean b = false;
				for (ScrollItem item : mItemList) {
					if (item.cov.getExtId() == fromuid
							&& Conversation.TYPE_CONTACT.equals(item.cov
									.getType())) {
						item.cov.setNotiFlag(Conversation.NOTIFICATION);
						((GroupLayout) item.gp).updateNotificator(true);
						b = true;
						break;
					}
				}
				if (!b) {
					Conversation cov = (ContactConversation) GlobalHolder
							.getInstance().findConversationByType(
									Conversation.TYPE_CONTACT, fromuid);

					GroupLayout gp = new GroupLayout(mContext, cov);
					mItemList.add(new ScrollItem(cov, gp));
				}
				break;

			case NEW_MESSAGE_READED_NOTIFICATION:
				long fromuidT = (Long) msg.obj;
				for (ScrollItem item : mItemList) {
					if (item.cov.getExtId() == fromuidT
							&& Conversation.TYPE_CONTACT.equals(item.cov
									.getType())) {
						item.cov.setNotiFlag(Conversation.NONE);
						((GroupLayout) item.gp).updateNotificator(false);
						break;
					}
				}
				break;

			case UPDATE_USER_SIGN:
				long fromuidS = (Long) msg.obj;
				for (ScrollItem item : mItemList) {
					if (item.cov.getExtId() == fromuidS
							&& Conversation.TYPE_CONTACT.equals(item.cov
									.getType())) {
						User u = GlobalHolder.getInstance().getUser(fromuidS);
						((GroupLayout) item.gp).updateGroupOwner(u == null ? ""
								: u.getName());
						updateConversation(fromuidS, u == null ? ""
								: u.getName());
						break;
					}
				}
				break;
			case UPDATE_USER_AVATAR:
				Object[] arObj = (Object[]) msg.obj;
				for (ScrollItem item : mItemList) {
					if (Conversation.TYPE_CONTACT.equals(item.cov.getType())
							&& item.cov.getExtId() == (Long) arObj[0]) {
						User u = GlobalHolder.getInstance().getUser(
								item.cov.getExtId());
						((GroupLayout) item.gp).updateIcon(u.getAvatarBitmap());
					}
				}
				break;
			}
		}

	}

}
