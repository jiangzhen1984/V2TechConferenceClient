package com.v2tech.view.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Implementation Notes:
 *
 * Some terminology:
 *
 *     index    - index of the items that are currently visible
 *     position - index of the items in the cursor
 */

/**
 * A view that shows items in a vertically scrolling list. The items come from
 * the {@link ListAdapter} associated with this view.
 * 
 * <p>
 * See the <a href="{@docRoot}guide/topics/ui/layout/listview.html">List
 * View</a> guide.
 * </p>
 * 
 * @attr ref android.R.styleable#ListView_entries
 * @attr ref android.R.styleable#ListView_divider
 * @attr ref android.R.styleable#ListView_dividerHeight
 * @attr ref android.R.styleable#ListView_headerDividersEnabled
 * @attr ref android.R.styleable#ListView_footerDividersEnabled
 */
public class HorizontalListView extends AdapterView<ListAdapter> {

	ListAdapter mAdapter;

	public HorizontalListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public HorizontalListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public HorizontalListView(Context context) {
		super(context);
	}

	@Override
	public ListAdapter getAdapter() {
		return mAdapter;
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		mAdapter = adapter;
		requestLayout();
	}

	@Override
	public View getSelectedView() {
		return null;
	}

	@Override
	public void setSelection(int position) {

	}

	int mLastX = 0;
	int mOffsetX = 0;
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		int type = ev.getAction();
		switch (type) {
		case MotionEvent.ACTION_DOWN:
			mLastX = (int) ev.getX();
			break;
		case MotionEvent.ACTION_MOVE:
			mOffsetX = (int)ev.getX() - mLastX;
			break;
		}
		this.offsetLeftAndRight(mOffsetX);
		this.invalidate();
		return true;
	}

	
	  // used for temporary calculations.
    private final Rect mTempRect = new Rect();
    
	@SuppressLint("NewApi")
	@Override
	protected void dispatchDraw(Canvas canvas) {
	
//		int count = getChildCount();
//		int lastLeft = 0;
//		Paint mDividerPaint = new Paint();
//		for (int i =0; i < count; i++)  {
//			View child = this.getChildAt(i);
//			Rect bounds = mTempRect;
//			bounds.top = this.getTop();
//			bounds.bottom = this.getBottom();
//			bounds.left = lastLeft;
//			if (child.getMeasuredWidth() <= 0) {
//				child.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
//			}
//			bounds.right = bounds.left + child.getMeasuredWidth();
//			lastLeft += bounds.right;
//			child.setLeft(bounds.left);
//			child.setRight(bounds.right);
//			canvas.drawRect(bounds, mDividerPaint);
//		}
		super.dispatchDraw(canvas);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}

	public void draw(Canvas canvas) {
		super.draw(canvas);
	}

	int mFirstPositionIndex = 0;
	boolean loaded = false;

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {

		for (int i = mFirstPositionIndex; i < mAdapter.getCount() && !loaded; i++) {
			View child = mAdapter.getView(i, null, this);
			child.forceLayout();
			addViewInLayout(child, 0, new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.MATCH_PARENT), false);
			int childWidth = child.getWidth();
			if (child.getMeasuredWidth() <= 0) {
				child.measure(right, top);
				childWidth = child.getMeasuredWidth();
				
			} 
			child.layout(left + childWidth * i, top, left + childWidth * (i +1) ,bottom);
		}
		invalidate();
		loaded = true;
		super.onLayout(changed, left, top, right, bottom);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		int mItemCount = mAdapter == null ? 0 : mAdapter.getCount();
		int childWidthSize = 0;
		if (mItemCount > 0) {
			View child = mAdapter.getView(0, null, this);
			child.measure(widthMeasureSpec, heightMeasureSpec);
			childWidthSize = child.getMeasuredWidth();
			heightSize = child.getMeasuredHeight();
		}
		for (int i = 0; i < mItemCount; i++) {
			widthSize += childWidthSize;
		}
		setMeasuredDimension(widthSize, heightSize);

	}

}
