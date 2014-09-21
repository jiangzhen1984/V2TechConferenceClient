package com.v2tech.vo;

import java.text.DecimalFormat;
import java.text.Format;

public class VFile {

	protected String id;
	protected String path;
	protected long size;
	protected State state;
	protected long proceedSize;
	protected String name;
	protected User uploader;

	public enum State {
		DOWNLOADED, DOWNLOADING, DOWNLOAD_PAUSE, UPLOADED, UPLOADING, UPLOAD_PAUSE
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public long getProceedSize() {
		return proceedSize;
	}

	public void setProceedSize(long proceedSize) {
		this.proceedSize = proceedSize;
		if (this.proceedSize == size) {
			if (this.state == State.DOWNLOADING) {
				this.state = State.DOWNLOADED;
			} else if (this.state == State.UPLOADING) {
				this.state = State.UPLOADED;
			}
		}
			
	}
	
	public String getFileSizeStr() {
		Format df = new DecimalFormat("#.0");

		if (size >= 1073741824) {
			return (df.format((double) size / (double) 1073741824)) + "G";
		} else if (size >= 1048576) {
			return (df.format((double) size / (double) 1048576)) + "M";
		} else if (size >= 1024) {
			return (df.format((double) size / (double) 1024)) + "K";
		} else {
			return size + "B";
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public User getUploader() {
		return uploader;
	}

	public void setUploader(User uploader) {
		this.uploader = uploader;
	}
	
	

}
