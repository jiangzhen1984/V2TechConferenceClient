package com.v2tech.view.adapter;

import com.v2tech.view.widget.CommonAdapter.CommonAdapterItemWrapper;
import com.v2tech.vo.VMessage;

public class VMessageAdater implements CommonAdapterItemWrapper {

	private VMessage vm;

	public VMessageAdater(VMessage vm) {
		super();
		this.vm = vm;
	}

	@Override
	public Object getItemObject() {
		return vm;
	}

	@Override
	public long getItemLongId() {
		return vm.getId();
	}

}
