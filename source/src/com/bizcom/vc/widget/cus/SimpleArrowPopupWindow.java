package com.bizcom.vc.widget.cus;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

public class SimpleArrowPopupWindow extends PopupWindow {

	private RelativeLayout mRootView;
	private View mContent;
	private View mArrow;
	private WindowManager mWindowManager;

	public SimpleArrowPopupWindow(Context context) {
		super(context);
	}

	public SimpleArrowPopupWindow(View contentView) {
		super(contentView);
	}

	public SimpleArrowPopupWindow(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SimpleArrowPopupWindow(int width, int height) {
		super(width, height);
	}

	public SimpleArrowPopupWindow(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	public SimpleArrowPopupWindow(View contentView, int width, int height) {
		super(contentView, width, height);
	}

	public SimpleArrowPopupWindow(Context context, AttributeSet attrs,
			int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public SimpleArrowPopupWindow(View contentView, View arrow, int width,
			int height) {
		mRootView = new RelativeLayout(contentView.getContext());
		mContent = contentView;
		if (mContent.getId() == View.NO_ID) {
			mContent.setId(mContent.hashCode());
		}
		setContentView(mRootView);
		setWidth(width);
		setHeight(height);
		this.mArrow = arrow;
		if (this.mArrow.getId() == View.NO_ID) {
			this.mArrow.setId(this.mArrow.getId());
		}
		
		RelativeLayout.LayoutParams arrowrl = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		mRootView.addView(this.mArrow, arrowrl);
		
		RelativeLayout.LayoutParams contentrl = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		mRootView.addView(this.mContent, contentrl);
		
		
	}

	@Override
	public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
		if (mArrow == null) {
			super.showAsDropDown(anchor, xoff, yoff);
			return;
		}
		if (mContent.getWidth() <= 0 || mContent.getHeight() <= 0) {
			mContent.measure(View.MeasureSpec.UNSPECIFIED,
					View.MeasureSpec.UNSPECIFIED);
		}
		if (mArrow.getWidth() <= 0 || mArrow.getHeight() <= 0) {
			mArrow.measure(View.MeasureSpec.UNSPECIFIED,
					View.MeasureSpec.UNSPECIFIED);
		}
		int[] location = new int[2];
		anchor.getLocationInWindow(location);
		DisplayMetrics dm = new DisplayMetrics();
		mWindowManager.getDefaultDisplay().getMetrics(dm);

		RelativeLayout.LayoutParams arrowRL = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		if ((gravity & Gravity.TOP) == Gravity.TOP) {
			arrowRL.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		}
		
		mRootView.updateViewLayout(this.mArrow, arrowRL);

	}

}
