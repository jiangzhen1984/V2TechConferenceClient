package com.v2tech.view.conversation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.V2.jni.V2GlobalEnum;
import com.V2.jni.ind.V2Group;
import com.V2.jni.util.V2Log;
import com.V2.jni.util.XmlAttributeExtractor;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.ContentDescriptor.HistoriesCrowd;
import com.v2tech.db.DataBaseContext;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.GlobalConfig;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.FileInfoBean;
import com.v2tech.vo.GroupQualicationState;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFileItem;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VMessageQualification.ReadState;
import com.v2tech.vo.VMessageQualification.Type;
import com.v2tech.vo.VMessageQualificationApplicationCrowd;
import com.v2tech.vo.VMessageQualificationInvitationCrowd;
import com.v2tech.vo.VMessageTextItem;
import com.v2tech.vo.VideoBean;

public class MessageBuilder {

	private static final int MESSAGE_TYPE_IMAGE = 0;
	private static final int MESSAGE_TYPE_AUDIO = 1;
	public static Context context;

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
		VMessageAudioItem item = new VMessageAudioItem(vm, uuid , audioPath, seconds , VMessageAbstractItem.STATE_READED);
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
					vMessageAudioItem.getState());
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

	// *********************************qualification message
	// **********************************************
	/**
	 * Save new qualification message to database, and fill id to msg object
	 * 
	 * @param context
	 * @param msg
	 * @return
	 */
	public static Uri saveQualicationMessage(Context context,
			VMessageQualification msg) {
		if (context == null)
			context = MessageBuilder.context;

		if (msg == null) {
			V2Log.e("To store failed...please check the given VMessageQualification Object in the databases");
			return null;
		}
		DataBaseContext mContext = new DataBaseContext(context);
		ContentValues values = new ContentValues();
		values.put(ContentDescriptor.HistoriesCrowd.Cols.OWNER_USER_ID,
				GlobalHolder.getInstance().getCurrentUserId());
		values.put(
				ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE,
				GlobalConfig.getGlobalServerTime());
		Uri uri = null;
		switch (msg.getType()) {
		case CROWD_INVITATION:
			VMessageQualificationInvitationCrowd crowdInviteMsg = (VMessageQualificationInvitationCrowd) msg;
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_FROM_USER_ID,
					crowdInviteMsg.getInvitationUser().getmUserId());
			if (crowdInviteMsg.getBeInvitatonUser() != null) {
				values.put(
						ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_TO_USER_ID,
						crowdInviteMsg.getBeInvitatonUser().getmUserId());
			}
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID,
					crowdInviteMsg.getInvitationUser().getmUserId());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_APPLY_REASON,
					"");
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REFUSE_REASON,
					crowdInviteMsg.getRejectReason());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
					crowdInviteMsg.getReadState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_STATE,
					crowdInviteMsg.getQualState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_AUTHTYPE,
					crowdInviteMsg.getCrowdGroup().getAuthType().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_BASE_INFO,
					crowdInviteMsg.getCrowdGroup().toXml());
			values.put(ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID,
					crowdInviteMsg.getCrowdGroup().getmGId());
            values.put(HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE,
                    crowdInviteMsg.getmTimestamp().getTime());
			uri = mContext.getContentResolver().insert(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI, values);
			return uri;
		case CROWD_APPLICATION:
			VMessageQualificationApplicationCrowd crowdApplyMsg = (VMessageQualificationApplicationCrowd) msg;
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_FROM_USER_ID,
					crowdApplyMsg.getApplicant().getmUserId());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_TO_USER_ID,
					GlobalHolder.getInstance().getCurrentUserId());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID,
					crowdApplyMsg.getApplicant().getmUserId());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_APPLY_REASON,
					crowdApplyMsg.getApplyReason());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REFUSE_REASON,
					crowdApplyMsg.getRejectReason());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
					crowdApplyMsg.getReadState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_STATE,
					crowdApplyMsg.getQualState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_AUTHTYPE,
					crowdApplyMsg.getCrowdGroup().getAuthType().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_BASE_INFO,
					crowdApplyMsg.getCrowdGroup().toXml());
			values.put(ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID,
					crowdApplyMsg.getCrowdGroup().getmGId());
			values.put(HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE,
					crowdApplyMsg.getmTimestamp().getTime());
			uri = mContext.getContentResolver().insert(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI, values);
			crowdApplyMsg.setId(ContentUris.parseId(uri));
			return uri;
		case CONTACT:
			break;
		default:
			throw new RuntimeException(
					"invalid VMessageQualification enum type.. please check the type");
		}
		return uri;
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public static int updateQualicationMessageState(long id , GroupQualicationState obj) {
		
		if (obj == null)
			return -1;
		
		ContentValues values = new ContentValues();
		switch (obj.qualicationType) {
		case CROWD_INVITATION:
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REFUSE_REASON,
					obj.refuseReason);
			break;
		case CROWD_APPLICATION:
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_APPLY_REASON,
					obj.applyReason);
			break;
		case CONTACT:
			break;
		default:
			throw new RuntimeException(
					"invalid VMessageQualification enum type.. please check the type");
		}
		values.put(ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_STATE,
				obj.state.intValue());
		values.put(
				ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
				ReadState.UNREAD.intValue());
		String where = ContentDescriptor.HistoriesCrowd.Cols.ID
				+ " = ?";
		values.put(
				ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE,
				GlobalConfig.getGlobalServerTime());
		int updates = context.getContentResolver().update(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI, values, where,
				new String[] { String.valueOf(id) });
		return updates;
	}

    /**
     * Update a qualification message to database by V2Group Object
     * @param crowd
     * @param obj
     * @return
     */
    public static long updateQualicationMessageState(V2Group crowd,
                                                     GroupQualicationState obj) {
        if (crowd == null){
            V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser given V2Group"
                    + "is null!");
            return -1;
        }

        if (obj == null){
            V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser get crowdGroup"
                    + "is null!");
            return -1;
        }

        if (crowd.owner == null){
            V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser get Owner User"
                    + "is null!");
            return -1;
        }

        long userID = crowd.owner.uid;
        long groupID = crowd.id;

        CrowdGroup crowdGroup = (CrowdGroup) GlobalHolder.getInstance()
                .getGroupById(V2GlobalEnum.GROUP_TYPE_CROWD, groupID);
        if (crowdGroup == null) {
            V2Log.e("MessageBuilder updateQualicationMessageState --> the VMessageQualification Object is null , Need to build"
                    + "groupID is : " + groupID + " userID is : " + userID);
            User user = GlobalHolder.getInstance().getUser(userID);
            if(user == null) {
                V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser get Owner User" +
                        "from GlobalHolder is null!");
                return -1;
            }
            crowdGroup = new CrowdGroup(crowd.id,
                    crowd.name , user, null);
            crowdGroup.setBrief(crowd.brief);
            crowdGroup.setAnnouncement(crowd.announce);
        }

        return updateQualicationMessageState(crowdGroup , userID , obj);
    }

    /**
     * Update a qualification message to database by groupID and userID
     * @param groupID
     * @param userID
     * @param obj
     * @return
     */
    public static long updateQualicationMessageState(long groupID , long userID ,
                                                     GroupQualicationState obj) {
        if (obj == null) {
            V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser given GroupQualicationState"
                    + "is null!");
            return -1;
        }

        CrowdGroup crowdGroup = (CrowdGroup) GlobalHolder.getInstance()
                .getGroupById(V2GlobalEnum.GROUP_TYPE_CROWD, groupID);
        if (crowdGroup == null) {
            V2Log.e("MessageBuilder updateQualicationMessageState --> the VMessageQualification Object is null , Need to build"
                    + "groupID is : " + groupID + " userID is : " + userID);
            User user = GlobalHolder.getInstance().getUser(userID);
            if(user == null) {
                V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser get Owner User" +
                        "from GlobalHolder is null!");
                return -1;
            }
            
            if(obj.isOwnerGroup)
	            crowdGroup = new CrowdGroup(groupID,
	                    null , GlobalHolder.getInstance().getCurrentUser() , null);
            else
            	crowdGroup = new CrowdGroup(groupID,
	                    null , user , null);
            return updateQualicationMessageState(crowdGroup , userID , obj);
        } else {
            return updateQualicationMessageState(crowdGroup , userID , obj);
        }
    }

    /**
     * Update a qualification message to database
     * @param crowdGroup
     * @param obj
     * @return
     */
	public static long updateQualicationMessageState(CrowdGroup crowdGroup, long userId ,
			GroupQualicationState obj) {

		if (obj == null){
			V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser given GroupQualicationState"
					+ "is null!");
			return -1;
		}

		if (crowdGroup == null){
			V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser get crowdGroup"
					+ "is null!");
			return -1;
		}

		if (crowdGroup.getOwnerUser() == null){
			V2Log.e("MessageBuilder updateQualicationMessageState --> update failed... beacuser get Owner User"
					+ "is null!");
			return -1;
		}

		long uid = -1;
		long userID = crowdGroup.getOwnerUser().getmUserId();
		if(userID == GlobalHolder.getInstance().getCurrentUserId())
			uid = userId;
		else
			uid = GlobalHolder.getInstance().getCurrentUserId();
        long groupID = crowdGroup.getmGId();
		DataBaseContext mContext = new DataBaseContext(context);
		VMessageQualification crowdQuion = MessageBuilder
				.queryQualMessageByCrowdId(null, userId, groupID);
		if (crowdQuion == null) {
			if (obj.qualicationType == Type.CROWD_APPLICATION) {
                User applicant = GlobalHolder.getInstance().getUser(uid);
                if (applicant == null)
                    applicant = new User(uid);
				crowdQuion = new VMessageQualificationApplicationCrowd(
						crowdGroup, applicant);
				((VMessageQualificationApplicationCrowd) crowdQuion)
						.setApplyReason(obj.applyReason);
			} else {
				crowdQuion = new VMessageQualificationInvitationCrowd(
						crowdGroup, GlobalHolder.getInstance().getCurrentUser());
                crowdQuion
						.setRejectReason(obj.refuseReason);
			}
			crowdQuion.setReadState(ReadState.UNREAD);
			crowdQuion.setQualState(obj.state);
			crowdQuion.setmTimestamp(new Date(GlobalConfig
                    .getGlobalServerTime()));
			Uri uri = MessageBuilder.saveQualicationMessage(null, crowdQuion);
			if (uri != null){
				long id = Long.parseLong(uri.getLastPathSegment());
				crowdQuion.setId(id);
				return id;
			}
			else{
				V2Log.e("MessageBuilder updateQualicationMessageState --> Save VMessageQualification Object failed , "
						+ "the Uri is null...groupID is : " + groupID + " userID is : " + userID);
				return -1;
			}
		}

		ContentValues values = new ContentValues();
		switch (obj.qualicationType) {
		case CROWD_INVITATION:
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REFUSE_REASON,
					obj.refuseReason);
			break;
		case CROWD_APPLICATION:
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_APPLY_REASON,
					obj.applyReason);
			break;
		case CONTACT:
			break;
		default:
			throw new RuntimeException(
					"invalid VMessageQualification enum type.. please check the type");
		}
		values.put(ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_STATE,
				obj.state.intValue());
		values.put(
				ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
				ReadState.UNREAD.intValue());
		values.put(
				HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE,
				GlobalConfig.getGlobalServerTime());
		String where = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID
				+ " = ? and " + HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID + " = ?";
        String[] args = new String[]{String.valueOf(groupID) , String.valueOf(userId)};
		mContext.getContentResolver().update(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI, values, where,
                args);
		return crowdQuion.getId();
	}

	/**
	 * Update a qualification message to database
	 * 
	 * @param context
	 * @param msg
	 * @return
	 */
	public static int updateQualicationMessage(Context context,
			VMessageQualification msg) {
		if (msg == null) {
			V2Log.e("To store failed...please check the given VMessageQualification Object in the databases");
			return -1;
		}

        if(msg.getType() == Type.CROWD_APPLICATION){
            if(((VMessageQualificationApplicationCrowd)msg).getApplicant() == null) {
                V2Log.e("To store failed...please check the given VMessageQualification Object , Because applicant user is null!");
                return -1;
            }
            else if(((VMessageQualificationApplicationCrowd)msg).getCrowdGroup() == null){
                V2Log.e("To store failed...please check the given VMessageQualification Object , Because crowd group is null!");
                return -1;
            }
        }

        if(msg.getType() == Type.CROWD_INVITATION){
            if(((VMessageQualificationInvitationCrowd)msg).getInvitationUser() == null) {
                V2Log.e("To store failed...please check the given VMessageQualification Object , Because invitationUser user is null!");
                return -1;
            }
            else if(((VMessageQualificationInvitationCrowd)msg).getCrowdGroup() == null){
                V2Log.e("To store failed...please check the given VMessageQualification Object , Because crowd group is null!");
                return -1;
            }
        }

		DataBaseContext mContext = new DataBaseContext(context);
		ContentValues values = new ContentValues();
		String[] selectionArgs = null;
		switch (msg.getType()) {
		case CROWD_INVITATION:
			VMessageQualificationInvitationCrowd crowdInviteMsg = (VMessageQualificationInvitationCrowd) msg;
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_APPLY_REASON,
					"");
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REFUSE_REASON,
					crowdInviteMsg.getRejectReason());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
					crowdInviteMsg.getReadState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_STATE,
					crowdInviteMsg.getQualState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_AUTHTYPE,
					crowdInviteMsg.getCrowdGroup().getAuthType().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_BASE_INFO,
					crowdInviteMsg.getCrowdGroup().toXml());
			selectionArgs = new String[] { String.valueOf(crowdInviteMsg
					.getCrowdGroup().getmGId()) , String.valueOf(crowdInviteMsg.getInvitationUser().getmUserId()) };
			break;
		case CROWD_APPLICATION:
			VMessageQualificationApplicationCrowd crowdApplyMsg = (VMessageQualificationApplicationCrowd) msg;
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_APPLY_REASON,
					crowdApplyMsg.getApplyReason());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REFUSE_REASON,
					crowdApplyMsg.getRejectReason());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
					crowdApplyMsg.getReadState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_STATE,
					crowdApplyMsg.getQualState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_AUTHTYPE,
					crowdApplyMsg.getCrowdGroup().getAuthType().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_BASE_INFO,
					crowdApplyMsg.getCrowdGroup().toXml());
			selectionArgs = new String[] { String.valueOf(crowdApplyMsg
					.getCrowdGroup().getmGId()) , String.valueOf(crowdApplyMsg.getApplicant().getmUserId()) };
			break;
		case CONTACT:
			break;
		default:
			throw new RuntimeException(
					"invalid VMessageQualification enum type.. please check the type");
		}
		String where = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID
				+ " = ? and " + HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID + " = ?";
//		values.put(
//				ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE,
//				GlobalConfig.getGlobalServerTime());
		int updates = mContext.getContentResolver().update(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI, values, where,
				selectionArgs);
		return updates;
	}

    /**
     * Query qualification message by Message's id
     * @param context
     * @param id
     * @return
     */
	public static VMessageQualification queryQualMessageById(Context context,
			long id) {

		DataBaseContext mContext = new DataBaseContext(context);

		String selection = "" + ContentDescriptor.HistoriesCrowd.Cols.ID
				+ " = ? ";
		String[] selectionArgs = new String[] { id + "" };
		String sortOrder = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE
				+ " desc";
		Cursor cursor = mContext.getContentResolver().query(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI,
				ContentDescriptor.HistoriesCrowd.Cols.ALL_CLOS, selection,
				selectionArgs, sortOrder);

		if (cursor == null || cursor.getCount() <= 0)
			return null;
		VMessageQualification msg = null;
		if (cursor.moveToNext()) {
			msg = extraMsgFromCursor(cursor);
		}
		cursor.close();

		return msg;

	}

	public static VMessageQualification queryQualMessageByCrowdId(
			Context context, User user, CrowdGroup cg) {

		if (user == null || cg == null) {
			V2Log.e("To query failed...please check the given User Object");
			return null;
		}

		return queryQualMessageByCrowdId(context, user.getmUserId(),
				cg.getmGId());
	}

    /**
     * Query qualification message by crowd group id and user id
     * @param context
     * @param userID
     * @param groupID
     * @return
     */
	public static VMessageQualification queryQualMessageByCrowdId(
			Context context, long userID, long groupID) {
		if (context == null)
			context = MessageBuilder.context;

		DataBaseContext mContext = new DataBaseContext(context);
		String selection = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID
				+ "= ? and "
				+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID
				+ "= ?";
		String[] selectionArgs = new String[] { String.valueOf(userID),
				String.valueOf(groupID) };
		String sortOrder = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE
				+ " desc";
		Cursor cursor = mContext.getContentResolver().query(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI,
				ContentDescriptor.HistoriesCrowd.Cols.ALL_CLOS, selection,
				selectionArgs, sortOrder);

		if (cursor == null || cursor.getCount() <= 0)
			return null;
		VMessageQualification msg = null;
		if (cursor.moveToNext()) {
			msg = extraMsgFromCursor(cursor);
		}
		cursor.close();

		return msg;

	}

    /**
     * Query qualification of apply type message by apply user id
     * @param userID
     * @return
     */
    public static VMessageQualification queryApplyQualMessageByUserId(long userID) {

        DataBaseContext mContext = new DataBaseContext(context);
        String selection = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID
                + "= ? ";
        String[] selectionArgs = new String[] { String.valueOf(userID) };
        String sortOrder = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE
                + " desc";
        Cursor cursor = mContext.getContentResolver().query(
                ContentDescriptor.HistoriesCrowd.CONTENT_URI,
                ContentDescriptor.HistoriesCrowd.Cols.ALL_CLOS, selection,
                selectionArgs, sortOrder);

        if (cursor == null || cursor.getCount() <= 0)
            return null;

        VMessageQualification msg = null;
        if (cursor.moveToFirst()) {
            msg = extraMsgFromCursor(cursor);
        }
        cursor.close();
        return msg;
    }

    /**
	 * Get a List Collection for qualification message from database
	 * 
	 * @param context
	 * @param user
	 * @return
	 */
	public static List<VMessageQualification> queryQualMessageList(
			Context context, User user) {
		DataBaseContext mContext = new DataBaseContext(context);
		if (user == null) {
			V2Log.e("To query failed...please check the given User Object");
			return null;
		}

		List<VMessageQualification> list = new ArrayList<VMessageQualification>();
		String selection = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_FROM_USER_ID
				+ "= ? or "
				+ ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_TO_USER_ID
				+ "= ?";
		String[] selectionArgs = new String[] {
				String.valueOf(user.getmUserId()),
				String.valueOf(user.getmUserId()) };
		String sortOrder = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE
				+ " desc";
		Cursor cursor = mContext.getContentResolver().query(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI,
				ContentDescriptor.HistoriesCrowd.Cols.ALL_CLOS, selection,
				selectionArgs, sortOrder);

		if (cursor == null || cursor.getCount() <= 0)
			return null;

		while (cursor.moveToNext()) {
			VMessageQualification qualification = extraMsgFromCursor(cursor);
			if (qualification != null)
				list.add(qualification);
		}
		cursor.close();
		return list;
	}

	/**
	 * according Cursor Object , extract VMessageQualification Object.
	 * 
	 * @param cursor
	 * @return
	 */
	public static VMessageQualification extraMsgFromCursor(Cursor cursor) {
		String xml = cursor.getString(cursor.getColumnIndex("CrowdXml"));
		if (TextUtils.isEmpty(xml)) {
			V2Log.e("MessageBuilder extraMsgFromCursor -->pase the CrowdXml failed.. XML is null");
			return null;
		}

		List<V2Group> parseCrowd = XmlAttributeExtractor.parseCrowd(xml);
		if(parseCrowd == null){
			V2Log.e("MessageBuilder extraMsgFromCursor -->pase the CrowdXml failed.. XML is : " + xml);
			return null;
		}
		
		V2Group v2Group = parseCrowd.get(0);
		if (v2Group == null || v2Group.creator == null) {
			V2Log.e("MessageBuilder extraMsgFromCursor --> pase the CrowdXml failed..v2Group or v2Group.createor is null");
			return null;
		}

		CrowdGroup group = null;

		long mid = cursor
				.getLong(cursor.getColumnIndex(HistoriesCrowd.Cols.ID));

		long crowdGroupID = cursor.getLong(cursor.getColumnIndex("CrowdID"));
		long saveDate = cursor.getLong(cursor.getColumnIndex("SaveDate"));
		int authType = cursor.getInt(cursor.getColumnIndex("CrowdAuthType"));
		int joinState = cursor.getInt(cursor.getColumnIndex("JoinState"));
		int readState = cursor.getInt(cursor.getColumnIndex("ReadState"));
		String applyReason = cursor.getString(cursor
				.getColumnIndex("ApplyReason"));
		String refuseReason = cursor.getString(cursor
				.getColumnIndex("RefuseReason"));
		group = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				crowdGroupID);
		if (group == null) {
			group = new CrowdGroup(crowdGroupID, v2Group.name, GlobalHolder
					.getInstance().getUser(v2Group.owner.uid), new Date());
			group.setBrief(v2Group.brief);
			group.setAnnouncement(v2Group.announce);
			group.setAuthType(CrowdGroup.AuthType.fromInt(authType));
		}

		long fromUserID = cursor.getLong(cursor.getColumnIndex("FromUserID"));
		long toUserID = cursor.getLong(cursor.getColumnIndex("ToUserID"));
		if (v2Group.creator.uid == fromUserID) {
			VMessageQualificationInvitationCrowd inviteCrowd = new VMessageQualificationInvitationCrowd(
					group, GlobalHolder.getInstance().getUser(toUserID));
			inviteCrowd.setRejectReason(refuseReason);
			inviteCrowd.setQualState(VMessageQualification.QualificationState
					.fromInt(joinState));
			inviteCrowd.setReadState(VMessageQualification.ReadState
					.fromInt(readState));
			inviteCrowd.setId(mid);
			inviteCrowd.setmTimestamp(new Date(saveDate));
			return inviteCrowd;
		} else {
			VMessageQualificationApplicationCrowd applyCrowd = new VMessageQualificationApplicationCrowd(
					group, GlobalHolder.getInstance().getUser(fromUserID));
			applyCrowd.setApplyReason(applyReason);
			applyCrowd.setRejectReason(refuseReason);
			applyCrowd.setQualState(VMessageQualification.QualificationState
					.fromInt(joinState));
			applyCrowd.setReadState(VMessageQualification.ReadState
					.fromInt(readState));
			applyCrowd.setId(mid);
			applyCrowd.setmTimestamp(new Date(saveDate));
			return applyCrowd;
		}
	}

	/**
	 * Delete a qualification message by VMessageQualification Object id
	 * 
	 * @param context
	 * @param id
	 * @return
	 */
	public static boolean deleteQualMessage(Context context, long id) {
		DataBaseContext mContext = new DataBaseContext(context);
		String where = ContentDescriptor.HistoriesCrowd.Cols.ID + " = ?";
		String[] selectionArgs = new String[] { String.valueOf(id) };
		int ret = mContext.getContentResolver().delete(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI, where,
				selectionArgs);
		if (ret >= 0)
			return true;
		else
			return false;
	}

	/**
	 * Delete a qualification message by User Object
	 * 
	 * @param context
	 * @param user
	 */
	public static void deleteQualMessage(Context context, User user) {
		if (user == null) {
			V2Log.e("To delete failed...please check the given user Object or type in the databases");
			return;
		}
		DataBaseContext mContext = new DataBaseContext(context);
		String where = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID
				+ " = ?";
		String[] selectionArgs = new String[] { String.valueOf(user
				.getmUserId()) };
		mContext.getContentResolver().delete(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI, where,
				selectionArgs);
	}
}
