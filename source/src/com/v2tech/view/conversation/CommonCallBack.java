package com.v2tech.view.conversation;

/**
 * 回调基类
 * 
 * @author
 * 
 */
public class CommonCallBack {

	private CommonCallBack() {
	};

	private static CommonCallBack callback = new CommonCallBack();

	public static CommonCallBack getInstance() {
		return callback;
	}

	private CommonUpdateConversationToCreate conversationCreate;
	private CommonUpdateConversationStateInterface conversationStateInterface;
	private CommonUpdateMessageBodyPopupWindowInterface messageBodyPopup;

	public void setMessageBodyPopup(
			CommonUpdateMessageBodyPopupWindowInterface messageBodyPopup) {
		this.messageBodyPopup = messageBodyPopup;
	}

	public void setConversationCreate(
			CommonUpdateConversationToCreate conversationCreate) {
		this.conversationCreate = conversationCreate;
	}

	public void setConversationStateInterface(
			CommonUpdateConversationStateInterface conversationStateInterface) {
		this.conversationStateInterface = conversationStateInterface;
	}

	/**
	 * 由 ContactDetail 回调， 通知界面创建会话
	 * 
	 * @author
	 */
	public static interface CommonUpdateConversationToCreate {

		public void updateConversationToCreate(int groupType , long groupID , long remoteUserID);
	}

	/**
	 * 该回调用于，登录时ConversationTabFragment构建完毕后，会回调JNIService，让其存储的延迟广播立刻发送。
	 * 延迟广播为服务器发送的所有组织信息和用户信息
	 * 
	 * @author Administrator
	 * 
	 */
	public static interface CommonUpdateConversationStateInterface {

		/**
		 * 该回调用于，登录时ConversationTabFragment构建完毕后，会回调JNIService，让其存储的延迟广播立刻发送。
		 * 延迟广播为服务器发送的所有组织信息和用户信息
		 * 
		 * @author Administrator
		 * 
		 */
		public void updateConversationState();
	}

	/**
	 * 该回调用于，当聊天界面来新的消息，会让MessageBodyView里正在显示的PopupWindow消失
	 * 
	 * @author Administrator
	 * 
	 */
	public static interface CommonUpdateMessageBodyPopupWindowInterface {

		/**
		 * 该回调用于，当聊天界面来新的消息，会让MessageBodyView里正在显示的PopupWindow消失
		 * 
		 * @author Administrator
		 * 
		 */
		public void updateMessageBodyPopupWindow(MessageBodyView view);
	}

	public void executeConversationCreate(int groupType , long groupID , long remoteUserID) {
		if (conversationCreate != null)
			conversationCreate.updateConversationToCreate(groupType , groupID , remoteUserID);
	}

	public void executeUpdateConversationState() {
		if (conversationStateInterface != null)
			conversationStateInterface.updateConversationState();
	}

	public void executeUpdatePopupWindowState(MessageBodyView view) {
		if (messageBodyPopup != null)
			messageBodyPopup.updateMessageBodyPopupWindow(view);
	}

}
