package com.v2tech.view.group;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.Registrant;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestFetchGroupFilesResponse;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.VCrowdFile;
import com.v2tech.vo.VFile;

public class CrowdFilesActivity extends Activity {

	private static final int FETCH_FILES_DONE = 0;

	private static final int REQUEST_CODE = 100;

	private List<VCrowdFile> mFiles;
	private List<VCrowdFile> mUploadedFiles;

	private Context mContext;
	private ListView mListView;
	private FileListAdapter adapter;
	private View mReturnButton;
	private View mShowUploadedFileButton;
	private TextView mTitle;
	private boolean showUploaded;

	private CrowdGroupService service;
	private CrowdGroup crowd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.crowd_files_activity);
		mListView = (ListView) findViewById(R.id.crowd_files_list);
		mListView.setTextFilterEnabled(true);
		mTitle = (TextView) findViewById(R.id.crowd_files_title);
		
		mReturnButton = findViewById(R.id.crowd_members_return_button);
		mReturnButton.setOnClickListener(mBackButtonListener);
		mShowUploadedFileButton = findViewById(R.id.crowd_files_uploaded_file_button);
		mShowUploadedFileButton
				.setOnClickListener(mShowUploadedFileButtonListener);

		mFiles = new ArrayList<VCrowdFile>();
		mUploadedFiles = new ArrayList<VCrowdFile>();
		service = new CrowdGroupService();
		adapter = new FileListAdapter();
		mListView.setAdapter(adapter);
		overridePendingTransition(R.animator.left_in, R.animator.left_out);

		crowd = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.CHATING.intValue(),
				getIntent().getLongExtra("cid", 0));

		loadFiles();
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.animator.right_in, R.animator.right_out);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		service.clearCalledBack();
	}

	private void loadFiles() {
		if (crowd == null) {
			V2Log.e("Unknow crowd");
			return;
		}
		service.fetchGroupFiles(crowd, new Registrant(mLocalHandler,
				FETCH_FILES_DONE, null));
	}

	private OnClickListener mBackButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (showUploaded) {
				showUploaded = false;
				mTitle.setText(R.string.crowd_files_title);
				mShowUploadedFileButton.setVisibility(View.VISIBLE);
				return;
			}
			onBackPressed();
		}

	};

	private OnClickListener mShowUploadedFileButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			showUploaded = true;
			mTitle.setText(R.string.crowd_files_title_uploaded);
			mShowUploadedFileButton.setVisibility(View.GONE);
			adapter.notifyDataSetChanged();
		}

	};

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FETCH_FILES_DONE:
				JNIResponse res = (JNIResponse) msg.obj;
				if (res.getResult() == JNIResponse.Result.SUCCESS) {
					RequestFetchGroupFilesResponse rf = (RequestFetchGroupFilesResponse) res;
					mFiles.clear();
					mFiles.addAll(rf.getList());
					for (VCrowdFile f : mFiles) {
						if (f.getUploader().getmUserId() == GlobalHolder.getInstance().getCurrentUserId()) {
							mUploadedFiles.add(f);
						}
					}
					adapter.notifyDataSetChanged();
				} else {
					// TODO show error
				}
				break;
			}
		}

	};

	class FileListAdapter extends BaseAdapter implements Filterable {

		class ViewItem {
			ImageView mFileIcon;
			TextView mFileName;
			TextView mFileSize;
			TextView mFileButton;
			TextView mFileText;
			TextView mVeocity;
			ImageView mProgress;
			View mProgressParent;
		}

		private LayoutInflater layoutInflater;

		public FileListAdapter() {
			layoutInflater = LayoutInflater.from(mContext);
		}

		@Override
		public int getCount() {
			if (showUploaded) {
				return mUploadedFiles.size();
			} else {
				return mFiles.size();
			}
		}

		@Override
		public Object getItem(int position) {
			if (showUploaded) {
				return mUploadedFiles.get(position);
			} else {
				return mFiles.get(position);
			}
		}

		@Override
		public long getItemId(int position) {
			if (showUploaded) {
				return mUploadedFiles.get(position).hashCode();
			} else {
				return mFiles.get(position).hashCode();
			}
		}

		@Override
		public Filter getFilter() {
			return null;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewItem item = null;
			if (convertView == null) {
				convertView = layoutInflater.inflate(
						R.layout.crowd_file_adapter_item, null);
				item = new ViewItem();
				item.mFileIcon = (ImageView) convertView
						.findViewById(R.id.crowd_file_icon);
				item.mFileName = (TextView) convertView
						.findViewById(R.id.crowd_file_name);
				item.mFileSize = (TextView) convertView
						.findViewById(R.id.crowd_file_size);
				item.mFileButton = (TextView) convertView
						.findViewById(R.id.crowd_file_button);

				item.mFileText = (TextView) convertView
						.findViewById(R.id.crowd_file_text);
				item.mVeocity = (TextView) convertView
						.findViewById(R.id.file_volcity);

				item.mProgressParent = convertView
						.findViewById(R.id.file_download_progress_state_ly);
				item.mProgress = (ImageView) convertView
						.findViewById(R.id.ile_download_progress_state);

				item.mFileButton.setOnClickListener(mButtonListener);
				convertView.setTag(item);
			} else {
				item = (ViewItem) convertView.getTag();
			}
			VCrowdFile file = null;
			if (showUploaded) {
				file = mUploadedFiles.get(position);
			} else {
				file = mFiles.get(position);
			}
			updateViewItem(file, item);

			return convertView;
		}

		private void updateViewItem(VCrowdFile file, ViewItem item) {
			item.mFileName.setText(file.getName());
			item.mFileSize.setText(file.getFileSizeStr());
			// TODO update file icon according to file type
			if (file.getState() == VFile.State.DOWNLOADING) {
				item.mFileButton
						.setText(R.string.crowd_files_button_name_pause);
			} else if (file.getState() == VFile.State.UPLOAD_PAUSE
					|| file.getState() == VFile.State.DOWNLOAD_PAUSE) {
				item.mFileButton
						.setText(R.string.crowd_files_button_name_resume);
			}

			if (file.getState() == VFile.State.DOWNLOADED
					|| file.getState() == VFile.State.UPLOADED) {
				item.mFileText
						.setText(file.getState() == VFile.State.DOWNLOADED ? R.string.crowd_files_name_downloaded
								: R.string.crowd_files_name_uploaded);
				item.mFileText.setVisibility(View.VISIBLE);
				item.mFileButton.setVisibility(View.GONE);
			} else {
				item.mFileText.setVisibility(View.GONE);
				item.mFileButton.setVisibility(View.VISIBLE);
			}
		}

		private OnClickListener mButtonListener = new OnClickListener() {

			@Override
			public void onClick(View v) {

			}

		};

	}

}
