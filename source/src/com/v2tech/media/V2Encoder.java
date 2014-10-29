package com.v2tech.media;

/**
 * Use to define Audio or Video encode operation.
 * @author jiangzhen
 *
 */
public interface V2Encoder {

	/**
	 * Start to record and encode
	 */
	public void start();
	
	
	/**
	 * Stop recording.
	 */
	public void stop();
	
	
	/**
	 * Get current state
	 * @return
	 * @see V2EncoderState
	 */
	public V2EncoderState getState();
	
	/**
	 * Return current frame's db
	 * @return
	 */
	public double getDB();
	
	
	public enum V2EncoderState {
		NORMAL, INITIALIZED, RECORDING, STOPPED, OUTPUT_ERROR, ERROR
	}
}
