package com.v2tech.view;

import v2av.VideoRecorder;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
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

import com.V2.jni.ConfRequest;
import com.V2.jni.VideoRequest;
import com.v2tech.R;

public class VideoActivity extends Activity {

	private static final int ONLY_SHOW_LOCAL_VIDEO = 1;

	private VideoRequest mVideo = VideoRequest.getInstance(this);
	private ConfRequest mCR = ConfRequest.getInstance(this);

	private Handler mVideoHandler = new VideoHandler();

	private Context mContext;
	private SurfaceView mLocalSurView;
	private RelativeLayout mVideoLayout;

	private ImageView mSettingIV;
	private ImageView mQuitIV;
	private PopupWindow mSettingWindow;
	private static int Measuredwidth = 0;
	private static int Measuredheight = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_in_metting);
		mContext = this;
		this.mVideoLayout = (RelativeLayout) findViewById(R.id.in_metting_video_main);
		this.mSettingIV = (ImageView) findViewById(R.id.in_meeting_setting_iv);
		this.mSettingIV.setOnClickListener(mShowSettingListener);
		this.mQuitIV = (ImageView) findViewById(R.id.in_meeting_log_out_iv);
		this.mQuitIV.setOnClickListener(mShowQuitWindowListener);
		init();
	}

	private OnClickListener mShowQuitWindowListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			showQuitDialog();			
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

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void init() {
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
	}

	
	private void showQuitDialog() {
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
		d.show();
	}
	
	
	private void quit() {
		//mVideo.closeVideoChat(nGroupID, nToUserID, szDeviceID, businessType);
		//mVideo.closeVideoDevice(nGroupID, nUserID, szDeviceID, vp, businessType);
	//	mCR.leaveConf(nConfID)
		Intent i = new Intent();
		i.putExtra("error_msg", "退出失败");
		this.setResult(1, i);
		finish();
	}
	
	
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showQuitDialog();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}




	class VideoHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ONLY_SHOW_LOCAL_VIDEO:
				showLocalSurViewOnly();
				break;
			}
		}

	}

}
