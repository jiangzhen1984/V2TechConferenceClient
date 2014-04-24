package v2av;

import java.util.ArrayList;

import v2av.VideoCaptureDevInfo.VideoCaptureDevice;

public class VideoDeviceAndroid
{
	private VideoCaptureDevInfo mCapDevInfo = VideoCaptureDevInfo.CreateVideoCaptureDevInfo();
	
	private String GetVideoDevInfo()
	{
		if (mCapDevInfo == null) {
			mCapDevInfo = VideoCaptureDevInfo.CreateVideoCaptureDevInfo();
		}
		if (mCapDevInfo.deviceList == null) {
			mCapDevInfo.deviceList =  new ArrayList<VideoCaptureDevice>();
		}
		int devNum = mCapDevInfo.deviceList.size();
		
		StringBuilder sb = new StringBuilder();
		sb.append("<devicelist>");
		
		for (int i = 0; i < devNum; i++)
		{
			VideoCaptureDevice dev = mCapDevInfo.deviceList.get(i);
			
			sb.append("<device devName='");
			sb.append(dev.deviceUniqueName);
			sb.append("' fps='");
			sb.append(dev.strFPSs);
			sb.append("'>");
			
			int capabiliesLen = dev.captureCapabilies.length;
			for (int j = 0; j < capabiliesLen; j++)
			{
				CaptureCapability capab = dev.captureCapabilies[j];
				sb.append("<size width='");
				sb.append(capab.width);
				sb.append("' height='");
				sb.append(capab.height);
				sb.append("'>");
				sb.append("</size>");
			}
			
			sb.append("</device>");
		}
		
		sb.append("</devicelist>");
		
		return sb.toString();
	}
}
