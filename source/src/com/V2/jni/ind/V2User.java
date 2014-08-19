package com.V2.jni.ind;

public class V2User {

	
	public long uid;
	public String name;
	//2 means non-registered user
	public int type;
	
	
	public V2User() {
		
	}
	
	public V2User(long uid) {
		super();
		this.uid = uid;
	}
	public V2User(long uid, String name) {
		super();
		this.uid = uid;
		this.name = name;
	}
	
	
	
	
}
