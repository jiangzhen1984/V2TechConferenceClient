package com.v2tech.service.jni;

public class FileTransStatusIndication extends JNIIndication {
	
	public static final int IND_TYPE_PROGRESS = 1;
	public static final int IND_TYPE_TRANS_ERR = 2;
	
	
	public int indType;
	public int nTransType;
	public String uuid;
	
	public FileTransStatusIndication(int indType, int nTransType,
			String uuid) {
		super(Result.SUCCESS);
		this.indType = indType;
		this.nTransType = nTransType;
		this.uuid = uuid;
	}
	
	
	

	public static class FileTransProgressStatusIndication extends FileTransStatusIndication {
		public long nTranedSize;

		public FileTransProgressStatusIndication(int nTransType,
				String uuid, long nTranedSize) {
			super(IND_TYPE_PROGRESS, nTransType, uuid);
			this.nTranedSize = nTranedSize;
		}
	}
	
	
	public static class FileTransErrorIndication extends FileTransStatusIndication {

		public FileTransErrorIndication(String uuid) {
			super(IND_TYPE_TRANS_ERR, 1, uuid);
		}
		
	}
}
