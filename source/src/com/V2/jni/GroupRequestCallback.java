package com.V2.jni;



/**
 * 
 * @author 28851274
 *
 */
public interface GroupRequestCallback {


	/**
	 *  When log in successfully, this function will be called by JNI.<br>
	 *  To indicate group information current user belongs and owns.
	 * 
	 * @param groupType  1:contact type 4: conference type
	 * @param sXml &nbsp;&nbsp;<br>
	 * type {@link com.v2tech.logic.Group.GroupType#CONTACT}(1):<br>
	 * {@code <xml><pubgroup id='61' name='ronghuo的组织'><pubgroup id='21' name='1'/></pubgroup></xml>}<br>
	 * 
	 * 
	 * type {@link com.v2tech.logic.Group.GroupType#CONFERENCE}(4):<br>
	 *  {@code <xml><conf createuserid='1124' id='513891897880' start time='1389189927'
	 * subject='est'/><conf createuserid='1124' id='513891899176'
	 * starttime='1389190062' subject='eee'/></xml> }
	 * 
	 * @see com.v2tech.logic.Group#GroupType
	 */
	public void OnGetGroupInfoCallback(int groupType, String sXml);
	
	
	
	/**
	 *  When log in successfully, this function will be call by JNI.<br>
	 *  To indicate users information who users belong group
	 * @param groupType  <ul><br>
	 * 		<li>1: contact type {@link com.v2tech.logic.Group.GroupType#CONTACT}<br></li> 
	 * 		<li>4: conference type {@link com.v2tech.logic.Group.GroupType#CONFERENCE}<br></li>
	 * 		</ul>
	 * @param nGroupID
	 * @param sXml
	 */
	public void OnGetGroupUserInfoCallback(int groupType, long nGroupID, String sXml);
}
