package com.v2tech.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.View;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class DuplicatedSurface extends View {
	
	private Surface mSurface;

	public DuplicatedSurface(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mSurface = new Surface(new SurfaceTexture(3));
	}

	public DuplicatedSurface(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DuplicatedSurface(Context context) {
		super(context);
	}

}
