package com.bizcom.vo;

import java.util.UUID;

import com.V2.jni.util.EscapedcharactersProcessing;


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
				+ "\" FontIndex=\"0\" Text=\"" + EscapedcharactersProcessing.convert(text) + "\"/>";
		return str;
	}

}

