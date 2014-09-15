package com.V2.jni;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.V2.jni.ind.SendingResultJNIObjectInd;
import com.V2.jni.util.V2Log;

public class ChatRequest {

	public static final int BT_CONF = 1;
	public static final int BT_IM = 2;

	private static ChatRequest mChatRequest;

	private ChatRequestCallback callback;

	private ChatRequest(Context context) {

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

	public void setChatRequestCallback(ChatRequestCallback callback) {
		this.callback = callback;
		for (ChatText ct : ctL) {
			this.callback.OnRecvChatTextCallback(ct.eGroupType , ct.nGroupID, ct.nToUserID,
					ct.nFromUserID, ct.nTime, ct.szSeqID , ct.szXmlText);
		}

//		for (ChatPicture cp : cpL) {
//			this.callback.OnRecvChatPictureCallback(cp.nGroupID,
//					cp.nBusinessType, cp.nFromUserID, cp.nTime, cp.nSeqId,
//					cp.pPicData);
//		}
		
		for (ChatBinary cb : btL) {
			this.callback.OnRecvChatBinaryCallback(cb.eGroupType , cb.nGroupID,
					 cb.nFromUserID,  cb.nToUserID, cb.nTime, cb.messageId,
					cb.binaryType , cb.binaryPath);
		}
		// clear cache.
		ctL.clear();
		btL.clear();
//		cpL.clear();
	}

	public static synchronized ChatRequest getInstance() {
		if (mChatRequest == null) {
			throw new RuntimeException(
					" mChatRequest is null do getInstance(Context context) first ");
		}
		return mChatRequest;
	}

	public native boolean initialize(ChatRequest request);

	public native void unInitialize();

	/**
	 * Send text content to user.<br>
	 * If nGroupID is 0 P2P message, otherwise is group message.<br>
	 * The xml content structure as third parameter szText.<br>
	 * <p>
	 * If we only have text context to send, we don't need to add tag
	 * TPictureChatItem to xml.<br>
	 * If we only have image or we need to add tag content TTextChatItem to xml
	 * content. And call this function, we call
	 * {@link #sendChatPicture(long, long, byte[], int, int)} to send image. <br>
	 * If we have mix content like text and image to send, we need to add tag
	 * TTextChatItem and TPictureChatItem. And call this function, we call
	 * {@link #sendChatPicture(long, long, byte[], int, int)} to send image<br>
	 * </p>
	 * 
	 * @param nGroupID
	 *            conference ID
	 * @param nToUserID
	 *            user ID
	 * 
	 * @param nSeqId
	 *            unique Id. It's used to if sent message failed, you will
	 *            according to this parameter distinguish which message is
	 *            failed. TODO add call back which API will be call if failed
	 * @param szText
	 * <br>
	 *            < ?xml version="1.0" encoding="utf-8"?><br>
	 *            < TChatData IsAutoReply="False"> <br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;< FontList> <br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;< TChatFont Color="255"
	 *            Name="Segoe UI" Size="18" Style=""/><br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;< /FontList><br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;< ItemList><br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<
	 *            TTextChatItem NewLine="True" FontIndex="0"
	 *            Text="杩����涓�涓�娴�璇�"/><br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<
	 *            TPictureChatItem NewLine="False" AutoResize="True"
	 *            FileExt=".png" GUID="{F3870296-746D-4E11-B69B-050B2168C624}"
	 *            Height="109" Width="111"/><br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;< /ItemList><br>
	 *            < /TChatData><br>
	 * @param bussinessType
	 *            1 as Conference type, 2 as other type
	 * 
	 * @see {@link #sendChatPicture(long, long, byte[], int, int)}
	 */
	public native void sendTextMessage(int eGroupType, long nGroupID,
			long nToUserID, String nSeqId, String szText, int nLen);

	/**
	 * <p>
	 * Send binary data to user.
	 * </p>
	 * <p>
	 * If input 0 as nGroupId, means P2P send data. Before call this API, call
	 * {@link #sendChatText(long, long, String, int)} first
	 * </p>
	 * 
	 * @param eGroupType ：VMessage的MsgCode值
	 * @param nGroupID
	 * @param fileName ：图片相对于数据库的位置。完整路径
	 * @param nToUserID
	 * @param binaryType
	 * 			  2 as picture ， 3 as audio
	 * @param nSeqId
	 *            unique Id. It's used to if sent message failed, you will
	 *            according to this parameter distinguish which message is
	 *            failed. TODO add call back which API will be call if failed
	 * @param pPicData
	 * <br>
	 *            |----image header 52 bytes|----------------image
	 *            data-------------| <br>
	 *            |{UUID} extension bytes |----------------image
	 *            data-------------| <br>
	 * @param nLength
	 *            52+image size
	 * @param bussinessType
	 *            1 as Conference type, 2 as other type
	 * 
	 * @see {@link #sendChatText(long, long, String, int)}
	 */
	public native void sendBinaryMessage(int eGroupType, long nGroupID,
			long nToUserID, int binaryType, String nSeqId, String fileName , int bussinessType);

//	List<ChatPicture> cpL = new ArrayList<ChatPicture>();
//	List<ChatAudio> caL = new ArrayList<ChatAudio>();
	List<ChatText> ctL = new ArrayList<ChatText>();
	List<ChatBinary> btL = new ArrayList<ChatBinary>();

	/**
	 * 
	 * @param nGroupID
	 * @param nBusinessType
	 * @param nFromUserID
	 * @param nTime
	 * @param szSeqID
	 * @param szXmlText
	 */
	private void OnRecvText(int eGroupType, long nGroupID, long nFromUserID,
			long nToUserID, long nTime, String szSeqID, String szXmlText) {
		V2Log.e("ChatRequest UI", "OnRecvChatText ---> eGroupType :"
				+ eGroupType + " | nGroupID: " + nGroupID + " | nFromUserID: "
				+ nFromUserID + " | nToUserID: " + nToUserID + " | nTime: "
				+ nTime + " | szSeqID: " + szSeqID + " | szXmlText: "
				+ szXmlText);
		if (callback != null) {
			 callback.OnRecvChatTextCallback(eGroupType, nGroupID, nFromUserID,
						 nToUserID, nTime, szSeqID, szXmlText);
		} else {
			 ctL.add(new ChatText(eGroupType, nGroupID, nFromUserID,
					 nToUserID, nTime, szSeqID, szXmlText));
		}
	}

	/**
	 * 接收图片和音频等二进制文件
	 * 
	 * @param eGroupType
	 * @param nGroupID
	 * @param nFromUserID
	 * @param nToUserID
	 * @param nTime
	 * @param binaryType
	 * @param messageId
	 * @param binaryPath
	 */
	private void OnRecvBinary(int eGroupType, long nGroupID, long nFromUserID,
			long nToUserID, long nTime, int binaryType, String messageId,
			String binaryPath) {
		V2Log.e("ChatRequest UI", "OnRecvChatBinary ---> eGroupType :"
				+ eGroupType + " | nGroupID: " + nGroupID + " | nFromUserID: "
				+ nFromUserID + " | nToUserID: " + nToUserID + " | nTime: "
				+ nTime + " | binaryType: " + binaryType + " | messageId: "
				+ messageId + " | binaryPath: " + binaryPath);
		if (callback != null) {
			 callback.OnRecvChatBinary(eGroupType, nGroupID, nFromUserID,
						nToUserID, nTime, binaryType, messageId,
						binaryPath);
		} else {
			btL.add(new ChatBinary(eGroupType, nGroupID, nFromUserID,
						nToUserID, nTime, binaryType, messageId,
						binaryPath));
		}
	}

	/**
	 * 
	 * @param uuid
	 * @param code
	 *            1 : text 2:image 3: audio
	 * @param ret
	 */
	private void OnSendTextResult(int eGroupType, long nGroupID,
			long nFromUserID, long nToUserID, String sSeqID, int nResult) {
		V2Log.e("ChatRequest UI", "OnSendChatResult ---> eGroupType :"
				+ eGroupType + " | nGroupID: " + nGroupID + " | nFromUserID: "
				+ nFromUserID + " | nToUserID: " + nToUserID + " | sSeqID: "
				+ sSeqID + " | nResult: " + nResult);
		if (callback != null) {
			 callback.OnSendChatResult(new SendingResultJNIObjectInd(sSeqID,
			 SendingResultJNIObjectInd.Result.fromInt(nResult), nResult));
		}
	}

	/**
	 * 
	 * @param uuid
	 * @param code
	 * @param ret
	 */
	private void OnSendBinaryResult(int eGroupType, long nGroupID,
			long nFromUserID, long nToUserID, int mediaType,String sSeqID,
			int nResult) {
		V2Log.e("ChatRequest UI", "OnSendBinaryResult ---> eGroupType :"
				+ eGroupType + " | nGroupID: " + nGroupID + " | nFromUserID: "
				+ nFromUserID + " | nToUserID: " + nToUserID + " | mediaType: "
				+ mediaType + " | sSeqID: " + sSeqID + " | nResult: " + nResult);
		if (callback != null) {
			 callback.OnSendChatResult(new SendingResultJNIObjectInd(sSeqID,
			 SendingResultJNIObjectInd.Result.fromInt(nResult), nResult));
		}
	}
	
	
	class ChatText {
		int eGroupType;
		long nGroupID;
//		int nBusinessType;
		long nFromUserID;
		long nToUserID;
		long nTime;
		String szSeqID;
		String szXmlText;

		public ChatText(int eGroupType, long nGroupID, long nFromUserID,
				long nToUserID, long nTime, String szSeqID, String szXmlText) {
			super();
			this.eGroupType = eGroupType;
			this.nGroupID = nGroupID;
			this.nToUserID = nToUserID;
			this.szSeqID = szSeqID;
//			this.nBusinessType = nBusinessType;
			this.nFromUserID = nFromUserID;
			this.nTime = nTime;
			this.szXmlText = szXmlText;
		}

	}
	
	class ChatBinary {
		int eGroupType;
		long nGroupID;
//		int nBusinessType;
		long nFromUserID;
		long nToUserID;
		long nTime;
		int binaryType;
		String messageId;
		String binaryPath;

		public ChatBinary(int eGroupType, long nGroupID, long nFromUserID,
				long nToUserID, long nTime, int binaryType, String messageId,
				String binaryPath) {
			super();
			this.eGroupType = eGroupType;
			this.nGroupID = nGroupID;
//			this.nBusinessType = nBusinessType;
			this.nFromUserID = nFromUserID;
			this.nToUserID = nToUserID;
			this.nTime = nTime;
			this.binaryType = binaryType;
			this.messageId = messageId;
			this.binaryPath = binaryPath;
		}
	}

	class ChatPicture {
		long nGroupID;
		int nBusinessType;
		long nFromUserID;
		long nTime;
		String nSeqId;
		byte[] pPicData;

		public ChatPicture(long nGroupID, int nBusinessType, long nFromUserID,
				long nTime, String nSeqId, byte[] pPicData) {
			super();
			this.nGroupID = nGroupID;
			this.nBusinessType = nBusinessType;
			this.nFromUserID = nFromUserID;
			this.nTime = nTime;
			this.nSeqId = nSeqId;
			this.pPicData = pPicData;
		}

	}

	class ChatAudio {
		long nGroupID;
		int nBusinessType;
		long nFromUserID;
		long nTime;
		String nSeqId;
		String audioPath;

		public ChatAudio(long nGroupID, int nBusinessType, long nFromUserID,
				long nTime, String nSeqId, String audioPath) {
			super();
			this.nGroupID = nGroupID;
			this.nBusinessType = nBusinessType;
			this.nFromUserID = nFromUserID;
			this.nTime = nTime;
			this.nSeqId = nSeqId;
			this.audioPath = audioPath;
		}

	}

	/*
	 * ext 图片锟斤拷锟斤拷展锟斤拷
	 */
	@SuppressWarnings("resource")
	public byte[] getSendPicData(String imgpath) {
		String uuid = UUID.randomUUID().toString();
		String guid = "{" + uuid + "}";

		String extname = imgpath.substring(imgpath.indexOf("."));

		int bytes = 0;
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(new File(imgpath));
			bytes = stream.available();

			int length = 52 + bytes;

			byte[] allbytes = new byte[length];

			byte[] guidarr = guid.getBytes();
			byte[] extarr = extname.getBytes();
			for (int i = 0; i < 41; i++) {
				if (i < guidarr.length) {
					allbytes[i] = guidarr[i];
				} else {
					allbytes[i] = 0;
				}
			}

			for (int i = 41; i < 52; i++) {
				if (i - 40 > 4) {
					allbytes[i] = 0;
				} else {
					allbytes[i] = extarr[i - 41];
				}
			}

			stream.read(allbytes, 52, bytes);

			return allbytes;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	// 锟斤拷锟酵计�路锟斤拷锟矫碉拷图片锟斤拷锟街斤拷
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
		} finally {
		}
	}

	public static byte[] Bitmap2Bytes(Bitmap bm) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
		return baos.toByteArray();
	}

	// public byte[] getImgSendData(String filepath,long userid,ChatMsgEntity
	// chat) {
	// String uuid=UUID.randomUUID().toString();
	// String filename="{"+uuid+"}";
	// byte[] nameEntity = filename.getBytes();
	// byte[] name = Arrays.copyOf(nameEntity, 41);
	//
	// String extname=filepath.substring(filepath.indexOf("."));
	// byte[] extEntity = extname.getBytes();
	// byte[] ext = Arrays.copyOf(extEntity, 11);
	//
	//
	// byte[] photoArray;
	// byte[] transArr = null ;
	//
	// try {
	// InputStream InputStream=new FileInputStream(filepath);
	// photoArray = readStream(InputStream);
	// transArr= byteMerger(name,ext,photoArray);
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// Bitmap temp=BitmapFactory.decodeFile(filepath);
	// chat.setHeight(temp.getHeight());
	// chat.setWidth(temp.getWidth());
	// String preImg=XmlParserUtils.createSendImgMsg(filename, extname,
	// temp.getWidth(), temp.getHeight());
	// sendChatText(0, userid, preImg, 2);
	// return transArr;
	// }

	private byte[] byteMerger(byte[] byte_1, byte[] byte_2, byte[] byte_3) {
		int len_1 = byte_1.length;
		int len_2 = byte_2.length;
		int len_3 = byte_3.length;
		byte[] byte_4 = new byte[len_1 + len_2 + len_3];
		System.arraycopy(byte_1, 0, byte_4, 0, len_1);
		System.arraycopy(byte_2, 0, byte_4, len_1, len_2);
		System.arraycopy(byte_3, 0, byte_4, len_1 + len_2, len_3);
		return byte_4;
	}

}