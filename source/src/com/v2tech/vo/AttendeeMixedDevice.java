package com.v2tech.vo;

import java.util.Random;

public class AttendeeMixedDevice extends Attendee {

	private MixVideo mv;
	private UserDeviceConfig[] udcs;
	private long id;

	public AttendeeMixedDevice(MixVideo mv) {
		super();
		this.mv = mv;
		if (this.mv != null) {
			MixVideo.MixVideoDevice[] uds = mv.getUdcs();
			udcs = new UserDeviceConfig[uds.length];
			for (int i = 0; i < uds.length; i++) {
				udcs[0] = new UserDeviceConfig(
						0,
						mv.getId(),
						null,
						UserDeviceConfig.UserDeviceConfigType.EVIDEODEVTYPE_VIDEOMIXER);
				udcs[0].setBelongsAttendee(this);
			}
		}

		Random r = new Random();
		id = r.nextLong() | r.hashCode();
	}

	public MixVideo getMV() {
		return this.mv;
	}

	@Override
	public long getAttId() {
		return id;
	}

	@Override
	public UserDeviceConfig getDefaultDevice() {
		if (udcs != null && udcs.length > 0) {
			return udcs[0];
		}
		return null;
	}

	@Override
	public String getAttName() {
		return "混合视频  (" + mv.getWidth() + "  x " + mv.getHeight() + ")";
	}

}
