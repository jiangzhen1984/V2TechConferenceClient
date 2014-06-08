package com.v2tech.vo;

public class VMessageFaceItem extends VMessageAbstractItem {

	private int index;

	public VMessageFaceItem(VMessage vm, int index) {
		super(vm);
		this.type = ITEM_TYPE_FACE;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String toXmlItem() {
		String str = "<TSysFaceChatItem NewLine=\""
				+ (isNewLine ? "TRUE" : "FALSE") + " FileName=\"" + index
				+ ".png ShortCut=\"\" />";
		return str;
	}

}
