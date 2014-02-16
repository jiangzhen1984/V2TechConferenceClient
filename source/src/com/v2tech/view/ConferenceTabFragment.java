package com.v2tech.view;

import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.v2tech.R;
import com.v2tech.logic.AsynResult;
import com.v2tech.logic.Group;
import com.v2tech.util.V2Log;

public class ConferenceTabFragment extends Fragment {

	private static final int SERVER_BOUNDED = 1;
	private static final int FILL_CONFS_LIST = 2;
	private static final int REQUEST_ENTER_CONF = 3;
	private static final int REQUEST_ENTER_CONF_RESPONSE = 4;
	private static final int REQUEST_EXIT_CONF = 5;

	private static final int RETRY_COUNT = 10;

	private Tab1BroadcastReceiver receiver = new Tab1BroadcastReceiver();
	private IntentFilter intentFilter;

	private JNIService mService;

	private List<Group> mConferenceList;

	private LinearLayout mGroupContainer;

	private ConfsHandler mHandler = new ConfsHandler();

	private ProgressDialog mWaitingDialog;

	private long currentConfId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().registerReceiver(receiver, getIntentFilter());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.tab_fragment_conference, container,
				false);
		mGroupContainer = (LinearLayout) v
				.findViewById(R.id.group_list_container);
		return v;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(receiver);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	private IntentFilter getIntentFilter() {
		if (intentFilter == null) {
			intentFilter = new IntentFilter();
			intentFilter.addAction(JNIService.JNI_BROADCAST_GROUP_NOTIFICATION);
			intentFilter.addCategory(JNIService.JNI_BROADCAST_CATEGROY);
			intentFilter.addAction(MainActivity.SERVICE_BOUNDED_EVENT);
		}
		return intentFilter;
	}

	@Override
	public void onStart() {
		super.onStart();
		Message m = Message.obtain(mHandler, FILL_CONFS_LIST);
		mHandler.sendMessageDelayed(m, 500);
	}

	@Override
	public void onStop() {
		super.onStop();
		mHandler.removeMessages(FILL_CONFS_LIST);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Message.obtain(mHandler, REQUEST_EXIT_CONF, currentConfId)
				.sendToTarget();
	}

	private void addGroupList(List<Group> list) {
		if (list == null || list.size() <= 0) {
			V2Log.w(" group list is null");
			return;
		}
		if (mGroupContainer == null) {
			V2Log.e(" NO aviable layout");
			return;
		}
		mGroupContainer.removeAllViews();
		for (final Group g : list) {
			final GroupLayout gp = new GroupLayout(this.getActivity(), g);
			gp.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					mWaitingDialog = ProgressDialog
							.show(getActivity(),
									"",
									getActivity()
											.getResources()
											.getString(
													R.string.requesting_enter_conference),
									true);
					currentConfId = g.getmGId();
					Message.obtain(mHandler, REQUEST_ENTER_CONF,
							Long.valueOf(g.getmGId())).sendToTarget();
				}

			});
			mGroupContainer.addView(gp);
		}

	}

	class Tab1BroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals("TAB1_ACTION")) {
			} else if (intent.getAction().equals(
					JNIService.JNI_BROADCAST_GROUP_NOTIFICATION)) {
				Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
			} else if (intent.getAction().equals(
					MainActivity.SERVICE_BOUNDED_EVENT)) {
				mService = ((MainActivity) getActivity()).getService();
				Message.obtain(mHandler, FILL_CONFS_LIST).sendToTarget();
			}
		}

	}

	class ConfsHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SERVER_BOUNDED:
				mService = ((MainActivity) getActivity()).getService();
				if (mService != null) {
					Message.obtain(this, FILL_CONFS_LIST).sendToTarget();
				} else {
					this.sendMessageDelayed(
							Message.obtain(this, SERVER_BOUNDED), 500);
				}
				break;
			case FILL_CONFS_LIST:
				if (mService == null) {
					V2Log.w(" Doesn't bound service yet");
					return;
				}
				mConferenceList = mService.getGroup(Group.GroupType.CONFERENCE);
				// No server return send asynchronous message and waiting for
				// response
				if (mConferenceList != null) {
					addGroupList(mConferenceList);
				} else {
					if (msg.arg1 < RETRY_COUNT) {
						Message m = Message.obtain(this, FILL_CONFS_LIST,
								msg.arg1 + 1, 0);
						this.sendMessageDelayed(m, 1000);
					} else {
						Toast.makeText(getActivity(), "无法获取组信息",
								Toast.LENGTH_LONG).show();
					}
				}
				break;
			case REQUEST_ENTER_CONF:
				mService.requestEnterConference((Long) msg.obj,
						Message.obtain(this, REQUEST_ENTER_CONF_RESPONSE));
				break;
			case REQUEST_ENTER_CONF_RESPONSE:
				AsynResult ar = (AsynResult) msg.obj;
				if (ar.getState() == AsynResult.AsynState.SUCCESS) {
					Object[] re = (Object[]) ar.getObject();
					if ((Integer) re[0] == 0) {
						Intent i = new Intent(getActivity(),
								VideoActivityV2.class);
						i.putExtra("gid", (Long) re[1]);
						startActivityForResult(i, 0);
					} else {
						Toast.makeText(getActivity(),
								R.string.error_request_enter_conference,
								Toast.LENGTH_SHORT).show();
					}

				} else if (ar.getState() == AsynResult.AsynState.TIME_OUT) {
					Toast.makeText(getActivity(),
							R.string.error_request_enter_conference_time_out,
							Toast.LENGTH_SHORT).show();
					// TODO should send exit request
					// mService.requestEnterConference(confID, msg)
				} else {
					Toast.makeText(getActivity(),
							R.string.error_request_enter_conference,
							Toast.LENGTH_SHORT).show();
				}
				if (mWaitingDialog != null) {
					mWaitingDialog.dismiss();
				}

				break;
			case REQUEST_EXIT_CONF:
				mService.requestExitConference((Long) msg.obj, null);
				break;
			}
		}

	}

}
