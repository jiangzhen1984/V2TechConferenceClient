package com.bizcom.request.jni;

import com.bizcom.vo.Conference;
import com.bizcom.vo.UserDeviceConfig;

public class OpenVideoRequest extends JNIRequest {
	
	Conference conf;
	UserDeviceConfig userDevice;

	public OpenVideoRequest(Conference conf, UserDeviceConfig userDevice) {
		super();
		this.conf = conf;
		this.userDevice = userDevice;
	}

}
