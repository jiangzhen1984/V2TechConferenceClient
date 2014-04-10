package com.v2tech.view.vo;

import com.v2tech.logic.Group;
import com.v2tech.logic.User;

public class V2ImageDoc extends V2Doc {

	public V2ImageDoc(String id, Group mGroup, int mBType, User mSharedUser) {
		super(id, mGroup, mBType, mSharedUser);
		this.mDocType = DOC_TYPE_IMAGE;
	}

}
