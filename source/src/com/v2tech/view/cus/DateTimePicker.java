package com.v2tech.view.cus;

import java.util.Calendar;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupWindow;

import com.v2tech.R;
import com.v2tech.R.drawable;

public class DateTimePicker extends PopupWindow {
	
	private Calendar ca;
	
	private EditText mYearET;
	private EditText mMonthET;
	private EditText mDayET;
	private EditText mHourET;
	private EditText mMinuteET;
	
	private OnDateSetListener listener;
	
	
	public DateTimePicker(Context context) {
		super(context);
		init(context);
	}
	
	

	
	
	public DateTimePicker(Context context, int width, int height) {
		super(width, height);
		init(context);
	}



	private void init(Context context) {
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.date_time_picker, null,
				false);
		View v = inflater.inflate(R.layout.date_time_picker, null,
				false);
		this.setContentView(v);
		this.setFocusable(true);
		this.setTouchable(true);
		this.setOutsideTouchable(true);
		this.setContentView(v);
		this.setBackgroundDrawable(new ColorDrawable(drawable.transparent));
		
		ca = Calendar.getInstance();
	}

	

	public interface OnDateSetListener {
		
		public void onDateTimeSet(int year,
									int monthOfYear, int dayOfMonth, int hour, int minute);
	}
	
	
	public void setOnDateSetListener(OnDateSetListener listener) {
		this.listener = listener;
	}
}
