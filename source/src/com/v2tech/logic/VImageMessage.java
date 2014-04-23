package com.v2tech.logic;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import com.v2tech.util.StorageUtil;
import com.v2tech.util.V2Log;

public class VImageMessage extends VMessage {

	private String mExtension;
	private int mHeight = -1;
	private int mWidth = -1;
	private byte[] originImageData;
	private String mImagePath;
	
	

	protected VImageMessage() {
		super();
	}

	public VImageMessage(User u, User toUser, String imagePath, boolean isRemote) {
		super(u, toUser, null, isRemote);
		if (imagePath == null) {
			throw new NullPointerException(" image path can not be null");
		}
		this.mImagePath = imagePath;
		this.mType = VMessage.MessageType.IMAGE;
		init();
	}

	/**
	 * @deprecated
	 * @param u
	 * @param toUser
	 * @param data
	 */
	public VImageMessage(User u, User toUser, byte[] data) {
		super(u, toUser, null, true);
		this.mType = VMessage.MessageType.IMAGE;
		this.originImageData = data;
		if (this.originImageData != null) {
			if (this.originImageData.length < 52) {
				throw new RuntimeException("Illegal image data");
			}
			byte[] uuidBytes = new byte[38];
			System.arraycopy(originImageData, 0, uuidBytes, 0, 38);
			mUUID = new String(uuidBytes);
			byte[] extensionBytes = new byte[4];
			System.arraycopy(originImageData, 41, extensionBytes, 0, 4);
			mExtension = new String(extensionBytes);
			mImagePath = StorageUtil.getAbsoluteSdcardPath() + "/v2tech/pics/"
					+ mUUID + mExtension;
			File f = new File(mImagePath);
			OutputStream os = null;
			try {
				os = new FileOutputStream(f);
				os.write(data, 52, data.length-52);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	
	
	
	

	public VImageMessage(User u, User toUser, String uuid, String ext) {
		super(u, toUser, null, true);
		this.mType = VMessage.MessageType.IMAGE;
		this.mUUID = uuid;
		this.mExtension = ext;
		mImagePath = StorageUtil.getAbsoluteSdcardPath() + "/v2tech/pics/"
				+ uuid + ext;
	}
	
	
	public void updateImageData(byte[] b) {
		this.originImageData = b;
		saveFile();
	}

	
	
	private void saveFile() {
		File f = new File(mImagePath);
		OutputStream os = null;
		try {
			os = new FileOutputStream(f);
			os.write(originImageData, 0, originImageData.length);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	@Override
	public String getText() {
		return mUUID + "|" + mExtension + "|" + mHeight + "|" + mWidth + "|"
				+ mImagePath;
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
		return originImageData;
		
//		byte[] d = new byte[52 + originImageData.length];
//		byte[] uud = mUUID.getBytes();
//		System.arraycopy(uud, 0, d, 0, uud.length);
//		byte[] et = mExtension.getBytes();
//		System.arraycopy(et, 0, d, 41, et.length);
//		System.arraycopy(originImageData, 0, d, 52, originImageData.length);
//		return d;
	}

	public int getHeight() {
		if (mHeight <= 0) {
			loadBounds();
		}
		return mHeight;
	}

	public int getWidth() {
		if (mWidth <= 0) {
			loadBounds();
		}
		return mWidth;
	}

	public String getImagePath() {
		return this.mImagePath;
	}
	
	
	private void loadBounds() {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mImagePath, opts);
		mHeight = opts.outHeight;
		mWidth = opts.outWidth;
	}

	//FIXME optimze code
	private boolean loadImageData() {
		if (originImageData == null) {
			File f = new File(mImagePath);
			if (!f.exists()) {
				V2Log.e(" file doesn't exist " + mImagePath);
				return false;
			}
			originImageData = new byte[(int) f.length()];
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

//		InputStream is = null;
//		is = new ByteArrayInputStream(originImageData);
//		Bitmap bm = BitmapFactory.decodeStream(is);
//		mHeight = bm.getHeight();
//		mWidth = bm.getWidth();
//		bm.recycle();
//		try {
//			is.close();
//		} catch (IOException e) {
//			V2Log.e("can not close stream for bitmap");
//		}
		return true;
	}
	
	
	public static List<VMessage> extraMetaFrom(User fromUser, User toUser, String xml) {
		List<VMessage> li = new ArrayList<VMessage>();
		InputStream is = null;

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
			try {
				dBuilder = dbFactory.newDocumentBuilder();
				is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
				Document doc = dBuilder.parse(is);

				doc.getDocumentElement().normalize();
				
				NodeList imgMsgItemNL = doc.getElementsByTagName("TPictureChatItem");
				for (int i=0;i<imgMsgItemNL.getLength(); i++) {
					Element msgEl = (Element)imgMsgItemNL.item(i);
					String uuid = msgEl.getAttribute("GUID");
					if (uuid == null) {
						V2Log.e("Invalid uuid ");
						continue;
					}
					VMessage vmImage = new VImageMessage(fromUser, toUser, uuid.substring(1, uuid.length() - 1), msgEl.getAttribute("FileExt"));
					vmImage.setDate(new Date());
					vmImage.setType(MessageType.IMAGE);
					li.add(vmImage);
				}
				
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return li;
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
				.append("<TPictureChatItem NewLine=\"True\" AutoResize=\"True\" FileExt=\""
						+ mExtension
						+ "\" GUID=\""
						+ mUUID
						+ "\" Height=\""
						+ getHeight() + "\" Width=\"" + getWidth() + "\"/>")
				.append("</ItemList>").append("</TChatData>");
		return sb.toString();
	}
	
	
	Bitmap mFullQualityBitmap = null;
	Bitmap mCompressedBitmap = null;
	public Bitmap getFullQuantityBitmap() {
		if (mFullQualityBitmap == null || mFullQualityBitmap.isRecycled()) {
			 BitmapFactory.Options options= new  BitmapFactory.Options();
			 options.inJustDecodeBounds = true;
			 options.inPreferredConfig = Config.ALPHA_8;
			 options.inDither = true;
			BitmapFactory.decodeFile(this.mImagePath, options);
			if (options.outWidth > 1920 || options.outHeight > 1080) {
				options.inJustDecodeBounds = false;
				options.inSampleSize = 2;
			} else if (options.outWidth > 800 || options.outHeight > 600){
				options.inJustDecodeBounds = false;
				options.inSampleSize = 1;
			}else {
				options.inJustDecodeBounds = false;
				options.inSampleSize = 1;
			}
			mFullQualityBitmap = BitmapFactory.decodeFile(this.mImagePath, options);
		}
		return mFullQualityBitmap;
	}
	
	public Bitmap getCompressedBitmap() {
		if (mCompressedBitmap == null || mFullQualityBitmap.isRecycled()) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(this.mImagePath, options);
			if (options.outHeight > 1080 || options.outHeight > 1080) {
				options.inSampleSize = 4;
			} else if (options.outHeight > 500 || options.outHeight > 500) {
				options.inSampleSize = 2;
			} else {
				options.inSampleSize = 1;
			}
			options.inJustDecodeBounds = false;
			mCompressedBitmap = BitmapFactory.decodeFile(this.mImagePath, options);
		}
		return mCompressedBitmap;
	}
	
	public void recycle() {
		if (mFullQualityBitmap != null) {
			mFullQualityBitmap.recycle();
			mFullQualityBitmap = null;
		}
		if (mCompressedBitmap != null) {
			mCompressedBitmap.recycle();
			mCompressedBitmap = null;
		}
	}
}
