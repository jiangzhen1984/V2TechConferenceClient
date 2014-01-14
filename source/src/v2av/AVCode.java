package v2av;

public enum AVCode 
{
	//未初使化.videoRecordinit,调用StartVideoRecord
	Err_ErrorState,
	//打开摄像头失败
	Err_CameraOpenError,
	//摄像头已经打开过.
	Err_CameraAlreadyOpen,
	//视频编码器初使化失败
	Err_VideoEncoderInitError,
	//视频编码器启动失败
	Err_VideoEncoderStartError,
	//正常,无错误
	Err_None,
}
