package com.bizcom.util;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.V2.jni.util.EscapedcharactersProcessing;
import com.V2.jni.util.V2Log;
import com.bizcom.vo.V2Doc;
import com.bizcom.vo.V2Shape;
import com.bizcom.vo.V2ShapeEarser;
import com.bizcom.vo.V2ShapeEllipse;
import com.bizcom.vo.V2ShapeLine;
import com.bizcom.vo.V2ShapeMeta;
import com.bizcom.vo.V2ShapePoint;
import com.bizcom.vo.V2ShapeRect;
import com.bizcom.vo.VMessage;
import com.bizcom.vo.VMessageAbstractItem;
import com.bizcom.vo.VMessageAudioItem;
import com.bizcom.vo.VMessageFaceItem;
import com.bizcom.vo.VMessageFileItem;
import com.bizcom.vo.VMessageImageItem;
import com.bizcom.vo.VMessageLinkTextItem;
import com.bizcom.vo.VMessageTextItem;
import com.bizcom.vo.V2Doc.Page;

public class XmlParser {

	private static final String TAG = "XmlParser";

	public XmlParser() {
	}

	public static String parseForMessageUUID(String xml) {

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		InputStream is = null;
		String uuid = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			Document doc = dBuilder.parse(is);

			doc.getDocumentElement().normalize();
			NodeList ChatMsgData = doc.getElementsByTagName("TChatData");
			if (ChatMsgData.getLength() <= 0) {
				return null;
			}
			Element chatElement = (Element) ChatMsgData.item(0);
			uuid = chatElement.getAttribute("MessageID");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return uuid;
	}

	public static VMessage parseForMessage(VMessage vm) {
		String xml = vm.getmXmlDatas();
		if (xml == null)
			return vm;

		InputStream is = null;
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			Document doc = dBuilder.parse(is);

			doc.getDocumentElement().normalize();
			Element element = (Element) doc.getElementsByTagName("TChatData")
					.item(0);
			boolean isAutoReply = "True".equals(element
					.getAttribute("IsAutoReply"));
			vm.setAutoReply(isAutoReply);

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
						xml = EscapedcharactersProcessing.reverse(text);
						String newText = text.replace("0xD0xA", "\n");
						va = new VMessageTextItem(vm, newText);
						va.setNewLine(isNewLine);
					} else if (msgEl.getTagName().equals("TLinkTextChatItem")) {
						String text = msgEl.getAttribute("Text");
						String url = msgEl.getAttribute("URL");
						xml = EscapedcharactersProcessing.reverse(url);
						va = new VMessageLinkTextItem(vm, text, url);
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

					} else if (msgEl.getTagName().equals("TAudioChatItem")) {

						String uuid = msgEl.getAttribute("FileID");
						if (uuid == null) {
							V2Log.e("Invalid uuid ");
							continue;
						}
						String fileExt = msgEl.getAttribute("FileExt");
						String seconds = msgEl.getAttribute("Seconds");

						VMessageAudioItem vii = new VMessageAudioItem(vm, uuid,
								fileExt, Integer.valueOf(seconds));
						vii.setNewLine(isNewLine);

					} else if (msgEl.getTagName().equals("file")) {
						String uuid = msgEl.getAttribute("id");
						if (uuid == null) {
							V2Log.e("Invalid uuid ");
							continue;
						}

						String filePath = msgEl.getAttribute("name");
						VMessageFileItem vii = new VMessageFileItem(vm,
								filePath, 0);
						vii.setUuid(uuid);
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
				vii.setFilePath("wait");
			}
			vm.notReceiveImageSize = imgMsgItemNL.getLength();

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
						audioItemEl.getAttribute("FileExt"),
						Integer.parseInt(audioItemEl.getAttribute("Seconds")));
				vii.setNewLine(true);
				vii.setReceive(false);
				vii.setState(VMessageAbstractItem.STATE_NORMAL);
				vii.setReadState(VMessageAbstractItem.STATE_UNREAD);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static V2Doc.Doc parserDocPage(String docId, String xml) {
		V2Doc.Doc pr = new V2Doc.Doc();
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
			Page[] pages = new Page[pageList.getLength()];
			for (int i = 0; i < pageList.getLength(); i++) {
				Element page = (Element) pageList.item(i);
				String pid = page.getAttribute("id");
				if (pid == null) {
					continue;
				}
				int no = Integer.parseInt(pid);
				pages[no - 1] = new Page(no, docId, null);
			}
			pr.addPages(pages);
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
							earser.addPoint(Integer.parseInt(str[index]),
									Integer.parseInt(str[index + 1]));
							earser.addPoint(Integer.parseInt(str[index + 2]),
									Integer.parseInt(str[index + 3]));
							//
							// earser.lineToLine(Integer.parseInt(str[index]),
							// Integer.parseInt(str[index + 1]),
							// Integer.parseInt(str[index + 2]),
							// Integer.parseInt(str[index + 3]));
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
