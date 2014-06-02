package v2av;

import java.io.IOException;
import java.nio.ByteBuffer;


import v2av.VideoCaptureDevInfo.CapParams;
import v2av.VideoCaptureDevInfo.FrontFacingCameraType;
import v2av.VideoCaptureDevInfo.VideoCaptureDevice;

//import v2av.VideoRecordInfo.CameraID;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import android.view.SurfaceHolder;

public class VideoRecorder
{
	public static SurfaceHolder VideoPreviewSurfaceHolder = null;
	public static int DisplayRotation = 0;
	
	private int mSrcWidth;
	private int mSrcHeight;
	
	private int mVideoWidth;
	private int mVideoHeight;
	private int mBitrate;
	private int mFrameRate;
	private int mCameraRotation;
	private boolean mbMirror;
	private String prs;
	
	private VideoEncoder mEncoder = null;
	
	private Camera mCamera = null;
	private VideoCaptureDevInfo mCapDevInfo = null;
	
	private EncoderPreviewCallBack mVRCallback = null;
	
	VideoRecorder()
	{
		Log.e("VideoRecorder UI", "VideoRecorder constructor");
		mCapDevInfo = VideoCaptureDevInfo.CreateVideoCaptureDevInfo();
	}
	private int StartRecordVideo()
	{
		VideoCaptureDevice device = mCapDevInfo.GetDevice(mCapDevInfo.GetDefaultDevName());
    	if (device == null)
    	{
    		return -1;
    	}
    	
    	CapParams capParams = mCapDevInfo.GetCapParams();
    	mVideoWidth = capParams.width;
    	mVideoHeight = capParams.height;
    	mBitrate = capParams.bitrate;
    	mFrameRate = capParams.fps;
    	
    	VeritifyRecordInfo(device);
    	
    	switch (InitCamera(device))
		{
			case Err_CameraOpenError:
			{
				return -1;
			}
			default:
			{
				break;
			}
		}
		
    	StartPreview();
		
		return 0;
	}
	
	private int StopRecordVideo()
	{
		StopPreview();
		UninitCamera();
		
		return 0;
	}
	
	private int GetRecordWidth()
	{
		return mVideoWidth;
	}
	
	private int GetRecordHeight()
	{
		return mVideoHeight;
	}
	
	private int GetRecordBitrate()
	{
		return mBitrate;
	}
	
	private int GetRecordFPS()
	{
		return mFrameRate;
	}
	
	private int GetPreviewSize()
	{
		if(mCamera == null)
		{
			return -1;
		}
		Camera.Parameters para = mCamera.getParameters();
		Size s = para.getPreviewSize();
		
		mSrcWidth = s.width;
		mSrcHeight = s.height;
		
		return 0;
	}
	
	private int GetPreviewWidth()
	{
		return mSrcWidth;
	}
	
	private int GetPreviewHeight()
	{
		return mSrcHeight;
	}
	
	private int StartSend()
	{
		mVRCallback = new EncoderPreviewCallBack(this);

		if (StartRecord(mVRCallback) != true)
		{
			return -1;
		}
		
		return 0;
	}

	private int StopSend()
	{
		StopRecord();
		if (mVRCallback != null)
		{
			mVRCallback = null;
		}
		
		return 0;
	}
	
	private void VeritifyRecordInfo(VideoCaptureDevice device)
    {
    	boolean bSupport = false;
    	
    	CaptureCapability capbility = null;
    	for(int i = 0; i < device.captureCapabilies.length; ++i)
		{
    		capbility = device.captureCapabilies[i];
    		if (capbility.width == mVideoWidth 
    				&& capbility.height == mVideoHeight)
    		{
    			bSupport = true;
    			if (mFrameRate > capbility.maxFPS)
    			{
    				mFrameRate = capbility.maxFPS;
    			}
    			break;
    		}
		}
    	
    	if (!bSupport)
    	{ 		
    		mVideoWidth = 176;
    		mVideoHeight = 144;
    		if (mFrameRate > 15)
    		{
    			mFrameRate = 15;
    		}
    	}

    	if (device.frontCameraType == VideoCaptureDevInfo.FrontFacingCameraType.Android23)
		{
    		mbMirror = true;
		}
		else
		{
			mbMirror = false;
		}
    	mCameraRotation = device.orientation;
    }
	
	public void onGetVideoFrame(byte [] databuf, int len)
	{
		//��֡
		if(dropFrame()){
			
			return;
		}
		
		if (mEncoder != null)
		{
			mEncoder.encodeframe(databuf, len);
		}
	}
	
	//�������
	private int framecount=0; //���ڱ��֡��
	private boolean  dropFrame(){
		framecount++;
		int fps=10;
		boolean isdrop=false;
		if(mFrameRate==15){
			switch (fps) {
				case 5:
					if(framecount%3!=0){
						isdrop=true;
					}
					
					break;
				case 8:
					if(framecount%2!=0){
						isdrop=true;
					}
					
					break;
				case 10:
					if(framecount%3==0){
						isdrop=true;
					}
					break;
				case 15:
					break;
			}
		}else{
			switch (fps) {
			case 5:
				int devide_5=fps/5;
				if(framecount%devide_5!=0){
					isdrop=true;
				}
				
				break;
			case 8:
				int devide_8=fps/8;
				if(framecount%devide_8!=0){
					isdrop=true;
				}
				
				break;

			case 10:
				int devide_10=fps/10;
				if(framecount%devide_10!=0){
					isdrop=true;
				}
				break;
			case 15:
				int devide_15=fps/15;
				if(framecount%devide_15!=0){
					isdrop=true;
				}
				break;
			}
		}	
//		Logger.i(null,"��:"+isdrop);	
		return isdrop;	
	}
	
	private AVCode InitCamera(VideoCaptureDevice device)
	{
		Log.e("VideoRecorder UI", "InitCamera");
		if(mCamera != null)
		{
			return AVCode.Err_CameraAlreadyOpen;
		}
		
		if (Build.VERSION.SDK_INT  <= VERSION_CODES.GINGERBREAD)
		{
    		Log.e("VideoRecorder UI", "System Version Error");
    		return AVCode.Err_ErrorState;
		}
		else
		{
			if (!OpenCamera(device))
    		{
    			Log.e("VideoRecorder UI","OpenCamera failed!!!!!!!!!!!!!!!!!!!!!");
    		}
		}
		
	    if(mCamera == null)
	    {
	    	return AVCode.Err_CameraOpenError;
	    }
	    
	    Log.e("VideoRecorder UI", "InitCamera OK");
		
		return AVCode.Err_None;
	}
	
	private boolean OpenCamera(VideoCaptureDevice device)
	{
		try
		{
			mCamera = Camera.open(device.index);
		}
		catch (RuntimeException e)
		{
			e.printStackTrace();
		}
	    
	    if(mCamera == null)
	    {
	    	return false;
	    }

	    Camera.Parameters para = mCamera.getParameters();
		para.setPreviewSize(mVideoWidth, mVideoHeight);
		para.setPreviewFrameRate(mFrameRate);
//		para.setPreviewFrameRate(15);
		para.setPreviewFormat(ImageFormat.NV21);
		mCamera.setParameters(para);
		
		int result;
		if (device.frontCameraType == FrontFacingCameraType.Android23)
		{
			result = (device.orientation + DisplayRotation) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		}
		else
		{
			// back-facing
			result = (device.orientation - DisplayRotation + 360) % 360;
		}
		mCamera.setDisplayOrientation(result);

		return true;
	}
	
	private void UninitCamera()
	{
	   	if(mCamera != null)
		{
			try 
			{
				mCamera.setPreviewCallback(null);
			} 
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			try 
			{
				Thread.sleep(50);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
			mCamera.release();
			mCamera = null;
		}
	}
	
	private void StartPreview()
	{
		Log.e("VideoRecorder UI", "StartPreview");
		
		if(mCamera == null)
		{
			return;
		}
		
		try 
		{
			if (VideoPreviewSurfaceHolder != null)
			{
				Log.e("VideoRecorder UI", "setPreviewDisplay");
				mCamera.setPreviewDisplay(VideoPreviewSurfaceHolder);
				mCamera.startPreview();
			}
		}
		catch (IOException e) 
		{
			Log.e("VideoRecorder UI","----����ͷ��ʼԤ��ʧ��----");
			e.printStackTrace();
			
			mCamera.release();
			mCamera = null;
		}
		
		Log.e("VideoRecorder UI", "StartPreview ok");
	}
	
	private void StopPreview()
	{
		if(mCamera == null)
		{
			return;
		}
		
		mCamera.stopPreview();
	}
	
	private boolean StartRecord(IPreviewCallBack callback)
	{
		Log.e("VideoRecorder UI","----����ͷ��ʼ�ɼ����----");
		if(mCamera == null)
		{
			return false;
		}
		
		mEncoder = new VideoEncoder();
		
		mCamera.setPreviewCallback(callback);
		
		return true;
	}
	
	private void StopRecord()
	{
		if(mCamera == null)
		{
			return;
		}
		
		try 
		{
			mCamera.setPreviewCallback(null);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		mEncoder = null;
	}
	
	
}
