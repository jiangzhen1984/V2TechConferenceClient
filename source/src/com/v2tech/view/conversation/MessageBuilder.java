package com.v2tech.view.conversation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.V2.jni.util.V2Log;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.GlobalConfig;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFileItem;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VMessageTextItem;
import com.v2tech.vo.VideoBean;

public class MessageBuilder {

	private static final int MESSAGE_TYPE_IMAGE = 0;
	private static final int MESSAGE_TYPE_AUDIO = 1;
	private static final int MESSAGE_TYPE_FILE = 2;
	
	public static VMessage buildGroupTextMessage(int groupType , long gid, User fromUser,
			String text) {
		String[] array = text.split("\n");
		VMessage vm = new VMessage(groupType , gid, fromUser , new Date(GlobalConfig.getGlobalServerTime()));
		for (int i = 0; i < array.length; i++) {
			String str = array[i];
			VMessageTextItem vti = new VMessageTextItem(vm, str);
			vti.setNewLine(true);
		}
		return vm;
	}

	public static VMessage buildTextMessage(int groupType , long groupID , User fromUser, User toUser,
			String text) {
		VMessage vm = new VMessage(groupType , groupID , fromUser, toUser , new Date(GlobalConfig.getGlobalServerTime()));
		VMessageTextItem vti = new VMessageTextItem(vm, text);
		vti.setNewLine(true);
		return vm;
	}

	public static VMessage buildImageMessage(int groupType , long groupID ,User fromUser, User toUser,
			String imagePath) {
		File newFile = copyBinaryData(MESSAGE_TYPE_IMAGE , imagePath);
		if(newFile == null)
			return null;
		imagePath = newFile.getAbsolutePath();
		VMessage vm = new VMessage(groupType , groupID , fromUser , toUser , new Date(GlobalConfig.getGlobalServerTime()));
		VMessageImageItem item = new VMessageImageItem(vm, imagePath);
		return item.getVm();
	}
	
	public static VMessage buildAudioMessage(int groupType , long groupID ,User fromUser, User toUser,
			String audioPath , int seconds) {
		File newFile = copyBinaryData(MESSAGE_TYPE_AUDIO , audioPath);
		if(newFile == null)
			return null;
		audioPath = newFile.getAbsolutePath();
		VMessage vm = new VMessage(groupType , groupID , fromUser , toUser , new Date(GlobalConfig.getGlobalServerTime()));
		VMessageAudioItem item = new VMessageAudioItem(vm, audioPath , seconds);
		return item.getVm();
	}

	public static VMessage buildFileMessage(int groupType , long groupID , User fromUser, User toUser,
			String filePath , int fileType) {
		File newFile = copyBinaryData(MESSAGE_TYPE_FILE , filePath);
		if(newFile == null)
			return null;
		filePath = newFile.getAbsolutePath();
		VMessage vm = new VMessage(groupType , groupID , fromUser, toUser , new Date(GlobalConfig.getGlobalServerTime()));
		new VMessageFileItem(vm, filePath , fileType);
		return vm;
	}

	public static Uri saveMessage(Context context, VMessage vm) {
		if (vm == null || vm.getFromUser() == null || vm.getToUser() == null) {
			return null;
		}
		
		int type = 0;
		if(vm.getMsgCode() == 0 && vm.getGroupId() == 0l)
			type = MessageLoader.CONTACT_TYPE;
		else
			type = MessageLoader.CROWD_TYPE;
		
		long remoteID = 0;
		if(vm.getFromUser().getmUserId() == vm.getFromUser().getmUserId())
			remoteID = vm.getToUser().getmUserId();
		else
			remoteID = vm.getFromUser().getmUserId();
		
		
		if(!MessageLoader.init(context , 0  , 0 , remoteID , type))
			return null;
		
		//确定远程用户
		long remote = -1;
		if(vm.getFromUser().getmUserId() == GlobalHolder.getInstance().getCurrentUserId())
			remote = vm.getToUser().getmUserId();
		else
			remote = vm.getFromUser().getmUserId();
		//判断数据库是否存在
		long groupType = vm.getMsgCode();
		long groupID = vm.getGroupId();
		if(!MessageLoader.init(context , groupType , groupID , remote , 
				remote == 0 ? MessageLoader.CROWD_TYPE: MessageLoader.CONTACT_TYPE))
			return null;
		//直接将xml存入数据库中，方便以后扩展。
		ContentValues values = new ContentValues();
		values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE, groupType);
		values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_ID, groupID);
		values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID, vm
				.getFromUser().getmUserId());
		if (vm.getToUser() != null) {
			values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID, vm
					.getToUser().getmUserId());
			values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_REMOTE_USER_ID, vm
					.getToUser().getmUserId());
		}
		values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID, vm
				.getUUID());
		values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TRANSTATE, vm
				.getState());
		values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_CONTENT, vm
				.getmXmlDatas());
		values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE, vm
				.getmDateLong());
		values.put(ContentDescriptor.HistoriesMessage.Cols.OWNER_USER_ID, GlobalHolder.getInstance().getCurrentUserId());
		Uri uri = context.getContentResolver().insert(
				ContentDescriptor.HistoriesMessage.CONTENT_URI, values);
		return uri;
	}
	
	public static Uri saveBinaryVMessage(Context context, VMessage vm){
		
		if (vm == null || vm.getFromUser() == null || vm.getToUser() == null) 
			return null;
		
		Uri uri = null;
		//确定远程用户
		long remote = -1;
		if(vm.getFromUser().getmUserId() == GlobalHolder.getInstance().getCurrentUserId())
			remote = vm.getToUser().getmUserId();
		else
			remote = vm.getFromUser().getmUserId();
		
		long groupType = vm.getMsgCode();
		long groupID = vm.getGroupId();
		if(!MessageLoader.init(context , groupType , groupID , remote , 
				remote == 0 ? MessageLoader.CROWD_TYPE: MessageLoader.CONTACT_TYPE))
			return null;
		ContentValues values = new ContentValues();
		switch (vm.getReceiveMessageType()) {
			case GlobalConfig.MEDIA_TYPE_IMAGE:
				List<VMessageImageItem> imageItems = vm.getImageItems();
				for (VMessageImageItem vMessageImageItem : imageItems) {
					values.put(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_TYPE, groupType);
					values.put(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_ID, groupID);
					values.put(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_FROM_USER_ID, vm
							.getFromUser().getmUserId());
					if (vm.getToUser() != null) {
						values.put(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_TO_USER_ID, vm
								.getToUser().getmUserId());
						values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_REMOTE_USER_ID, vm
								.getToUser().getmUserId());
					}
					values.put(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_ID, vm
							.getUUID());
					values.put(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_TRANSTATE, vm
							.getState());
					values.put(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_SAVEDATE, vm
							.getmDateLong());
					values.put(ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_PATH, vMessageImageItem
							.getFilePath());
					values.put(ContentDescriptor.HistoriesGraphic.Cols.OWNER_USER_ID, GlobalHolder.getInstance().getCurrentUserId());
					uri = context.getContentResolver().insert(
							ContentDescriptor.HistoriesGraphic.CONTENT_URI, values);
				}
				break;
			case GlobalConfig.MEDIA_TYPE_AUDIO:
				List<VMessageAudioItem> audioItems = vm.getAudioItems();
				for (VMessageAudioItem vMessageAudioItem : audioItems) {
					values.put(ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_GROUP_TYPE, groupType);
					values.put(ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_GROUP_ID, groupID);
					values.put(ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_FROM_USER_ID, vm
							.getFromUser().getmUserId());
					if (vm.getToUser() != null) {
						values.put(ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_TO_USER_ID, vm
								.getToUser().getmUserId());
						values.put(ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_REMOTE_USER_ID, vm
								.getToUser().getmUserId());
					}
					values.put(ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_ID, vm
							.getUUID());
					values.put(ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_SEND_STATE, vm
							.getState());
					values.put(ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_SAVEDATE, vm
							.getmDateLong());
					values.put(ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_PATH, vMessageAudioItem
							.getAudioFilePath());
					values.put(ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_SECOND, vMessageAudioItem
							.getSeconds());
					values.put(ContentDescriptor.HistoriesAudios.Cols.OWNER_USER_ID, GlobalHolder.getInstance().getCurrentUserId());
					uri = context.getContentResolver().insert(
							ContentDescriptor.HistoriesAudios.CONTENT_URI, values);
				}
				break;
			default:
				break;
		}
		return uri;
	}
	
	
	public static Uri saveMediaChatHistories(Context context , VideoBean bean){
		
		if (bean == null || bean.formUserID == -1 || bean.toUserID == -1) 
			return null;
		
		Uri uri = null;
		ContentValues values = new ContentValues();
		values.put(ContentDescriptor.HistoriesMedia.Cols.OWNER_USER_ID, GlobalHolder.getInstance().getCurrentUserId());
		values.put(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_SAVEDATE, GlobalConfig.getGlobalServerTime());
		values.put(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_CHAT_ID, bean.mediaChatID);
		values.put(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_FROM_USER_ID, bean.formUserID);
		values.put(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TO_USER_ID, bean.toUserID);
		values.put(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_REMOTE_USER_ID, bean.remoteUserID);
		values.put(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TYPE, bean.mediaType);
		values.put(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_STATE, bean.mediaState);
		values.put(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_START_DATE, bean.startDate);
		values.put(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_END_DATE, bean.endDate);
		values.put(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_READ_STATE, bean.readSatate);
		uri = context.getContentResolver().insert(
				ContentDescriptor.HistoriesMedia.CONTENT_URI, values);
		return uri;
	}
	
	public static int updateVMessageItem(Context context,
			VMessageAbstractItem item) {
		ContentValues itemVal = new ContentValues();
		itemVal.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE, item.getType());
		itemVal.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TRANSTATE, item.getState());
		String content = null;

		itemVal.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_CONTENT, content);
		itemVal.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID, item.getUuid());
		return context.getContentResolver().update(
				ContentDescriptor.HistoriesMessage.CONTENT_URI, itemVal,
				ContentDescriptor.HistoriesMessage.Cols.ID + "=?",
				new String[] { item.getId() + "" });
	}
	
	
	
	
	public static int updateVMessageItemToSentFalied(Context context, String uuid) {
		ContentValues itemVal = new ContentValues();
		itemVal.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TRANSTATE , VMessageAbstractItem.STATE_SENT_FALIED);

		return context.getContentResolver().update(
				ContentDescriptor.HistoriesMessage.CONTENT_URI, itemVal,
				ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID + "=?",
				new String[] { uuid });
	}
	
	/**
	 * 将二进制数据拷贝到用户自身目录下存储
	 * @param filePath
	 * @return
	 */
	private static File copyBinaryData(int type , String filePath){
		
		if(TextUtils.isEmpty(filePath))
			return null;
		
		String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
		File srcFile = new File(filePath);
		File desFile = null;
		if(!srcFile.exists())
			return null;
		
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try{
			fis = new FileInputStream(srcFile);
			User user = GlobalHolder.getInstance().getCurrentUser();
			switch (type) {
				case MESSAGE_TYPE_IMAGE:
					desFile = new File(GlobalConfig.getGlobalPicsPath(user) + "/" + fileName);
					break;
				case MESSAGE_TYPE_AUDIO:
					desFile = new File(GlobalConfig.getGlobalAudioPath(user) + "/" + fileName);
					break;
				case MESSAGE_TYPE_FILE:
					desFile = new File(GlobalConfig.getGlobalFilePath(user) + "/" + fileName);
					break;
				default:
					throw new RuntimeException("the copy binary was wroing , unknow type :" + type);
			}
			
			if(filePath.equals(desFile.getAbsolutePath())){
				return srcFile;
			}
			
			fos = new FileOutputStream(desFile);
			int len = 0;
			byte[] buf = new byte[1024];
			while((len = fis.read(buf)) != -1){
				fos.write(buf, 0, len);
				fos.flush();
			}
			return desFile;
		}
		catch (Exception e) {
			V2Log.e(e.getStackTrace().toString());
			return null;
		}
		finally{
			if(fis != null){
				try {
					fis.close();
				} catch (IOException e) {
					V2Log.e("MessageBuilder copyBinaryData : the FileInputStream closed failed...");
					e.printStackTrace();
				}
			}
			
			if(fos != null){
				try {
					fos.close();
				} catch (IOException e) {
					V2Log.e("MessageBuilder copyBinaryData : the FileOutputStream closed failed...");
					e.printStackTrace();
				}
			}
		}
	}
}
