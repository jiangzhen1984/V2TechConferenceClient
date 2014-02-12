package com.v2tech.view.conversation;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.v2tech.R;

public class VideoConversationFragment extends Fragment {

	private static final int UPDATE_TIME = 1;

	private SurfaceView mLocalSurface;
	private TextView mTimerTV;

	private long mTimeLine = 0;

	private LocalHandler mLocalHandler;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.video_conversation_main, container,
				false);
		mLocalSurface = (SurfaceView) v
				.findViewById(R.id.video_conversation_local_surface_view);

		mTimerTV = (TextView) v
				.findViewById(R.id.video_conversation_time_counter);

		mLocalHandler = new LocalHandler(Looper.getMainLooper());
		 Message.obtain(mLocalHandler, UPDATE_TIME).sendToTarget();
		return v;
	}

	@Override
	public void onStart() {
		super.onStart();
		// VideoRecorder.VideoPreviewSurfaceHolder = mLocalSurface.getHolder();
		// VideoRecorder.VideoPreviewSurfaceHolder
		// .setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		// VideoRecorder.VideoPreviewSurfaceHolder
		// .setFormat(PixelFormat.TRANSPARENT);
	}

	private void updateTimer() {
		int hour = (int) mTimeLine / 3600;

		int minute = (int) (mTimeLine - (hour * 3600)) / 60;

		int second = (int) mTimeLine - (hour * 3600 + minute * 60);
		mTimerTV.setText((hour < 10 ? "0" + hour : hour) + ":"
				+ (minute < 10 ? "0" + minute : minute) + ":"
				+ (second < 10 ? "0" + second : second));
	}

	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	class LocalHandler extends Handler {

		public LocalHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_TIME:
				mTimeLine++;
				updateTimer();
				Message m = Message.obtain(mLocalHandler, UPDATE_TIME);
				mLocalHandler.sendMessageDelayed(m, 1000);
				break;
			}
		}

	}

}
