package com.bizcom.vc.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
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
import com.bizcom.util.SearchUtils;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vo.Group;
import com.bizcom.vo.User;
import com.v2tech.R;

/**
 * Group List view wrapper.<br>
 * 
 * @author jiangzhen
 * 
 */
public class GroupListView extends ListView {

	private static final String TAG = "GroupListView";

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
	private List<ItemData> mBaseList;

	/**
	 * This list use to display current list collection. First time this is same
	 * with mBaseList. But use input searched text, then this list is searched
	 * text.
	 */
	private List<ItemData> mFilterList;

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
	private LongSparseArray<ItemData> mItemMap;

	/**
	 * Use to record all same user items. key: user id<br>
	 * One user can belong to different group
	 */
	private LongSparseArray<Set<ItemData>> mUserItemListMap;

	/**
	 * Use to record all user items which belongs to same group. key : group id
	 * 提供一个从User转换成UserItemData的缓存
	 */
	private LongSparseArray<LongSparseArray<ItemData>> mUserItemDataMapOfMap;

	/**
	 * Use to indicate current data set is filtered or not
	 * 只有listView要显示的数据，可以是GroupItemData或是UserItemData
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
		mItemMap = new LongSparseArray<ItemData>();
		mUserItemListMap = new LongSparseArray<Set<ItemData>>();
		mUserItemDataMapOfMap = new LongSparseArray<LongSparseArray<ItemData>>();
		mGroupList = new ArrayList<Group>();
		mBaseList = new ArrayList<ItemData>();
		mFilterList = new ArrayList<ItemData>();
		adapter = new LocalAdapter();
		filter = new LocalFilter();
		mContext = this.getContext();
		this.setAdapter(adapter);
		this.setOnItemClickListener(mItemClickedListener);
		this.setOnItemLongClickListener(mItemLongClickListener);
		this.setOnScrollListener(mLocalScrollListener);
	}
	
	public void initCreateMode() {
		setShowedCheckedBox(true);
		setTextFilterEnabled(true);
		setIgnoreCurrentUser(true);
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

	public void setContactsGroupList(List<Group> list) {
		mGroupList.clear();
		mGroupList.addAll(list);
		mBaseList.clear();
		for (int i = 0; i < list.size(); i++) {
			GroupItemData item = (GroupItemData) getItem(list.get(i));
			mBaseList.add(item);
			if (item.isExpanded) {
				int startPos = getGroupItemPos(item);
				expand(item, startPos);
			}
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
//		boolean sort = false;
		for (int i = 0; i < mFilterList.size(); i++) {
			ItemData item = mFilterList.get(i);
			if (item instanceof GroupItemData
					&& ((GroupItemData) item).isExpanded) {
				Group temp = ((GroupItemData) item).mGroup;
				int start = calculateGroupStartIndex(temp);
				int end = calculateGroupEnd(temp, i);
				int pos = updateUserPosition(((GroupItemData) item), start, end,
						user, us);
				V2Log.d("GroupListView", user.getmUserId() + " the user "+ user.getName() +" add pos is : " + pos +
						" state is : " + us.name() + " start : " + start + " - end : " + end);
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
		GroupItemData targetItem = (GroupItemData) getItem(group);
		for (Group temp : mGroupList) {
			checkBelongGroupAllChecked(temp, temp.getUsers());
			if(!flag){
				GroupItemData tempItem = (GroupItemData) getItem(temp);
				if(tempItem.getLevel() < targetItem.getLevel() &&
						temp.getChildGroup().contains(group)){
					tempItem.setChecked(false);
				}
			}
		}
		adapter.notifyDataSetChanged();
	}

	/**
	 * Update user's checked status of item
	 * 
	 * @param flag
	 */
	public void updateUserItemcCheck(List<User> user, boolean flag) {
		for (int i = 0; i < user.size(); i++) {
			updateCheckItem(user.get(i), false);
		}

		for (Group group : mGroupList) {
			checkBelongGroupAllChecked(group, user);
		}
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
			Set<ItemData> items = mUserItemListMap.get(u.getmUserId());
			if (items == null || items.size() <= 0) {
				continue;
			}

			for (ItemData item : items) {
				if (item.isChecked()) {
					count = count + 1;
					break;
				}
			}
		}
		
		List<Group> childGroup = group.getChildGroup();
		for (Group child : childGroup) {
			ItemData childGroupItem = getItem(child);
			if(childGroupItem.isChecked()){
				count = count + 1;
			}
		}

		ItemData item = mItemMap.get(group.getmGId());
		if (item != null) {
			int allCount = list.size() + group.getChildGroup().size();
			if (count == allCount && allCount != 0)
				item.setChecked(true);
			else
				item.setChecked(false);
		}
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
			ItemData item = mFilterList.get(i);
			if (item.getId() == user.getmUserId()) {
				mFilterList.remove(i);
				i--;
			}
		}

		// 从所用用户列表中删除该user
		mUserItemListMap.remove(user.getmUserId());
		// 从该user所属的组中删除该user
		Set<Group> gSet = user.getBelongsGroup();
		for (Group g : gSet) {
			LongSparseArray<ItemData> userGroup = mUserItemDataMapOfMap.get(g
					.getmGId());
			if (userGroup != null) {
				userGroup.remove(user.getmUserId());
			}
		}

		// 更新listView的显示
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
			ItemData item = mFilterList.get(i);
			Object obj = item.getObject();
			if (obj instanceof Group
					&& ((Group) obj).getGroupType() == group.getGroupType()
					&& ((Group) obj).getmGId() == group.getmGId()) {
				// obj == group 不能用地址比较，老是出错。
				if (((GroupItemData) item).isExpanded) {
					// Calculate group end position
					int startPos = calculateAddGroupStartIndex(group);
					int endPos = calculateAddGroupEndIndex(group, startPos);

					int pos = calculateIndex(startPos, endPos - 1, user,
							user.getmStatus());
					//计算出的位置pos，是指的是被添加的item与该pos的item对比之后break的结果
					int replaceItem = pos;
					Log.i(TAG, "计算插入位置 : " + pos);
					if (pos != -1) {
						ItemData userItem = this.getItem(group, user);
						User insertUser = ((User) userItem.getObject());
						insertUser.updateStatus(user.getmStatus());
						
						ItemData itemData = mFilterList.get(replaceItem);
						if(itemData instanceof UserItemData){
							User replacedUser = ((User) itemData.getObject());
							int result = replacedUser.compareTo(insertUser);
							if(result < 0)
								pos++;
						} 
						
						Log.i(TAG, "组名 = " + group.getName() + " 组开始位置 = "
								+ startPos + " ，组结束位置  = " + endPos + " ,插入位置 = "
								+ pos);
						
						if(pos >= mFilterList.size()){
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
			ItemData item = mFilterList.get(i);
			Object obj = item.getObject();
			if (!found && obj == src && ((GroupItemData) item).isExpanded) {
				found = true;
			}

			if (dest == obj && ((GroupItemData) item).isExpanded) {
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
	 * Mark user as selected, and call {@link ItemData#isChecked()} will return
	 * true The function only called by {ConferenceCreateActivity#doPreSelect}
	 * 
	 * @param user
	 */
	public void selectUser(User user) {
		if (user == null) {
			return;
		}
		Set<Group> groupList = user.getBelongsGroup();
		for (Group g : groupList) {
			ItemData item = getItem(g, user);
			item.setChecked(true);

			GroupItemData groupItem = (GroupItemData) getItem(g);
			if (!groupItem.isExpanded) {
				int pos = getGroupItemPos(groupItem);
				if (pos != -1) {
					groupItem.isExpanded = true;
					expand(groupItem, pos);
				}
			}
		}
		adapter.notifyDataSetChanged();
	}

	/**
	 * Mark user list as selected, and call {@link ItemData#isChecked()} will
	 * return true The function only called by
	 * {ConferenceCreateActivity#doPreSelect}
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
				ItemData item = getItem(g, user);
				item.setChecked(true);

				GroupItemData groupItem = (GroupItemData) getItem(g);
				if (!groupItem.isExpanded) {
					int pos = getGroupItemPos(groupItem);
					if (pos != -1){
						groupItem.isExpanded = true;
						expand(groupItem, pos);
					}
				}
			}
		}
		adapter.notifyDataSetChanged();
	}

	/**
	 * Mark group as selected, and call {@link ItemData#isChecked()} will return
	 * true.<br>
	 * Users belong this group will mark, unless you expand this group.
	 * 
	 * @param group
	 */
	public void selectGroup(Group group) {
		if (group == null) {
			return;
		}
		ItemData item = getItem(group);
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

	public void updateDisplay() {
		adapter.notifyDataSetChanged();
	}

	/**
	 * Update user item check status according to flag
	 * 
	 * @param u
	 * @param flag
	 */
	private void updateCheckItemWithoutNotification(User u, boolean flag) {
		Set<ItemData> itemDataSet = mUserItemListMap.get(u.getmUserId());
		if (itemDataSet == null || itemDataSet.size() <= 0) {
			if(itemDataSet == null)
				itemDataSet = new HashSet<GroupListView.ItemData>();
			Set<Group> belongsGroup = u.getBelongsGroup();
			Iterator<Group> iterator = belongsGroup.iterator();
			while (iterator.hasNext()) {
				Group tempGroup = iterator.next();
				ItemData item = getItem(tempGroup, u);
				itemDataSet.add(item);
			}
			return;
		}
		for (ItemData item : itemDataSet) {
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
		ItemData item = mItemMap.get(group.getmGId());
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
	private int updateUserPosition(GroupItemData gitem, int gstart,
			int gend, User user, User.Status newSt) {
		if(gend >= mFilterList.size())
			gend = mFilterList.size() - 1;
		int pos = -1;
		int start = gstart;
		int end = gend;

			
		while (start < end && end < mFilterList.size() && mFilterList.size() > start) {
			ItemData item = mFilterList.get(start);
			ItemData endItem = mFilterList.get(end);

			if (item instanceof UserItemData) {
				if (((User) ((UserItemData) item).getObject()).getmUserId() == user
						.getmUserId()) {
					pos = start;
				}
			} else {
				// If sub group is expended, we should update end position
				if (((GroupItemData) item).isExpanded) {
					GroupItemData subGroupItem = (GroupItemData) item;

					int subGroupStartIndex = calculateGroupStartIndex(subGroupItem.mGroup);
					int subGroupEndIndex = calculateGroupEnd(
							subGroupItem.mGroup, start);
					updateUserPosition(subGroupItem, subGroupStartIndex,
							subGroupEndIndex, user, newSt);
					start += subGroupEndIndex;
				}
				start++;
			}

			if (endItem instanceof UserItemData) {
				if (((User) ((UserItemData) endItem).getObject()).getmUserId() == user
						.getmUserId()) {
					pos = end;
				}
			} else {
				// If sub group is expended, we should update end position
				if (((GroupItemData) endItem).isExpanded) {
					GroupItemData subGroupItem = (GroupItemData) endItem;
					int subGroupStartIndex = calculateGroupStartIndex(subGroupItem.mGroup);
					int subGroupEndIndex = calculateGroupEnd(
							subGroupItem.mGroup, start);
					updateUserPosition(subGroupItem, subGroupStartIndex,
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
		if (pos != -1 && pos < mFilterList.size()) {

			// Reset start and end position
			Group currentGroup = (Group) gitem.getObject();
			int startPos = calculateGroupStartIndex(currentGroup);
			int endPos = gend;
//			int startPos = gstart;
//			 end = gend - 1;
//			int endPos = gend;

			// remove current status
			ItemData origin = mFilterList.remove(pos);
			pos = calculateIndex(startPos, endPos, user, newSt);

			if (pos != -1) {
				if (pos == mFilterList.size()) {
					mFilterList.add(origin);
				} else {
					mFilterList.add(pos, origin);
				}
			}
			return pos;
		}
		return pos;
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
	public int calculateGroupEnd(Group group, int startIndex) {
		if (startIndex >= mFilterList.size()) {
			return -1;
		}

		int groupEnd = startIndex + calculateGroupEndIndex(group);
		while (startIndex < groupEnd && startIndex < mFilterList.size()) {
			ItemData item = mFilterList.get(startIndex);
			Object obj = item.getObject();
			// If current item is group and group level same and is not self,
			// then group end is this item index - 1
			if ((obj instanceof Group && group.getLevel() == ((Group) obj)
					.getLevel()) && obj != group) {
				return getGroupItemPos((GroupItemData) item);
			}
			startIndex++;
		}
		return startIndex;
	}

	public int calculateGroupEndIndex(Group group) {
		int groupEnd = group.getUsers().size();
		groupEnd += getExpandGroupSize(group.getChildGroup());
		return groupEnd;
	}
	
	public int calculateAddGroupEndIndex(Group group , int startIndex){
		int groupEnd = startIndex + group.getUsers().size();
		return groupEnd - 1;
	}

	public int calculateGroupStartIndex(Group group) {
		GroupItemData item = (GroupItemData) getItem(group);
		return calculateGroupStartIndex(item, group);
	}

	public int calculateGroupStartIndex(GroupItemData item, Group group) {
		int itemStartPos = getGroupItemPos(item);
		int startPos = itemStartPos + getExpandGroupSize(group.getChildGroup());
		return startPos + 1;
	}
	
	public int calculateAddGroupStartIndex(Group group) {
		GroupItemData item = (GroupItemData) getItem(group);
		int itemStartPos = getGroupItemPos(item);
		int startPos = itemStartPos + getExpandGroupSize(group.getChildGroup());
		return startPos + 1;
	}

	public int getExpandGroupSize(List<Group> groups) {
		int groupLength = 0;
		for (int i = 0; i < groups.size(); i++) {
			groupLength += 1;
			Group child = groups.get(i);

			GroupItemData item = (GroupItemData) getItem(child);
			if (item.isExpanded) {
				groupLength += child.getUsers().size();
			}

			if (child.getChildGroup().size() > 0) {
				groupLength += getExpandGroupSize(child.getChildGroup());
			}
		}
		return groupLength;
	}

	/**
	 * According ItemData , get pos in mFilterList;
	 * 
	 * @param item
	 * @return
	 */
	public int getGroupItemPos(GroupItemData item) {
		for (int i = 0; i < mFilterList.size(); i++) {
			ItemData itemData = mFilterList.get(i);
			if (itemData instanceof GroupItemData) {
				GroupItemData temp = (GroupItemData) itemData;
				if (temp.mGroup.getmGId() == item.mGroup.getmGId()) {
					return i;
				}
			}
		}
		return -1;
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
			if (start == mFilterList.size()){
				ItemData lastItem = mFilterList.get(start - 1);
				User lastUser = (User) ((UserItemData) lastItem).getObject();
				boolean result = compareUserSort(user , lastUser , ust);
				if(result){
					pos = pos - 1;
				}
				break;
			}
			
			ItemData item = mFilterList.get(start++);
			if (item instanceof GroupItemData) {
				continue;
			}
			User u = (User) ((UserItemData) item).getObject();
			// if item is current user, always sort after current user
			boolean result = compareUserSort(user , u , ust);
			if(result)
				break;
			else
				continue;
		}

		return pos;
	}
	
	/**
	 * continue is false , break is true
	 * @param comparedUser
	 * @param beComparedUser
	 * @param compUserStus
	 * @return
	 */
	private boolean compareUserSort(User comparedUser , User beComparedUser , User.Status compUserStus){
		if (beComparedUser == null
				|| beComparedUser.getmUserId() == GlobalHolder.getInstance()
						.getCurrentUserId()) {
			return false;
		}

		if (compUserStus == User.Status.ONLINE || compUserStus == User.Status.BUSY
				|| compUserStus == User.Status.DO_NOT_DISTURB
				|| compUserStus == User.Status.LEAVE) {
			if ((beComparedUser.getmStatus() == User.Status.ONLINE
					|| beComparedUser.getmStatus() == User.Status.BUSY
					|| beComparedUser.getmStatus() == User.Status.DO_NOT_DISTURB || beComparedUser
					.getmStatus() == User.Status.LEAVE)
					&& beComparedUser.compareTo(comparedUser) < 0) {
				return false;
			} else {
				return true;
			}
		} else if (compUserStus == User.Status.OFFLINE || compUserStus == User.Status.HIDDEN) {
			if ((beComparedUser.getmStatus() == User.Status.OFFLINE || beComparedUser.getmStatus() == User.Status.HIDDEN)
					&& comparedUser.compareTo(beComparedUser) > 0) {
				return false;
			} else if (beComparedUser.getmStatus() != User.Status.OFFLINE
					&& beComparedUser.getmStatus() != User.Status.HIDDEN) {
				return false;
			} else {
				return true;
			}
		} else {
			if (beComparedUser.getmStatus() == User.Status.ONLINE) {
				return false;
			} else if (beComparedUser.getmStatus() == User.Status.OFFLINE
					|| beComparedUser.getmStatus() == User.Status.HIDDEN) {
				return true;
			} else if (beComparedUser.compareTo(comparedUser) < 0) {
				return false;
			} else {
				return true;
			}
		}
	}

	private ItemData getItem(Group g) {
		ItemData item = mItemMap.get(g.getmGId());
		if (item == null) {
			item = new GroupItemData(g);
			Group parent = g.getParent();
			if (parent != null) {
				ItemData itemParent = mItemMap.get(parent.getmGId());
				if (itemParent != null)
					item.setChecked(itemParent.isChecked());
			}
			mItemMap.put(g.getmGId(), item);
		}
		return item;
	}

	/**
	 */
	private ItemData getItem(Group g, User u) {
		LongSparseArray<ItemData> map = mUserItemDataMapOfMap.get(g.getmGId());
		if (map == null) {
			map = new LongSparseArray<ItemData>();
			mUserItemDataMapOfMap.put(g.getmGId(), map);
		}
		ItemData item = map.get(u.getmUserId());
		if (item == null) {
			item = new UserItemData(u, g.getLevel() + 1);
			// Initialize user item check status.
			// If exist one group checked, then user item should be checked
			boolean checked = false;
			Set<Group> parents = u.getBelongsGroup();
			for (Group parent : parents) {
				ItemData parentItem = getItem(parent);
				if (parentItem.isChecked()) {
					checked = true;
					break;
				}
			}
			// Update check status according group status.
			item.setChecked(checked);
			map.put(u.getmUserId(), item);
		}

		Set<ItemData> itemList = mUserItemListMap.get(u.getmUserId());
		if (itemList == null) {
			itemList = new HashSet<ItemData>();
			mUserItemListMap.put(u.getmUserId(), itemList);
		} else {
			Iterator<ItemData> iterator = itemList.iterator();
			ItemData next = iterator.next();
			if (next.isChecked()) {
				item.setChecked(true);
			} else {
				item.setChecked(false);
			}
		}
		itemList.add(item);

		return item;
	}

	@Override
	public void setFilterText(String filterText) {
		if (TextUtils.isEmpty(filterText)) {
			return;
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
	private void collapse(GroupItemData item, int pos) {
		int level = item.getLevel();
		int start = pos;
		int end = mFilterList.size();
		while (++start < end) {
			ItemData it = mFilterList.get(start);

			if (it.getLevel() != level && it instanceof GroupItemData) {
				GroupItemData current = (GroupItemData) it;
				if (current.isExpaned()) {
					((GroupItemData) it).isExpanded = false;
					collapse((GroupItemData) item, start);
				}
			}

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
	private int expand(GroupItemData item, int pos) {
		Group g = (Group) item.getObject();
		List<Group> subGroupList = g.getChildGroup();
		// DO not user for(Group g:list) concurrency problem
		for (int i = subGroupList.size() - 1; i >= 0; i--) {
			Group subG = subGroupList.get(i);
			GroupItemData groupItem = (GroupItemData) getItem(subG);
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
			ItemData item = mFilterList.get(position);
			if (item instanceof GroupItemData) {
				if (((GroupItemData) item).isExpanded) {
					collapse((GroupItemData) item, position);
					((GroupItemData) item).isExpanded = false;
				} else {
					expand((GroupItemData) item, position);
					((GroupItemData) item).isExpanded = true;
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
			ItemData item = mFilterList.get(position);

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
			ItemData item = (ItemData) v.getTag();
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

		private boolean isFirstSearch = true;

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults fr = new FilterResults();
			List<ItemData> list = null;
			List<User> searchUser = null;
			if (TextUtils.isEmpty(constraint.toString())) {
				list = mBaseList;
				SearchUtils.clearAll();
				isFirstSearch = true;
			} else {
				// list = new ArrayList<ItemData>();
				// for (Group g : mGroupList) {
				// search(list, g, constraint);
				// }
				// Collections.sort(list);
				list = new ArrayList<GroupListView.ItemData>();
				if (isFirstSearch) {
					SearchUtils.clearAll();
					List<Object> users = new ArrayList<Object>();
					for (Group group : mGroupList) {
						convertGroupToUser(users, group);
					}
					SearchUtils.receiveList = users;
					isFirstSearch = false;
				}

				searchUser = SearchUtils
						.receiveGroupUserFilterSearch(constraint.toString());

				if (searchUser != null) {
					for (int i = 0; i < searchUser.size(); i++) {
						User user = searchUser.get(i);
						list.add(getItem(user.getFirstBelongsGroup(), user));
					}
				}
			}

			fr.values = list;
			fr.count = list.size();
			list = null;
			searchUser = null;
			return fr;
		}

		private void convertGroupToUser(List<Object> users, Group group) {
			users.addAll(group.getUsers());
			List<Group> gList = group.getChildGroup();
			for (Group subG : gList) {
				convertGroupToUser(users, subG);
			}
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			if (results.values != null) {
				mFilterList = (List<ItemData>) results.values;
				Collections.sort(mFilterList);
				adapter.notifyDataSetChanged();
			} else {
				// TODO toast search error
			}
		}

		@Override
		public CharSequence convertResultToString(Object resultValue) {
			return super.convertResultToString(resultValue);
		}

		void search(List<ItemData> list, Group g, CharSequence constraint) {
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
				int position, long id, ItemData item);

		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id, ItemData item);

		public void onCheckboxClicked(View view, ItemData item);
	}

	public interface ItemData extends Comparable<ItemData> {
		public long getId();

		public Object getObject();

		public int getLevel();

		public boolean isChecked();

		void setChecked(boolean flag);
	}

	class GroupItemData implements ItemData {

		private Group mGroup;
		private boolean isExpanded;
		private boolean isChecked;
		private boolean searchedCurrentUser;
		private boolean existCurrentUser;

		public GroupItemData(Group group) {
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
		public int compareTo(ItemData another) {
			return 0;
		}

		@Override
		public int getLevel() {
			return mGroup.getLevel();
		}

		public boolean isExpaned() {
			return isExpanded;
		}

		public void setExpaned(boolean isExpaned) {
			this.isExpanded = isExpaned;
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

	class UserItemData implements ItemData {

		private User mUser;
		private int mLevel;
		private boolean isChecked;

		public UserItemData(User user, int level) {
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
		public int compareTo(ItemData another) {
			if(another == null || another.getObject() == null)
				return -1;
			
			if (another instanceof UserItemData) {
				User anotherUser = (User) another.getObject();
				boolean result = compareUserSort(anotherUser, mUser, anotherUser.getmStatus());
				if(result)
					return 1;
				else 
					return -1;
			} else 
				return 1;
		}

	}

	/**
	 * Adapter item view
	 * 
	 * @author jiangzhen
	 * 
	 */
	class GroupListViewAdapterItem extends LinearLayout {

		private ItemData mItem;
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

		public void update(ItemData item, boolean paddingFlag) {
			if (item == null) {
				return;
			}
			if (this.mItem == null || this.mItem != item) {
				this.mItem = item;
			}

			if (item instanceof GroupItemData) {
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

		private void updateGroupItem() {
			Group g = ((Group) ((GroupItemData) mItem).getObject());
			TextView mGroupNameTV = (TextView) mRoot
					.findViewById(R.id.group_name);
			mGroupNameTV.setText(g.getName());
			updateGroupItemUserOnlineNumbers();
			if (((GroupItemData) mItem).isExpanded) {
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

		private void updateGroupItemUserOnlineNumbers() {
			GroupItemData gi = ((GroupItemData) mItem);
			Group g = ((Group) gi.getObject());
			TextView tv = ((TextView) mRoot
					.findViewById(R.id.group_online_statist));
			int count = g.getUserCount();
			int onlineCount = 0;
			Set<User> sets = g.getOnlineUserSet();
			for (User u : sets) {
				User.Status status = u.getmStatus();
				User user = GlobalHolder.getInstance().getUser(u.getmUserId());
				status = user.getmStatus();
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

			tv.setText(" [ "
					+ onlineCount
					+ " / "
					+ ((mIgnoreCurrentUser && gi.existCurrentUser) ? count - 1
							: count) + " ]");
			tv.invalidate();
		}

		private void updateUserItem() {
			User u = ((User) ((UserItemData) mItem).getObject());
			u = GlobalHolder.getInstance().getUser(u.getmUserId());
//			if (GlobalHolder.getInstance().getGlobalState().isGroupLoaded()) {
//				if (TextUtils.isEmpty(u.getName()))
//					throw new RuntimeException(
//							"所有用户信息已获取完毕，但该用户空名：id is --> "
//									+ u.getmUserId());
//			}

			ImageView mPhotoIV = (ImageView) mRoot.findViewById(R.id.user_img);
			if (u.getAvatarBitmap() != null) {
				mPhotoIV.setImageBitmap(u.getAvatarBitmap());
			} else {
				mPhotoIV.setImageResource(R.drawable.avatar);
			}
			
			TextView mUserNameTV = (TextView) mRoot
					.findViewById(R.id.user_name);
			boolean isFriend = GlobalHolder.getInstance().isFriend(u);
			if (isFriend) {
				if (!TextUtils.isEmpty(u.getNickName()))
					mUserNameTV.setText(u.getNickName());
				else {
					mUserNameTV.setText(u.getName());
				}
			} else {
				mUserNameTV.setText(u.getName());
			}

			TextView mUserSignatureTV = (TextView) mRoot
					.findViewById(R.id.user_signature);
			mUserSignatureTV.setText(u.getSignature() == null ? "" : u
					.getSignature());
//			mUserSignatureTV.setSingleLine(true);
//			mUserSignatureTV.setEllipsize(TruncateAt.END);
			
			updateUserStatus(u.getDeviceType(), u.getmStatus());
			
			if (mCBFlag) {
				mCb = (CheckBox) mRoot.findViewById(R.id.user_check_view);
				mCb.setVisibility(View.VISIBLE);
				mCb.setChecked(mItem.isChecked());
				mCb.setOnClickListener(mCheckBoxListener);
				
			} else {
				mRoot.findViewById(R.id.user_check_view).setVisibility(
						View.GONE);
			}

		}

		private void updateUserStatus(User.DeviceType dType, User.Status st) {
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
	}

}