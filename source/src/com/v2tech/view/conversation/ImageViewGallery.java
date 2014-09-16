package com.v2tech.view.conversation;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.vo.Conversation;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageImageItem;

public class ImageViewGallery extends FragmentActivity {

	private View mReturnButton;

	private ViewPager mImageViewPager;

	private List<ListItem> vimList;

	private long initMid;

	private int initPos;

	private TextView mTitle;

	private ImageAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_view_gallery);

		mImageViewPager = (ViewPager) findViewById(R.id.image_view_view_pager);
		initMid = getIntent().getLongExtra("cid", 0);
		int type = getIntent().getIntExtra("type", 0);
		if (type == 1) {
			long gid = getIntent().getLongExtra("gid", 0);
			loadImages(gid); 
		} else {
			loadImages(getIntent().getLongExtra("uid1", 0), getIntent()
					.getLongExtra("uid2", 0));
		}

		mTitle = (TextView) findViewById(R.id.image_galley_title);
		mReturnButton = findViewById(R.id.image_galley_detail_return_button);
		mReturnButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onBackPressed();
			}

		});

		mImageViewPager.setOffscreenPageLimit(1);

		mImageViewPager.setOnPageChangeListener(pageChangeListener);
		mTitle.setText((initPos + 1) + "/" + vimList.size());
		mImageViewPager.setCurrentItem(initPos);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		vimList.clear();
	}

	/**
	 * Load group image message list
	 * 
	 * @param groupId
	 */
	private void loadImages(long groupId) {
		List<VMessage> list = MessageLoader
				.loadGroupImageMessage(this, Long.valueOf(Conversation.TYPE_GROUP) , groupId);
		populateImageMessage(list);
	}

	private void loadImages(long user1Id, long user2Id) {
		List<VMessage> list = MessageLoader.loadImageMessage(this, user1Id,
				user2Id);
		populateImageMessage(list);
	}

	private void populateImageMessage(List<VMessage> list) {
		boolean flag = false;
		vimList = new ArrayList<ListItem>();
		for (VMessage vm : list) {
			List<VMessageImageItem> items = vm.getImageItems();
			for (VMessageImageItem item : items) {
				vimList.add(new ListItem(item));
				if (vm.getId() == this.initMid) {
					flag = true;
				} 
				if (!flag) {
					initPos++;
				}
			}
			
		}

		adapter = new ImageAdapter(this.getSupportFragmentManager());
		mImageViewPager.setAdapter(adapter);
	}

	private OnPageChangeListener pageChangeListener = new OnPageChangeListener() {

		@Override
		public void onPageScrollStateChanged(int arg0) {

		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {

		}

		@Override
		public void onPageSelected(int pos) {
			mTitle.setText((pos + 1) + "/" + vimList.size());
		}

	};

	class ImageAdapter extends FragmentPagerAdapter {

		public ImageAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int pos) {
			return vimList.get(pos).frg;
		}

		@Override
		public int getCount() {
			return vimList == null ? 0 : vimList.size();
		}

	}

	class ListItem {
		VMessageImageItem vm;
		PlaceSlideFragment frg;

		public ListItem(VMessageImageItem vm) {
			super();
			this.vm = vm;
			frg = new PlaceSlideFragment();
			frg.setMessage(vm);
			// frg.setBitmap(vm.getFullQuantityBitmap());
		}

	}

}
