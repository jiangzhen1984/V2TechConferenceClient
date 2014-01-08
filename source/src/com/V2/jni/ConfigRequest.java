package com.V2.jni;

public class ConfigRequest
{
	
	//设置配置文件的属性
	public native void setConfigProp(String szItemPath, String szConfigAttr, String szValue);
	//获取配置文件的属性
	public native void getConfigProp(String szItemPath,String szConfigAttr, byte[] pValueBuf, int nBufLen);
	//获取配置文件的属性数量
	public native void getConfigPropCount(String szItemPath);
	//删除配置文件中某个属性
	public native void removeConfigProp(String szItemPath,String szConfigAttr);
	//设置服务器地址
	public native void setServerAddress(String szServerIP, int nPort);
	//设置存储卡地址
	public native void setExtStoragePath(String szPath);
}
