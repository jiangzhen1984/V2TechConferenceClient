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
	
	
	public V2EncoderState getState();
	
	
	
	public enum V2EncoderState {
		NORMAL, INITIALIZED, RECORDING, STOPPED, OUTPUT_ERROR, ERROR
	}
}
