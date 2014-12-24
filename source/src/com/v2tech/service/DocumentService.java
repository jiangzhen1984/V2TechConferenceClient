package com.v2tech.service;

import android.os.Handler;

import com.V2.jni.GroupRequest;
import com.V2.jni.GroupRequestCallbackAdapter;
import com.V2.jni.WBRequest;
import com.V2.jni.WBRequestCallback;
import com.V2.jni.ind.V2Document;
import com.V2.jni.util.V2Log;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.util.XmlParser;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.User;
import com.v2tech.vo.V2BlankBoardDoc;
import com.v2tech.vo.V2Doc;
import com.v2tech.vo.V2ImageDoc;
import com.v2tech.vo.V2ShapeMeta;

/**
 * Use to handle document business.<br>
 * 
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
	private GroupRequestCallbackCB grCB;

	public DocumentService() {
		cb = new WBRequestCallbackCB();
		WBRequest.getInstance().addCallbacks(cb);
		grCB = new GroupRequestCallbackCB();
		GroupRequest.getInstance().addCallback(grCB);
	}

	/**
	 * Register listener for new document notification
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
	public void registerNewDocNotification(Handler h, int what, Object obj) {
		registerListener(KEY_NEW_DOC_LISTENER, h, what, obj);
	}

	/**
	 * unRegister listener for new document notification
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
	public void unRegisterNewDocNotification(Handler h, int what, Object obj) {
		unRegisterListener(KEY_NEW_DOC_LISTENER, h, what, obj);
	}

	/**
	 * Register listener for document page updated notification
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
	public void registerDocPageNotification(Handler h, int what, Object obj) {
		registerListener(KEY_DOC_PAGE_NOTIFY_LISTENER, h, what, obj);
	}

	/**
	 * unRegister listener for document page updated notification
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
	public void unRegisterDocPageNotification(Handler h, int what, Object obj) {
		unRegisterListener(KEY_DOC_PAGE_NOTIFY_LISTENER, h, what, obj);
	}

	/**
	 * Register listener for document page activate notification
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
	public void registerdocPageActiveNotification(Handler h, int what,
			Object obj) {
		registerListener(KEY_PAGE_ACTIVE_NOTIFY_LISTENER, h, what, obj);
	}

	/**
	 * unRegister listener for document page activate notification
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
	public void unRegisterdocPageActiveNotification(Handler h, int what,
			Object obj) {
		unRegisterListener(KEY_PAGE_ACTIVE_NOTIFY_LISTENER, h, what, obj);
	}

	/**
	 * Register listener for document activation notification
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
	public void registerDocDisplayNotification(Handler h, int what, Object obj) {
		registerListener(KEY_DOC_DISPLAY_NOTIFY_LISTENER, h, what, obj);
	}

	/**
	 * unRegister listener for document page activation notification
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
	public void unRegisterDocDisplayNotification(Handler h, int what, Object obj) {
		unRegisterListener(KEY_DOC_DISPLAY_NOTIFY_LISTENER, h, what, obj);
	}

	/**
	 * Register listener for document add new page notification
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
	public void registerDocPageAddedNotification(Handler h, int what, Object obj) {
		registerListener(KEY_DOC_PAGE_ADD_NOTIFY_LISTENER, h, what, obj);
	}

	/**
	 * unRegister listener for document add new page notification
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
	public void unRegisterDocPageAddedNotification(Handler h, int what,
			Object obj) {
		unRegisterListener(KEY_DOC_PAGE_ADD_NOTIFY_LISTENER, h, what, obj);
	}

	/**
	 * Register listener for document closed notification
	 * 
	 * @param h
	 * @param what
	 * @param obj
	 */
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

	public void switchDoc(V2Doc doc, boolean syncFlag, MessageListener listener) {
		if (doc == null) {
			if (listener != null) {
				super.sendResult(listener, new JNIResponse(
						JNIResponse.Result.INCORRECT_PAR));
			}
			return;
		}
		
		V2Log.e(doc.getId()+"   ====>"+ doc.getActivatePage().getNo());
		WBRequest.getInstance().ActivePage(doc.getSharedUser().getmUserId() , doc.getId(), doc.getActivatePage().getNo(), 0,  syncFlag);
	}

	@Override
	public void clearCalledBack() {
		WBRequest.getInstance().removeCallback(cb);
		GroupRequest.getInstance().removeCallback(grCB);
	}

	class GroupRequestCallbackCB extends GroupRequestCallbackAdapter {

		@Override
		public void OnGroupWBoardNotification(V2Document doc, DocOpt opt) {
			if (doc == null) {
				return;
			}
			Group g = GlobalHolder.getInstance().getGroupById(
					GroupType.CONFERENCE.intValue(), doc.mGroup.id);
			switch (opt) {
			case CREATE: {
				V2Doc v2doc = null;
				// Blank board
				if (doc.mType == V2Document.Type.BLANK_BOARD) {
					v2doc = new V2BlankBoardDoc(doc.mId, "Blank board"
							+ doc.mIndex, g, 0, null);

				} else {
					String name = doc.mFileName;
					int pos = doc.mFileName.lastIndexOf("/");
					if (pos == -1) {
						pos = doc.mFileName.lastIndexOf("\\");
						if (pos != -1) {
							name = doc.mFileName.substring(pos + 1);
						}
					}
					v2doc = new V2ImageDoc(doc.mId, name, g, 0, null);
				}
				notifyListenerWithPending(KEY_NEW_DOC_LISTENER, 0, 0, v2doc);
			}
				break;
			case DESTROY: {
				notifyListenerWithPending(KEY_DOC_CLOSE_NOTIFY_LISTENER, 0, 0,
						new V2Doc(doc.mId, "", g, 0, null));
			}
				break;
			case RENAME:
				break;
			default:
				break;

			}
		}

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
			// Blank board
			if (type == 1) {
				doc = new V2BlankBoardDoc(szWBoardID, "Blank board", g,
						nBusinessType, u);

			} else {
				doc = new V2ImageDoc(szWBoardID, szFileName, g, nBusinessType,
						u);
			}
			notifyListenerWithPending(KEY_NEW_DOC_LISTENER, 0, 0, doc);

		}

		@Override
		public void OnWBoardPageListCallback(String szWBoardID,
				String szPageData, int nPageID) {
			notifyListenerWithPending(KEY_DOC_PAGE_NOTIFY_LISTENER, 0, 0,
					XmlParser.parserDocPage(szWBoardID, szPageData));
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

			notifyListenerWithPending(KEY_DOC_CLOSE_NOTIFY_LISTENER, 0, 0,
					new V2Doc(szWBoardID, "", g, nBusinessType, u));
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
			notifyListenerWithPending(KEY_PAGE_CANVAS_NOTIFY_LISTENER, 0, 0,
					meta);
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
			notifyListenerWithPending(KEY_PAGE_CANVAS_NOTIFY_LISTENER, 0, 0,
					meta);
		}

	}

}
