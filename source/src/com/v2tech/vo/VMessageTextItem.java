package com.v2tech.vo;

import java.util.UUID;

public class VMessageTextItem extends VMessageAbstractItem {

	private String text;

	public VMessageTextItem(VMessage vm, String text) {
		super(vm);
		this.text = text;
		this.type = ITEM_TYPE_TEXT;
		this.uuid = UUID.randomUUID().toString();
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String toXmlItem() {
		String str = "<TTextChatItem NewLine=\""
				+ (isNewLine ? "True" : "False")
				+ "\" FontIndex=\"0\" Text=\"" + text + "\"/>";
		return str;
	}

}

