package com.bizcom.vc.widget.cus;

import android.content.Context;
import android.graphics.Canvas;
import android.widget.TextView;

public class ChatTextView extends TextView {

	public ChatTextView(Context context) {
		super(context);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		try {
			super.onDraw(canvas);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
