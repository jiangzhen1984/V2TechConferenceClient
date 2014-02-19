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
	 * If nGroupID is 0, means this API as IM API. Otherwise as conference API.<br>
	 * The xml content structure as third parameter szText.<br>
	 * <p>
	 *    If we only have text context to send, we don't need to add tag TPictureChatItem to xml.<br>
	 *    If we only have image or we need to add tag content TTextChatItem to xml content. And call this function,  we call {@link #sendChatPicture(long, long, byte[], int, int)} to send image. <br>
	 *    If we have mix content like text and image to send, we need to add tag TTextChatItem and TPictureChatItem.  And call this function,  we call {@link #sendChatPicture(long, long, byte[], int, int)} to send image<br>
	 * </p>
	 * @param nGroupID
	 *            conference ID
	 * @param nToUserID
	 *            user ID
	 * @param szText
	 * <br>
	 *            < ?xml version="1.0" encoding="utf-8"?><br>
	 *            < TChatData IsAutoReply="False"> <br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;< FontList> <br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;< TChatFont Color="255" Name="Segoe UI" Size="18" Style=""/><br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;< /FontList><br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;< ItemList><br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;< TTextChatItem NewLine="True" FontIndex="0" Text="这是一个测试"/><br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;< TPictureChatItem NewLine="False" AutoResize="True"
	 *            FileExt=".png" GUID="{F3870296-746D-4E11-B69B-050B2168C624}"
	 *            Height="109" Width="111"/><br>
	 *            &nbsp;&nbsp;&nbsp;&nbsp;< /ItemList><br>
	 *           < /TChatData><br>
	 * @param bussinessType  1 as Conference type,  2 as other type
	 * 
	 * @see {@link #sendChatPicture(long, long, byte[], int, int)}
	 */
	public native void sendChatText(long nGroupID, long nToUserID,
			String szText, int bussinessType);

	// �����������Ƶ��� nGroupID��0
	public native void sendChatAudio(long nGroupID, long nToUserID,
			String szText, String filename, int bussinessType);

	/**
	 * <p>Send image data to user.</p>
	 * <p>If input 0 as nGroupId, means P2P send data. Before call this API, call {@link #sendChatText(long, long, String, int)} first </p>
	 * @param nGroupID
	 * @param nToUserID
	 * @param pPicData <br>
	 *   |----image  header    52 bytes|----------------image data-------------|  <br>
	 *   |{UUID} extension bytes       |----------------image data-------------|  <br>
	 * @param nLength  52+image size
	 * @param bussinessType  1 as Conference type,  2 as other type
	 * 
	 * @see {@link #sendChatText(long, long, String, int)}
	 */
	public native void sendChatPicture(long nGroupID, long nToUserID,
			byte[] pPicData, int nLength, int bussinessType);

	/**
	 * 
	 * @param nGroupID
	 * @param nBusinessType
	 * @param nFromUserID
	 * @param nTime
	 * @param szXmlText
	 */
	public void OnRecvChatText(long nGroupID, int nBusinessType,
			long nFromUserID, long nTime, String szXmlText) {
		Log.e("ImRequest UI", "OnRecvChatText " + nGroupID + " "
				+ nBusinessType + " " + nFromUserID + " " + nTime + " "
				+ szXmlText);
		if (callback != null) {
			callback.OnRecvChatTextCallback(nGroupID, nBusinessType, nFromUserID, nTime, szXmlText);
		}

	}
	
	
	public void OnRecvChatAudio(long gid, int i, long j, long j1, String str,
			String str1) {

	}
	
	public void OnSendChatAudio(String str, int i) {

	}
	

	// �յ����˷�����ͼƬ������Ϣ�Ļص�
	public void OnRecvChatPicture(long nGroupID, int nBusinessType,
			long nFromUserID, long nTime, byte[] pPicData) {

		Log.e("ImRequest UI", "OnRecvChatPicture ���� " + nGroupID + " "
				+ nBusinessType + " " + nFromUserID + " " + nTime + " ");
		Log.e("ImRequest UI", "OnRecvChatPicture ****maximum heap size***"
				+ Runtime.getRuntime().maxMemory() + "*nLength=====**"
				+ "****pPicData.length===***" + pPicData.length);
		if (callback != null) {
			callback.OnRecvChatPictureCallback(nGroupID, nBusinessType, nFromUserID, nTime, pPicData);
		}
	}

	private String SaveImage2SD(long nGroupID, long nFromUserID, long nTime,
			byte[] pPicData) {
		byte[] guidArr = new byte[41];
		byte[] extArr = new byte[11];
		byte[] picDataArr = new byte[pPicData.length - 52];
		// int len = pPicData.length;
		for (int i = 0; i < pPicData.length; i++) {
			if (i <= 40) {
				if (pPicData[i] == 0) {
					i = 40;
					continue;
				}
				guidArr[i] = pPicData[i];
			} else if (i > 40 && i < 52) {
				if (pPicData[i] == 0) {
					i = 51;
					continue;
				}
				extArr[i - 41] = pPicData[i];
			} else {
				picDataArr[i - 52] = pPicData[i];
			}
		}
		StringBuffer sb = new StringBuffer();
		String fromId = "";
		if (nGroupID == 0) {
			fromId = nFromUserID + "";
		} else {
			fromId = "0" + nGroupID;
		}
		String guid = new String(guidArr);

		String ext = new String(extArr); // ��չ��
		sb.append(guid.trim()).append(ext.trim());

		// Logger.i(null, "ͼƬ�����:"+sb.toString());
		// String target = picNvoiceUtil.saveImage(picDataArr,
		// sb.toString(),nFromUserID,context);

		// return target;
		return "";
	}

	/*
	 * ext ͼƬ����չ��
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

	// ���ͼƬ·���õ�ͼƬ���ֽ�
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
