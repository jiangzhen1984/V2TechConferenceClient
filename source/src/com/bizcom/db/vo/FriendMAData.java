package com.bizcom.db.vo;

import android.graphics.Bitmap;

public class FriendMAData {

	public long _id;//应对数据库的_id
	public Bitmap dheadImage;
	public String name;
	public String authenticationMessage;
	// 别人加我：允许任何人：0已添加您为好友，需要验证：1未处理，2已同意，3已拒绝
	// 我加别人：允许认识人：4成为好友，需要验证：5等待验证，4被同意（成为好友），6被拒绝
	public int state;
	public long remoteUserID;
	// 对应数据库中id
	public long dbRecordIndex;
}
