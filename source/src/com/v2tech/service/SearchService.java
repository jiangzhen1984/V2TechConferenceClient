package com.v2tech.service;

import java.util.List;

import android.os.Handler;
import android.os.Message;

import com.V2.jni.GroupRequest;
import com.V2.jni.GroupRequestCallbackAdapter;
import com.V2.jni.ImRequest;
import com.V2.jni.ImRequestCallbackAdapter;
import com.V2.jni.ind.V2Group;
import com.V2.jni.ind.V2User;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.SearchedResult;
import com.v2tech.vo.User;

/**
 * Used to search
 * 
 * @author 28851274
 * 
 */
public class SearchService extends AbstractHandler {

	private static final int SEARCH = 1;

	private ImRequestCallbackCB imCB;
	private GroupRequestCB grCB;

	public SearchService() {
		super();
		imCB = new ImRequestCallbackCB(this);
		ImRequest.getInstance().addCallback(imCB);

		grCB = new GroupRequestCB(this);
		GroupRequest.getInstance().addCallback(grCB);
	}

	/**
	 * Search content from server side
	 * 
	 * @param par
	 * @param caller
	 */
	public void search(SearchParameter par, Registrant caller) {
		if (!this.checkParamNull(caller, par)) {
			return;
		}

		initTimeoutMessage(SEARCH, DEFAULT_TIME_OUT_SECS, caller);

		if (par.mType == Type.CROWD) {
			int startNo = (par.mPageNo - 1) * par.mPageSize;
			int gType = 0;
			if (par.mType == Type.CROWD) {
				gType = GroupType.CHATING.intValue();
			} else if (par.mType == Type.CONFERENCE) {
				gType = GroupType.CONFERENCE.intValue();
			}
			GroupRequest.getInstance().searchGroup(gType, par.text, startNo,
					par.mPageSize);
		} else if (par.mType == Type.MEMBER) {
			int startNo = par.mPageNo * par.mPageSize;
			ImRequest.getInstance().searchMember(par.text,
					startNo == 0 ? 1 : startNo, par.mPageSize);
		}
	}

	@Override
	public void clearCalledBack() {
		ImRequest.getInstance().removeCallback(imCB);
		GroupRequest.getInstance().removeCallback(grCB);
	}

	class ImRequestCallbackCB extends ImRequestCallbackAdapter {

		private Handler mCallbackHandler;

		public ImRequestCallbackCB(Handler mCallbackHandler) {

			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnSearchUserCallback(List<V2User> list) {
			SearchedResult result = new SearchedResult();
			for (V2User u : list) {
				result.addItem(SearchedResult.Type.USER, u.uid, u.name);
			}
			JNIResponse jniRES = new JNIResponse(JNIResponse.Result.SUCCESS);
			jniRES.resObj = result;
			Message.obtain(mCallbackHandler, SEARCH, jniRES).sendToTarget();
		}

	}

	class GroupRequestCB extends GroupRequestCallbackAdapter {
		private Handler mCallbackHandler;

		public GroupRequestCB(Handler mCallbackHandler) {

			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnSearchCrowdCallback(List<V2Group> list) {
			SearchedResult result = new SearchedResult();
			for (V2Group g : list) {
				User creator = new User(g.creator.uid, g.creator.name);
				result.addCrowdItem(g.id, g.name, creator, g.brief, g.authType);
			}
			JNIResponse jniRES = new JNIResponse(JNIResponse.Result.SUCCESS);
			jniRES.resObj = result;
			Message.obtain(mCallbackHandler, SEARCH, jniRES).sendToTarget();
		}

	}

	public SearchParameter generateSearchPatameter(Type type, String content,
			int nPage) {
		SearchParameter par = new SearchParameter();
		par.mType = type;
		par.text = content;
		par.mPageNo = nPage;
		par.mPageSize = 200;
		return par;
	}

	public class SearchParameter {
		String text;
		Type mType;
		int mPageNo;
		int mPageSize;
	}

	public enum Type {
		CONFERENCE, CROWD, MEMBER, ALL
	}
}
