package com.v2tech.view.group;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.v2tech.R;
import com.v2tech.service.AsyncResult;
import com.v2tech.service.CrowdGroupService;
import com.v2tech.service.FileOperationEnum;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.service.jni.FileTransStatusIndication;
import com.v2tech.service.jni.FileTransStatusIndication.FileTransProgressStatusIndication;
import com.v2tech.service.jni.JNIResponse;
import com.v2tech.service.jni.RequestFetchGroupFilesResponse;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.FileUitls;
import com.v2tech.util.V2Toast;
import com.v2tech.view.JNIService;
import com.v2tech.view.conversation.ConversationSelectFile;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.view.group.CrowdFilesActivity.FileListAdapter.ViewItem;
import com.v2tech.vo.CrowdGroup;
import com.v2tech.vo.FileInfoBean;
import com.v2tech.vo.NetworkStateCode;
import com.v2tech.vo.Group.GroupType;
import com.v2tech.vo.VCrowdFile;
import com.v2tech.vo.VFile;

public class CrowdFilesActivity extends Activity {

	private static final int FETCH_FILES_DONE = 0;

	private static final int OPERATE_FILE = 1;

	private static final int FILE_TRANS_NOTIFICATION = 2;

	private static final int FILE_REMOVE_NOTIFICATION = 3;

	private static final int FILE_REMOVE_DONE = 4;

	private static final int NEW_FILE_NOTIFICATION = 5;

	private static final int SHOW_DELETE_BUTTON_FLAG = 1;

	private static final int HIDE_DELETE_BUTTON_FLAG = 0;

	private static final int REQUEST_CODE = 100;

	private static final int RECEIVE_SELECTED_FILE = 200;

	private Map<String, VCrowdFile> mFileMap;
	private List<VCrowdFile> mFiles;
	private List<VCrowdFile> mUploadedFiles;
	private ArrayList<FileInfoBean> mCheckedList;

	private Context mContext;
	private LinearLayout mUploadFinish;
	private ListView mListView;
	private FileListAdapter adapter;
	private View mReturnButton;
	private View mCannelButton;
	private TextView mShowUploadedFileButton;
	private TextView mTitle;
	private ImageView mUploadingFileNotificationIcon;
	private boolean showUploaded;
	private boolean isInDeleteMode;

	private CrowdGroupService service;
	private CrowdGroup crowd;
	private LocalReceiver localReceiver;
	private NewFileMonitorReceiver mNewFileMonitorReceiver;
	private long currentLoginUserID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.crowd_files_activity);
		currentLoginUserID = GlobalHolder.getInstance().getCurrentUserId();

		mUploadFinish = (LinearLayout) findViewById(R.id.crowd_files_uploaded_hint);
		mListView = (ListView) findViewById(R.id.crowd_files_list);
		mListView.setTextFilterEnabled(true);
		mTitle = (TextView) findViewById(R.id.crowd_files_title);
		mUploadingFileNotificationIcon = (ImageView) findViewById(R.id.crowd_file_upload_icon);
		mUploadingFileNotificationIcon
				.setOnClickListener(mShowUPloadingFileListener);
		mUploadingFileNotificationIcon.setVisibility(View.GONE);

		mReturnButton = findViewById(R.id.crowd_members_return_button);
		mReturnButton.setOnClickListener(mBackButtonListener);

		mCannelButton = findViewById(R.id.crowd_files_uploaded_cancel_button);
		mCannelButton.setOnClickListener(mCannelButtonListener);

		mShowUploadedFileButton = (TextView) findViewById(R.id.crowd_files_uploaded_file_button);
		mShowUploadedFileButton
				.setOnClickListener(mShowUploadedFileButtonListener);

		mFiles = new ArrayList<VCrowdFile>();
		mUploadedFiles = new ArrayList<VCrowdFile>();
		mFileMap = new HashMap<String, VCrowdFile>();

		service = new CrowdGroupService();
		// register file transport listener
		service.registerFileTransStatusListener(mLocalHandler,
				FILE_TRANS_NOTIFICATION, null);
		// register file removed listener
		service.registerFileRemovedNotification(mLocalHandler,
				FILE_REMOVE_NOTIFICATION, null);

		service.registerNewFileNotification(mLocalHandler,
				NEW_FILE_NOTIFICATION, null);

		adapter = new FileListAdapter();
		mListView.setAdapter(adapter);
		mListView.setOnItemLongClickListener(mDeleteModeListener);
		overridePendingTransition(R.animator.left_in, R.animator.left_out);

		crowd = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.CHATING.intValue(),
				getIntent().getLongExtra("cid", 0));
		// Reset crowd new file count to 0
		crowd.resetNewFileCount();
		loadFiles();
		initReceiver();
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.animator.right_in, R.animator.right_out);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RECEIVE_SELECTED_FILE) {
			if (data != null) {
				mCheckedList = data.getParcelableArrayListExtra("checkedFiles");
				V2Log.e("get checked files size is :" + mCheckedList.size());
				if (mCheckedList.size() > 0) {
					for (int i = 0; i < mCheckedList.size(); i++) {
						FileInfoBean fb = mCheckedList.get(i);
						VCrowdFile vf = new VCrowdFile();
						vf.setCrowd(crowd);
						vf.setUploader(GlobalHolder.getInstance()
								.getCurrentUser());
						vf.setId(UUID.randomUUID().toString());
						vf.setPath(fb.filePath);
						vf.setName(fb.fileName);
						vf.setSize(fb.fileSize);
						vf.setState(VFile.State.UPLOADING);
						service.handleCrowdFile(vf,
								FileOperationEnum.OPERATION_START_SEND, null);
						mUploadedFiles.add(vf);
						mFileMap.put(vf.getId(), vf);
					}

					// Update screen to uploading UI
					// showUploaded = true;
					// mShowUploadedFileButton.setVisibility(View.GONE);
					adapterUploadShow();
					adapter.notifyDataSetChanged();
				}
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		service.unRegisterFileTransStatusListener(mLocalHandler,
				FILE_TRANS_NOTIFICATION, null);
		service.unRegisterFileRemovedNotification(mLocalHandler,
				FILE_REMOVE_NOTIFICATION, null);

		service.unRegisterNewFileNotification(mLocalHandler,
				NEW_FILE_NOTIFICATION, null);
		service.clearCalledBack();
		this.unregisterReceiver(localReceiver);
		this.unregisterReceiver(mNewFileMonitorReceiver);
	}

	@Override
	public void onBackPressed() {
		if (isInDeleteMode) {
			mCannelButton.setVisibility(View.INVISIBLE);
			isInDeleteMode = false;
			// resume all uploading files
			suspendOrResumeUploadingFiles(false);
			adapter.notifyDataSetChanged();
			// set cancel button text to upload text
			mShowUploadedFileButton.setText(R.string.crowd_files_title_upload);
			return;
		} else if (showUploaded) {
			mCannelButton.setVisibility(View.INVISIBLE);
			showUploaded = false;
			mTitle.setText(R.string.crowd_files_title);
			mShowUploadedFileButton.setText(R.string.crowd_files_title_upload);
			mShowUploadedFileButton.setVisibility(View.VISIBLE);
			adapterUploadShow();
			adapter.notifyDataSetChanged();
			return;
		}
		super.onBackPressed();
	}

	private void initReceiver() {
		localReceiver = new LocalReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(JNIService.JNI_BROADCAST_KICED_CROWD);
		filter.addAction(JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION);
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		this.registerReceiver(localReceiver, filter);

		mNewFileMonitorReceiver = new NewFileMonitorReceiver();
		filter = new IntentFilter();
		filter.addAction(JNIService.BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION);
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
		this.registerReceiver(mNewFileMonitorReceiver, filter);
	}

	private void loadFiles() {
		if (crowd == null) {
			V2Log.e("Unknow crowd");
			return;
		}

		// loading upLoading files from database;
		List<VCrowdFile> fileItems = MessageLoader
				.loadGroupUpLoadingFileMessage(mContext, crowd.getGroupType()
						.intValue(), crowd.getmGId(), crowd);
		if (fileItems != null && fileItems.size() > 0)
			showUploaded = true;
		else
			showUploaded = false;

		service.fetchGroupFiles(crowd, new MessageListener(mLocalHandler,
				FETCH_FILES_DONE, null));
	}

	/**
	 * Save file list and update file state
	 * 
	 * @param files
	 */
	private void handleFetchFilesDone(List<VCrowdFile> files) {
		if (files == null) {
			V2Log.e("files list is null");
			return;
		}
		mFiles.clear();
		mFiles.addAll(files);
		String path = GlobalConfig.getGlobalPath() + "/files/"
				+ crowd.getmGId();
		File groupPath = new File(path);

		for (VCrowdFile f : mFiles) {
			mFileMap.put(f.getId(), f);
			// update file state
			if (groupPath.exists()) {
				File file = new File(groupPath + "/" + f.getName());
				if (file.exists() && file.length() == f.getSize()) {
					if (f.getUploader().getmUserId() == GlobalHolder
							.getInstance().getCurrentUserId()) {
						f.setState(VCrowdFile.State.UPLOADED);
					} else {
						f.setState(VCrowdFile.State.DOWNLOADED);
					}
				}
			} else {
				f.setState(VCrowdFile.State.UNKNOWN);
			}
		}

		if (!groupPath.exists()) {
			groupPath.mkdirs();
		}
		// Update file state

		adapter.notifyDataSetChanged();
	}

	private void handleFileTransNotification(FileTransStatusIndication ind) {
		VCrowdFile file = mFileMap.get(ind.uuid);
		if (file == null) {
			V2Log.e(" File id doesn't exist: " + ind.uuid);
			return;
		}
		if (ind.indType == FileTransStatusIndication.IND_TYPE_PROGRESS) {
			FileTransProgressStatusIndication progress = (FileTransProgressStatusIndication) ind;
			V2Log.e(" receive progress upload file state.... normal");
			if (progress.progressType == FileTransStatusIndication.IND_TYPE_PROGRESS_END) {
				file.setProceedSize(file.getSize());
			} else {
				file.setProceedSize(progress.nTranedSize);
			}
			if (file.getProceedSize() == file.getSize()
					&& file.getUploader().getmUserId() == GlobalHolder
							.getInstance().getCurrentUserId()) {
				this.mUploadedFiles.remove(file);
				adapterUploadShow();
			}

		} else if (ind.indType == FileTransStatusIndication.IND_TYPE_DOWNLOAD_ERR) {
			file.setState(VFile.State.DOWNLOAD_FAILED);
		} else if (ind.indType == FileTransStatusIndication.IND_TYPE_TRANS_ERR) {
			file.setState(VFile.State.DOWNLOAD_FAILED);
		}

		adapter.notifyDataSetChanged();

	}

	/**
	 * Handle file removed notification
	 * 
	 * @param files
	 */
	private void handleFileRemovedEvent(List<VCrowdFile> files) {
		for (VCrowdFile removedFile : files) {
			for (int i = 0; i < mFiles.size(); i++) {
				if (mFiles.get(i).getId().equals(removedFile.getId())) {
					VCrowdFile file = mFiles.get(i);
					service.handleCrowdFile(file,
							FileOperationEnum.OPERATION_CANCEL_DOWNLOADING,
							null);
					file.setState(VFile.State.REMOVED);
					break;
				}
			}
		}
		adapter.notifyDataSetChanged();
	}

	private void suspendOrResumeUploadingFiles(boolean flag) {
		for (int i = 0; i < this.mUploadedFiles.size(); i++) {
			VCrowdFile vf = mUploadedFiles.get(i);
			if (flag) {
				vf.setState(VFile.State.UPLOAD_PAUSE);
				service.handleCrowdFile(vf,
						FileOperationEnum.OPERATION_PAUSE_SENDING, null);
			} else {
				vf.setState(VFile.State.UPLOADING);
				service.handleCrowdFile(vf,
						FileOperationEnum.OPERATION_RESUME_SEND, null);
			}
		}
	}

	/**
	 * Handle new file notification
	 * 
	 * @param files
	 */
	private void handleNewFileEvent(List<VCrowdFile> files) {
		for (VCrowdFile vCrowdFile : files) {
			mFiles.add(0, vCrowdFile);
			mFileMap.put(vCrowdFile.getId(), vCrowdFile);
		}
		adapter.notifyDataSetChanged();
	}

	private void handleFileRemovedEvent(VCrowdFile file) {
		List<VCrowdFile> list = mFiles;
		if (this.showUploaded) {
			list = this.mUploadedFiles;
		}

		if (file == null) {
			return;
		}
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getId().equals(file.getId())) {
				list.remove(i);
				if (file.getState() == VCrowdFile.State.DOWNLOADING) {
					service.handleCrowdFile(file,
							FileOperationEnum.OPERATION_CANCEL_DOWNLOADING,
							null);
				}
				break;
			}
		}
		adapter.notifyDataSetChanged();
	}

	/**
	 * adapter upload show style
	 */
	private void adapterUploadShow() {

		if (showUploaded) {
			mUploadingFileNotificationIcon.setVisibility(View.INVISIBLE);
			if (mUploadedFiles.size() <= 0)
				mUploadFinish.setVisibility(View.VISIBLE);
			else
				mUploadFinish.setVisibility(View.INVISIBLE);
		} else {
			mUploadFinish.setVisibility(View.INVISIBLE);
			if (mUploadedFiles.size() > 0)
				mUploadingFileNotificationIcon.setVisibility(View.VISIBLE);
			else
				mUploadingFileNotificationIcon.setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * matching the crowd file type for show the icon
	 * 
	 * @param fileName
	 * @param holder
	 * @param file
	 */
	public void adapterFileIcon(String fileName, ViewItem holder) {

		if (fileName.endsWith(".jpg") || fileName.endsWith(".png")
				|| fileName.endsWith(".jpeg") || fileName.endsWith(".bmp")
				|| fileName.endsWith("gif")) {

			if (holder != null)
				holder.mFileIcon
						.setImageResource(R.drawable.selectfile_type_picture);

		} else if (fileName.endsWith(".doc")) {

			if (holder != null)
				holder.mFileIcon
						.setImageResource(R.drawable.selectfile_type_word);

		} else if (fileName.endsWith(".xls")) {
			if (holder != null)
				holder.mFileIcon
						.setImageResource(R.drawable.selectfile_type_excel);

		} else if (fileName.endsWith(".pdf")) {
			if (holder != null)
				holder.mFileIcon
						.setImageResource(R.drawable.selectfile_type_pdf);
		} else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
			if (holder != null)
				holder.mFileIcon
						.setImageResource(R.drawable.selectfile_type_ppt);
		} else if (fileName.endsWith(".zip") || fileName.endsWith(".rar")) {
			if (holder != null)
				holder.mFileIcon
						.setImageResource(R.drawable.selectfile_type_zip);
		} else if (fileName.endsWith(".vsd") || fileName.endsWith(".vss")
				|| fileName.endsWith(".vst") || fileName.endsWith(".vdx")) {
			if (holder != null)
				holder.mFileIcon
						.setImageResource(R.drawable.selectfile_type_viso);
		} else if (fileName.endsWith(".mp4") || fileName.endsWith(".rmvb")
				|| fileName.endsWith(".avi") || fileName.endsWith(".3gp")) {
			if (holder != null)
				holder.mFileIcon
						.setImageResource(R.drawable.selectfile_type_video);
		} else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav")
				|| fileName.endsWith(".ape") || fileName.endsWith(".wmv")) {
			if (holder != null)
				holder.mFileIcon
						.setImageResource(R.drawable.selectfile_type_sound);
		} else {
			if (holder != null)
				holder.mFileIcon
						.setImageResource(R.drawable.selectfile_type_ohter);
		}
	}

	private OnClickListener mBackButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();
		}

	};

	private OnClickListener mCannelButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (isInDeleteMode) {
				isInDeleteMode = false;
				// resume all uploading files
				suspendOrResumeUploadingFiles(false);
				adapter.notifyDataSetChanged();
				mCannelButton.setVisibility(View.INVISIBLE);
				return;
			}
		}

	};

	private OnClickListener mShowUPloadingFileListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// Update screen to uploading UI
			showUploaded = true;
			mTitle.setText(R.string.crowd_files_title_uploading);
			mShowUploadedFileButton.setVisibility(View.GONE);
			adapterUploadShow();
			adapter.notifyDataSetChanged();
		}

	};

	private OnClickListener mShowUploadedFileButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (isInDeleteMode) {
				// set cancel button text to upload text
				mShowUploadedFileButton
						.setText(R.string.crowd_files_title_upload);
				isInDeleteMode = false;
				adapter.notifyDataSetChanged();
			} else {
				Intent intent = new Intent(mContext,
						ConversationSelectFile.class);
				intent.putExtra("type", "crowdFile");
				startActivityForResult(intent, RECEIVE_SELECTED_FILE);
			}
		}

	};

	private OnItemLongClickListener mDeleteModeListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			if (isInDeleteMode) {
				if (showUploaded) {
					mCannelButton.setVisibility(View.INVISIBLE);
				}
				return false;
			} else {
				if (showUploaded) {
					mCannelButton.setVisibility(View.VISIBLE);
					isInDeleteMode = true;
					// Pause all uploading files
					suspendOrResumeUploadingFiles(true);
					adapter.notifyDataSetChanged();
					// update upload button text to cancel
					mShowUploadedFileButton
							.setText(R.string.crowd_files_title_cancel_button);
				} else {
					
					boolean showDeleteMode = false;
					if (crowd.getOwnerUser().getmUserId() == GlobalHolder.getInstance().getCurrentUserId()) {
						showDeleteMode = true;
					} else {
						for (VCrowdFile file : mFiles) {
							if (file.getUploader().getmUserId() == currentLoginUserID) {
								showDeleteMode = true;
								break;
							}
						}
					}

					if (showDeleteMode) {
						isInDeleteMode = true;
						// Pause all uploading files
						suspendOrResumeUploadingFiles(true);
						adapter.notifyDataSetChanged();
						// update upload button text to cancel
						mShowUploadedFileButton
								.setText(R.string.crowd_files_title_cancel_button);
					}
				}
				return true;
			}
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
					handleFetchFilesDone(rf.getList());
				} else {
					// TODO show error
				}
				break;
			case OPERATE_FILE:
				break;
			case FILE_TRANS_NOTIFICATION:
				FileTransStatusIndication ind = (FileTransStatusIndication) (((AsyncResult) msg.obj)
						.getResult());
				handleFileTransNotification(ind);
				break;
			case FILE_REMOVE_NOTIFICATION:
				JNIResponse jni = (JNIResponse) ((AsyncResult) msg.obj)
						.getResult();
				if (jni.getResult() == JNIResponse.Result.SUCCESS) {
					handleFileRemovedEvent(((RequestFetchGroupFilesResponse) jni)
							.getList());
				}
				break;
			case FILE_REMOVE_DONE:
				break;
			case NEW_FILE_NOTIFICATION:
				JNIResponse newFileni = (JNIResponse) ((AsyncResult) msg.obj)
						.getResult();
				if (newFileni.getResult() == JNIResponse.Result.SUCCESS) {
					handleNewFileEvent(((RequestFetchGroupFilesResponse) newFileni)
							.getList());
					adapterUploadShow();
				}
				break;
			}
		}

	};

	class LocalReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(JNIService.JNI_BROADCAST_KICED_CROWD)) {
				long crowdId = intent.getLongExtra("crowd", 0);
				if (crowdId == crowd.getmGId()) {
					for (VCrowdFile f : mFiles) {
						if (f.getState() == VFile.State.DOWNLOADING) {
							service.handleCrowdFile(
									f,
									FileOperationEnum.OPERATION_CANCEL_DOWNLOADING,
									null);
						} else if (f.getState() == VFile.State.UPLOADING) {
							service.handleCrowdFile(f,
									FileOperationEnum.OPERATION_CANCEL_SENDING,
									null);
						}
					}
					finish();
				}
			} else if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_CONNECT_STATE_NOTIFICATION)) {
				NetworkStateCode code = (NetworkStateCode) intent.getExtras()
						.get("state");
				if (code != NetworkStateCode.CONNECTED) {
					for (VCrowdFile file : mUploadedFiles) {
						file.setState(VFile.State.UPLOAD_FAILED);
					}
					adapter.notifyDataSetChanged();
				}
			}

		}

	}

	class NewFileMonitorReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					JNIService.BROADCAST_CROWD_NEW_UPLOAD_FILE_NOTIFICATION)) {
				long crowdId = intent.getLongExtra("groupID", 0);
				if (crowdId == crowd.getmGId()) {
					crowd.resetNewFileCount();
				}
			}

		}

	}

	class FileListAdapter extends BaseAdapter implements Filterable {

		class ViewItem {
			ImageView mFileDeleteModeButton;
			ImageView mFileIcon;
			TextView mFileName;
			TextView mFileSize;
			TextView mFileButton;
			TextView mFileText;
			TextView mVelocity;
			ImageView mProgress;
			TextView mFileProgress;
			View mProgressParent;
			ImageView mFailedIcon;
			TextView mFileDeleteButton;
			View mProgressLayout;
		}

		class Tag {
			VCrowdFile vf;
			ViewItem item;

			public Tag(VCrowdFile vf, ViewItem item) {
				super();
				this.vf = vf;
				this.item = item;
			}

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
			VCrowdFile file = null;
			if (showUploaded) {
				file = mUploadedFiles.get(position);
			} else {
				file = mFiles.get(position);
			}

			ViewItem item = null;
			Tag tag = null;
			if (convertView == null) {
				convertView = layoutInflater.inflate(
						R.layout.crowd_file_adapter_item, null);
				item = new ViewItem();
				tag = new Tag(file, item);
				item.mFileDeleteModeButton = (ImageView) convertView
						.findViewById(R.id.crowd_file_delete_left_button);
				item.mFileDeleteModeButton.setTag(tag);
				item.mFileDeleteModeButton
						.setOnClickListener(mDeleteModeButtonListener);

				item.mFileIcon = (ImageView) convertView
						.findViewById(R.id.ws_common_conversation_layout_icon);
				item.mFileName = (TextView) convertView
						.findViewById(R.id.ws_common_conversation_layout_topContent);
				item.mFileSize = (TextView) convertView
						.findViewById(R.id.ws_common_conversation_layout_belowContent);
				item.mFileButton = (TextView) convertView
						.findViewById(R.id.crowd_file_button);

				item.mFileText = (TextView) convertView
						.findViewById(R.id.crowd_file_text);
				item.mVelocity = (TextView) convertView
						.findViewById(R.id.file_velocity);
				item.mFileProgress = (TextView) convertView
						.findViewById(R.id.file_process_percent);

				item.mProgressParent = convertView
						.findViewById(R.id.file_download_progress_state_ly);
				item.mProgress = (ImageView) convertView
						.findViewById(R.id.ile_download_progress_state);
				item.mFailedIcon = (ImageView) convertView
						.findViewById(R.id.crowd_file_failed_icon);
				item.mFileDeleteButton = (TextView) convertView
						.findViewById(R.id.crowd_file_delete_button);
				item.mFileDeleteButton.setTag(tag);
				item.mFileDeleteButton
						.setOnClickListener(mDeleteButtonListener);

				item.mFailedIcon.setOnClickListener(mFailIconListener);
				item.mFileButton.setOnClickListener(mButtonListener);

				item.mProgressLayout = convertView
						.findViewById(R.id.crowd_file_item_progrss_ly);
				convertView.setTag(tag);
			} else {
				tag = (Tag) convertView.getTag();
				item = tag.item;
				tag.vf = file;
			}

			updateViewItem(tag);

			return convertView;
		}

		private void updateViewItem(Tag tag) {
			VCrowdFile file = tag.vf;
			ViewItem item = tag.item;
			item.mFileName.setText(file.getName());
			item.mFileSize.setText(file.getFileSizeStr());
			item.mFileButton.setTag(file);
			item.mFailedIcon.setTag(file);
			VFile.State fs = file.getState();

			// TODO show uploading item
			if (showUploaded) {

			}

			if (isInDeleteMode
					&& (file.getUploader().getmUserId() == currentLoginUserID
					|| GlobalHolder.getInstance().getCurrentUserId() == crowd
							.getOwnerUser().getmUserId())) {
				item.mFileDeleteModeButton.setVisibility(View.VISIBLE);
			} else {
				item.mFileDeleteModeButton.setVisibility(View.GONE);
				// Record flag for show delete button
				file.setFlag(HIDE_DELETE_BUTTON_FLAG);
			}

			adapterFileIcon(file.getName(), item);

			switch (fs) {
			case UNKNOWN:
				item.mFileButton
						.setText(R.string.crowd_files_button_name_download);
				item.mFailedIcon.setVisibility(View.GONE);
				item.mFileText.setVisibility(View.GONE);
				item.mFileButton.setVisibility(View.VISIBLE);
				item.mProgressLayout.setVisibility(View.GONE);
				break;
			case UPLOADING:
			case DOWNLOADING:
				item.mFileButton
						.setText(R.string.crowd_files_button_name_pause);
				item.mFileButton.setVisibility(View.VISIBLE);
				item.mFailedIcon.setVisibility(View.GONE);
				item.mFileText.setVisibility(View.GONE);
				item.mProgressLayout.setVisibility(View.VISIBLE);
				break;
			case UPLOAD_PAUSE:
			case DOWNLOAD_PAUSE:
				item.mFileButton
						.setText(R.string.crowd_files_button_name_resume);
				item.mFailedIcon.setVisibility(View.GONE);
				item.mFileText.setVisibility(View.GONE);
				item.mFileButton.setVisibility(View.VISIBLE);
				item.mProgressLayout.setVisibility(View.VISIBLE);
				break;
			case DOWNLOADED:
				item.mFileButton.setText(R.string.crowd_files_name_open_file);
				item.mFileButton.setVisibility(View.VISIBLE);
				item.mFileText.setVisibility(View.GONE);
				item.mFileProgress.setVisibility(View.GONE);
				item.mFailedIcon.setVisibility(View.GONE);
				item.mProgressLayout.setVisibility(View.GONE);
				break;
			case UPLOADED:
				item.mFileText.setText(R.string.crowd_files_name_uploaded);
				item.mFileText.setVisibility(View.VISIBLE);
				item.mFileButton.setVisibility(View.GONE);
				item.mFileProgress.setVisibility(View.GONE);
				item.mFailedIcon.setVisibility(View.GONE);
				item.mProgressLayout.setVisibility(View.GONE);
				break;
			case DOWNLOAD_FAILED:
			case UPLOAD_FAILED:
			case REMOVED:
				item.mFailedIcon.setVisibility(View.VISIBLE);
				item.mFileButton.setVisibility(View.GONE);
				item.mFileText.setVisibility(View.GONE);
				item.mProgressLayout.setVisibility(View.GONE);
				break;
			default:
				break;
			}

			if (file.getFlag() == SHOW_DELETE_BUTTON_FLAG) {
				item.mFileDeleteButton.setVisibility(View.VISIBLE);
				item.mFailedIcon.setVisibility(View.GONE);
				item.mFileText.setVisibility(View.GONE);
				item.mFileButton.setVisibility(View.GONE);
			} else {
				item.mFileDeleteButton.setVisibility(View.INVISIBLE);
			}

			if (fs == VFile.State.DOWNLOADING
					|| file.getState() == VFile.State.UPLOADING) {
				item.mFileProgress.setVisibility(View.VISIBLE);
				item.mFileProgress.setText(file.getProceedSizeStr() + "/"
						+ file.getFileSizeStr());
				item.mVelocity.setText(file.getSpeedStr() + "/S");
			}

			if (fs == VFile.State.DOWNLOADING || fs == VFile.State.UPLOADING) {
				float percent = (float) ((double) file.getProceedSize() / (double) file
						.getSize());
				updateProgress(item, percent);
			}

		}

		private void updateProgress(ViewItem item, float percent) {
			int width = item.mProgressParent.getWidth();
			ViewGroup.LayoutParams vl = item.mProgress.getLayoutParams();
			vl.width = (int) (width * percent);
			item.mProgress.setLayoutParams(vl);
		}

		private OnClickListener mFailIconListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				VCrowdFile file = (VCrowdFile) v.getTag();
				if (file.getState() == VFile.State.REMOVED) {
					mFiles.remove(file);
					V2Toast.makeText(mContext,
							R.string.crowd_files_deleted_notification,
							Toast.LENGTH_SHORT).show();
				} else {
					if (file.getState() == VFile.State.DOWNLOAD_FAILED) {
						file.setState(VFile.State.DOWNLOADING);
						file.setStartTime(new Date());
						service.handleCrowdFile(file,
								FileOperationEnum.OPERATION_START_DOWNLOAD,
								null);
					} else if (file.getState() == VFile.State.UPLOAD_FAILED) {
						file.setState(VFile.State.UPLOADING);
						file.setStartTime(new Date());
						service.handleCrowdFile(file,
								FileOperationEnum.OPERATION_START_SEND, null);
					}

				}
				adapter.notifyDataSetChanged();
			}

		};

		private OnClickListener mDeleteModeButtonListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				Tag tag = (Tag) v.getTag();
				ViewItem item = tag.item;

				if (tag.vf.getUploader() != null
						&& tag.vf.getUploader().getmUserId() != GlobalHolder
								.getInstance().getCurrentUserId()
						&& GlobalHolder.getInstance().getCurrentUserId() != crowd
								.getOwnerUser().getmUserId()) {
					Toast.makeText(
							mContext,
							R.string.crowd_files_delete_file_no_rights_notification,
							Toast.LENGTH_SHORT).show();
					return;
				}

				if (tag.vf.getState() == VFile.State.UPLOADING) {
					Toast.makeText(
							mContext,
							R.string.crowd_files_delete_file_no_rights_for_uploading_notification,
							Toast.LENGTH_SHORT).show();
					return;
				}

				if (item.mFileDeleteButton.getVisibility() == View.VISIBLE) {
					item.mFileDeleteButton.setVisibility(View.GONE);
					switch (tag.vf.getState()) {
					case DOWNLOAD_FAILED:
					case UPLOAD_FAILED:
					case REMOVED:
						item.mFailedIcon.setVisibility(View.VISIBLE);
						break;
					case DOWNLOADED:
					case UPLOADED:
						item.mFileText.setVisibility(View.VISIBLE);
						break;
					default:
						item.mFileButton.setVisibility(View.VISIBLE);
						break;
					}
					tag.vf.setFlag(HIDE_DELETE_BUTTON_FLAG);
				} else {
					item.mFileDeleteButton.setVisibility(View.VISIBLE);
					item.mFailedIcon.setVisibility(View.GONE);
					item.mFileButton.setVisibility(View.GONE);
					item.mFileText.setVisibility(View.GONE);
					tag.vf.setFlag(SHOW_DELETE_BUTTON_FLAG);
				}
			}

		};

		private OnClickListener mDeleteButtonListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				Tag tag = (Tag) v.getTag();
				List<VCrowdFile> list = new ArrayList<VCrowdFile>();
				list.add(tag.vf);
				handleFileRemovedEvent(tag.vf);
				service.removeGroupFiles(crowd, list, null);
			}

		};

		private OnClickListener mButtonListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isInDeleteMode) {
					Toast.makeText(mContext,
							R.string.crowd_files_in_deletion_failed,
							Toast.LENGTH_SHORT).show();
					return;

				}
				VCrowdFile file = (VCrowdFile) v.getTag();
				if (file.getState() == VFile.State.UNKNOWN
						|| file.getState() == VFile.State.DOWNLOAD_FAILED) {
					file.setState(VFile.State.DOWNLOADING);
					file.setStartTime(new Date());
					((TextView) v)
							.setText(R.string.crowd_files_button_name_pause);
					service.handleCrowdFile(file,
							FileOperationEnum.OPERATION_START_DOWNLOAD, null);
				} else if (file.getState() == VFile.State.DOWNLOADING) {
					file.setState(VFile.State.DOWNLOAD_PAUSE);
					((TextView) v)
							.setText(R.string.crowd_files_button_name_resume);
					service.handleCrowdFile(file,
							FileOperationEnum.OPERATION_PAUSE_DOWNLOADING, null);
				} else if (file.getState() == VFile.State.UPLOADING) {
					file.setState(VFile.State.UPLOAD_PAUSE);
					((TextView) v)
							.setText(R.string.crowd_files_button_name_pause);
					service.handleCrowdFile(file,
							FileOperationEnum.OPERATION_PAUSE_SENDING, null);
				} else if (file.getState() == VFile.State.DOWNLOAD_PAUSE) {
					file.setState(VFile.State.DOWNLOADING);
					((TextView) v)
							.setText(R.string.crowd_files_button_name_pause);
					service.handleCrowdFile(file,
							FileOperationEnum.OPERATION_RESUME_DOWNLOAD, null);
				} else if (file.getState() == VFile.State.UPLOAD_PAUSE) {
					if (isInDeleteMode) {
						Toast.makeText(mContext,
								R.string.crowd_files_resume_uploading_failed,
								Toast.LENGTH_SHORT).show();
						return;
					}
					file.setState(VFile.State.UPLOADING);
					((TextView) v)
							.setText(R.string.crowd_files_button_name_resume);
					v.invalidate();
					service.handleCrowdFile(file,
							FileOperationEnum.OPERATION_RESUME_SEND, null);
				} else if (file.getState() == VFile.State.DOWNLOADED) {
					FileUitls.openFile(mContext, file.getPath());
				}
				adapter.notifyDataSetChanged();
			}

		};

	}

}
