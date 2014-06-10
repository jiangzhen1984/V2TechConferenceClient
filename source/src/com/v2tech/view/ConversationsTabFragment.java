package com.v2tech.view;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.service.BitmapManager;
import com.v2tech.service.ConferenceService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.V2Log;
import com.v2tech.view.conference.GroupLayout;
import com.v2tech.view.conference.VideoActivityV2;
import com.v2tech.vo.Conference;
import com.v2tech.vo.ConferenceConversation;
import com.v2tech.vo.ContactConversation;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.CrowdConversation;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;

public class ConversationsTabFragment extends Fragment implements TextWatcher {

	private static final int FILL_CONFS_LIST = 2;
	private static final int UPDATE_USER_SIGN = 8;
	private static final int UPDATE_CONVERSATION = 10;
	private static final int UPDATE_SEARCHED_LIST = 11;
	private static final int REMOVE_CONVERSATION = 12;

	private static final int SUB_ACTIVITY_CODE_VIDEO_ACTIVITY = 0;

	private Tab1BroadcastReceiver receiver = new Tab1BroadcastReceiver();
	private IntentFilter intentFilter;
	private boolean mIsStartedSearch;

	private Set<Group> mConferenceList;

	private ConfsHandler mHandler = new ConfsHandler();

	private ImageView mLoadingImageIV;

	private boolean isLoadedCov = false;

	private View rootView;

	private List<ScrollItem> mItemList = new CopyOnWriteArrayList<ScrollItem>();
	private List<ScrollItem> mCacheItemList;

	private Context mContext;

	private ListView mConversationsListView;

	private ConversationsAdapter adapter = new ConversationsAdapter();

	private ConferenceService cb;

	private String mCurrentTabFlag;

	private List<Object[]> pendingNotification;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String tag = this.getArguments().getString("tag");
		if (PublicIntent.TAG_CONF.equals(tag)) {
			mCurrentTabFlag = Conversation.TYPE_CONFERNECE;
		} else if (PublicIntent.TAG_COV.equals(tag)) {
			mCurrentTabFlag = Conversation.TYPE_CONTACT;
		} else if (PublicIntent.TAG_GROUP.equals(tag)) {
			mCurrentTabFlag = Conversation.TYPE_GROUP;
		}
		getActivity().registerReceiver(receiver, getIntentFilter());
		mContext = getActivity();
		cb = new ConferenceService();

		pendingNotification = new ArrayList<Object[]>();

		BitmapManager.getInstance().registerBitmapChangedListener(
				this.bitmapChangedListener);

		mConferenceList = new HashSet<Group>();
		Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
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
		/*	
			RelativeLayout rt = (RelativeLayout)rootView.findViewById(R.id.RelativeLayout2);
			
			mLoadingImageIV = new ImageView(getActivity());
			mLoadingImageIV.setImageResource(R.drawable.progress_animation);
			
			RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			rl.addRule(RelativeLayout.CENTER_IN_PARENT);
			rt.addView(mLoadingImageIV, rl);
			
			((AnimationDrawable) mLoadingImageIV.getDrawable()).start();*/

			// TextView tv = (TextView)
			// rootView.findViewById(R.id.fragment_title);
			// if (mCurrentTabFlag.equals(Conversation.TYPE_CONFERNECE)) {
			// tv.setText(R.string.tab_conference_name);
			// } else if (mCurrentTabFlag.equals(Conversation.TYPE_CONTACT)) {
			// tv.setText(R.string.tab_conversation_name);
			// } else if (mCurrentTabFlag.equals(Conversation.TYPE_GROUP)) {
			// tv.setText(R.string.tab_group_name);
			// }

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
		mItemList.clear();
		BitmapManager.getInstance().unRegisterBitmapChangedListener(
				this.bitmapChangedListener);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		((ViewGroup) rootView.getParent()).removeView(rootView);
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
			intentFilter.addCategory(PublicIntent.DEFAULT_CATEGORY);
			intentFilter.addAction(PublicIntent.UPDATE_CONVERSATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION);
			// Only conference conversation fragment can hand conference
			if (mCurrentTabFlag.equals(Conversation.TYPE_CONFERNECE)) {
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_CONFERENCE_INVATITION);
				intentFilter
						.addAction(JNIService.JNI_BROADCAST_CONFERENCE_REMOVED);
				intentFilter
						.addAction(PublicIntent.BROADCAST_NEW_CONFERENCE_NOTIFICATION);
			}
			if (mCurrentTabFlag.equals(Conversation.TYPE_GROUP)) {
				intentFilter
						.addAction(PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION);
			}

		}
		return intentFilter;
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	/**
	 * FIXME optimize code
	 */

	private void populateConversation(final Group g, boolean flag) {
		// FIXME
		if (mConferenceList != null) {
			boolean ret = mConferenceList.add(g);
			if (!ret) {
				return;
			}
		}
		Conversation cov = new ConferenceConversation(g);
		final GroupLayout gp = new GroupLayout(this.getActivity(), cov);
		gp.updateNotificator(flag);
		if (mCurrentTabFlag.equals(Conversation.TYPE_CONFERNECE)) {
			gp.setOnClickListener(mEnterConfListener);
		} else if (mCurrentTabFlag.equals(Conversation.TYPE_GROUP)) {

			gp.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					// FIXME request enter conference should be here or in video
					// activity
					Intent i = new Intent(
							PublicIntent.START_CONVERSACTION_ACTIVITY);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					i.putExtra("user1id", GlobalHolder.getInstance()
							.getCurrentUserId());
					i.putExtra("gid", g.getmGId());
					i.putExtra("user2Name", g.getName());
					i.addCategory(PublicIntent.DEFAULT_CATEGORY);
					startActivity(i);

				}
			});

		}
		mItemList.add(0, new ScrollItem(cov, gp));
	}

	private void populateConversation(List<Group> list) {
		if (list == null || list.size() <= 0) {
			V2Log.w(" group list is null");
			return;
		}

		for (final Group g : list) {
			if (g.getOwnerUser() == null) {
				g.setOwnerUser(GlobalHolder.getInstance().getUser(g.getOwner()));
			}
			populateConversation(g, false);
		}
		adapter.notifyDataSetChanged();

	}

	private OnClickListener mEnterConfListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// if (isInMeeting) {
			// return;
			// }
			GroupLayout gp = (GroupLayout) v;

			Intent i = new Intent(getActivity(), VideoActivityV2.class);
			i.putExtra("gid", gp.getGroupId());
			startActivityForResult(i, SUB_ACTIVITY_CODE_VIDEO_ACTIVITY);

			gp.updateNotificator(false);

		}

	};

	@Override
	public void afterTextChanged(Editable s) {

		if (s != null && s.length() > 0) {
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
		String searchKey = s == null ? "" : s.toString();
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

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	/**
	 * Use to update current list view adapter and start video activity. this
	 * function will be call only in case of current user create conference
	 * 
	 * @param g
	 */
	public void updateNewGroup(Group g) {
		if (g == null) {
			V2Log.e("Unmatformd group infor");
			return;
		}
		if (mCurrentTabFlag == Conversation.TYPE_CONFERNECE
				&& g.getGroupType() == GroupType.CONFERENCE) {
			// if (g != null) {
			Conversation cov = new ConferenceConversation(g);
			final GroupLayout gp = new GroupLayout(getActivity(), cov);
			gp.setOnClickListener(mEnterConfListener);

			mItemList.add(0, new ScrollItem(cov, gp));
			adapter.notifyDataSetChanged();

			// Automatically start video activity
			// because user has joined conference, once create successfully
			Intent i = new Intent(getActivity(), VideoActivityV2.class);
			i.putExtra("gid", g.getmGId());
			i.putExtra("in", true);
			startActivityForResult(i, SUB_ACTIVITY_CODE_VIDEO_ACTIVITY);

		}

	}

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
		// Notification main activity tab to show notification icon
		((NotificationListener) getActivity())
				.updateNotificator(Conversation.TYPE_CONTACT);
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
		if (extId == 0) {
			return;
		}
		boolean foundFlag = false;
		GroupLayout gl = null;
		for (ScrollItem item : mItemList) {
			if (item.cov.getExtId() == extId) {
				item.cov.setNotiFlag(showNotification ? Conversation.NOTIFICATION
						: Conversation.NONE);
				gl = (GroupLayout) item.gp;
				foundFlag = true;
				break;
			}
		}

		if (!foundFlag) {
			Conversation cov = GlobalHolder.getInstance()
					.findConversationByType(mCurrentTabFlag, extId);
			if (cov == null) {
				if (mCurrentTabFlag.equals(Conversation.TYPE_CONTACT)) {
					cov = new ContactConversation(GlobalHolder.getInstance()
							.getUser(extId),
							showNotification ? Conversation.NOTIFICATION
									: Conversation.NONE);
				} else if (mCurrentTabFlag.equals(Conversation.TYPE_GROUP)) {
					cov = new CrowdConversation(GlobalHolder.getInstance()
							.getGroupById(GroupType.CHATING, extId));
				}
				GlobalHolder.getInstance().addConversation(cov);
				if (mCurrentTabFlag.equals(Conversation.TYPE_CONTACT)) {
					User fromUser = GlobalHolder.getInstance().getUser(extId);
					ContentValues conCv = new ContentValues();
					conCv.put(ContentDescriptor.Conversation.Cols.EXT_ID, extId);
					conCv.put(ContentDescriptor.Conversation.Cols.TYPE,
							Conversation.TYPE_CONTACT);
					conCv.put(ContentDescriptor.Conversation.Cols.EXT_NAME,
							fromUser == null ? "" : fromUser.getName());
					conCv.put(ContentDescriptor.Conversation.Cols.NOTI_FLAG,
							showNotification ? Conversation.NOTIFICATION
									: Conversation.NONE);
					conCv.put(ContentDescriptor.Conversation.Cols.OWNER,
							GlobalHolder.getInstance().getCurrentUserId());
					mContext.getContentResolver().insert(
							ContentDescriptor.Conversation.CONTENT_URI, conCv);
				}

			}
			gl = new GroupLayout(mContext, cov);
			mItemList.add(new ScrollItem(cov, gl));
			adapter.notifyDataSetChanged();
		}

		gl.update(content, date, showNotification);

	}

	private void removeConversation(long id, String type, long owner) {

		if (Conversation.TYPE_CONTACT.equals(type)) {

			mContext.getContentResolver().delete(
					ContentDescriptor.Conversation.CONTENT_URI,
					ContentDescriptor.Conversation.Cols.EXT_ID + "=? and "
							+ ContentDescriptor.Conversation.Cols.TYPE + "=?",
					new String[] { id + "", type });
			GlobalHolder.getInstance().removeConversation(type, id);
			// FIXME remove notification?
		} else if (Conversation.TYPE_CONFERNECE.equals(type)) {
			// remove group from list
			for (Group g : mConferenceList) {
				if (g.getmGId() == id) {
					mConferenceList.remove(g);
					break;
				}
			}
			this.cb.quitConference(new Conference(id, owner), null);
		} else {
			V2Log.e(" Unkonw type:" + type);
		}

		for (ScrollItem li : mItemList) {
			if (li.cov.getExtId() == id) {
				mItemList.remove(li);
				break;
			}
		}

		adapter.notifyDataSetChanged();

	}

	/**
	 * send pending notification
	 */
	private void firePendingNotification() {
		for (Object[] obj : pendingNotification) {
			Message.obtain(mHandler, UPDATE_CONVERSATION, obj).sendToTarget();
		}
		pendingNotification.clear();

	}

	class ScrollItem {
		Conversation cov;
		View gp;

		public ScrollItem(Conversation g, View gp) {
			super();
			this.cov = g;
			this.gp = gp;
			this.gp.setOnLongClickListener(mLongClickListener);
			this.gp.setTag(cov);
		}

	}

	private BitmapManager.BitmapChangedListener bitmapChangedListener = new BitmapManager.BitmapChangedListener() {

		@Override
		public void notifyAvatarChanged(User user, Bitmap bm) {
			for (ScrollItem item : mItemList) {
				if (Conversation.TYPE_CONTACT.equals(item.cov.getType())
						&& item.cov.getExtId() == user.getmUserId()) {
					((GroupLayout) item.gp).updateIcon(bm);
				}
			}

		}
	};

	private OnLongClickListener mLongClickListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(final View v) {
			String[] item = null;

			if (mCurrentTabFlag == Conversation.TYPE_CONFERNECE) {
				item = new String[] { mContext.getResources().getString(
						R.string.conversations_delete_conf) };
			} else if (mCurrentTabFlag == Conversation.TYPE_CONTACT) {
				item = new String[] { mContext.getResources().getString(
						R.string.conversations_delete_conversaion) };
			} else {
				item = new String[] { mContext.getResources().getString(
						R.string.conversations_delete_conversaion) };
			}

			final Conversation cov = (Conversation) v.getTag();
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle(cov.getName()).setItems(item,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							long ownID = 0;
							if (cov.getType().equals(
									Conversation.TYPE_CONFERNECE)) {
								ownID = ((ConferenceConversation) cov)
										.getGroup().getOwner();
							}
							removeConversation(cov.getExtId(), cov.getType(),
									ownID);
						}
					});
			AlertDialog ad = builder.create();
			ad.show();
			return true;
		}

	};

	class RemoveConversationRequest {
		long id;
		String type;

		public RemoveConversationRequest(long id, String type) {
			super();
			this.id = id;
			this.type = type;
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
			} else if (PublicIntent.UPDATE_CONVERSATION.equals(intent
					.getAction())) {
				if (JNIService.JNI_BROADCAST_CONFERENCE_REMOVED.equals(intent
						.getStringExtra("action"))) {
					return;
				}
				
				Object[] ar = new Object[] { intent.getLongExtra("extId", 0),
						intent.getExtras().getString("type"),
						intent.getExtras().getString("content"),
						intent.getExtras().getString("date"),
						intent.getExtras().getBoolean("noti") };

				if (mCurrentTabFlag.equals(ar[1])) {
					Message.obtain(mHandler, UPDATE_CONVERSATION, ar)
							.sendToTarget();

				}

			} else if (JNIService.JNI_BROADCAST_GROUP_USER_UPDATED_NOTIFICATION
					.equals(intent.getAction())) {
				// FIXME crash
				for (ScrollItem item : mItemList) {
					if (item.cov.getType().equals(Conversation.TYPE_CONFERNECE)) {
						Group g = ((ConferenceConversation) item.cov)
								.getGroup();
						User u = GlobalHolder.getInstance().getUser(
								g.getOwner());
						if (u == null) {
							continue;
						}
						((GroupLayout) item.gp).updateContent(u.getName());
						g.setOwnerUser(u);
					} else if (item.cov.getType().equals(
							Conversation.TYPE_CONTACT)) {
						User u = GlobalHolder.getInstance().getUser(
								((ContactConversation) item.cov).getExtId());
						if (u == null) {
							continue;
						}
						((ContactConversation) item.cov).updateUser(u);
					}
				}
			} else if (JNIService.JNI_BROADCAST_CONFERENCE_INVATITION
					.equals(intent.getAction())) {
				long gid = intent.getLongExtra("gid", 0);
				Group g = GlobalHolder.getInstance().getGroupById(
						GroupType.CONFERENCE, gid);
				if (g != null) {
					g.setOwnerUser(GlobalHolder.getInstance().getUser(
							g.getOwner()));
					// check if group exist in list means:
					// user operates exist group again
					if (mConferenceList != null && mConferenceList.size() > 0) {
						for (Group eg : mConferenceList) {
							if (eg.getmGId() == g.getmGId()) {
								return;
							}
						}
					}
					populateConversation(g, true);
					adapter.notifyDataSetChanged();
				} else {
					V2Log.e("Can not get group information of invatition :"
							+ gid);
				}
			} else if (JNIService.JNI_BROADCAST_CONFERENCE_REMOVED
					.equals(intent.getAction())) {
				for (ScrollItem item : mItemList) {
					if (item.cov.getType().equals(Conversation.TYPE_CONFERNECE)) {
						Group g = ((ConferenceConversation) item.cov)
								.getGroup();
						if (g.getmGId() == intent.getLongExtra("gid", 0)) {
							mItemList.remove(item);
							break;
						}
					}
				}
				adapter.notifyDataSetChanged();
			} else if (PublicIntent.BROADCAST_NEW_CROWD_NOTIFICATION
					.equals(intent.getAction())) {
				long gid = intent.getLongExtra("crowd", 0);
				Group g = GlobalHolder.getInstance().getGroupById(
						GroupType.CHATING, gid);
				if (g != null) {
					populateConversation(g, false);
					adapter.notifyDataSetChanged();
				} else {
					V2Log.e("Can not get crowd :" + gid);
				}
			} else if (PublicIntent.BROADCAST_NEW_CONFERENCE_NOTIFICATION
					.equals(intent.getAction())) {
				Group conf = GlobalHolder.getInstance().getGroupById(
						GroupType.CONFERENCE, intent.getLongExtra("newGid", 0));
				updateNewGroup(conf);
			}
		}

	}

	class ConfsHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FILL_CONFS_LIST:
				// FIXME optimze code
				if (mCurrentTabFlag.equals(Conversation.TYPE_CONFERNECE)) {
					List<Group> gl = GlobalHolder.getInstance().getGroup(
							Group.GroupType.CONFERENCE);
					if (gl != null && gl.size() > 0) {
						populateConversation(gl);
					}
				} else if (mCurrentTabFlag.equals(Conversation.TYPE_GROUP)) {
					List<Group> gl = GlobalHolder.getInstance().getGroup(
							Group.GroupType.CHATING);
					if (gl != null && gl.size() > 0) {
						populateConversation(gl);
					}
				}
				if (mCurrentTabFlag.equals(Conversation.TYPE_CONTACT)) {
					if (!isLoadedCov) {
						loadConversation();
					}
				}

				firePendingNotification();

				break;

			case UPDATE_USER_SIGN:
				long fromuidS = (Long) msg.obj;
				for (ScrollItem item : mItemList) {
					((GroupLayout) item.gp).update();
					if (item.cov.getExtId() == fromuidS
							&& Conversation.TYPE_CONTACT.equals(item.cov
									.getType())) {
						User u = GlobalHolder.getInstance().getUser(fromuidS);
						updateConversationToDB(fromuidS,
								u == null ? "" : u.getName());
						break;
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
			case REMOVE_CONVERSATION:
				break;
			}
		}

	}

}
