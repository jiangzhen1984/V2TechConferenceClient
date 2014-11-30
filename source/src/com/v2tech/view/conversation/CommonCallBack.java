package com.v2tech.view.conversation;

import java.util.List;

import com.v2tech.view.widget.CommonAdapter.CommonAdapterItemWrapper;

/**
 * 回调基类
 * @author 
 *
 */
public class CommonCallBack {

	private CommonCallBack(){};
	private static CommonCallBack callback = new CommonCallBack();
	public static CommonCallBack getInstance(){
		return callback;
	}
	
	private CommonUpdateFileStateInterface fileStateInterface;
	private CommonUpdateConversationStateInterface conversationStateInterface;
	private CommonUpdateMessageBodyPopupWindowInterface messageBodyPopup;
	
	public void setMessageBodyPopup(
			CommonUpdateMessageBodyPopupWindowInterface messageBodyPopup) {
		this.messageBodyPopup = messageBodyPopup;
	}

	public void setFileStateInterface(
			CommonUpdateFileStateInterface fileStateInterface) {
		this.fileStateInterface = fileStateInterface;
	}

	public void setConversationStateInterface(
			CommonUpdateConversationStateInterface conversationStateInterface) {
		this.conversationStateInterface = conversationStateInterface;
	}

	/**
	 * 当程序退出时，在ManActivity中处理聊天界面里，所有文件发送及下载的情况。及时更新数据库
	 * @author 
	 * @deprecated 当程序异常退出，则无法更新数据库了
	 */
	public static interface CommonUpdateFileStateInterface{
		
		/**
		 * @deprecated 当程序异常退出，则无法更新数据库了
		 */
		public void updateFileState(List<CommonAdapterItemWrapper> messageArray);
	}
	
	/**
	 *  该回调用于，登录时ConversationTabFragment构建完毕后，会回调JNIService，让其存储的延迟广播立刻发送。
	 *  延迟广播为服务器发送的所有组织信息和用户信息
	 * @author Administrator
	 *
	 */
	public static interface CommonUpdateConversationStateInterface{
		
		/**
		 *  该回调用于，登录时ConversationTabFragment构建完毕后，会回调JNIService，让其存储的延迟广播立刻发送。
		 *  延迟广播为服务器发送的所有组织信息和用户信息
		 * @author Administrator
		 *
		 */
		public void updateConversationState();
	}
	
	/**
	 *  该回调用于，当聊天界面来新的消息，会让MessageBodyView里正在显示的PopupWindow消失
	 * @author Administrator
	 *
	 */
	public static interface CommonUpdateMessageBodyPopupWindowInterface{
		
		/**
		 *  该回调用于，当聊天界面来新的消息，会让MessageBodyView里正在显示的PopupWindow消失
		 * @author Administrator
		 *
		 */
		public void updateMessageBodyPopupWindow(MessageBodyView view);
	}
	
	public void executeUpdateFileState(List<CommonAdapterItemWrapper> messageArray){
		if(fileStateInterface != null)
			fileStateInterface.updateFileState(messageArray);
	}
	
	public void executeUpdateConversationState(){
		if(conversationStateInterface != null)
			conversationStateInterface.updateConversationState();
	}
	
	public void executeUpdatePopupWindowState(MessageBodyView view){
		if(messageBodyPopup != null)
			messageBodyPopup.updateMessageBodyPopupWindow(view);
	}
	
	
}
