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
			Date date = new Date(Long.parseLong(time) / 1000);
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

			listCrowd.add(new V2Group(Long.parseLong(crowdElement
					.getAttribute("id")), crowdElement.getAttribute("name"),
					V2Group.TYPE_CROWD, creator));
		}

		return listCrowd;
	}

	public static List<V2Group> parseContactsGroup(String xml) {
		Document doc = buildDocument(xml);
		if (doc == null) {
			return null;
		}
		if (doc.getChildNodes().getLength()<=0) {
			return null;
		}
		List<V2Group> list = new ArrayList<V2Group>();
		iterateNodeList(V2Group.TYPE_CONTACTS_GROUP, null, doc.getChildNodes().item(0).getChildNodes(),
				list);
		return list;
	}

	public static List<V2Group> parseOrgGroup(String xml) {
		Document doc = buildDocument(xml);
		if (doc == null) {
			return null;
		}
		if (doc.getChildNodes().getLength()<=0) {
			return null;
		}
		List<V2Group> list = new ArrayList<V2Group>();
		iterateNodeList(V2Group.TYPE_ORG, null, doc.getChildNodes().item(0).getChildNodes(), list);

		return list;
	}

	private static void iterateNodeList(int type, V2Group parent,
			NodeList gList, List<V2Group> list) {

		for (int j = 0; j < gList.getLength(); j++) {
			Element subGroupEl = (Element) gList.item(j);
			V2Group group = null;

			group = new V2Group(
					Long.parseLong(subGroupEl.getAttribute("id")),
					subGroupEl.getAttribute("name"), type);

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

	private static Document buildDocument(String xml) {
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
