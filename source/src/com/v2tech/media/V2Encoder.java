package com.v2tech.media;

/**
 * Use to define Audio or Video encode operation.<br>
 * @see AACEncoder
 * 
 * @author jiangzhen
 *
 */
public interface V2Encoder {

	/**
	 * Start to record and encode
	 */
	public void start();
	
	
	/**
	 * Stop recording.<br>
	 */
	public void stop();
	
	
	/**
	 * Release all resource. <br>
	 * If you don't want to record any more, you should call this.<br>
	 */
	public void release();
	
	/**
	 * Get current state
	 * @return
	 * @see MediaState
	 */
	public MediaState getState();
	
	/**
	 * Return current frame's db
	 * @return
	 */
	public double getDB();
	
	
	/**
	 * Set error callback
	 * @param callback
	 */
	public void setErrorCallback(ErrorCallback callback);
	
}
