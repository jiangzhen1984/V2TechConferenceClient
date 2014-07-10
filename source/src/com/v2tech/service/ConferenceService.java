package com.v2tech.service;

import java.util.List;

import android.os.Handler;
import android.os.Message;

import com.V2.jni.ConfRequest;
import com.V2.jni.ConfRequestCallback;
import com.V2.jni.GroupRequest;
import com.V2.jni.GroupRequestCallbackAdapter;
import com.V2.jni.VideoMixerRequest;
import com.V2.jni.VideoMixerRequestCallback;
import com.V2.jni.VideoRequest;
import com.V2.jni.VideoRequestCallbackAdapter;
import com.v2tech.service.jni.JNIIndication;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.PermissionUpdateIndication;
import com.v2tech.service.jni.RequestCloseUserVideoDeviceResponse;
import com.v2tech.service.jni.RequestConfCreateResponse;
import com.v2tech.service.jni.RequestEnterConfResponse;
import com.v2tech.service.jni.RequestExitedConfResponse;
import com.v2tech.service.jni.RequestOpenUserVideoDeviceResponse;
import com.v2tech.service.jni.RequestPermissionResponse;
import com.v2tech.service.jni.RequestUpdateCameraParametersResponse;
import com.v2tech.util.V2Log;
import com.v2tech.vo.CameraConfiguration;
import com.v2tech.vo.Conference;
import com.v2tech.vo.ConferenceGroup;
import com.v2tech.vo.ConferencePermission;
import com.v2tech.vo.Group;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.MixVideo;
import com.v2tech.vo.User;
import com.v2tech.vo.UserDeviceConfig;

/**
 * <ul>
 * This class is use to conference business.
 * </ul>
 * <ul>
 * When user entered conference room, user can use
 * {@link #requestOpenVideoDevice(Conference, UserDeviceConfig, Message)} and
 * {@link #requestCloseVideoDevice(Conference, UserDeviceConfig, Message)} to
 * open or close video include self.
 * </ul>
 * <ul>
 * <li>User request to enter conference :
 * {@link #requestEnterConference(Conference, Registrant)}</li>
 * <li>User request to exit conference :
 * {@link #requestExitConference(Conference, Registrant)}</li>
 * <li>User request to open video device :
 * {@link #requestOpenVideoDevice(Conference, UserDeviceConfig, Registrant)}</li>
 * <li>User request to close video device:
 * {@link #requestCloseVideoDevice(Conference, UserDeviceConfig, Registrant)}</li>
 * <li>User request to request speak in meeting
 * {@link #applyForControlPermission(ConferencePermission, Registrant)}</li>
 * <li>User request to release speaker in meeting
 * {@link #applyForReleasePermission(ConferencePermission, Registrant)}</li>
 * <li>User create conference: {@link #createConference(Conference, Registrant)}
 * </li>
 * </ul>
 * 
 * @author 28851274
 * 
 */
public class ConferenceService extends AbstractHandler {

	private static final int JNI_REQUEST_ENTER_CONF = 1;
	private static final int JNI_REQUEST_EXIT_CONF = 2;
	private static final int JNI_REQUEST_OPEN_VIDEO = 3;
	private static final int JNI_REQUEST_CLOSE_VIDEO = 4;
	private static final int JNI_REQUEST_SPEAK = 5;
	private static final int JNI_REQUEST_RELEASE_SPEAK = 6;
	private static final int JNI_REQUEST_CREATE_CONFERENCE = 7;
	private static final int JNI_REQUEST_QUIT_CONFERENCE = 8;
	private static final int JNI_REQUEST_INVITE_ATTENDEES = 9;

	private static final int JNI_UPDATE_CAMERA_PAR = 75;

	private static final int KEY_KICKED_LISTNER = 100;
	private static final int KEY_ATTENDEE_DEVICE_LISTNER = 101;
	private static final int KEY_ATTENDEE_STATUS_LISTNER = 102;
	private static final int KEY_SYNC_LISTNER = 103;
	private static final int KEY_PERMISSION_CHANGED_LISTNER = 104;
	private static final int KEY_MIXED_VIDEO_LISTNER = 105;

	private VideoRequestCB videoCallback;
	private ConfRequestCB confCallback;
	private GroupRequestCB groupCallback;
	private MixerRequestCB mrCallback;

	public ConferenceService() {
		super();
		videoCallback = new VideoRequestCB(this);
		VideoRequest.getInstance().addCallback(videoCallback);
		confCallback = new ConfRequestCB(this);
		ConfRequest.getInstance().addCallback(confCallback);
		groupCallback = new GroupRequestCB(this);
		GroupRequest.getInstance().addCallback(groupCallback);
		mrCallback = new MixerRequestCB(this);
		VideoMixerRequest.getInstance().addCallbacks(mrCallback);

	}

	/**
	 * User request to enter conference.<br>
	 * 
	 * @param conf
	 *            {@link Conference} object which user wants to enter
	 * @param caller
	 *            if input is null, ignore response. Message.object is
	 *            {@link com.v2tech.service.jni.RequestEnterConfResponse}
	 * 
	 * @see com.v2tech.service.jni.RequestEnterConfResponse
	 */
	public void requestEnterConference(Conference conf, Registrant caller) {
		initTimeoutMessage(JNI_REQUEST_ENTER_CONF, DEFAULT_TIME_OUT_SECS,
				caller);
		ConfRequest.getInstance().enterConf(conf.getId());
	}

	/**
	 * User request to quit conference. This API just use to for quit conference
	 * this time.<br>
	 * User will receive this conference when log in next time.
	 * 
	 * @param conf
	 *            {@link Conference} object which user wants to enter
	 * @param msg
	 *            if input is null, ignore response Message. Response Message
	 *            object is
	 *            {@link com.v2tech.service.jni.RequestExitedConfResponse}
	 */
	public void requestExitConference(Conference conf, Registrant caller) {
		initTimeoutMessage(JNI_REQUEST_EXIT_CONF, DEFAULT_TIME_OUT_SECS, caller);
		ConfRequest.getInstance().exitConf(conf.getId());
		// send response to caller because exitConf no call back from JNI
		JNIResponse jniRes = new RequestExitedConfResponse(conf.getId(),
				System.currentTimeMillis() / 1000, JNIResponse.Result.SUCCESS);
		Message res = Message.obtain(this, JNI_REQUEST_EXIT_CONF, jniRes);
		// send delayed message for that make sure send response after JNI
		// request
		this.sendMessageDelayed(res, 300);
	}

	/**
	 * Create conference.
	 * <ul>
	 * </ul>
	 * 
	 * @param conf
	 *            {@link Conference} object.
	 * @param caller
	 *            if input is null, ignore response Message. Response Message
	 *            object is
	 *            {@link com.v2tech.service.jni.RequestConfCreateResponse}
	 */
	public void createConference(Conference conf, Registrant caller) {
		if (conf == null) {
			if (caller != null && caller.getHandler() != null) {
				JNIResponse jniRes = new RequestConfCreateResponse(0, 0,
						RequestConfCreateResponse.Result.FAILED);
				sendResult(caller, jniRes);
			}
			return;
		}
		initTimeoutMessage(JNI_REQUEST_CREATE_CONFERENCE,
				DEFAULT_TIME_OUT_SECS, caller);
		GroupRequest.getInstance().createGroup(
				Group.GroupType.CONFERENCE.intValue(),
				conf.getConferenceConfigXml(), conf.getInvitedAttendeesXml());
	}

	/**
	 * User request to quit this conference for ever.<br>
	 * User never receive this conference information any more.
	 * 
	 * @param conf
	 * @param caller
	 */
	public void quitConference(Conference conf, Registrant caller) {
		if (conf == null) {
			if (caller != null) {
				JNIResponse jniRes = new RequestConfCreateResponse(0, 0,
						RequestConfCreateResponse.Result.INCORRECT_PAR);
				sendResult(caller, jniRes);
			}
			return;
		}
		initTimeoutMessage(JNI_REQUEST_QUIT_CONFERENCE, DEFAULT_TIME_OUT_SECS,
				caller);
		// If conference owner is self, then delete group
		if (conf.getCreator() == GlobalHolder.getInstance().getCurrentUserId()) {
			GroupRequest.getInstance().delGroup(
					Group.GroupType.CONFERENCE.intValue(), conf.getId());
			// If conference owner isn't self, just leave group
		} else {
			GroupRequest.getInstance().leaveGroup(
					Group.GroupType.CONFERENCE.intValue(), conf.getId());
		}
	}

	/**
	 * Chair man invite extra attendee to join current conference.<br>
	 * 
	 * @param conf
	 *            conference which user current joined
	 * @param list
	 *            additional attendee
	 * @param caller
	 *            caller
	 */
	public void inviteAttendee(Conference conf, List<User> list,
			Registrant caller) {
		if (list == null || conf == null || list.isEmpty()) {
			if (caller != null) {
				JNIResponse jniRes = new JNIResponse(
						JNIResponse.Result.INCORRECT_PAR);
				sendResult(caller, jniRes);
			}
			return;
		}
		StringBuffer attendees = new StringBuffer();
		attendees.append("<userlist> ");
		for (User at : list) {
			attendees.append(" <user id='" + at.getmUserId() + " ' />");
		}
		attendees.append("</userlist>");
		GroupRequest.getInstance().inviteJoinGroup(
				GroupType.CONFERENCE.intValue(), conf.getConferenceConfigXml(),
				attendees.toString(), "");

		// send response to caller because invite attendee no call back from JNI
		JNIResponse jniRes = new JNIResponse(JNIResponse.Result.SUCCESS);
		Message res = Message
				.obtain(this, JNI_REQUEST_INVITE_ATTENDEES, jniRes);
		// send delayed message for that make sure send response after JNI
		// request
		this.sendMessageDelayed(res, 300);
	}

	/**
	 * User request to open video device.
	 * 
	 * @param conf
	 *            {@link Conference} object which user entered
	 * @param userDevice
	 *            {@link UserDeviceConfig} if want to open local video,
	 *            {@link UserDeviceConfig#getVp()} should be null and
	 *            {@link UserDeviceConfig#getDeviceID()} should be ""
	 * @param caller
	 *            if input is null, ignore response Message.object is
	 *            {@link com.v2tech.service.jni.RequestOpenUserVideoDeviceResponse}
	 * 
	 * @see UserDeviceConfig
	 */
	public void requestOpenVideoDevice(Conference conf,
			UserDeviceConfig userDevice, Registrant caller) {
		if (conf == null || userDevice == null) {
			if (caller != null) {
				JNIResponse jniRes = new RequestOpenUserVideoDeviceResponse(0,
						0, RequestConfCreateResponse.Result.INCORRECT_PAR);
				sendResult(caller, jniRes);
			}
			return;
		}
		initTimeoutMessage(JNI_REQUEST_OPEN_VIDEO, DEFAULT_TIME_OUT_SECS,
				caller);
		V2Log.i(" request open video group:" + conf.getId() + "   UID:"
				+ userDevice.getUserID() + " deviceid:"
				+ userDevice.getDeviceID() + "   videoplayer:"
				+ userDevice.getVp());
		VideoRequest.getInstance().openVideoDevice(
				userDevice.getType().ordinal(), userDevice.getUserID(),
				userDevice.getDeviceID(), userDevice.getVp(),
				userDevice.getBusinessType());
		JNIResponse jniRes = new RequestOpenUserVideoDeviceResponse(
				conf.getId(), System.currentTimeMillis() / 1000,
				RequestOpenUserVideoDeviceResponse.Result.SUCCESS);

		// send delayed message for that make sure send response after JNI
		Message res = Message.obtain(this, JNI_REQUEST_OPEN_VIDEO, jniRes);
		this.sendMessageDelayed(res, 300);

	}

	/**
	 * User request to close video device.
	 * 
	 * @param nGroupID
	 * @param userDevice
	 *            {@link UserDeviceConfig} if want to open local video,
	 *            {@link UserDeviceConfig#getVp()} should be null and
	 *            {@link UserDeviceConfig#getDeviceID()} should be ""
	 * @param caller
	 *            if input is null, ignore response Message.object is
	 *            {@link com.v2tech.service.jni.RequestCloseUserVideoDeviceResponse}
	 * 
	 * @see UserDeviceConfig
	 */
	public void requestCloseVideoDevice(Conference conf,
			UserDeviceConfig userDevice, Registrant caller) {
		if (conf == null || userDevice == null) {
			if (caller != null) {
				JNIResponse jniRes = new RequestCloseUserVideoDeviceResponse(
						0,
						0,
						RequestCloseUserVideoDeviceResponse.Result.INCORRECT_PAR);
				sendResult(caller, jniRes);
			}
			return;
		}
		initTimeoutMessage(JNI_REQUEST_CLOSE_VIDEO, DEFAULT_TIME_OUT_SECS,
				caller);

		VideoRequest.getInstance().closeVideoDevice(
				userDevice.getType().ordinal(), userDevice.getUserID(),
				userDevice.getDeviceID(), userDevice.getVp(),
				userDevice.getBusinessType());
		JNIResponse jniRes = new RequestCloseUserVideoDeviceResponse(
				conf.getId(), System.currentTimeMillis() / 1000,
				RequestCloseUserVideoDeviceResponse.Result.SUCCESS);

		// send delayed message for that make sure send response after JNI
		Message res = Message.obtain(this, JNI_REQUEST_CLOSE_VIDEO, jniRes);
		this.sendMessageDelayed(res, 300);
	}

	/**
	 * User request speak permission on the conference.
	 * 
	 * @param type
	 *            speak type should be {@link ConferencePermission#SPEAKING}
	 * @param caller
	 *            if input is null, ignore response Message.object is
	 *            {@link com.v2tech.service.jni.RequestPermissionResponse}
	 * 
	 * @see ConferencePermission
	 */
	public void applyForControlPermission(ConferencePermission type,
			Registrant caller) {
		initTimeoutMessage(JNI_REQUEST_SPEAK, DEFAULT_TIME_OUT_SECS, caller);

		ConfRequest.getInstance().applyForControlPermission(type.intValue());

		JNIResponse jniRes = new RequestPermissionResponse(
				RequestPermissionResponse.Result.SUCCESS);

		// send delayed message for that make sure send response after JNI
		Message res = Message.obtain(this, JNI_REQUEST_SPEAK, jniRes);
		this.sendMessageDelayed(res, 300);
	}

	/**
	 * Request release permission on the conference.
	 * 
	 * @param type
	 *            speak type should be {@link ConferencePermission#SPEAKING}
	 * @param caller
	 *            if input is null, ignore response Message.object is
	 *            {@link com.v2tech.service.jni.RequestPermissionResponse}
	 * 
	 * @see ConferencePermission
	 */
	public void applyForReleasePermission(ConferencePermission type,
			Registrant caller) {

		initTimeoutMessage(JNI_REQUEST_RELEASE_SPEAK, DEFAULT_TIME_OUT_SECS,
				caller);

		ConfRequest.getInstance().releaseControlPermission(type.intValue());

		JNIResponse jniRes = new RequestPermissionResponse(
				RequestPermissionResponse.Result.SUCCESS);

		// send delayed message for that make sure send response after JNI
		Message res = Message.obtain(this, JNI_REQUEST_RELEASE_SPEAK, jniRes);
		this.sendMessageDelayed(res, 300);
	}

	/**
	 * Update current user's camera. Including front-side or back-side camera
	 * switch.
	 * 
	 * @param cc
	 *            {@link CameraConfiguration}
	 * @param caller
	 *            if input is null, ignore response Message.object is
	 *            {@link com.v2tech.service.jni.RequestUpdateCameraParametersResponse}
	 */
	public void updateCameraParameters(CameraConfiguration cc, Registrant caller) {
		initTimeoutMessage(JNI_UPDATE_CAMERA_PAR, DEFAULT_TIME_OUT_SECS, caller);
		VideoRequest.getInstance().setCapParam(cc.getDeviceId(),
				cc.getCameraIndex(), cc.getFrameRate(), cc.getBitRate());
	}


	/**
	 * Register listener for out conference by kick.
	 * 
	 * @param msg
	 */
	public void registerKickedConfListener(Handler h, int what, Object obj) {
		registerListener(KEY_KICKED_LISTNER, h, what, obj);
	}

	public void removeRegisterOfKickedConfListener(Handler h, int what,
			Object obj) {
		unRegisterListener(KEY_KICKED_LISTNER, h, what, obj);

	}

	// =============================
	/**
	 * Register listener for out conference by kick.
	 * 
	 * @param msg
	 */
	public void registerAttendeeDeviceListener(Handler h, int what, Object obj) {
		registerListener(KEY_ATTENDEE_DEVICE_LISTNER, h, what, obj);
	}

	public void removeAttendeeDeviceListener(Handler h, int what, Object obj) {
		unRegisterListener(KEY_ATTENDEE_DEVICE_LISTNER, h, what, obj);
	}

	/**
	 * Register listener for out conference by kick.
	 * 
	 * @param msg
	 */
	public void registerAttendeeListener(Handler h, int what, Object obj) {
		registerListener(KEY_ATTENDEE_STATUS_LISTNER, h, what, obj);
	}

	public void removeAttendeeListener(Handler h, int what, Object obj) {
		unRegisterListener(KEY_ATTENDEE_STATUS_LISTNER, h, what, obj);
	}

	/**
	 * Register listener for chairman control or release desktop
	 * 
	 * @param msg
	 */
	public void registerSyncDesktopListener(Handler h, int what, Object obj) {
		registerListener(KEY_SYNC_LISTNER, h, what, obj);
	}

	public void removeSyncDesktopListener(Handler h, int what, Object obj) {
		unRegisterListener(KEY_SYNC_LISTNER, h, what, obj);
	}

	/**
	 * Register listener for permission changed
	 * 
	 * @param msg
	 */
	public void registerPermissionUpdateListener(Handler h, int what, Object obj) {
		registerListener(KEY_PERMISSION_CHANGED_LISTNER, h, what, obj);
	}

	public void unRegisterPermissionUpdateListener(Handler h, int what,
			Object obj) {
		unRegisterListener(KEY_PERMISSION_CHANGED_LISTNER, h, what, obj);
	}

	public void registerVideoMixerListener(Handler h, int what, Object obj) {
		registerListener(KEY_MIXED_VIDEO_LISTNER, h, what, obj);
	}

	public void unRegisterVideoMixerListener(Handler h, int what, Object obj) {
		unRegisterListener(KEY_MIXED_VIDEO_LISTNER, h, what, obj);
	}

	class ConfRequestCB implements ConfRequestCallback {

		private Handler mCallbackHandler;

		public ConfRequestCB(Handler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnEnterConfCallback(long nConfID, long nTime,
				String szConfData, int nJoinResult) {
			ConferenceGroup cache = (ConferenceGroup) GlobalHolder
					.getInstance().findGroupById(nConfID);
			if (cache != null) {
				ConferenceGroup.extraAttrFromXml(cache, szConfData);
			}

			JNIResponse jniConfCreateRes = new RequestConfCreateResponse(
					nConfID, 0, RequestConfCreateResponse.Result.SUCCESS);
			Message.obtain(mCallbackHandler, JNI_REQUEST_CREATE_CONFERENCE,
					jniConfCreateRes).sendToTarget();

			JNIResponse jniRes = new RequestEnterConfResponse(
					nConfID,
					nTime,
					szConfData,
					nJoinResult == JNIResponse.Result.SUCCESS.value() ? JNIResponse.Result.SUCCESS
							: JNIResponse.Result.FAILED);
			Message.obtain(mCallbackHandler, JNI_REQUEST_ENTER_CONF, jniRes)
					.sendToTarget();
		}

		@Override
		public void OnConfMemberEnterCallback(long nConfID, long nTime,
				String szUserInfos) {
			int start = szUserInfos.indexOf("id='");
			if (start != -1) {
				int end = szUserInfos.indexOf("'", start + 4);
				if (end != -1) {
					String id = szUserInfos.substring(start + 4, end);
					final User u = GlobalHolder.getInstance().getUser(
							Long.parseLong(id));
					if (u == null) {
						V2Log.e(" Can't not find user " + id);
						return;
					}
	
					notifyListenerWithPending(KEY_ATTENDEE_STATUS_LISTNER, 1, 0, u);


				} else {
					V2Log.e("Invalid attendee user id ignore callback message");
				}
			} else {
				V2Log.e("Invalid attendee user id ignore callback message");
			}
		}

		@Override
		public void OnConfMemberExitCallback(long nConfID, long nTime,
				long nUserID) {

			User u = GlobalHolder.getInstance().getUser(nUserID);
			notifyListenerWithPending(KEY_ATTENDEE_STATUS_LISTNER, 0, 0, u);

		}

		@Override
		public void OnKickConfCallback(int nReason) {
			notifyListenerWithPending(KEY_KICKED_LISTNER, nReason, 0, null);
		}

		@Override
		public void OnGrantPermissionCallback(long userid, int type, int status) {
			JNIIndication jniInd = new PermissionUpdateIndication(userid, type,
					status);
			notifyListenerWithPending(KEY_PERMISSION_CHANGED_LISTNER, 0, 0, jniInd);
		}

		@Override
		public void OnConfNotify(String confXml, String creatorXml) {

		}

	}

	class VideoRequestCB extends VideoRequestCallbackAdapter {

		private Handler mCallbackHandler;

		public VideoRequestCB(Handler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnRemoteUserVideoDevice(String szXmlData) {
			if (szXmlData == null) {
				V2Log.e(" No avaiable user device configuration");
				return;
			}
			List<UserDeviceConfig> ll = UserDeviceConfig
					.parseFromXml(szXmlData);
			GlobalHolder.getInstance().addAttendeeDevice(ll);

			notifyListenerWithPending(KEY_ATTENDEE_DEVICE_LISTNER, 0, 0, ll);
		}

		

		@Override
		public void OnSetCapParamDone(String szDevID, int nSizeIndex,
				int nFrameRate, int nBitRate) {
			JNIResponse jniRes = new RequestUpdateCameraParametersResponse(
					new CameraConfiguration(szDevID, 1, nFrameRate, nBitRate),
					RequestUpdateCameraParametersResponse.Result.SUCCESS);
			Message.obtain(mCallbackHandler, JNI_UPDATE_CAMERA_PAR, jniRes)
					.sendToTarget();

		}

		

	}

	class GroupRequestCB extends GroupRequestCallbackAdapter {

		private Handler mCallbackHandler;

		public GroupRequestCB(Handler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}



		@Override
		public void OnModifyGroupInfoCallback(int groupType, long nGroupID,
				String sXml) {
			if (groupType == Group.GroupType.CONFERENCE.intValue()) {
				ConferenceGroup cache = (ConferenceGroup) GlobalHolder
						.getInstance().findGroupById(nGroupID);

				// if doesn't find matched group, mean this is new group
				if (cache == null) {

				} else {
					int flag = ConferenceGroup.extraAttrFromXml(cache, sXml);

					if ((flag & ConferenceGroup.EXTRA_FLAG_SYNC) == ConferenceGroup.EXTRA_FLAG_SYNC) {
						notifyListenerWithPending(KEY_SYNC_LISTNER,
								(cache.isSyn() ? 1 : 0), 0, null);
					}

				}

			}
		}


	}

	class MixerRequestCB implements VideoMixerRequestCallback {

		private Handler mCallbackHandler;

		public MixerRequestCB(Handler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnCreateVideoMixerCallback(String sMediaId, int layout,
				int width, int height) {
			if (sMediaId == null || sMediaId.isEmpty()) {
				V2Log.e(" OnCreateVideoMixerCallback -- > unlmatform parameter sMediaId is null ");
				return;
			}
			notifyListenerWithPending(KEY_MIXED_VIDEO_LISTNER, 1, 0, new MixVideo(
					sMediaId, MixVideo.LayoutType.fromInt(layout), width,
					height));
		}

		@Override
		public void OnDestroyVideoMixerCallback(String sMediaId) {
			notifyListenerWithPending(KEY_MIXED_VIDEO_LISTNER, 2, 0, new MixVideo(
					sMediaId, MixVideo.LayoutType.UNKOWN));
		}

		@Override
		public void OnAddVideoMixerCallback(String sMediaId, long nDstUserId,
				String sDstDevId, int pos) {
			UserDeviceConfig udc = new UserDeviceConfig(nDstUserId, sDstDevId,
					null);
			MixVideo mix = new MixVideo(sMediaId);
			notifyListenerWithPending(KEY_MIXED_VIDEO_LISTNER, 3, 0,
					mix.createMixVideoDevice(pos, sMediaId, udc));
		}

		@Override
		public void OnDelVideoMixerCallback(String sMediaId, long nDstUserId,
				String sDstDevId) {
			UserDeviceConfig udc = new UserDeviceConfig(nDstUserId, sDstDevId,
					null);
			MixVideo mix = new MixVideo(sMediaId);
			notifyListenerWithPending(KEY_MIXED_VIDEO_LISTNER, 4, 0,
					mix.createMixVideoDevice(-1, sMediaId, udc));

		}

	}

}
