package com.v2tech.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Process;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.v2tech.R;

public class SettingTabFragment extends Fragment {
	
	private Tab1BroadcastReceiver receiver;
	private IntentFilter intentFilter;
	
	private TextView mQuitButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.tab_fragment_setting, container, false);
		mQuitButton = (TextView) v.findViewById(R.id.setting_quit_button);
		mQuitButton.setOnClickListener(mQuitButtonListener);
		return v;
	}

	@Override
	public void onPause() {

		super.onPause();
		getActivity().unregisterReceiver(receiver);
	}

	@Override
	public void onResume() {

		super.onResume();
		receiver = new Tab1BroadcastReceiver();
		getActivity().registerReceiver(receiver, getIntentFilter());
	}

	private IntentFilter getIntentFilter() {
		if (intentFilter == null) {
			intentFilter = new IntentFilter();
			intentFilter.addAction("TAB1_ACTION");
		}
		return intentFilter;
	}

	
	private OnClickListener mQuitButtonListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			getActivity().finish();
			Process.killProcess(Process.myPid());
		}
		
	}; 
	
	
	
	class Tab1BroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals("TAB1_ACTION")) {
			}
		}

	}
}
