package com.v2tech.logic;

/**
 * Use to convey asynchronous result
 * 
 * @author 28851274
 * 
 */
public class AsynResult {

	/**
	 * This state just indicate result that communicate with JNI. <br>
	 * Notice this state does'nt mean you can get correct result. <br>
	 * e.g.:<br>
	 *     call login function  if you input incorrect user name or password, it still return SUCCESS<br>
	 *     unless you call login time out or other errors,  you will get FAIL or TIME_OUT.
	 * @author 28851274
	 *
	 */
	public enum AsynState {

		SUCCESS(0),
		FAIL(-1),
		TIME_OUT(-2),
		INCORRECT_PAR(-3);

		private int code;

		private AsynState(int i) {
			code = i;
		}
		
		public int intValue() {
			return code;
		}
	}

	private AsynState mState;

	private Object mObject;

	private Exception e;

	public AsynResult(AsynState mState, Object mObject) {
		super();
		this.mState = mState;
		this.mObject = mObject;
	}

	public AsynResult(AsynState mState, Exception e) {
		super();
		this.mState = mState;
		this.e = e;
	}

	public Exception getE() {
		return e;
	}

	public AsynState getState() {
		return mState;
	}

	public Object getObject() {
		return mObject;
	}

}
