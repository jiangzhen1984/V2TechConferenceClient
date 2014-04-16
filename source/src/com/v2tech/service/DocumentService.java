package com.v2tech.service;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Message;

import com.V2.jni.WBRequest;
import com.V2.jni.WBRequestCallback;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.logic.Group;
import com.v2tech.logic.Group.GroupType;
import com.v2tech.logic.Registrant;
import com.v2tech.logic.User;
import com.v2tech.util.XmlParser;
import com.v2tech.view.vo.V2Doc;
import com.v2tech.view.vo.V2ImageDoc;

public class DocumentService extends AbstractHandler {

	private WBRequestCallbackCB cb;

	public DocumentService() {
		cb = new WBRequestCallbackCB();
		WBRequest.getInstance().addCallbacks(cb);
	}

	private List<Registrant> newDocNotificatorHodler = new ArrayList<Registrant>();

	public void registerNewDocNotification(Handler h, int what, Object obj) {
		newDocNotificatorHodler.add(new Registrant(h, what, obj));
	}

	public void unRegisterNewDocNotification(Handler h, int what, Object obj) {
		for (Registrant r : newDocNotificatorHodler) {
			if (r.getHandler() == h && r.getWhat() == what
					&& r.getObject() == obj) {
				newDocNotificatorHodler.remove(r);
				return;
			}
		}
	}
	
	private List<Registrant> docPageNotificatorHodler = new ArrayList<Registrant>();

	public void registerDocPageNotification(Handler h, int what, Object obj) {
		docPageNotificatorHodler.add(new Registrant(h, what, obj));
	}

	public void unRegisterDocPageNotification(Handler h, int what, Object obj) {
		for (Registrant r : docPageNotificatorHodler) {
			if (r.getHandler() == h && r.getWhat() == what
					&& r.getObject() == obj) {
				docPageNotificatorHodler.remove(r);
				return;
			}
		}
	}
	
	private List<Registrant> docPageActiveNotificatorHodler = new ArrayList<Registrant>();

	public void registerdocPageActiveNotification(Handler h, int what, Object obj) {
		docPageActiveNotificatorHodler.add(new Registrant(h, what, obj));
	}

	public void unRegisterdocPageActiveNotification(Handler h, int what, Object obj) {
		for (Registrant r : docPageActiveNotificatorHodler) {
			if (r.getHandler() == h && r.getWhat() == what
					&& r.getObject() == obj) {
				docPageActiveNotificatorHodler.remove(r);
				return;
			}
		}
	}
	
	private List<Registrant> docDisplayNotificatorHodler = new ArrayList<Registrant>();

	public void registerDocDisplayNotification(Handler h, int what, Object obj) {
		docDisplayNotificatorHodler.add(new Registrant(h, what, obj));
	}

	public void unRegisterDocDisplayNotification(Handler h, int what, Object obj) {
		for (Registrant r : docDisplayNotificatorHodler) {
			if (r.getHandler() == h && r.getWhat() == what
					&& r.getObject() == obj) {
				docDisplayNotificatorHodler.remove(r);
				return;
			}
		}
	}

	class WBRequestCallbackCB implements WBRequestCallback {

		@Override
		public void OnWBoardChatInvite(long nGroupID, int nBusinessType,
				long nFromUserID, String szWBoardID, int nWhiteIndex,
				String szFileName, int type) {
			Group g = GlobalHolder.getInstance().getGroupById(
					GroupType.CONFERENCE, nGroupID);
			User u = GlobalHolder.getInstance().getUser(nFromUserID);
			int pos = szFileName.lastIndexOf("/");
			if (pos == -1) {
				pos = szFileName.lastIndexOf("\\");
				if (pos != -1) {
					szFileName = szFileName.substring(pos+1);
				}
			}
			for (Registrant r : newDocNotificatorHodler) {
				if (r.getHandler() != null) {
					Message m = Message.obtain();
					m.what =  r.getWhat();
					m.obj = new AsyncResult(r.getObject(), new V2ImageDoc(
							szWBoardID, szFileName, g, nBusinessType, u));
					r.getHandler().sendMessage(m);
				}

			}

		}

		@Override
		public void OnWBoardPageList(String szWBoardID, String szPageData,
				int nPageID) {
			for (Registrant r : docPageNotificatorHodler) {
				if (r.getHandler() != null) {
					Message m = Message.obtain();
					m.what =  r.getWhat();
					m.obj = new AsyncResult(r.getObject(), XmlParser.parserDocPage(szWBoardID, szPageData));
					r.getHandler().sendMessage(m);
				}

			}

		}

		@Override
		public void OnWBoardActivePage(long nUserID, String szWBoardID,
				int nPageID) {
			for (Registrant r : docPageActiveNotificatorHodler) {
				if (r.getHandler() != null) {
					Message m = Message.obtain();
					m.what =  r.getWhat();
					m.obj = new AsyncResult(r.getObject(), new V2Doc.Page(nPageID, szWBoardID, null));
					r.getHandler().sendMessage(m);
				}

			}

		}

		@Override
		public void OnWBoardDocDisplay(String szWBoardID, int nPageID,
				String szFileName, int result) {
			for (Registrant r : docDisplayNotificatorHodler) {
				if (r.getHandler() != null) {
					Message m = Message.obtain();
					m.what =  r.getWhat();
					m.obj = new AsyncResult(r.getObject(), new V2Doc.Page(nPageID, szWBoardID, szFileName));
					r.getHandler().sendMessage(m);
				}

			}

		}

	}

	public class AsyncResult {
		Object userObject;
		Object result;

		public AsyncResult(Object userObject, Object result) {
			super();
			this.userObject = userObject;
			this.result = result;
		}

		public Object getUserObject() {
			return userObject;
		}

		public void setUserObject(Object userObject) {
			this.userObject = userObject;
		}

		public Object getResult() {
			return result;
		}

		public void setResult(Object result) {
			this.result = result;
		}
		
		

	}
}
