package com.bizcom.request.jni;

import java.util.List;

import com.bizcom.vo.VCrowdFile;

/**
 * Used to wrap response data from JNI when receive call from JNI
 * 
 * @author 28851274
 * 
 */
public class RequestFetchGroupFilesResponse extends JNIResponse {

    private long groupID;
    private List<VCrowdFile> list;

	/**
	 * This class is wrapper that wrap response of chat service
	 * 
	 * @param result
	 *            {@link Result}
	 */
	public RequestFetchGroupFilesResponse(Result result) {
		super(result);
	}

	public List<VCrowdFile> getList() {
		return list;
	}

	public void setList(List<VCrowdFile> list) {
		this.list = list;
	}

    public long getGroupID() {
        return groupID;
    }

    public void setGroupID(long groupID) {
        this.groupID = groupID;
    }
}
