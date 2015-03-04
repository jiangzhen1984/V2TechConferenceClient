package com.bizcom.vc.activity.crow;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.bizcom.bo.GroupUserObject;
import com.bizcom.request.V2CrowdGroupRequest;
import com.bizcom.request.util.HandlerWrap;
import com.bizcom.util.LocalSharedPreferencesStorage;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.service.JNIService;
import com.bizcom.vo.CrowdGroup;
import com.bizcom.vo.Group.GroupType;
import com.v2tech.R;

public class CrowdContentUpdateActivity extends Activity {

	public final static int UPDATE_TYPE_BRIEF = 1;
	public final static int UPDATE_TYPE_ANNOUNCEMENT = 2;

	private final static int REQUEST_UPDATE_CROWD_DONE = 1;

	private EditText mContentET;
	private TextView mContentTitle;
	private TextView mReturnButton;

	private TextView mUpdateButton;

	private CrowdGroup crowd;
	private V2CrowdGroupRequest service = new V2CrowdGroupRequest();
	private State mState = State.NONE;
	private boolean inEditMode;
	private int mType;
	private LocalReceiver localReceiver;
	private Toast mToast;
	private Context mContext;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.crowd_content_activity);
		mContext = this;
		mContentET = (EditText) findViewById(R.id.crowd_content_et);
		mContentTitle = (TextView) findViewById(R.id.crowd_content_title);

		mUpdateButton = (TextView) findViewById(R.id.crowd_content_update_button);
		mUpdateButton.setOnClickListener(mUpdateButtonListener);

		mReturnButton = (TextView) findViewById(R.id.crowd_return_button);
		mReturnButton.setOnClickListener(mReturnButtonListener);

		crowd = (CrowdGroup) GlobalHolder.getInstance().getGroupById(
				GroupType.CHATING.intValue(),
				getIntent().getExtras().getLong("cid"));

		if (crowd == null
				|| GlobalHolder.getInstance().getCurrentUserId() != crowd
						.getOwnerUser().getmUserId()) {
			mUpdateButton.setVisibility(View.GONE);
		}

		mType = getIntent().getExtras().getInt("type");
		updateView(mType, inEditMode);
		overridePendingTransition(R.anim.left_in, R.anim.left_out);
		initReceiver();
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.right_in, R.anim.right_out);
	}

	@Override
	public void onBackPressed() {
		if (inEditMode) {
			inEditMode = false;
			updateView(mType, inEditMode);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		service.clearCalledBack();
		this.unregisterReceiver(localReceiver);
	}

	private void initReceiver() {
		localReceiver = new LocalReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(JNIService.JNI_BROADCAST_KICED_CROWD);
		filter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
		this.registerReceiver(localReceiver, filter);
	}

	private void updateView(int type, boolean editMode) {
		if (editMode) {
			mUpdateButton
					.setText(R.string.crowd_content_udpate_announce_button);
			mContentTitle.setText(R.string.crowd_content_title);
			mContentET.setEnabled(true);
			mContentET.requestFocus();
		} else {
			mContentET.setEnabled(false);
			mUpdateButton.setText(R.string.crowd_content_title);
			if (type == UPDATE_TYPE_BRIEF) {
				mContentET.setText(crowd.getBrief());
				mContentTitle.setText(R.string.crowd_content_brief);
			} else if (type == UPDATE_TYPE_ANNOUNCEMENT) {
				mContentET.setText(crowd.getAnnouncement());
				mContentTitle.setText(R.string.crowd_content_announce);
			}
		}

	}

	class LocalReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(JNIService.JNI_BROADCAST_KICED_CROWD)) {
				GroupUserObject obj = intent.getParcelableExtra("group");
				if (obj == null) {
					V2Log.e("CrowdContentUpdateActivity",
							"Received the broadcast to quit the crowd group , but crowd id is wroing... ");
					return;
				}
				if (obj.getmGroupId() == crowd.getmGId()) {
					finish();
				}
			}

		}

	}

	private OnClickListener mReturnButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			onBackPressed();

		}

	};

	private OnClickListener mUpdateButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (!inEditMode) {
				inEditMode = true;
				updateView(mType, inEditMode);
			} else {
				synchronized (mState) {
					if (mState == State.PENDING) {
						return;
					}
					
					if (!GlobalHolder.getInstance().isServerConnected()) {
						Toast.makeText(mContext, R.string.error_local_connect_to_server, Toast.LENGTH_SHORT).show();
						return ;
					}
					mState = State.PENDING;
				}
				if (mType == UPDATE_TYPE_BRIEF) {
					crowd.setBrief(mContentET.getText().toString());
				} else if (mType == UPDATE_TYPE_ANNOUNCEMENT) {
					crowd.setAnnouncement(mContentET.getText().toString());
				}
				service.updateCrowd(crowd, new HandlerWrap(mLocalHandler,
						REQUEST_UPDATE_CROWD_DONE, null));
			}
		}

	};

	private Handler mLocalHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case REQUEST_UPDATE_CROWD_DONE:
				synchronized (mState) {
					mState = State.NONE;
				}
				if (mToast != null) {
					mToast.cancel();
				}

				mToast = Toast.makeText(CrowdContentUpdateActivity.this,
						R.string.crowd_content_udpate_succeed,
						Toast.LENGTH_SHORT);
				mToast.show();
				setResult(mType, null);
				inEditMode = false;
				updateView(mType, inEditMode);
				// finish();
				break;
			}
		}

	};

	enum State {
		NONE, PENDING;
	}

}
