package com.bizcom.vo;

public class VMessageFaceItem extends VMessageAbstractItem {

	private int index;

	public VMessageFaceItem(VMessage vm, int index) {
		super(vm , ITEM_TYPE_FACE);
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String toXmlItem() {
		String str = "<TSysFaceChatItem NewLine=\""
				+ (isNewLine ? "True" : "False") + "\" FileName=\"" + index
				+ ".png\" ShortCut=\"\" />";
		return str;
	}

}
