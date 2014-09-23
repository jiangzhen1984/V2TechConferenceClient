package com.v2tech.vo;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.graphics.Bitmap;
import android.text.format.DateUtils;

public class ContactConversation extends Conversation {
	
	private User u;
	private CharSequence msg;
	private String date;
	private String dateLong;
	
	public String getDateLong() {
		return dateLong;
	}

	public void setDateLong(String dateLong) {
		this.dateLong = dateLong;
	}
	
	public ContactConversation(User u) {
		super();
		this.u = u;
		if (u != null) {
			this.mExtId =u.getmUserId();
			this.mType = TYPE_CONTACT;
		}
	}
	

	@Override
	public String getName() {
		if (u != null) {
			return u.getName();
		}
		return super.getName();
	}

	@Override
	public CharSequence getMsg() {
		return msg;
	}

	@Override
	public String getDate() {
		if(dateLong != null){
			long longDate = Long.valueOf(dateLong);
			Date dates = new Date(longDate);
			SimpleDateFormat format = null;
			if(DateUtils.isToday(longDate)){
				format = new SimpleDateFormat("HH:mm:ss");
				return format.format(dates.getTime());
			}
			
			Calendar cale = Calendar.getInstance();
			cale.setTime(dates);
			Calendar currentCale = Calendar.getInstance();
			int days = cale.get(Calendar.DAY_OF_MONTH);
			int currentCaleDays = currentCale.get(Calendar.DAY_OF_MONTH);
			if(currentCaleDays - 1 == days){
				format = new SimpleDateFormat("HH:mm:ss");
				return "昨天  " + format.format(dates);
			}
		}
		return date;
	}
	
	public void setMsg(CharSequence msg) {
		this.msg = msg;
	}
	
	public void setDate(String date) {
		this.date = date;
	}
	
	public Bitmap getAvatar() {
		if (u != null) {
			return u.getAvatarBitmap();
		} else {
			return null;
		}
	}
	
	public void updateUser(User u) {
		this.u = u;
	}
	
	
	
}
