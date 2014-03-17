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
import android.graphics.Bitmap;
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
import android.widget.TextView;
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
import com.v2tech.util.BitmapUtil;
import com.v2tech.util.V2Log;
import com.v2tech.view.conference.GroupLayout;
import com.v2tech.view.conference.VideoActivityV2;

public class ConversationsTabFragment extends Fragment {

	private static final int FILL_CONFS_LIST = 2;
	private static final int REQUEST_ENTER_CONF = 3;
	private static final int REQUEST_ENTER_CONF_RESPONSE = 4;
	private static final int REQUEST_EXIT_CONF = 5;
	private static final int UPDATE_USER_SIGN = 8;
	private static final int UPDATE_USER_AVATAR = 9;
	private static final int UPDATE_CONVERSATION = 10;
	private static final int UPDATE_SEARCHED_LIST = 11;

	private Tab1BroadcastReceiver receiver = new Tab1BroadcastReceiver();
	private IntentFilter intentFilter;
	private boolean mIsStartedSearch;

	private List<Group> mConferenceList;

	private EditText mSearchTextET;

	private ConfsHandler mHandler = new ConfsHandler();

	private ProgressDialog mWaitingDialog;

	private ImageView mLoadingImageIV;

	private long currentConfId;

	private boolean isLoaded = false;
	private boolean isLoadedCov = false;

	private View rootView;

	private List<ScrollItem> mItemList = new ArrayList<ScrollItem>();
	private List<ScrollItem> mCacheItemList;

	private Context mContext;

	private LinearLayout networkNotificationContainer;

	private ListView mConversationsListView;

	private TextView mCreateConferenceButtonTV;

	private ConversationsAdapter adapter = new ConversationsAdapter();

	private ConferenceService cb = new ConferenceService();;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().registerReceiver(receiver, getIntentFilter());

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
			mCreateConferenceButtonTV = (TextView) rootView
					.findViewById(R.id.conference_create_button);
			if (mCreateConferenceButtonTV != null) {
				mCreateConferenceButtonTV
						.setOnClickListener(mConferenceCreateButtonListener);
			}

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
		isLoaded = false;
		mItemList.clear();
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
			intentFilter.addCategory(PublicIntent.DEFAULT_CATEGORY);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_AVATAR_CHANGED_NOTIFICATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
			intentFilter.addCategory(PublicIntent.DEFAULT_CATEGORY);
			intentFilter.addAction(PublicIntent.UPDATE_CONVERSATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);

		}
		return intentFilter;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!isLoaded) {
			Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
		}
		mSearchTextET.addTextChangedListener(searchListener);
	}

	@Override
	public void onStop() {
		super.onStop();
		mSearchTextET.removeTextChangedListener(searchListener);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0) {
			Message.obtain(mHandler, REQUEST_EXIT_CONF, currentConfId)
					.sendToTarget();
		}
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
//					// // TODO hidden request to enter conference feature
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

	private OnClickListener mConferenceCreateButtonListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			Intent i = new Intent(PublicIntent.START_CONFERENCE_CREATE_ACTIVITY);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			startActivityForResult(i, 1);
		}

	};

	private TextWatcher searchListener = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {

			if (s.length() > 0) {
				if (!mIsStartedSearch) {
					mCacheItemList = mItemList;
					mIsStartedSearch = true;
				}
			} else {
				mItemList = mCacheItemList;
				adapter.notifyDataSetChanged();
				mIsStartedSearch = false;
				return;
			}
			List<ScrollItem> newItemList = new ArrayList<ScrollItem>();
			String searchKey = s.toString();
			for (ScrollItem item : mItemList) {
				if (item.cov.getName() != null
						&& item.cov.getName().contains(searchKey)) {
					newItemList.add(item);
				} else if (item.cov.getMsg() != null
						&& item.cov.getMsg().contains(searchKey)) {
					newItemList.add(item);
				}
			}
			mItemList = newItemList;
			adapter.notifyDataSetChanged();
		}

	};

	private void loadConversation() {
		isLoadedCov = true;
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

	private void updateConversationToDB(long extId, String name) {
		ContentValues ct = new ContentValues();
		ct.put(ContentDescriptor.Conversation.Cols.EXT_NAME, name);
		getActivity().getContentResolver().update(
				ContentDescriptor.Conversation.CONTENT_URI,
				ct,
				ContentDescriptor.Conversation.Cols.EXT_ID + "=? and "
						+ ContentDescriptor.Conversation.Cols.TYPE + "=?",
				new String[] { extId + "", Conversation.TYPE_CONTACT });
	}

	private void updateConversation(long extId, String mType, String content,
			String date, boolean showNotification) {
		boolean foundFlag = false;
		GroupLayout gl = null;
		for (ScrollItem item : mItemList) {
			if (item.cov.getExtId() == extId
					&& Conversation.TYPE_CONTACT.equals(item.cov.getType())) {
				item.cov.setNotiFlag(showNotification ? Conversation.NOTIFICATION
						: Conversation.NONE);
				gl = (GroupLayout) item.gp;
				foundFlag = true;
				break;
			}
		}

		if (!foundFlag) {
			Conversation cov = (ContactConversation) GlobalHolder.getInstance()
					.findConversationByType(Conversation.TYPE_CONTACT, extId);
			if (cov == null) {
				cov = new ContactConversation(GlobalHolder.getInstance()
						.getUser(extId),
						showNotification ? Conversation.NOTIFICATION
								: Conversation.NONE);
				GlobalHolder.getInstance().addConversation(cov);
				//TODO insert to database
				
				User fromUser = GlobalHolder.getInstance().getUser(extId);
				ContentValues conCv = new ContentValues();
				conCv.put(ContentDescriptor.Conversation.Cols.EXT_ID, extId);
				conCv.put(ContentDescriptor.Conversation.Cols.TYPE,
						Conversation.TYPE_CONTACT);
				conCv.put(ContentDescriptor.Conversation.Cols.EXT_NAME,
						fromUser.getName());
				conCv.put(ContentDescriptor.Conversation.Cols.NOTI_FLAG, showNotification? 
						Conversation.NOTIFICATION:Conversation.NONE);
				conCv.put(ContentDescriptor.Conversation.Cols.OWNER, GlobalHolder
						.getInstance().getCurrentUserId());
				mContext.getContentResolver().insert(
						ContentDescriptor.Conversation.CONTENT_URI, conCv);
				
			}
			gl = new GroupLayout(mContext, cov);
			mItemList.add(new ScrollItem(cov, gl));
			adapter.notifyDataSetChanged();
		}

		gl.update(content, date, showNotification);
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
			return mItemList == null ? 0 : mItemList.size();
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
			// TODO optimze code
			if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_GROUP_NOTIFICATION)) {
				Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
			} else if (JNIService.JNI_BROADCAST_USER_UPDATE_NAME_OR_SIGNATURE
					.equals(intent.getAction())) {
				if (mItemList != null && mItemList.size() > 0) {
					Message.obtain(mHandler, UPDATE_USER_SIGN,
							intent.getExtras().get("uid")).sendToTarget();
				}
			} else if (JNIService.JNI_BROADCAST_USER_AVATAR_CHANGED_NOTIFICATION
					.equals(intent.getAction())) {
				Object[] ar = new Object[] { intent.getExtras().get("uid"),
						intent.getExtras().get("avatar") };
				Message.obtain(mHandler, UPDATE_USER_AVATAR, ar).sendToTarget();
			} else if (JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION
					.equals(intent.getAction())) {
				NetworkStateCode code = (NetworkStateCode) intent.getExtras()
						.get("state");
				if (code != NetworkStateCode.CONNECTED) {
					networkNotificationContainer.setVisibility(View.VISIBLE);
				} else {
					networkNotificationContainer.setVisibility(View.GONE);
				}
			} else if (PublicIntent.UPDATE_CONVERSATION.equals(intent
					.getAction())) {
				Object[] ar = new Object[] { intent.getLongExtra("extId", 0),
						intent.getExtras().getString("type"),
						intent.getExtras().getString("content"),
						intent.getExtras().getString("date"),
						intent.getExtras().getBoolean("noti") };
				Message.obtain(mHandler, UPDATE_CONVERSATION, ar)
						.sendToTarget();
			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {
				// TODO
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
				if (mConferenceList != null) {
					if (!isLoaded) {
						populateConversation(mConferenceList);
						isLoaded = true;
					}
				}

				if (!isLoadedCov) {
					loadConversation();
				}
				if (mLoadingImageIV.getParent() != null) {
					((ViewGroup) mLoadingImageIV.getParent())
							.removeView(mLoadingImageIV);
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

			case UPDATE_USER_SIGN:
				long fromuidS = (Long) msg.obj;
				for (ScrollItem item : mItemList) {
					if (item.cov.getExtId() == fromuidS
							&& Conversation.TYPE_CONTACT.equals(item.cov
									.getType())) {
						User u = GlobalHolder.getInstance().getUser(fromuidS);
						((GroupLayout) item.gp).updateGroupOwner(u == null ? ""
								: u.getName());
						updateConversationToDB(fromuidS,
								u == null ? "" : u.getName());
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
						if (u != null) {
							((GroupLayout) item.gp).updateIcon(u
									.getAvatarBitmap());
						} else {
							// FIXME handle concurrency
							Bitmap bm = BitmapUtil
									.loadAvatarFromPath(GlobalHolder
											.getInstance().getAvatarPath(
													item.cov.getExtId()));
							GlobalHolder.getInstance().saveAvatar(
									item.cov.getExtId(), bm);
							((GroupLayout) item.gp).updateIcon(bm);
						}
					}
				}
				break;

			case UPDATE_CONVERSATION:
				Object[] oar = (Object[]) msg.obj;
				updateConversation((Long) oar[0], (String) oar[1],
						(String) oar[2], (String) oar[3], (Boolean) oar[4]);

				break;
			case UPDATE_SEARCHED_LIST:
				break;
			}
		}

	}

}
