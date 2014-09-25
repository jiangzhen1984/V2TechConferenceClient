package com.v2tech.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.util.GlobalConfig;

public class SettingTabFragment extends Fragment {
	private ViewGroup contentVeiw;
	// R.id.setting_quit_button
	private TextView mQuitButton;
	// R.id.rl_authentication
	private RelativeLayout rlAuthentication;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		contentVeiw = (ViewGroup) inflater.inflate(
				R.layout.tab_fragment_setting, container, false);
		connectView();
		bindViewEnvent();
		return contentVeiw;
	}

	private void connectView() {
		mQuitButton = (TextView) contentVeiw
				.findViewById(R.id.setting_quit_button);
		rlAuthentication = (RelativeLayout) contentVeiw
				.findViewById(R.id.rl_authentication);

	}

	private void bindViewEnvent() {
		mQuitButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				// FIXME optimize code for wrap similar code
				GlobalConfig.saveLogoutFlag(getActivity());

				Intent i = new Intent();
				i.setAction(PublicIntent.FINISH_APPLICATION);
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				getActivity().sendBroadcast(i);
			}

		});

		rlAuthentication.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(SettingTabFragment.this
						.getActivity(), SettingAuthenticationActivity.class);
				SettingTabFragment.this.getActivity().startActivity(intent);
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
	}
}
