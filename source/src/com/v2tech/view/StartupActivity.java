package com.v2tech.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class StartupActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences sf = this.getSharedPreferences("config", Context.MODE_PRIVATE);
		int flag = sf.getInt("LoggedIn", 0);
		System.out.println(flag +"=========================");
		if (flag == 1) {
			startActivity(new Intent(this, MainActivity.class));
		} else {
			startActivity(new Intent(this, LoginActivity.class));
		}
		finish();
	}
	
	

}
