package com.bizcom.vc.adapter;

import android.view.View;

import com.bizcom.vc.adapter.CommonAdapter.CommonAdapterItemDateAndViewWrapper;
import com.bizcom.vo.VMessage;

public class VMessageDataAndViewWrapper implements CommonAdapterItemDateAndViewWrapper {

	private VMessage vm;
	private View v;
	public VMessageDataAndViewWrapper(VMessage vm) {
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

	public void setVm(VMessage vm) {
		this.vm = vm;
	}

	@Override
	public View getView() {
		return v;
	}
	
	public void setView(View v){
		this.v = v;
	}
}
