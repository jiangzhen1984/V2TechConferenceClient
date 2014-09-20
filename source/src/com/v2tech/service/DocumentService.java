package com.v2tech.service;

import android.os.Handler;

import com.V2.jni.WBRequest;
import com.V2.jni.WBRequestCallback;
import com.V2.jni.util.V2Log;
import com.v2tech.util.XmlParser;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;
import com.v2tech.vo.V2BlankBoardDoc;
import com.v2tech.vo.V2Doc;
import com.v2tech.vo.V2ImageDoc;
import com.v2tech.vo.V2ShapeMeta;

/**
 * @author jiangzhen
 * 
 */
public class DocumentService extends AbstractHandler {

	private static final int KEY_NEW_DOC_LISTENER = 50;

	private static final int KEY_DOC_PAGE_NOTIFY_LISTENER = 52;

	private static final int KEY_PAGE_ACTIVE_NOTIFY_LISTENER = 53;

	private static final int KEY_DOC_PAGE_ADD_NOTIFY_LISTENER = 54;

	private static final int KEY_DOC_DISPLAY_NOTIFY_LISTENER = 55;

	private static final int KEY_DOC_CLOSE_NOTIFY_LISTENER = 56;

	private static final int KEY_PAGE_CANVAS_NOTIFY_LISTENER = 57;

	private WBRequestCallbackCB cb;

	public DocumentService() {
		cb = new WBRequestCallbackCB();
		WBRequest.getInstance().addCallbacks(cb);
	}

	/**
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
	public void registerNewDocNotification(Handler h, int what, Object obj) {
		registerListener(KEY_NEW_DOC_LISTENER, h, what, obj);
	}

	/**
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
	public void unRegisterNewDocNotification(Handler h, int what, Object obj) {
		unRegisterListener(KEY_NEW_DOC_LISTENER, h, what, obj);
	}

	public void registerDocPageNotification(Handler h, int what, Object obj) {
		registerListener(KEY_DOC_PAGE_NOTIFY_LISTENER, h, what, obj);
	}

	public void unRegisterDocPageNotification(Handler h, int what, Object obj) {
		unRegisterListener(KEY_DOC_PAGE_NOTIFY_LISTENER, h, what, obj);
	}

	public void registerdocPageActiveNotification(Handler h, int what,
			Object obj) {
		registerListener(KEY_PAGE_ACTIVE_NOTIFY_LISTENER, h, what, obj);
	}

	public void unRegisterdocPageActiveNotification(Handler h, int what,
			Object obj) {
		unRegisterListener(KEY_PAGE_ACTIVE_NOTIFY_LISTENER, h, what, obj);
	}

	public void registerDocDisplayNotification(Handler h, int what, Object obj) {
		registerListener(KEY_DOC_DISPLAY_NOTIFY_LISTENER, h, what, obj);
	}

	public void unRegisterDocDisplayNotification(Handler h, int what, Object obj) {
		unRegisterListener(KEY_DOC_DISPLAY_NOTIFY_LISTENER, h, what, obj);
	}

	public void registerDocPageAddedNotification(Handler h, int what, Object obj) {
		registerListener(KEY_DOC_PAGE_ADD_NOTIFY_LISTENER, h, what, obj);
	}

	public void unRegisterDocPageAddedNotification(Handler h, int what,
			Object obj) {
		unRegisterListener(KEY_DOC_PAGE_ADD_NOTIFY_LISTENER, h, what, obj);
	}

	public void registerDocClosedNotification(Handler h, int what, Object obj) {
		registerListener(KEY_DOC_CLOSE_NOTIFY_LISTENER, h, what, obj);
	}

	public void unRegisterDocClosedNotification(Handler h, int what, Object obj) {
		unRegisterListener(KEY_DOC_CLOSE_NOTIFY_LISTENER, h, what, obj);
	}

	public void registerPageCanvasUpdateNotification(Handler h, int what,
			Object obj) {
		registerListener(KEY_PAGE_CANVAS_NOTIFY_LISTENER, h, what, obj);
	}

	public void unRegisterPageCanvasUpdateNotification(Handler h, int what,
			Object obj) {
		unRegisterListener(KEY_PAGE_CANVAS_NOTIFY_LISTENER, h, what, obj);
	}
	
	
	

	@Override
	public void clearCalledBack() {
		WBRequest.getInstance().removeCallback(cb);
	}




	class WBRequestCallbackCB implements WBRequestCallback {

		@Override
		public void OnWBoardChatInviteCallback(long nGroupID,
				int nBusinessType, long nFromUserID, String szWBoardID,
				int nWhiteIndex, String szFileName, int type) {
			Group g = GlobalHolder.getInstance().getGroupById(
					GroupType.CONFERENCE.intValue(), nGroupID);
			User u = GlobalHolder.getInstance().getUser(nFromUserID);
			int pos = szFileName.lastIndexOf("/");
			if (pos == -1) {
				pos = szFileName.lastIndexOf("\\");
				if (pos != -1) {
					szFileName = szFileName.substring(pos + 1);
				}
			}

			V2Doc doc = null;
			//Blank board
			if (type == 1) {
				doc =  new V2BlankBoardDoc(
						szWBoardID, "Blank board", g, nBusinessType, u);
				
			} else {
				doc =  new V2ImageDoc(
						szWBoardID, szFileName, g, nBusinessType, u);
			}
			notifyListenerWithPending(KEY_NEW_DOC_LISTENER, 0, 0,doc);

		}

		@Override
		public void OnWBoardPageListCallback(String szWBoardID,
				String szPageData, int nPageID) {
			notifyListenerWithPending(KEY_DOC_PAGE_NOTIFY_LISTENER, 0, 0,  XmlParser.parserDocPage(szWBoardID, szPageData));
		}

		@Override
		public void OnWBoardActivePageCallback(long nUserID, String szWBoardID,
				int nPageID) {
			notifyListenerWithPending(KEY_PAGE_ACTIVE_NOTIFY_LISTENER, 0, 0,
					new V2Doc.Page(nPageID, szWBoardID, null));
		}

		@Override
		public void OnWBoardDocDisplayCallback(String szWBoardID, int nPageID,
				String szFileName, int result) {

			notifyListenerWithPending(KEY_DOC_DISPLAY_NOTIFY_LISTENER, 0, 0,
					new V2Doc.Page(nPageID, szWBoardID, szFileName));
		}

		@Override
		public void OnWBoardClosedCallback(long nGroupID, int nBusinessType,
				long nUserID, String szWBoardID) {
			Group g = GlobalHolder.getInstance().getGroupById(
					GroupType.CONFERENCE.intValue(), nGroupID);
			User u = GlobalHolder.getInstance().getUser(nUserID);

			notifyListenerWithPending(KEY_DOC_CLOSE_NOTIFY_LISTENER, 0, 0, new V2Doc(
					szWBoardID, "", g, nBusinessType, u));
		}

		@Override
		public void OnWBoardAddPageCallback(String szWBoardID, int nPageID) {
			notifyListenerWithPending(KEY_DOC_PAGE_ADD_NOTIFY_LISTENER, 0, 0,
					new V2Doc.Page(nPageID, szWBoardID, ""));
		}

		@Override
		public void OnRecvAddWBoardDataCallback(String szWBoardID, int nPageID,
				String szDataID, String szData) {
			V2ShapeMeta meta = XmlParser.parseV2ShapeMetaSingle(szData);
			if (meta == null) {
				V2Log.e("No shape data");
				return;
			}
			meta.setDocId(szWBoardID);
			meta.setPageNo(nPageID);
			notifyListenerWithPending(KEY_PAGE_CANVAS_NOTIFY_LISTENER, 0, 0, meta);
		}

		@Override
		public void OnRecvAppendWBoardDataCallback(String szWBoardID,
				int nPageID, String szDataID, String szData) {
			V2ShapeMeta meta = XmlParser.parseV2ShapeMetaSingle(szData);
			if (meta == null) {
				V2Log.e("No shape data");
				return;
			}
			meta.setDocId(szWBoardID);
			meta.setPageNo(nPageID);
			notifyListenerWithPending(KEY_PAGE_CANVAS_NOTIFY_LISTENER, 0, 0, meta);
		}

	}

}
