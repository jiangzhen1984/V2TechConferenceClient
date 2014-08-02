package com.v2tech.view.cus;

import java.util.Calendar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.v2tech.R;

public class DateTimePicker extends PopupWindow {

	private Calendar ca;

	private EditText mYearET;
	private EditText mMonthET;
	private EditText mDayET;
	private EditText mHourET;
	private EditText mMinuteET;
	private TextView mSetTV;
	private TextView mCancelTV;

	private OnDateSetListener listener;

	private PopupWindow mRoot;

	public DateTimePicker(Context context) {
		super(context);
		init(context);
	}

	public DateTimePicker(Context context, int width, int height) {
		super(width, height);
		init(context);
	}

	private void init(Context context) {
		mRoot = this;
		LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.date_time_picker, null, false);
		this.setContentView(v);
		this.setFocusable(true);
		this.setTouchable(true);
		this.setOutsideTouchable(true);
		this.setContentView(v);
		this.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		mYearET = (EditText) v.findViewById(R.id.date_time_picker_year);
		mMonthET = (EditText) v.findViewById(R.id.date_time_picker_month);
		mDayET = (EditText) v.findViewById(R.id.date_time_picker_day);
		mHourET = (EditText) v.findViewById(R.id.date_time_picker_hour);
		mMinuteET = (EditText) v.findViewById(R.id.date_time_picker_minute);
		mSetTV = (TextView) v.findViewById(R.id.date_time_picker_setting);
		mSetTV.setOnClickListener(mSettingListener);
		mCancelTV = (TextView) v.findViewById(R.id.date_time_picker_cancel);
		mCancelTV.setOnClickListener(mCancelListener);
		ca = Calendar.getInstance();
		updateTime();

		View wid = null;
		wid = v.findViewById(R.id.date_time_button_plus_year);
		wid.setOnClickListener(this.mUpdateDateTimeListner);

		wid = v.findViewById(R.id.date_time_button_plus_month);
		wid.setOnClickListener(this.mUpdateDateTimeListner);

		wid = v.findViewById(R.id.date_time_button_plus_day);
		wid.setOnClickListener(this.mUpdateDateTimeListner);

		wid = v.findViewById(R.id.date_time_button_plus_hour);
		wid.setOnClickListener(this.mUpdateDateTimeListner);

		wid = v.findViewById(R.id.date_time_button_plus_minute);
		wid.setOnClickListener(this.mUpdateDateTimeListner);

		wid = v.findViewById(R.id.date_time_button_minus_year);
		wid.setOnClickListener(this.mUpdateDateTimeListner);

		wid = v.findViewById(R.id.date_time_button_minus_month);
		wid.setOnClickListener(this.mUpdateDateTimeListner);

		wid = v.findViewById(R.id.date_time_button_minus_day);
		wid.setOnClickListener(this.mUpdateDateTimeListner);

		wid = v.findViewById(R.id.date_time_button_minus_hour);
		wid.setOnClickListener(this.mUpdateDateTimeListner);

		wid = v.findViewById(R.id.date_time_button_minus_minute);
		wid.setOnClickListener(this.mUpdateDateTimeListner);

	}

	private OnClickListener mSettingListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (listener != null) {
				int year = 0, monthOfYear = 0, dayOfMonth = 0, hour = 0, minute = 0;
				String yearStr = mYearET.getText().toString();
				if (yearStr != null && !yearStr.equals("")
						&& TextUtils.isDigitsOnly(yearStr)) {
					year = Integer.parseInt(yearStr);
				}
				String monthStr = mMonthET.getText().toString();
				if (monthStr != null && !monthStr.equals("")
						&& TextUtils.isDigitsOnly(monthStr)) {
					monthOfYear = Integer.parseInt(monthStr);
				}
				String dayStr = mDayET.getText().toString();
				if (dayStr != null && !dayStr.equals("")
						&& TextUtils.isDigitsOnly(dayStr)) {
					dayOfMonth = Integer.parseInt(dayStr);
				}
				String hourStr = mHourET.getText().toString();
				if (hourStr != null && !hourStr.equals("")
						&& TextUtils.isDigitsOnly(hourStr)) {
					hour = Integer.parseInt(hourStr);
				}
				String minuteStr = mMinuteET.getText().toString();
				if (minuteStr != null && !minuteStr.equals("")
						&& TextUtils.isDigitsOnly(minuteStr)) {
					minute = Integer.parseInt(minuteStr);
				}

				listener.onDateTimeSet(year, monthOfYear, dayOfMonth, hour,
						minute);
			}
			mRoot.dismiss();
		}

	};

	private OnClickListener mCancelListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			mRoot.dismiss();
		}

	};

	private OnClickListener mUpdateDateTimeListner = new OnClickListener() {

		@Override
		public void onClick(View v) {
			int id = v.getId();
			switch (id) {
			case R.id.date_time_button_plus_year:
				ca.add(Calendar.YEAR, 1);
				break;
			case R.id.date_time_button_plus_month:
				ca.add(Calendar.MONTH, 1);
				break;
			case R.id.date_time_button_plus_day:
				ca.add(Calendar.DAY_OF_MONTH, 1);
				break;
			case R.id.date_time_button_plus_hour:
				ca.add(Calendar.HOUR_OF_DAY, 1);
				break;
			case R.id.date_time_button_plus_minute:
				ca.add(Calendar.MINUTE, 1);
				break;
			case R.id.date_time_button_minus_year:
				ca.add(Calendar.YEAR, -1);
				break;
			case R.id.date_time_button_minus_month:
				ca.add(Calendar.MONTH, -1);
				break;
			case R.id.date_time_button_minus_day:
				ca.add(Calendar.DAY_OF_MONTH, -1);
				break;
			case R.id.date_time_button_minus_hour:
				ca.add(Calendar.HOUR_OF_DAY, -1);
				break;
			case R.id.date_time_button_minus_minute:
				ca.add(Calendar.MINUTE, -1);
				break;

			}
			updateTime();
		}

	};

	public void updateCalendar(Calendar c) {
		this.ca = c;
	}

	private void updateTime() {
		mYearET.setText(ca.get(Calendar.YEAR) + "");
		mMonthET.setText((ca.get(Calendar.MONTH) + 1) + "");
		mDayET.setText(ca.get(Calendar.DAY_OF_MONTH) + "");
		int hour = ca.get(Calendar.HOUR_OF_DAY);
		mHourET.setText(hour < 10 ? ("0" + hour) : (hour + ""));
		int minute = ca.get(Calendar.MINUTE);
		mMinuteET.setText(minute < 10 ? ("0" + minute) : (minute + ""));
	}

	public interface OnDateSetListener {

		public void onDateTimeSet(int year, int monthOfYear, int dayOfMonth,
				int hour, int minute);
	}

	public void setOnDateSetListener(OnDateSetListener listener) {
		this.listener = listener;
	}

	@Override
	public void showAsDropDown(View anchor) {
		final Rect displayFrame = new Rect();
		anchor.getWindowVisibleDisplayFrame(displayFrame);
		final int anchorHeight = anchor.getHeight();
		int[] mDrawingLocation = new int[2];
		anchor.getLocationInWindow(mDrawingLocation);
		if (this.getContentView().getMeasuredWidth() <= 0) {
			this.getContentView().measure(
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
					android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		if (displayFrame.bottom - anchorHeight - mDrawingLocation[1] > this
				.getContentView().getMeasuredHeight()) {
			super.showAsDropDown(anchor);
		} else {
			super.showAsDropDown(anchor, 0, -(anchorHeight + this
					.getContentView().getMeasuredHeight()));
		}
	}

}
