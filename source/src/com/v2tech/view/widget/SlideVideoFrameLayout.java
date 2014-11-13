package com.v2tech.view.widget;

import com.v2tech.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class SlideVideoFrameLayout extends FrameLayout {

	public int flag = 0;
	public int lastRight = 0;

	public SlideVideoFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		measureChildren(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(widthSize, heightSize);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		if (flag == 1) {
			lastRight = right;
			flag++;
			super.onLayout(changed, left, top, right, bottom);
		} else {
			if (flag == 0) {
				flag++;
			}
			int childCount = getChildCount();
			for (int i = 0; i < childCount; i++) {
				View childView = getChildAt(i);
				int measureHeight = childView.getMeasuredHeight();
				int measuredWidth = childView.getMeasuredWidth();
				int childViewLeft = childView.getLeft();
				if (childView.getId() == R.id.small_window_video_layout) {
					childViewLeft = childViewLeft + (right - lastRight);
					if (childViewLeft < 0) {
						childViewLeft = 0;
					}
				}

				childView.layout(childViewLeft, childView.getTop(),
						childViewLeft + measuredWidth, childView.getTop()
								+ measureHeight);

			}

			lastRight = right;

		}

	}

	public static class TouchMoveListener implements OnTouchListener {
		private final int LENGTH = 5;
		int lastX;
		int lastY;
		int x1 = 0;
		int y1 = 0;
		boolean isNotClick;
		int screenWidth;
		int screenHeight;

		public TouchMoveListener(Context context) {
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			screenWidth = ((View) (v.getParent())).getWidth();
			screenHeight = ((View) (v.getParent())).getHeight();
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				isNotClick = false;
				lastX = (int) event.getRawX();
				lastY = (int) event.getRawY();
				x1 = lastX;
				y1 = lastY;
				break;
			case MotionEvent.ACTION_MOVE:
				int dx = (int) event.getRawX() - lastX;
				int dy = (int) event.getRawY() - lastY;

				if (event.getPointerCount() == 1) {
					if (Math.abs((event.getRawX() - x1)) > LENGTH
							|| Math.abs((event.getRawY() - y1)) > LENGTH) {
						isNotClick = true;// 是点击事件。
					}
				}

				int left = v.getLeft() + dx;
				int top = v.getTop() + dy;
				int right = v.getRight() + dx;
				int bottom = v.getBottom() + dy;
				// set bound
				if (left < 0) {
					left = 0;
					right = left + v.getWidth();
				}
				if (right > screenWidth) {
					right = screenWidth;
					left = right - v.getWidth();
				}
				if (top < 0) {
					top = 0;
					bottom = top + v.getHeight();
				}
				if (bottom > screenHeight) {
					bottom = screenHeight;
					top = bottom - v.getHeight();
				}
				v.layout(left, top, right, bottom);
				lastX = (int) event.getRawX();
				lastY = (int) event.getRawY();
				break;
			case MotionEvent.ACTION_UP:
				if (event.getPointerCount() == 1) {
					if (Math.abs((event.getRawX() - x1)) > LENGTH
							|| Math.abs((event.getRawY() - y1)) > LENGTH) {
						isNotClick = true;
					}
				}

				break;
			}
			return isNotClick;

		}
	}
}
