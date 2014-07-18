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
	public static int CodecType = 0;
	
	private int mSrcWidth;
	private int mSrcHeight;
	
	private int mVideoWidth;
	private int mVideoHeight;
	private int mBitrate;
	private int mPreviewFormat;
	private int mFrameRate;
	private int mSelectedFrameRate;
//	private int mCameraRotation;
//	private boolean mbMirror;
	private int framecount;

	private VideoEncoder mEncoder = null;
	
	private Camera mCamera = null;
	private VideoCaptureDevInfo mCapDevInfo = null;
	
	private EncoderPreviewCallBack mVRCallback = null;
	
	VideoRecorder()
	{
		mCapDevInfo = VideoCaptureDevInfo.CreateVideoCaptureDevInfo();
	}
	
	@SuppressWarnings("unused")
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
    	mPreviewFormat = capParams.format;
    	
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
	
	@SuppressWarnings("unused")
	private int StopRecordVideo()
	{
		Log.i("VideoRecorder UI","StopRecordVideo");
		
		StopPreview();
		UninitCamera();
		
		return 0;
	}
	
	@SuppressWarnings("unused")
	private int GetCodecType()
	{
		return CodecType;
	}
	@SuppressWarnings("unused")
	private int GetRecordWidth()
	{
		return mVideoWidth;
	}
	
	@SuppressWarnings("unused")
	private int GetRecordHeight()
	{
		return mVideoHeight;
	}
	
	@SuppressWarnings("unused")
	private int GetRecordBitrate()
	{
		return mBitrate;
	}
	
	@SuppressWarnings("unused")
	private int GetRecordFPS()
	{
		return mFrameRate;
	}
	
	@SuppressWarnings("unused")
	private int GetRecordFormat()
	{
		return mPreviewFormat;
	}
	
	@SuppressWarnings("unused")
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
	
	@SuppressWarnings("unused")
	private int GetPreviewWidth()
	{
		return mSrcWidth;
	}
	
	@SuppressWarnings("unused")
	private int GetPreviewHeight()
	{
		return mSrcHeight;
	}
	
	@SuppressWarnings("unused")
	private int StartSend()
	{
		mVRCallback = new EncoderPreviewCallBack(this);

		if (StartRecord(mVRCallback) != true)
		{
			return -1;
		}
		
		return 0;
	}

	@SuppressWarnings("unused")
	private int StopSend()
	{
		StopRecord();
		if (mVRCallback != null)
		{
			mVRCallback = null;
		}
		
		return 0;
	}
	
	private VideoSize GetSrcSizeByEncSize(VideoCaptureDevice device, int width, int height)
    {
    	VideoSize size = new VideoSize();
    	
    	for(CaptureCapability capability:device.capabilites)
		{
    		if (capability.width*capability.height >= width*height)
    		{
    			size.width = capability.width;
    			size.height = capability.height;
    			
    			break;
    		}
		}
    	
    	return size;
    }
	
	private int SelectFramerate(VideoCaptureDevice device, int fps)
    {
    	int selectedFps = 0;
    	
    	for (Integer framerate:device.framerates)
    	{
    		if (framerate >= fps)
    		{
    			selectedFps = framerate;
    			break;
    		}
    	}
    	
    	return selectedFps;
    }
	
	private void VeritifyRecordInfo(VideoCaptureDevice device)
    {
		VideoSize size = GetSrcSizeByEncSize(device, mVideoWidth, mVideoHeight);
		mSrcWidth = size.width;
    	mSrcHeight = size.height;
    	
    	mSelectedFrameRate = SelectFramerate(device, mFrameRate);
/*
    	if (device.frontCameraType == VideoCaptureDevInfo.FrontFacingCameraType.Android23)
		{
    		mbMirror = true;
		}
		else
		{
			mbMirror = false;
		}
*/
//    	mCameraRotation = device.orientation;
    }
	
	public void onGetVideoFrame(byte [] databuf, int len)
	{
		if (DropFrame())
    	{
    		return;
    	}
		
		if (mEncoder != null)
		{
			mEncoder.encodeframe(databuf, len);
		}
	}
	
	private boolean DropFrame()
    {
    	if (mSelectedFrameRate <= mFrameRate)
    	{
    		return false;
    	}
    	
    	framecount++;
    	
    	switch (mSelectedFrameRate)
    	{
    	case 10:
    		if (framecount % 2 != 0)
    		{
    			return true;
    		}
    		break;
    	case 15:
    		if (mFrameRate == 5)
    		{
    			if (framecount % 3 != 0)
        		{
        			return true;
        		}
    		}
    		else // mVideoRecordInfo.mFrameRate == 10
    		{
    			if (framecount % 3 == 0)
        		{
        			return true;
        		}
    		}
    		break;
    	case 30:
    		if (mFrameRate == 5)
    		{
    			if (framecount % 6 != 0)
        		{
        			return true;
        		}
    		}
    		else if (mFrameRate == 10)
    		{
    			if (framecount % 3 != 0)
        		{
        			return true;
        		}
    		}
    		else // mVideoRecordInfo.mFrameRate == 15
    		{
    			if (framecount % 2 != 0)
        		{
        			return true;
        		}
    		}
    		break;
    	default:
    		break;
    	}
		
		return false;
    }
	
	private AVCode InitCamera(VideoCaptureDevice device)
	{
		Log.i("VideoRecorder UI", "InitCamera");
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
		para.setPreviewSize(mSrcWidth, mSrcHeight);
		para.setPreviewFrameRate(mFrameRate);
		para.setPreviewFormat(mPreviewFormat);
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
		Log.i("VideoRecorder UI", "StartPreview");
		
		if(mCamera == null)
		{
			return;
		}
		
		try 
		{
			if (VideoPreviewSurfaceHolder != null)
			{
				mCamera.setPreviewDisplay(VideoPreviewSurfaceHolder);
				mCamera.startPreview();
			}
		}
		catch (IOException e) 
		{
			Log.e("ConfRoomActivity","----ÉãÏñÍ·¿ªÊ¼Ô¤ÀÀÊ§°Ü----");
			e.printStackTrace();
			
			mCamera.release();
			mCamera = null;
		}
	}
	
	private void StopPreview()
	{
		Log.i("VideoRecorder UI", "StopPreview");
		
		if(mCamera == null)
		{
			return;
		}
		
		mCamera.stopPreview();
	}
	
	private boolean StartRecord(IPreviewCallBack callback)
	{
		Log.i("VideoRecorder UI", "StartRecord");
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
		Log.i("VideoRecorder UI", "StopRecord");
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
