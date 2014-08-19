package com.V2.jni.ind;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class V2Group {
	
	
	
	/**
	 * Organization type
	 */
	public static final int TYPE_ORG = 1;
	
	/**
	 * Contacts group type
	 */
	public static final int TYPE_CONTACTS_GROUP = 2;
	
	/**
	 * Crowd type
	 */
	public static final int TYPE_CROWD = 3;
	
	/**
	 * Conference type
	 */
	public static final int TYPE_CONF = 4;
	
	public long id;
	public String name;
	public int type;
	public V2User owner;
	
	public V2Group parent;
	public Set<V2Group> childs = new HashSet<V2Group>();
	
	//for conference
	public Date createTime;
	public V2User chairMan;
	
	
	
	
	public V2Group(long id, String name, int type) {
		super();
		this.id = id;
		this.name = name;
		this.type = type;
	}


	public V2Group(long id, String name, int type, V2User owner) {
		super();
		this.id = id;
		this.name = name;
		this.type = type;
		this.owner = owner;
	}


	public V2Group(long id, String name, int type, V2User owner, Date createTime) {
		super();
		this.id = id;
		this.name = name;
		this.type = type;
		this.owner = owner;
		this.createTime = createTime;
	}
	
	
	public V2Group(long id, String name, int type, V2User owner, Date createTime, V2User chairMan) {
		super();
		this.id = id;
		this.name = name;
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
	
	
	
	

}
