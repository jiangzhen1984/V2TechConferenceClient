package com.bizcom.vc.adapter;

import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Use to wrap simple adapter for ListView
 * 
 * <ul>
 * Use {@link CommonAdapterGetViewListener} to wrap construct view object.
 * </ul>
 * <ul>
 * Use {@link CommonAdapterItemDateAndViewWrapper} to wrap List object.
 * </ul>
 * 
 * @author jiangzhen
 * 
 */
public class CommonAdapter extends BaseAdapter {

	private List<CommonAdapterItemDateAndViewWrapper> dataset;

	private CommonAdapterGetViewListener listener;

	private int batchCount;

	public interface CommonAdapterItemDateAndViewWrapper {
		public Object getItemObject();
		
		public long getItemLongId();

		public View getView();
	}

	public interface CommonAdapterGetViewListener {
		public View getView(CommonAdapterItemDateAndViewWrapper wr, View view, ViewGroup vg);
	}

	public CommonAdapter(List<CommonAdapterItemDateAndViewWrapper> dataset,
			CommonAdapterGetViewListener listener) {
		this(dataset, listener, 1);
	}

	public CommonAdapter(List<CommonAdapterItemDateAndViewWrapper> dataset,
			CommonAdapterGetViewListener listener, int viewCount) {
		super();
		this.dataset = dataset;
		this.listener = listener;
		this.batchCount = viewCount;
		if (this.dataset == null || this.listener == null) {
			throw new NullPointerException(" parameters can not be null");
		}
	}

	@Override
	public int getCount() {
		return dataset.size();
	}

	@Override
	public Object getItem(int pos) {
		return dataset.get(pos).getItemObject();
	}

	@Override
	public long getItemId(int pos) {
		return dataset.get(pos).getItemLongId();
	}

	public int getViewTypeCount() {
		return batchCount;
	}

	@Override
	public View getView(int pos, View view, ViewGroup vg) {
		return listener.getView(dataset.get(pos), view, vg);
	}

}
