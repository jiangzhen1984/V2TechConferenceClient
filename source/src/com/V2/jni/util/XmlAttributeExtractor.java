package com.V2.jni.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.V2.jni.ind.FileJNIObject;
import com.V2.jni.ind.V2Group;
import com.V2.jni.ind.V2User;

public class XmlAttributeExtractor {

	public static String extract(String str, String startStr, String endStr) {
		if (startStr == null || endStr == null || startStr.isEmpty()
				|| endStr.isEmpty()) {
			return null;
		}
		int start = str.indexOf(startStr);
		if (start == -1) {
			return null;
		}
		int len = startStr.length();
		int end = str.indexOf(endStr, start + len);
		if (end == -1) {
			return null;
		}
		return str.substring(start + len, end);
	}
	
	
	/**
	 * 
	 * @param xml
	 * @param tag
	 * @return
	 */
	public static List<V2User> parseUserList(String xml, String tag) {
		Document doc = buildDocument(xml);
		List<V2User> listUser = new ArrayList<V2User>();
		NodeList userNodeList = doc.getElementsByTagName(tag);
		Element userElement;

		for (int i = 0; i < userNodeList.getLength(); i++) {
			userElement = (Element) userNodeList.item(i);
			V2User user = null;
			String uid = userElement.getAttribute("id");
			if (uid != null && !uid.isEmpty()) {
				user = new V2User(Long.parseLong(uid));
				String name = userElement.getAttribute("nickname");
				user.name = name;
				listUser.add(user);
			}
		}

		return listUser;
	}

	public static List<V2Group> parseConference(String xml) {
		Document doc = buildDocument(xml);
		if (doc == null) {
			return null;
		}
		List<V2Group> listConf = new ArrayList<V2Group>();
		NodeList conferenceList = doc.getElementsByTagName("conf");
		Element conferenceElement;
		for (int i = 0; i < conferenceList.getLength(); i++) {
			conferenceElement = (Element) conferenceList.item(i);
			String chairManStr = conferenceElement.getAttribute("chairman");
			long cid = 0;
			if (chairManStr != null && !chairManStr.isEmpty()) {
				cid = Long.parseLong(chairManStr);
			}

			String time = conferenceElement.getAttribute("starttime");
			Long times = Long.valueOf(time) * 1000;
			Date date = new Date(times);
			String name = conferenceElement.getAttribute("subject");
			String uid = conferenceElement.getAttribute("createuserid");
			V2User user = null;
			if (uid != null && !uid.isEmpty()) {
				user = new V2User(Long.parseLong(uid));
			}

			listConf.add(new V2Group(Long.parseLong(conferenceElement
					.getAttribute("id")), name, V2Group.TYPE_CONF, user, date,
					new V2User(cid)));
		}
		return listConf;
	}

	public static List<V2Group> parseCrowd(String xml) {
		Document doc = buildDocument(xml);
		List<V2Group> listCrowd = new ArrayList<V2Group>();
		NodeList crowdList = doc.getElementsByTagName("crowd");
		Element crowdElement;

		for (int i = 0; i < crowdList.getLength(); i++) {
			crowdElement = (Element) crowdList.item(i);
			V2User creator = null;
			String uid = crowdElement.getAttribute("creatoruserid");
			if (uid != null && !uid.isEmpty()) {
				creator = new V2User(Long.parseLong(uid));
			}

			if (crowdElement.getAttribute("name") == null)
				V2Log.e("parseCrowd the name is wroing...the group is :"
						+ crowdElement.getAttribute("id"));

			V2Group crowd = new V2Group(Long.parseLong(crowdElement
					.getAttribute("id")), crowdElement.getAttribute("name"),
					V2Group.TYPE_CROWD, creator);
			crowd.brief = crowdElement.getAttribute("summary");
			crowd.announce = crowdElement.getAttribute("announcement");
			crowd.creator = creator;
			listCrowd.add(crowd);
		}

		return listCrowd;
	}

	public static List<V2Group> parseContactsGroup(String xml) {
		Document doc = buildDocument(xml);
		if (doc == null) {
			return null;
		}
		if (doc.getChildNodes().getLength() <= 0) {
			return null;
		}
		List<V2Group> list = new ArrayList<V2Group>();
		iterateNodeList(V2Group.TYPE_CONTACTS_GROUP, null, doc.getChildNodes()
				.item(0).getChildNodes(), list);
		return list;
	}

	public static List<V2Group> parseOrgGroup(String xml) {
		Document doc = buildDocument(xml);
		if (doc == null) {
			return null;
		}
		if (doc.getChildNodes().getLength() <= 0) {
			return null;
		}
		List<V2Group> list = new ArrayList<V2Group>();
		iterateNodeList(V2Group.TYPE_ORG, null, doc.getChildNodes().item(0)
				.getChildNodes(), list);

		return list;
	}

	private static void iterateNodeList(int type, V2Group parent,
			NodeList gList, List<V2Group> list) {

		for (int j = 0; j < gList.getLength(); j++) {
			Element subGroupEl = (Element) gList.item(j);
			V2Group group = null;

			group = new V2Group(Long.parseLong(subGroupEl.getAttribute("id")),
					subGroupEl.getAttribute("name"), type);
			// If type is contact and is first item, means this group is default
			if (type == V2Group.TYPE_CONTACTS_GROUP && j == 0) {
				group.isDefault = true;
				// TODO use localization
				group.name = "我的好友";
			}

			if (parent == null) {
				list.add(group);
			} else {
				parent.childs.add(group);
				group.parent = parent;
			}
			// Iterate sub group
			iterateNodeList(type, group, subGroupEl.getChildNodes(), null);
		}

	}

	public static List<FileJNIObject> parseFiles(String xml) {
		Document doc = buildDocument(xml);
		if (doc == null) {
			return null;
		}
		if (doc.getChildNodes().getLength() <= 0) {
			return null;
		}

		List<FileJNIObject> list = new ArrayList<FileJNIObject>();
		NodeList nList = doc.getChildNodes().item(0).getChildNodes();
		for (int j = 0; j < nList.getLength(); j++) {
			Element el = (Element) nList.item(j);
			// //<file encrypttype='1' id='C2A65B9B-63C7-4C9E-A8DD-F15F74ABA6CA'
			// * name='83025aafa40f4bfb24fdb8d1034f78f0f7361801.gif'
			// size='497236'
			// * time='1411112464' uploader='11029' url=
			// *
			// 'http://192.168.0.38:8090/crowd/C2A65B9B-63C7-4C9E-A8DD-F15F74ABA6CA/C2A65B9B-63C7-4C9E-A8DD-F15F74ABA6CA/83025aafa40f4bfb24fdb8d1034f78f0f7361801.gif'/><
			String id = el.getAttribute("id");
			String name = el.getAttribute("name");
			String uploader = el.getAttribute("uploader");
			String url = el.getAttribute("url");
			String size = el.getAttribute("size");
			int index = name.lastIndexOf("/");
			if (index != -1) {
				name = name.substring(index);
			}
			
			FileJNIObject file = new FileJNIObject(new V2User(
					Long.parseLong(uploader)), id, name, Long.parseLong(size),
					1);
			file.url = url;
			list.add(file);
		}
		return list;
	}

	public static Document buildDocument(String xml) {
		if (xml == null || xml.isEmpty()) {
			V2Log.e(" conference xml is null");
			return null;
		}

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		InputStream is = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			Document doc = dBuilder.parse(is);

			doc.getDocumentElement().normalize();
			return doc;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}
}
