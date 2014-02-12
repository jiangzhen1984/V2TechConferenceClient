package com.v2tech.view.cus;

public interface ScrollViewListener {
	
	
	void onScrollBottom(ItemScrollView scrollView, int x, int y, int oldx,
			int oldy);

	void onScrollTop(ItemScrollView scrollView, int x, int y, int oldx, int oldy);

}
