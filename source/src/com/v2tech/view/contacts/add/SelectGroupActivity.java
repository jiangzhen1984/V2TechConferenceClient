package com.v2tech.view.contacts.add;

import com.v2tech.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class SelectGroupActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact_add_select_group);

		// 返回
		findViewById(R.id.contact_detail_return_button).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						onBackPressed();
					}
				});

		// 选择分组
		findViewById(R.id.radio_button).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						onBackPressed();
					}
				});

	}
}
