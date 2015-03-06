package com.V2.jni.ind;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.V2.jni.util.EscapedcharactersProcessing;
import com.bizcom.vc.application.V2GlobalConstants;

public class V2Group {

	/**
	 * Organization type
	 */
	public static final int TYPE_ORG = V2GlobalConstants.GROUP_TYPE_DEPARTMENT;

	/**
	 * Contacts group type
	 */
	public static final int TYPE_CONTACTS_GROUP = V2GlobalConstants.GROUP_TYPE_CONTACT;

	/**
	 * Crowd type
	 */
	public static final int TYPE_CROWD = V2GlobalConstants.GROUP_TYPE_CROWD;

	/**
	 * Conference type
	 */
	public static final int TYPE_CONF = V2GlobalConstants.GROUP_TYPE_CONFERENCE;

	/**
	 * Discussion board
	 */
	public static final int TYPE_DISCUSSION_BOARD = V2GlobalConstants.GROUP_TYPE_DISCUSSION;

	public long id;
	private String name;
	public int type;
	public BoUserInfoBase owner;

	public V2Group parent;
	public Set<V2Group> childs = new HashSet<V2Group>();
	public List<BoUserInfoBase> members;

	// for conference
	public Date createTime;
	public BoUserInfoBase chairMan;
	// remote user update sync attribute or note
	public boolean isUpdateSync;
	public boolean isSync;
	public boolean isVoiceActivation;
	// remote user update invitation attribute or note

	public boolean isUpdateInvitate;
	public boolean canInvitation;

	// for crowd
	public BoUserInfoBase creator;
	public int authType;
	private String brief;
	private String announce;
	public int groupSize;

	// for contact group
	public boolean isDefault;

	// for xml
	public String xml;

	public V2Group(int type) {
		super();
		this.type = type;
	}

	public V2Group(long id, int type) {
		this(id, null, type, null, null, null);
	}

	public V2Group(long id, String name, int type) {
		this(id, name, type, null, null, null);
	}

	public V2Group(long id, String name, int type, BoUserInfoBase owner) {
		this(id, name, type, owner, null, null);
	}

	public V2Group(long id, String name, int type, BoUserInfoBase owner, Date createTime) {
		this(id, name, type, owner, createTime, null);
	}

	public V2Group(long id, String name, int type, BoUserInfoBase owner,
			Date createTime, BoUserInfoBase chairMan) {
		super();
		this.id = id;
		this.name = EscapedcharactersProcessing.reverse(name);
		this.type = type;
		this.owner = owner;
		this.createTime = createTime;
		this.chairMan = chairMan;
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
		V2Group other = (V2Group) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public String getBrief() {
		return brief;
	}

	public void setBrief(String brief) {
		this.brief = EscapedcharactersProcessing.reverse(brief);
	}

	public String getAnnounce() {
		return announce;
	}

	public void setAnnounce(String announce) {
		this.announce = EscapedcharactersProcessing.reverse(announce);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = EscapedcharactersProcessing.reverse(name);
	}
}
