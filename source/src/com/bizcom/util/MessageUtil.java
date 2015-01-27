package com.bizcom.util;

import java.sql.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.widget.EditText;
import android.widget.Toast;

import com.V2.jni.util.V2Log;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.GlobalHolder;
import com.bizcom.vc.widget.cus.PasteEditText;
import com.bizcom.vo.User;
import com.bizcom.vo.VMessage;
import com.bizcom.vo.VMessageAbstractItem;
import com.bizcom.vo.VMessageFaceItem;
import com.bizcom.vo.VMessageLinkTextItem;
import com.bizcom.vo.VMessageTextItem;
import com.v2tech.R;

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
		if (vm.getAudioItems().size() > 0) {
			builder.append(context.getResources().getText(
					R.string.conversation_display_item_audio));
			return builder;
		}

		if (vm.getFileItems().size() > 0) {
			builder.append(context.getResources().getText(
					R.string.conversation_display_item_file));
			return builder;
		}

		// If no text and face item, now means send picture
		if (vm.getImageItems().size() > 0) {
			builder.append(context.getResources().getText(
					R.string.conversation_display_item_pic));
			return builder;
		}

		for (int i = 0; i < vm.getItems().size(); i++) {
			VMessageAbstractItem item = vm.getItems().get(i);
			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_TEXT) {
				builder.append(((VMessageTextItem) item).getText()).append(" ");
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_LINK_TEXT) {
				builder.append(((VMessageLinkTextItem) item).getText()).append(
						" ");
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FACE) {
				Drawable dr = context
						.getResources()
						.getDrawable(
								GlobalConfig.GLOBAL_FACE_ARRAY[((VMessageFaceItem) item)
										.getIndex()]);
				appendSpan(builder, dr, ((VMessageFaceItem) item).getIndex());
			}
		}
		return builder;
	}

	/**
	 * Get text content from message. convert face item to special symbol
	 * 
	 * @param mContext
	 * @param vm
	 * @return
	 * 
	 * @see GlobalConfig#getEmojiStrByIndex(int)
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
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_LINK_TEXT) {
				builder.append(((VMessageLinkTextItem) item).getText());
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
		String emoji = GlobalConfig.getEmojiStrByIndex(index);
		builder.append(emoji);

		ImageSpan is = new ImageSpan(drw, index + "");
		builder.setSpan(is, builder.length() - emoji.length(),
				builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	public static void buildChatPasteMessageContent(Context mContext,
			EditText mMessageET) {
		Editable edit = mMessageET.getEditableText();
		int num = 0;
		int flagCount = 0;
		String[] split = edit.toString().split("/:");
		for (String string : split) {
			if (string.contains(":")) {
				num++;
			}
		}
		if (num > 10 && split.length > 10) {
			Toast.makeText(mContext,
					R.string.error_contact_message_face_too_much,
					Toast.LENGTH_SHORT).show();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < split.length; i++) {

				if (flagCount == 10) {
					flagCount = 0;
					break;
				}

				if (split[i].contains(":")) {
					flagCount++;
					sb.append("/:");
					if (flagCount == 10 && split[i].split(" ").length > 1) {
						sb.append(split[i].split(" ")[0]);
					} else
						sb.append(split[i]);
				} else {
					sb.append(split[i]);
				}

			}
			edit.clear();
			edit.append(sb.toString().trim());
			mMessageET.setSelection(sb.toString().trim().length());
			sb.delete(0, sb.length());
		}
		num = 0;

		int start = -1, end;
		int index = 0;
		while (index < edit.length()) {
			if (edit.charAt(index) == '/' && index < edit.length() - 1
					&& edit.charAt(index + 1) == ':') {
				start = index;
				index += 2;
				continue;
			} else if (start != -1) {
				if (edit.charAt(index) == ':' && index < edit.length() - 1
						&& edit.charAt(index + 1) == '/') {
					end = index + 2;
					SpannableStringBuilder builder = new SpannableStringBuilder();

					int ind = GlobalConfig.getDrawableIndexByEmoji(edit
							.subSequence(start, end).toString());
					// replay emoji and clean
					if (ind > 0) {
						MessageUtil.appendSpan(
								builder,
								mContext.getResources().getDrawable(
										GlobalConfig.GLOBAL_FACE_ARRAY[ind]),
								ind);
						edit.replace(start, end, builder);
					}
					index = start;
					start = -1;
				}
			}
			index++;
		}
	}

	public static VMessage buildChatMessage(Context mContext,
			EditText mMessageET, int groupType, long remoteGroupID,
			User remoteUser) {
		String content = mMessageET.getEditableText().toString();
		if (content == null || content.equals("")) {
			Toast.makeText(mContext, R.string.util_message_toast_error,
					Toast.LENGTH_SHORT).show();
			return null;
		}

		content = removeEmoji(content);

		VMessage vm = new VMessage(groupType, remoteGroupID, GlobalHolder
				.getInstance().getCurrentUser(), remoteUser, new Date(
				GlobalConfig.getGlobalServerTime()));
		String[] array = content.split("\n");
		for (int i = 0; i < array.length; i++) {
			String str = array[i];
			int len = str.length();
			if (str.length() <= 4) {
				VMessageAbstractItem vai = new VMessageTextItem(vm, str);
				vai.setNewLine(true);
				continue;
			}

			int emojiStart = -1, end, strStart = 0;
			int index = 0;
			Pattern pattern = Pattern
					.compile("(http://|https://|www\\.){1}[^\u4e00-\u9fa5\\s]*?\\.(com|net|cn|me|tw|fr|html){1}(/[^\u4e00-\u9fa5\\s]*){0,1}");
			while (index < str.length()) {
				if (str.charAt(index) == '/' && index < len - 1
						&& str.charAt(index + 1) == ':') {
					emojiStart = index;
					index += 2;
					continue;
				} else if (emojiStart != -1) {
					// Found end flag of emoji
					if (str.charAt(index) == ':' && index < len - 1
							&& str.charAt(index + 1) == '/') {
						end = index + 2;

						// If emojiStart lesser than strStart,
						// mean there exist string before emoji
						if (strStart < emojiStart) {
							String strTextContent = str.substring(strStart,
									emojiStart);
							VMessageTextItem vti = new VMessageTextItem(vm,
									strTextContent);
							// If strStart is 0 means string at new line
							if (strStart == 0) {
								vti.setNewLine(true);
							}

						}

						int ind = GlobalConfig.getDrawableIndexByEmoji(str
								.subSequence(emojiStart, end).toString());
						if (ind > 0) {
							// new face item and add list
							VMessageFaceItem vfi = new VMessageFaceItem(vm, ind);
							// If emojiStart is 0 means emoji at new line
							if (emojiStart == 0) {
								vfi.setNewLine(true);
							}

						}
						// Assign end to index -1, do not assign end because
						// index will be ++
						index = end - 1;
						strStart = end;
						emojiStart = -1;
					}
				}

				int lastStart = 0;
				int lastEnd = 0;
				boolean firstMather = true;
				// check if exist last string
				if (index == len - 1 && strStart <= index) {
					String strTextContent = str.substring(strStart, len);
					Matcher matcher = pattern.matcher(strTextContent);
					while (matcher.find()) {
						String url = matcher.group(0);
						V2Log.e("ConversationP2PTextActivity", "从文本内容检测到网址："
								+ url);
						// 检测网址前面是否有文本内容
						if (firstMather == true) {
							firstMather = false;
							if (matcher.start(0) != strStart) {

								VMessageTextItem vti = new VMessageTextItem(vm,
										strTextContent.substring(strStart,
												matcher.start(0)));
								// If strStart is 0 means string at new line
								if (strStart == 0) {
									vti.setNewLine(true);
								}
							}
							new VMessageLinkTextItem(vm, url, url);
						} else {
							if (matcher.start(0) != lastEnd) {
								VMessageTextItem vti = new VMessageTextItem(vm,
										strTextContent.substring(
												matcher.end(0) + 1, lastStart));
								// If strStart is 0 means string at new line
								if (matcher.end(0) + 1 == 0) {
									vti.setNewLine(true);
								}
							}
						}
						lastStart = matcher.start(0);
						lastEnd = matcher.end(0);
					}

					if (strTextContent.length() != lastEnd) {
						String lastText = strTextContent.substring(lastEnd,
								strTextContent.length());
						VMessageTextItem vti = new VMessageTextItem(vm,
								lastText);
						// vti.setNewLine(true);
					}
					strStart = index;
				}
				index++;
			}
		}
		mMessageET.setText("");
		return vm;
	}

	/**
	 * FIXME optimize code 去除IOS自带表情
	 * 
	 * @param content
	 * @return
	 */
	public static String removeEmoji(String content) {
		byte[] bys = new byte[] { -16, -97 };
		byte[] bc = content.getBytes();
		byte[] copy = new byte[bc.length];
		int j = 0;
		for (int i = 0; i < bc.length; i++) {
			if (i < bc.length - 2 && bys[0] == bc[i] && bys[1] == bc[i + 1]) {
				i += 3;
				continue;
			}
			copy[j] = bc[i];
			j++;
		}
		return new String(copy, 0, j);
	}
}
