package com.v2tech.view;

import java.util.ArrayList;
import java.util.List;
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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.logic.Conference;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.Group;
import com.v2tech.logic.Group.GroupType;
import com.v2tech.logic.NetworkStateCode;
import com.v2tech.logic.User;
import com.v2tech.service.ConferenceService;
import com.v2tech.util.BitmapUtil;
import com.v2tech.util.V2Log;
import com.v2tech.view.conference.GroupLayout;
import com.v2tech.view.conference.VideoActivityV2;
import com.v2tech.view.vo.ConferenceConversation;
import com.v2tech.view.vo.ContactConversation;
import com.v2tech.view.vo.Conversation;
import com.v2tech.view.vo.CrowdConversation;

public class ConversationsTabFragment extends Fragment {

	private static final int FILL_CONFS_LIST = 2;
	private static final int UPDATE_USER_SIGN = 8;
	private static final int UPDATE_USER_AVATAR = 9;
	private static final int UPDATE_CONVERSATION = 10;
	private static final int UPDATE_SEARCHED_LIST = 11;
	private static final int REMOVE_CONVERSATION = 12;

	private static final int SUB_ACTIVITY_CODE_VIDEO_ACTIVITY = 0;
	private static final int SUB_ACTIVITY_CODE_CREATE_CONF = 100;

	private Tab1BroadcastReceiver receiver = new Tab1BroadcastReceiver();
	private IntentFilter intentFilter;
	private boolean mIsStartedSearch;

	private List<Group> mConferenceList;

	private EditText mSearchTextET;

	private ConfsHandler mHandler = new ConfsHandler();

	private ImageView mLoadingImageIV;

	private boolean isLoaded = false;
	private boolean isLoadedCov = false;
	private boolean isInMeeting = false;

	private View rootView;

	private List<ScrollItem> mItemList = new CopyOnWriteArrayList<ScrollItem>();
	private List<ScrollItem> mCacheItemList;

	private Context mContext;

	private LinearLayout networkNotificationContainer;

	private ListView mConversationsListView;

	private ImageView mFeatureIV;

	private ConversationsAdapter adapter = new ConversationsAdapter();

	private ConferenceService cb = new ConferenceService();

	private String mCurrentTabFlag;

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
			mFeatureIV = (ImageView) rootView
					.findViewById(R.id.conversation_features);
			mFeatureIV.setOnClickListener(mFeatureButtonListener);

			mSearchTextET.setOnTouchListener(mTouchListener);
			rootView.setOnTouchListener(mTouchListener);
			mConversationsListView.setOnTouchListener(mTouchListener);

			if (mCurrentTabFlag.equals(Conversation.TYPE_GROUP)
				) {
				mFeatureIV.setVisibility(View.INVISIBLE);
			}

			TextView tv = (TextView) rootView.findViewById(R.id.fragment_title);
			if (mCurrentTabFlag.equals(Conversation.TYPE_CONFERNECE)) {
				tv.setText(R.string.tab_conference_name);
			} else if (mCurrentTabFlag.equals(Conversation.TYPE_CONTACT)) {
				tv.setText(R.string.tab_conversation_name);
			} else if (mCurrentTabFlag.equals(Conversation.TYPE_GROUP)) {
				tv.setText(R.string.tab_group_name);
			}

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
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_USER_AVATAR_CHANGED_NOTIFICATION);
			intentFilter
					.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
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
			}

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
		if (requestCode == SUB_ACTIVITY_CODE_VIDEO_ACTIVITY) {
			isInMeeting = false;
			// Message.obtain(mHandler, REQUEST_EXIT_CONF, currentConfId)
			// .sendToTarget();
		} else if (requestCode == SUB_ACTIVITY_CODE_CREATE_CONF) {
			if (resultCode == Activity.RESULT_CANCELED) {
				return;
			}

			if (resultCode == Activity.RESULT_OK) {
				long gid = data.getLongExtra("newGid", 0);
				Group g = GlobalHolder.getInstance().getGroupById(
						GroupType.CONFERENCE, gid);
				if (g != null) {
					Conversation cov = new ConferenceConversation(g);
					final GroupLayout gp = new GroupLayout(this.getActivity(),
							cov);
					gp.setOnClickListener(mEnterConfListener);

					mItemList.add(0, new ScrollItem(cov, gp));
					adapter.notifyDataSetChanged();

					Intent i = new Intent(getActivity(), VideoActivityV2.class);
					i.putExtra("gid", g.getmGId());
					startActivityForResult(i, SUB_ACTIVITY_CODE_VIDEO_ACTIVITY);

				} else {
					V2Log.e(" Can not find created group id :" + gid);
				}
			}
		}
	}

	private void populateConversation(final Group g, boolean flag) {
		Conversation cov = new ConferenceConversation(g);
		final GroupLayout gp = new GroupLayout(this.getActivity(), cov);
		gp.updateNotificator(flag);
		if (mCurrentTabFlag.equals(Conversation.TYPE_CONFERNECE)) {
			gp.setOnClickListener(mEnterConfListener);
		} else if (mCurrentTabFlag.equals(Conversation.TYPE_GROUP)) {

			gp.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					Intent i = new Intent(
							PublicIntent.START_CONVERSACTION_ACTIVITY);
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

	private OnClickListener mConferenceCreateButtonListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			Intent i = new Intent(PublicIntent.START_CONFERENCE_CREATE_ACTIVITY);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			startActivityForResult(i, SUB_ACTIVITY_CODE_CREATE_CONF);
			if (pw != null) {
				pw.dismiss();
			}
		}

	};

	private OnTouchListener mTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (mSearchTextET != null) {
				InputMethodManager imm = (InputMethodManager) getActivity()
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mSearchTextET.getWindowToken(), 0);
			}
			return false;
		}

	};

	private OnClickListener mEnterConfListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (isInMeeting) {
				return;
			}
			isInMeeting = true;
			GroupLayout gp = (GroupLayout) v;

			Intent i = new Intent(getActivity(), VideoActivityV2.class);
			i.putExtra("gid", gp.getGroupId());
			startActivityForResult(i, SUB_ACTIVITY_CODE_VIDEO_ACTIVITY);

			gp.updateNotificator(false);

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

	};

	private PopupWindow pw;
	private OnClickListener mFeatureButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {

			if (pw != null) {
				pw.showAsDropDown(view);
				return;
			}
			LinearLayout root = new LinearLayout(mContext);
			root.setOrientation(LinearLayout.VERTICAL);
			if (mCurrentTabFlag.equals(Conversation.TYPE_CONTACT)) {
				View v = getMenuButtonView(R.string.conversation_popup_menu_video_call_button);
				root.addView(v);
				
				v = getMenuButtonView(R.string.conversation_popup_menu_call_button);
				root.addView(v);
				
				v = getMenuButtonView(R.string.conversation_popup_menu_sms_call_button);
				root.addView(v);
				
				v = getMenuButtonView(R.string.conversation_popup_menu_email_button);
				root.addView(v);
				v = getMenuButtonView(R.string.conversation_popup_menu_files_button);
				root.addView(v);
				v = getMenuButtonView(R.string.conversation_popup_menu_setting_button);
				root.addView(v);
				
			} else {
				root.setBackgroundColor(mContext.getResources().getColor(
						R.color.confs_title_bg));
				View v = getMenuButtonView(R.string.conference_create_title);
				v.setOnClickListener(mConferenceCreateButtonListener);
				root.addView(v);
			}
			pw = new PopupWindow(root, ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT, true);
			pw.setBackgroundDrawable(new ColorDrawable(R.color.confs_title_bg));
			pw.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss() {
					pw.dismiss();
				}

			});

			pw.setFocusable(true);
			pw.setTouchable(true);
			pw.setOutsideTouchable(true);
			pw.showAsDropDown(view);

		}

	};
	
	private View getMenuButtonView(int resId) {
		LinearLayout l = new LinearLayout(mContext);
		
		TextView tv = new TextView(mContext);
		tv.setTextSize(18);
		tv.setGravity(Gravity.CENTER);
		tv.setText(resId);

		LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		ll.setMargins(5, 20, 5, 20);
		ll.gravity = Gravity.CENTER;
		tv.setLayoutParams(ll);
		
		l.addView(tv);
		l.setLayoutParams(ll);
		
		LinearLayout line =  new LinearLayout(mContext);
		line.setBackgroundColor(Color.GRAY);
		
		l.addView(line,  new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 1));
		
		return  l;
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
			Conversation cov = (ContactConversation) GlobalHolder.getInstance()
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

	private OnLongClickListener mLongClickListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(final View v) {

			final Conversation cov = (Conversation) v.getTag();
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle(cov.getName()).setItems(
					new String[] { mContext.getResources().getString(
							R.string.conversations_delete_conversaion) },
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
					mConferenceList = GlobalHolder.getInstance().getGroup(
							Group.GroupType.CONFERENCE);
					if (mConferenceList != null && mConferenceList.size() > 0) {
						if (!isLoaded) {
							populateConversation(mConferenceList);
							isLoaded = true;
						}
					}
				} else if (mCurrentTabFlag.equals(Conversation.TYPE_GROUP)) {
					mConferenceList = GlobalHolder.getInstance().getGroup(
							Group.GroupType.CHATING);
					if (mConferenceList != null && mConferenceList.size() > 0) {
						if (!isLoaded) {
							populateConversation(mConferenceList);
							isLoaded = true;
						}
					}
				}
				if (mCurrentTabFlag.equals(Conversation.TYPE_CONTACT)) {
					if (!isLoadedCov) {
						loadConversation();
					}
				}
				if (mLoadingImageIV.getParent() != null) {
					((ViewGroup) mLoadingImageIV.getParent())
							.removeView(mLoadingImageIV);
				}

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
			case REMOVE_CONVERSATION:
				break;
			}
		}

	}

}
