package com.bizcom.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OrderedHashMap<K, V> extends HashMap<K, V> {
	private static final long serialVersionUID = 1L;
	private List<K> keylist = null;

	@Override
	public V put(K key, V value) {
		if (key != null) {
			if (keylist == null) {
				keylist = new ArrayList<K>();
			}
			keylist.add(key);
		}

		return super.put(key, value);
	}

	@Override
	public V remove(Object key) {
		if (key != null) {
			if (keylist == null) {
				keylist = new ArrayList<K>();
			} else {
				keylist.remove(key);
			}
		}

		return super.remove(key);
	}

	public List<K> keyOrderList() {
		if (keylist == null) {
			keylist = new ArrayList<K>();
		}
		return keylist;
	}

}
