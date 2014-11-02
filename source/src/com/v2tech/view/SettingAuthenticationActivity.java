package com.v2tech.view;

import com.v2tech.R;
import com.v2tech.service.GlobalHolder;
import com.v2tech.service.MessageListener;
import com.v2tech.service.UserService;
import com.v2tech.vo.User;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioGroup;

public class SettingAuthenticationActivity extends Activity {

	// rg_authentication
	private RadioGroup rgAutentication;
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
		new UserService().updateUser(currentUser, new MessageListener(mHandler,
				UPDATEUSER_CALLBACK, null));
		super.onBackPressed();
	}
}
