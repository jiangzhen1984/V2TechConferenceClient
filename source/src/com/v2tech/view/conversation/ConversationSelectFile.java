package com.v2tech.view.conversation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
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
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.view.Display;
import android.view.Gravity;
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
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.util.StorageUtil;
import com.v2tech.util.V2Log;

public class ConversationSelectFile extends Activity {

	protected static final String TAG = "ConversationSelectFile";
	protected static final int NEW_BITMAP = 5;
	protected static final int SCAN_SDCARD = 6;
	protected static final int UPDATE_BITMAP = 7;
	protected static final int REMOVE_BITMAP = 8;
	protected static final int SCROLL_STATE_TOUCH_SCROLL = 2;
	private static final int ITEM_CHECKED = 7;
	private static final int ITEM_UNCHECKED = 8;
	private String mCurrentPath = StorageUtil.getSdcardPath();
	private TextView backButton;
	private TextView finishButton;
	private TextView titleText;
	private TextView selectedFileSize;
	private TextView sendButton;

	private ListView filesList;
	private GridView filesGrid;

	private int totalSize = 0;

	private ArrayList<FileInfoBean> mFileLists;
	private ArrayList<FileInfoBean> mFolderLists;
	private ArrayList<FileInfoBean> mCheckedList;
	private ArrayList<String> mCheckedNameList;
	private LruCache<String, Bitmap> bitmapLru;
	private HashMap<Bitmap, ViewHolder> bitmapMapping;

	private FileListAdapter adapter;
	private ImageListAdapter imageAdapter;
	private String type;
	private int mScreenHeight;
	private int mScreenWidth;
	private Handler handler = new Handler() {

		public void handleMessage(Message msg) {

			switch (msg.what) {
			case SCAN_SDCARD:
				loading.setVisibility(View.GONE);
				imageAdapter = new ImageListAdapter();
				filesGrid.setAdapter(imageAdapter);
				break;
			case NEW_BITMAP:
				imageAdapter.notifyDataSetChanged();
				break;

			case UPDATE_BITMAP:
				ViewHolder holder = (ViewHolder) ((Object[]) msg.obj)[0];
				Bitmap bt = (Bitmap) ((Object[]) msg.obj)[1];
				if (!bt.isRecycled()) {
					holder.fileIcon.setImageBitmap(bt);
					bitmapMapping.put(bt, holder);
				} else {
					// TODO reload
				}
				break;
			case REMOVE_BITMAP:
				ViewHolder cach = bitmapMapping.remove(msg.obj);
				if (cach != null) {
					cach.fileIcon.setImageResource(R.drawable.ic_launcher);
				}
				((Bitmap) msg.obj).recycle();
				break;
			default:
				break;
			}
		};
	};
	private LinearLayout loading;
	private ExecutorService service;
	protected int isLoading;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_selectfile);
		findview();
		init();
		setListener();
		Display display = getWindowManager().getDefaultDisplay();
		mScreenHeight = display.getHeight();
		mScreenWidth = display.getWidth();
	}

	private void findview() {

		titleText = (TextView) findViewById(R.id.selectfile_title);
		selectedFileSize = (TextView) findViewById(R.id.selectfile_entry_size);
		sendButton = (TextView) findViewById(R.id.selectfile_message_send);
		backButton = (TextView) findViewById(R.id.selectfile_back);
		finishButton = (TextView) findViewById(R.id.selectfile_finish);
		loading = (LinearLayout) findViewById(R.id.selectfile_loading);

		filesGrid = (GridView) findViewById(R.id.selectfile_gridview);
		filesList = (ListView) findViewById(R.id.selectfile_lsitview);

	}

	private void init() {
		// 设置LRU集合的大小
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int mCacheSize = maxMemory / 8;
		bitmapLru = new BitmapLRU(mCacheSize);
		bitmapMapping = new HashMap<Bitmap, ViewHolder>();
		// 获取Intent携带的数据
		Intent intent = getIntent();
		mCheckedList = intent.getParcelableArrayListExtra("checkedFiles");
		type = intent.getStringExtra("type");
		// 创建初始对象
		mCheckedNameList = new ArrayList<String>();
		service = Executors.newCachedThreadPool();

		if (mCheckedList.size() > 0) {
			changeSendAble();
			for (FileInfoBean bean : mCheckedList) {

				bean.isCheck = ITEM_CHECKED;
				mCheckedNameList.add(bean.fileName);
				totalSize += bean.fileSize;
			}
			selectedFileSize.setText("已选" + getFileSize(totalSize));
			sendButton.setText("发送(" + mCheckedList.size() + ")");
		}

		if ("image".equals(type)) {
			titleText.setText("图片");
			filesList.setVisibility(View.GONE);
			filesGrid.setVisibility(View.VISIBLE);
			new Thread(new Runnable() {

				@Override
				public void run() {
					loading.setVisibility(View.VISIBLE);
					getSdcardImages();
					handler.sendEmptyMessage(SCAN_SDCARD);

				}
			}).start();

		} else if ("file".equals(type)) {
			titleText.setText("文件");
			filesGrid.setVisibility(View.GONE);
			filesList.setVisibility(View.VISIBLE);

			updateFileItems(mCurrentPath);
			adapter = new FileListAdapter();
			filesList.setAdapter(adapter);
		}

	}

	/**
	 * 发送按钮可用
	 */
	private void changeSendAble() {
		sendButton.setClickable(true);
		sendButton.setTextColor(Color.WHITE);
		sendButton
				.setBackgroundResource(R.drawable.conversation_selectfile_send_able);
		sendButton.setGravity(Gravity.CENTER);
	}

	/**
	 * 发送按钮不可用
	 */
	private void changeSendUnable() {
		sendButton.setClickable(false);
		sendButton.setTextColor(Color.GRAY);
		sendButton.setBackgroundResource(R.drawable.button_bg_noable);
		sendButton.setGravity(Gravity.CENTER);
	}

	private void setListener() {

		backButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				V2Log.d(TAG, "当前文件路径：" + mCurrentPath);
				if (StorageUtil.getSdcardPath()
						.equals(mCurrentPath.substring(0,
								mCurrentPath.lastIndexOf("/")))) {
					backButton.setText("返回");
				}

				if (StorageUtil.getSdcardPath().equals(mCurrentPath)) {

					// 如果已经是顶级路径，则结束掉当前界面
					reutrnResult(1);
					return;
				}
				File file = new File(mCurrentPath);
				mCurrentPath = file.getParent();
				updateFileItems(file.getParent());
				adapter.notifyDataSetChanged();
			}

		});

		sendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				reutrnResult(3);
			}
		});

		finishButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mCheckedList.clear();
				reutrnResult(0);
			}

		});

		filesList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				if (position >= mFolderLists.size()) { // 文件

					notifyListChange(view, position);

				} else { // position小于文件夹集合mFolderLists的长度，则就是文件夹

					FileInfoBean bean = mFolderLists.get(position);
					backButton.setText("上一级");
					mCurrentPath = bean.filePath;
					V2Log.d(TAG, "当前文件路径：" + mCurrentPath);
					updateFileItems(bean.filePath);
				}
				adapter.notifyDataSetChanged();
			}

		});

		filesGrid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				notifyListChange(view, position);
				imageAdapter.notifyDataSetChanged();
			}
		});

		filesGrid.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {

				isLoading = scrollState;
				if (scrollState == 0) {
					imageAdapter.notifyDataSetChanged();
				}
				V2Log.d(TAG, "当前状态：" + scrollState);
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {

			}
		});

	}

	/**
	 * @description:通过contentprovider获得sd卡上的图片
	 * @return:void
	 */
	private void getSdcardImages() {

		if (mFileLists == null) {
			mFileLists = new ArrayList<FileInfoBean>();
		} else {
			mFileLists.clear();
		}
		// 指定要查询的uri资源
		Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		// 获取ContentResolver
		ContentResolver contentResolver = getContentResolver();
		// 查询的字段
		String[] projection = { MediaStore.Images.Media._ID,
				MediaStore.Images.Media.DISPLAY_NAME,
				MediaStore.Images.Media.DATA, MediaStore.Images.Media.SIZE };
		// 条件
		String selection = MediaStore.Images.Media.MIME_TYPE + "=?";
		// 条件值(這裡的参数不是图片的格式，而是标准，所有不要改动)
		String[] selectionArgs = { "image/jpeg" };
		// 排序
		String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " desc";
		// 查询sd卡上的图片
		Cursor cursor = contentResolver.query(uri, projection, selection,
				selectionArgs, sortOrder);
		if (cursor != null) {
			FileInfoBean bean = null;
			cursor.moveToFirst();
			while (cursor.moveToNext()) {
				bean = new FileInfoBean();
				// 获得图片uri
				bean.filePath = cursor.getString(cursor
						.getColumnIndex(MediaStore.Images.Media.DATA));
				bean.fileSize = Long.valueOf(cursor.getString(cursor
						.getColumnIndex(MediaStore.Images.Media.SIZE)));
				bean.fileName = cursor.getString(cursor
						.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
				if (mCheckedNameList.contains(bean.fileName)) {
					bean.isCheck = ITEM_CHECKED;
				}
				mFileLists.add(bean);
			}
			// 关闭cursor
			cursor.close();
		}
	}

	/**
	 * 根据文件路径更新当前ListView
	 * 
	 * @param path
	 */
	private void updateFileItems(String path) {

		if (mFileLists == null) {
			mFileLists = new ArrayList<FileInfoBean>();
			mFolderLists = new ArrayList<FileInfoBean>();
		} else {
			mFileLists.clear();
			mFolderLists.clear();
		}

		File[] files = folderScan(path);
		if (files == null) {
			return;
		}

		FileInfoBean file = null;
		File currentFile;
		for (int i = 0; i < files.length; i++) {

			if (files[i].isHidden() && !files[i].canRead()) {
				continue;
			}

			currentFile = files[i];
			file = new FileInfoBean();

			file.fileName = currentFile.getName();
			file.filePath = currentFile.getAbsolutePath();
			file.fileSize = currentFile.length();
			if (currentFile.isDirectory()) {
				mFolderLists.add(file);
				file.isDir = true;
			} else {
				adapterFileIcon(file.fileName, null, file);
				if (mCheckedNameList.contains(file.fileName)) {
					file.isCheck = ITEM_CHECKED;
				}
				mFileLists.add(file);
				file.isDir = false;
			}
		}
	}

	/**
	 * 获得当前路径的所有文件
	 * 
	 * @param path
	 * @return
	 */
	private File[] folderScan(String path) {

		File file = new File(path);
		File[] files = file.listFiles();
		return files;
	}

	/**
	 * 获取文件大小
	 * 
	 * @param totalSpace
	 * @return
	 */
	private static String getFileSize(long totalSpace) {

		BigDecimal filesize = new BigDecimal(totalSpace);
		BigDecimal megabyte = new BigDecimal(1024 * 1024);
		float returnValue = filesize.divide(megabyte, 2, BigDecimal.ROUND_UP)
				.floatValue();
		if (returnValue > 1)
			return (returnValue + "MB");
		BigDecimal kilobyte = new BigDecimal(1024);
		returnValue = filesize.divide(kilobyte, 2, BigDecimal.ROUND_UP)
				.floatValue();
		return (returnValue + "  KB ");
	}

	class FileListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mFileLists.size() + mFolderLists.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			final ViewHolder holder;
			if (convertView == null) {

				holder = new ViewHolder();
				convertView = View.inflate(ConversationSelectFile.this,
						R.layout.activity_selectfile_adapter, null);
				holder.fileIcon = (ImageView) convertView
						.findViewById(R.id.selectfile_adapter_icon);
				holder.fileFolderName = (TextView) convertView
						.findViewById(R.id.selectfile_adapter_folderName);
				holder.fileCheck = (CheckBox) convertView
						.findViewById(R.id.selectfile_adapter_checkbox);
				holder.fileArrow = (ImageView) convertView
						.findViewById(R.id.selectfile_adapter_arrow);
				convertView.setTag(holder);
			} else {

				holder = (ViewHolder) convertView.getTag();
			}

			if (position >= mFolderLists.size()) {

				if (mFileLists.get(position - mFolderLists.size()).isCheck == ITEM_CHECKED) {
					holder.fileCheck.setChecked(true);
				} else {
					holder.fileCheck.setChecked(false);
				}

				adapterFileIcon(
						mFileLists.get(position - mFolderLists.size()).fileName,
						holder, null);
				holder.fileCheck.setVisibility(View.VISIBLE);
				holder.fileArrow.setVisibility(View.GONE);
				holder.fileFolderName.setText(mFileLists.get(position
						- mFolderLists.size()).fileName);
			} else {

				holder.fileIcon.setImageResource(R.drawable.selectfile_folder);
				holder.fileFolderName
						.setText(mFolderLists.get(position).fileName);
				holder.fileCheck.setVisibility(View.GONE);
				holder.fileArrow.setVisibility(View.VISIBLE);
			}

			return convertView;
		}
	}

	class ImageListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mFileLists.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {

			final ViewHolder holder;
			if (convertView == null) {

				holder = new ViewHolder();
				convertView = View.inflate(ConversationSelectFile.this,
						R.layout.activity_imagefile_adapter, null);
				holder.fileIcon = (ImageView) convertView
						.findViewById(R.id.selectfile_adapter_image);
				holder.fileCheck = (CheckBox) convertView
						.findViewById(R.id.selectfile_adapter_check);
				convertView.setTag(holder);
			} else {

				holder = (ViewHolder) convertView.getTag();
			}

			LayoutParams para = holder.fileIcon.getLayoutParams();

			Configuration conf = getResources().getConfiguration();
			if (conf.smallestScreenWidthDp >= 600) {
				para.height = mScreenHeight / 3;//
			} else {
				para.height = mScreenHeight / 6;//
			}
			para.width = (mScreenWidth - 20) / 3;// 一屏显示3列
			holder.fileIcon.setLayoutParams(para);

			final FileInfoBean fb = mFileLists.get(position);
			Bitmap bit = bitmapLru.get(fb.fileName);
			if (bit == null || bit.isRecycled()) {

				if (isLoading != SCROLL_STATE_TOUCH_SCROLL && isLoading != 1) {
					new Thread() {
						public void run() {
							service.execute(new Runnable() {

								@Override
								public void run() {

									try {

										Bitmap bitmap = handlerImage(fb.filePath);
										if (fb.fileName == null
												&& bitmap != null) {
											bitmap.recycle();
											return;
										}

										if (bitmap == null) {
											V2Log.e("Can not extra bitmap ");
											return;
										}
										bitmapLru.put(fb.fileName, bitmap);
										Message.obtain(handler, UPDATE_BITMAP,
												new Object[] { holder, bitmap })
												.sendToTarget();
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							});
						}
					}.start();
					;

				} else {
					// 加载中显示的图片
					holder.fileIcon.setImageResource(R.drawable.ic_launcher);
				}
			} else {

				holder.fileIcon.setImageBitmap(bit);
				bitmapMapping.put(bit, holder);
			}

			if (mFileLists.get(position).isCheck == ITEM_CHECKED) {
				holder.fileCheck.setChecked(true);
				holder.fileCheck.setVisibility(View.VISIBLE);
			} else {
				holder.fileCheck.setChecked(false);
				holder.fileCheck.setVisibility(View.INVISIBLE);
			}
			return convertView;
		}
	}

	class ViewHolder {

		public CheckBox fileCheck;
		public TextView fileFolderName;
		public ImageView fileArrow;
		public ImageView fileIcon;

	}

	private void reutrnResult(int resultCode) {

		Intent intent = new Intent();
		intent.putParcelableArrayListExtra("checkedFiles", mCheckedList);
		setResult(resultCode, intent);
		mFileLists.clear();
		mCheckedNameList.clear();
		if ("file".equals(type) && mFolderLists != null) {

			mFolderLists.clear();
		}
		finish();
	}

	public void adapterFileIcon(String fileName, ViewHolder holder,
			FileInfoBean file) {

		if (fileName.endsWith(".jpg") || fileName.endsWith(".png")
				|| fileName.endsWith(".jpeg") || fileName.endsWith(".bmp")
				|| fileName.endsWith("gif")) {

			if (holder != null) {
				holder.fileIcon
						.setImageResource(R.drawable.selectfile_type_picture);
			} else if (file != null) {
				file.fileType = 1;
			}

		} else if (fileName.endsWith(".doc")) {

			if (holder != null) {
				holder.fileIcon
						.setImageResource(R.drawable.selectfile_type_word);
			} else if (file != null) {
				file.fileType = 2;
			}

		} else if (fileName.endsWith(".xls")) {
			if(holder != null){
				holder.fileIcon
				.setImageResource(R.drawable.selectfile_type_excel);
			}
			else if(file != null){
				file.fileType = 3;
			}
		} else if (fileName.endsWith(".pdf")) {
			if(holder != null){
				holder.fileIcon
				.setImageResource(R.drawable.selectfile_type_pdf);
			}
			else if(file != null){
				file.fileType = 4;
			}
		} else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
			if(holder != null){
				holder.fileIcon
				.setImageResource(R.drawable.selectfile_type_ppt);
			}
			else if(file != null){
				file.fileType = 5;
			}
		} else if (fileName.endsWith(".zip") || fileName.endsWith(".rar")) {
			if(holder != null){
				holder.fileIcon
				.setImageResource(R.drawable.selectfile_type_zip);
			}
			else if(file != null){
				file.fileType = 6;
			}
		} else if (fileName.endsWith(".vsd") || fileName.endsWith(".vss")
				|| fileName.endsWith(".vst") || fileName.endsWith(".vdx")) {
			if(holder != null){
				holder.fileIcon
				.setImageResource(R.drawable.selectfile_type_viso);
			}
			else if(file != null){
				file.fileType = 7;
			}
		} else if (fileName.endsWith(".mp4") || fileName.endsWith(".rmvb")
				|| fileName.endsWith(".avi") || fileName.endsWith(".3gp")) {
			if(holder != null){
				holder.fileIcon
				.setImageResource(R.drawable.selectfile_type_video);
			}
			else if(file != null){
				file.fileType = 8;
			}
		} else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav")
				|| fileName.endsWith(".ape") || fileName.endsWith(".wmv")) {
			if(holder != null){
				holder.fileIcon
				.setImageResource(R.drawable.selectfile_type_sound);
			}
			else if(file != null){
				file.fileType = 9;
			}
		} else {
			if(holder != null){
				holder.fileIcon
				.setImageResource(R.drawable.selectfile_type_ohter);
			}
			else if(file != null){
				file.fileType = 10;
			}
		}
	}

	/**
	 * item点击事件，来更新List显示，并添加到集合
	 */
	private void notifyListChange(View view, int position) {
		// 文件
		FileInfoBean bean = null;
		CheckBox button;

		if ("image".equals(type)) {

			bean = mFileLists.get(position);
			button = (CheckBox) view
					.findViewById(R.id.selectfile_adapter_check);
		} else {

			bean = mFileLists.get(position - mFolderLists.size());
			button = (CheckBox) view
					.findViewById(R.id.selectfile_adapter_checkbox);
		}

		// 判断当前item被选择的状态
		if (button.isChecked()) {
			button.setChecked(false);
			bean.isCheck = ITEM_UNCHECKED;
			for (int i = 0; i < mCheckedList.size(); i++) {
				if (mCheckedList.get(i).fileName.equals(bean.fileName)) {
					mCheckedList.remove(i);
				}
			}
			mCheckedNameList.remove(bean.fileName);
			totalSize -= bean.fileSize;
			if (mCheckedList.size() == 0) {
				changeSendUnable();
			}
		} else {
			// 如果当前item没有被选中，则进一步判断一下当前mCheckedList长度是否为0，如果为0则变为可点击
			if (mCheckedList.size() == 0) {

				changeSendAble();
			}
			bean.isCheck = ITEM_CHECKED;
			button.setChecked(true);
			mCheckedList.add(bean);
			mCheckedNameList.add(bean.fileName);
			totalSize += bean.fileSize;
		}
		selectedFileSize.setText("已选 " + getFileSize(totalSize));
		sendButton.setText("发送(" + mCheckedList.size() + ")");

	}

	/**
	 * 对图片进行压缩
	 * 
	 * @param path
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Bitmap handlerImage(String path) throws FileNotFoundException,
			IOException {

		Options options = new Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);
		options.inJustDecodeBounds = false;

		int width = options.outWidth;
		int height = options.outHeight;

		int widthScale = width / mScreenWidth;
		int heightScale = height / mScreenHeight;

		int scale = 1;
		if (widthScale > heightScale)
			scale = widthScale + 4;
		else
			scale = heightScale + 4;

		if (scale < 0) {
			scale = 1;
		}

		options.inSampleSize = scale;
		options.inPreferredConfig = Bitmap.Config.RGB_565;
		options.inInputShareable = true;// 。当系统内存不够时候图片自动被回收
		options.inPurgeable = true;
		return BitmapFactory.decodeFile(path, options);
	}

	class BitmapLRU extends LruCache<String, Bitmap> {

		public BitmapLRU(int maxSize) {
			super(maxSize);
		}

		@Override
		protected void entryRemoved(boolean evicted, String key,
				Bitmap oldValue, Bitmap newValue) {
			super.entryRemoved(evicted, key, oldValue, newValue);
			Message.obtain(handler, REMOVE_BITMAP, oldValue).sendToTarget();
		}
	}
}
