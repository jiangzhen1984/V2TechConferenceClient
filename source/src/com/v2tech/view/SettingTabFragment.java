package com.v2tech.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.util.GlobalConfig;

public class SettingTabFragment extends Fragment {

	private TextView mQuitButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.tab_fragment_setting, container,
				false);
		mQuitButton = (TextView) v.findViewById(R.id.setting_quit_button);
		mQuitButton.setOnClickListener(mQuitButtonListener);
		
		
		v.findViewById(R.id.go_authentication).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent intent=new Intent(SettingTabFragment.this.getActivity(),SettingAuthenticationActivity.class);
				SettingTabFragment.this.getActivity().startActivity(intent);
			}
		});;
		
		
		return v;
	}

	@Override
	public void onPause() {

		super.onPause();
	}

	@Override
	public void onResume() {

		super.onResume();
	}


	private OnClickListener mQuitButtonListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			//FIXME optimize code for wrap similar code
			GlobalConfig.saveLogoutFlag(getActivity());

			Intent i = new Intent();
			i.setAction(PublicIntent.FINISH_APPLICATION);
			i.addCategory(PublicIntent.DEFAULT_CATEGORY);
			getActivity().sendBroadcast(i);
		}

	};

}
