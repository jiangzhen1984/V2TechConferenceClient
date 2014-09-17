package com.v2tech.view.conversation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.V2.jni.ind.V2Group;
import com.V2.jni.ind.V2User;
import com.V2.jni.util.V2Log;
import com.V2.jni.util.XmlAttributeExtractor;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.db.DataBaseContext;
import com.v2tech.service.GlobalHolder;
import com.v2tech.util.GlobalConfig;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.User;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageAudioItem;
import com.v2tech.vo.VMessageFileItem;
import com.v2tech.vo.VMessageImageItem;
import com.v2tech.vo.VMessageQualification;
import com.v2tech.vo.VMessageQualificationCrowd;
import com.v2tech.vo.VMessageTextItem;
import com.v2tech.vo.VideoBean;

public class MessageBuilder {

	private static final int MESSAGE_TYPE_IMAGE = 0;
	private static final int MESSAGE_TYPE_AUDIO = 1;
	private static final int MESSAGE_TYPE_FILE = 2;

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
		File newFile = copyBinaryData(MESSAGE_TYPE_IMAGE, imagePath);
		if (newFile == null)
			return null;
		imagePath = newFile.getAbsolutePath();
		VMessage vm = new VMessage(groupType, groupID, fromUser, toUser,
				new Date(GlobalConfig.getGlobalServerTime()));
		VMessageImageItem item = new VMessageImageItem(vm, imagePath);
		return item.getVm();
	}

	public static VMessage buildAudioMessage(int groupType, long groupID,
			User fromUser, User toUser, String audioPath, int seconds) {
		File newFile = copyBinaryData(MESSAGE_TYPE_AUDIO, audioPath);
		if (newFile == null)
			return null;
		audioPath = newFile.getAbsolutePath();
		VMessage vm = new VMessage(groupType, groupID, fromUser, toUser,
				new Date(GlobalConfig.getGlobalServerTime()));
		VMessageAudioItem item = new VMessageAudioItem(vm, audioPath, seconds);
		return item.getVm();
	}

	public static VMessage buildFileMessage(int groupType, long groupID,
			User fromUser, User toUser, String filePath, int fileType) {
		File newFile = copyBinaryData(MESSAGE_TYPE_FILE, filePath);
		if (newFile == null)
			return null;
		filePath = newFile.getAbsolutePath();
		VMessage vm = new VMessage(groupType, groupID, fromUser, toUser,
				new Date(GlobalConfig.getGlobalServerTime()));
		new VMessageFileItem(vm, filePath, fileType);
		return vm;
	}

	/**
	 * put the chat datas VMessage Object to DataBases
	 * 
	 * @param context
	 * @param vm
	 * @return
	 */
	public static Uri saveMessage(Context context, VMessage vm) {
		if (vm == null || vm.getFromUser() == null || vm.getToUser() == null) {
			return null;
		}

		DataBaseContext mContext = new DataBaseContext(context);
		int type = 0;
		if (vm.getMsgCode() == 0 && vm.getGroupId() == 0l)
			type = MessageLoader.CONTACT_TYPE;
		else
			type = MessageLoader.CROWD_TYPE;

		long remoteID = 0;
		if (vm.getFromUser().getmUserId() == vm.getFromUser().getmUserId())
			remoteID = vm.getToUser().getmUserId();
		else
			remoteID = vm.getFromUser().getmUserId();

		if (!MessageLoader.init(context, 0, 0, remoteID, type))
			return null;

		// 确定远程用户
		long remote = -1;
		if (vm.getFromUser().getmUserId() == GlobalHolder.getInstance()
				.getCurrentUserId())
			remote = vm.getToUser().getmUserId();
		else
			remote = vm.getFromUser().getmUserId();
		// 判断数据库是否存在
		long groupType = vm.getMsgCode();
		long groupID = vm.getGroupId();
		if (!MessageLoader.init(context, groupType, groupID, remote,
				remote == 0 ? MessageLoader.CROWD_TYPE
						: MessageLoader.CONTACT_TYPE))
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
			values.put(
					ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_REMOTE_USER_ID,
					vm.getToUser().getmUserId());
		}
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
		if (vm == null || vm.getFromUser() == null || vm.getToUser() == null)
			return null;

		Uri uri = null;
		// 确定远程用户
		long remote = -1;
		if (vm.getFromUser().getmUserId() == GlobalHolder.getInstance()
				.getCurrentUserId())
			remote = vm.getToUser().getmUserId();
		else
			remote = vm.getFromUser().getmUserId();

		long groupType = vm.getMsgCode();
		long groupID = vm.getGroupId();
		if (!MessageLoader.init(context, groupType, groupID, remote,
				remote == 0 ? MessageLoader.CROWD_TYPE
						: MessageLoader.CONTACT_TYPE))
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
				values.put(
						ContentDescriptor.HistoriesMessage.Cols.HISTORY_MESSAGE_REMOTE_USER_ID,
						vm.getToUser().getmUserId());
			}
			values.put(
					ContentDescriptor.HistoriesGraphic.Cols.HISTORY_GRAPHIC_ID,
					vm.getUUID());
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
				values.put(
						ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_REMOTE_USER_ID,
						vm.getToUser().getmUserId());
			}
			values.put(ContentDescriptor.HistoriesAudios.Cols.HISTORY_AUDIO_ID,
					vm.getUUID());
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
		if (vm == null || vm.getFromUser() == null || vm.getToUser() == null
				|| vm.getFileItems().size() <= 0)
			return null;

		// 确定远程用户
		long remote = -1;
		if (vm.getFromUser().getmUserId() == GlobalHolder.getInstance()
				.getCurrentUserId())
			remote = vm.getToUser().getmUserId();
		else
			remote = vm.getFromUser().getmUserId();

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
			values.put(
					ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_TO_USER_ID,
					vm.getToUser().getmUserId());
			values.put(
					ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_REMOTE_USER_ID,
					remote);
			values.put(ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_ID,
					vm.getUUID());
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
	 * @param vm
	 * @return
	 */
	public static Uri saveMediaChatHistories(Context context, VideoBean bean) {

		DataBaseContext mContext = new DataBaseContext(context);
		if (bean == null || bean.formUserID == -1 || bean.toUserID == -1)
			return null;

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
			VMessageAbstractItem item) {
		DataBaseContext mContext = new DataBaseContext(context);
		ContentValues itemVal = new ContentValues();
		int updates = 0;
		switch (item.getType()) {
		case VMessageAbstractItem.ITEM_TYPE_FILE:
			VMessageFileItem fileItem = (VMessageFileItem) item;
			itemVal.put(
					ContentDescriptor.HistoriesFiles.Cols.HISTORY_FILE_SEND_STATE,
					VMessageAbstractItem.STATE_SENT_FALIED);
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

	/**
	 * 将二进制数据拷贝到用户自身目录下存储
	 * 
	 * @param filePath
	 * @return
	 */
	private static File copyBinaryData(int type, String filePath) {

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
			User user = GlobalHolder.getInstance().getCurrentUser();
			switch (type) {
			case MESSAGE_TYPE_IMAGE:
				desFile = new File(GlobalConfig.getGlobalPicsPath(user) + "/"
						+ fileName);
				break;
			case MESSAGE_TYPE_AUDIO:
				desFile = new File(GlobalConfig.getGlobalAudioPath(user) + "/"
						+ fileName);
				break;
			case MESSAGE_TYPE_FILE:
				desFile = new File(GlobalConfig.getGlobalFilePath(user) + "/"
						+ fileName);
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
	public static boolean saveQualicationMessage(Context context,
			VMessageQualification msg) {
		DataBaseContext mContext = new DataBaseContext(context);
		if (msg == null || msg.getInvitationUser().getmUserId() == -1
				|| msg.getBeInvitatonUser().getmUserId() == -1) {
			V2Log.e("To store failed...please check the given VMessageQualification Object in the databases");
			return false;
		}
		// 确定远程用户
		long remote = -1;
		if (msg.getInvitationUser().getmUserId() == GlobalHolder.getInstance()
				.getCurrentUserId())
			remote = msg.getBeInvitatonUser().getmUserId();
		else
			remote = msg.getInvitationUser().getmUserId();
		ContentValues values = new ContentValues();
		switch (msg.getType()) {
		case CONTACT:
			break;
		case CROWD:
			VMessageQualificationCrowd crowdMsg = (VMessageQualificationCrowd) msg;
			values.put(ContentDescriptor.HistoriesCrowd.Cols.OWNER_USER_ID,
					GlobalHolder.getInstance().getCurrentUserId());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_SAVEDATE,
					GlobalConfig.getGlobalServerTime());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_FROM_USER_ID,
					crowdMsg.getInvitationUser().getmUserId());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_TO_USER_ID,
					crowdMsg.getBeInvitatonUser().getmUserId());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID,
					remote);
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_APPLY_REASON,
					crowdMsg.getApplyReason());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REFUSE_REASON,
					crowdMsg.getRejectReason());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
					crowdMsg.getReadState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_STATE,
					crowdMsg.getQualState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_AUTHTYPE,
					crowdMsg.getmCrowdGroup().getAuthType().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_BASE_INFO,
					crowdMsg.getmCrowdGroup().toXml());
			mContext.getContentResolver().insert(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI, values);
			return true;
		}
		return false;
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
		DataBaseContext mContext = new DataBaseContext(context);
		if (msg == null || msg.getInvitationUser().getmUserId() == -1
				|| msg.getBeInvitatonUser().getmUserId() == -1) {
			V2Log.e("To update failed...please check the given VMessageQualification Object in the databases");
			return -1;
		}
		ContentValues values = new ContentValues();
		switch (msg.getType()) {
		case CONTACT:
			break;
		case CROWD:
			VMessageQualificationCrowd crowdMsg = (VMessageQualificationCrowd) msg;
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_READ_STATE,
					crowdMsg.getReadState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_STATE,
					crowdMsg.getQualState().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_AUTHTYPE,
					crowdMsg.getmCrowdGroup().getAuthType().intValue());
			values.put(
					ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_BASE_INFO,
					crowdMsg.getmCrowdGroup().toXml());
			String where = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_ID
					+ " = ?";
			String[] selectionArgs = new String[] { String.valueOf(crowdMsg
					.getmCrowdGroup().getmGId()) };
			int updates = mContext.getContentResolver().update(
					ContentDescriptor.HistoriesCrowd.CONTENT_URI, values,
					where, selectionArgs);
			return updates;
		}
		return -1;
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
		VMessageQualificationCrowd crowd = null;
		CrowdGroup group = null;
		while (cursor.moveToNext()) {

			long crowdGroupID = cursor
					.getLong(cursor.getColumnIndex("CrowdID"));
			long fromUserID = cursor.getLong(cursor
					.getColumnIndex("FromUserID"));
			long toUserID = cursor.getLong(cursor.getColumnIndex("ToUserID"));
			long saveDate = cursor.getLong(cursor.getColumnIndex("SaveDate"));
			int authType = cursor
					.getInt(cursor.getColumnIndex("CrowdAuthType"));
			int joinState = cursor.getInt(cursor.getColumnIndex("JoinState"));
			int readState = cursor.getInt(cursor.getColumnIndex("ReadState"));
			String applyReason = cursor.getString(cursor
					.getColumnIndex("ApplyReason"));
			String refuseReason = cursor.getString(cursor
					.getColumnIndex("RefuseReason"));
			String xml = cursor.getString(cursor.getColumnIndex("CrowdXml"));

			Document doc = XmlAttributeExtractor.buildDocument(xml);
			NodeList crowdList = doc.getElementsByTagName("crowd");
			Element crowdElement = (Element) crowdList.item(0);
			String name = crowdElement.getAttribute("name");
			group = new CrowdGroup(crowdGroupID, name, GlobalHolder
					.getInstance().getCurrentUser(), new Date(saveDate));
			group.setBrief(crowdElement.getAttribute("summary"));
			group.setAnnouncement(crowdElement.getAttribute("announcement"));
			group.setAuthType(CrowdGroup.AuthType.fromInt(authType));
			crowd = new VMessageQualificationCrowd(group, GlobalHolder
					.getInstance().getUser(fromUserID), GlobalHolder
					.getInstance().getUser(toUserID));
			crowd.setApplyReason(applyReason);
			crowd.setRejectReason(refuseReason);
			crowd.setQualState(VMessageQualification.QualificationState
					.fromInt(joinState));
			crowd.setReadState(VMessageQualification.ReadState
					.fromInt(readState));
			list.add(crowd);
		}
		cursor.close();
		return list;
	}

	/**
	 * Delete a qualification message
	 * 
	 * @param context
	 * @param user
	 * @param type
	 */
	public static void deleteQualMessage(Context context, User user,
			VMessageQualification.Type type) {
		DataBaseContext mContext = new DataBaseContext(context);
		if (user == null || type == null) {
			V2Log.e("To delete failed...please check the given user Object or type in the databases");
			return ;
		}
		
		String where = ContentDescriptor.HistoriesCrowd.Cols.HISTORY_CROWD_REMOTE_USER_ID
				+ " = ?";
		String[] selectionArgs = new String[] { String.valueOf(user.getmUserId())};
		mContext.getContentResolver().delete(
				ContentDescriptor.HistoriesCrowd.CONTENT_URI, 
				where, selectionArgs);
	}
}
