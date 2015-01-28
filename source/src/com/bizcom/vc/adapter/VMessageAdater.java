package com.bizcom.vc.adapter;

import android.view.View;

import com.bizcom.vc.adapter.CommonAdapter.CommonAdapterItemWrapper;
import com.bizcom.vo.VMessage;

public class VMessageAdater implements CommonAdapterItemWrapper {

	private VMessage vm;
	private View v;

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

	@Override
	public View getView() {
		return v;
	}

	public void setView(View v) {
		this.v = v;
	}

	public void setVm(VMessage vm) {
		this.vm = vm;
	}
}
