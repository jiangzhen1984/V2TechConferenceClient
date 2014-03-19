package com.v2tech.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.util.GlobalConfig;
import com.v2tech.util.Notificator;

public class SettingTabFragment extends Fragment {
	
	private Tab1BroadcastReceiver receiver;
	private IntentFilter intentFilter;
	
	private TextView mQuitButton;
	private TextView mVersionTV;

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
		mVersionTV  = (TextView) v.findViewById(R.id.verson_id);
		mVersionTV.setText(GlobalConfig.GLOBAL_VERSION_NAME);
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
			//FIXME optimze code
			SharedPreferences sf = getActivity().getSharedPreferences("config", Context.MODE_PRIVATE);
			Editor ed = sf.edit();
			ed.putInt("LoggedIn", 0);
			ed.commit();
			Notificator.cancelSystemNotification(getActivity());
			getActivity().finish();
			System.exit(0);
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
