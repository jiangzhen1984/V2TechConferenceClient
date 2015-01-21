package com.bizcom.request;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import com.V2.jni.util.V2Log;
import com.bizcom.bo.UserAvatarObject;
import com.bizcom.util.BitmapUtil;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vo.User;

/**
 * <ul>
 * This is simple Bitmap Management class.
 * </ul>
 * <ul>
 * This class doesn't manage memory, this just manage bitmap changed indication.
 * <br>
 * Because once one of bitmap changed, view must change before recycle,
 * otherwise will be get exception error.
 * </ul>
 * New bitmap change event coming, bitmap manager will receive event, and will
 * notify all listeners which register with
 * {@link #registerBitmapChangedListener(BitmapChangedListener)}. <br>
 * After notify all view listeners, then notify cache listener which register
 * with {@link #registerLastBitmapChangedListener(BitmapChangedListener)}.
 * 
 * <ul>
 * </ul>
 * 
 * @author 28851274
 * 
 */
public class BitmapManager {

	public static final int BITMAP_UPDATE = 0x0001;
	
	private static BitmapManager mInstance;

	private List<WeakReference<BitmapChangedListener>> mListeners;

	private List<WeakReference<BitmapChangedListener>> mLastListeners;

	private LruCache<String, Bitmap> bitmapLru;
	
	private ExecutorService service;
	
	private BitmapManager() {
		mListeners = new ArrayList<WeakReference<BitmapChangedListener>>();
		mLastListeners = new ArrayList<WeakReference<BitmapChangedListener>>();
		service = Executors.newCachedThreadPool();
		// 设置LRU集合的大小
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int mCacheSize = maxMemory / 8;
		bitmapLru = new BitmapLRU(mCacheSize);
	}

	public synchronized static BitmapManager getInstance() {
		if (mInstance == null) {
			mInstance = new BitmapManager();
		}

		return mInstance;
	}

	/**
	 * It's used to register listener which monitor bitmaps changed event
	 * 
	 * @param listener
	 * @see {@link BitmapChangedListener}
	 */
	public void registerBitmapChangedListener(BitmapChangedListener listener) {
		synchronized (mListeners) {
			mListeners.add(new WeakReference<BitmapChangedListener>(listener));
		}
	}

	public void unRegisterBitmapChangedListener(BitmapChangedListener listener) {
		synchronized (mListeners) {
			for (int i = 0; i < mListeners.size(); i++) {
				WeakReference<BitmapChangedListener> ref = mListeners.get(i);
				if (ref.get() != null && ref.get() == listener) {
					mListeners.remove(ref);
					i--;
				}
			}
		}
	}

	/**
	 * It's used to recycle our cache bitmap.<br>
	 * <ul>
	 * This listener will call after all normal listeners
	 * {@link #registerBitmapChangedListener(BitmapChangedListener)}
	 * </ul>
	 * 
	 * @param listener
	 */
	public void registerLastBitmapChangedListener(BitmapChangedListener listener) {
		synchronized (mLastListeners) {
			mLastListeners.add(new WeakReference<BitmapChangedListener>(
					listener));
		}
	}

	public void unRegisterLastBitmapChangedListener(
			BitmapChangedListener listener) {
		synchronized (mLastListeners) {
			for (int i = 0; i < mLastListeners.size(); i++) {
				WeakReference<BitmapChangedListener> ref = mLastListeners
						.get(i);
				if (ref.get() != null && ref.get() == listener) {
					mLastListeners.remove(ref);
					i--;
				}
			}
		}
	}

	/**
	 * Asynchronized load avatar and notify all listener avatar changed
	 * 
	 * @param avatar
	 */
	public void loadUserAvatarAndNotify(UserAvatarObject avatar) {
		if (avatar == null) {
			V2Log.e("BitmapManager loadUserAvatarAndNotify --> unformaled parameter avatar , UserAvatarObject is null");
			return;
		}
		// TODO handle concurrency
		new AsyncAvatarLoader().execute(avatar);
	}

	class AsyncAvatarLoader extends
			AsyncTask<UserAvatarObject, UserAvatarObject, Void> {

		@Override
		protected Void doInBackground(UserAvatarObject... objs) {
			for (UserAvatarObject uao : objs) {
				String avatarPath = uao.getAvatarPath();
				Bitmap avatar = BitmapUtil.loadAvatarFromPath(avatarPath);
				if(avatar == null)
					V2Log.e("BitmapManager AvatarLoader --> Loading user avatar failed ... Because get Bitmap is null"
							+ " , file path is : " + avatarPath);
				uao.setBm(avatar);
				publishProgress(uao);
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(UserAvatarObject... objs) {
			for (UserAvatarObject uao : objs) {
				User u = GlobalHolder.getInstance().getUser(uao.getUId());
				if (u == null) {
					u = new User(uao.getUId());
				}
				notifiyAvatarChanged(u, uao.getBm());
			}
		}
	}

	private void notifiyAvatarChanged(User u, Bitmap bm) {
		synchronized (mListeners) {
			for (WeakReference<BitmapChangedListener> ref : mListeners) {
				Object obj = ref.get();
				if (obj != null) {
					((BitmapChangedListener) obj).notifyAvatarChanged(u, bm);
				}
			}
		}

		synchronized (mLastListeners) {
			for (WeakReference<BitmapChangedListener> ref : mLastListeners) {
				Object obj = ref.get();
				if (obj != null) {
					((BitmapChangedListener) obj).notifyAvatarChanged(u, bm);
				}
			}
		}
	}
	
	public void loadBitmapFromPath(final LoadBitmapCallBack callback){
		service.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Bitmap bitmap = bitmapLru.get(callback.filePath);
					if(bitmap == null || bitmap.isRecycled()){
						bitmap = BitmapUtil.loadAvatarFromPath(callback.filePath);
						
						if(bitmap == null)
							return ;
								
						if(bitmap.isRecycled()){
							bitmap.recycle();
							bitmap = null;
							return ;
						}
						
						bitmapLru.put(callback.filePath, bitmap);
						callback.bitmapCallBack(bitmap);
					} else {
						callback.bitmapCallBack(bitmap);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	class BitmapLRU extends LruCache<String, Bitmap> {

		public BitmapLRU(int maxSize) {
			super(maxSize);
		}

		@Override
		protected void entryRemoved(boolean evicted, String key,
				Bitmap oldValue, Bitmap newValue) {
			super.entryRemoved(evicted, key, oldValue, newValue);
			oldValue.recycle();
			oldValue = null;
		}
	}
	
	public abstract class LoadBitmapCallBack{
		
		public String filePath;
		public LoadBitmapCallBack(String filePath){
			this.filePath = filePath;
		}
		
		public abstract void bitmapCallBack(Bitmap bitmap);
	}

	/**
	 * Bitmap changed event listener
	 * 
	 * @author 28851274
	 * 
	 */
	public interface BitmapChangedListener {
		public void notifyAvatarChanged(User user, Bitmap bm);
	}
}
