package com.v2tech.view.conversation;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.util.StorageUtil;
import com.v2tech.util.V2Log;
import com.v2tech.view.bo.ConversationNotificationObject;

public class ConversationSelectFile extends Activity{

	protected static final String TAG = "ConversationSelectFile";
	private String mCurrentPath = StorageUtil.getSdcardPath();
	private TextView backButton;
	private TextView finishButton;
	private ListView filesList;
	
	private ArrayList<FileInfoBean> mFileLists;
	private ArrayList<FileInfoBean> mCheckedList;
	private FileListAdapter adapter;
	private ConversationNotificationObject cov;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_selectfile);
		findview();
		init();
		setListener();
		
		DisplayMetrics outMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
	}
	
	private void findview() {
		
		backButton = (TextView) findViewById(R.id.selectfile_back);
		finishButton = (TextView) findViewById(R.id.selectfile_finish);
		filesList = (ListView) findViewById(R.id.selectfile_lsitview);
	}
	
	private void init() {
		
		cov = getIntent().getParcelableExtra("obj");
		
		mCheckedList = new ArrayList<FileInfoBean>();
		updateFileItems(mCurrentPath);
		V2Log.d(TAG, "当前mFileLists集合中个数：" + mFileLists.size());
		adapter = new FileListAdapter();
		filesList.setAdapter(adapter);
	}
	
	private void setListener() {
		
		backButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if(StorageUtil.getSdcardPath().equals(mCurrentPath)){
					mCheckedList.clear();
					mFileLists.clear();
					//如果已经是顶级路径，则结束掉当前界面
					Intent intent = new Intent(ConversationSelectFile.this , ConversationView.class);
					intent.putExtra("obj", cov);
					startActivity(intent);
					finish();
					return ;
				}
				File file = new File(mCurrentPath);
				mCurrentPath = file.getParent();
				V2Log.d(TAG, "当前文件路径：" + mCurrentPath);
				updateFileItems(file.getParent());
				adapter.notifyDataSetChanged();
			}
		});
		
		finishButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ConversationSelectFile.this , ConversationView.class);
				intent.putParcelableArrayListExtra("checkedFiles", mCheckedList);
				intent.putExtra("obj", cov);
				startActivity(intent);
				mFileLists.clear();
				finish();
			}
		});
		
		filesList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				FileInfoBean bean = mFileLists.get(position);
				if(bean.isDir){
					mCurrentPath = bean.filePath;
					V2Log.d(TAG, "当前文件路径：" + mCurrentPath);
					updateFileItems(bean.filePath);
					adapter.notifyDataSetChanged();
				}
				else{
					RadioButton button = (RadioButton) view.findViewById(R.id.selectfile_adapter_check);
					if(button.isChecked()){
						button.setChecked(false);
						mCheckedList.remove(bean);
					}
					else{
						button.setChecked(true);
						mCheckedList.add(bean);
					}
				}
			}
		});
		
	}
	
	/**
	 * 根据文件路径更新当前ListView
	 * @param path
	 */
	private void updateFileItems(String path) {
		
		if(mFileLists == null)
			mFileLists = new ArrayList<FileInfoBean>();
		else
			mFileLists.clear();
		
		File[] files = folderScan(path);
		if(files == null){
			return ;
		}
		
		FileInfoBean file = null;
		File currentFile;
		for (int i = 0; i < files.length; i++) {
			
			if(files[i].isHidden()){
				continue;
			}
			
			currentFile = files[i];
			file = new FileInfoBean();
			
			if(currentFile.isDirectory() && currentFile.canRead()){
				
				File[] listFiles = currentFile.listFiles();
				if(listFiles != null && listFiles.length > 0){
					
					file.fileItmes = String.valueOf(listFiles.length);
				}
				else{
					
					file.fileItmes = "0";
				}
				file.isDir = true;
			}
			else{
				file.fileSize = gainFileItemsSize(currentFile.length());
				file.fileDate = gainFileDate(currentFile.lastModified());
				file.isDir = false;
			}
			file.fileName = currentFile.getName();
			file.filePath = currentFile.getAbsolutePath();
			mFileLists.add(file);
		}
	}
	
	/**
	 * 获取文件大小
	 * @param size
	 * @return
	 */
	private String gainFileItemsSize(long size) {
		
		 BigDecimal filesize  =   new  BigDecimal(size);
	     BigDecimal megabyte  =   new  BigDecimal( 1024 * 1024);
	     float  returnValue  =  filesize.divide(megabyte,  2 , BigDecimal.ROUND_UP).floatValue();
	     if  (returnValue  >   1 )
	        return (returnValue  +   "MB");
	     
	     BigDecimal kilobyte  =   new  BigDecimal( 1024 );
	     returnValue  =  filesize.divide(kilobyte,  2 , BigDecimal.ROUND_UP).floatValue();
	     return (returnValue  +   "  KB " );
	}

	/**
	  * 获得当前路径的所有文件  
	  * @param path
	  * @return
	  */
    private File[] folderScan(String path) {
    	
        File file = new File(path);  
        File[] files = file.listFiles();  
        return files;  
    }  

    /**
     * 获取文件创建日期
     * @param fileName
     * @return
     */
    private String gainFileDate(long time) {
    	
    	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
        Date date = new Date(time);
        return formatter.format(date);
	}


	class FileListAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			return mFileLists.size();
		}

		@Override
		public Object getItem(int position) {
			return mFileLists.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			ViewHolder holder;
			if(convertView == null){
				
				holder = new ViewHolder();
				convertView = View.inflate(ConversationSelectFile.this, R.layout.activity_selectfile_adapter, null);
				holder.fileName = (TextView) convertView.findViewById(R.id.selectfile_adapter_name);
				holder.fileFolderName = (TextView) convertView.findViewById(R.id.selectfile_adapter_folderName);
				holder.fileDate = (TextView) convertView.findViewById(R.id.selectfile_adapter_date);
				holder.fileSize = (TextView) convertView.findViewById(R.id.selectfile_adapter_size);
				holder.fileNumbers = (TextView) convertView.findViewById(R.id.selectfile_adapter_numbers);
				holder.fileCheck = (RadioButton) convertView.findViewById(R.id.selectfile_adapter_check);
				convertView.setTag(holder);
			}
			else{
				
				holder = (ViewHolder) convertView.getTag();
			}
			
			holder.fileDate.setText(mFileLists.get(position).fileDate);  
			holder.fileCheck.setTag(mFileLists.get(position));
			if(mFileLists.get(position).isDir){
				holder.fileSize.setVisibility(View.GONE);
				holder.fileName.setVisibility(View.GONE);
				holder.fileDate.setVisibility(View.GONE);
				holder.fileFolderName.setVisibility(View.VISIBLE);
				holder.fileNumbers.setVisibility(View.VISIBLE);
				
				holder.fileFolderName.setText(mFileLists.get(position).fileName);
				holder.fileNumbers.setText("( " + mFileLists.get(position).fileItmes + " )");
			}
			else{
				holder.fileSize.setVisibility(View.VISIBLE);
				holder.fileName.setVisibility(View.VISIBLE);
				holder.fileDate.setVisibility(View.VISIBLE);
				holder.fileFolderName.setVisibility(View.GONE);
				holder.fileNumbers.setVisibility(View.GONE);
				
				holder.fileName.setText(mFileLists.get(position).fileName);
				holder.fileSize.setText(mFileLists.get(position).fileSize);
				holder.fileDate.setText(mFileLists.get(position).fileDate);
			}
			
//			holder.fileCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//				
//				@Override
//				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//					
//					FileInfoBean bean = (FileInfoBean) buttonView.getTag();
//					if(isChecked)
//						mCheckedList.add(bean);
//					else
//						mCheckedList.remove(bean);
//				}
//			});
			return convertView;
		}
	}
	
	class ViewHolder{
		
		public TextView fileName;
		public TextView fileDate;
		public TextView fileSize;
		public TextView fileNumbers;
		public RadioButton fileCheck;
		public TextView fileFolderName;
	}
	
}
