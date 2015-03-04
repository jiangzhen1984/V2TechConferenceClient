package com.bizcom.vc.activity.setting;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bizcom.request.V2ImRequest;
import com.bizcom.request.util.HandlerWrap;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vo.User;
import com.v2tech.R;

public class SettingAuthenticationActivity extends Activity {

	// rg_authentication
	private RadioGroup rgAutentication;
	private V2ImRequest service;
	private static final int UPDATEUSER_CALLBACK = 0;
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case UPDATEUSER_CALLBACK:
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_fragment_setting_authentication);
		service = new V2ImRequest();
		findViewById(R.id.setting_back).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						onBackPressed();
					}
				});

		rgAutentication = (RadioGroup) findViewById(R.id.rg_authentication);
		switch (GlobalHolder.getInstance().getCurrentUser().getAuthtype()) {
		case 0:
			rgAutentication.check(R.id.rb_allow_anybogy);
			break;
		case 1:
			rgAutentication.check(R.id.rb_require_authorization);
			break;
		case 2:
			rgAutentication.check(R.id.rb_unallow_anybogy);
			break;
		}

	}
	
	@Override
	public void onBackPressed() {
		if (!GlobalHolder.getInstance().isServerConnected()) {
			Toast.makeText(SettingAuthenticationActivity.this, R.string.error_local_connect_to_server, Toast.LENGTH_SHORT).show();
		}
		else{
			int authtype = 0;
			switch (rgAutentication.getCheckedRadioButtonId()) {
			case R.id.rb_allow_anybogy:// 允许任何人
				authtype = 0;
				break;
			case R.id.rb_require_authorization:// 需要验证
				authtype = 1;
				break;
			case R.id.rb_unallow_anybogy:// 不允许任何人
				authtype = 2;
				break;
			default:
				authtype = 0;
				break;
			}

			User currentUser = GlobalHolder.getInstance().getCurrentUser();
			currentUser.setAuthtype(authtype);
			service.updateUserInfo(currentUser, new HandlerWrap(mHandler,
					UPDATEUSER_CALLBACK, null));
		}
		super.onBackPressed();
	}
}
