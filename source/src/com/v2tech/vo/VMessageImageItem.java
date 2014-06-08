package com.v2tech.vo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import com.v2tech.util.BitmapUtil;
import com.v2tech.util.V2Log;

public class VMessageImageItem extends VMessageAbstractItem {

	private String uuid;
	private String filePath;
	private String extension;
	private Bitmap mFullQualityBitmap = null;
	private Bitmap mCompressedBitmap = null;

	public VMessageImageItem(VMessage vm, String filePath) {
		super(vm);
		this.filePath = filePath;
		this.type = ITEM_TYPE_IMAGE;
	}

	public VMessageImageItem(VMessage vm, String uuid, String extension) {
		super(vm);
		this.uuid = uuid;
		this.extension = extension;
		this.type = ITEM_TYPE_IMAGE;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getUUID() {
		return uuid;
	}

	public void setUUID(String uuid) {
		this.uuid = uuid;
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

	public String toXmlItem() {
		int[] w = new int[2];
		BitmapUtil.getFullBitmapBounds(this.filePath, w);
		String str = " <TPictureChatItem NewLine=\"True\" AutoResize=\"True\" FileExt=\""
				+ getExtension()
				+ "\" GUID=\"\" Height=\""
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
			mCompressedBitmap = BitmapUtil.getCompressedBitmap(this.filePath);
		}
		return mCompressedBitmap;
	}

	public synchronized Bitmap getFullQuantityBitmap() {
		if (mFullQualityBitmap == null || mFullQualityBitmap.isRecycled()) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			options.inPreferredConfig = Config.ALPHA_8;
			options.inDither = true;
			BitmapFactory.decodeFile(this.filePath, options);
			options.inJustDecodeBounds = false;
			if (options.outWidth > 1920 || options.outHeight > 1080) {
				options.inSampleSize = 4;
				mFullQualityBitmap = BitmapFactory.decodeFile(this.filePath,
						options);
			} else if (options.outWidth > 800 || options.outHeight > 600) {
				options.inSampleSize = 2;
				mFullQualityBitmap = BitmapFactory.decodeFile(this.filePath,
						options);
			} else {
				options.inSampleSize = 1;
				mFullQualityBitmap = BitmapFactory.decodeFile(this.filePath,
						options);
			}

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