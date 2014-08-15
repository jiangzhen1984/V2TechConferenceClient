package com.v2tech.view.conversation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.util.BitmapUtil;
import com.v2tech.util.V2Log;

public class ConversationSelectImage extends Activity {

	private static final String TAG = "ConversationSelectImage";
	protected static final int CANCEL_SELECT_PICTURE = 0;
	private static final int SCROLL_STATE_TOUCH_SCROLL = 1;
	private static final int UPDATE_BITMAP = 3;
	protected static final int SCAN_SDCARD = 4;
	private RelativeLayout buttomTitle;
	private LinearLayout loading;
	private TextView backButton;
	private TextView finishButton;
	private TextView title;
	private GridView gridViews;
	private ListView listViews;
	private ImageListAdapter imageAdapter;
	private ArrayList<FileInfoBean> pictres;
	private String[][] selectArgs = {{String.valueOf(MediaStore.Images.Media.INTERNAL_CONTENT_URI) , "image/png"} ,
			{String.valueOf(MediaStore.Images.Media.INTERNAL_CONTENT_URI) , "image/jpeg"} ,
			{String.valueOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI) , "image/png"} ,
			{String.valueOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI) , "image/jpeg"}};

	private final int LRU_MAX_MEMORY = (int) ((Runtime.getRuntime().maxMemory()) / 8);
	private LruCache<String, Bitmap> lruCache = new LruCache<String, Bitmap>(
			LRU_MAX_MEMORY) {

		@Override
		protected void entryRemoved(boolean evicted, String key,
				Bitmap oldValue, Bitmap newValue) {
			if (key != null) {
				if (lruCache != null) {
					Bitmap bm = lruCache.remove(key);
					if (bm != null) {
						bm.recycle();
						bm = null;
					}
				}
			}
		}
	};

	private Handler handler = new Handler() {

		public void handleMessage(Message msg) {

			switch (msg.what) {
				case UPDATE_BITMAP:
					ViewHolder holder = (ViewHolder) ((Object[]) msg.obj)[0];
					Bitmap bt = (Bitmap) ((Object[]) msg.obj)[1];
					FileInfoBean fb = (FileInfoBean) ((Object[]) msg.obj)[2];
					if (!bt.isRecycled()) {
						holder.fileIcon.setImageBitmap(bt);
					} else {
						startLoadBitmap(holder, fb);
					}
					break;
				case SCAN_SDCARD:
					loading.setVisibility(View.GONE);
					imageAdapter = new ImageListAdapter();
					gridViews.setAdapter(imageAdapter);
			}
		}
	};
	private int mScreenHeight;
	private int mScreenWidth;
	protected int isLoading;
	private ExecutorService service;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_selectfile);

		Display display = getWindowManager().getDefaultDisplay();
		mScreenHeight = display.getHeight();
		mScreenWidth = display.getWidth();
		findview();
		init();
		setListener();
	}

	private void findview() {

		backButton = (TextView) findViewById(R.id.selectfile_back);
		finishButton = (TextView) findViewById(R.id.selectfile_finish);
		title = (TextView) findViewById(R.id.selectfile_title);
		buttomTitle = (RelativeLayout) findViewById(R.id.activity_selectfile_buttom);
		gridViews = (GridView) findViewById(R.id.selectfile_gridview);
		listViews = (ListView) findViewById(R.id.selectfile_lsitview);
		loading = (LinearLayout) findViewById(R.id.selectfile_loading);
	}

	private void init() {

		title.setText("图片");
		listViews.setVisibility(View.GONE);
		gridViews.setVisibility(View.VISIBLE);
		finishButton.setVisibility(View.INVISIBLE);
		buttomTitle.setVisibility(View.INVISIBLE);
		service = Executors.newCachedThreadPool();
		pictres = new ArrayList<FileInfoBean>();
		new Thread(new Runnable() {

			@Override
			public void run() {
				loading.setVisibility(View.VISIBLE);
				for (int i = 0; i < selectArgs.length; i++) {
					
					initPictures(Uri.parse(selectArgs[i][0]) , selectArgs[i][1]);
				}
				handler.sendEmptyMessage(SCAN_SDCARD);

			}
		}).start();
	}

	private void initPictures(Uri uri , String select) {

		ContentResolver resolver = getContentResolver();
		String[] projection = { MediaStore.Images.Media._ID,
				MediaStore.Images.Media.DISPLAY_NAME,
				MediaStore.Images.Media.DATA, MediaStore.Images.Media.SIZE };
		String selection = MediaStore.Images.Media.MIME_TYPE + "=?";
		String[] selectionArgs = { select };
		String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " desc";
		Cursor cursor = resolver.query(uri, projection, selection,
				selectionArgs, sortOrder);
		if (cursor != null) {
			FileInfoBean bean = null;
			while (cursor.moveToNext()) {
				bean = new FileInfoBean();
				String filePath = cursor.getString(cursor
						.getColumnIndex(MediaStore.Images.Media.DATA));
				bean.filePath = filePath;
				bean.fileSize = Long.valueOf(cursor.getString(cursor
						.getColumnIndex(MediaStore.Images.Media.SIZE)));
				bean.fileName = cursor.getString(cursor
						.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
				bean.fileType = 1;
				pictres.add(bean);
				bean = null;
			}
			cursor.close();
		}
	}

	private void setListener() {

		backButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				finish();
				setResult(CANCEL_SELECT_PICTURE);
			}
		});

		gridViews.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {

				isLoading = scrollState;
				if (scrollState == 0) {
					imageAdapter.notifyDataSetChanged();
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {

			}
		});

		gridViews.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent();
				intent.putExtra("checkedImage", pictres.get(position).filePath);
				setResult(100, intent);
				finish();
			}
		});
	}

	class ImageListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return pictres.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {

			final ViewHolder holder;
			if (convertView == null) {

				holder = new ViewHolder();
				convertView = View.inflate(ConversationSelectImage.this,
						R.layout.activity_imagefile_adapter, null);
				holder.fileIcon = (ImageView) convertView
						.findViewById(R.id.selectfile_adapter_image);
				holder.fileCheck = (CheckBox) convertView
						.findViewById(R.id.selectfile_adapter_check);
				convertView.setTag(holder);
			} else {

				holder = (ViewHolder) convertView.getTag();
			}
			holder.fileCheck.setVisibility(View.INVISIBLE);
			LayoutParams para = holder.fileIcon.getLayoutParams();

			Configuration conf = getResources().getConfiguration();
			if (conf.smallestScreenWidthDp >= 600) {
				para.height = mScreenHeight / 3;//
			} else {
				para.height = mScreenHeight / 4;//
			}
			para.width = (mScreenWidth - 20) / 3;// 一屏显示3列
			holder.fileIcon.setLayoutParams(para);

			if (pictres.size() <= 0) {
				V2Log.e(TAG, "error mFileLists size zero");
			}
			final FileInfoBean fb = pictres.get(position);
			Bitmap bit = lruCache.get(fb.fileName);
			if (bit == null || bit.isRecycled()) {

				if (isLoading != SCROLL_STATE_TOUCH_SCROLL && isLoading != 1)
					// 开始加载图片
					startLoadBitmap(holder, fb);
				else
					// 加载中显示的图片
					holder.fileIcon.setImageResource(R.drawable.ic_launcher);

			} else {

				holder.fileIcon.setImageBitmap(bit);
			}
			return convertView;
		}

	}

	class ViewHolder {

		public ImageView fileIcon;
		public CheckBox fileCheck;

	}

	public void startLoadBitmap(final ViewHolder holder, final FileInfoBean fb) {
		service.execute(new Runnable() {

			@Override
			public void run() {
				try {

					Bitmap bitmap = BitmapUtil.getCompressedBitmap(fb.filePath);
					if (fb.fileName == null && bitmap != null) {
						if (!bitmap.isRecycled()) {  
							bitmap.recycle();
							bitmap = null;
						} 
						return;
					}
					
					if(bitmap == null){
						V2Log.e(TAG, "get null when loading "+fb.fileName+" picture.");
						return ;
					}

					lruCache.put(fb.fileName, bitmap);
					Message.obtain(handler, UPDATE_BITMAP,
							new Object[] { holder, bitmap, fb }).sendToTarget();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
