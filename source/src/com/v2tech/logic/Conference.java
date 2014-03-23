package com.v2tech.logic;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Conference {

	private long id;

	private String name;
	private String startTime;
	private String endTime;
	private List<User> invitedList;
	private Date d;

	public Conference(long id) {
		this.id = id;
	}

	public Conference(String name, String startTime, String endTime,
			List<User> invitedList) {
		this.name = name;
		this.startTime = startTime;
		this.endTime = endTime;
		this.invitedList = invitedList;
	}

	public long getID() {
		return this.id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Conference other = (Conference) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	
	public String getName() {
		return this.name;
	}
	
	public String getStartTimeStr() {
		return this.startTime;
	}
	
	public Date getDate() {
		if (d == null && this.startTime != null && this.startTime.trim().length() == 16) {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
			try {
				d = df.parse(this.startTime);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			return d;
		}
		return null;
	}
	
	public long getId() {
		return this.id;
	}

	/**
	 * <conf canaudio="1" candataop="1" canvideo="1" conftype="0" haskey="0" //
	 * id="0" key="" // layout="1" lockchat="0" lockconf="0" lockfiletrans="0"
	 * mode="2" // pollingvideo="0" // subject="ss" // chairuserid='0'
	 * chairnickname=''> // </conf>
	 * 
	 * @return
	 */
	public String getConferenceConfigXml() {
		StringBuilder sb = new StringBuilder();
		sb.append(
				"<conf canaudio=\"1\" candataop=\"1\" canvideo=\"1\" conftype=\"0\" haskey=\"0\" ")
				.append(" id=\"0\" key=\"\" layout=\"1\" lockchat=\"0\" lockconf=\"0\" lockfiletrans=\"0\" mode=\"2\" pollingvideo=\"0\" ")
				.append(" syncdesktop=\"0\" syncdocument=\"1\" syncvideo=\"0\" ")
				.append("subject=\" ").append(this.name).append("\" ")
				.append("chairuserid=\"")
				.append(GlobalHolder.getInstance().getCurrentUserId())
				.append("\" ").append("chairnickname=\"")
				.append(GlobalHolder.getInstance().getCurrentUser().getName())
				.append("\"  starttime=\""+this.startTime+"\" >").append("</conf>");
		return sb.toString();

	}

	public String getInvitedAttendeesXml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<xml>");
		for (User u : this.invitedList) {
			sb.append("<user id=\"").append(u.getmUserId()).append("\" ")
					.append("nickname=\"").append(u.getName()).append("\"/>");
		}

		sb.append("</xml>");
		return sb.toString();
	}

}
