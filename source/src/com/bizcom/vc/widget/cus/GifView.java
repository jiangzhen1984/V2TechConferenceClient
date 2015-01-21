package com.bizcom.vc.widget.cus;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.V2.jni.util.V2Log;

public class GifView extends View implements Runnable {
	private static final String TAG = "GifView";
	private static final int Time = 150; //间隔
	private GIFFrameManager mGIFFrameManager = null;
	private int frames;
	private String filePath;
	private int mScreenWidth;
	private int mScreenHeight;
	private ExecutorService service;
	
	public GifView(Context context) {
		super(context);
		init(context);
	}

	public GifView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public GifView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	private void init(Context context){
		mGIFFrameManager = new GIFFrameManager();
		WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		mScreenWidth = wm.getDefaultDisplay().getWidth();
		mScreenHeight = wm.getDefaultDisplay().getHeight();
		service = Executors.newCachedThreadPool();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int index = mGIFFrameManager.nextFrame();
		Log.e(TAG, "current frame index is :" + index);
		if(index == 0 & frames != 0)
			restartGif();
		else{
			Bitmap bitmap = mGIFFrameManager.getImage();
			if (bitmap != null) {
				if(!bitmap.isRecycled()){
					canvas.drawBitmap(bitmap, (mScreenWidth - bitmap.getWidth()) / 2 , 
							(mScreenHeight - bitmap.getHeight()) / 2, null);
					bitmap.recycle();
					bitmap = null;
				}
			}
		}
	}

	private void restartGif() {
		frames = 0;
		mGIFFrameManager.clearBitmap();
		setGIFResource(filePath);
	}

	public byte[] fileConnect(InputStream in) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int ch = -1;
		byte[] buf = new byte[1024];
		try {
			while ((ch = in.read(buf)) != -1) {
				out.write(buf , 0 , ch);
			}
			byte[] b = out.toByteArray();
			out.close();
			out = null;
			in.close();
			in = null;
			return b;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void run() {
		while (!Thread.interrupted()) {
			try {
				Thread.sleep(Time);
				postInvalidate();
			} catch (Exception ex) {
				ex.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
	}

//	public void setGIFResource(final byte[] bytes) {
//		if (bytes == null || bytes.length <= 0) {
//			return;
//		}
//
//		new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				mGIFFrameManager.CreateGifImage(bytes);
//			}
//		}).start();
//		new Thread(this).start();
//	}

//	public void setGIFResource(InputStream is) {
//		if (is == null) {
//			return;
//		}
//
//		mGIFFrameManager.CreateGifImage(fileConnect(is));
//		new Thread(this).start();
//	}
//
//	public void setGIFResource(Bitmap bitmap) {
//		if (bitmap == null) {
//			return;
//		}
//		ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		bitmap.compress(CompressFormat.PNG, 0 /* ignored for PNG */, bos);
//		byte[] bitmapdata = bos.toByteArray();
//		mGIFFrameManager.CreateGifImage(bitmapdata);
//		new Thread(this).start();
//	}

	public void setGIFResource(String filePath) {
		if (TextUtils.isEmpty(filePath)) {
			return;
		}

		final File file = new File(filePath);
		if (file.isDirectory() || !file.exists())
			return;

		this.filePath = filePath;
		service.execute(new Runnable() {
			@Override
			public void run() {
				FileInputStream is = null;
				try {
					is = new FileInputStream(file);
					byte[] fileConnect = fileConnect(is);
					frames = mGIFFrameManager.CreateGifImage(fileConnect);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} finally {
					if (is != null)
						try {
							is.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
				}
			}
		});
		service.execute(this);
	}
}

class GIFFrameManager {

	private Vector<Bitmap> frames;
	private  int index;
	private static GIFEncoder gifdecoder;

	public GIFFrameManager() {
		frames = new Vector<Bitmap>(1);
		index = 0;
	}

	public void addImage(Bitmap image) {
		frames.addElement(image);
	}

	public int size() {
		return frames.size();
	}

	public Bitmap getImage() {
		if (size() == 0) {
			return null;
		} else {
			return (Bitmap) frames.elementAt(index);
		}
	}
	
	public void clearBitmap(){
		frames.clear();
	}

	public int nextFrame() {
		V2Log.e("GifView", "size : " + size());
		if (index + 1 < size()) {
			index++;
		} else {
			index = 0;
		}
		return index;
	}

	public int CreateGifImage(byte bytes[]) {
		try {
			gifdecoder = new GIFEncoder(bytes);
			Bitmap image = null;
			for (; gifdecoder.moreFrames(); gifdecoder.nextFrame()) {
				try {
					image = gifdecoder.decodeImage();
					if (image != null) {
						addImage(image);
					}
					continue;
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
			gifdecoder.clear();
			gifdecoder = null;
			return frames.size();
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
}

class GIFEncoder {
	private int E0;
	private int E1[];
	private int E2;
	private int E6;
	private boolean E7;
	private int E8[];
	private int width;
	private int height;
	private int ED;
	private boolean EE;
	private boolean EF;
	private int F0[];
	private int F1;
	private boolean F2;
	private int F3;
	private long F4;
	private int F5;
	private static final int F6[] = { 8, 8, 4, 2 };
	private static final int F8[] = { 0, 4, 2, 1 };
	int curFrame;
	int poolsize;
	int FA;
	byte C2[];
	int FB;
	int FC;
	int FD;

	public GIFEncoder(byte abyte0[]) {
		E0 = -1;
		E1 = new int[280];
		E2 = -1;
		E6 = 0;
		E7 = false;
		E8 = null;
		width = 0;
		height = 0;
		ED = 0;
		EE = false;
		EF = false;
		F0 = null;
		F1 = 0;
		F5 = 0;
		curFrame = 0;
		C2 = abyte0;
		poolsize = C2.length;
		FA = 0;
	}

	public boolean moreFrames() {
		return poolsize - FA >= 16;
	}

	public void nextFrame() {
		curFrame++;
	}

	public Bitmap decodeImage() {
		return decodeImage(curFrame);
	}

	public Bitmap decodeImage(int i) {
		if (i <= E0) {
			return null;
		}
		if (E0 < 0) {
			if (!E3()) {
				return null;
			}
			if (!E4()) {
				return null;
			}
		}
		do {
			if (!E9(1)) {
				return null;
			}
			int j = E1[0];
			if (j == 59) {
				return null;
			}
			if (j == 33) {
				if (!E7()) {
					return null;
				}
			} else if (j == 44) {
				if (!E5()) {
					return null;
				}
				Bitmap image = createImage();
				E0++;
				if (E0 < i) {
					image = null;
				} else {
					return image;
				}
			}
		} while (true);
	}

	public void clear() {
		C2 = null;
		E1 = null;
		E8 = null;
		F0 = null;
	}

	private Bitmap createImage() {
		int i = width;
		int j = height;
		int j1 = 0;
		int k1 = 0;
		int ai[] = new int[4096];
		int ai1[] = new int[4096];
		int ai2[] = new int[8192];
		if (!E9(1)) {
			return null;
		}
		int k = E1[0];
		int[] image = new int[width * height];
		int ai3[] = E8;
		if (EE) {
			ai3 = F0;
		}
		if (E2 >= 0) {
			ai3[E2] = 0xffffff;
		}
		int l2 = 1 << k;
		int j3 = l2 + 1;
		int k2 = k + 1;
		int l3 = l2 + 2;
		int k3 = -1;
		int j4 = -1;
		for (int l1 = 0; l1 < l2; l1++) {
			ai1[l1] = l1;
		}
		int j2 = 0;
		E2();
		j1 = 0;
		label0: for (int i2 = 0; i2 < j; i2++) {
			int i1 = 0;
			do {
				if (i1 >= i) {
					break;
				}
				if (j2 == 0) {
					int i4 = E1(k2);
					if (i4 < 0) {
						return Bitmap.createBitmap(image, width, height,
								Config.RGB_565);
					}
					if (i4 > l3 || i4 == j3) {
						return Bitmap.createBitmap(image, width, height,
								Config.RGB_565);
					}
					if (i4 == l2) {
						k2 = k + 1;
						l3 = l2 + 2;
						k3 = -1;
						continue;
					}
					if (k3 == -1) {
						ai2[j2++] = ai1[i4];
						k3 = i4;
						j4 = i4;
						continue;
					}
					int i3 = i4;
					if (i4 == l3) {
						ai2[j2++] = j4;
						i4 = k3;
					}
					for (; i4 > l2; i4 = ai[i4]) {
						ai2[j2++] = ai1[i4];
					}
					j4 = ai1[i4];
					if (l3 >= 4096) {
						return Bitmap.createBitmap(image, width, height,
								Config.RGB_565);
					}
					ai2[j2++] = j4;
					ai[l3] = k3;
					ai1[l3] = j4;
					if (++l3 >= 1 << k2 && l3 < 4096) {
						k2++;
					}
					k3 = i3;
				}
				int l = ai2[--j2];
				if (l < 0) {
					return Bitmap.createBitmap(image, width, height,
							Config.RGB_565);
				}
				if (i1 == 0) {
					FC = 0;
					FB = ai3[l];
					FD = 0;
				} else if (FB != ai3[l]) {
					for (int mm = FD; mm <= FD + FC; mm++) {
						image[j1 * width + mm] = FB;
					}
					FC = 0;
					FB = ai3[l];
					FD = i1;
					if (i1 == i - 1) {
						image[j1 * width + i1] = ai3[l];
					}
				} else {
					FC++;
					if (i1 == i - 1) {
						for (int mm = FD; mm <= FD + FC; mm++) {
							image[j1 * width + mm] = FB;
						}
					}
				}
				i1++;
			} while (true);
			if (EF) {
				j1 += F6[k1];
				do {
					if (j1 < j) {
						continue label0;
					}
					if (++k1 > 3) {
						return Bitmap.createBitmap(image, width, height,
								Config.RGB_565);
					}
					j1 = F8[k1];
				} while (true);
			}
			j1++;
		}
		return Bitmap.createBitmap(image, width, height, Config.RGB_565);
	}

	private int E1(int i) {
		while (F5 < i) {
			if (F2) {
				return -1;
			}
			if (F1 == 0) {
				F1 = E8();
				F3 = 0;
				if (F1 <= 0) {
					F2 = true;
					break;
				}
			}
			F4 += E1[F3] << F5;
			F3++;
			F5 += 8;
			F1--;
		}
		int j = (int) F4 & (1 << i) - 1;
		F4 >>= i;
		F5 -= i;
		return j;
	}

	private void E2() {
		F5 = 0;
		F1 = 0;
		F4 = 0L;
		F2 = false;
		F3 = -1;
	}

	private boolean E3() {
		if (!E9(6)) {
			return false;
		}
		return E1[0] == 71 && E1[1] == 73 && E1[2] == 70 && E1[3] == 56
				&& (E1[4] == 55 || E1[4] == 57) && E1[5] == 97;
	}

	private boolean E4() {
		if (!E9(7)) {
			return false;
		}
		int i = E1[4];
		E6 = 2 << (i & 7);
		E7 = EB(i, 128);
		E8 = null;
		return !E7 || E6(E6, true);
	}

	private boolean E5() {
		if (!E9(9)) {
			return false;
		}
		width = EA(E1[4], E1[5]);
		height = EA(E1[6], E1[7]);
		int i = E1[8];
		EE = EB(i, 128);
		ED = 2 << (i & 7);
		EF = EB(i, 64);
		F0 = null;
		return !EE || E6(ED, false);
	}

	private boolean E6(int i, boolean flag) {
		int ai[] = new int[i];
		for (int j = 0; j < i; j++) {
			if (!E9(3)) {
				return false;
			}
			ai[j] = E1[0] << 16 | E1[1] << 8 | E1[2] | 0xff000000;
		}
		if (flag) {
			E8 = ai;
		} else {
			F0 = ai;
		}
		return true;
	}

	private boolean E7() {
		if (!E9(1)) {
			return false;
		}
		int i = E1[0];
		int j = -1;
		switch (i) {
		case 249:
			j = E8();
			if (j < 0) {
				return true;
			}
			if ((E1[0] & 1) != 0) {
				E2 = E1[3];
			} else {
				E2 = -1;
			}
			break;
		}
		do {
			j = E8();
		} while (j > 0);
		return true;
	}

	private int E8() {
		if (!E9(1)) {
			return -1;
		}
		int i = E1[0];
		if (i != 0 && !E9(i)) {
			return -1;
		} else {
			return i;
		}
	}

	private boolean E9(int i) {
		if (FA + i >= poolsize) {
			return false;
		}
		for (int j = 0; j < i; j++) {
			int k = C2[FA + j];
			if (k < 0) {
				k += 256;
			}
			E1[j] = k;
		}
		FA += i;
		return true;
	}

	private static final int EA(int i, int j) {
		return j << 8 | i;
	}

	private static final boolean EB(int i, int j) {
		return (i & j) == j;
	}
}