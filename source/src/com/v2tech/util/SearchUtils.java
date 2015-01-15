package com.v2tech.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.Editable;
import android.text.TextUtils;
import android.util.SparseArray;

import com.V2.jni.util.V2Log;
import com.v2tech.db.provider.SearchContentProvider;
import com.v2tech.view.ConversationsTabFragment.ScrollItem;
import com.v2tech.view.conference.VideoAttendeeListLayout.Wrapper;
import com.v2tech.vo.User;

public class SearchUtils {

	private static final String TAG = "SearchUtils";
	private static List<Object> searchList = new ArrayList<Object>();
	private static List<Object> searchCacheList = new ArrayList<Object>();
	private static List<Object> surplusList = new ArrayList<Object>();
	private static List<Object> singleCacheList = new ArrayList<Object>();
	private static SparseArray<List<Object>> contentCacheList = new SparseArray<List<Object>>();
	private static SparseArray<String> contentLengthCacheList = new SparseArray<String>();
	public static List<Object> receiveList = new ArrayList<Object>();
	
	private static boolean isShouldAdd;
	private static boolean isShouldQP; // 是否需要启动全拼
	private static int startIndex = 0;
	private static boolean isBreak; // 用于跳出getSearchList函数中的二级循环
	public static boolean mIsStartedSearch;

	private static final int TYPE_CONVERSATION = 10;
	private static final int TYPE_ITEM_DATA = 11;
	private static final int TYPE_WRAPPER = 12;
	private static int type = TYPE_CONVERSATION;

	/**
	 * For ConversationTabFragment search
	 * 
	 * @param content
	 * @return
	 */
	public static List<ScrollItem> startConversationSearch(Editable content) {
		type = TYPE_CONVERSATION;

		List<Object> cache = contentCacheList.get(content.length());
		if (cache != null && 
				contentLengthCacheList.get(content.length()) != null &&
				contentLengthCacheList.get(content.length()).equals(content.toString())) {
			V2Log.e(TAG, "find cache list : key --> " + content.length()
					+ " and value --> " + cache.size());
			List<ScrollItem> cacheItems = new ArrayList<ScrollItem>();
			for (Object object : cache) {
				cacheItems.add((ScrollItem) object);
			}
			contentCacheList.delete(content.length() + 1);
			contentLengthCacheList.delete(content.length() + 1);
			return cacheItems;
		} else {
			if (content.length() - 1 != 0) {
				startIndex = content.length() - 1;
				searchList.clear();
				List<Object> lastCache = contentCacheList.get(content.length() - 1);
				if(lastCache != null)
					searchList.addAll(lastCache);
			}

			List<ScrollItem> searchItems = new ArrayList<ScrollItem>();
			List<Object> search = search(content.toString());
			if(search != null && search.size() > 0){
				for (Object object : search) {
					searchItems.add((ScrollItem) object);
				}
	
				List<Object> temp = new ArrayList<Object>();
				temp.addAll(search);
				contentCacheList.put(content.length(), temp);
				contentLengthCacheList.put(content.length(), content.toString());
				V2Log.e(TAG, "put cache list : key --> " + content.length()
						+ " and value --> " + search.size());
				temp = null;
			}
			search = null;
			return searchItems;
		}
	}
	
	public static List<Wrapper> startVideoAttendeeSearch(String content) {
		type = TYPE_WRAPPER;

		List<Object> cache = contentCacheList.get(content.length());
		if (cache != null) {
			V2Log.e(TAG, "find cache list : key --> " + content.length()
					+ " and value --> " + cache.size());
			List<Wrapper> cacheItems = new ArrayList<Wrapper>();
			for (Object object : cache) {
				cacheItems.add((Wrapper) object);
			}
			contentCacheList.delete(content.length() + 1);
			return cacheItems;
		} else {
			if (content.length() - 1 != 0) {
				startIndex = content.length() - 1;
				searchList.clear();
				List<Object> lastCache = contentCacheList.get(content.length() - 1);
				if(lastCache != null)
					searchList.addAll(lastCache);
			}

			List<Wrapper> searchItems = new ArrayList<Wrapper>();
			List<Object> search = search(content.toString());
			if(search != null && search.size() > 0){
				for (Object object : search) {
					searchItems.add((Wrapper) object);
				}
	
				List<Object> temp = new ArrayList<Object>();
				temp.addAll(search);
				contentCacheList.put(content.length(), temp);
				V2Log.e(TAG, "put cache list : key --> " + content.length()
						+ " and value --> " + search.size());
				temp = null;
			}
			search = null;
			return searchItems;
		}
	}

	public static List<User> receiveGroupUserFilterSearch(String content) {
		List<User> result = new ArrayList<User>();
		char[] chars = content.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			int count = i;
			String target = String.valueOf(chars, 0, ++count);
			result = startGroupUserFilterSearch(target);
		}
		return result;
	}

	/**
	 * For GroupListView Filter
	 * 
	 * @param content
	 * @return
	 */
	public static List<User> startGroupUserFilterSearch(String content) {
		type = TYPE_ITEM_DATA;

		List<Object> cache = contentCacheList.get(content.length());
		if (cache != null &&
				contentLengthCacheList.get(content.length()) != null &&
				contentLengthCacheList.get(content.length()).equals(content.toString())) {
			V2Log.e(TAG, "find cache list : key --> " + content.length()
					+ " and value --> " + cache.size());
			List<User> cacheUsers = new ArrayList<User>();
			for (Object object : cache) {
				cacheUsers.add((User) object);
			}
			contentCacheList.delete(content.length() + 1);
			contentLengthCacheList.delete(content.length() + 1);
			return cacheUsers;
		} else {
			if (content.length() - 1 != 0) {
				startIndex = content.length() - 1;
				searchList.clear();
				List<Object> lastCache = contentCacheList.get(content.length() - 1);
				if(lastCache != null)
					searchList.addAll(lastCache);
			}

			List<User> searchUsers = new ArrayList<User>();
			List<Object> search = search(content.toString());
			if(search != null && search.size() > 0){
				for (Object object : search) {
					searchUsers.add((User) object);
				}
	
				List<Object> temp = new ArrayList<Object>();
				temp.addAll(search);
				contentCacheList.put(content.length(), temp);
				contentLengthCacheList.put(content.length(), content.toString());
				V2Log.e(TAG, "put cache list : key --> " + content.length()
						+ " and value --> " + search.size());
				temp = null;
			}
			search = null;
			return searchUsers;
		}
	}

	private static List<Object> search(String content) {
		if (content != null && content.length() > 0) {
			if (!mIsStartedSearch) {
				mIsStartedSearch = true;
				searchList.addAll(receiveList);
			}

			V2Log.e(TAG, "Editable :" + content.toString());
			char[] charSimpleArray = content.toString()
					.toLowerCase(Locale.getDefault()).toCharArray();
			// 搜字母查询
			for (int i = startIndex; i < charSimpleArray.length; i++) {
				if (isChineseWord(charSimpleArray[i])) {
					V2Log.e(TAG, "--- " + charSimpleArray[i] + " ---is Chinese");
					searchCacheList = getSearchList(searchList,
							String.valueOf(charSimpleArray[i]),
							content.toString(), i, true, true);
				} else {
					V2Log.e(TAG, "--- " + charSimpleArray[i] + " ---not Chinese");
					searchCacheList = getSearchList(searchList,
							String.valueOf(charSimpleArray[i]),
							content.toString(), i, true, false);
				}

				searchList.clear();
				if (searchCacheList.size() > 0) {
					isShouldQP = false;
					singleCacheList.addAll(searchCacheList);
					searchCacheList.clear();
					
					V2Log.d(TAG, "简拼找到结果 , 全拼再搜索一遍");
					if(surplusList.size() > 0){
						searchCacheList.addAll(surplusList);
						surplusList.clear();
					}
					startQPSearch(content);
					searchList.addAll(singleCacheList);
					searchCacheList.clear();
					singleCacheList.clear();
				} else {
					isShouldQP = true;
					List<Object> cache = contentCacheList.get(content.length() - 1);
					if(cache != null)
						searchCacheList
								.addAll(contentCacheList.get(content.length() - 1));
					V2Log.e(TAG, "简拼没有结果 开启全拼搜索");
				}
			}
			

			if (isShouldQP) { // 如果长度大于5则不按首字母查询
				searchList.clear();
				startQPSearch(content);
			}
			startIndex++;
			return searchList;
		} else {
			clearAll();
		}
		return searchList;
	}

	public static void clearAll() {
		SearchContentProvider.closedDataBase();
		if (mIsStartedSearch) {
			receiveList.clear();
			searchCacheList.clear();
			searchList.clear();
			contentCacheList.clear();
			surplusList.clear();
			singleCacheList.clear();
			mIsStartedSearch = false;
			startIndex = 0;
		}
	}

	public static void startQPSearch(String content){
		content = content.toLowerCase();
		V2Log.e(TAG, "全拼搜索的集合大小：" + searchCacheList.size());
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < searchCacheList.size(); i++) {
			Object obj = searchCacheList.get(i);
			V2Log.e(TAG, "current search word : " + getObjectValue(obj));
			if(TextUtils.isEmpty(getObjectValue(obj))){
				return ;
			}
			
			// 获取名字，将名字变成拼音串起来
			//FIXME 包含有多音字，需要重新组合
			char[] charArray = getObjectValue(obj).toCharArray();
			for (char c : charArray) {
				String charStr = null;
				if (isChineseWord(c)) {
					charStr = GlobalConfig.allChinese.get(String
							.valueOf(c));
				} else {
					charStr = String.valueOf(c);
				}
				sb.append(charStr);
			}
			V2Log.e(TAG, "current searh material : " + sb.toString());
			String material = sb.toString().toLowerCase();
			// 判断该昵称第一个字母，与输入的第一字母是否匹配
			Character first = material.toCharArray()[0];
			char[] targetChars = content.toString().toCharArray();
			if (!first.equals(targetChars[0])) {
				isShouldAdd = true;
			} else {
				for (int j = 0; j < targetChars.length; j++) {
					if (j >= material.length()
							|| targetChars[j] != material.charAt(j)) {
						isShouldAdd = true;
						V2Log.e(TAG, "material not contains "
								+ targetChars[j]);
						break;
					}
					isShouldAdd = false;
				}
			}

			if (!isShouldAdd) {
				V2Log.e(TAG, "added ---------" + getObjectValue(obj));
				if (!searchList.contains(obj))
					searchList.add(obj);
			} else {
				if (searchList.contains(obj))
					searchList.remove(obj);
			}
			sb.delete(0, sb.length());
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
			String searchKey, String content, int index, boolean isFirstSearch,
			boolean isChinese) {
		V2Log.e(TAG, "getSearchList--> searchList : " + list.size()
				+ " | searchKey : " + searchKey + " | startIndex : " + index
				+ " | content : " + content);
		List<Object> tempList = new ArrayList<Object>();
		if (searchKey == null || searchKey.length() < 0) {
			return tempList;
		}

		String searchTarget;
		for (int i = 0; i < list.size(); i++) { // 一级循环，循环所有消息
			boolean isAdd = false;
			Object obj = list.get(i);
			// 判断是否能获取到消息item的名字
			if (getObjectValue(obj) != null) {
				// 将名字分割为字符数组遍历
				char[] charArray = getObjectValue(obj).toCharArray();
				for (int j = index; j < charArray.length; j++) { // 二级循环，循环消息名称
					searchTarget = String.valueOf(charArray[j]);
					if (isFirstSearch && isChinese) {
						if (searchKey.contains(searchTarget)) {
							isAdd = true;
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
								if (indexOf == 0) { // &&
													// content.indexOf(searchKey)
													// == index
									isAdd = true;
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
								isAdd = true;
								break;
							}
						}
					}
				}
				
				if(isAdd){
					tempList.add(obj);
				}
				else{
					surplusList.add(obj);
				}
			}
		}
		return tempList;
	}

	public static String getObjectValue(Object obj) {

		switch (type) {
		case TYPE_CONVERSATION:
			return ((ScrollItem) obj).cov.getName();
		case TYPE_ITEM_DATA:
			return ((User) obj).getName();
		case TYPE_WRAPPER:
			return ((Wrapper) obj).a.getAttName();
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
