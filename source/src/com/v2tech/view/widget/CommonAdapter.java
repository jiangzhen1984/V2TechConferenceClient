package com.v2tech.view.widget;

import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Use to wrap simple adapter for ListView
 * 
 * <ul>Use {@link ViewConvertListener} to wrap construct view object.</ul>
 * <ul>Use {@link CommonAdapterItemWrapper} to wrap List object. </ul>
 * @author jiangzhen
 *
 */
public class CommonAdapter extends BaseAdapter {

	
	private List<CommonAdapterItemWrapper> adapter;
	
	private ViewConvertListener listener;
	
	private int  batchCount;
	
	public interface CommonAdapterItemWrapper {
		public Object getItemObject();
		public long getItemLongId();
	}
	
	public interface ViewConvertListener {
		public View converView(CommonAdapterItemWrapper wr, View view, ViewGroup vg);
	}
	
	
	
	
	public CommonAdapter(List<CommonAdapterItemWrapper> adapter,
			ViewConvertListener listener) {
		this(adapter, listener, 10);
	}
	
	
	
	public CommonAdapter(List<CommonAdapterItemWrapper> adapter,
			ViewConvertListener listener, int viewCount) {
		super();
		this.adapter = adapter;
		this.listener = listener;
		this.batchCount = viewCount;
		if (this.adapter == null || this.listener == null) {
			throw new NullPointerException(" parameters can not be null");
		}
	}
	
	

	@Override
	public int getCount() {
		return adapter.size();
	}

	@Override
	public Object getItem(int pos) {
		return adapter.get(pos).getItemObject();
	}

	@Override
	public long getItemId(int pos) {
		return adapter.get(pos).getItemLongId();
	}
	
	public int getViewTypeCount() {
		return batchCount;
	}

	@Override
	public View getView(int pos, View view, ViewGroup vg) {
		return listener.converView(adapter.get(pos), view, vg);
	}

}
