package v2av;

import java.util.ArrayList;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;

public class VideoCaptureDevInfo
{
	private final static String TAG = "VideoCaptureDevInfo";
	
	private String mDefaultDevName = "";
	
	private CapParams mCapParams = new CapParams();
	
	public class CapParams
	{
		public int width=176;
		public int height=144;
		public int bitrate=70000;
		public int fps=15;
	}
	
	public void SetDefaultDevName(String devName)
	{
		mDefaultDevName = devName;
	}
	
	public String GetDefaultDevName()
	{
		return mDefaultDevName;
	}
	
	public void SetCapParams(int width, int height, int bitrate, int fps)
	{
		mCapParams.width = width;
		mCapParams.height = height;
		mCapParams.bitrate = bitrate;
		mCapParams.fps = fps;
	}
	
	public CapParams GetCapParams()
	{
		return mCapParams;
	}
	
	// Private class with info about all available cameras and the capabilities
    public class VideoCaptureDevice
    {
        VideoCaptureDevice()
        {
            frontCameraType = FrontFacingCameraType.None;
            index = 0;
        }

        public String deviceUniqueName;
        public CaptureCapability captureCapabilies[];
        public FrontFacingCameraType frontCameraType;

        // Orientation of camera as described in
        // android.hardware.Camera.CameraInfo.Orientation
        public int orientation;
        
        // Camera index used in Camera.Open on Android 2.3 and onwards
        public int index;
        
        public String strFPSs = "";
        public List<Integer> FPSs=new ArrayList<Integer>();
    }
    
    public enum FrontFacingCameraType
    {
        None, // This is not a front facing camera
        GalaxyS, // Galaxy S front facing camera.
        HTCEvo, // HTC Evo front facing camera
        Android23, // Android 2.3 front facing camera.
    }
    
    public List<VideoCaptureDevice> deviceList;
    
    private static VideoCaptureDevInfo s_self = null;
    public static VideoCaptureDevInfo CreateVideoCaptureDevInfo()
    {
    	if (s_self == null)
    	{
    		s_self = new VideoCaptureDevInfo();
    		if (s_self.Init() != 0)
    		{
    			s_self = null;
    			Log.d(TAG, "Failed to create VideoCaptureDevInfo.");
    		}
    	}

        return s_self;
    }
    
    public VideoCaptureDevice GetDevice(String devName)
    {
    	VideoCaptureDevice device = null;
    	
    	for (VideoCaptureDevice dev:deviceList)
    	{
    		if (dev.deviceUniqueName.equals(devName))
    		{
    			device = dev;
    			break;
    		}
    	}
    	
    	return device;
    }
    
    private VideoCaptureDevInfo()
    {
        deviceList = new ArrayList<VideoCaptureDevice>();
    }
    
    private int Init()
    {
        // Populate the deviceList with available cameras and their capabilities.
        Camera camera = null;
        try
        {
            // From Android 2.3 and onwards
            for (int i = 0; i < Camera.getNumberOfCameras(); ++i)
            {
                VideoCaptureDevice newDevice = new VideoCaptureDevice();

                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                newDevice.index = i;
                newDevice.orientation = info.orientation;
                if(info.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
                {
                    newDevice.deviceUniqueName = "Camera Facing back";
                    newDevice.frontCameraType = FrontFacingCameraType.None;
                    Log.d(TAG, "Camera " + i +", Facing back, Orientation "+ info.orientation);
                   
                }
                else
                {
                    newDevice.deviceUniqueName = "Camera Facing front";
                    newDevice.frontCameraType = FrontFacingCameraType.Android23;
                    Log.d(TAG, "Camera " + i +", Facing front, Orientation "+ info.orientation);
                    this.mDefaultDevName = newDevice.deviceUniqueName;
                }

                camera = Camera.open(i);
                Camera.Parameters parameters = camera.getParameters();
                AddDeviceInfo(newDevice, parameters);
                camera.release();
                camera = null;
                deviceList.add(newDevice);
            }
                
            VerifyCapabilities();
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Failed to init VideoCaptureDeviceInfo ex" +
                    ex.getLocalizedMessage());
            return -1;
        }
        
        return 0;
    }
    
 // Adds the capture capabilities of the currently opened device
    private void AddDeviceInfo(VideoCaptureDevice newDevice, Camera.Parameters parameters)
    {
        List<Size> sizes = parameters.getSupportedPreviewSizes();
        List<Integer> frameRates = parameters.getSupportedPreviewFrameRates();
        int maxFPS = 0;
        for(Integer frameRate:frameRates)
        {
        	newDevice.strFPSs = newDevice.strFPSs + frameRate + ",";
        	newDevice.FPSs.add(frameRate);
            if(frameRate > maxFPS)
            {
                maxFPS = frameRate;
            }
        }

        newDevice.captureCapabilies = new CaptureCapability[sizes.size()];
        for(int i = 0; i < sizes.size(); ++i)
        {
            Size s = sizes.get(i);
            newDevice.captureCapabilies[i] = new CaptureCapability();
            newDevice.captureCapabilies[i].height = s.height;
            newDevice.captureCapabilies[i].width = s.width;
            newDevice.captureCapabilies[i].maxFPS = maxFPS;
        }
    }

    // Function that make sure device specific capabilities are
    // in the capability list.
    // Ie Galaxy S supports CIF but does not list CIF as a supported capability.
    // Motorola Droid Camera does not work with frame rate above 15fps.
    // http://code.google.com/p/android/issues/detail?id=5514#c0
    private void VerifyCapabilities()
    {
        // Nexus S or Galaxy S
        if(android.os.Build.DEVICE.equals("GT-I9000") ||
                android.os.Build.DEVICE.equals("crespo"))
        {
            CaptureCapability specificCapability =
                    new CaptureCapability();
            specificCapability.width = 352;
            specificCapability.height = 288;
            specificCapability.maxFPS = 15;
            AddDeviceSpecificCapability(specificCapability);

            specificCapability = new CaptureCapability();
            specificCapability.width = 176;
            specificCapability.height = 144;
            specificCapability.maxFPS = 15;
            AddDeviceSpecificCapability(specificCapability);

            specificCapability = new CaptureCapability();
            specificCapability.width = 320;
            specificCapability.height = 240;
            specificCapability.maxFPS = 15;
            AddDeviceSpecificCapability(specificCapability);
        }
        
        // Motorola Milestone Camera server does not work at 30fps
        // even though it reports that it can
        if(android.os.Build.MANUFACTURER.equals("motorola") &&
                android.os.Build.DEVICE.equals("umts_sholes"))
        {
            for(VideoCaptureDevice device:deviceList)
            {
                for(CaptureCapability capability:device.captureCapabilies)
                {
                    capability.maxFPS=15;
                }
            }
        }
    }
    
    private void AddDeviceSpecificCapability(CaptureCapability specificCapability)
    {
    	for (VideoCaptureDevice device:deviceList)
        {
    		boolean foundCapability = false;
    		for(CaptureCapability capability:device.captureCapabilies)
    		{
    			if(capability.width == specificCapability.width &&
    					capability.height == specificCapability.height)
    			{
    				foundCapability = true;
    				break;
    			}
    		}
    		
    		if (foundCapability==false)
    		{
    			CaptureCapability newCaptureCapabilies[] = new CaptureCapability[device.captureCapabilies.length+1];
    			for(int i = 0; i < device.captureCapabilies.length; ++i)
    			{
    				newCaptureCapabilies[i+1] = device.captureCapabilies[i];
    			}
    			newCaptureCapabilies[0] = specificCapability;
    			device.captureCapabilies = newCaptureCapabilies;
    		}
        }
	}
}
