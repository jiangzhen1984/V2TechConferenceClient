package com.v2tech.view.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.vo.Group;
import com.v2tech.vo.User;

/**
 * Group List view wrapper.<br>
 * 
 * @author jiangzhen
 * 
 */
public class GroupListView extends ListView {

	private Context mContext;
	private List<Group> mGroupList;
	private List<Item> mBaseList;
	private List<Item> mFilterList;
	private LocalAdapter adapter;
	private LocalFilter filter;
	private boolean mCBFlag;
	private GroupListViewListener mListener;
	private LongSparseArray<Item> mItemMap;
	private LongSparseArray<Set<Item>> mUserItemListMap;
	private LongSparseArray<LongSparseArray<Item>> mGroupItemUserMap;
	private boolean mIsInFilter;
	private int currentListViewType;

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
	
	public int getCurrentListViewType() {
		return currentListViewType;
	}

	public void setCurrentListViewType(int currentListViewType) {
		this.currentListViewType = currentListViewType;
	}

	public void setShowedCheckedBox(boolean flag) {
		mCBFlag = flag;
	}

	public GroupListViewListener getListener() {
		return mListener;
	}

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
				sort = updateUserStatus(((GroupItem) item), i + 1, user, us);
			}
		}
		if (sort) {
			adapter.notifyDataSetChanged();
		}
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
		adapter.notifyDataSetChanged();
	}
	
	
	/**
	 * Remote list view item according user 
	 * @param user
	 */
	public void removeItem(User user) {
		if (user == null) {
			return;
		}
		for (int i=0; i < mFilterList.size(); i++) {
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
	
	public void addUser(User user) {
		adapter.notifyDataSetChanged();
	}
	
	public void updateItem(User user) {
		//FIXME optimze for avatar
		adapter.notifyDataSetChanged();
	}

	private void updateCheckItemWithoutNotification(User u, boolean flag) {
		Set<Item> list = mUserItemListMap.get(u.getmUserId());
		if (list == null || list.size() <= 0) {
			return;
		}
		for (Item item : list) {
			item.setChecked(flag);
		}
	}

	private void updateCheckItemWithoutNotification(Group group, boolean flag) {
		Item item = mItemMap.get(group.getmGId());
		item.setChecked(flag);
		List<User> list = group.getUsers();
		for (User u : list) {
			updateCheckItem(u, flag);
		}
		List<Group> subGroupList = group.getChildGroup();
		for (int i = 0; i < subGroupList.size(); i++) {
			updateCheckItemWithoutNotification(subGroupList.get(i), flag);
		}
	}

	private boolean updateUserStatus(GroupItem gitem, int index, User user,
			User.Status newSt) {
		int pos = -1;
		int start = index;
		int end = ((Group) gitem.getObject()).getSubSize();

		while (start <= end && mFilterList.size() > start) {
			Item item = mFilterList.get(start);
			Item endItem = mFilterList.get(end);

			if (item instanceof UserItem) {
				if (((User) ((UserItem) item).getObject()).getmUserId() == user
						.getmUserId()) {
					pos = start;
				}
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

			if (endItem instanceof UserItem) {
				if (((User) ((UserItem) endItem).getObject()).getmUserId() == user
						.getmUserId()) {
					pos = end;
				}
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

		// Update user new position;
		if (pos != -1) {
			// Reset start and end position
			start = index;
			end = ((Group) gitem.getObject()).getSubSize() - 1;
			// remove current status
			Item origin = mFilterList.remove(pos);

			while (start < end) {
				pos = start;
				Item item = mFilterList.get(start++);
				if (item instanceof GroupItem) {
					V2Log.e("=======" + ((Group)item.getObject()).getName());
					continue;
				}
				User u = (User) ((UserItem) item).getObject();
				// if item is current user, always sort after current user
				if (u == null || u.getmUserId() == GlobalHolder.getInstance()
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

	private Item getItem(Group g) {
		Item item = mItemMap.get(g.getmGId());
		if (item == null) {
			item = new GroupItem(g);
			mItemMap.put(g.getmGId(), item);
		}
		return item;
	}

	/**
	 */
	private Item getItem(Group g, User u) {
		LongSparseArray<Item> map = mGroupItemUserMap.get(g.getmGId());
		if (map == null) {
			map = new LongSparseArray<Item>();
			mGroupItemUserMap.put(u.getmUserId(), map);
		}
		Item item = map.get(u.getmUserId());
		if (item == null) {
			item = new UserItem(u, g.getLevel() + 1);
			map.put(u.getmUserId(), item);

			Set<Item> itemList = mUserItemListMap.get(u.getmUserId());
			if (itemList == null) {
				itemList = new HashSet<Item>();
				mUserItemListMap.put(u.getmUserId(), itemList);
			}
			itemList.add(item);
		}

		return item;
	}
	
	

	@Override
	public void setFilterText(String filterText) {
		mIsInFilter = true;
		super.setFilterText(filterText);
	}

	@Override
	public void clearTextFilter() {
		mIsInFilter = false;
		super.clearTextFilter();
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
		adapter.notifyDataSetChanged();
	}

	/**
	 * Expand group item
	 * 
	 * @param item
	 * @param pos
	 *            position of want to expand group
	 */
	private void expand(GroupItem item, int pos) {
		Group g = (Group) item.getObject();
		List<Group> subGroupList = g.getChildGroup();
		// DO not user for(Group g:list) concurrency problem
		for (int i = subGroupList.size() - 1; i >=0; i--) {
			Group subG = subGroupList.get(i);
			if (mFilterList.size() == pos + 1) {
				mFilterList.add(getItem(subG));
			} else {
				mFilterList.add(pos + 1, getItem(subG));
			}
			pos++;
		}

		List<User> list = g.getUsers();
		Collections.sort(list);
		for (int i = 0; i < list.size(); i++) {
			User u = list.get(i);
			if (mFilterList.size() == pos + 1) {
				mFilterList.add(getItem(g, u));
			} else {
				mFilterList.add(pos + 1, getItem(g, u));
			}
			pos++;
		}
		adapter.notifyDataSetChanged();
	}

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

	public interface GroupListViewListener {
		public void onItemClicked(AdapterView<?> parent, View view,
				int position, long id, Item item);

		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id, Item item);
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
				} else {
					groupRoot.setPadding(35, 0, 0, 0);
				}
				mRoot.findViewById(R.id.user_view_root)
						.setVisibility(View.GONE);
			} else {
				updateUserItem();
				View userRoot = mRoot.findViewById(R.id.user_view_root);
				userRoot.setVisibility(View.VISIBLE);
				if (paddingFlag) {
					userRoot.setPadding((item.getLevel() - 1) * 35, 0, 0, 0);
				} else {
					userRoot.setPadding(35, 0, 0, 0);
				}

				mRoot.findViewById(R.id.group_view_root).setVisibility(
						View.GONE);
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
			int maxWidth = 0;
			if (mCb != null) {
				int[] cbL = new int[2];
				mCb.getLocationInWindow(cbL);
				maxWidth = cbL[0] - nameLo[0] - 80;
			} else {
				maxWidth = this.getWidth() - nameLo[0] - 80;
			}

			mUserSignatureTV.setMaxWidth(maxWidth);
			mUserSignatureTV.setSingleLine(true);
			mUserSignatureTV.setEllipsize(TruncateAt.END);

			if(currentListViewType == V2GlobalEnum.GROUP_TYPE_CONTACT && !TextUtils.isEmpty(u.getNickName()))
				mUserNameTV.setText(u.getNickName());
			else
				mUserNameTV.setText(u.getName());

			updateStatus(u.getDeviceType(), u.getmStatus());
			if (mCBFlag) {
				mCb = (CheckBox) mRoot.findViewById(R.id.user_check_view);
				mCb.setVisibility(View.VISIBLE);
				mCb.setChecked(mItem.isChecked());
			} else {
				mRoot.findViewById(R.id.user_check_view).setVisibility(
						View.GONE);
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
			Group g = ((Group) ((GroupItem) mItem).getObject());
			((TextView) mRoot.findViewById(R.id.group_online_statist))
					.setText(g.getOnlineUserCount() + " / " + g.getUserCount());
		}

	}

}