package com.v2tech.media;


/**
 * State of encoding or decoding audio resource
 * @author 28851274
 *
 */
public enum MediaState {

	/**
	 * Default state
	 */
	NORMAL,

	/**
	 * All native resource initialized
	 * 
	 */
	INITIALIZED,

	/**
	 * Recording audio
	 */
	RECORDING,

	/**
	 * Decoding audio resource
	 */
	DECODING,

	/**
	 * STOPED RECORDING or Decoding audio resource
	 */
	STOPPED,
	
	/**
	 * Pause playing, this state only in decode
	 */
	PAUSE,

	/**
	 * All native resource released
	 */
	RELEASED,

	/**
	 * flush recorded audio resource error
	 */
	OUTPUT_ERROR,
	
	/**
	 * decode audio resource error
	 */
	DECODE_ERROR,

	/**
	 * Other errors
	 */
	ERROR
}
