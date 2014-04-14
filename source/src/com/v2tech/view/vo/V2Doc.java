package com.v2tech.view.vo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.v2tech.logic.Group;
import com.v2tech.logic.User;

public class V2Doc {

	public static final int DOC_TYPE_IMAGE = 1;

	protected String id;
	protected Group mGroup;
	protected int mBType;
	protected User mSharedUser;
	protected int mDocType;
	protected List<Page> pages;
	
	protected int currentPageNo = 1;	
	
	
	
	public static class PageArray {
		String docId;
		Page[] pr;

		public PageArray(Page[] pr) {
			this.pr = pr;
		}
		
		

		public PageArray() {
		}



		public String getDocId() {
			return docId;
		}

		public void setDocId(String docId) {
			this.docId = docId;
		}

		public Page[] getPr() {
			return pr;
		}

		public void setPr(Page[] pr) {
			this.pr = pr;
		}
		
		
		
	}
	
	
	
	public static class Page {
		int no;
		String docId;
		String filePath;
		
		public Page(int no, String docId, String filePath) {
			this.no = no;
			this.docId = docId;
			this.filePath = filePath;
		}

		public int getNo() {
			return no;
		}

		public void setNo(int no) {
			this.no = no;
		}

		public String getDocId() {
			return docId;
		}

		public void setDocId(String docId) {
			this.docId = docId;
		}

		public String getFilePath() {
			return filePath;
		}

		public void setFilePath(String filePath) {
			this.filePath = filePath;
		}
		
		
		
	}
	
	

	public V2Doc(String id, Group mGroup, int mBType, User mSharedUser) {
		super();
		this.id = id;
		this.mGroup = mGroup;
		this.mBType = mBType;
		this.mSharedUser = mSharedUser;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Group getGroup() {
		return mGroup;
	}

	public void setGroup(Group mGroup) {
		this.mGroup = mGroup;
	}

	public int getBType() {
		return mBType;
	}

	public void setBType(int mBType) {
		this.mBType = mBType;
	}

	public User getSharedUser() {
		return mSharedUser;
	}

	public void setSharedUser(User mSharedUser) {
		this.mSharedUser = mSharedUser;
	}

	
	public int getDocType() {
		return mDocType;
	}

	public void setDocType(int mDocType) {
		this.mDocType = mDocType;
	}

	
	public void addPage(Page p) {
		if (p == null) {
			throw new NullPointerException(" page is null");
		}
		if (this.pages == null) {
			this.pages = new CopyOnWriteArrayList<Page>();
		}
		this.pages.add(p);
	}
	
	public Page findPage(int no) {
		if (pages == null) {
			return null;
		}
		for (Page p : this.pages) {
			if (p.no == no) {
				return p;
			}
		}
		return null;
	}
	
	public int getActivatePageNo() {
		return this.currentPageNo;
	}
	
	public Page getActivatePage() {
		if (pages == null) {
			return null;
		}
		for (Page p : pages) {
			if (p.no == this.currentPageNo) {
				return p;
			}
		}
		return null;
	}
	
	public void setActivatePageNo(int no) {
		this.currentPageNo = no;
	}
	
	
	public int getPageSize() {
		return pages == null? 0 : pages.size();
	}

}