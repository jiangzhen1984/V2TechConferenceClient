package com.v2tech.media;

/**
 * Define decoder
 * @author 28851274
 *
 */
public interface V2Decoder {
	
	
	/**
	 * Start to decode and flush raw data to hardware
	 */
	public void play();
	
	
	/**
	 * Stop decoding
	 */
	public void stop();
	
	
	/**
	 * Seek to specific position
	 * @param sec
	 */
	public void seek(int sec);
	
	
	/**
	 * pause current playing audio resource
	 */
	public void pause();
	
	
	/**
	 * Release all resource.<br>
	 * You can't decode any more if you call this.
	 */
	public void release();
	
	
	/**
	 * get current audio duration
	 * @return second of duration
	 */
	public int getDuration();
	
	
	
	/**
	 * Set error callback
	 * @param callback
	 */
	public void setErrorCallback(ErrorCallback callback);

}
