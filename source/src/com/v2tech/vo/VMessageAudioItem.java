package com.v2tech.vo;

import java.util.UUID;

public class VMessageAudioItem extends VMessageAbstractItem {

	private String uuid;
	private String extension;
	private String audioFilePath;
	private int seconds;
	
	
	public VMessageAudioItem(VMessage vm, String uuid, String extension,
			String audioFilePath, int seconds) {
		super(vm);
		this.uuid = uuid;
		this.extension = extension;
		this.audioFilePath = audioFilePath;
		this.seconds = seconds;
		this.type = ITEM_TYPE_AUDIO;
	}


	public VMessageAudioItem(VMessage vm, String audioFilePath, int seconds) {
		super(vm);
		this.type = ITEM_TYPE_AUDIO;
		this.audioFilePath = audioFilePath;
		this.seconds = seconds;
		this.uuid = UUID.randomUUID().toString();
		if (this.audioFilePath != null && !this.audioFilePath.isEmpty()) {
			int start = this.audioFilePath.lastIndexOf(".");
			if (start != -1) {
				this.extension = this.audioFilePath.substring(start);
			}
		}
	
	}

	public String getAudioFilePath() {
		return audioFilePath;
	}

	public void setAudioFilePath(String audioFilePath) {
		this.audioFilePath = audioFilePath;
	}
	
	

	public String getUUID() {
		return uuid;
	}
	
	

	public int getSeconds() {
		return seconds;
	}


	/**
	 */
	public String toXmlItem() {
		String xml = "<TAudioChatItem NewLine=\"True\" FileExt=\"" + extension
				+ "\" FileID=\"" + uuid + "\" RecvUserID=\""
				+ vm.getToUser().getmUserId() + "\" Seconds=\"" + seconds
				+ "\" SendUserID=\"" + vm.getFromUser().getmUserId() + "\"/>";
		return xml;
	}

}
