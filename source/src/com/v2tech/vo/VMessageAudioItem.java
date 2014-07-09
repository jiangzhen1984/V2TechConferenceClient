package com.v2tech.vo;

import java.util.UUID;

public class VMessageAudioItem extends VMessageAbstractItem {

	private String extension;
	private String audioFilePath;
	private int seconds;
	
	private boolean isPlaying;
	
	
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
	
	

	public int getSeconds() {
		return seconds;
	}
	
	


	public boolean isPlaying() {
		return isPlaying;
	}


	public void setPlaying(boolean isPlaying) {
		this.isPlaying = isPlaying;
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
