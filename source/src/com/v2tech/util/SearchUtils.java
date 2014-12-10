package com.v2tech.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.Editable;

import com.V2.jni.util.V2Log;
import com.v2tech.db.provider.SearchContentProvider;
import com.v2tech.view.ConversationsTabFragment.ScrollItem;
import com.v2tech.view.widget.GroupListView.ItemData;
import com.v2tech.vo.Group;
import com.v2tech.vo.User;

public class SearchUtils {

	private static List<Object> searchList = new ArrayList<Object>();
	private static List<Object> searchCacheList = new ArrayList<Object>();
	private static List<Object> firstSearchCacheList = new ArrayList<Object>();
	
	private static List<Object> receiveList = new ArrayList<Object>();
	private static final String TAG = "SearchUtils";
	private static int lastSize;
	private static boolean isShouldAdd;
	private static boolean isShouldQP; // 是否需要启动全拼
	private static int startIndex = 0;
	private static boolean isBreak; // 用于跳出getSearchList函数中的二级循环
	public static boolean mIsStartedSearch;
	
	private static final int TYPE_CONVERSATION = 10;
	private static final int TYPE_ITEM_DATA = 11;
	private static int type = TYPE_CONVERSATION;
	
	public static List<ScrollItem> startConversationSearch(List<ScrollItem> list , Editable content){
		type = TYPE_CONVERSATION;
		receiveList.addAll(list);
		List<Object> search = search(content.toString());
		list.clear();
		for (Object object : search) {
			list.add((ScrollItem)object);
		}
		receiveList.clear();
		return list;
	}
	
	public static List<User> startGroupUserSearch(List<Group> mGroupList , CharSequence content){
		type = TYPE_ITEM_DATA;
		List<User> users = new ArrayList<User>();
		for (Group group : mGroupList) {
			convertGroupToUser(users , group);
		}
		receiveList.addAll(users);
		List<Object> search = search(content.toString());
		users.clear();
		for (Object object : search) {
			users.add((User)object);
		}
		receiveList.clear();
		return users;
	}
	
	public static List<User> startGroupUserFilterSearch(List<User> mItemList , CharSequence content){
		type = TYPE_ITEM_DATA;
		List<User> users = mItemList;
		receiveList.addAll(users);
		List<Object> search = search(content.toString());
		users.clear();
		for (Object object : search) {
			users.add((User)object);
		}
		receiveList.clear();
		return users;
	}
	
	
	private static void convertGroupToUser(List<User> users , Group group) {
		users.addAll(group.getUsers());
		List<Group> gList = group.getChildGroup();
		for (Group subG : gList) {
			convertGroupToUser(users , subG);
		}
	}

	private static List<Object> search(String content){
		if (content != null && content.length() > 0) {
			if (!mIsStartedSearch) {
				searchList.clear();
				lastSize = 0;
				startIndex = 0;
				mIsStartedSearch = true;
				searchList.addAll(receiveList);
			}
	
			int length = content.length();
			if (length < lastSize) {
				searchList.clear();
				searchList.addAll(receiveList);
				startIndex = content.length() - 1;
			}
			lastSize = length;
		
			StringBuilder sb = new StringBuilder();
			V2Log.e(TAG, "Editable :" + content.toString());

			char[] charSimpleArray = content.toString()
					.toLowerCase(Locale.getDefault()).toCharArray();
			if (charSimpleArray.length < 5) {
				// 搜字母查询
				for (int i = startIndex; i < charSimpleArray.length; i++) {
					if (isChineseWord(charSimpleArray[i])) {
						V2Log.e(TAG, charSimpleArray[i] + " is Chinese");
						searchCacheList = getSearchList(searchList, 
								String.valueOf(charSimpleArray[i]), content.toString(), i, true,
								true);
					} else {
						V2Log.e(TAG, charSimpleArray[i] + " not Chinese");
						searchCacheList = getSearchList(searchList,
								String.valueOf(charSimpleArray[i]),content.toString(), i, true,
								false);
					}

					if (i == 0 && content.toString().length() == 1
							&& firstSearchCacheList.size() == 0) {
						firstSearchCacheList.addAll(searchCacheList);
					}

					searchList.clear();
					if (searchCacheList.size() > 0) {
						isShouldQP = false;
						searchList.addAll(searchCacheList);
						searchCacheList.clear();
						V2Log.e(TAG, "简拼找到结果 展示");
					} else {
						isShouldQP = true;
						searchCacheList.addAll(firstSearchCacheList);
						V2Log.e(TAG, "简拼没有结果 开启全拼");
					}
				}
			} else {
				isShouldQP = true;
				searchCacheList.addAll(firstSearchCacheList);
				V2Log.e(TAG, "简拼没有结果 开启全拼");
			}

			// if(s.toString().length() >= 5 && searchCacheList.size() > 0){
			// //如果长度大于5则不按首字母查询
			if (isShouldQP) { // 如果长度大于5则不按首字母查询
				searchList.clear();
				V2Log.e(TAG, "searchCacheList size :" + searchCacheList.size());
				for (int i = 0; i < searchCacheList.size(); i++) {
					Object obj = searchCacheList.get(i);
					V2Log.e(TAG,
							"searchList : "
									+ getObjectValue(obj)
									+ "--StringBuilder : " + sb.toString());
					// 获取名字，将名字变成拼音串起来
					char[] charArray = getObjectValue(obj)
							.toCharArray();
					for (char c : charArray) {
						String charStr = GlobalConfig.allChinese.get(String
								.valueOf(c));
						// V2techSearchContentProvider
						// .queryChineseToEnglish(mContext, "HZ = ?",
						// new String[] { String.valueOf(c) });
						sb.append(charStr);
					}
					V2Log.e(TAG, "StringBuilder : " + sb.toString());
					String material = sb.toString();
					// 判断该昵称第一个字母，与输入的第一字母是否匹配
					Character first = material.toCharArray()[0];
					char[] targetChars = content.toString().toCharArray();
					if (!first.equals(targetChars[0])) {
						isShouldAdd = true;
					} else {
						for(int j = 0 ; j < targetChars.length ; j++){
//						for (char c : targetChars) {
//							if (!material.contains(String.valueOf(c))
//									&& first.equals(c)) {
								if (targetChars[j] != material.charAt(j)) {
								isShouldAdd = true;
								V2Log.e(TAG, "material not contains " + targetChars[j]);
								break;
							}
							isShouldAdd = false;
						}
					}

					if (!isShouldAdd) {
						V2Log.e(TAG, "added ---------"
								+ getObjectValue(obj));
						if(!searchList.contains(obj))
							searchList.add(obj);
					}
					else{
						if(searchList.contains(obj))
							searchList.remove(obj);
					}
					sb.delete(0, sb.length());
				}
			}
			startIndex++;
			return searchList;
		} else {
			clearAll();
		}
		return searchList;
	}
	
	public static void clearAll(){
		SearchContentProvider.closedDataBase();
		if (mIsStartedSearch) {
			firstSearchCacheList.clear();
			receiveList.clear();
			searchCacheList.clear();
			searchList.clear();
			mIsStartedSearch = false;
			startIndex = 0;
		}
	}
	
	/**
	 * 根据 searchKey 获得搜索后的集合
	 * 
	 * @param list
	 * @param searchKey
	 * @param content
	 * @param index
	 * @param isFirstSearch
	 *            判断是否是首字母搜索
	 * @param isChinese
	 *            判断searchKey是否为中文
	 * @return
	 */
	public static List<Object> getSearchList(List<Object> list,
			String searchKey, String content , int index, boolean isFirstSearch,
			boolean isChinese) {
		V2Log.e(TAG, "searchList :" + list.size() + "--searchKey :" + searchKey
				+ "--index :" + index + " content : " + content);
		List<Object> tempList = new ArrayList<Object>();
		if (searchKey == null || searchKey.length() < 0) {
			return tempList;
		}

		String searchTarget;
		for (int i = 0; i < list.size(); i++) { // 一级循环，循环所有消息
			Object obj = list.get(i);
//			Conversation cov = list.get(i).cov;
			// 判断是否能获取到消息item的名字
			if (getObjectValue(obj) != null) {
				// 将名字分割为字符数组遍历
				char[] charArray = getObjectValue(obj).toCharArray();
				for (int j = 0; j < charArray.length; j++) { // 二级循环，循环消息名称
					searchTarget = String.valueOf(charArray[j]);
					if (isFirstSearch && isChinese) {
						if (searchKey.contains(searchTarget)) {
							tempList.add(obj);
							break;
						}
					} else if (isFirstSearch && !isChinese) {
						// if (isChineseWord(cov.getName().charAt(index))) {
						if (isChineseWord(charArray[j])) {
							String englishChar = GlobalConfig.allChinese
									.get(searchTarget);
							V2Log.e(TAG, "englishChar :" + englishChar);
							if (englishChar == null)
								continue;
							String[] split = englishChar.split(";");
							for (String string : split) { // 三级循环，循环多音字

								int indexOf = string.indexOf(searchKey);
								if (indexOf == 0 && content.indexOf(searchKey) == index) {
									tempList.add(obj);
									isBreak = true;
									break;
								}
							}
							// tempList添加元素后就直接跳出二级循环。
							if (isBreak) {
								isBreak = false;
								break;
							}
							// if(searchTarget.contains(searchKey)){
						} else {
							searchTarget = searchTarget.toLowerCase(Locale
									.getDefault());
							V2Log.e(TAG, "searchTarget :" + searchTarget);
							int indexOf = searchTarget.indexOf(searchKey);
							// if(searchTarget.contains(searchKey)){
							if (indexOf != -1) {
								tempList.add(obj);
								break;
							}
						}
					}
					// else if (!isFirstSearch && isChinese) {
					// String englishChar =
					// GlobalConfig.allChinese.get(searchTarget);
					// if (cov.getName().contains(searchKey) &&
					// first.equals(searchKey)){
					// tempList.add(Object);
					// break;
					// }
					// }
					// else if (!isFirstSearch && !isChinese) {
					//
					// }
				}
				// 判断该消息人的名字，在index位置是否能取到字符
				// if (index >= cov.getName().length()) {
				// continue;
				// } else {
				//
				// searchTarget = String.valueOf(cov.getName().charAt(index));
				// if (searchTarget == null) {
				// continue;
				// }
				// }
			}

			// 暂不要求消息内容
			// else if (cov.getMsg() != null &&
			// cov.getMsg().toString().contains(searchKey)) {
			// newItemList.add(cov);
			// }
		}
		return tempList;
	}
	
	public static String getObjectValue(Object obj){
		
		switch (type) {
		case TYPE_CONVERSATION:
			return ((ScrollItem)obj).cov.getName();
		case TYPE_ITEM_DATA:
			return ((User)obj).getName();
		default:
			break;
		}
		return null;
	}
		
	/**
	 * 判断给定的字符是否为汉字
	 * 
	 * @param mChar
	 * @return
	 */
	public static boolean isChineseWord(char mChar) {
		Pattern pattern = Pattern.compile("[\\u4E00-\\u9FA5]"); // 判断是否为汉字
		Matcher matcher = pattern.matcher(String.valueOf(mChar));
		return matcher.find();
	}
}
