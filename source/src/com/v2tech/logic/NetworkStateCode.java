package com.v2tech.logic;

public enum NetworkStateCode {
	SUCCESS(0), INCORRECT_INFO(1), TIME_OUT(-1), CONNECTED_ERROR(301), UNKNOW_CODE(-3);

	private int code;

	private NetworkStateCode(int code) {
		this.code = code;
	}
	
	public int intValue() {
		return code;
	}

	public static NetworkStateCode fromInt(int code) {
		switch (code) {
		case 0:
			return SUCCESS;
		case 1:
			return INCORRECT_INFO;
		case -1:
			return TIME_OUT;
		case 301:
			return CONNECTED_ERROR;
		default:
			return UNKNOW_CODE;
		}
	}
}