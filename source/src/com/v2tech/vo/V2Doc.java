package com.v2tech.vo;

import java.util.ArrayList;
import java.util.List;

import android.util.SparseArray;

public class V2Doc {

	public static final int DOC_TYPE_IMAGE = 1;
	public static final int DOC_TYPE_BLANK_BOARD = 2;

	protected String id;
	protected Group mGroup;
	protected int mBType;
	protected User mSharedUser;
	protected int mDocType;
	protected Doc doc;
	protected String mDocName;

	protected int currentPageNo = 1;

	public static class Doc {
		String docId;
		SparseArray<Page> pages;

		public Doc(Page[] pr) {
			pages = new SparseArray<Page>();
			addPages(pr);
		}

		public Doc(SparseArray<Page> page) {
			this.pages = page;
		}

		public Doc() {
			pages = new SparseArray<Page>();
		}

		public String getDocId() {
			return docId;
		}

		public void setDocId(String docId) {
			this.docId = docId;
		}

		public void addPages(Page[] pr) {
			for (int i = 0; pr != null && i < pr.length; i++) {
				pages.put(i, pr[i]);
			}
		}

		/**
		 * Update cache page
		 * @param p
		 */
		public void addPage(Page p) {
			Page existP = pages.get(p.getNo());
			if (existP != null) {
				existP.update(p);
			} else {
				pages.put(p.getNo(), p);
				
			}
		}
		
		public void updatePage(Page p) {
			pages.put(p.getNo(), p);
		}

		public Page getPage(int no) {
			return pages.get(no);
		}
		
		
		public int getPageSize() {
			return this.pages.size();
		}
		
		public Page getPageByIndex(int index){
			return this.pages.valueAt(index);
		}
		
		
		public void update(Doc doc) {
			
			if (doc == null) {
				return;
			}
			
			for (int i = 0; i < doc.getPageSize(); i++) {
				Page newP = doc.getPageByIndex(i);
				Page oldP = getPage(newP.no);
				if (oldP == null) {
					updatePage(newP);
				} else {
					oldP.update(newP);
				}
			}
			
		}

	}

	public static class Page {
		int no;
		String docId;
		String filePath;
		List<V2ShapeMeta> vsMeta;
		
		protected Page() {
			
		}

		public Page(int no, String docId, String filePath) {
			this.no = no;
			this.docId = docId;
			this.filePath = filePath;
			vsMeta = new ArrayList<V2ShapeMeta>();
		}

		public Page(int no, String docId, String filePath,
				List<V2ShapeMeta> vsMeta) {
			this.no = no;
			this.docId = docId;
			this.filePath = filePath;
			this.vsMeta = vsMeta;
			if (this.vsMeta == null) {
				this.vsMeta = new ArrayList<V2ShapeMeta>();
			}
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

		public List<V2ShapeMeta> getVsMeta() {
			return vsMeta;
		}

		public void setVsMeta(List<V2ShapeMeta> vsMeta) {
			this.vsMeta = vsMeta;
		}

		public void addMeta(V2ShapeMeta meta) {
			this.vsMeta.add(meta);
		}
		
		
		public void update(Page p) {
			if (p == null) {
				return;
			}
			
			if (p.no > 0) {
				this.no = p.no;
			}
			if (p.docId != null) {
				this.docId = p.docId;
			}
			if (p.filePath != null) {
				this.filePath = p.filePath;
			}
			if (this.vsMeta == null) {
				this.vsMeta = new ArrayList<V2ShapeMeta>();
			}
			this.vsMeta.addAll(p.vsMeta);
			
		}

	}
	
	
	public static class BlankBorad extends Page {
		public BlankBorad(int no, String docId,
				List<V2ShapeMeta> vsMeta) {
			
			this.no = no;
			this.docId = docId;
			this.vsMeta = vsMeta;
			if (this.vsMeta == null) {
				this.vsMeta = new ArrayList<V2ShapeMeta>();
			}
		}
	}

	public V2Doc(String id, String docName, Group mGroup, int mBType,
			User mSharedUser) {
		super();
		this.id = id;
		this.mDocName = docName;
		this.mGroup = mGroup;
		this.mBType = mBType;
		this.mSharedUser = mSharedUser;
		this.doc = new Doc();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDocName() {
		return mDocName;
	}

	public void setDocName(String docName) {
		this.mDocName = docName;
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
		doc.addPage(p);
	}

	public Page findPage(int no) {
		return doc.getPage(no);
	}

	public int getActivatePageNo() {
		return this.currentPageNo;
	}

	public Page getActivatePage() {
		return doc.getPage(currentPageNo);
	}

	public void setActivatePageNo(int no) {
		this.currentPageNo = no;
	}

	public int getPageSize() {
		return doc.getPageSize();
	}

	/**
	 * Get page according page number.
	 * @param no from 1 start
	 * @return
	 */
	public Page getPage(int no) {
		if (no <= 0 || no > doc.getPageSize()) {
//			throw new IndexOutOfBoundsException("Page no is incorrect ");
			return null;
		}
		return doc.getPage(no);
	}
	/**
	 * Update existed page array. Will ignore if parameter is null
	 * @param doc
	 */
	public void updateDoc(Doc doc) {
		if (doc == null) {
			return;
		}
		this.doc.update(doc);
	}
	
	
	
	/**
	 *  Update current doc. Will ignore if parameter is null
	 * @param v2doc
	 */
	public void updateV2Doc(V2Doc v2doc) {
		if (v2doc.getDocName() != null) {
			this.mDocName = v2doc.getDocName();
		}
		if (v2doc.getGroup() != null) {
			this.mGroup = v2doc.getGroup();
		}
		if (v2doc.getSharedUser() != null) {
			this.mSharedUser = v2doc.getSharedUser();
		}
		this.mBType = v2doc.getBType();
		this.doc.update(v2doc.doc);
	}
}
