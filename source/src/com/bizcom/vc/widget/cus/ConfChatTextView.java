package com.bizcom.vc.widget.cus;

import android.content.Context;
import android.graphics.Canvas;
import android.widget.TextView;

public class ConfChatTextView extends TextView {

	public ConfChatTextView(Context context) {
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
