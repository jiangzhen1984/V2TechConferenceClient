package com.v2tech.view.conversation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
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
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.db.ContentDescriptor;
import com.v2tech.vo.User;
import com.v2tech.vo.VImageMessage;
import com.v2tech.vo.VMessage;

public class ImageViewGallery extends FragmentActivity {

	private View mReturnButton;
	
	private ViewPager mImageViewPager;

	private List<ListItem> vimList;
	
	private long initMid;
	
	private int initPos;
	
	private TextView mTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_view_gallery);

		mImageViewPager = (ViewPager) findViewById(R.id.image_view_view_pager);
		initMid = getIntent().getLongExtra("cid", 0);
		loadImages(getIntent().getLongExtra("uid1", 0), getIntent()
				.getLongExtra("uid2", 0));
	
		mTitle = (TextView)findViewById(R.id.image_galley_title);
		mReturnButton = findViewById(R.id.image_galley_detail_return_button);
		mReturnButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onBackPressed();
			}
			
		});
		
		mImageViewPager.setAdapter(new ImageAdapter(this.getSupportFragmentManager()));
		mImageViewPager.setOffscreenPageLimit(1);
		
		mImageViewPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrollStateChanged(int arg0) {
				
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				
			}

			@Override
			public void onPageSelected(int pos) {
				mTitle.setText((pos+1)+"/"+vimList.size());
			}
			
		});
		mTitle.setText((initPos+1)+"/"+vimList.size());
		mImageViewPager.setCurrentItem(initPos);
	}

	private void loadImages(long user1Id, long user2Id) {
		
		if (user1Id <= 0 || user2Id <= 0) {
			Toast.makeText(ImageViewGallery.this, "invalid user id",
					Toast.LENGTH_SHORT).show();
			return;
		}
		vimList = new ArrayList<ListItem>();
		String selection = "((" + ContentDescriptor.Messages.Cols.FROM_USER_ID
				+ "=? and " + ContentDescriptor.Messages.Cols.TO_USER_ID
				+ "=? ) or " + "("
				+ ContentDescriptor.Messages.Cols.FROM_USER_ID + "=? and "
				+ ContentDescriptor.Messages.Cols.TO_USER_ID + "=? )) and "
				+ ContentDescriptor.Messages.Cols.MSG_TYPE + "=?";
		String[] args = new String[] { user1Id + "", user2Id + "",
				user2Id + "", user1Id + "", VMessage.MessageType.IMAGE.getIntValue() + "" };

		Cursor mCur = this.getContentResolver()
				.query(ContentDescriptor.Messages.CONTENT_URI,
						ContentDescriptor.Messages.Cols.ALL_CLOS, selection,
						args, null);

		if (mCur.getCount() == 0) {
			mCur.close();
			return;
		}
		int index =0;
		while (mCur.moveToNext()) {
			VMessage vm = extractMsg(mCur, user1Id, user2Id);
			vimList.add(new ListItem((VImageMessage) vm));
			if (vm.getId() == initMid) {
				initPos = index;
			}
			index++;
		}
		mCur.close();
	}

	private VMessage extractMsg(Cursor cur, long user1Id, long user2Id) {
		if (cur.isClosed()) {
			throw new RuntimeException(" cursor is closed");
		}
		DateFormat dp = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		User localUser = new User(user1Id);
		User remoteUser = new User(user2Id);

		int id = cur.getInt(0);
		long localUserId = cur.getLong(1);
		// msg_content column
		String content = cur.getString(5);
		// message type
		int type = cur.getInt(6);
		// date time
		String dateString = cur.getString(7);

		VMessage vm = null;
		if (type == VMessage.MessageType.TEXT.getIntValue()) {
			vm = new VMessage(localUser, remoteUser, content,
					localUserId == user2Id);
		} else {
			vm = new VImageMessage(localUser, remoteUser,
					content.split("\\|")[4], localUserId == user2Id);
		}
		vm.setId(id);
		try {
			vm.setDate(dp.parse(dateString));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return vm;

	}

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
			return vimList.size();
		}

	}

	class ListItem {
		VImageMessage vm;
		PlaceSlideFragment frg;

		public ListItem(VImageMessage vm) {
			super();
			this.vm = vm;
			frg = new PlaceSlideFragment();
			frg.setMessage(vm);
			//frg.setBitmap(vm.getFullQuantityBitmap());
		}

	}
	


}
