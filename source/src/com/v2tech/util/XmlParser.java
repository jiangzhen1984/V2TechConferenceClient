package com.v2tech.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.v2tech.logic.ConferenceGroup;
import com.v2tech.logic.ContactGroup;
import com.v2tech.logic.Group;
import com.v2tech.logic.NormalGroup;
import com.v2tech.logic.Group.GroupType;
import com.v2tech.logic.OrgGroup;
import com.v2tech.view.vo.V2Doc;
import com.v2tech.view.vo.V2Doc.Page;

public class XmlParser {

	public XmlParser() {
	}

	/**
	 * 
	 * type org(1): type contact(2) <xml><pubgroup id='61'
	 * name='ronghuo的组织'><pubgroup id='21' name='1'/></pubgroup></xml>
	 * 
	 * type Chating(3) <xml><crowd announcement='' authtype='0'
	 * creatoruserid='1120' id='43' name='vvvvv' size='100' summary=''/></xml> <br>
	 * 
	 * type conference(4): <xml><conf createuserid='1124' id='513891897880'
	 * start time='1389189927' subject='est'/><conf createuserid='1124'
	 * id='513891899176' starttime='1389190062' subject='eee'/></xml>
	 * 
	 * @param xml
	 * @return
	 */
	public static List<Group> parserFromXml(int type, String xml) {
		List<Group> list = new ArrayList<Group>();

		InputStream is = null;

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			Document doc = dBuilder.parse(is);

			doc.getDocumentElement().normalize();

			if (type == GroupType.ORG.intValue()
					|| type == Group.GroupType.CONTACT.intValue()) {
				NodeList gList = doc.getChildNodes().item(0).getChildNodes();
				Element element;
				for (int i = 0; i < gList.getLength(); i++) {
					element = (Element) gList.item(i);
					Group g = null;
					if (type == GroupType.ORG.intValue()) {
						g = new OrgGroup(Long.parseLong(element
								.getAttribute("id")),
								element.getAttribute("name"));
					} else if (type == Group.GroupType.CONTACT.intValue()) {
						g = new ContactGroup(Long.parseLong(element
								.getAttribute("id")),
								element.getAttribute("name"));
					}
					list.add(g);

					//iterate all sub groups
					iterateNodeList(type, g, element.getChildNodes());
				}

			} else if (type == GroupType.CONFERENCE.intValue()) {
				NodeList conferenceList = doc.getElementsByTagName("conf");
				Element conferenceElement;

				for (int i = 0; i < conferenceList.getLength(); i++) {
					conferenceElement = (Element) conferenceList.item(i);
					list.add(new ConferenceGroup(Long
							.parseLong(conferenceElement.getAttribute("id")),
							GroupType.fromInt(type), conferenceElement
									.getAttribute("subject"), conferenceElement
									.getAttribute("createuserid"),
							conferenceElement.getAttribute("starttime")));
				}
			} else if (type == GroupType.CHATING.intValue()) {
				NodeList conferenceList = doc.getElementsByTagName("crowd");
				Element conferenceElement;

				for (int i = 0; i < conferenceList.getLength(); i++) {
					conferenceElement = (Element) conferenceList.item(i);
					list.add(new NormalGroup(Long.parseLong(conferenceElement
							.getAttribute("id")), GroupType.fromInt(type),
							conferenceElement.getAttribute("name"), Long
									.parseLong(conferenceElement
											.getAttribute("creatoruserid"))));
				}
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return list;
	}

	private static void iterateNodeList(int type, Group g, NodeList gList) {

		for (int j = 0; j < gList.getLength(); j++) {
			Element subGroupEl = (Element) gList.item(j);
			Group subGroup = null;
			if (type == GroupType.ORG.intValue()) {

				subGroup = new OrgGroup(Long.parseLong(subGroupEl
						.getAttribute("id")), subGroupEl.getAttribute("name"));

			} else if (type == Group.GroupType.CONTACT.intValue()) {
				subGroup = new ContactGroup(Long.parseLong(subGroupEl
						.getAttribute("id")), subGroupEl.getAttribute("name"));
			}

			g.addGroupToGroup(subGroup);
			//Iterate sub group
			iterateNodeList(type, subGroup, subGroupEl.getChildNodes());
		}

	}
	
	
	
	
	public static V2Doc.PageArray parserDocPage(String docId, String xml) {
		V2Doc.PageArray pr = new V2Doc.PageArray();
		pr.setDocId(docId);
		InputStream is = null;

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			Document doc = dBuilder.parse(is);
			doc.getDocumentElement().normalize();
			
			NodeList pageList = doc.getElementsByTagName("page");
			Page[] p = new Page[pageList.getLength()];
			for (int i =0; i < pageList.getLength(); i++) {
				Element page = (Element)pageList.item(i);
				String pid = page.getAttribute("id");
				if (pid == null) {
					continue;
				}
				int no = Integer.parseInt(pid);
				p[no-1] = new Page(no, docId, null);
			}
			pr.setPr(p);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return pr;
	}
}
