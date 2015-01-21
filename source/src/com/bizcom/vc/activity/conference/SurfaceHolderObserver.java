package com.bizcom.vc.activity.conference;

import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;

import com.bizcom.request.DeviceService;
import com.bizcom.request.MessageListener;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.vo.Group;
import com.bizcom.vo.UserDeviceConfig;

public class SurfaceHolderObserver implements SurfaceHolder.Callback {

	private final static int OPEN_DEVICE_DONE = 1;
	private final static int CLOSE_DEVICE_DONE = 2;

	private DeviceService service;
	private UserDeviceConfig udc;
	//FIXME this observer should not care this 
	private Group g; 
	private State state;
	private boolean isCreate;
	private boolean isValid;

	public SurfaceHolderObserver(Group g, DeviceService service, UserDeviceConfig udc) {
		super();
		this.g = g;
		this.service = service;
		this.udc = udc;
		state = State.CLOSED;
		isValid = true;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		isCreate = true;
		udc.getVp().SetViewSize(width, height);
		synchronized (state) {
			if (state == State.CLOSED || state == State.CLOSING) {
				state = State.SHOWING;
				udc.getVp().SetSurface(holder);
				updateDeviceState(false);
				service.requestOpenVideoDevice(g, udc, new MessageListener(handler,
						OPEN_DEVICE_DONE, null));
			}
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		isCreate = true;
		synchronized (state) {
			if (state == State.CLOSED || state == State.CLOSING) {
				state = State.SHOWING;
				udc.getVp().SetSurface(holder);
				updateDeviceState(false);
				service.requestOpenVideoDevice(g, udc, new MessageListener(handler,
						OPEN_DEVICE_DONE, null));
			}
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		isCreate = false;
		synchronized (state) {
			if ((state == State.SHOWED || state == State.SHOWING) && !isValid) {
				state = State.CLOSING;
				updateDeviceState(true);
				service.requestCloseVideoDevice(g, udc, new MessageListener(handler,
						CLOSE_DEVICE_DONE, null));
			}
		}
	}

	public void open() {
		if (!isCreate) {
			return;
		}
		if (state == State.SHOWED || state == State.SHOWING) {
			return;
		}
		synchronized (state) {
			state = State.SHOWING;
			updateDeviceState(false);
			service.requestOpenVideoDevice(g, udc, new MessageListener(handler,
					OPEN_DEVICE_DONE, null));
		}
	}

	public void close() {
		if (state == State.CLOSED || state == State.CLOSING) {
			return;
		}
		synchronized (state) {
			state = State.CLOSING;
			updateDeviceState(true);
			service.requestCloseVideoDevice(g, udc, new MessageListener(handler,
					CLOSE_DEVICE_DONE, null));
		}
	}
	
	
	private void updateDeviceState(boolean suspend) {
		if (udc.getVp()  != null) {
			udc.getVp().setSuspended(suspend);
		}
	}
	

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}



	enum State {
		SHOWED, SHOWING, CLOSED, CLOSING
	}

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			JNIResponse.Result res = ((JNIResponse) msg.obj).getResult();
			switch (msg.what) {
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
