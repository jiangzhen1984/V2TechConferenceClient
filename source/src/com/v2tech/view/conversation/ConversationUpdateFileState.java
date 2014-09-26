package com.v2tech.view.conversation;

import java.util.List;

import com.v2tech.view.widget.CommonAdapter.CommonAdapterItemWrapper;

/**
 * 当程序退出时，在ManActivity中处理聊天界面里，所有文件发送及下载的情况。及时更新数据库
 * @author 
 *
 */
public class ConversationUpdateFileState {

	private ConversationUpdateFileState(){};
	private static ConversationUpdateFileState fileState = new ConversationUpdateFileState();
	public static ConversationUpdateFileState getInstance(){
		return fileState;
	}
	
	private FileStateInterface fileStateInterface;
	
	public void setFileStateInterface(FileStateInterface fileStateInterface) {
		this.fileStateInterface = fileStateInterface;
	}
	
	/**
	 * 当程序退出时，在ManActivity中处理聊天界面里，所有文件发送及下载的情况。及时更新数据库
	 * @author 
	 *
	 */
	public static interface FileStateInterface{
		
		public void updateFileState(List<CommonAdapterItemWrapper> messageArray);
	}
	
	public void executeUpdate(List<CommonAdapterItemWrapper> messageArray){
		fileStateInterface.updateFileState(messageArray);
		
	}
}
