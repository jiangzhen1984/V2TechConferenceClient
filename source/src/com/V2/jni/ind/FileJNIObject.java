package com.V2.jni.ind;

import android.os.Parcel;
import android.os.Parcelable;

public class FileJNIObject extends JNIObjectInd implements Parcelable{

	public V2User user;
	public String fileId;
	public String fileName;
	public long fileSize;
	public int fileType;
	
	//For crowd file type
	public String url;
	

	/**
	 * 
	 * @param userid
	 * @param szFileID
	 * @param szFileName
	 * @param nFileBytes
	 * @param linetype 2: offline file  1: online file
	 */
	public FileJNIObject(V2User user, String szFileID, String szFileName,
			long nFileBytes, int linetype) {
		this.user = user;
		this.fileId = szFileID;
		this.fileName = szFileName;
		this.fileSize = nFileBytes;
		this.fileType = linetype;
		this.mType = JNIIndType.FILE;
	}
	
	/**
	 * 
	 * @param user
	 * @param szFileID
	 * @param szFileName
	 * @param nFileBytes
	 * @param linetype
	 * @param url
	 */
	public FileJNIObject(Parcelable user, String szFileID, String szFileName,
			long nFileBytes, int linetype , String url) {
		this.user = (V2User) user;
		this.fileId = szFileID;
		this.fileName = szFileName;
		this.fileSize = nFileBytes;
		this.fileType = linetype;
		this.mType = JNIIndType.FILE;
		this.url = url;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}


	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(user, 0);
		dest.writeString(fileId);
		dest.writeString(fileName);
		dest.writeLong(fileSize);
		dest.writeInt(fileType);		
		dest.writeString(url);		
	}
	
	public static final Parcelable.Creator<FileJNIObject> CREATOR = new Creator<FileJNIObject>() {

		@Override
		public FileJNIObject[] newArray(int i) {
			return new FileJNIObject[i];
		}

		@Override
		public FileJNIObject createFromParcel(Parcel parcel) {
			return new FileJNIObject(parcel.readParcelable(V2User.class.getClassLoader()) , parcel.readString(), parcel.readString(),
					parcel.readLong(), parcel.readInt(), parcel.readString());
		}
	};
}
