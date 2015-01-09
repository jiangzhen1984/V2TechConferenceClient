package com.v2tech.view.cus;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import com.v2tech.view.widget.GroupListView;

public class SearchEditText extends EditText {

	private GroupListView mGroupListView;

	public SearchEditText(Context context) {
		super(context);
	}

	public SearchEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SearchEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void addTextListener(GroupListView mGroupListView) {
		this.mGroupListView = mGroupListView;
		addTextChangedListener(textChangedListener);
	}

	public void removeTextListener() {
		removeTextChangedListener(textChangedListener);
	}

	private TextWatcher textChangedListener = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {

			if (s.toString().isEmpty()) {
				mGroupListView.clearTextFilter();
			} else {
				mGroupListView.setFilterText(s.toString());
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {

		}
	};
}
