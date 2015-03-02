package com.bizcom.request.util;


/**
 * Class defined file transport operation.
 * @author 28851274
 *
 */
public enum FileOperationEnum {
	

	/**
	 * Pause sending file to others
	 */
	OPERATION_PAUSE_SENDING,

	/**
	 * Resume send file to others
	 */
	OPERATION_RESUME_SEND,

	/**
	 * Pause downloading file
	 */
	OPERATION_PAUSE_DOWNLOADING,

	/**
	 * Resume download file
	 */
	OPERATION_RESUME_DOWNLOAD,

	/**
	 * Cancel sending file
	 */
	OPERATION_CANCEL_SENDING,

	/**
	 * Cancel download file
	 */
	OPERATION_CANCEL_DOWNLOADING,
	
	/**
	 *  Start to download
	 */
	OPERATION_START_DOWNLOAD,
	
	/**
	 * Start to send
	 */
	OPERATION_START_SEND,
}
