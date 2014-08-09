package com.v2tech.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.v2tech.vo.ConferenceGroup;
import com.v2tech.vo.ContactGroup;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.OrgGroup;
import com.v2tech.vo.User;
import com.v2tech.vo.V2Doc;
import com.v2tech.vo.V2Doc.Page;
import com.v2tech.vo.V2Shape;
import com.v2tech.vo.V2ShapeEarser;
import com.v2tech.vo.V2ShapeEllipse;
import com.v2tech.vo.V2ShapeLine;
import com.v2tech.vo.V2ShapeMeta;
import com.v2tech.vo.V2ShapePoint;
import com.v2tech.vo.V2ShapeRect;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFaceItem;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VMessageTextItem;

public class XmlParser {

	public XmlParser() {
	}

	public static VMessage parseForMessage(User from, User to, Date date,
			String xml) {
		VMessage vm = new VMessage(from, to, date);
		InputStream is = null;
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			Document doc = dBuilder.parse(is);

			doc.getDocumentElement().normalize();
			NodeList textMsgItemNL = doc.getElementsByTagName("ItemList");
			if (textMsgItemNL.getLength() <= 0) {
				return null;
			}
			Element msgEl = (Element) textMsgItemNL.item(0);
			NodeList itemList = msgEl.getChildNodes();

			for (int i = 0; i < itemList.getLength(); i++) {
				Node n = itemList.item(i);
				if (n instanceof Element) {
					msgEl = (Element) itemList.item(i);
					boolean isNewLine = "True".equals(msgEl
							.getAttribute("NewLine"));
					VMessageAbstractItem va = null;
					if (msgEl.getTagName().equals("TTextChatItem")) {
						String text = msgEl.getAttribute("Text");
						text = text.replaceAll("&lt;", "<");
						text = text.replaceAll("&gt;", ">");
						text = text.replaceAll("&apos;", "'");
						text = text.replaceAll("&quot;", "\"");
						text = text.replaceAll("&amp;", "&");

						va = new VMessageTextItem(vm, text);
						va.setNewLine(isNewLine);
					} else if (msgEl.getTagName().equals("TSysFaceChatItem")) {
						String fileName = msgEl.getAttribute("FileName");
						int start = fileName.indexOf(".");
						int index = Integer.parseInt(fileName.substring(0,
								start));
						va = new VMessageFaceItem(vm, index);
						va.setNewLine(isNewLine);
					} else if (msgEl.getTagName().equals("TPictureChatItem")) {

						String uuid = msgEl.getAttribute("GUID");
						if (uuid == null) {
							V2Log.e("Invalid uuid ");
							continue;
						}
						VMessageImageItem vii = new VMessageImageItem(vm, uuid,
								msgEl.getAttribute("FileExt"));
						vii.setNewLine(isNewLine);

					}

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return vm;
	}

	public static void extraImageMetaFrom(VMessage vm, String xml) {
		InputStream is = null;

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			Document doc = dBuilder.parse(is);

			doc.getDocumentElement().normalize();

			NodeList imgMsgItemNL = doc
					.getElementsByTagName("TPictureChatItem");
			for (int i = 0; i < imgMsgItemNL.getLength(); i++) {
				Element msgEl = (Element) imgMsgItemNL.item(i);
				String uuid = msgEl.getAttribute("GUID");
				if (uuid == null) {
					V2Log.e("Invalid uuid ");
					continue;
				}
				boolean isNewLine = "True"
						.equals(msgEl.getAttribute("NewLine"));
				VMessageImageItem vii = new VMessageImageItem(vm, uuid,
						msgEl.getAttribute("FileExt"));
				vii.setNewLine(isNewLine);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void extraAudioMetaFrom(VMessage vm, String xml) {
		InputStream is = null;

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			Document doc = dBuilder.parse(is);

			doc.getDocumentElement().normalize();

			NodeList audioMsgItemNL = doc
					.getElementsByTagName("TAudioChatItem");
			for (int i = 0; i < audioMsgItemNL.getLength(); i++) {
				Element audioItemEl = (Element) audioMsgItemNL.item(i);
				String uuid = audioItemEl.getAttribute("FileID");
				if (uuid == null) {
					V2Log.e("Invalid uuid ");
					continue;
				}
				VMessageAudioItem vii = new VMessageAudioItem(vm, uuid,
						audioItemEl.getAttribute("FileExt"), null,
						Integer.parseInt(audioItemEl.getAttribute("Seconds")));
				vii.setNewLine(true);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

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

					// iterate all sub groups
					iterateNodeList(type, g, element.getChildNodes());
				}

			} else if (type == GroupType.CONFERENCE.intValue()) {
				NodeList conferenceList = doc.getElementsByTagName("conf");
				Element conferenceElement;
				for (int i = 0; i < conferenceList.getLength(); i++) {
					conferenceElement = (Element) conferenceList.item(i);
					String chairManStr = conferenceElement
							.getAttribute("chairman");
					long cid = 0;
					if (chairManStr != null && !chairManStr.isEmpty()) {
						cid = Long.parseLong(chairManStr);
					}

					list.add(new ConferenceGroup(Long
							.parseLong(conferenceElement.getAttribute("id")),
							GroupType.fromInt(type), conferenceElement
									.getAttribute("subject"), conferenceElement
									.getAttribute("createuserid"),
							conferenceElement.getAttribute("starttime"), cid));
				}
			} else if (type == GroupType.CHATING.intValue()) {
				NodeList conferenceList = doc.getElementsByTagName("crowd");
				Element conferenceElement;

				for (int i = 0; i < conferenceList.getLength(); i++) {
					conferenceElement = (Element) conferenceList.item(i);
					list.add(new CrowdGroup(Long.parseLong(conferenceElement
							.getAttribute("id")), conferenceElement
							.getAttribute("name"), Long
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
			// Iterate sub group
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
			for (int i = 0; i < pageList.getLength(); i++) {
				Element page = (Element) pageList.item(i);
				String pid = page.getAttribute("id");
				if (pid == null) {
					continue;
				}
				int no = Integer.parseInt(pid);
				p[no - 1] = new Page(no, docId, null);
			}
			pr.addPages(p);
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

	/**
	 * 
	 * @param xml
	 * @return
	 */
	public static List<V2ShapeMeta> parseV2ShapeMeta(String xml) {
		List<V2ShapeMeta> metaList = new ArrayList<V2ShapeMeta>();

		InputStream is = null;
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			Document doc = dBuilder.parse(is);
			doc.getDocumentElement().normalize();

			NodeList nodeList = null;
			// FIXME optimize code
			nodeList = doc.getElementsByTagName("TBeelineMeta");
			if (nodeList == null || nodeList.getLength() <= 0) {
				nodeList = doc.getElementsByTagName("TFreedomLineMeta");
			}
			for (int i = 0; i < nodeList.getLength(); i++) {
				Element e = (Element) nodeList.item(i);

				V2ShapeMeta meta = null;
				for (V2ShapeMeta m : metaList) {
					if (m.getId().equals(e.getAttribute("ID"))) {
						meta = m;
						break;
					}
				}
				if (meta == null) {
					new V2ShapeMeta(e.getAttribute("ID"));
				}

				V2ShapePoint[] points = null;
				NodeList shapeDataList = e.getChildNodes();
				V2ShapeLine shape = new V2ShapeLine(points);

				for (int j = 0; j < shapeDataList.getLength(); j++) {
					Element shapeE = (Element) shapeDataList.item(j);

					if (shapeE.getTagName().equals("Points")) {
						String pointsStr = e.getTextContent();
						String[] str = pointsStr.split(" ");
						points = new V2ShapePoint[str.length / 2];
						for (int index = 0, pi = 0; index < points.length; index++, pi += 2) {
							points[index] = new V2ShapePoint(
									Integer.parseInt(str[pi]),
									Integer.parseInt(str[pi + 1]));
						}
					} else if (shapeE.getTagName().equals("Pen")) {
						shape.setWidth(Integer.parseInt(shapeE
								.getAttribute("Width")));
						shape.setColor(Integer.parseInt(shapeE
								.getAttribute("Color")));
					}

				}

				meta.addShape(shape);
				metaList.add(meta);
			}

			boolean isRect = true;
			nodeList = doc.getElementsByTagName("TRectangleMeta");
			if (nodeList == null || nodeList.getLength() <= 0) {
				nodeList = doc.getElementsByTagName("TEllipseMeta");
				isRect = false;
			}

			for (int i = 0; i < nodeList.getLength(); i++) {
				Element e = (Element) nodeList.item(i);

				V2ShapeMeta meta = new V2ShapeMeta(e.getAttribute("ID"));

				NodeList shapeDataList = e.getChildNodes();
				V2Shape shape = null;

				for (int j = 0; j < shapeDataList.getLength(); j++) {
					Element shapeE = (Element) shapeDataList.item(j);

					if (shapeE.getTagName().equals("Points")) {
						String pointsStr = e.getTextContent();
						String[] str = pointsStr.split(" ");
						if (str.length == 4) {
							if (isRect) {
								shape = new V2ShapeRect(
										Integer.parseInt(str[0]),
										Integer.parseInt(str[1]),
										Integer.parseInt(str[2]),
										Integer.parseInt(str[3]));
							} else {
								shape = new V2ShapeEllipse(
										Integer.parseInt(str[0]),
										Integer.parseInt(str[1]),
										Integer.parseInt(str[2]),
										Integer.parseInt(str[3]));
							}
						} else {
							V2Log.e("Incorrect data ");
						}
					} else if (shapeE.getTagName().equals("Pen")) {
						shape.setWidth(Integer.parseInt(shapeE
								.getAttribute("Width")));
						shape.setColor(Integer.parseInt(shapeE
								.getAttribute("Color")));
					}

				}

				meta.addShape(shape);
				metaList.add(meta);
			}

			if (nodeList == null || nodeList.getLength() <= 0) {
				V2Log.w(" No avaliable meta");
				return metaList;
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

		return metaList;

	}

	/**
	 * FIXME optimze code
	 * 
	 * @param xml
	 * @return
	 */
	public static V2ShapeMeta parseV2ShapeMetaSingle(String xml) {

		InputStream is = null;
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			Document doc = dBuilder.parse(is);
			doc.getDocumentElement().normalize();

			NodeList nodeList = null;
			// FIXME optimize code
			nodeList = doc.getElementsByTagName("TBeelineMeta");
			if (nodeList == null || nodeList.getLength() <= 0) {
				nodeList = doc.getElementsByTagName("TFreedomLineMeta");
			}
			V2ShapeMeta meta = null;
			for (int i = 0; i < nodeList.getLength(); i++) {
				Element e = (Element) nodeList.item(i);

				meta = new V2ShapeMeta(e.getAttribute("ID"));

				V2ShapePoint[] points = null;
				NodeList shapeDataList = e.getChildNodes();
				V2ShapeLine shape = new V2ShapeLine(points);

				for (int j = 0; j < shapeDataList.getLength(); j++) {
					if (shapeDataList.item(j).getNodeType() != Element.ELEMENT_NODE) {
						continue;
					}
					Element shapeE = (Element) shapeDataList.item(j);

					if (shapeE.getTagName().equals("Points")) {
						String pointsStr = e.getTextContent().trim();
						String[] str = pointsStr.split(" ");
						points = new V2ShapePoint[str.length / 2];
						for (int index = 0, pi = 0; index < points.length; index++, pi += 2) {
							points[index] = new V2ShapePoint(
									Integer.parseInt(str[pi]),
									Integer.parseInt(str[pi + 1]));
						}
						shape.addPoints(points);
					} else if (shapeE.getTagName().equals("Pen")) {
						shape.setWidth(Integer.parseInt(shapeE
								.getAttribute("Width")));
						shape.setColor(Integer.parseInt(shapeE
								.getAttribute("Color")));
					}

				}

				meta.addShape(shape);
			}

			boolean isRect = true;
			nodeList = doc.getElementsByTagName("TRectangleMeta");
			if (nodeList == null || nodeList.getLength() <= 0) {
				nodeList = doc.getElementsByTagName("TEllipseMeta");
				isRect = false;
			}

			for (int i = 0; i < nodeList.getLength(); i++) {
				Element e = (Element) nodeList.item(i);

				meta = new V2ShapeMeta(e.getAttribute("ID"));

				NodeList shapeDataList = e.getChildNodes();
				V2Shape shape = null;

				for (int j = 0; j < shapeDataList.getLength(); j++) {
					if (shapeDataList.item(j).getNodeType() != Element.ELEMENT_NODE) {
						continue;
					}
					Element shapeE = (Element) shapeDataList.item(j);

					if (shapeE.getTagName().equals("Points")) {
						String pointsStr = e.getTextContent().trim();
						String[] str = pointsStr.split(" ");
						if (str.length == 4) {
							if (isRect) {
								shape = new V2ShapeRect(
										Integer.parseInt(str[0]),
										Integer.parseInt(str[1]),
										Integer.parseInt(str[2]),
										Integer.parseInt(str[3]));
							} else {
								shape = new V2ShapeEllipse(
										Integer.parseInt(str[0]),
										Integer.parseInt(str[1]),
										Integer.parseInt(str[2]),
										Integer.parseInt(str[3]));
							}
						} else {
							V2Log.e("Incorrect data ");
						}
					} else if (shapeE.getTagName().equals("Pen")) {
						shape.setWidth(Integer.parseInt(shapeE
								.getAttribute("Width")));
						shape.setColor(Integer.parseInt(shapeE
								.getAttribute("Color")));
					}

				}

				meta.addShape(shape);
			}

			nodeList = doc.getElementsByTagName("TEraseLineMeta");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Element e = (Element) nodeList.item(i);
				meta = new V2ShapeMeta(e.getAttribute("ID"));
				
				NodeList shapeDataList = e.getChildNodes();

				V2ShapeEarser earser = new V2ShapeEarser();
				for (int j = 0; j < shapeDataList.getLength(); j++) {
					if (shapeDataList.item(j).getNodeType() != Element.ELEMENT_NODE) {
						continue;
					}
					Element shapeE = (Element) shapeDataList.item(j);

					if (shapeE.getTagName().equals("Points")) {
						String pointsStr = e.getTextContent().trim();
						String[] str = pointsStr.split(" ");
						int len = str.length / 4;
						for (int index = 0; index < len; index += 4) {
							earser.lineToLine(Integer.parseInt(str[index]),
									Integer.parseInt(str[index + 2]),
									Integer.parseInt(str[index + 3]),
									Integer.parseInt(str[index + 3]));
						}
					} else if (shapeE.getTagName().equals("Pen")) {

					}
				}
				meta.addShape(earser);

			}

			return meta;

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
		return null;

	}
}
