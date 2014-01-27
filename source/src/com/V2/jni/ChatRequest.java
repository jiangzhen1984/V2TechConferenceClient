package com.V2.jni;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

//import com.xinlan.im.R;
//import com.xinlan.im.adapter.XiuLiuApplication;
//import com.xinlan.im.bean.msgtype.ChatRecImgType;
//import com.xinlan.im.bean.msgtype.HasMsgType;
//import com.xinlan.im.bean.msgtype.MsgType;
//import com.xinlan.im.ui.SplashActivity;
//import com.xinlan.im.ui.chat.bean.ChatItem;
//import com.xinlan.im.ui.chat.bean.ChatMSG;
//import com.xinlan.im.ui.chat.bean.ChatMsgEntity;
//import com.xinlan.im.ui.chat.bean.TGifChatItem;
//import com.xinlan.im.ui.chat.bean.TPicChatItem;
//import com.xinlan.im.ui.chat.bean.TTextChatItem;
//import com.xinlan.im.ui.chat.db.DbHelper;
//import com.xinlan.im.utils.Logger;
//import com.xinlan.im.utils.XmlParserUtils;
//import com.xinlan.im.utils.picNvoiceUtil;

public class ChatRequest
{
//	private Activity context;
	private static ChatRequest mChatRequest;
//	private DbHelper dbHelper;
//	private XiuLiuApplication app;
//	private SharedPreferences preferences; 
	
	private  boolean  islinsheng, isviber;
//	private ChatMSG msg;


	
	@SuppressWarnings("static-access")
	private ChatRequest(Context context){
//		this.context = context;
//		dbHelper=new DbHelper(context);
//		app=(XiuLiuApplication) context.getApplication();
//		preferences=context.getSharedPreferences("config",context.MODE_APPEND);
		
	};

	public static synchronized ChatRequest getInstance(Context context) {
		if (mChatRequest == null) {
			mChatRequest = new ChatRequest(context);
			if (!mChatRequest.initialize(mChatRequest)) {
				Log.e("mChatRequest", "can't initialize mChatRequest ");
			}
		}

		return mChatRequest;
	}
	
	public  static synchronized ChatRequest getInstance() {
		if (mChatRequest == null) {
			throw new RuntimeException(" mChatRequest is null do getInstance(Context context) first ");
		}
		return mChatRequest;
	}

	
	public native boolean initialize(ChatRequest request);
	public native void unInitialize();
	
	//����������������    nGroupID��0
	public native void sendChatText(long  nGroupID, long  nToUserID, String szText,int bussinessType);
	//�����������Ƶ���    nGroupID��0
		public native void sendChatAudio(long  nGroupID, long  nToUserID, String szText,String filename,int bussinessType);
	//���������ͼƬ���
	public native void sendChatPicture(long  nGroupID, long  nToUserID, byte[] pPicData, int nLength,int bussinessType);
	
	//�յ����˷���������������Ϣ�Ļص�
	public void OnRecvChatText(long nGroupID, int nBusinessType, long  nFromUserID, long  nTime, String szXmlText)
	{
		Log.e("ImRequest UI", "OnRecvChatText ����" + nGroupID + " " + nBusinessType + " " + nFromUserID + " " + nTime + " " + szXmlText);
		
//		islinsheng=preferences.getBoolean("islinsheng", true);
//		isviber=preferences.getBoolean("isviber", true);
//
//		if(islinsheng){
//			MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.office);
//			mediaPlayer.start();
//		}
//		
//		if(isviber){
//			Vibrator mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
//			mVibrator.vibrate(200);
//		}
//		
//		List<ChatItem> msgText = XmlParserUtils.parserChatInfo(new ByteArrayInputStream(szXmlText.getBytes()),nFromUserID,context);
//		
//	    msg = new ChatMSG();
//		msg.setnBusinessType(nBusinessType);
//		msg.setnFromUserID(nFromUserID);
//		msg.setnGroupID(nGroupID);
//		msg.setnTime(nTime);
//		msg.getItems().addAll(msgText);
//		
//		//ƴװ������Ϣ
//		HasMsgType hasMsgType=new HasMsgType();
//		hasMsgType.setChatmsg(msg);
//		
//		Intent addIntent=new Intent(SplashActivity.IM);
//		addIntent.putExtra("MsgType", MsgType.HAS_MSG_COME);
//		addIntent.putExtra("MSG", hasMsgType);
//		context.sendOrderedBroadcast(addIntent,null);
	}
	

	//�յ����˷�����ͼƬ������Ϣ�Ļص�
	public void OnRecvChatPicture(long  nGroupID, int nBusinessType, long  nFromUserID, long  nTime, byte[] pPicData)
	{
		    //��һ��ͼƬ����ʼ��ʾһ��ͼƬ��֪ͨҳ��ˢ��
		
			Log.e("ImRequest UI", "OnRecvChatPicture ���� " + nGroupID + " " + nBusinessType + " " + nFromUserID + " " + nTime + " ");
			Log.e("ImRequest UI", "OnRecvChatPicture ****maximum heap size***"+ Runtime.getRuntime().maxMemory()+"*nLength=====**"+"****pPicData.length===***"+pPicData.length );
			
//			String localpath=SaveImage2SD(nGroupID, nFromUserID, nTime,pPicData);
//			
//			Logger.i(null, "ͼƬ��׼·��:"+localpath);
//			
//			
//			
//			//ƴװ������Ϣ
//			Intent addIntent=new Intent(SplashActivity.IM);
//			addIntent.putExtra("MsgType", MsgType.REFRESH_IMG);
//			context.sendOrderedBroadcast(addIntent,null);
			
	}

	private String SaveImage2SD(long nGroupID, long nFromUserID, long nTime, byte[] pPicData) {
		byte[] guidArr = new byte[41];
		byte[] extArr = new byte[11];
		byte[] picDataArr = new byte[pPicData.length-52];
//			int len = pPicData.length;
		for(int i=0;i<pPicData.length;i++){
			if(i<=40){
				if(pPicData[i]==0){
					i=40;
					continue;
				}
				guidArr[i]=pPicData[i];
			}else if(i>40&&i<52){
				if(pPicData[i]==0){
					i=51;
					continue;
				}
				extArr[i-41]=pPicData[i];
			}else{
				picDataArr[i-52]=pPicData[i];
			}
		}
		StringBuffer sb = new StringBuffer();
		String fromId = "";
		if(nGroupID==0){
			fromId = nFromUserID+"";
		}else{
			fromId = "0"+nGroupID;
		}
		String guid = new String(guidArr);  
		
		String ext = new String(extArr);  //��չ��
		sb.append(guid.trim()).append(ext.trim());
		
//		Logger.i(null, "ͼƬ�����:"+sb.toString());
//		String target = picNvoiceUtil.saveImage(picDataArr, sb.toString(),nFromUserID,context);
		
//		return target;
		return "";
	}
	
	

	/*
	 * ext  ͼƬ����չ��
	 */
	@SuppressWarnings("resource")
	public byte[] getSendPicData(String imgpath){
		String uuid=UUID.randomUUID().toString();
		String guid="{"+uuid+"}";
		
		String extname=imgpath.substring(imgpath.indexOf("."));
		
		int bytes = 0;
		FileInputStream stream=null;
		try {
			stream=new FileInputStream(new File(imgpath));
			bytes = stream.available();
			
			int length=52+bytes;
			
			byte[] allbytes=new byte[length];
			
			byte[] guidarr = guid.getBytes();
			byte[] extarr=extname.getBytes();
			for(int i=0;i<41;i++){
				if(i<guidarr.length){
					allbytes[i]=guidarr[i];
				}else{
					allbytes[i]=0;
				}
			}
			
			for(int i=41;i<52;i++){
				if(i-40>4){
					allbytes[i]=0;
				}else{
					allbytes[i]=extarr[i-41];
				}
			}
			
			stream.read(allbytes, 52,bytes);
			
			return allbytes;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	//���ͼƬ·���õ�ͼƬ���ֽ�
	public static byte[] readStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream;
		byte[] data;
		try {
			byte[] buffer = new byte[1024];
			int len = -1;
			outStream = new ByteArrayOutputStream();
			while ((len = inStream.read(buffer)) != -1) {
			        outStream.write(buffer, 0, len);
			}
			data = outStream.toByteArray();
			
			outStream.close();
			inStream.close();
			return data;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}finally{
		}
	}
	
	 public static byte[] Bitmap2Bytes(Bitmap bm){
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
         return baos.toByteArray();
	 }
	 
	
	 
	 
//	public byte[] getImgSendData(String filepath,long userid,ChatMsgEntity chat) {
//		String uuid=UUID.randomUUID().toString();
//		String filename="{"+uuid+"}";
//		byte[] nameEntity = filename.getBytes();
//		byte[] name = Arrays.copyOf(nameEntity, 41);
//		
//		String extname=filepath.substring(filepath.indexOf("."));
//		byte[] extEntity = extname.getBytes();
//		byte[] ext = Arrays.copyOf(extEntity, 11);
//		
//		
//		byte[] photoArray;
//		byte[] transArr = null ;
//		
//		try {
//			InputStream InputStream=new FileInputStream(filepath);
//			photoArray = readStream(InputStream);
//			transArr= byteMerger(name,ext,photoArray);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		Bitmap temp=BitmapFactory.decodeFile(filepath);
//		chat.setHeight(temp.getHeight());
//		chat.setWidth(temp.getWidth());
//		String preImg=XmlParserUtils.createSendImgMsg(filename, extname, temp.getWidth(), temp.getHeight());
//		sendChatText(0, userid, preImg, 2);
//		return transArr;
//	}

	private byte[] byteMerger(byte[] byte_1, byte[] byte_2,byte[] byte_3){  
		int len_1 = byte_1.length;
		int len_2 = byte_2.length;
		int len_3 = byte_3.length;
	    byte[] byte_4 = new byte[len_1+len_2+len_3];
	    System.arraycopy(byte_1, 0, byte_4, 0, len_1);  
	    System.arraycopy(byte_2, 0, byte_4, len_1, len_2);
	    System.arraycopy(byte_3, 0, byte_4, len_1+len_2, len_3);
	    return byte_4;  
	}

	
}
