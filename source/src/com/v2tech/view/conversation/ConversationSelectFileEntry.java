package com.v2tech.view.conversation;

import java.math.BigDecimal;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.view.bo.ConversationNotificationObject;

public class ConversationSelectFileEntry extends Activity implements OnClickListener{

	private RelativeLayout entryImage;
	private RelativeLayout entryFile;
	private TextView backKey;
	private ConversationNotificationObject cov;
	private ArrayList<FileInfoBean> mCheckedList;
	
	private TextView selectedFileSize;
	private TextView sendButton;
	private long totalSize;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_selectfile_entry);
		
		findview();
		
		Intent intent = getIntent();
		cov = intent.getParcelableExtra("obj");
		mCheckedList = intent.getParcelableArrayListExtra("checkedFiles");
		if(mCheckedList == null){
			mCheckedList = new ArrayList<FileInfoBean>();
		}
		else if(mCheckedList != null && mCheckedList.size() > 0){
			
			sendButton.setClickable(true);
			sendButton.setOnClickListener(this);
			sendButton.setBackgroundResource(R.drawable.conversation_selectfile_send_able);
			for (FileInfoBean bean : mCheckedList) {
				
				totalSize += bean.fileSize;
			}
			selectedFileSize.setText("已选" + getFileSize(totalSize));
			sendButton.setText("发送("+mCheckedList.size()+")");
		}
	}

	private void findview() {
		
		entryImage = (RelativeLayout) findViewById(R.id.selectfile_entry_image);
		entryFile = (RelativeLayout) findViewById(R.id.selectfile_entry_file);
		backKey = (TextView) findViewById(R.id.selectfile_back);
		
		selectedFileSize = (TextView) findViewById(R.id.selectfile_entry_size);
		sendButton = (TextView) findViewById(R.id.selectfile_message_send);
		
		entryImage.setOnClickListener(this);
		entryFile.setOnClickListener(this);
		backKey.setOnClickListener(this);
		
	}

	@Override
	public void onClick(View v) {
		
		Intent intent = new Intent(this , ConversationSelectFile.class);
		intent.putExtra("obj", cov);
		intent.putParcelableArrayListExtra("checkedFiles", mCheckedList);
		switch (v.getId()) {
			case R.id.selectfile_entry_image:
				intent.putExtra("type", "image");
				startActivity(intent);
				break;
			case R.id.selectfile_entry_file:
				intent.putExtra("type", "file");
				startActivity(intent);
				break;
			case R.id.selectfile_back:
				Intent backIntent = new Intent(this , ConversationView.class);
				backIntent.putExtra("obj", cov);
				startActivity(backIntent);
				break;
			case R.id.selectfile_message_send:
				mCheckedList.clear();
				Intent sendIntent = new Intent(this , ConversationView.class);
				sendIntent.putParcelableArrayListExtra("checkedFiles", mCheckedList);
				sendIntent.putExtra("obj", cov);
				startActivity(sendIntent);
			default:
				break;
		}
		mCheckedList.clear();
		finish();
	}
	
	/**
	 * 获取文件大小
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

}
