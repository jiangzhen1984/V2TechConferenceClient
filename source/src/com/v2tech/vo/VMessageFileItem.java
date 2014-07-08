package com.v2tech.vo;

import java.io.File;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.UUID;

public class VMessageFileItem extends VMessageAbstractItem {

	private String filePath;

	private String fileName;

	private long fileSize;
	
	private float progress;
	
	private long downloadedSize;
	
	private float speed;
	
	public VMessageFileItem(VMessage vm, String filePath) {
		super(vm);
		this.filePath = filePath;
		if (filePath != null) {
			int start = filePath.lastIndexOf("/");
			if (start != -1) {
				this.fileName = filePath.substring(start);
			}
			File f = new File(filePath);
			fileSize = f.length();
		}
		this.type = VMessageAbstractItem.ITEM_TYPE_FILE;
		this.uuid = UUID.randomUUID().toString();
	}
	
	
	public VMessageFileItem(VMessage vm, String uuid, String fileName) {
		super(vm);
		this.fileName = fileName;
		this.uuid = uuid;
		this.type = VMessageAbstractItem.ITEM_TYPE_FILE;
	}

	public long getFileSize() {
		return fileSize;
	}
	
	public void setFileSize(long size) {
		this.fileSize = size;
	}

	public String getFileSizeStr() {
		Format df=new DecimalFormat("#.00");
		
		if (fileSize >= 1073741824) {
			return (df.format((double) fileSize / (double) 1073741824)) + "G";
		} else if (fileSize >= 1048576) {
			return (df.format((double) fileSize / (double) 1073741824)) + "M";
		} else if (fileSize >= 1024) {
			return (df.format((double) fileSize / (double) 1024)) + "K";
		} else {
			return fileSize + "B";
		}
	}

	public String getFileName() {
		return fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	

	@Override
	public String toXmlItem() {
		return "";
	}

	public float getProgress() {
		return progress;
	}

	public void setProgress(float progress) {
		this.progress = progress;
	}

	public long getDownloadedSize() {
		return downloadedSize;
	}

	public void setDownloadedSize(long downloadedSize) {
		this.downloadedSize = downloadedSize;
	}

	public float getSpeed() {
		return speed;
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}
	
	

}
