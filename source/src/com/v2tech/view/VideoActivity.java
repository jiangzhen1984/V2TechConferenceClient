package com.v2tech.view;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import v2av.VideoPlayer;
import v2av.VideoRecorder;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.V2.jni.ChatRequest;
import com.V2.jni.ConfRequest;
import com.V2.jni.ConfigRequest;
import com.V2.jni.VideoRequest;
import com.v2tech.R;
import com.v2tech.logic.GlobalHolder;
import com.v2tech.util.V2Log;

public class VideoActivity extends Activity {

	private static final int ONLY_SHOW_LOCAL_VIDEO = 1;
	
	private static final int GET_CONF_USER_LIST =10;

	
	
	
	public static final String JNI_EVENT_VIDEO_CATEGORY = "com.v2tech.conf_video_event";
	public static final String JNI_EVENT_VIDEO_CATEGORY_OPEN_VIDEO_EVENT_ACTION = "com.v2tech.conf_video_event.open_video_event";

	
	
	public static final String JNI_EVENT_CONF_USER_CATEGORY = "com.v2tech.conf_user_event";
	public static final String JNI_EVENT_CONF_USER_CATEGORY_GET_USER_LIST_ACTION = "com.v2tech.conf_user_event.get_user_list";
	public static final String JNI_EVENT_CONF_USER_CATEGORY_NEW_USER_ENTERED_ACTION = "com.v2tech.conf_user_event.new_user_entered";
		
	private VideoRequest mVideo = VideoRequest.getInstance(this);
	private ChatRequest mChat = ChatRequest.getInstance(this);
	private ConfRequest mCR = ConfRequest.getInstance(this);
	private ConfigRequest mConfig = new ConfigRequest();

	private Handler mVideoHandler = new VideoHandler();

	private Context mContext;
	private SurfaceView mLocalSurView;
	private RelativeLayout mVideoLayout;

	private ImageView mSettingIV;
	private ImageView mListUserIV;
	private ImageView mQuitIV;
	private PopupWindow mSettingWindow;
	private PopupWindow mUserListWindow;
	private Dialog mQuitDialog;
	private static int Measuredwidth = 0;
	private static int Measuredheight = 0;
	
	private Long mGroupId;
	private String mDeviceId;
	private Map<Long, VideoPlayer> mVPHolder = new HashMap<Long, VideoPlayer>();
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_in_metting);
		mContext = this;
		this.mVideoLayout = (RelativeLayout) findViewById(R.id.in_metting_video_main);
		this.mSettingIV = (ImageView) findViewById(R.id.in_meeting_setting_iv);
		this.mSettingIV.setOnClickListener(mShowSettingListener);
		this.mListUserIV = (ImageView) findViewById(R.id.in_meeting_show_attendee_iv);
		this.mListUserIV.setOnClickListener(mShowConfUsersListener);
		this.mQuitIV = (ImageView) findViewById(R.id.in_meeting_log_out_iv);
		this.mQuitIV.setOnClickListener(mShowQuitWindowListener);
		initConfsListener();
		init();
	}

	
	private OnClickListener mShowQuitWindowListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			showQuitDialog();
		}
		
	};
	
	
	private OnClickListener mShowConfUsersListener = new OnClickListener() {

		@Override
		public void onClick(View anchor) {
			if (mUserListWindow == null) {
				LayoutInflater inflater = (LayoutInflater) mContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View view = inflater.inflate(
						R.layout.in_meeting_user_list_pop_up_window, null);
				mUserListWindow = new PopupWindow(view,
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				mUserListWindow
						.setBackgroundDrawable(mContext
								.getResources()
								.getDrawable(
										R.drawable.rounded_corners_of_in_metting_setting_pop_up_window));
				mUserListWindow.setFocusable(true);
				mUserListWindow.setTouchable(true);
				mUserListWindow.setOutsideTouchable(true);
				mUserListWindow.setOnDismissListener(new OnDismissListener() {

					@Override
					public void onDismiss() {
						mUserListWindow.dismiss();
					}

				});
			}
			
			if (!mUserListWindow.isShowing()) {
				mUserListWindow.showAsDropDown(anchor);
			}
		}
		
	};
	
	private OnClickListener mShowSettingListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mSettingWindow == null) {
				LayoutInflater inflater = (LayoutInflater) mContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View view = inflater.inflate(
						R.layout.in_meeting_setting_pop_up_window, null);
				mSettingWindow = new PopupWindow(view,
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				mSettingWindow
						.setBackgroundDrawable(mContext
								.getResources()
								.getDrawable(
										R.drawable.rounded_corners_of_in_metting_setting_pop_up_window));
				mSettingWindow.setFocusable(true);
				mSettingWindow.setTouchable(true);
				mSettingWindow.setOutsideTouchable(true);
				mSettingWindow.setOnDismissListener(new OnDismissListener() {

					@Override
					public void onDismiss() {
						mSettingWindow.dismiss();
					}

				});
			}
			
			if (!mSettingWindow.isShowing()) {
				mSettingWindow.showAsDropDown(v);
			}

		}

	};

	
	private BroadcastReceiver mConfUserChangeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(JNI_EVENT_VIDEO_CATEGORY_OPEN_VIDEO_EVENT_ACTION)) {
				if (intent.getIntExtra("result", 1) != 0 ) {
					//TODO handle open video failed;
					Toast.makeText(context, R.string.error_in_meeting_open_video_falied, Toast.LENGTH_LONG).show();;				}
			}
		}
		
	};
	
	
	
	
	private void initConfsListener() {
		IntentFilter filter = new IntentFilter();
		filter.addCategory(JNI_EVENT_VIDEO_CATEGORY);
		filter.addAction(JNI_EVENT_VIDEO_CATEGORY_OPEN_VIDEO_EVENT_ACTION);
		
		filter.addCategory(JNI_EVENT_CONF_USER_CATEGORY);
		filter.addAction(JNI_EVENT_CONF_USER_CATEGORY_GET_USER_LIST_ACTION);
		filter.addAction(JNI_EVENT_CONF_USER_CATEGORY_NEW_USER_ENTERED_ACTION);
		mContext.registerReceiver(mConfUserChangeReceiver, filter);
	}
	
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void init() {
		mGroupId = this.getIntent().getLongExtra("gid", 0);
		if (mGroupId <= 0) {
			Toast.makeText(this, R.string.error_in_meeting_invalid_gid, Toast.LENGTH_LONG).show();
			return;
		}
		
		mDeviceId  = this.getIntent().getExtras().getString("did");
		if (mDeviceId == null || mDeviceId.equals("")) {
			Toast.makeText(this, R.string.error_in_meeting_invalid_gid, Toast.LENGTH_LONG).show();
			return;
		}
		
		Message.obtain(mVideoHandler, ONLY_SHOW_LOCAL_VIDEO).sendToTarget();

		Point size = new Point();
		WindowManager w = getWindowManager();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			w.getDefaultDisplay().getSize(size);

			Measuredwidth = size.x;
			Measuredheight = size.y;
		} else {
			Display d = w.getDefaultDisplay();
			Measuredwidth = d.getWidth();
			Measuredheight = d.getHeight();
		}


	}

	
	
	private void showLocalSurViewOnly() {
		
		File path = Environment.getExternalStorageDirectory();
		String szPath = path.getPath();
		mConfig.setExtStoragePath(szPath);
		V2Log.e(mChat.initialize(mChat)+"");
		
		if (mLocalSurView == null) {
			mLocalSurView = new SurfaceView(this);
		}
		mVideoLayout.removeAllViews();
		mVideoLayout.addView(mLocalSurView, new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));

		mLocalSurView.getHolder().setType(
				SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mLocalSurView.getHolder().setFormat(PixelFormat.TRANSPARENT);
		VideoRecorder.VideoPreviewSurfaceHolder = mLocalSurView.getHolder();
		
		VideoPlayer vp = new VideoPlayer();
		mVPHolder.put(GlobalHolder.getLoggedUserId(), vp);
		vp.SetSurface(mLocalSurView.getHolder());
		vp.SetViewSize(Measuredwidth, Measuredheight);
		
		mVideo.setDefaultVideoDev(mDeviceId);
		mCR.enterConf(mGroupId);
		mVideo.openVideoDevice(mGroupId, GlobalHolder.getLoggedUserId() , "", vp, 1);
	}
	
	
	
	
	
	
	
	 @Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		mContext.unregisterReceiver(mConfUserChangeReceiver);
		super.onDestroy();
		mVPHolder.clear();
	}







	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mQuitDialog != null && mQuitDialog.isShowing()) {
			mQuitDialog.dismiss();
		}
		
		if(mSettingWindow != null && mSettingWindow.isShowing()) {
			mSettingWindow.dismiss();
		}
		if (this.mUserListWindow != null && mUserListWindow.isShowing()) {
			mUserListWindow.dismiss();
		}
	}




	
	private void showQuitDialog() {
		if (mQuitDialog == null) {
			
			final Dialog d = new Dialog(mContext, R.style.InMeetingQuitDialog);
			
			d.setContentView(R.layout.in_meeting_quit_window);
			final Button cancelB = (Button)d.findViewById(R.id.IMWCancelButton);
			cancelB.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					d.dismiss();
				}
				
			});
			final Button quitB = (Button)d.findViewById(R.id.IMWQuitButton);
			quitB.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					d.dismiss();
					quit();
				}
				
			});
			mQuitDialog = d;
		}
		
		mQuitDialog.show();
	}
	
	
	private void quit() {
		//mVideo.closeVideoChat(nGroupID, nToUserID, szDeviceID, businessType);
		for(Map.Entry<Long, VideoPlayer> entry:this.mVPHolder.entrySet()) {
			mVideo.closeVideoDevice(mGroupId, entry.getKey(), mDeviceId, entry.getValue(), 1);
		}
		mCR.exitConf(this.mGroupId);
		
		Intent i = new Intent();
		i.putExtra("error_msg", "退出失败");
		this.setResult(1, i);
		finish();
	}
	
	
	
	private void getConfsUserList() {
		mCR.getConfUserList(this.mGroupId);
	}
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showQuitDialog();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	//TODO do log out



	class VideoHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ONLY_SHOW_LOCAL_VIDEO:
				showLocalSurViewOnly();
				break;
			case GET_CONF_USER_LIST:
				getConfsUserList();
				break;
			}
		}

	}

}
