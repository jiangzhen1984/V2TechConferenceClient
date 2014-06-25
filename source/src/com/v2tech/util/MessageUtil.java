package com.v2tech.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

import com.v2tech.R;
import com.v2tech.vo.VMessage;
import com.v2tech.vo.VMessageAbstractItem;
import com.v2tech.vo.VMessageFaceItem;
import com.v2tech.vo.VMessageTextItem;

public class MessageUtil {

	/**
	 * Convert message content to comfortable content for conversation display
	 * if only send image: [pic] if send emoji and text: show text and emoji
	 * 
	 * @param context
	 * @param vm
	 * @return
	 */
	public static CharSequence getMixedConversationContent(Context context,
			VMessage vm) {
		if (vm == null || context == null) {
			return null;
		}

		SpannableStringBuilder builder = new SpannableStringBuilder();
		for (int i = 0; i < vm.getItems().size(); i++) {
			VMessageAbstractItem item = vm.getItems().get(i);
			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_TEXT) {
				builder.append(((VMessageTextItem) item).getText()).append(" ");
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FACE) {
				Drawable dr = context
						.getResources()
						.getDrawable(
								GlobalConfig.GLOBAL_FACE_ARRAY[((VMessageFaceItem) item)
										.getIndex()]);
				appendSpan(builder, dr, ((VMessageFaceItem) item).getIndex());
			}
		}

		// If no text and face item, now means send picture
		if (builder.length() <= 0) {
			builder.append(context.getResources().getText(
					R.string.conversation_display_item_pic));
		}

		return builder;

	}

	/**
	 * TODO add comment
	 * 
	 * @param context
	 * @param vm
	 * @return
	 */
	public static CharSequence getMixedConversationCopyedContent(VMessage vm) {
		if (vm == null) {
			return null;
		}

		SpannableStringBuilder builder = new SpannableStringBuilder();
		for (int i = 0; i < vm.getItems().size(); i++) {
			VMessageAbstractItem item = vm.getItems().get(i);
			if (builder.length() != 0 && item.isNewLine()) {
				builder.append("\n");
			}
			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_TEXT) {
				builder.append(((VMessageTextItem) item).getText());
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FACE) {
				builder.append(GlobalConfig
						.getEmojiStrByIndex(((VMessageFaceItem) item)
								.getIndex()));
			}
		}

		return builder;
	}

	public static void appendSpan(SpannableStringBuilder builder, Drawable drw,
			int index) {
		if (builder == null || drw == null) {
			return;
		}

		drw.setBounds(0, 0, drw.getIntrinsicWidth(), drw.getIntrinsicHeight());
		builder.append(".");

		ImageSpan is = new ImageSpan(drw, index + "");
		builder.setSpan(is, builder.length() - ".".length(), builder.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}
}
