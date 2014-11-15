package com.v2tech.view;

public class PublicIntent {

	public static final String TAG_CONTACT = "contacts";
	public static final String TAG_ORG = "org";
	public static final String TAG_GROUP = "group";
	public static final String TAG_CONF = "conference";
	public static final String TAG_COV = "conversation";

	public static final int MESSAGE_NOTIFICATION_ID = 1;
	public static final int VIDEO_NOTIFICATION_ID = 2;
	public static final int APPLICATION_STATUS_BAR_NOTIFICATION = 3;

	public static final String DEFAULT_CATEGORY = "com.v2tech";
	/**
	 * Used to start conversation UI
	 */
	public static final String START_CONVERSACTION_ACTIVITY = "com.v2tech.start_conversation_activity";

	public static final String START_P2P_CONVERSACTION_ACTIVITY = "com.v2tech.start_p2p_conversation_activity";

	public static final String START_VIDEO_IMAGE_GALLERY = "com.v2tech.image_gallery";

	/**
	 * Start conference create activity<br>
	 * key uid: pre-selected user id
	 * key gid: pre-selected group id
	 */
	public static final String START_CONFERENCE_CREATE_ACTIVITY = "com.v2tech.start_conference_create_activity";

	public static final String START_ABOUT_ACTIVITY = "com.v2tech.start_about_activity";

	public static final String START_SETTING_ACTIVITY = "com.v2tech.start_setting_activity";

	public static final String START_GROUP_CREATE_ACTIVITY = "com.v2tech.start_group_create_activity";

	public static final String START_CROWD_MEMBERS_ACTIVITY = "com.v2tech.start_crowd_members_activity";

	public static final String START_CROWD_FILES_ACTIVITY = "com.v2tech.start_crowd_files_activity";

	public static final String UPDATE_CONVERSATION = "com.v2tech.update_conversation";

	public static final String SHOW_CROWD_CONTENT_ACTIVITY = "com.v2tech.crowd_content_activity";

	public static final String SHOW_CROWD_DETAIL_ACTIVITY = "com.v2tech.crowd_detail_activity";

	public static final String SHOW_CONTACT_DETAIL_ACTIVITY = "com.v2tech.contact_detail_activity";
	
	/**
	 * key : crowd : object of crowd
	 * key : authdisable : disable crowd authentication even through crowd need authentication
	 */
	public static final String SHOW_CROWD_APPLICATION_ACTIVITY = "com.v2tech.crowd_application_activity";
	
	/**
	 * Start searching activity
	 * key : type 0 crowd   1: member
	 */
	public static final String START_SEARCH_ACTIVITY = "com.v2tech.start_search_activity";
	

	/**
	 * extras key: obj value:
	 * {@link com.v2tech.view.bo.ConversationNotificationObject}
	 */
	public static final String REQUEST_UPDATE_CONVERSATION = "com.v2tech.request_update_conversation";

	public static final String FINISH_APPLICATION = "com.v2tech.finish_application";

	public static final String PREPARE_FINISH_APPLICATION = "com.v2tech.prepare_finish_application";

	// ===========================Broadcast not for start activity
	// 
	/**
	 * broadcast for new crowd notification, if user created to user is invited<br>
	 * key : crowd : crowd id 
	 */
	public static final String BROADCAST_NEW_CROWD_NOTIFICATION = "com.v2tech.jni.broadcast.new_crowd_notification";
	
	/**
	 * Broadcast for user update the comment name
	 * key: no
	 */
	public static final String BROADCAST_USER_COMMENT_NAME_NOTIFICATION = "com.v2tech.broadcast.user_comment_name_notification";
	
	/** Broadcast for new conference. This is only for conference is created by self
	 * extra key: newGid : group id
	 * we can get conference object from GlobalHolder
	 * 
	 */
	public static final String BROADCAST_NEW_CONFERENCE_NOTIFICATION = "com.v2tech.jni.broadcast.new_conference_notification";
	// Request update contacts group
	public static final String BROADCAST_REQUEST_UPDATE_CONTACTS_GROUP = "com.v2tech.broadcast.update_contacts_group";
	// Broadcast for crowd is deleted notification
	public static final String BROADCAST_CROWD_DELETED_NOTIFICATION = "com.v2tech.broadcast.crowd_deleted_notification";
	
	/**
	 * Broadcast for contact group updated 
	 * key: userId 
	 * key: srcGroupId 
	 * key: destGroupId
	 */
	public static final String BROADCAST_CONTACT_GROUP_UPDATED_NOTIFICATION = "com.v2tech.broadcast.contact_group_notification";
	
	/**
	 * Broadcast for user quit crowd
	 * key: userId 
	 * key: groupId 
	 * key: kicked boolean false means quit by self, true crowd owner kicked 
	 */
	public static final String BROADCAST_CROWD_QUIT_NOTIFICATION = "com.v2tech.broadcast.crowd_quit_notification";
	
	
	/**
	 * Broadcast for user joined conference, to inform that quit P2P conversation
	 * key: confid conference if 
	 */
	public static final String BROADCAST_JOINED_CONFERENCE_NOTIFICATION = "com.v2tech.broadcast.joined_conference_notification";


}
