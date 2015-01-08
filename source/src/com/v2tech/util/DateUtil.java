package com.v2tech.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.text.format.DateUtils;

public class DateUtil {

	/**
	 * get specific format String date <br>
	 * <ul>
	 * today HH:mm:ss (时:分:秒)<br>
	 * yesterday : yesterday (昨天) <br>
	 * more before : yyyy-MM-dd HH:mm:ss <br>
	 * </ul>
	 * 
	 * @param longDate
	 * @return
	 */
	public static String getStringDate(long longDate) {
		SimpleDateFormat format = null;
		if (DateUtils.isToday(longDate))
			return getShortDate(longDate);

		Date dates = new Date(longDate);
		Calendar cale = Calendar.getInstance();
		cale.setTime(dates);
		Calendar currentCale = Calendar.getInstance();
		int days = cale.get(Calendar.DAY_OF_MONTH);
		int currentCaleDays = currentCale.get(Calendar.DAY_OF_MONTH);
		if (currentCaleDays - 1 == days) {
			return "昨天  " + getShortDate(longDate);
		}

		format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return format.format(longDate);
	}

	/**
	 * get the time format , like HH:mm:ss
	 * 
	 * @param mTimeLine
	 * @return
	 */
	public static String getShortDate(long mTimeLine) {

		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		Date currentTime = new Date(mTimeLine);
		String dateString = formatter.format(currentTime);
		return dateString;
	}

	/**
	 * get standard date time , like 2014-09-01 14:20:22
	 * 
	 * @return
	 */
	public static String getStandardDate(Date date) {

		if (date == null)
			throw new RuntimeException("Given date object is null...");
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return format.format(date.getTime());
	}
	
	/**
	 * get the time format , like HH:mm:ss
	 * 
	 * @param mTimeLine
	 * @return
	 */
	public static String calculateTime(long mTimeLine) {

		mTimeLine = mTimeLine / 1000;
		int hour = (int) mTimeLine / 3600;
		int minute = (int) (mTimeLine - (hour * 3600)) / 60;
		int second = (int) mTimeLine - (hour * 3600 + minute * 60);
		return (hour < 10 ? "0" + hour : hour) + ":"
				+ (minute < 10 ? "0" + minute : minute) + ":"
				+ (second < 10 ? "0" + second : second);
	}
}
