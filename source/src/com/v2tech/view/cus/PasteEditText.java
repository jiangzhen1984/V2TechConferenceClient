package com.v2tech.view.cus;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import com.v2tech.util.MessageUtil;

public class PasteEditText extends EditText{

	public PasteEditText(Context context) {
		super(context);
		init();
	}

	public PasteEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public PasteEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		addTextChangedListener(textChangedListener);
	}
	
	private TextWatcher textChangedListener = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {
			MessageUtil.buildChatPasteMessageContent(getContext() , PasteEditText.this);
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
