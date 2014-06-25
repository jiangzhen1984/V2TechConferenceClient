package com.v2tech.view.widget;

import android.content.ClipboardManager;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import com.v2tech.util.MessageUtil;
import com.v2tech.view.conversation.MessageLoader;
import com.v2tech.vo.VMessage;

public class FaceItemEditText extends EditText {

	public FaceItemEditText(Context context) {
		super(context);
	}

	public FaceItemEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FaceItemEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean onTextContextMenuItem(int id) {
		if (id == android.R.id.paste) {
			ClipboardManager clipboard = (ClipboardManager) getContext()
					.getSystemService(Context.CLIPBOARD_SERVICE);
			int c = clipboard.getPrimaryClip().getItemCount();
			if (c == 1 ) {
				String str = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
				int start = str.indexOf("[[[[");
				int end =str.lastIndexOf("]]]]");
				if (start != -1 && end != -1) {
					try {
						Long lid = Long.parseLong(str.substring(start+4, end));
						VMessage m = MessageLoader.loadMessageById(getContext(), lid);
						if (m != null) {
							CharSequence ch = MessageUtil.getMixedConversationContent(getContext(), m);
							this.append(ch, this.getSelectionStart(), this.getSelectionEnd());
							return true;
						}
					} catch(Exception e) {
						
					}
				}
			}
		} 
		
		return super.onTextContextMenuItem(id);
	}

}
