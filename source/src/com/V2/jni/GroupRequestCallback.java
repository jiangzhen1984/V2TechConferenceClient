package com.V2.jni;

import java.util.List;

import com.V2.jni.ind.FileJNIObject;
import com.V2.jni.ind.V2Group;
import com.V2.jni.ind.V2User;

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
	 *            1:org 2: contacts group 3: crowd type 4: conference type
	 * @param list
	 *            group list
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
	 *            {@link com.v2tech.vo.Group.GroupType#FRIGROUP}<br>
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
	 * @param sXml
	 *            <ul>
	 *            If current modified group type is conference, sXml content is
	 *            {@code <conf syncdesktop="1/0"></conf>},<br>
	 *            1: chairman is synchronizing desktop, user can't change doc
	 *            content by self. 0: chairman released desktop control
	 *            </ul>
	 */
	public void OnModifyGroupInfoCallback(int groupType, long nGroupID,
			String sXml);
	

	/**
	 * Invite user join conference or crowd. 
	 * @param group 
	 */
	public void OnInviteJoinGroupCallback(V2Group group);
	
	
	/**
	 * Add contact relation request.
	 * @param user 
	 * @param additInfo 
	 */
	public void OnRequestCreateRelationCallback(V2User user,
			String additInfo);

	/**
	 * Callback of delete group
	 * @param groupType
	 * @param nGroupID
	 * @param bMovetoRoot
	 */
	public void OnDelGroupCallback(int groupType, long nGroupID,
			boolean bMovetoRoot);

	/**
	 * TODO add comment
	 * 
	 * @param groupType
	 * @param nGroupID
	 * @param nUserID
	 */
	public void OnDelGroupUserCallback(int groupType, long nGroupID,
			long nUserID);

	/**
	 * TODO add comment
	 * @deprecated should use object instead of xml
	 * @param groupType
	 * @param nGroupID
	 * @param sXml
	 */
	public void OnAddGroupUserInfoCallback(int groupType, long nGroupID,
			String sXml);

	/**
	 * new group created call back
	 * 
	 * @param group
	 */
	public void onAddGroupInfo(V2Group group);

	/**
	 * update contact group callback
	 * 
	 * @param groupType
	 * @param srcGroup
	 * @param desGroup
	 * @param u
	 */
	public void OnMoveUserToGroup(int groupType, V2Group srcGroup,
			V2Group desGroup, V2User u);

	/**
	 * Callback of accept join crowd invitation
	 * 
	 * @param groupType
	 * @param groupId
	 * @param nUserID
	 */
	public void OnAcceptInviteJoinGroup(int groupType, long groupId,
			long nUserID);
	
	
	/**
	 * Callback of accept apply join crowd invitation
	 * 
	 * @param group
	 */
	public void OnAcceptApplyJoinGroup(V2Group group);

	
	/**
	 * @deprecated should handle xml
	 * @param groupType
	 * @param nGroupID
	 * @param nUserID
	 * @param sxml
	 */
	public void OnRefuseInviteJoinGroup(int groupType, long nGroupID, long nUserID,
			String sxml);
	
	/**
	 * call back for get group file list
	 * @param group
	 * @param list
	 * 
	 * @see GroupRequest#getGroupFileInfo(int, long)
	 */
	public void OnGetGroupFileInfo(V2Group group, List<FileJNIObject> list);
	
	
	
	/**
	 * call back for removed group file list
	 * @param group
	 * @param list
	 * 
	 * @see GroupRequest#delGroupFile(int, long, String)
	 */
	public void OnDelGroupFile(V2Group group, List<FileJNIObject> list);

}
