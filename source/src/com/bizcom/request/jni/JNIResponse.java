package com.bizcom.request.jni;

/**
 * JNI call back data wrapper
 * 
 * @author 28851274
 * 
 */
public class JNIResponse {

	public enum Result {
		SUCCESS(0), FAILED(1), NO_RESOURCE(2), LOGED_OVER_TIME(3), CONNECT_ERROR(
				301), SERVER_REJECT(300), UNKNOWN(-1), TIME_OUT(-2), INCORRECT_PAR(
				-3), ERR_CONF_NO_EXIST(200), ERR_CONF_LOCKDOG_NORESOURCE(205);

		private int val;

		private Result(int i) {
			this.val = i;
		}

		public int value() {
			return val;
		}

		public static Result fromInt(int code) {
			switch (code) {
			case 0:
				return SUCCESS;
			case 1:
				return FAILED;
			case 2:
				return NO_RESOURCE;
			case 3:
				return LOGED_OVER_TIME;
			case 301:
				return CONNECT_ERROR;
			case 300:
				return SERVER_REJECT;
			case 200:
				return ERR_CONF_NO_EXIST;
			case 205:
				return ERR_CONF_LOCKDOG_NORESOURCE;
			case -2:
				return TIME_OUT;
			case -3:
				return INCORRECT_PAR;
			default:
				return UNKNOWN;
			}
		}

	}

	protected Result res;

	public Object callerObject;

	public Object resObj;

	public JNIResponse(Result res) {
		super();
		this.res = res;
	}

	public Result getResult() {
		return res;
	}
}
