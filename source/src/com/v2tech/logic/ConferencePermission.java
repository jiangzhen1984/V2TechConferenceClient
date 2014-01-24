package com.v2tech.logic;


/**
 * It's enum definition. Used to delegate permission of conference.
 * @author 28851274
 *
 */
public enum ConferencePermission {
	// 3 means speaking permission, this number is defined by server side.
	SPEAKING(3);

	private int code;
	
	private ConferencePermission(int code) {
		this.code = code;
	}
	
	public int intValue() {
		return code;
	}
}
