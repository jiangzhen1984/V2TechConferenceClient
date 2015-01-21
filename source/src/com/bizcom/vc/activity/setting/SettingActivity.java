package com.bizcom.vc.activity.setting;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.v2tech.R;

public class SettingActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_root_layout);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new SettingTabFragment()).commit();
		}
		this.overridePendingTransition(R.animator.alpha_from_0_to_1,
				R.animator.alpha_from_1_to_0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void finish() {
		super.finish();
		this.overridePendingTransition(R.animator.alpha_from_0_to_1,
				R.animator.alpha_from_1_to_0);
	}

}
