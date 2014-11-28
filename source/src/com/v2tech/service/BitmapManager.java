package com.v2tech.service;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.V2.jni.util.V2Log;
import com.v2tech.util.BitmapUtil;
import com.v2tech.view.bo.UserAvatarObject;
import com.v2tech.vo.User;

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

	private static BitmapManager mInstance;

	private List<WeakReference<BitmapChangedListener>> mListeners;

	private List<WeakReference<BitmapChangedListener>> mLastListeners;

	private BitmapManager() {
		mListeners = new ArrayList<WeakReference<BitmapChangedListener>>();
		mLastListeners = new ArrayList<WeakReference<BitmapChangedListener>>();
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
