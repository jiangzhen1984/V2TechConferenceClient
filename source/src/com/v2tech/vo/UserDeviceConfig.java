package com.v2tech.vo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.v2tech.view.JNIService;

import android.view.SurfaceView;
import v2av.VideoPlayer;

/**
 * User video device configuration object.<br>
 * Local user video device is unnecessary VideoPlayer and device id information.<br>
 * If we want to open remote user's video, we need VidePlayer and device id
 * information. To get remote user's device id, we can listen
 * {@link com.V2.jni.VideoRequestCallback#OnRemoteUserVideoDevice(String)}. <br>
 * If object as local device, mVP is null and deviceID is "".
 * 
 * @see v2av.VideoPlayer com.V2.jni.VideoRequestCallback
 * @author jiangzhen
 * 
 */
public class UserDeviceConfig {

	public enum UserDeviceConfigType {
		UNKOWN, EVIDEODEVTYPE_VIDEO, // camera
		EVIDEODEVTYPE_CAMERA, //
		EVIDEODEVTYPE_FILE, // file source
		EVIDEODEVTYPE_VIDEOMIXER // mixed video
	}

	private long mUerID;
	private String mDeviceID;
	private VideoPlayer mVP;
	private int mBusinessType;
	private SurfaceView mSVHolder;
	private boolean isShowing;

	private Attendee mBelongsAttendee;

	private UserDeviceConfigType type;

	public UserDeviceConfig(long mUerID, String mDeviceID, VideoPlayer mVP,
			UserDeviceConfigType type) {
		this(mUerID, mDeviceID, mVP, 1);
		this.mUerID = mUerID;
		this.mDeviceID = mDeviceID;
		this.mVP = mVP;
		this.type = type;
	}

	public UserDeviceConfig(long mUerID, String mDeviceID, VideoPlayer mVP) {
		this(mUerID, mDeviceID, mVP, 1);
		this.mUerID = mUerID;
		this.mDeviceID = mDeviceID;
		this.mVP = mVP;
		this.type = UserDeviceConfigType.EVIDEODEVTYPE_VIDEO;
	}

	/**
	 * According to parameter construct new user device donfiguration object
	 * @param mUerID
	 * @param mDeviceID
	 * @param mVP
	 * @param mBusinessType
	 */
	public UserDeviceConfig(long mUerID, String mDeviceID, VideoPlayer mVP,
			int mBusinessType) {
		this.mUerID = mUerID;
		this.mDeviceID = mDeviceID;
		this.mVP = mVP;
		this.mBusinessType = mBusinessType;
	}

	/**
	 * Just clear all resources which this object holded. Notice: Before call
	 * this function, you must call
	 * {@link JNIService#requestCloseVideoDevice(long, UserDeviceConfig, android.os.Message)}
	 * first
	 */
	public void doClose() {
		this.isShowing = false;
		this.mSVHolder = null;
		this.mVP = null;
	}

	public long getUserID() {
		return mUerID;
	}

	public void setUserID(long userID) {
		this.mUerID = userID;
	}

	public String getDeviceID() {
		return mDeviceID;
	}

	public void setDeviceID(String deviceID) {
		this.mDeviceID = deviceID;
	}

	public VideoPlayer getVp() {
		return mVP;
	}

	public void setVp(VideoPlayer vp) {
		this.mVP = vp;
	}

	public int getBusinessType() {
		return mBusinessType;
	}

	public void setBusinessType(int businessType) {
		this.mBusinessType = businessType;
	}

	public SurfaceView getSVHolder() {
		return mSVHolder;
	}

	public void setSVHolder(SurfaceView sVHolder) {
		this.mSVHolder = sVHolder;
	}

	public boolean isShowing() {
		return isShowing;
	}

	public void setShowing(boolean isShowing) {
		this.isShowing = isShowing;
	}

	public Attendee getBelongsAttendee() {
		return mBelongsAttendee;
	}

	public void setBelongsAttendee(Attendee BelongsAttendee) {
		this.mBelongsAttendee = BelongsAttendee;
	}

	public UserDeviceConfigType getType() {
		return this.type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((mDeviceID == null) ? 0 : mDeviceID.hashCode());
		result = prime * result + (int) (mUerID ^ (mUerID >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserDeviceConfig other = (UserDeviceConfig) obj;
		if (mDeviceID == null) {
			if (other.mDeviceID != null)
				return false;
		} else if (!mDeviceID.equals(other.mDeviceID))
			return false;
		if (mUerID != other.mUerID)
			return false;
		return true;
	}

	/**
	 * <xml><videolist defaultid='15:9158Capture____0'><video bps='256'
	 * desc='9158Capture____0' fps='15' id='15:9158Capture____0' inuse='1'
	 * selectedindex='0' videotype='1'><sizelist/></video><video bps='256'
	 * desc='Logitech QuickCam Easy/Cool____3607289526' fps='15' id='15:Logitech
	 * QuickCam Easy/Cool____3607289526' inuse='0' selectedindex='0'
	 * videotype='1'><sizelist/></video><video bps='256' desc='Luminositi
	 * Softcam [VFW]____0' fps='15' id='15:Luminositi Softcam [VFW]____0'
	 * inuse='1' selectedindex='0'
	 * videotype='1'><sizelist/></video></videolist></xml>
	 * 
	 * @param xmlData
	 * @return
	 */
	public static List<UserDeviceConfig> parseFromXml(long uid, String xmlData) {
		if (xmlData == null) {
			throw new RuntimeException(" user video data is null");
		}
		List<UserDeviceConfig> l = new ArrayList<UserDeviceConfig>();

		InputStream is = null;

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			is = new ByteArrayInputStream(xmlData.getBytes("UTF-8"));
			Document doc = dBuilder.parse(is);

			doc.getDocumentElement().normalize();

			NodeList videoList = doc.getElementsByTagName("videolist");
			for (int j = 0; j < videoList.getLength(); j++) {
				Element videoListE = (Element) videoList.item(j);
				NodeList videol = videoListE.getElementsByTagName("video");
				for (int t = 0; t < videol.getLength(); t++) {
					Element video = (Element) videol.item(t);
					String deviceId = video.getAttribute("id");
					l.add(new UserDeviceConfig(uid, deviceId, null));
				}
			}

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return l;
	}

}
