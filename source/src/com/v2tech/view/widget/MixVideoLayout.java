package com.v2tech.view.widget;

import android.content.Context;
import android.widget.FrameLayout;

import com.v2tech.vo.MixVideo;

public class MixVideoLayout extends FrameLayout {
	
	private MixVideo mMx;
	
	public MixVideoLayout(Context context, MixVideo mx) {
		super(context);
		this.mMx = mx;
	}
	

}
