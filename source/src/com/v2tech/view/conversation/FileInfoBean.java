package com.v2tech.view.conversation;
import android.os.Parcel;
import android.os.Parcelable;

public class FileInfoBean implements Parcelable{
		
		public String fileName;
		public String fileDate;
		public String fileSize;
		public String fileItmes;
		public String filePath;
		public boolean isDir;
		
		public FileInfoBean() {
			super();
		}

		public FileInfoBean(String fileName, String fileDate, String fileSize,
				String fileItmes, String filePath) {
			this.fileName = fileName;
			this.fileDate = fileDate;
			this.fileSize = fileSize;
			this.fileItmes = fileItmes;
			this.filePath = filePath;
		}

		@Override
		public int describeContents() {
			return 0;
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			
			dest.writeString(fileName);
			dest.writeString(fileDate);
			dest.writeString(fileSize);
			dest.writeString(fileItmes);
			dest.writeString(filePath);
		}
		
		 public static final Parcelable.Creator<FileInfoBean> CREATOR = new Creator<FileInfoBean>() {
	         
		        @Override
		        public FileInfoBean[] newArray(int i) {
		            return new FileInfoBean[i];
		        }
		         
		        @Override
		        public FileInfoBean createFromParcel(Parcel parcel) {
		            return new FileInfoBean(parcel.readString(), 
		            						parcel.readString(), 
		            						parcel.readString(), 
		            						parcel.readString(), 
		            						parcel.readString());
		        }
		    };  
		}
	