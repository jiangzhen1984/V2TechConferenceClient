package com.V2.jni;

import java.util.List;

import com.V2.jni.ind.V2Group;


/**
 * 
 * @author 28851274
 * 
 */
public interface GroupRequestCallback {

	/**
	 * When log in successfully, this function will be called by JNI.<br>
	 * To indicate group information current user belongs and owns.
	 * 
	 * @param groupType
	 *            1:org  2: contacts group 3: crowd type 4: conference type
	 * @param list group list
	 * 
	 * @see com.v2tech.vo.Group#GroupType
	 */
	public void OnGetGroupInfoCallback(int groupType, List<V2Group> list);

	/**
	 * When log in successfully, this function will be call by JNI.<br>
	 * To indicate users information who users belong group
	 * <ul>
	 * XML:<br>
	 * {@code <xml><user accounttype='1' address='沈阳' birthday='1981-10-27' email='' fax='02422523280' id='174' mobile='13998298300' needauth='0' nickname='朱  江' privacy='0' sex='1' sign='13998298300' telephone='02422523280'/></xml>}
	 * 
	 * </ul>
	 * 
	 * @param groupType
	 *            <ul>
	 *            <br>
	 *            <li>1: contact type
	 *            {@link com.v2tech.vo.Group.GroupType#CONTACT}<br>
	 *            </li>
	 *            <li>4: conference type
	 *            {@link com.v2tech.vo.Group.GroupType#CONFERENCE}<br>
	 *            </li>
	 *            </ul>
	 * @param nGroupID
	 * @param sXml
	 */
	public void OnGetGroupUserInfoCallback(int groupType, long nGroupID,
			String sXml);

	/**
	 * <ul>
	 * Group information update callback
	 * </ul>
	 * <ul>
	 *  
	 *   
	 * </ul>
	 * <ul>
	 * TODO: as now only support conference create call back
	 * </ul>
	 * 
	 * @param groupType
	 *            4: conference
	 * @param nGroupID
	 *            new conference ID
	 * @param sXml   <ul>
	 *   If current modified group type is conference, sXml content is {@code <conf syncdesktop="1/0"></conf>},<br>
	 *   1: chairman is synchronizing desktop, user can't change doc content by self.
	 *   0: chairman released desktop control
	 *   </ul>
	 */
	public void OnModifyGroupInfoCallback(int groupType, long nGroupID,
			String sXml);
	
	/**
	 * <ul>Invitation call back.</ul>
	 * <ul>If other users invite this user to join conference, then this call back will be called. </ul>
	 * @param groupType  4 means conference invitation.
	 * @param groupInfo if it's conference invitation, {@code <conf createuserid='1138' id='513956640327' starttime='2012' subject=' 啊'/>}
	 * @param userInfo if it's conference invitation {@code <user id='1138' uetype='2'/>}
	 * @param additInfo
	 */
	public void OnInviteJoinGroupCallback(int groupType, String groupInfo,
			String userInfo, String additInfo);
	
	
	/**
	 * 
	 * @param groupType
	 * @param nGroupID
	 * @param bMovetoRoot
	 */
	public void OnDelGroupCallback(int groupType, long nGroupID, boolean bMovetoRoot);
	
	
	/**
	 * TODO add comment
	 * @param groupType
	 * @param nGroupID
	 * @param nUserID
	 */
	public void OnDelGroupUserCallback(int groupType, long nGroupID, long nUserID);
	
	
	/**
	 * TODO add comment
	 * @param groupType
	 * @param nGroupID
	 * @param sXml
	 */
	public void OnAddGroupUserInfoCallback(int groupType, long nGroupID, String sXml);
	
	
	/**
	 * new group created call back
	 * @param group
	 */
	public void onAddGroupInfo(V2Group group);
}
