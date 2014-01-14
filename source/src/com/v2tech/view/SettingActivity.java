package com.v2tech.view;

import com.v2tech.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class SettingActivity extends Activity {

	private Context mContext;
	private ImageView mSettingButtonIV;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.activity_setting);
		
		mSettingButtonIV =(ImageView)findViewById(R.id.setting_conf_button);
		mSettingButtonIV.setOnClickListener(mConfsButtonListener);
	}

	
	private OnClickListener mConfsButtonListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			mContext.startActivity(new Intent(mContext, ConfsActivity.class));
			//overridePendingTransition(R.animator.down_in,R.animator.down_out);
			finish();
		}
		
	};
	
}
