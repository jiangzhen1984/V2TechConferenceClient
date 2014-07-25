package com.v2tech.vo;

import java.io.File;
import java.math.BigDecimal;
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
	
	private int fileType;
	
	public int getFileType() {
		return fileType;
	}

	public void setFileType(int fileType) {
		this.fileType = fileType;
	}


	//Always send offline file
	private int transType = 2;
	
	public VMessageFileItem(VMessage vm, String filePath) {
		super(vm);
		this.filePath = filePath;
		if (filePath != null) {
			int start = filePath.lastIndexOf("/");
			if (start != -1) {
				this.fileName = filePath.substring(start+1);
			}
			File f = new File(filePath);
			fileSize = f.length();
		}
		this.type = VMessageAbstractItem.ITEM_TYPE_FILE;
		this.uuid = UUID.randomUUID().toString();
	}
	
	public VMessageFileItem(VMessage vm, String filePath , int fileType) {
		super(vm);
		this.filePath = filePath;
		if (filePath != null) {
			int start = filePath.lastIndexOf("/");
			if (start != -1) {
				this.fileName = filePath.substring(start+1);
			}
			File f = new File(filePath);
			fileSize = f.length();
		}
		this.type = VMessageAbstractItem.ITEM_TYPE_FILE;
		this.uuid = UUID.randomUUID().toString();
		this.fileType = fileType;
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
		Format df=new DecimalFormat("#.0");
		
		if (fileSize >= 1073741824) {
			return (df.format((double) fileSize / (double) 1073741824)) + "G";
		} else if (fileSize >= 1048576) {
			return (df.format((double) fileSize / (double) 1048576)) + "M";
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
	
	public String getDownloadSizeStr() {
		Format df=new DecimalFormat("#.0");
		
		if (downloadedSize >= 1073741824) {
			return (df.format((double) downloadedSize / (double) 1073741824)) + "G";
		} else if (downloadedSize >= 1048576) {
			return (df.format((double) downloadedSize / (double) 1048576)) + "M";
		} else if (downloadedSize >= 1024) {
			return (df.format((double) downloadedSize / (double) 1024)) + "K";
		} else {
			return downloadedSize + "B";
		}
	}

	public void setDownloadedSize(long downloadedSize) {
		this.downloadedSize = downloadedSize;
	}

	public String getSpeedStr() {
		
		return getFileSize(speed);
	}
	
	public float getSpeed() {
		
		return speed;
	}
	
	/**
	 * 获取文件大小
	 * 
	 * @param totalSpace
	 * @return
	 */
	private String getFileSize(float totalSpace) {

		BigDecimal filesize = new BigDecimal(totalSpace);
		BigDecimal megabyte = new BigDecimal(1024 * 1024);
		float returnValue = filesize.divide(megabyte, 2, BigDecimal.ROUND_UP)
				.floatValue();
		if (returnValue > 1)
			return (returnValue + "MB");
		BigDecimal kilobyte = new BigDecimal(1024);
		returnValue = filesize.divide(kilobyte, 2, BigDecimal.ROUND_UP)
				.floatValue();
		return (returnValue + "  KB ");
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}


	public int getTransType() {
		return transType;
	}


	public void setTransType(int transType) {
		this.transType = transType;
	}


	
	

}
