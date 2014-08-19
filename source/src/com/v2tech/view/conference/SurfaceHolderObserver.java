package com.v2tech.view.conference;

import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;

import com.v2tech.service.DeviceService;
import com.v2tech.service.Registrant;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.vo.UserDeviceConfig;

public class SurfaceHolderObserver implements SurfaceHolder.Callback {

	private final static int OPEN_DEVICE_DONE = 1;
	private final static int CLOSE_DEVICE_DONE = 2;
	
	private DeviceService service;
	private UserDeviceConfig udc;
	private State state;
	private boolean isCreate;
	
	
	
	
	
	public SurfaceHolderObserver(DeviceService service, UserDeviceConfig udc) {
		super();
		this.service = service;
		this.udc = udc;
		state = State.CLOSED;
	}

	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		isCreate = true;
		if (state == State.CLOSED) {
			state = State.SHOWING;
			udc.getVp().SetSurface(holder);
			service.requestOpenVideoDevice(udc, new Registrant(handler,OPEN_DEVICE_DONE, null));
		}
		udc.getVp().SetViewSize(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		isCreate = true;
		if (state == State.CLOSED) {
			state = State.SHOWING;
			udc.getVp().SetSurface(holder);
			service.requestOpenVideoDevice(udc, new Registrant(handler,OPEN_DEVICE_DONE, null));
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		isCreate = false;
		if (state == State.SHOWED || state == State.SHOWING) {
			state = State.CLOSING;
			service.requestCloseVideoDevice(udc, new Registrant(handler,CLOSE_DEVICE_DONE, null));
		}
	}
	
	
	public void open() {
		if (!isCreate) {
			return;
		}
		if (state == State.SHOWED || state == State.SHOWING) {
			return;
		}
		service.requestOpenVideoDevice(udc, new Registrant(handler,OPEN_DEVICE_DONE, null));
	}
	
	public void close() {
		if (state == State.CLOSED || state == State.CLOSING) {
			return;
		}
		state = State.CLOSING;
		service.requestCloseVideoDevice(udc, new Registrant(handler,CLOSE_DEVICE_DONE, null));
	}

	
	
	enum State {
		SHOWED,SHOWING,CLOSED,CLOSING
	}
	
	
	
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			JNIResponse.Result res =((JNIResponse)msg.obj).getResult();
			switch(msg.what) {
			case OPEN_DEVICE_DONE:
				if (res == JNIResponse.Result.SUCCESS) {
					state = State.SHOWED;
				} else {
					state = State.CLOSED;
				}
				break;
			case CLOSE_DEVICE_DONE:
				if (res == JNIResponse.Result.SUCCESS) {
					state = State.CLOSED;
				}
				break;
			
			}
		}
		
	};
	
}
