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

	public static final String START_CONFERENCE_CREATE_ACTIVITY = "com.v2tech.start_conference_create_activity";

	public static final String START_ABOUT_ACTIVITY = "com.v2tech.start_about_activity";

	public static final String START_SETTING_ACTIVITY = "com.v2tech.start_setting_activity";

	public static final String START_GROUP_CREATE_ACTIVITY = "com.v2tech.start_group_create_activity";

	public static final String UPDATE_CONVERSATION = "com.v2tech.update_conversation";

	/**
	 * extras key: obj  value: {@link com.v2tech.view.bo.ConversationNotificationObject}
	 */
	public static final String REQUEST_UPDATE_CONVERSATION = "com.v2tech.request_update_conversation";

	public static final String FINISH_APPLICATION = "com.v2tech.finish_application";

	public static final String PREPARE_FINISH_APPLICATION = "com.v2tech.prepare_finish_application";
	
	
	//===========================Broadcast not for start activity
	//broadcast for new crowd notification, if user created to user is invited
	public static final String BROADCAST_NEW_CROWD_NOTIFICATION = "com.v2tech.jni.broadcast.new_crowd_notification";
	//Broadcaset for new conference. This is only use conference is created by self
	// extra key: newGid : group id
	// we can get conference object from GlobalHolder
	public static final String BROADCAST_NEW_CONFERENCE_NOTIFICATION = "com.v2tech.jni.broadcast.new_conference_notification";
	//Request update contacts group
	public static final String BROADCAST_REQUEST_UPDATE_CONTACTS_GROUP = "com.v2tech.broadcast.update_contacts_group";
	

}
