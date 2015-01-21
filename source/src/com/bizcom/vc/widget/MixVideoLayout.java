package com.bizcom.vc.widget;

import android.content.Context;
import android.widget.FrameLayout;

import com.bizcom.vo.MixVideo;

public class MixVideoLayout extends FrameLayout {
	
	private MixVideo mMx;
	
	public MixVideoLayout(Context context, MixVideo mx) {
		super(context);
		this.mMx = mx;
	}
	

}
