package com.v2tech.view;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.util.GlobalConfig;

public class AboutActivity extends Activity {

	private TextView mVersion;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		mVersion = (TextView)findViewById(R.id.about_version);
		mVersion.setText(GlobalConfig.GLOBAL_VERSION_NAME);
	}
	
	

}
