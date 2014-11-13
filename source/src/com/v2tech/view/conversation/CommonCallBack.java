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
	
	
	public void setFileStateInterface(
			CommonUpdateFileStateInterface fileStateInterface) {
		this.fileStateInterface = fileStateInterface;
	}

	public void setConversationStateInterface(
			CommonUpdateConversationStateInterface conversationStateInterface) {
		this.conversationStateInterface = conversationStateInterface;
	}

	public static interface CommonUpdateFileStateInterface{
		/**
		 * 当程序退出时，在ManActivity中处理聊天界面里，所有文件发送及下载的情况。及时更新数据库
		 * @author 
		 *
		 */
		public void updateFileState(List<CommonAdapterItemWrapper> messageArray);
	}
	
	public static interface CommonUpdateConversationStateInterface{
		
		public void updateConversationState();
	}
	
	public void executeUpdateFileState(List<CommonAdapterItemWrapper> messageArray){
		if(fileStateInterface != null)
			fileStateInterface.updateFileState(messageArray);
	}
	
	public void executeUpdateConversationState(){
		if(conversationStateInterface != null)
			conversationStateInterface.updateConversationState();
	}
}
