package com.bizcom.vo;

import com.V2.jni.util.EscapedcharactersProcessing;

public class VMessageLinkTextItem extends VMessageAbstractItem {

	private String text;
	private String url;

	public VMessageLinkTextItem(VMessage vm, String text, String url) {
		super(vm , ITEM_TYPE_LINK_TEXT);
		this.text = text;
		this.url = url;
		this.uuid = vm.getUUID();
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getUrl() {
		if(url.contains("http://"))
			return url.substring(7);
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String toXmlItem() {
		String str = "<TLinkTextChatItem NewLine=\""
				+ (isNewLine ? "True" : "False") + "\" FontIndex=\"1\" Text=\""
				+ EscapedcharactersProcessing.convert(text) + "\" " + " URL=\""
				+ EscapedcharactersProcessing.convert(url)
				+ "\" LinkType=\"lteHttp\" />";
		return str;
	}

}
