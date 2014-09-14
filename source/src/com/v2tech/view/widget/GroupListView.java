package com.v2tech.view.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.vo.Group;
import com.v2tech.vo.User;

public class GroupListView extends ListView {

	private Context mContext;
	private List<Group> mGroupList;
	private List<Item> mBaseList;
	private List<Item> mFilterList;
	private LocalAdapter adapter;
	private LocalFilter filter;
	private boolean mCBFlag;

	public GroupListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public GroupListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public GroupListView(Context context) {
		super(context);
		init();
	}

	private void init() {
		mGroupList = new ArrayList<Group>();
		mBaseList = new ArrayList<Item>();
		mFilterList = new ArrayList<Item>();
		adapter = new LocalAdapter();
		filter = new LocalFilter();
		mContext = this.getContext();
		this.setAdapter(adapter);
		this.setOnItemClickListener(mItemClickedListener);
	}

	public void setGroupList(List<Group> list) {
		mGroupList.clear();
		mGroupList.addAll(list);
		mBaseList.clear();
		for (int i = 0; i < list.size(); i++) {
			mBaseList.add(new GroupItem(list.get(i)));
		}
		mFilterList = mBaseList;
		adapter.notifyDataSetChanged();
	}
	
	public void setShowedCheckedBox(boolean flag) {
		mCBFlag = flag;
	}

	/**
	 * Update User online status and update user item position
	 * 
	 * @param user
	 * @param us
	 */
	public void updateUserStatus(User user, User.Status us) {
		boolean sort = false;
		for (int i = 0; i < mFilterList.size(); i++) {
			Item item = mFilterList.get(i);
			Object obj = item.getObject();
			if (obj instanceof GroupItem && ((GroupItem) obj).isExpaned) {
				sort = updateUserStatus(((GroupItem) obj), i + 1, user, us);
			}
		}
		if (sort) {
			adapter.notifyDataSetChanged();
		}
	}

	private boolean updateUserStatus(GroupItem gitem, int index, User user,
			User.Status newSt) {
		int pos = -1;
		int start = index + 1;
		int end = ((Group) gitem.getObject()).getSubSize();

		while (start < end) {
			Item item = mFilterList.get(start);
			Item endItem = mFilterList.get(end);

			if (item instanceof UserItem
					&& ((User) ((UserItem) item).getObject()).getmUserId() == user
							.getmUserId()) {
				pos = start;
			} else {
				// If sub group is expended, we should update end position
				if (((GroupItem) item).isExpaned) {
					GroupItem subGroupItem = (GroupItem) item;
					end += ((Group) subGroupItem.getObject()).getSubSize();
					updateUserStatus(subGroupItem, start + 1, user, newSt);
					start += ((Group) subGroupItem.getObject()).getSubSize();
				}
				start++;
			}

			if (endItem instanceof UserItem
					&& ((User) ((UserItem) endItem).getObject()).getmUserId() == user
							.getmUserId()) {
				pos = end;
			} else {
				// If sub group is expended, we should update end position
				if (((GroupItem) endItem).isExpaned) {
					GroupItem subGroupItem = (GroupItem) endItem;
					end += ((Group) subGroupItem.getObject()).getSubSize();
					updateUserStatus(subGroupItem, start + 1, user, newSt);
					start += ((Group) subGroupItem.getObject()).getSubSize();
				}
				start++;
			}

			if (pos != -1) {
				break;
			}
			start++;
			end--;
		}
		// Reset start and end position
		start = index + 1;
		end = ((Group) gitem.getObject()).getSubSize() - 1;
		// remove current status
		Item origin = mFilterList.remove(pos);
		// Update position;
		if (pos != -1) {
			while (start++ < end) {
				pos = start;
				Item item = mFilterList.get(start);
				User u = (User) ((UserItem) item).getObject();
				// if item is current user, always sort after current user
				if (u.getmUserId() == GlobalHolder.getInstance()
						.getCurrentUserId()) {
					continue;
				}
				if (newSt == User.Status.ONLINE) {
					if (u.getmStatus() == User.Status.ONLINE
							&& u.compareTo(user) < 0) {
						continue;
					} else {
						break;
					}
				} else if (newSt == User.Status.OFFLINE
						|| newSt == User.Status.HIDDEN) {
					if ((u.getmStatus() == User.Status.OFFLINE || u
							.getmStatus() == User.Status.HIDDEN)
							&& user.compareTo(u) > 0) {
						continue;
					} else if (u.getmStatus() != User.Status.OFFLINE
							&& u.getmStatus() != User.Status.HIDDEN) {
						continue;
					} else {
						break;
					}
				} else {
					if (u.getmStatus() == User.Status.ONLINE) {
						continue;
					} else if (u.getmStatus() == User.Status.OFFLINE
							|| u.getmStatus() == User.Status.HIDDEN) {
						break;
					} else if (u.compareTo(user) < 0) {
						continue;
					} else {
						break;
					}
				}
			}
			
			
			if (pos == mFilterList.size()) {
				mFilterList.add(origin);
			} else {
				mFilterList.add(pos, origin);
			}
			
			return true;
		}
		return false;
	}

	@Override
	public void clearTextFilter() {
		mFilterList = mBaseList;
		adapter.notifyDataSetChanged();
	}
	
	private void collapse(GroupItem item, int pos) {
		int level = item.getLevel();
		int start = pos;
		int end = mFilterList.size();
		while(++start < end) {
			Item it = mFilterList.get(start);
			if (it.getLevel() > level) {
				mFilterList.remove(start--);
				end = mFilterList.size();
				continue;
			} else if (it.getLevel() == level) {
				break;
			}
		}
		adapter.notifyDataSetChanged();
	}
	
	private void expand(GroupItem item, int pos) {
		Group g = (Group)item.getObject();
		List<User> list = g.getUsers();
		Collections.sort(list);
		for (User u : list) {
			if (mFilterList.size() == pos +1) {
				mFilterList.add(new UserItem(u, g.getLevel() + 1));
			} else {
				mFilterList.add(pos + 1, new UserItem(u, g.getLevel() + 1));
			}
		}
		adapter.notifyDataSetChanged();
	}
	
	
	
	private OnItemClickListener mItemClickedListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Item item = mFilterList.get(position);
			if (item instanceof GroupItem) {
				if (((GroupItem)item).isExpaned) {
					collapse((GroupItem)item, position);
					((GroupItem)item).isExpaned = false;
				} else {
					expand((GroupItem)item, position);
					((GroupItem)item).isExpaned = true;
				}
			}
		}
		
	};
	

	private static final int UPDATE_USER_STATUS = 1;
	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
		}

	};

	class LocalAdapter extends BaseAdapter implements Filterable {

		@Override
		public int getCount() {
			return mFilterList.size();
		}

		@Override
		public Object getItem(int position) {
			return mFilterList.get(position).getObject();
		}

		@Override
		public long getItemId(int position) {
			return mFilterList.get(position).getId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			GroupListViewAdapterItem view = null;
			if (convertView == null) {
				view = new GroupListViewAdapterItem(mContext, parent);
				convertView = view;
			} else {
				view = (GroupListViewAdapterItem) convertView;
			}

			view.update(mFilterList.get(position));
			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public Filter getFilter() {
			return filter;
		}

	}

	/**
	 * Use to query item
	 * 
	 * @author jiangzhen
	 * 
	 */
	class LocalFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			List<Item> list = new ArrayList<Item>();
			for (Group g : mGroupList) {
				search(list, g, constraint);
			}
			Collections.sort(list);
			FilterResults fr = new FilterResults();
			fr.values = list;
			fr.count = list.size();
			return fr;
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			mFilterList = (List<Item>) results.values;
		}

		@Override
		public CharSequence convertResultToString(Object resultValue) {
			return super.convertResultToString(resultValue);
		}

		void search(List<Item> list, Group g, CharSequence constraint) {
			List<User> uList = g.getUsers();
			for (User u : uList) {
				if (u.getName().contains(constraint)
						|| u.getNickName().contains(constraint)
						|| u.getArra().contains(constraint)) {
					list.add(new UserItem(u, g.getLevel() + 1));
				}
			}
			List<Group> gList = g.getChildGroup();
			for (Group subG : gList) {
				search(list, subG, constraint);
			}
		}

	}

	interface Item extends Comparable<Item> {
		public long getId();

		public Object getObject();

		public int getLevel();
	}

	class GroupItem implements Item {

		private Group mGroup;
		private boolean isExpaned;

		public GroupItem(Group group) {
			this.mGroup = group;
		}

		@Override
		public Object getObject() {
			return mGroup;
		}

		@Override
		public long getId() {
			return this.mGroup.getmGId();
		}

		@Override
		public int compareTo(Item another) {
			return 0;
		}

		@Override
		public int getLevel() {
			return mGroup.getLevel();
		}

		public boolean isExpaned() {
			return isExpaned;
		}

		public void setExpaned(boolean isExpaned) {
			this.isExpaned = isExpaned;
		}

	}

	class UserItem implements Item {

		private User mUser;
		private int mLevel;

		public UserItem(User user, int level) {
			this.mUser = user;
			this.mLevel = level;
		}

		@Override
		public Object getObject() {
			return mUser;
		}

		@Override
		public long getId() {
			return mUser.getmUserId();
		}

		@Override
		public int getLevel() {
			return mLevel;
		}

		@Override
		public int compareTo(Item another) {
			if (another instanceof UserItem) {
				return mUser.compareTo(((UserItem) another).mUser);
			} else {
				return 1;
			}
		}

	}

	/**
	 * Adapter item view
	 * @author jiangzhen
	 *
	 */
	public class GroupListViewAdapterItem extends LinearLayout {

		private Item mItem;
		private View mRoot;

		public GroupListViewAdapterItem(Context context, ViewGroup root) {
			super(context);
			init(null);
		}

		private void init(ViewGroup root) {
			mRoot = LayoutInflater.from(getContext()).inflate(
					R.layout.group_list_view_adapter_item, root,
					root == null ? false : true);

			this.addView(mRoot, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));
		}

		public void update(Item item) {
			if (item == null) {
				return;
			}
			if (this.mItem == null || this.mItem != item) {
				this.mItem = item;
			}

			if (item instanceof GroupItem) {
				updateGroupItem();
				mRoot.findViewById(R.id.group_view_root).setVisibility(View.VISIBLE);
				mRoot.findViewById(R.id.user_view_root).setVisibility(View.GONE);
			} else {
				updateUserItem();
				mRoot.findViewById(R.id.group_view_root).setVisibility(View.GONE);
				mRoot.findViewById(R.id.user_view_root).setVisibility(View.VISIBLE);
			}
		}

		private void updateUserItem() {
			User u = ((User) ((UserItem) mItem).getObject());

			ImageView mPhotoIV = (ImageView) mRoot.findViewById(R.id.user_img);
			if (u.getAvatarBitmap() != null) {
				mPhotoIV.setImageBitmap(u.getAvatarBitmap());
			} else {
				mPhotoIV.setImageResource(R.drawable.avatar);
			}
			TextView mUserNameTV = (TextView) mRoot
					.findViewById(R.id.user_name);
			TextView mUserSignatureTV = (TextView) mRoot
					.findViewById(R.id.user_signature);

			mUserNameTV.setText(u.getName());
			mUserSignatureTV.setText(u.getSignature() == null ? "" : u
					.getSignature());
			updateStatus(u.getmStatus());
			if (mCBFlag) {
				mRoot.findViewById(R.id.user_check_view).setVisibility(View.VISIBLE);
			} else {
				mRoot.findViewById(R.id.user_check_view).setVisibility(View.GONE);
			}

		}

		private void updateGroupItem() {
			Group g = ((Group) ((GroupItem) mItem).getObject());

			TextView mGroupNameTV = (TextView) mRoot
					.findViewById(R.id.group_name);
			mGroupNameTV.setText(g.getName());
			updateUserStatus();
			if (((GroupItem) mItem).isExpaned) {
				((ImageView)mRoot.findViewById(R.id.group_arrow)).setImageResource(R.drawable.arrow_down_gray);
			} else {
				((ImageView)mRoot.findViewById(R.id.group_arrow)).setImageResource(R.drawable.arrow_right_gray);
			}
			
			
			if (mCBFlag) {
				mRoot.findViewById(R.id.group_view_ck).setVisibility(View.VISIBLE);
			} else {
				mRoot.findViewById(R.id.group_view_ck).setVisibility(View.GONE);
			}
		}

		public void updateStatus(User.Status st) {
			ImageView mStatusIV = (ImageView) mRoot
					.findViewById(R.id.user_status_iv);

			switch (st) {
			case ONLINE:
				mStatusIV.setImageResource(R.drawable.online);
				break;
			case LEAVE:
				mStatusIV.setImageResource(R.drawable.leave);
				break;
			case BUSY:
				mStatusIV.setImageResource(R.drawable.busy);
				break;
			case DO_NOT_DISTURB:
				mStatusIV.setImageResource(R.drawable.do_not_distrub);
				break;
			default:
				break;
			}
			if (st == User.Status.OFFLINE || st == User.Status.HIDDEN) {
				mStatusIV.setVisibility(View.GONE);
			} else {
				mStatusIV.setVisibility(View.VISIBLE);
			}
			mStatusIV.invalidate();
		}

		public void updateUserStatus() {
			Group g = ((Group) ((GroupItem) mItem).getObject());
			((TextView) mRoot.findViewById(R.id.group_online_statist))
					.setText(g.getOnlineUserCount() + " / " + g.getUserCount());
		}

	}

}
