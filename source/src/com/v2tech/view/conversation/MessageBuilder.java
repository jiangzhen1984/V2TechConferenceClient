package com.v2tech.view.conversation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.util.V2Log;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.DataBaseContext;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.GlobalConfig;
import com.v2tech.vo.FileInfoBean;
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
	public static DataBaseContext mContext;

	public static void init(Context context) {
		mContext = new DataBaseContext(context);
	}

	public static VMessage buildGroupTextMessage(int groupType, long gid,
			User fromUser, String text) {
		String[] array = text.split("\n");
		VMessage vm = new VMessage(groupType, gid, fromUser, new Date(
				GlobalConfig.getGlobalServerTime()));
		for (int i = 0; i < array.length; i++) {
			String str = array[i];
			VMessageTextItem vti = new VMessageTextItem(vm, str);
			vti.setNewLine(true);
		}
		return vm;
	}

	public static VMessage buildTextMessage(int groupType, long groupID,
			User fromUser, User toUser, String text) {
		VMessage vm = new VMessage(groupType, groupID, fromUser, toUser,
				new Date(GlobalConfig.getGlobalServerTime()));
		VMessageTextItem vti = new VMessageTextItem(vm, text);
		vti.setNewLine(true);
		return vm;
	}

	public static VMessage buildImageMessage(int groupType, long groupID,
			User fromUser, User toUser, String imagePath) {
		String uuid = UUID.randomUUID().toString();
		File newFile = copyBinaryData(MESSAGE_TYPE_IMAGE, imagePath, uuid);
		if (newFile == null)
			return null;
		imagePath = newFile.getAbsolutePath();
		VMessage vm = new VMessage(groupType, groupID, fromUser, toUser,
				new Date(GlobalConfig.getGlobalServerTime()));
		VMessageImageItem item = new VMessageImageItem(vm, imagePath);
		item.setState(VMessageAbstractItem.STATE_NORMAL);
		item.setUuid(uuid);
		return item.getVm();
	}

	public static VMessage buildAudioMessage(int groupType, long groupID,
			User fromUser, User toUser, String audioPath, int seconds) {
		VMessage vm = new VMessage(groupType, groupID, fromUser, toUser,
				new Date(GlobalConfig.getGlobalServerTime()));
		String uuid = audioPath.substring(audioPath.lastIndexOf("/") + 1,
				audioPath.lastIndexOf("."));
		VMessageAudioItem item = new VMessageAudioItem(vm, uuid, audioPath,
				seconds, VMessageAbstractItem.STATE_READED);
		item.setState(VMessageAbstractItem.STATE_NORMAL);
		return item.getVm();
	}

	public static VMessage buildFileMessage(int groupType, long groupID,
			User fromUser, User toUser, FileInfoBean bean) {
		VMessage vm = new VMessage(groupType, groupID, fromUser, toUser,
				new Date(GlobalConfig.getGlobalServerTime()));
		VMessageFileItem item = new VMessageFileItem(vm, bean.filePath,
				VMessageFileItem.STATE_FILE_SENDING);
		return item.getVm();
	}

	/**
	 * put the chat datas VMessage Object to DataBases
	 * 
	 * @param context
	 * @param vm
	 * @return
	 */
	public static Uri saveMessage(Context context, VMessage vm) {
		if (vm == null)
			throw new NullPointerException("the given VMessage object is null");

		DataBaseContext mContext = new DataBaseContext(context);
		int type = 0;
		// 确定远程用户
		long remote = -1;
		if (vm.getMsgCode() == 0 && vm.getGroupId() == 0l) {
			type = MessageLoader.CONTACT_TYPE;
			if (vm.getFromUser().getmUserId() == GlobalHolder.getInstance()
					.getCurrentUserId())
				remote = vm.getToUser().getmUserId();
			else
				remote = vm.getFromUser().getmUserId();
		} else
			type = MessageLoader.CROWD_TYPE;

		// 判断数据库是否存在
		long groupType = vm.getMsgCode();
		long groupID = vm.getGroupId();
		if (!MessageLoader.isTableExist(context, groupType, groupID, remote,
				type))
			return null;
		// 直接将xml存入数据库中，方便以后扩展。
		ContentValues values = new ContentValues();
		values.put(
				ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_TYPE,
				groupType);
		values.put(
				ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_GROUP_ID,
				groupID);
		values.put(
				ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_FROM_USER_ID,
				vm.getFromUser().getmUserId());
		if (vm.getToUser() != null) {
			values.put(
					ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TO_USER_ID,
					vm.getToUser().getmUserId());
		}
		values.put(
				ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_REMOTE_USER_ID,
				remote);
		values.put(ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_ID,
				vm.getUUID());
		values.put(
				ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_TRANSTATE,
				vm.getState());
		values.put(
				ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_CONTENT,
				vm.getmXmlDatas());
		values.put(
				ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_SAVEDATE,
				vm.getmDateLong());
		values.put(ContentDescriptor.HistoriesMessage.Cols.OWNER_USER_ID,
				GlobalHolder.getInstance().getCurrentUserId());
		Uri uri = mContext.getContentResolver().insert(
				ContentDescriptor.HistoriesMessage.CONTENT_URI, values);
		vm.setId(ContentUris.parseId(uri));
		return uri;
	}

	/**
	 * put the binary(image or record audio) datas VMessage Object to DataBases
	 * 
	 * @param context
	 * @param vm
	 * @return
	 */
	public static Uri saveBinaryVMessage(Context context, VMessage vm) {

		DataBaseContext mContext = new DataBaseContext(context);
		if (vm == null)
			throw new NullPointerException("the given VMessage object is null");

		Uri uri = null;
		int type = 0;
		// 确定远程用户
		long remote = -1;
		if (vm.getMsgCode() == 0 && vm.getGroupId() == 0l) {
			type = MessageLoader.CONTACT_TYPE;
			if (vm.getFromUser().getmUserId() == GlobalHolder.getInstance()
					.getCurrentUserId())
				remote = vm.getToUser().getmUserId();
			else
				remote = vm.getFromUser().getmUserId();
		} else
			type = MessageLoader.CROWD_TYPE;

		long groupType = vm.getMsgCode();
		long groupID = vm.getGroupId();
		if (!MessageLoader.isTableExist(context, groupType, groupID, remote,
				type))
			return null;
		ContentValues values = new ContentValues();

		List<VMessageImageItem> imageItems = vm.getImageItems();
		for (VMessageImageItem vMessageImageItem : imageItems) {
			values.put(
					ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_TYPE,
					groupType);
			values.put(
					ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_GROUP_ID,
					groupID);
			values.put(
					ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_FROM_USER_ID,
					vm.getFromUser().getmUserId());
			if (vm.getToUser() != null) {
				values.put(
						ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_TO_USER_ID,
						vm.getToUser().getmUserId());
			}
			values.put(
					ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_REMOTE_USER_ID,
					remote);
			values.put(
					ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_ID,
					vMessageImageItem.getUuid());
			values.put(
					ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_TRANSTATE,
					vm.getState());
			values.put(
					ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_SAVEDATE,
					vm.getmDateLong());
			values.put(
					ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_PATH,
					vMessageImageItem.getFilePath());
			values.put(ContentDescriptor.HistoriesGraphic.Cols.OWNER_USER_ID,
					GlobalHolder.getInstance().getCurrentUserId());
			uri = mContext.getContentResolver().insert(
					ContentDescriptor.HistoriesGraphic.CONTENT_URI, values);
		}

		List<VMessageAudioItem> audioItems = vm.getAudioItems();
		for (VMessageAudioItem vMessageAudioItem : audioItems) {
			values.put(
					ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_GROUP_TYPE,
					groupType);
			values.put(
					ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_GROUP_ID,
					groupID);
			values.put(
					ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_FROM_USER_ID,
					vm.getFromUser().getmUserId());
			if (vm.getToUser() != null) {
				values.put(
						ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_TO_USER_ID,
						vm.getToUser().getmUserId());
			}
			values.put(
					ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_REMOTE_USER_ID,
					remote);
			values.put(ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_ID,
					vMessageAudioItem.getUuid());
			values.put(
					ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_SEND_STATE,
					vm.getState());
			values.put(
					ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_SAVEDATE,
					vm.getmDateLong());
			values.put(
					ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_PATH,
					vMessageAudioItem.getAudioFilePath());
			values.put(
					ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_SECOND,
					vMessageAudioItem.getSeconds());
			values.put(
					ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_READ_STATE,
					vMessageAudioItem.getReadState());
			values.put(ContentDescriptor.HistoriesAudios.Cols.OWNER_USER_ID,
					GlobalHolder.getInstance().getCurrentUserId());
			uri = mContext.getContentResolver().insert(
					ContentDescriptor.HistoriesAudios.CONTENT_URI, values);
		}
		return uri;
	}

	/**
	 * put the file datas VMessage Object to DataBases
	 * 
	 * @param context
	 * @param vm
	 * @return
	 */
	public static Uri saveFileVMessage(Context context, VMessage vm) {

		DataBaseContext mContext = new DataBaseContext(context);
		if (vm == null)
			throw new NullPointerException("the given VMessage object is null");

		if (vm.getFileItems().size() <= 0)
			return null;

		// 确定远程用户
		long remote = -1;
		switch (vm.getMsgCode()) {
		case V2GlobalEnum.GROUP_TYPE_USER:
			if (vm.getFromUser().getmUserId() == GlobalHolder.getInstance()
					.getCurrentUserId())
				remote = vm.getToUser().getmUserId();
			else
				remote = vm.getFromUser().getmUserId();
			break;
		case V2GlobalEnum.GROUP_TYPE_DEPARTMENT:
		case V2GlobalEnum.GROUP_TYPE_CROWD:
			remote = vm.getGroupId();
			break;
		default:
			break;
		}
		Uri uri = null;
		VMessageFileItem file = null;
		for (int i = 0; i < vm.getFileItems().size(); i++) {
			file = vm.getFileItems().get(i);
			ContentValues values = new ContentValues();
			values.put(ContentDescriptor.HistoriesFiles.Cols.OWNER_USER_ID,
					GlobalHolder.getInstance().getCurrentUserId());
			values.put(
					ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SAVEDATE,
					GlobalConfig.getGlobalServerTime());
			values.put(
					ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_FROM_USER_ID,
					vm.getFromUser().getmUserId());
			if (vm.getToUser() != null) {
				values.put(
						ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_TO_USER_ID,
						vm.getToUser().getmUserId());
			}
			values.put(
					ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_REMOTE_USER_ID,
					remote);
			values.put(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID,
					file.getUuid());
			values.put(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_PATH,
					file.getFilePath());
			values.put(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SIZE,
					file.getFileSize());
			values.put(
					ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE,
					file.getState());
			uri = mContext.getContentResolver().insert(
					ContentDescriptor.HistoriesFiles.CONTENT_URI, values);
		}
		return uri;
	}

	/**
	 * put the media(audio or video) record datas VMessage Object to DataBases
	 * 
	 * @param context
	 * @param bean
	 * @return
	 */
	public static Uri saveMediaChatHistories(Context context, VideoBean bean) {

		DataBaseContext mContext = new DataBaseContext(context);
		if (bean == null)
			throw new NullPointerException("the given VideoBean object is null");

		Uri uri = null;
		ContentValues values = new ContentValues();
		values.put(ContentDescriptor.HistoriesMedia.Cols.OWNER_USER_ID,
				GlobalHolder.getInstance().getCurrentUserId());
		values.put(
				ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_SAVEDATE,
				GlobalConfig.getGlobalServerTime());
		values.put(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_CHAT_ID,
				bean.mediaChatID);
		values.put(
				ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_FROM_USER_ID,
				bean.formUserID);
		values.put(
				ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TO_USER_ID,
				bean.toUserID);
		values.put(
				ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_REMOTE_USER_ID,
				bean.remoteUserID);
		values.put(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_TYPE,
				bean.mediaType);
		values.put(ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_STATE,
				bean.mediaState);
		values.put(
				ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_START_DATE,
				bean.startDate);
		values.put(
				ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_END_DATE,
				bean.endDate);
		values.put(
				ContentDescriptor.HistoriesMedia.Cols.HISTORY_MEDIA_READ_STATE,
				bean.readSatate);
		uri = mContext.getContentResolver().insert(
				ContentDescriptor.HistoriesMedia.CONTENT_URI, values);
		return uri;
	}

	public static int updateVMessageItem(Context context,
			VMessageAbstractItem item) {
		DataBaseContext mContext = new DataBaseContext(context);
		ContentValues itemVal = new ContentValues();
		int updates = 0;
		switch (item.getType()) {
		case VMessageAbstractItem.ITEM_TYPE_FILE:
			VMessageFileItem fileItem = (VMessageFileItem) item;
			itemVal.put(
					ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE,
					fileItem.getState());
			updates = mContext.getContentResolver().update(
					ContentDescriptor.HistoriesFiles.CONTENT_URI,
					itemVal,
					ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID
							+ "=?",
					new String[] { String.valueOf(fileItem.getUuid()) });
			break;
		default:
			break;
		}
		return updates;
	}

	public static int updateVMessageItemToSentFalied(Context context,
			VMessage vm) {
		DataBaseContext mContext = new DataBaseContext(context);
		ContentValues itemVal = new ContentValues();
		int updates = 0;
		List<VMessageAbstractItem> items = vm.getItems();
		for (VMessageAbstractItem item : items) {
			switch (item.getType()) {
			case VMessageAbstractItem.ITEM_TYPE_FILE:
				if (vm.isLocal()) {
					itemVal.put(
							ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE,
							VMessageAbstractItem.STATE_FILE_SENT_FALIED);
				} else {
					itemVal.put(
							ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE,
							VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED);
				}

				updates = mContext.getContentResolver().update(
						ContentDescriptor.HistoriesFiles.CONTENT_URI,
						itemVal,
						ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID
								+ "=?",
						new String[] { String.valueOf(item.getUuid()) });
				break;
			default:
				break;
			}
		}
		return updates;
	}

	/**
	 * 将二进制数据拷贝到用户自身目录下存储
	 * 
	 * @param filePath
	 * @return
	 */
	private static File copyBinaryData(int type, String filePath, String uuid) {

		if (TextUtils.isEmpty(filePath))
			return null;

		String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
		File srcFile = new File(filePath);
		File desFile = null;
		if (!srcFile.exists())
			return null;

		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(srcFile);
			switch (type) {
			case MESSAGE_TYPE_IMAGE:
				desFile = new File(GlobalConfig.getGlobalPicsPath() + "/"
						+ uuid + fileName.substring(fileName.lastIndexOf(".")));
				break;
			case MESSAGE_TYPE_AUDIO:
				desFile = new File(GlobalConfig.getGlobalAudioPath() + "/"
						+ uuid + fileName.substring(fileName.lastIndexOf(".")));
				break;
			default:
				throw new RuntimeException(
						"the copy binary was wroing , unknow type :" + type);
			}

			if (filePath.equals(desFile.getAbsolutePath())) {
				return srcFile;
			}

			fos = new FileOutputStream(desFile);
			int len = 0;
			byte[] buf = new byte[1024];
			while ((len = fis.read(buf)) != -1) {
				fos.write(buf, 0, len);
				fos.flush();
			}
			return desFile;
		} catch (Exception e) {
			V2Log.e(e.getStackTrace().toString());
			return null;
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					V2Log.e("MessageBuilder copyBinaryData : the FileInputStream closed failed...");
					e.printStackTrace();
				}
			}

			if (fos != null) {
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
