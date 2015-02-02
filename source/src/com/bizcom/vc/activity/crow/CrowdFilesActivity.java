package com.bizcom.vc.activity.crow;

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
import android.view.View.MeasureSpec;
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
import com.bizcom.bo.GroupUserObject;
import com.bizcom.request.AsyncResult;
import com.bizcom.request.CrowdGroupService;
import com.bizcom.request.FileOperationEnum;
import com.bizcom.request.MessageListener;
import com.bizcom.request.jni.FileTransStatusIndication;
import com.bizcom.request.jni.JNIResponse;
import com.bizcom.request.jni.RequestFetchGroupFilesResponse;
import com.bizcom.request.jni.FileTransStatusIndication.FileTransProgressStatusIndication;
import com.bizcom.util.FileUitls;
import com.bizcom.util.V2Toast;
import com.bizcom.vc.activity.conversation.ConversationSelectFile;
import com.bizcom.vc.activity.conversation.MessageBuilder;
import com.bizcom.vc.activity.conversation.MessageLoader;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.application.V2GlobalConstants;
import com.bizcom.vc.listener.CommonCallBack;
import com.bizcom.vc.listener.CommonCallBack.CrowdFileExeType;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.FileDownLoadBean;
import com.bizcom.vo.FileInfoBean;
import com.bizcom.vo.NetworkStateCode;
import com.bizcom.vo.VCrowdFile;
import com.bizcom.vo.VFile;
import com.bizcom.vo.VMessage;
import com.bizcom.vo.VMessageAbstractItem;
import com.bizcom.vo.VMessageFileItem;
import com.bizcom.vo.Group.GroupType;
import com.bizcom.vo.VFile.State;
import com.v2tech.R;

public class CrowdFilesActivity extends Activity {

	private static final String TAG = "CrowdFilesActivity";

	private static final int FETCH_FILES_DONE = 0;

	private static final int OPERATE_FILE = 1;

	private static final int FILE_TRANS_NOTIFICATION = 2;

	private static final int FILE_REMOVE_NOTIFICATION = 3;

	private static final int NEW_FILE_NOTIFICATION = 5;

	private static final int SHOW_DELETE_BUTTON_FLAG = 1;

	private static final int HIDE_DELETE_BUTTON_FLAG = 0;

	private static final int REQUEST_CODE = 100;

	private static final int RECEIVE_SELECTED_FILE = 200;

	private static final int TYPE_FILE = 10;
	private static final int TYPE_IMAGE = 11;
	private static final int TYPE_AUDIO = 12;

	private Map<String, VCrowdFile> mShowProgressFileMap;
	private HashMap<String, VMessageFileItem> mLocalSaveFile;
	private HashMap<String, VCrowdFile> mPauseFiles;
	private List<VCrowdFile> mServerExistFiles;
	private List<VCrowdFile> mUploadedFiles;
	private Map<String, VMessage> mUploadingVMFiles;
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
	private boolean isFromChatActivity; // 如果从聊天界面点击正在上传的文件，进入正在上传界面，就直接返回聊天界面
	private boolean isFromStartState = true;

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

		mReturnButton = findViewById(R.id.crowd_members_return_button);
		mReturnButton.setOnClickListener(mBackButtonListener);

		mCannelButton = findViewById(R.id.crowd_files_uploaded_cancel_button);
		mCannelButton.setOnClickListener(mCannelButtonListener);

		mShowUploadedFileButton = (TextView) findViewById(R.id.crowd_files_uploaded_file_button);
		mShowUploadedFileButton
				.setOnClickListener(mShowUploadedFileButtonListener);

		mServerExistFiles = new ArrayList<VCrowdFile>();
		mUploadedFiles = new ArrayList<VCrowdFile>();
		mShowProgressFileMap = new HashMap<String, VCrowdFile>();
		mPauseFiles = new HashMap<String, VCrowdFile>();
		mUploadingVMFiles = new HashMap<String, VMessage>();

		initReceiver();
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
		mLocalSaveFile = new HashMap<String, VMessageFileItem>();
		crowd = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.CHATING.intValue(),
				getIntent().getLongExtra("cid", 0));
		// Reset crowd new file count to 0
		crowd.resetNewFileCount();
		loadFiles();
		CrowdFileActivityType type = (CrowdFileActivityType) getIntent()
				.getSerializableExtra("crowdFileActivityType");
		if (type != null) {
			if (CrowdFileActivityType.CROWD_FILE_UPLOING_ACTIVITY == type) {
				isFromChatActivity = true;
				mUploadingFileNotificationIcon.performClick();
			}
		}
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
				if (mCheckedList.size() > 0) {
					for (int i = 0; i < mCheckedList.size(); i++) {
						FileInfoBean fb = mCheckedList.get(i);
						// 对象转换
						VCrowdFile vf = convertToVCrowdFile(fb);
						// 添加到集合
						mUploadedFiles.add(vf);
						mShowProgressFileMap.put(vf.getId(), vf);
						// 保存到数据库
						VMessage vm = MessageBuilder.buildFileMessage(
								V2GlobalConstants.GROUP_TYPE_CROWD, crowd.getmGId(),
								GlobalHolder.getInstance().getCurrentUser(),
								null, fb);
						VMessageFileItem fileItem = vm.getFileItems().get(0);
						fileItem.setUuid(vf.getId());
						vm.setmXmlDatas(vm.toXml());
						vm.setDate(new Date(GlobalConfig.getGlobalServerTime()));
						vm = MessageBuilder.saveMessage(this, vm);
						MessageBuilder.saveFileVMessage(this, vm);
						mUploadingVMFiles.put(vf.getId(), vm);
						// 回调聊天界面产生气泡
						CommonCallBack.getInstance()
								.executeUpdateCrowdFileState(vf.getId(), vm,
										CrowdFileExeType.ADD_FILE);

						GlobalHolder.getInstance().changeGlobleTransFileMember(
								V2GlobalConstants.FILE_TRANS_SENDING, mContext,
								true, crowd.getmGId(),
								"CrowdFilesActivity onActivityResult");
						File file = new File(fb.filePath);
						if(file.exists() && file.isFile()){
							GlobalHolder.getInstance().mTransingLockFiles.put(vf.getId(), fb.filePath);
						}
						// 发送文件
						service.handleCrowdFile(vf,
								FileOperationEnum.OPERATION_START_SEND, null);
					}
					// Update screen to uploading UI
					adapterUploadShow();
					adapter.notifyDataSetChanged();
				}
			}
		}
	}

	private VCrowdFile convertToVCrowdFile(FileInfoBean fb) {
		VCrowdFile vf = new VCrowdFile();
		vf.setCrowd(crowd);
		vf.setUploader(GlobalHolder.getInstance().getCurrentUser());
		vf.setId(UUID.randomUUID().toString());
		vf.setPath(fb.filePath);
		vf.setName(fb.fileName);
		vf.setSize(fb.fileSize);
		vf.setState(VFile.State.UPLOADING);
		return vf;
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
			mUploadingFileNotificationIcon.setClickable(true);
			isInDeleteMode = false;
			// resume all uploading files
			if (showUploaded)
				suspendOrResumeUploadingFiles(false);
			else
				suspendOrResumeDownloadingFiles(false);
			adapter.notifyDataSetChanged();
			// set cancel button text to upload text
			mShowUploadedFileButton.setText(R.string.crowd_files_title_upload);
			return;
		} else if (showUploaded) {
			if (isFromChatActivity) {
				super.onBackPressed();
				return;
			}

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
		filter.addAction(JNIService.JNI_BROADCAST_FILE_STATUS_ERROR_NOTIFICATION);
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
		// loading local files and judging whether show uploading icon
		loadLocalSaveFile();
		// fetch files from server
		service.fetchGroupFiles(crowd, new MessageListener(mLocalHandler,
				FETCH_FILES_DONE, null));
	}

	private void loadLocalSaveFile() {
		List<VMessageFileItem> fileItems = MessageLoader
				.loadGroupFileItemConvertToVCrowdFile(crowd.getmGId(), crowd);
		if (fileItems != null && fileItems.size() > 0) {
			for (int i = 0; i < fileItems.size(); i++) {
				VMessageFileItem temp = fileItems.get(i);
				VCrowdFile crowdFile = MessageLoader.convertToVCrowdFile(
						temp, crowd);
				mLocalSaveFile.put(temp.getUuid(), temp);
				
				if (temp.getState() == VMessageAbstractItem.STATE_FILE_SENDING
						|| temp.getState() == VMessageAbstractItem.STATE_FILE_SENT_FALIED
						|| temp.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_SENDING
						|| temp.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_DOWNLOADING
						|| temp.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADED_FALIED
						|| temp.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING) {
					mUploadingVMFiles.put(fileItems.get(i).getUuid(), fileItems
							.get(i).getVm());

					if (temp.getState() == VMessageAbstractItem.STATE_FILE_SENDING
							|| temp.getState() == VMessageAbstractItem.STATE_FILE_SENT_FALIED
							|| temp.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_SENDING) {
						mUploadedFiles.add(crowdFile);
					}

					if (temp.getState() == VMessageAbstractItem.STATE_FILE_SENDING
							|| temp.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_SENDING
							|| temp.getState() == VMessageAbstractItem.STATE_FILE_PAUSED_DOWNLOADING
							|| temp.getState() == VMessageAbstractItem.STATE_FILE_DOWNLOADING) {
						mShowProgressFileMap.put(crowdFile.getId(), crowdFile);
					}
				}
			}

			if (mUploadedFiles.size() > 0)
				mUploadingFileNotificationIcon.setVisibility(View.VISIBLE);
			else
				mUploadingFileNotificationIcon.setVisibility(View.GONE);
		} else
			mUploadingFileNotificationIcon.setVisibility(View.GONE);
	}

	/**
	 * Save file list and update file state
	 * 
	 * @param files
	 */
	private void handleFetchFilesDone(List<VCrowdFile> fetchFiles) {
		if (fetchFiles == null) {
			V2Log.e("Fetch files from server side failed ! Because get collection is null !");
			return;
		}

		mServerExistFiles.clear();
		mServerExistFiles.addAll(fetchFiles);
		for (VCrowdFile serverFile : mServerExistFiles) {
			// 从数据库读取该文件存的状态
			VMessageFileItem dataBaseSaveFile = mLocalSaveFile.get(serverFile.getId());
			if(dataBaseSaveFile != null && serverFile.getId().equals(dataBaseSaveFile.getUuid())){
				String localName = dataBaseSaveFile.getFileName();
				serverFile.setName(localName);
				
				String serverPath = serverFile.getPath();
				String prefix = serverPath.substring(0, serverPath.lastIndexOf("/"));
				String newPath = prefix + "/" + localName;
				serverFile.setPath(newPath);
			}
			
			// 自己上传的文件，从数据库获取文件路径，判断文件是否存在
			if (serverFile.getUploader().getmUserId() == GlobalHolder
					.getInstance().getCurrentUserId()) {
				if (dataBaseSaveFile != null) {
					File ownerFile = new File(dataBaseSaveFile.getFilePath());
					if (!ownerFile.exists()) {
						if(State.fromInt(dataBaseSaveFile.getState()) == VCrowdFile.State.DOWNLOADED)
							serverFile.setState(VCrowdFile.State.UNKNOWN);
						else
							serverFile.setState(State.fromInt(dataBaseSaveFile.getState()));
					} else {
						if (ownerFile.length() == serverFile.getSize()) {
							serverFile.setState(VCrowdFile.State.DOWNLOADED);
							serverFile.setPath(dataBaseSaveFile.getFilePath());
						} else {
							ownerFile.delete();
							if(State.fromInt(dataBaseSaveFile.getState()) == VCrowdFile.State.DOWNLOADED)
								serverFile.setState(VCrowdFile.State.UNKNOWN);
							else
								serverFile.setState(State.fromInt(dataBaseSaveFile.getState()));
						}
					}
				} else {
					serverFile.setState(VCrowdFile.State.UNKNOWN);
				}
			} else {
				// 其他人的文件路径，则根据文件类型，通过默认路径去判断
				File localFile = buildDefaultFilePath(serverFile);
				if (!localFile.exists()) {
					if (dataBaseSaveFile != null) {
						changeServerFileState(serverFile, dataBaseSaveFile,
								localFile);
					} else
						serverFile.setState(VCrowdFile.State.UNKNOWN);
				} else {
					if (dataBaseSaveFile == null) {
						if (localFile.length() == serverFile.getSize()) {
							serverFile.setState(VCrowdFile.State.DOWNLOADED);
						} else {
							localFile.delete();
							serverFile.setState(VCrowdFile.State.UNKNOWN);
						}
					} else {
						changeServerFileState(serverFile, dataBaseSaveFile,
								localFile);
					}
				}
			}
		}
		adapter.notifyDataSetChanged();
	}

	private File buildDefaultFilePath(VCrowdFile serverFile) {
		// int fileDirType = -1;
		// int index = serverFile.getName().indexOf(".");
		// if(index == -1)
		// fileDirType = TYPE_FILE;
		// else{
		// String postfixName = serverFile.getName().substring(index);
		// if(postfixName.equals(".aac"))
		// fileDirType = TYPE_AUDIO;
		//
		// FileType type = FileUitls.getFileType(postfixName);
		// if(type == FileType.IMAGE)
		// fileDirType = TYPE_IMAGE;
		// else
		// fileDirType = TYPE_FILE;
		// }
		//
		// File localFile = null;
		// if(fileDirType == TYPE_IMAGE)
		// localFile = new File(GlobalConfig.getGlobalPicsPath() + "/" +
		// serverFile.getName());
		// else if(fileDirType == TYPE_AUDIO)
		// localFile = new File(GlobalConfig.getGlobalAudioPath() + "/" +
		// serverFile.getName());
		// else
		// localFile = new File(GlobalConfig.getGlobalFilePath() + "/" +
		// serverFile.getName());
		// return localFile;
		return new File(GlobalConfig.getGlobalFilePath() + "/"
				+ serverFile.getName());
	}

	private void changeServerFileState(VCrowdFile serverFile,
			VMessageFileItem dataBaseVm, File localFile) {
		
		VCrowdFile dataBaseSaveFile = MessageLoader.convertToVCrowdFile(
				dataBaseVm, crowd);
		
		State state = dataBaseSaveFile.getState();
		if (state == State.DOWNLOADING || state == State.DOWNLOAD_PAUSE) {
			mShowProgressFileMap.put(serverFile.getId(), serverFile);
		}

		if (state == State.DOWNLOADED) {
			if (localFile.exists()
					&& localFile.length() == serverFile.getSize()) {
				serverFile.setState(VCrowdFile.State.DOWNLOADED);
			} else {
				localFile.delete();
				serverFile.setState(VCrowdFile.State.UNKNOWN);
			}
		} else {
			if (serverFile.getId().equals(dataBaseSaveFile.getId())) {
				if (state == State.UPLOADED
						&& dataBaseSaveFile.getUploader().getmUserId() != GlobalHolder
								.getInstance().getCurrentUserId()) {
					serverFile.setPath(dataBaseSaveFile.getPath());
					serverFile.setState(State.UNKNOWN);
				} else
					serverFile.setState(state);
			}
		}
	}

	private void handleFileTransNotification(FileTransStatusIndication ind) {
		VCrowdFile file = mShowProgressFileMap.get(ind.uuid);
		if (file == null) {
			V2Log.e(" File id doesn't exist: " + ind.uuid);
			return;
		}
		if (ind.indType == FileTransStatusIndication.IND_TYPE_PROGRESS) {
			FileTransProgressStatusIndication progress = (FileTransProgressStatusIndication) ind;
			V2Log.e("CrowdFilesActivity handleFileTransNotification --> "
					+ "receive progress upload file state.... normal , file id is : "
					+ file.getId() + " file name is : " + file.getName());
			if (progress.progressType == FileTransStatusIndication.IND_TYPE_PROGRESS_END) {
				file.setProceedSize(file.getSize());
			} else {
				isFromStartState = false;
				file.setProceedSize(progress.nTranedSize);
			}
			if (file.getProceedSize() == file.getSize()
					&& file.getUploader().getmUserId() == GlobalHolder
							.getInstance().getCurrentUserId()) {
				this.mUploadedFiles.remove(file);
				adapterUploadShow();
			}

		} else if (ind.indType == FileTransStatusIndication.IND_TYPE_DOWNLOAD_ERR) {
			if (file.getState() == VFile.State.DOWNLOADING)
				file.setState(VFile.State.DOWNLOAD_FAILED);
			else
				file.setState(VFile.State.UPLOAD_FAILED);
			V2Log.e("CrowdFilesActivity handleFileTransNotification --> DWONLOAD_ERROR ...file id is : "
					+ file.getId()
					+ " file name is : "
					+ file.getName()
					+ " error code is : " + ind.errorCode);
		} else if (ind.indType == FileTransStatusIndication.IND_TYPE_TRANS_ERR) {
			if (file.getState() == VFile.State.DOWNLOADING) {
				file.setState(VFile.State.DOWNLOAD_FAILED);
			} else {
				file.setState(VFile.State.UPLOAD_FAILED);
			}
			V2Log.e("CrowdFilesActivity handleFileTransNotification --> TRANS_ERROR ...file id is : "
					+ file.getId()
					+ " file name is : "
					+ file.getName()
					+ " error code is : " + ind.errorCode);

			Intent i = new Intent();
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			i.setAction(PublicIntent.BROADCAST_CROWD_FILE_ACTIVITY_SEND_NOTIFICATION);
			i.putExtra("fileID", file.getId());
			sendBroadcast(i);
		}
		adapter.notifyDataSetChanged();
	}

	/**
	 * Handle file removed notification
	 * @param files
	 */
	private void handleFileRemovedEvent(List<VCrowdFile> files) {
		for (VCrowdFile removedFile : files) {
			for (int i = 0; i < mServerExistFiles.size(); i++) {
				if (mServerExistFiles.get(i).getId()
						.equals(removedFile.getId())) {
					VCrowdFile file = mServerExistFiles.get(i);
					V2Log.d(TAG,
							"handleFileRemovedEvent -->The group file was remove! cancel downloading was called!");
					if(file.getState() == State.DOWNLOADING){
						GlobalHolder.getInstance().changeGlobleTransFileMember(
								V2GlobalConstants.FILE_TRANS_DOWNLOADING, mContext, false,
								crowd.getmGId(), "CrowdFilesActivity handleFileRemovedEvent");
						service.handleCrowdFile(file,
								FileOperationEnum.OPERATION_CANCEL_DOWNLOADING,
								null);
					}
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
				if (vf.getState() != VFile.State.UPLOAD_FAILED) {
					vf.setState(VFile.State.UPLOAD_PAUSE);
					service.handleCrowdFile(vf,
							FileOperationEnum.OPERATION_PAUSE_SENDING, null);
				}
			} else {
				if (vf.getState() != VFile.State.UPLOAD_FAILED) {
					vf.setState(VFile.State.UPLOADING);
					service.handleCrowdFile(vf,
							FileOperationEnum.OPERATION_RESUME_SEND, null);
				}
			}
		}
	}

	private void suspendOrResumeDownloadingFiles(boolean flag) {
		for (int i = 0; i < this.mServerExistFiles.size(); i++) {
			VCrowdFile vf = mServerExistFiles.get(i);
			if (flag) {
				if (vf.getState() == VFile.State.DOWNLOADING) {
					vf.setState(VFile.State.DOWNLOAD_PAUSE);
					service.handleCrowdFile(vf,
							FileOperationEnum.OPERATION_PAUSE_DOWNLOADING, null);
				}
			} else {
				if (vf.getState() == VFile.State.DOWNLOAD_PAUSE) {
					vf.setState(VFile.State.DOWNLOADING);
					service.handleCrowdFile(vf,
							FileOperationEnum.OPERATION_RESUME_DOWNLOAD, null);
				}
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
			VMessage vm = mUploadingVMFiles.get(vCrowdFile.getId());
			if (vm != null) {
				vCrowdFile.setState(State.DOWNLOADED);
				vCrowdFile.setPath(vm.getFileItems().get(0).getFilePath());
			}
			mServerExistFiles.add(0, vCrowdFile);
		}
		adapter.notifyDataSetChanged();
	}

	private void handleFileRemovedEvent(VCrowdFile file) {
		List<VCrowdFile> list = mServerExistFiles;
		if (this.showUploaded) {
			list = this.mUploadedFiles;
		}

		if (file == null) {
			return;
		}
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getId().equals(file.getId())) {
				list.remove(i);
				CommonCallBack.getInstance().executeUpdateCrowdFileState(
						file.getId(), null, CrowdFileExeType.DELETE_FILE);
				if (file.getState() == VCrowdFile.State.DOWNLOADING) {
					V2Log.d(TAG,
							"handleFileRemovedEvent --> cancel downloading was called!");
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
			mUploadingFileNotificationIcon.setVisibility(View.GONE);
			if (mUploadedFiles.size() <= 0)
				mUploadFinish.setVisibility(View.VISIBLE);
			else
				mUploadFinish.setVisibility(View.INVISIBLE);
		} else {
			mUploadFinish.setVisibility(View.INVISIBLE);
			if (mUploadedFiles.size() > 0)
				mUploadingFileNotificationIcon.setVisibility(View.VISIBLE);
			else
				mUploadingFileNotificationIcon.setVisibility(View.GONE);
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
			onBackPressed();
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
//				// set cancel button text to upload text
				mShowUploadedFileButton
						.setText(R.string.crowd_files_title_upload);
				onBackPressed();
//				isInDeleteMode = false;
//				suspendOrResumeDownloadingFiles(false);
//				adapter.notifyDataSetChanged();
			} else {
				Intent intent = new Intent(mContext,
						ConversationSelectFile.class);
				intent.putExtra("type", "crowdFile");
				intent.putExtra("uid", crowd.getmGId());
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
				} else {
					boolean showDeleteMode = false;
					if (crowd.getOwnerUser().getmUserId() == currentLoginUserID) {
						showDeleteMode = true;
						suspendOrResumeDownloadingFiles(true);
					} else {
						for (VCrowdFile file : mServerExistFiles) {
							if (file.getUploader().getmUserId() == currentLoginUserID) {
								showDeleteMode = true;
								break;
							}
						}
					}

					if (showDeleteMode) {
						isInDeleteMode = true;
						// Pause all uploading files
						adapter.notifyDataSetChanged();
						// update upload button text to cancel
						mShowUploadedFileButton
								.setText(R.string.crowd_files_title_cancel_button);
						mUploadingFileNotificationIcon.setClickable(false);
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
				} else if (res.getResult() != JNIResponse.Result.SUCCESS
						& res.getResult() != JNIResponse.Result.TIME_OUT) {
					V2Log.e(TAG, "Get upload files failed ... ERROR CODE IS : "
							+ res.getResult().name());
					Toast.makeText(
							mContext,
							getResources().getString(
									R.string.crowd_files_fill_adapter_failed),
							Toast.LENGTH_SHORT).show();
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
			case NEW_FILE_NOTIFICATION:
				AsyncResult result = (AsyncResult) msg.obj;
				RequestFetchGroupFilesResponse newFileni = (RequestFetchGroupFilesResponse) result
						.getResult();
				if (newFileni.getResult() == JNIResponse.Result.SUCCESS) {
					if (newFileni.getGroupID() == crowd.getmGId()) {
						handleNewFileEvent(newFileni.getList());
						adapterUploadShow();
					}
				}
				break;
			}
		}

	};

	class LocalReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(JNIService.JNI_BROADCAST_KICED_CROWD)) {
				GroupUserObject obj = intent.getParcelableExtra("group");
				if (obj == null) {
					V2Log.e("CrowdFilesActivity",
							"Received the broadcast to quit the crowd group , but crowd id is wroing... ");
					return;
				}
				if (obj.getmGroupId() == crowd.getmGId()) {
					for (VCrowdFile f : mServerExistFiles) {
						if (f.getState() == VFile.State.DOWNLOADING) {
							V2Log.d(TAG,
									"JNI_BROADCAST_KICED_CROWD --> cancel downloading was called!");
							service.handleCrowdFile(
									f,
									FileOperationEnum.OPERATION_CANCEL_DOWNLOADING,
									null);
						} else if (f.getState() == VFile.State.UPLOADING) {
							V2Log.d(TAG,
									"JNI_BROADCAST_KICED_CROWD --> cancel sending was called!");
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
					for (int i = 0; i < mUploadedFiles.size(); i++) {
						VCrowdFile temp = mUploadedFiles.get(i);
						if (temp.getState() == State.UPLOAD_PAUSE
								|| temp.getState() == State.UPLOADING) {
							temp.setState(State.UPLOAD_FAILED);
						}
					}
					
					for (int i = 0; i < mServerExistFiles.size(); i++) {
						VCrowdFile temp = mServerExistFiles.get(i);
						if (temp.getState() == State.DOWNLOAD_PAUSE
								|| temp.getState() == State.DOWNLOADING) {
							temp.setState(State.DOWNLOAD_FAILED);
						}
					}
					adapter.notifyDataSetChanged();
				}
			} else if(intent.getAction().equals(
					JNIService.JNI_BROADCAST_FILE_STATUS_ERROR_NOTIFICATION)){
				String fileID = intent.getStringExtra("fileID");
				int transType = intent.getIntExtra("transType", -1);
				if (fileID == null || transType == -1)
					return;
				
				if(transType == V2GlobalConstants.FILE_TRANS_SENDING){
					for (int i = 0; i < mUploadedFiles.size(); i++) {
						VCrowdFile temp = mUploadedFiles.get(i);
						if(temp.getId().equals(fileID)){
							temp.setState(State.UPLOAD_FAILED);
							break;
						}
					}
				} else {
					for (int i = 0; i < mServerExistFiles.size(); i++) {
						VCrowdFile temp = mServerExistFiles.get(i);
						if(temp.getId().equals(fileID)){
							temp.setState(State.DOWNLOAD_FAILED);
							break;
						}
					}
				}
				adapter.notifyDataSetChanged();
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
				return mServerExistFiles.size();
			}
		}

		@Override
		public Object getItem(int position) {
			if (showUploaded) {
				return mUploadedFiles.get(position);
			} else {
				return mServerExistFiles.get(position);
			}
		}

		@Override
		public long getItemId(int position) {
			if (showUploaded) {
				return mUploadedFiles.get(position).hashCode();
			} else {
				return mServerExistFiles.get(position).hashCode();
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
				file = mServerExistFiles.get(position);
			}

			ViewItem item = null;
			Tag tag = null;
			if (convertView == null) {
				convertView = layoutInflater.inflate(
						R.layout.crowd_file_adapter_item, null);
				item = new ViewItem();
				tag = new Tag(file, item);
				item.mFileDeleteModeButton = (ImageView) convertView
						.findViewById(R.id.ic_delete);
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
					&& (file.getUploader().getmUserId() == currentLoginUserID || GlobalHolder
							.getInstance().getCurrentUserId() == crowd
							.getOwnerUser().getmUserId())) {
				item.mFileDeleteModeButton.setVisibility(View.VISIBLE);
			} else {
				item.mFileDeleteModeButton.setVisibility(View.GONE);
				// Record flag for show delete button
				file.setFlag(HIDE_DELETE_BUTTON_FLAG);
			}

			item.mFileIcon.setImageResource(FileUitls.adapterFileIcon(file
					.getName()));

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
				if (file.getUploader().getmUserId() == GlobalHolder
						.getInstance().getCurrentUserId()) {
					item.mFileButton
							.setText(R.string.crowd_files_name_open_file);
					item.mFileButton.setVisibility(View.VISIBLE);
					item.mFileText.setVisibility(View.GONE);
					item.mFileProgress.setVisibility(View.GONE);
					item.mFailedIcon.setVisibility(View.GONE);
					item.mProgressLayout.setVisibility(View.GONE);
				} else {
					item.mFileButton
							.setText(R.string.crowd_files_button_name_download);
					item.mFailedIcon.setVisibility(View.GONE);
					item.mFileText.setVisibility(View.GONE);
					item.mFileButton.setVisibility(View.VISIBLE);
					item.mProgressLayout.setVisibility(View.GONE);
				}
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
			
			item.mProgressParent.measure(MeasureSpec.UNSPECIFIED,
					MeasureSpec.UNSPECIFIED);
			updateProgress(item, file);
		}

		private void updateProgress(ViewItem item, VCrowdFile file) {

			item.mFileProgress.setVisibility(View.VISIBLE);
			if (!isFromStartState && (file.getState() == VFile.State.UPLOAD_PAUSE || 
					file.getState() == VFile.State.DOWNLOAD_PAUSE)) {
				item.mFileProgress.setText(file.getProceedSizeStr() + "/"
						+ file.getFileSizeStr());
				item.mVelocity.setText("0kb/S");
				
				double percent = ((double) file.getProceedSize() / (double) file
						.getSize());
				int width = item.mProgressLayout.getMeasuredWidth();
				ViewGroup.LayoutParams vl = item.mProgress.getLayoutParams();
				vl.width = (int) (width * percent);
				item.mProgress.setLayoutParams(vl);
			} else {
				FileDownLoadBean bean = GlobalHolder.getInstance().globleFileProgress
						.get(file.getId());
				double percent = 0;
				float speed = 0;
				if (bean != null) {
					file.setProceedSize(bean.currentLoadSize);
					long sec = (System.currentTimeMillis() - bean.lastLoadTime);
					long size = file.getProceedSize() - bean.lastLoadSize;
					percent = ((double) file.getProceedSize() / (double) file
							.getSize());
					speed = (size / sec) * 1000;
					item.mVelocity.setText(file.getFileSize(speed) + "/S");
				} else {
					item.mVelocity.setText("0kb/S");
				}
				
				item.mFileProgress.setText(file.getProceedSizeStr() + "/"
						+ file.getFileSizeStr());

				int width = item.mProgressLayout.getMeasuredWidth();
				ViewGroup.LayoutParams vl = item.mProgress.getLayoutParams();
				vl.width = (int) (width * percent);
				item.mProgress.setLayoutParams(vl);
			}
		}

		private OnClickListener mFailIconListener = new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (!GlobalHolder.getInstance().isServerConnected()) {
					Toast.makeText(mContext,
							R.string.error_discussion_no_network,
							Toast.LENGTH_SHORT).show();
					return;
				}

				VCrowdFile file = (VCrowdFile) v.getTag();
				if (file.getState() == VFile.State.REMOVED) {
					mServerExistFiles.remove(file);
					V2Toast.makeText(mContext,
							R.string.crowd_files_deleted_notification,
							Toast.LENGTH_SHORT).show();
				} else {
					int transType;
					if (file.getState() == VFile.State.UPLOAD_FAILED) {
						transType = V2GlobalConstants.FILE_TRANS_SENDING;
					} else {
						transType = V2GlobalConstants.FILE_TRANS_DOWNLOADING;
					}
						
					boolean flag = GlobalHolder.getInstance().changeGlobleTransFileMember(transType, mContext, true,
							crowd.getmGId(), "CrowdFilesActivity mFailIconListener");
					if(!flag)
						return ;

					if (file.getState() == VFile.State.DOWNLOAD_FAILED) {
						GlobalHolder.getInstance().changeGlobleTransFileMember(
								V2GlobalConstants.FILE_TRANS_DOWNLOADING, mContext,
								true, crowd.getmGId(),
								"CrowdFilesActivity mFailIconListener");
						file.setState(VFile.State.DOWNLOADING);
						file.setProceedSize(0);
						mShowProgressFileMap.put(file.getId(), file);
						file.setStartTime(new Date(GlobalConfig
								.getGlobalServerTime()));
						service.handleCrowdFile(file,
								FileOperationEnum.OPERATION_START_DOWNLOAD,
								null);
					} else if (file.getState() == VFile.State.UPLOAD_FAILED) {
						GlobalHolder.getInstance().changeGlobleTransFileMember(
								V2GlobalConstants.FILE_TRANS_SENDING, mContext,
								true, crowd.getmGId(),
								"CrowdFilesActivity mFailIconListener");
						file.setState(VFile.State.UPLOADING);
						mShowProgressFileMap.put(file.getId(), file);
						file.setProceedSize(0);
						file.setStartTime(new Date(GlobalConfig
								.getGlobalServerTime()));
						service.handleCrowdFile(file,
								FileOperationEnum.OPERATION_START_SEND, null);
					}
					VMessage vMessage = mUploadingVMFiles.get(file.getId());
					if (vMessage != null) {
						// 更新数据库
						VMessageFileItem fileItem = vMessage.getFileItems()
								.get(0);
						fileItem.setState(file.getState().intValue());
						fileItem.setUuid(file.getId());
						MessageBuilder.updateVMessageItem(mContext, fileItem);
						// 通知聊天界面更新
						Intent i = new Intent();
						i.addCategory(PublicIntent.DEFAULT_CATEGORY);
						i.setAction(PublicIntent.BROADCAST_CROWD_FILE_ACTIVITY_SEND_NOTIFICATION);
						i.putExtra("exeType",
								VMessageAbstractItem.STATE_FILE_SENDING);
						i.putExtra("fileID", file.getId());
						sendBroadcast(i);
					} else {
						VMessage vm = new VMessage(
								V2GlobalConstants.GROUP_TYPE_CROWD, crowd.getmGId(),
								file.getUploader(), GlobalHolder.getInstance()
										.getCurrentUser(), new Date(
										GlobalConfig.getGlobalServerTime()));
						VMessageFileItem fileItem = new VMessageFileItem(vm,
								file.getName(), file.getState().intValue(),
								file.getId());
						fileItem.setFilePath(file.getPath());
						vm.setDate(new Date(GlobalConfig.getGlobalServerTime()));
						MessageBuilder.saveFileVMessage(mContext, vm);
						mUploadingVMFiles.put(file.getId(), vm);
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

				if (!GlobalHolder.getInstance().isServerConnected()) {
					Toast.makeText(mContext,
							R.string.error_local_connect_to_server,
							Toast.LENGTH_SHORT).show();
					return;
				}

				Tag tag = (Tag) v.getTag();
				int transType = -1;
				if (tag.vf.getState() == State.DOWNLOADING
						|| tag.vf.getState() == State.DOWNLOAD_PAUSE) {
					transType = V2GlobalConstants.FILE_TRANS_DOWNLOADING;
				} else if (tag.vf.getState() == State.UPLOADING
						|| tag.vf.getState() == State.UPLOAD_PAUSE) {
					transType = V2GlobalConstants.FILE_TRANS_SENDING;
				}
				GlobalHolder.getInstance().changeGlobleTransFileMember(
						transType, mContext, false, crowd.getmGId(),
						"CrowdFilesActivity mDeleteButtonListener");
				List<VCrowdFile> list = new ArrayList<VCrowdFile>();
				list.add(tag.vf);
				handleFileRemovedEvent(tag.vf);
				service.removeGroupFiles(crowd, list, null);
				VMessage vMessage = mUploadingVMFiles.get(tag.vf.getId());
				MessageLoader.deleteFileItem(tag.vf.getId());
				if (vMessage != null) {
					CommonCallBack.getInstance().executeUpdateCrowdFileState(
							tag.vf.getId(), vMessage,
							CrowdFileExeType.DELETE_FILE);
				} else {
					V2Log.e(TAG,
							"Call P2PACtivity to delete this message failed ... id is : "
									+ tag.vf.getId() + " name is : "
									+ tag.vf.getName());
				}
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

				if (!GlobalHolder.getInstance().isServerConnected()) {
					Toast.makeText(mContext,
							R.string.error_discussion_no_network,
							Toast.LENGTH_SHORT).show();
					return;
				}

				VCrowdFile file = (VCrowdFile) v.getTag();
				if (file.getState() == VFile.State.DOWNLOADED
						|| (file.getState() == VFile.State.UPLOADED && file
								.getUploader().getmUserId() == GlobalHolder
								.getInstance().getCurrentUserId())) {
					FileUitls.openFile(file.getPath());
					return;
				}

				boolean isUpdatePath = false;
				boolean isUpload = false;
				if (file.getState() == VFile.State.UNKNOWN) {
					
					boolean isAdd = GlobalHolder.getInstance().changeGlobleTransFileMember(
							V2GlobalConstants.FILE_TRANS_DOWNLOADING, mContext,
							true, crowd.getmGId(),
							"CrowdFilesActivity mButtonListener");
					if(!isAdd){
						return ;
					}
					
					int count = 0;
					for (int i = 0; i < mServerExistFiles.size(); i++) {
						VCrowdFile temp = mServerExistFiles.get(i);
						if(!temp.getId().equals(file.getId())){
							if(temp.getName().equals(file.getName())){
								File tempFile = buildDefaultFilePath(temp);
								if(tempFile.exists()){
									count++;
								}
							}
						}
					}
					// if count > 0 mean have repeat file
					if(count != 0){
						String name = file.getName();
						String prefix = name.substring(0, name.indexOf("."));
						String postfixName = name.substring(name.indexOf("."));
						String newName = prefix + "("+count+")" + postfixName;
						file.setName(newName);
						
						String prefixPath = file.getPath().substring(0 , file.getPath().lastIndexOf("/"));
						String newPath = prefixPath + "/" + newName;
						file.setPath(newPath);
						isUpdatePath = true;
					}
					
					file.setState(VFile.State.DOWNLOADING);
					file.setStartTime(new Date(GlobalConfig
							.getGlobalServerTime()));
					((TextView) v)
							.setText(R.string.crowd_files_button_name_pause);
					mShowProgressFileMap.put(file.getId(), file);
					service.handleCrowdFile(file,
							FileOperationEnum.OPERATION_START_DOWNLOAD, null);
				} else if (file.getState() == VFile.State.DOWNLOADING) {
					file.setState(VFile.State.DOWNLOAD_PAUSE);
					mPauseFiles.put(file.getId(), file);
					((TextView) v)
							.setText(R.string.crowd_files_button_name_resume);
					service.handleCrowdFile(file,
							FileOperationEnum.OPERATION_PAUSE_DOWNLOADING, null);
				} else if (file.getState() == VFile.State.DOWNLOAD_PAUSE) {
					file.setState(VFile.State.DOWNLOADING);
					mPauseFiles.remove(file.getId());
					((TextView) v)
							.setText(R.string.crowd_files_button_name_pause);
					service.handleCrowdFile(file,
							FileOperationEnum.OPERATION_RESUME_DOWNLOAD, null);
				} else if (file.getState() == VFile.State.UPLOADING) {
					file.setState(VFile.State.UPLOAD_PAUSE);
					mPauseFiles.put(file.getId(), file);
					((TextView) v)
							.setText(R.string.crowd_files_button_name_pause);
					service.handleCrowdFile(file,
							FileOperationEnum.OPERATION_PAUSE_SENDING, null);
					isUpload = true;
				} else if (file.getState() == VFile.State.UPLOAD_PAUSE) {
					if (isInDeleteMode) {
						Toast.makeText(mContext,
								R.string.crowd_files_resume_uploading_failed,
								Toast.LENGTH_SHORT).show();
						return;
					}

					file.setState(VFile.State.UPLOADING);
					mPauseFiles.remove(file.getId());
					((TextView) v)
							.setText(R.string.crowd_files_button_name_resume);
					v.invalidate();
					service.handleCrowdFile(file,
							FileOperationEnum.OPERATION_RESUME_SEND, null);
					isUpload = true;
				} else {

				}
				adapter.notifyDataSetChanged();

				VMessageFileItem fileItem = MessageLoader
						.queryFileItemByID(file.getId());
				// save state to database
				if (fileItem != null) {
					if(isUpdatePath){
						fileItem.setFilePath(file.getPath());
						fileItem.setFileName(file.getName());
					}
					fileItem.setState(file.getState().intValue());
					fileItem.setUuid(file.getId());
					MessageBuilder.updateVMessageItem(mContext, fileItem);
				} else {
					V2Log.e(TAG, "没有从数据库获取到文件对象！");
					// 处于下载状态，如果数据库中不存在该记录，需要创建该记录
					VMessage vm = new VMessage(crowd.getGroupType().intValue(),
							crowd.getmGId(), file.getUploader(), null,
							new Date(GlobalConfig.getGlobalServerTime()));
					VMessageFileItem newFileItem = new VMessageFileItem(vm, file.getPath(), file.getState()
							.intValue());
					if(isUpdatePath){
						newFileItem.setFilePath(file.getPath());
						newFileItem.setFileName(file.getName());
					}
					vm.getFileItems().get(0).setUuid(file.getId());
					vm.setmXmlDatas(vm.toXml());
					MessageBuilder.saveFileVMessage(mContext, vm);
					vm = null;
				}

				if (isUpload) {
					CommonCallBack.getInstance().executeUpdateCrowdFileState(
							file.getId(), fileItem.getVm(),
							CrowdFileExeType.UPDATE_FILE);
				}
			}
		};
	}

	public enum CrowdFileActivityType {
		CROWD_FILE_ACTIVITY(0), CROWD_FILE_UPLOING_ACTIVITY(1), UNKNOWN(2);

		private int type;

		private CrowdFileActivityType(int type) {
			this.type = type;
		}

		public static CrowdFileActivityType fromInt(int code) {
			switch (code) {
			case 0:
				return CROWD_FILE_ACTIVITY;
			case 1:
				return CROWD_FILE_UPLOING_ACTIVITY;
			default:
				return UNKNOWN;

			}
		}

		public int intValue() {
			return type;
		}
	}
}
