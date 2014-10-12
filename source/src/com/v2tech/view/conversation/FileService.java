package com.v2tech.view.conversation;

import java.util.HashMap;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.V2.jni.FileRequest;
import com.V2.jni.FileRequestCallbackAdapter;
import com.v2tech.vo.VFile;

/**
 * Use to upload file and update uploading status to database
 * <ul>
 * intent key:  uuid  : file's uuid</br>
 * intent key:  filePath  : file's local path</br>
 * intent key:  gid  : if upload file to group, input group id</br>
 * </ul>
 * @author jiangzhen
 *
 */
public class FileService extends Service {

	private static final int UPDATE_FILE_STATE = 1;
	private static final int START_UPLOAD_FILE = 2;
	private Map<String, VFile> mMoniterMap = new HashMap<String, VFile>();

	private FileRequestCB frCB;
	private HandlerThread backThread;
	private Handler mLocalHandler;

	@Override
	public void onCreate() {
		super.onCreate();
		frCB = new FileRequestCB();
		FileRequest.getInstance().addCallback(frCB);
		backThread = new HandlerThread("FileUpdateService");
		backThread.start();
		mLocalHandler = new LocalHandler(backThread.getLooper());

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		FileRequest.getInstance().removeCallback(frCB);
		backThread.quit();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String fileId = intent.getStringExtra("uuid");
		String filePath = intent.getStringExtra("filePath");
		long gid = intent.getLongExtra("gid", 0);

		Message.obtain(mLocalHandler, START_UPLOAD_FILE,
				new LocalFileObject(fileId, filePath, gid)).sendToTarget();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	// TODO update file state to database
	private void updateFile(String uuid, FileState st) {
		mMoniterMap.remove(uuid);
		//Update database
		if (mMoniterMap.isEmpty()) {
			this.stopSelf();
		}
	}

	class FileRequestCB extends FileRequestCallbackAdapter {

		@Override
		public void OnFileTransEnd(String szFileID, String szFileName,
				long nFileSize, int nTransType) {
			if (mMoniterMap.containsKey(szFileID)) {
				Message.obtain(mLocalHandler, UPDATE_FILE_STATE,
						new TransObject(szFileID, FileState.DONE))
						.sendToTarget();
			}
		}

		@Override
		public void OnFileTransError(String szFileID, int errorCode,
				int nTransType) {
			if (mMoniterMap.containsKey(szFileID)) {
				Message.obtain(mLocalHandler, UPDATE_FILE_STATE,
						new TransObject(szFileID, FileState.ERROR))
						.sendToTarget();
			}
		}

		@Override
		public void OnFileTransCancel(String szFileID) {
			if (mMoniterMap.containsKey(szFileID)) {
				Message.obtain(mLocalHandler, UPDATE_FILE_STATE,
						new TransObject(szFileID, FileState.CANCEL))
						.sendToTarget();
			}
		}

	}

	enum FileState {
		DONE, ERROR, CANCEL;
	}

	class TransObject {
		String uuid;
		FileState st;

		public TransObject(String uuid, FileState st) {
			super();
			this.uuid = uuid;
			this.st = st;
		}

	}

	class LocalFileObject {
		String uuid;
		String filePath;
		long gid;

		public LocalFileObject(String uuid, String filePath, long gid) {
			super();
			this.uuid = uuid;
			this.filePath = filePath;
			this.gid = gid;
		}

	}

	class LocalHandler extends Handler {

		public LocalHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_FILE_STATE:
				TransObject to = (TransObject) msg.obj;
				updateFile(to.uuid, to.st);
				break;
			case START_UPLOAD_FILE:
				LocalFileObject lfo = (LocalFileObject) msg.obj;
				// TODO query file object from database
				// TODO put to map
				// mMoniterMap.put(lfo.uuid, file);

				// TODO upload P2P or upload group file
				break;
			}
		}

	}

}
