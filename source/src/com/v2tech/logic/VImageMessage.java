package com.v2tech.logic;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.v2tech.util.V2Log;

public class VImageMessage extends VMessage {

	private String mExtension;
	private String mUUID;
	private int mHeight = -1;
	private int mWidth = -1;
	private byte[] originImageData;
	private String mImagePath;

	public VImageMessage(User u, User toUser, String imagePath, boolean isRemote) {
		super(u, toUser, null, isRemote);
		if (imagePath == null) {
			throw new NullPointerException(" image path can not be null");
		}
		this.mImagePath = imagePath;
		this.mType = VMessage.MessageType.IMAGE;
		init();
	}

	public VImageMessage(User u, User toUser, byte[] data) {
		super(u, toUser, null, true);
		this.mType = VMessage.MessageType.IMAGE;
	}

	@Override
	public String getText() {
		return mUUID+"|" + mExtension +"|" +mHeight+"|"+mWidth;
	}

	private void init() {
		String uuid = UUID.randomUUID().toString();
		mUUID = "{" + uuid + "}";
		int pos = mImagePath.lastIndexOf(".");
		if (pos != -1) {
			mExtension = mImagePath.substring(pos);
		}
	}

	public byte[] getWrapperData() {
		if (originImageData == null && !loadImageData()) {
			return null;
		}
		byte[] d = new byte[52 + originImageData.length];
		System.arraycopy(mUUID.getBytes(), 0, d, 0, 41);
		byte[] et = mExtension.getBytes();
		System.arraycopy(et, 0, d, 41, et.length);
		System.arraycopy(originImageData, 0, d, 52, originImageData.length);
		return d;
	}

	public int getHeight() {
		if (mHeight <= 0) {
			loadImageData();
		}
		return mHeight;
	}

	public int getWidth() {
		if (mWidth <= 0) {
			loadImageData();
		}
		return mWidth;
	}
	
	
	private boolean loadImageData() {
		if (originImageData == null ) {
			File f = new File(mImagePath);
			if (!f.exists()) {
				V2Log.e(" file doesn't exist " + mImagePath);
				return false;
			}
			originImageData = new byte[(int)f.length()];
			InputStream is = null;
			try {
				is = new FileInputStream(f);
				is.read(originImageData);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		
		InputStream is = null;
		is = new ByteArrayInputStream(originImageData);
		Bitmap bm = BitmapFactory.decodeStream(is);
		mHeight = bm.getHeight();
		mWidth = bm.getWidth();
		bm.recycle();
		try {
			is.close();
		} catch (IOException e) {
			V2Log.e("can not close stream for bitmap");
		}
		return true;
	}

	// < TPictureChatItem NewLine="False" AutoResize="True" FileExt=".png"
	// GUID="{F3870296-746D-4E11-B69B-050B2168C624}" Height="109" Width="111"/>
	@Override
	public String toXml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
				.append("<TChatData IsAutoReply=\"False\">\n")
				.append("<FontList>\n")
				.append("<TChatFont Color=\"255\" Name=\"Segoe UI\" Size=\"18\" Style=\"\"/>")
				.append("</FontList>\n")
				.append("<ItemList>\n")
				.append("< TPictureChatItem NewLine=\"False\" AutoResize=\"True\" FileExt=\""
						+ mExtension
						+ "\" GUID=\""
						+ mUUID
						+ "\" Height=\""
						+ mHeight + "\" Width=\"" + mWidth + "\"/>")
				.append("</ItemList>").append("</TChatData>");
		return sb.toString();
	}

}
