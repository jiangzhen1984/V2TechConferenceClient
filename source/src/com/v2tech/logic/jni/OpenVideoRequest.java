package com.v2tech.logic.jni;

import com.v2tech.logic.Conference;
import com.v2tech.logic.UserDeviceConfig;

public class OpenVideoRequest extends JNIRequest {
	
	Conference conf;
	UserDeviceConfig userDevice;

	public OpenVideoRequest(Conference conf, UserDeviceConfig userDevice) {
		super();
		this.conf = conf;
		this.userDevice = userDevice;
	}

}
