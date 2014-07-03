package com.v2tech.vo;


public abstract class VMessageAbstractItem {

	
	public static final int ITEM_TYPE_ALL = 0;
	
	public static final int ITEM_TYPE_TEXT = 1;

	public static final int ITEM_TYPE_IMAGE = 2;

	public static final int ITEM_TYPE_FACE = 3;

	public static final int ITEM_TYPE_AUDIO = 4;

	public static final int ITEM_TYPE_VIDEO = 5;
	
	
	public static final int NEW_LINE_FLAG_VALUE = 1;
	
	
	public static final int STATE_NORMAL = 0;
	
	public static final int STATE_UNREAD = 1;
	
	public static final int STATE_SENT_FALIED = 2;

	protected VMessage vm;

	protected boolean isNewLine;

	protected int type;
	
	protected int state;
	
	protected String uuid;

	public VMessageAbstractItem(VMessage vm) {
		super();
		this.vm = vm;
		this.vm.addItem(this);
	}

	public VMessage getVm() {
		return vm;
	}

	public void setVm(VMessage vm) {
		this.vm = vm;
	}

	public boolean isNewLine() {
		return isNewLine;
	}

	public void setNewLine(boolean isNewLine) {
		this.isNewLine = isNewLine;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
	
	

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public abstract String toXmlItem();

	

}
