package com.bizcom.vo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import com.V2.jni.util.V2Log;
import com.bizcom.util.BitmapUtil;
import com.bizcom.vc.application.GlobalConfig;

public class VMessageImageItem extends VMessageAbstractItem{

	private String filePath;
	private String extension;
	private Bitmap mFullQualityBitmap = null;
	private Bitmap mCompressedBitmap = null;
	private boolean isReceived;
	private int transState;

	public VMessageImageItem(VMessage vm, String uuid , String filePath , int placeHolder) {
		this(vm , uuid , filePath , null);
	}

	public VMessageImageItem(VMessage vm, String uuid, String extension) {
		this(vm , uuid , null , extension);
	}
	
	public VMessageImageItem(VMessage vm, String uuid, String filePath , String extension) {
		super(vm);
		if(uuid == null)
			this.uuid = UUID.randomUUID().toString();
		else
			this.uuid = uuid;
		
		if(filePath == null && extension != null)
			this.filePath = GlobalConfig.getGlobalPicsPath() + "/" + uuid + extension;
		else
			this.filePath = filePath;
		
		if(filePath != null && extension == null){
			int pos = filePath.lastIndexOf(".");
			if (pos != -1) {
				this.extension = filePath.substring(pos);
			}
		} else
			this.extension = extension;
		this.type = ITEM_TYPE_IMAGE;
	}
	
	public String getFilePath() {
		if (filePath == null && extension != null)
			return GlobalConfig.getGlobalPicsPath() + "/" + uuid + extension;
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getExtension() {
		if (extension == null) {
			int pos = filePath.lastIndexOf(".");
			if (pos != -1) {
				extension = filePath.substring(pos);
			}
		}
		return extension;
	}

	public boolean isReceived() {
		return isReceived;
	}

	public void setReceived(boolean isReceived) {
		this.isReceived = isReceived;
	}
	
	public int getTransState() {
		return transState;
	}

	public void setTransState(int transState) {
		this.transState = transState;
	}

	public String toXmlItem() {
		int[] w = new int[2];
		BitmapUtil.getFullBitmapBounds(filePath, w);
		String str = " <TPictureChatItem NewLine=\"True\" AutoResize=\"True\" FileExt=\""
				+ getExtension()
				+ "\" GUID=\""+uuid+"\" Height=\""
				+ w[1]
				+ "\" Width=\"" + w[0] + "\" />";
		return str;
	}

	public byte[] loadImageData() {
		File f = new File(filePath);
		if (!f.exists()) {
			V2Log.e(" file doesn't exist " + filePath);
			return null;
		}
		InputStream is = null;
		try {
			byte[] data = new byte[(int) f.length()];
			is = new FileInputStream(f);
			is.read(data);
			return data;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
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

	public Size getCompressedBitmapSize() {
		int[] w = new int[2];
		BitmapUtil.getCompressedBitmapBounds(this.filePath, w);
		Size s = new Size();
		s.width = w[0];
		s.height = w[1];
		return s;
	}

	public synchronized Bitmap getCompressedBitmap() {
		if (mCompressedBitmap == null || mCompressedBitmap.isRecycled()) {
			mCompressedBitmap = BitmapUtil.getCompressedBitmap(filePath);
		}
		return mCompressedBitmap;
	}
	
	public Size getFullBitmapSize() {
		int[] w = new int[2];
		BitmapUtil.getFullBitmapBounds(this.filePath, w);
		Size s = new Size();
		s.width = w[0];
		s.height = w[1];
		return s;
	}

	public synchronized Bitmap getFullQuantityBitmap() {
		if (mFullQualityBitmap == null || mFullQualityBitmap.isRecycled()) {
			
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			options.inPreferredConfig = Config.ARGB_8888;
			options.inDither = true;
			BitmapFactory.decodeFile(this.filePath, options);
			options.inJustDecodeBounds = false;
			
			int widthScale = options.outWidth / GlobalConfig.SCREEN_WIDTH;
			int heightScale = options.outHeight / GlobalConfig.SCREEN_HEIGHT;
			if(widthScale > heightScale && options.outWidth > GlobalConfig.SCREEN_WIDTH)
				options.inSampleSize = widthScale;
			else if(heightScale > widthScale && options.outHeight > GlobalConfig.SCREEN_HEIGHT)
				options.inSampleSize = heightScale;
			mFullQualityBitmap = BitmapFactory.decodeFile(this.filePath,
						options);
		}
		return mFullQualityBitmap;
	}

	public void recycle() {
		if (mCompressedBitmap != null) {
			mCompressedBitmap.recycle();
			mCompressedBitmap = null;
		}
	}

	public void recycleFull() {
		if (mFullQualityBitmap != null) {
			mFullQualityBitmap.recycle();
			mFullQualityBitmap = null;
		}
	}

	public void recycleAll() {
		recycle();
		recycleFull();
	}

	public class Size {
		public int width;
		public int height;
	}
}