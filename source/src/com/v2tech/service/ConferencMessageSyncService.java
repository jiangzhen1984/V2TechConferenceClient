package com.v2tech.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;


/**
 * <ul> This android service is used for save all callback messages.</ul>
 * <ul>To use android service is that when start video activity, video activity
 * sometimes can't receive some callback messages.<br>
 *  Because when we enter conference successfully, video activity doesn't start yet and doesn't register conference service listeners.<br>
 *  <p>So start this service before start enter conference. Video activity will stop this service when finish</p>
 *  <p>Notice: this service only can start once.</p>
 *  </ul>
 * @author 28851274
 *
 */
public class ConferencMessageSyncService extends Service {

	
	private LocalBinder localBinder = new LocalBinder();
	
	private ConferenceService conferenceService = null;
	
	private DocumentService docService = null;
	
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		 conferenceService = new ConferenceService(true);
		 docService = new DocumentService();
	}






	@Override
	public void onDestroy() {
		super.onDestroy();
		docService.clearCalledBack();
		conferenceService.clearCalledBack();
		conferenceService = null;
		docService = null;
	}






	@Override
	public IBinder onBind(Intent intent) {
		return localBinder;
	}
	
	
	
	
	

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}






	public ConferenceService getConferenceService() {
		return conferenceService;
	}




	public DocumentService getDocService() {
		return docService;
	}




	public class LocalBinder extends Binder {
		public ConferencMessageSyncService getService() {
			return ConferencMessageSyncService.this;
		}
	}
}
