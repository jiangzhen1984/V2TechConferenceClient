package com.v2tech.service;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Message;

import com.V2.jni.ConfRequest;
import com.V2.jni.ConfRequestCallback;
import com.V2.jni.GroupRequest;
import com.V2.jni.GroupRequestCallback;
import com.V2.jni.VideoRequest;
import com.V2.jni.VideoRequestCallback;
import com.v2tech.service.jni.JNIResponse;
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
import com.v2tech.vo.ConferencePermission;
import com.v2tech.vo.Group;
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
 * {@link #requestEnterConference(Conference, Message)}</li>
 * <li>User request to exit conference :
 * {@link #requestExitConference(Conference, Message)}</li>
 * <li>User request to open video device :
 * {@link #requestOpenVideoDevice(Conference, UserDeviceConfig, Message)}</li>
 * <li>User request to close video device:
 * {@link #requestCloseVideoDevice(Conference, UserDeviceConfig, Message)}</li>
 * <li>User request to request speak in meeting
 * {@link #applyForControlPermission(ConferencePermission, Message)}</li>
 * <li>User request to release speaker in meeting
 * {@link #applyForReleasePermission(ConferencePermission, Message)}</li>
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

	private static final int JNI_UPDATE_CAMERA_PAR = 75;

	private VideoRequestCB videoCallback;
	private ConfRequestCB confCallback;
	private GroupRequestCB groupCallback;

	public ConferenceService() {
		super();
		videoCallback = new VideoRequestCB(this);
		VideoRequest.getInstance().addCallback(videoCallback);
		confCallback = new ConfRequestCB(this);
		ConfRequest.getInstance().addCallback(confCallback);
		groupCallback = new GroupRequestCB(this);
		GroupRequest.getInstance().addCallback(groupCallback);

	}

	/**
	 * User request to enter conference.<br>
	 * 
	 * @param conf
	 *            {@link Conference} object which user wants to enter
	 * @param caller
	 *            if input is null, ignore response Message.object is
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
	 * User request to quit conference. This API just use to for quit conference this time.<br>
	 * User will receive this conference when log in next time.
	 * 
	 * @param conf
	 *            {@link Conference} object which user wants to enter
	 * @param msg
	 *            if input is null, ignore response Message. Response Message object is
	 *            {@link com.v2tech.service.jni.RequestExitedConfResponse}
	 */
	public void requestExitConference(Conference conf, Registrant caller) {
		initTimeoutMessage(JNI_REQUEST_EXIT_CONF, DEFAULT_TIME_OUT_SECS,
				caller);
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
	 * <ul></ul>
	 * 
	 * @param conf {@link Conference} object.
	 * @param caller   if input is null, ignore response Message. Response Message object is
	 *            {@link com.v2tech.service.jni.RequestConfCreateResponse}
	 */
	public void createConference(Conference conf, Registrant caller) {
		if (conf == null) {
			if (caller != null && caller.getHandler() != null) {
				JNIResponse jniRes = new RequestConfCreateResponse(
						0, 0,
						RequestConfCreateResponse.Result.FAILED);
				sendResult(caller, jniRes);
			}
			return;
		}
		initTimeoutMessage(JNI_REQUEST_CREATE_CONFERENCE, DEFAULT_TIME_OUT_SECS,
				caller);
		GroupRequest.getInstance().createGroup(
				Group.GroupType.CONFERENCE.intValue(),
				conf.getConferenceConfigXml(), conf.getInvitedAttendeesXml());
	}
	
	
	/**
	 * User request to quit this conference for ever.<br>
	 * User never receive this conference information any more.
	 * @param conf
	 * @param caller
	 */
	public void quitConference(Conference conf, Registrant caller) {
		if (conf == null) {
			if (caller != null) {
				JNIResponse jniRes = new RequestConfCreateResponse(
						0, 0,
						RequestConfCreateResponse.Result.FAILED);
				sendResult(caller, jniRes);
			}
			return;
		}
		initTimeoutMessage(JNI_REQUEST_QUIT_CONFERENCE, DEFAULT_TIME_OUT_SECS,
				caller);
		if (conf.getCreator() == GlobalHolder.getInstance().getCurrentUserId()) {
			GroupRequest.getInstance().delGroup(Group.GroupType.CONFERENCE.intValue(), conf.getId());
		} else {
			GroupRequest.getInstance().leaveGroup(Group.GroupType.CONFERENCE.intValue(), conf.getId());
		}
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
		initTimeoutMessage(JNI_REQUEST_OPEN_VIDEO, DEFAULT_TIME_OUT_SECS,
				caller);

		VideoRequest.getInstance().openVideoDevice(conf.getId(),
				userDevice.getUserID(), userDevice.getDeviceID(),
				userDevice.getVp(), userDevice.getBusinessType());
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

		initTimeoutMessage(JNI_REQUEST_CLOSE_VIDEO, 
				DEFAULT_TIME_OUT_SECS, caller);

		VideoRequest.getInstance().closeVideoDevice(conf.getId(),
				userDevice.getUserID(), userDevice.getDeviceID(),
				userDevice.getVp(), userDevice.getBusinessType());
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
		initTimeoutMessage(JNI_REQUEST_SPEAK, DEFAULT_TIME_OUT_SECS,
				caller);

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

		initTimeoutMessage(JNI_REQUEST_RELEASE_SPEAK, 
				DEFAULT_TIME_OUT_SECS, caller);

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
		initTimeoutMessage(JNI_UPDATE_CAMERA_PAR, DEFAULT_TIME_OUT_SECS,
				caller);
		VideoRequest.getInstance().setCapParam(cc.getDeviceId(),
				cc.getCameraIndex(), cc.getFrameRate(), cc.getBitRate());
	}
	
	
	
	private List<Registrant> registerList = new ArrayList<Registrant>();
	/**
	 * Register listener for out conference by kick.
	 * @param msg
	 */
	public void registerKickedConfListener(Handler h, int what, Object obj) {
		registerList.add(new Registrant(h, what, obj));
	}
	
	public void removeRegisterOfKickedConfListener(Handler h, int what, Object obj) {
		for (Registrant re : registerList) {
			if (re.getHandler() == h && what == re.getWhat()) {
				registerList.remove(re);
			}
		}
	}
	
	
	
	private List<Registrant> registerAttendeeStatusListenersList = new ArrayList<Registrant>();
	/**
	 * Register listener for out conference by kick.
	 * @param msg
	 */
	public void registerAttendeeListener(Handler h, int what, Object obj) {
		registerAttendeeStatusListenersList.add(new Registrant(h, what, obj));
	}
	
	public void removeAttendeeListener(Handler h, int what, Object obj) {
		for (Registrant re : registerAttendeeStatusListenersList) {
			if (re.getHandler() == h && what == re.getWhat()) {
				registerAttendeeStatusListenersList.remove(re);
			}
		}
	}


	class ConfRequestCB implements ConfRequestCallback {

		private Handler mCallbackHandler;

		public ConfRequestCB(Handler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnEnterConfCallback(long nConfID, long nTime,
				String szConfData, int nJoinResult) {
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
					User u = GlobalHolder.getInstance().getUser(Long.parseLong(id));
					for (Registrant re : registerAttendeeStatusListenersList) {
						Handler h = re.getHandler();
						if (h != null) {
							Message.obtain(h, re.getWhat(), 1, 0, u).sendToTarget();
						}
					}
					
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
			
			for (Registrant re : registerAttendeeStatusListenersList) {
				Handler h = re.getHandler();
				if (h != null) {
					Message.obtain(h, re.getWhat(),0, 0, u).sendToTarget();
				}
			}

		}

		@Override
		public void OnKickConfCallback(int nReason) {
			for (Registrant re : registerList) {
				Handler h = re.getHandler();
				if (h != null) {
					Message.obtain(h, re.getWhat(), nReason, 0, re.getObject()).sendToTarget();
				}
			}
		}
		
		

	}

	class VideoRequestCB implements VideoRequestCallback {

		private Handler mCallbackHandler;

		public VideoRequestCB(Handler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnRemoteUserVideoDevice(String szXmlData) {

		}

		@Override
		public void OnVideoChatInviteCallback(long nGroupID, int nBusinessType,
				long nFromUserID, String szDeviceID) {

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

	class GroupRequestCB implements GroupRequestCallback {

		private Handler mCallbackHandler;

		public GroupRequestCB(Handler mCallbackHandler) {
			this.mCallbackHandler = mCallbackHandler;
		}

		@Override
		public void OnGetGroupInfoCallback(int groupType, String sXml) {

		}

		@Override
		public void OnGetGroupUserInfoCallback(int groupType, long nGroupID,
				String sXml) {

		}

		@Override
		public void OnModifyGroupInfoCallback(int groupType, long nGroupID,
				String sXml) {
			if (groupType == Group.GroupType.CONFERENCE.intValue()) {
				List<Group> confList = GlobalHolder.getInstance().getGroup(
						Group.GroupType.CONFERENCE);
				boolean newGroupIdFlag = true;
				for (Group g : confList) {
					if (g.getmGId() == nGroupID) {
						newGroupIdFlag = false;
						break;
					}
				}
				// if doesn't find matched group, mean this is new group
				if (newGroupIdFlag) {
					JNIResponse jniRes = new RequestConfCreateResponse(
							nGroupID, 0,
							RequestConfCreateResponse.Result.SUCCESS);
					Message.obtain(mCallbackHandler,
							JNI_REQUEST_CREATE_CONFERENCE, jniRes)
							.sendToTarget();
				}
			}
		}

		@Override
		public void OnInviteJoinGroupCallback(int groupType, String groupInfo,
				String userInfo, String additInfo) {
			
		}

		@Override
		public void OnDelGroupCallback(int groupType, long nGroupID,
				boolean bMovetoRoot) {
			
		}
		
		
		
		
		
	}

}
