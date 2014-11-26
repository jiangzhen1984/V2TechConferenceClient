package com.v2tech.view.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.vo.Group;
import com.v2tech.vo.User;
import com.v2tech.vo.UserDeviceConfig.UserDeviceConfigType;

/**
 * Group List view wrapper.<br>
 * 
 * @author jiangzhen
 * 
 */
public class GroupListView extends ListView {

	private Context mContext;

	/**
	 * This list hold all group data and use to search
	 */
	private List<Group> mGroupList;

	/**
	 * This list use to hold origin data.<br>
	 * Once use clear searched text, we use this list recover origin list First
	 * time to show list, this list is same with mFilterList. If use input
	 * searched, then use mFilterList to show seached list
	 */
	private List<Item> mBaseList;

	/**
	 * This list use to display current list collection. First time this is same
	 * with mBaseList. But use input searched text, then this list is searched
	 * text.
	 */
	private List<Item> mFilterList;

	private LocalAdapter adapter;

	/**
	 * Searched filter
	 */
	private LocalFilter filter;

	/**
	 * Flag to indicate show check box or not
	 */
	private boolean mCBFlag;

	/**
	 * To holder outer listener
	 */
	private GroupListViewListener mListener;

	/**
	 * 
	 */
	private OnScrollListener mOutScrollListener;

	/**
	 * Flag to indicate doesn't show current logged in User
	 */
	private boolean mIgnoreCurrentUser;

	private int mFirstVisibleIndex;
	private int mLastVisibleIndex;

	/**
	 * Use to record group item. key group id
	 */
	private LongSparseArray<Item> mItemMap;

	/**
	 * Use to record all same user items. key: user id<br>
	 * One user can belong to different group
	 */
	private LongSparseArray<Set<Item>> mUserItemListMap;

	/**
	 * Use to record all user items which belongs to same group. key : group id
	 */
	private LongSparseArray<LongSparseArray<Item>> mGroupItemUserMap;

	/**
	 * Use to indicate current data set is filtered or not
	 */
	private boolean mIsInFilter;

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
		mItemMap = new LongSparseArray<Item>();
		mUserItemListMap = new LongSparseArray<Set<Item>>();
		mGroupItemUserMap = new LongSparseArray<LongSparseArray<Item>>();
		mGroupList = new ArrayList<Group>();
		mBaseList = new ArrayList<Item>();
		mFilterList = new ArrayList<Item>();
		adapter = new LocalAdapter();
		filter = new LocalFilter();
		mContext = this.getContext();
		this.setAdapter(adapter);
		this.setOnItemClickListener(mItemClickedListener);
		this.setOnItemLongClickListener(mItemLongClickListener);
		this.setOnScrollListener(mLocalScrollListener);
	}

	public void setGroupList(List<Group> list) {
		mGroupList.clear();
		mGroupList.addAll(list);
		mBaseList.clear();
		for (int i = 0; i < list.size(); i++) {
			mBaseList.add(getItem(list.get(i)));
		}
		mFilterList = mBaseList;
		adapter.notifyDataSetChanged();
	}

	/**
	 * set flag to show or hide item check box view
	 * 
	 * @param flag
	 *            true for show, false for hide
	 */
	public void setShowedCheckedBox(boolean flag) {
		mCBFlag = flag;
	}

	/**
	 * Set flag to hide current logged user or not
	 * 
	 * @param flag
	 */
	public void setIgnoreCurrentUser(boolean flag) {
		this.mIgnoreCurrentUser = flag;
	}

	public GroupListViewListener getListener() {
		return mListener;
	}

	/**
	 * Set item listener
	 * 
	 * @param listener
	 */
	public void setListener(GroupListViewListener listener) {
		this.mListener = listener;
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
			if (item instanceof GroupItem && ((GroupItem) item).isExpaned) {
				int end = calculateGroupEnd(((GroupItem) item).mGroup, i + 1);
				sort = updateUserPosition(((GroupItem) item), i + 1, end, user,
						us);
			}
		}
		// Should always update status, because group need update statist
		// information
		// if (sort) {
		adapter.notifyDataSetChanged();
		// }
	}

	/**
	 * Update user's checked status of item
	 * 
	 * @param u
	 * @param flag
	 */
	public void updateCheckItem(User u, boolean flag) {
		if (u == null) {
			throw new NullPointerException("user is null");
		}
		updateCheckItemWithoutNotification(u, flag);
		adapter.notifyDataSetChanged();
	}

	/**
	 * if all users were checek in a group , the group item checkBox should was
	 * checked..
	 * 
	 * @param group
	 * @param list
	 */
	public void checkBelongGroupAllChecked(Group group, List<User> list) {
		int count = 0;
		for (User u : list) {
			Set<Item> items = mUserItemListMap.get(u.getmUserId());
			if (items == null || items.size() <= 0) {
				continue;
			}

			for (Item item : items) {
				if (item.isChecked()) {
					count = count + 1;
					break;
				}
			}
		}

		Item item = mItemMap.get(group.getmGId());
		if (item != null) {
			if (count == list.size())
				item.setChecked(true);
			else
				item.setChecked(false);
		}
		adapter.notifyDataSetChanged();
	}

	/**
	 * Update all group's checked status of item
	 * 
	 * @param group
	 * @param flag
	 */
	public void updateCheckItem(Group group, boolean flag) {
		if (group == null) {
			throw new NullPointerException("Group is null");
		}
		updateCheckItemWithoutNotification(group, flag);
		adapter.notifyDataSetChanged();
	}

	/**
	 * Remote list view item according user
	 * 
	 * @param user
	 */
	public void removeItem(User user) {
		if (user == null) {
			return;
		}
		for (int i = 0; i < mFilterList.size(); i++) {
			Item item = mFilterList.get(i);
			if (item.getId() == user.getmUserId()) {
				mFilterList.remove(i);
				i--;
			}
		}

		mUserItemListMap.remove(user.getmUserId());
		Set<Group> gSet = user.getBelongsGroup();
		for (Group g : gSet) {
			LongSparseArray<Item> map = mGroupItemUserMap.get(g.getmGId());
			if (map != null) {
				map.remove(user.getmUserId());
			}
		}
		adapter.notifyDataSetChanged();
	}

	/**
	 * Add new user to group
	 * 
	 * @param group
	 * @param user
	 */
	public void addUser(Group group, User user) {
		if (group == null || user == null) {
			V2Log.e(" incorrect group:" + group + " or user: " + user);
			return;
		}

		for (int i = 0; i < mFilterList.size(); i++) {
			Item item = mFilterList.get(i);
			Object obj = item.getObject();
			if (obj instanceof Group
					&& ((Group) obj).getGroupType() == group.getGroupType()
					&& ((Group) obj).getmGId() == group.getmGId()) {
				// obj == group 不能用地址比较，老是出错。
				if (((GroupItem) item).isExpaned) {
					// Calculate group end position
					int end = calculateGroupEnd((Group) obj, i);
					int pos = calculateIndex(i, end, user, user.getmStatus());
					if (pos != -1) {
						Item userItem = this.getItem(group, user);
						((User) userItem.getObject()).updateStatus(user
								.getmStatus());
						if (pos == mFilterList.size()) {
							mFilterList.add(userItem);
						} else {
							if (pos == 0 && mFilterList.size() == 1)
								mFilterList.add(pos + 1, userItem);
							else
								mFilterList.add(pos, userItem);
						}
						break;
					}
				}
			}
		}

		adapter.notifyDataSetChanged();
	}

	public void notifiyDataSetChanged() {
		adapter.notifyDataSetInvalidated();
	}

	/**
	 * Update User group
	 * 
	 * @param user
	 * @param src
	 *            from group
	 * @param dest
	 *            to group
	 */
	public void updateUserGroup(User user, Group src, Group dest) {
		if (user == null) {
			V2Log.e("Incorrect paramters: user is null");
			return;
		}
		int removeIndex = -1;
		boolean found = false;
		boolean insert = false;
		for (int i = 0; i < mFilterList.size(); i++) {
			Item item = mFilterList.get(i);
			Object obj = item.getObject();
			if (!found && obj == src && ((GroupItem) item).isExpaned) {
				found = true;
			}

			if (dest == obj && ((GroupItem) item).isExpaned) {
				insert = true;
			}

			// If found source group and user, then remove from source group
			if (found && obj == user) {
				removeIndex = i;
			}

			if (removeIndex != -1 && insert) {
				break;
			}

		}

		if (removeIndex != -1) {
			mFilterList.remove(removeIndex);
		}
		if (insert) {
			addUser(dest, user);
		} else {
			adapter.notifyDataSetChanged();
		}
	}

	/**
	 * Mark user as selected, and call {@link Item#isChecked()} will return true
	 * 
	 * @param user
	 */
	public void selectUser(User user) {
		if (user == null) {
			return;
		}
		Set<Group> groupList = user.getBelongsGroup();
		for (Group g : groupList) {
			Item item = getItem(g, user);
			item.setChecked(true);
		}
		adapter.notifyDataSetChanged();
	}

	/**
	 * Mark user list as selected, and call {@link Item#isChecked()} will return
	 * true
	 * 
	 * @param userList
	 */
	public void selectUser(List<User> userList) {
		if (userList == null) {
			return;
		}
		for (User user : userList) {
			Set<Group> groupList = user.getBelongsGroup();
			for (Group g : groupList) {
				Item item = getItem(g, user);
				item.setChecked(true);
			}
		}
		adapter.notifyDataSetChanged();
	}

	/**
	 * Mark group as selected, and call {@link Item#isChecked()} will return
	 * true.<br>
	 * Users belong this group will mark, unless you expand this group.
	 * 
	 * @param group
	 */
	public void selectGroup(Group group) {
		if (group == null) {
			return;
		}
		Item item = getItem(group);
		item.setChecked(true);
		adapter.notifyDataSetChanged();
	}

	/**
	 * Use to update user signature or avatar
	 * 
	 * @param user
	 */
	public void updateUser(User user) {
		// FIXME optimze for avatar
		adapter.notifyDataSetChanged();
	}

	/**
	 * Update user item check status according to flag
	 * 
	 * @param u
	 * @param flag
	 */
	private void updateCheckItemWithoutNotification(User u, boolean flag) {
		Set<Item> list = mUserItemListMap.get(u.getmUserId());
		if (list == null || list.size() <= 0) {
			return;
		}
		for (Item item : list) {
			item.setChecked(flag);
		}
	}

	/**
	 * Update group item check status according to flag
	 * 
	 * @param group
	 * @param flag
	 */
	private void updateCheckItemWithoutNotification(Group group, boolean flag) {
		Item item = mItemMap.get(group.getmGId());
		if (item != null) {
			item.setChecked(flag);
		}
		List<User> list = group.getUsers();
		for (User u : list) {
			updateCheckItem(u, flag);
		}
		List<Group> subGroupList = group.getChildGroup();
		for (int i = 0; i < subGroupList.size(); i++) {
			updateCheckItemWithoutNotification(subGroupList.get(i), flag);
		}
	}

	/**
	 * Update user position according to new user status
	 * 
	 * @param gitem
	 *            group item which user belongs and expanded
	 * @param gstart
	 *            first child position of group
	 * @param gend
	 *            group end position
	 * @param user
	 * @param newSt
	 * @return
	 */
	private boolean updateUserPosition(GroupItem gitem, int gstart, int gend,
			User user, User.Status newSt) {
		int pos = -1;
		int start = gstart;
		int end = gend;

		while (start <= end && mFilterList.size() > start) {
			Item item = mFilterList.get(start);
			Item endItem = mFilterList.get(end);

			if (item instanceof UserItem) {
				if (((User) ((UserItem) item).getObject()).getmUserId() == user
						.getmUserId()) {
					pos = start;
					// ((User) ((UserItem)
					// item).getObject()).updateStatus(newSt);
					// user.updateStatus(newSt);
				}
			} else {
				// If sub group is expended, we should update end position
				if (((GroupItem) item).isExpaned) {
					GroupItem subGroupItem = (GroupItem) item;
					int subGroupEndIndex = calculateGroupEnd(
							subGroupItem.mGroup, start);
					updateUserPosition(subGroupItem, start + 1,
							subGroupEndIndex, user, newSt);
					start += subGroupEndIndex;
				}
				start++;
			}

			if (endItem instanceof UserItem) {
				if (((User) ((UserItem) endItem).getObject()).getmUserId() == user
						.getmUserId()) {
					pos = end;
					// ((User) ((UserItem)
					// item).getObject()).updateStatus(newSt);
					// user.updateStatus(newSt);
				}
			} else {
				// If sub group is expended, we should update end position
				if (((GroupItem) endItem).isExpaned) {
					GroupItem subGroupItem = (GroupItem) endItem;
					int subGroupEndIndex = calculateGroupEnd(
							subGroupItem.mGroup, start);
					updateUserPosition(subGroupItem, start + 1,
							subGroupEndIndex, user, newSt);
					start += subGroupEndIndex;
				}
				start++;
			}

			if (pos != -1) {
				break;
			}
			start++;
			end--;
		}

		// Update user new position;
		if (pos != -1) {
			// Reset start and end position
			start = gstart;
			end = gend;
			// remove current status
			Item origin = mFilterList.remove(pos);

			pos = calculateIndex(start, end, user, newSt);

			if (pos != -1) {
				if (pos == mFilterList.size()) {
					mFilterList.add(origin);
				} else {
					mFilterList.add(pos, origin);
				}
			}

			return true;
		}
		return false;
	}

	/**
	 * Calculate end index of current group
	 * 
	 * @param group
	 * @param startIndex
	 *            current group index in list
	 * @return the last index of child position, if group child size is 0,
	 *         return current group index
	 */
	private int calculateGroupEnd(Group group, int startIndex) {
		if (startIndex >= mFilterList.size()) {
			return -1;
		}

		if (startIndex == mFilterList.size()) {
			return mFilterList.size();
		} else if (mFilterList.size() == 1) {
			return mFilterList.size();
		}

		int pos = startIndex;
		while (startIndex < mFilterList.size()) {
			Item item = mFilterList.get(startIndex);
			Object obj = item.getObject();
			// If current item is group and group level same and is not self,
			// then group end is this item index - 1
			if ((obj instanceof Group && group.getLevel() == ((Group) obj)
					.getLevel()) && obj != group) {
				pos = mFilterList.size() - startIndex;
				break;
			}
			pos = startIndex;

			startIndex++;
		}
		return pos;
	}

	/**
	 * Calculate user comfortable position which in list
	 * 
	 * @param start
	 *            start index which belongs group
	 * @param end
	 *            end index which belongs group
	 * @param user
	 * @param ust
	 * @return
	 */
	private int calculateIndex(int start, int end, User user, User.Status ust) {
		int pos = -1;
		if (start < 0 || start > mFilterList.size() || end > mFilterList.size()) {
			return -1;
		}

		// If start equal list size, return position of end group
		if (start == mFilterList.size() && end == mFilterList.size()) {
			return start;
		} else if (start == end) {
			return end + 1;
		}

		while (start <= end) {
			pos = start;
			if (start + 1 == mFilterList.size())
				break;
			Item item = mFilterList.get(start++);
			if (item instanceof GroupItem) {
				continue;
			}
			User u = (User) ((UserItem) item).getObject();
			// if item is current user, always sort after current user
			if (u == null
					|| u.getmUserId() == GlobalHolder.getInstance()
							.getCurrentUserId()) {
				continue;
			}
			if (ust == User.Status.ONLINE) {
				if (u.getmStatus() == User.Status.ONLINE
						&& u.compareTo(user) < 0) {
					continue;
				} else {
					break;
				}
			} else if (ust == User.Status.OFFLINE || ust == User.Status.HIDDEN) {
				if ((u.getmStatus() == User.Status.OFFLINE || u.getmStatus() == User.Status.HIDDEN)
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

		return pos;
	}

	private Item getItem(Group g) {
		Item item = mItemMap.get(g.getmGId());
		if (item == null) {
			item = new GroupItem(g);
			Group parent = g.getParent();
			if (parent != null) {
				Item itemParent = mItemMap.get(parent.getmGId());
				if (itemParent != null)
					item.setChecked(itemParent.isChecked());
			}
			mItemMap.put(g.getmGId(), item);
		}
		return item;
	}

	/**
	 */
	private Item getItem(Group g, User u) {
		Item groupItem = getItem(g);

		LongSparseArray<Item> map = mGroupItemUserMap.get(g.getmGId());
		if (map == null) {
			map = new LongSparseArray<Item>();
			mGroupItemUserMap.put(g.getmGId(), map);
		}
		Item item = map.get(u.getmUserId());
		if (item == null) {
			item = new UserItem(u, g.getLevel() + 1);
			// Initialize user item check status.
			// If exist one group checked, then user item should be checked
			boolean checked = false;
			Set<Group> parents = u.getBelongsGroup();
			for (Group parent : parents) {
				Item parentItem = getItem(parent);
				if (parentItem.isChecked()) {
					checked = true;
					break;
				}
			}
			// Update check status according group status.
			item.setChecked(checked);
			map.put(u.getmUserId(), item);

		}

		Set<Item> itemList = mUserItemListMap.get(u.getmUserId());
		if (itemList == null) {
			itemList = new HashSet<Item>();
			mUserItemListMap.put(u.getmUserId(), itemList);
		}
		itemList.add(item);

		return item;
	}

	@Override
	public void setFilterText(String filterText) {
		if (TextUtils.isEmpty(filterText)) {
			clearTextFilter();
		} else {
			if (this.isTextFilterEnabled()) {
				mIsInFilter = true;
				if (this.adapter instanceof Filterable) {
					Filter filter = ((Filterable) this.adapter).getFilter();
					filter.filter(filterText);
				}
			}
		}
	}

	@Override
	public void clearTextFilter() {
		if (this.isTextFilterEnabled() && mIsInFilter) {
			mIsInFilter = false;
			if (this.adapter instanceof Filterable) {
				Filter filter = ((Filterable) this.adapter).getFilter();
				filter.filter("");
			}
		}
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		if (listener != mItemClickedListener) {
			throw new RuntimeException(
					"Can not set others item listeners User setListener instead");
		}
		super.setOnItemClickListener(listener);
	}

	@Override
	public void setOnItemLongClickListener(OnItemLongClickListener listener) {
		if (listener != mItemLongClickListener) {
			throw new RuntimeException(
					"Can not set others item listeners Use setListener instead");
		}
		super.setOnItemLongClickListener(listener);
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		if (this.adapter != adapter) {
			throw new RuntimeException("Do not permit set adatper");
		}
		super.setAdapter(adapter);
	}

	@Override
	public void setOnScrollListener(OnScrollListener l) {
		if (l != this.mLocalScrollListener) {
			mOutScrollListener = l;
		} else {
			super.setOnScrollListener(l);
		}
	}

	/**
	 * collapse current expanded group
	 * 
	 * @param item
	 * @param pos
	 */
	private void collapse(GroupItem item, int pos) {
		int level = item.getLevel();
		int start = pos;
		int end = mFilterList.size();
		while (++start < end) {
			Item it = mFilterList.get(start);
			if (it.getLevel() > level) {
				mFilterList.remove(start--);
				end = mFilterList.size();
				continue;
			} else if (it.getLevel() == level) {
				break;
			}
		}
	}

	/**
	 * Expand group item
	 * 
	 * @param item
	 * @param pos
	 *            position of want to expand group
	 */
	private int expand(GroupItem item, int pos) {
		Group g = (Group) item.getObject();
		List<Group> subGroupList = g.getChildGroup();
		// DO not user for(Group g:list) concurrency problem
		for (int i = subGroupList.size() - 1; i >= 0; i--) {
			Group subG = subGroupList.get(i);
			GroupItem groupItem = (GroupItem) getItem(subG);
			if (mFilterList.size() == pos + 1) {

				mFilterList.add(groupItem);
			} else {
				mFilterList.add(pos + 1, groupItem);
			}
			pos++;
			if (groupItem.isExpaned()) {
				pos = expand(groupItem, pos);
			}
		}

		List<User> list = g.getUsers();
		Collections.sort(list);
		for (int i = 0; i < list.size(); i++) {
			User u = list.get(i);
			// check ignore current logged use flag
			if (mIgnoreCurrentUser
					&& u.getmUserId() == GlobalHolder.getInstance()
							.getCurrentUserId()) {
				continue;
			}

			if (mFilterList.size() == pos + 1) {
				mFilterList.add(getItem(g, u));
			} else {
				mFilterList.add(pos + 1, getItem(g, u));
			}
			pos++;
		}
		return pos;
	}

	/**
	 * Local item clicked listener
	 */
	private OnItemClickListener mItemClickedListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Item item = mFilterList.get(position);
			if (item instanceof GroupItem) {
				if (((GroupItem) item).isExpaned) {
					collapse((GroupItem) item, position);
					((GroupItem) item).isExpaned = false;
				} else {
					expand((GroupItem) item, position);
					((GroupItem) item).isExpaned = true;
				}
				adapter.notifyDataSetChanged();
			}
			if (mListener != null) {
				mListener.onItemClicked(parent, view, position, id, item);
				if (mCBFlag) {
					((GroupListViewAdapterItem) view).updateCheckBox();
				}
			}

		}

	};

	private OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			Item item = mFilterList.get(position);

			boolean flag = false;
			if (mListener != null) {
				flag = mListener.onItemLongClick(parent, view, position, id,
						item);
				if (mCBFlag) {
					((GroupListViewAdapterItem) view).updateCheckBox();
				}
			}
			return flag;
		}

	};

	private OnClickListener mCheckBoxListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Item item = (Item) v.getTag();
			if (mListener != null) {
				mListener.onCheckboxClicked(v, item);
			}
		}

	};

	/**
	 * 
	 */
	private OnScrollListener mLocalScrollListener = new OnScrollListener() {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			if (mOutScrollListener != null) {
				mOutScrollListener.onScrollStateChanged(view, scrollState);
			}
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if (mOutScrollListener != null) {
				mOutScrollListener.onScroll(view, firstVisibleItem,
						visibleItemCount, totalItemCount);
			}

			mFirstVisibleIndex = firstVisibleItem;
			mLastVisibleIndex = mFirstVisibleIndex + visibleItemCount;

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

			view.update(mFilterList.get(position), !mIsInFilter);
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
			FilterResults fr = new FilterResults();
			List<Item> list = null;
			if (constraint == null || constraint.toString().isEmpty()) {
				list = mBaseList;
			} else {
				list = new ArrayList<Item>();
				for (Group g : mGroupList) {
					search(list, g, constraint);
				}
				Collections.sort(list);
			}

			fr.values = list;
			fr.count = list.size();
			return fr;
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			if (results.values != null) {
				mFilterList = (List<Item>) results.values;
				adapter.notifyDataSetChanged();
			} else {
				// TODO toast search error
			}
		}

		@Override
		public CharSequence convertResultToString(Object resultValue) {
			return super.convertResultToString(resultValue);
		}

		void search(List<Item> list, Group g, CharSequence constraint) {
			List<User> uList = g.getUsers();
			for (User u : uList) {
				if (u.getName().contains(constraint)
						|| u.getArra().contains(constraint)) {
					list.add(getItem(g, u));
				}
			}
			List<Group> gList = g.getChildGroup();
			for (Group subG : gList) {
				search(list, subG, constraint);
			}
		}

	}

	/**
	 * Item response Listener
	 * 
	 * @author jiangzhen
	 * 
	 */
	public interface GroupListViewListener {
		public void onItemClicked(AdapterView<?> parent, View view,
				int position, long id, Item item);

		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id, Item item);

		public void onCheckboxClicked(View view, Item item);
	}

	public interface Item extends Comparable<Item> {
		public long getId();

		public Object getObject();

		public int getLevel();

		public boolean isChecked();

		void setChecked(boolean flag);
	}

	class GroupItem implements Item {

		private Group mGroup;
		private boolean isExpaned;
		private boolean isChecked;
		private boolean searchedCurrentUser;
		private boolean existCurrentUser;

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

		@Override
		public boolean isChecked() {
			return isChecked;
		}

		@Override
		public void setChecked(boolean flag) {
			isChecked = flag;
		}

		public boolean isSearchedCurrentUser() {
			return searchedCurrentUser;
		}

		public void setSearchedCurrentUser(boolean searchedCurrentUser) {
			this.searchedCurrentUser = searchedCurrentUser;
		}

		public boolean isExistCurrentUser() {
			return existCurrentUser;
		}

		public void setExistCurrentUser(boolean existCurrentUser) {
			this.existCurrentUser = existCurrentUser;
		}

	}

	class UserItem implements Item {

		private User mUser;
		private int mLevel;
		private boolean isChecked;

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
		public boolean isChecked() {
			return isChecked;
		}

		@Override
		public void setChecked(boolean flag) {
			isChecked = flag;
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
	 * 
	 * @author jiangzhen
	 * 
	 */
	class GroupListViewAdapterItem extends LinearLayout {

		private Item mItem;
		private View mRoot;
		private CheckBox mCb;

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

		public void update(Item item, boolean paddingFlag) {
			if (item == null) {
				return;
			}
			if (this.mItem == null || this.mItem != item) {
				this.mItem = item;
			}

			if (item instanceof GroupItem) {
				updateGroupItem();
				View groupRoot = mRoot.findViewById(R.id.group_view_root);
				groupRoot.setVisibility(View.VISIBLE);
				if (paddingFlag) {
					groupRoot.setPadding((item.getLevel() - 1) * 35, 0, 0, 0);
					// For show search result
				} else {
					groupRoot.setPadding(35, 0, 0, 0);
				}
				groupRoot.invalidate();
				mRoot.findViewById(R.id.user_view_root)
						.setVisibility(View.GONE);
			} else {

				View userRoot = mRoot.findViewById(R.id.user_view_root);
				userRoot.setVisibility(View.VISIBLE);
				if (paddingFlag) {
					userRoot.setPadding((item.getLevel() - 1) * 35, 0, 0, 0);
					// For show search result
				} else {
					userRoot.setPadding(35, 0, 0, 0);
				}
				updateUserItem();
				mRoot.findViewById(R.id.group_view_root).setVisibility(
						View.GONE);
			}

			// Update checkbox tag
			if (mCb != null) {
				mCb.setTag(item);
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
			mUserSignatureTV.setText(u.getSignature() == null ? "" : u
					.getSignature());
			int[] nameLo = new int[2];
			mUserSignatureTV.getLocationInWindow(nameLo);
			mUserSignatureTV.setSingleLine(true);
			mUserSignatureTV.setEllipsize(TruncateAt.END);

			boolean isFriend = GlobalHolder.getInstance().isFriend(u);
			if (isFriend) {
				if (!TextUtils.isEmpty(u.getNickName()))
					mUserNameTV.setText(u.getNickName());
				else
					mUserNameTV.setText(u.getName());
			} else
				mUserNameTV.setText(u.getName());

			updateStatus(u.getDeviceType(), u.getmStatus());
			if (mCBFlag) {
				mCb = (CheckBox) mRoot.findViewById(R.id.user_check_view);
				mCb.setVisibility(View.VISIBLE);
				mCb.setChecked(mItem.isChecked());
				mCb.setOnClickListener(mCheckBoxListener);

				int maxWidth = 0;
				int[] cbL = new int[2];
				mCb.getLocationInWindow(cbL);
				maxWidth = cbL[0] - nameLo[0] - 35;
				mUserSignatureTV.setMaxWidth(maxWidth);

			} else {
				mRoot.findViewById(R.id.user_check_view).setVisibility(
						View.GONE);
				int maxWidth = this.getWidth() - nameLo[0] - 35;
				mUserSignatureTV.setMaxWidth(maxWidth);
			}

		}

		private void updateGroupItem() {
			Group g = ((Group) ((GroupItem) mItem).getObject());

			TextView mGroupNameTV = (TextView) mRoot
					.findViewById(R.id.group_name);
			mGroupNameTV.setText(g.getName());
			updateUserStatus();
			if (((GroupItem) mItem).isExpaned) {
				((ImageView) mRoot.findViewById(R.id.group_arrow))
						.setImageResource(R.drawable.arrow_down_gray);
			} else {
				((ImageView) mRoot.findViewById(R.id.group_arrow))
						.setImageResource(R.drawable.arrow_right_gray);
			}

			if (mCBFlag) {
				mCb = (CheckBox) mRoot.findViewById(R.id.group_view_ck);
				mCb.setVisibility(View.VISIBLE);
				mCb.setChecked(mItem.isChecked());
				mCb.setOnClickListener(mCheckBoxListener);
			} else {
				mRoot.findViewById(R.id.group_view_ck).setVisibility(View.GONE);
			}
		}

		public void updateStatus(User.DeviceType dType, User.Status st) {
			ImageView mStatusIV = (ImageView) mRoot
					.findViewById(R.id.user_status_iv);
			if (dType == User.DeviceType.CELL_PHONE) {
				mStatusIV.setImageResource(R.drawable.cell_phone_user);
			} else {
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
			}

			ImageView mPhotoIV = (ImageView) mRoot.findViewById(R.id.user_img);
			TextView mUserNameTV = (TextView) mRoot
					.findViewById(R.id.user_name);
			if (st == User.Status.OFFLINE || st == User.Status.HIDDEN) {
				mStatusIV.setVisibility(View.GONE);
				mPhotoIV.setColorFilter(Color.GRAY, PorterDuff.Mode.LIGHTEN);
				mUserNameTV.setTextColor(getContext().getResources().getColor(
						R.color.contacts_user_view_item_color_offline));
			} else {
				mStatusIV.setVisibility(View.VISIBLE);
				mPhotoIV.clearColorFilter();
				mUserNameTV.setTextColor(getContext().getResources().getColor(
						R.color.conf_create_contacts_user_view_item_color));
			}

			mStatusIV.invalidate();
		}

		public void updateCheckBox() {
			if (!mCBFlag) {
				throw new RuntimeException(
						" Please set setShowedCheckedBox first");
			}
			if (mCb != null) {
				mCb.setChecked(mItem.isChecked());
			}
		}

		public void updateUserStatus() {
			GroupItem gi = ((GroupItem) mItem);
			Group g = ((Group) gi.getObject());
			TextView tv = ((TextView) mRoot
					.findViewById(R.id.group_online_statist));
			int count = g.getUserCount();
			int onlineCount = 0;
			Set<User> sets = g.getOnlineUserSet();
			for (User u : sets) {
				User.Status status = u.getmStatus();
				User user = GlobalHolder.getInstance().getUser(u.getmUserId());
				if (user != null) {
					status = user.getmStatus();
				}
				if ((status == User.Status.ONLINE || status == User.Status.BUSY
						|| status == User.Status.DO_NOT_DISTURB || status == User.Status.LEAVE)
						&& ((!mIgnoreCurrentUser || (mIgnoreCurrentUser && u
								.getmUserId() != GlobalHolder.getInstance()
								.getCurrentUserId())))) {
					onlineCount++;
				}
			}

			if (!gi.searchedCurrentUser && mIgnoreCurrentUser) {
				if (g.findUser(GlobalHolder.getInstance().getCurrentUser()) != null) {
					gi.existCurrentUser = true;
				}

				gi.searchedCurrentUser = true;
			}

			tv.setText(onlineCount
					+ " / "
					+ ((mIgnoreCurrentUser && gi.existCurrentUser) ? count - 1
							: count));
			tv.invalidate();
		}
	}

}
