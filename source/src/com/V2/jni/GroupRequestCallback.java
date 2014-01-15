package com.V2.jni;


/**
 * 
 * @author 28851274
 *
 */
public interface GroupRequestCallback {


	/**
	 * <xml><conf createuserid='1124' id='513891897880' start time='1389189927'
	 * subject='est'/><conf createuserid='1124' id='513891899176'
	 * starttime='1389190062' subject='eee'/></xml>
	 * 
	 * @param xml
	 */
	public void OnGetGroupInfoCallback(int groupType, String sXml);
}
