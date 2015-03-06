package com.bizcom.vc.activity.conference;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.V2.jni.util.V2Log;
import com.bizcom.util.DateUtil;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vc.widget.cus.ConfChatTextView;
import com.bizcom.vo.ConferenceGroup;
import com.bizcom.vo.VMessage;
import com.bizcom.vo.VMessageAbstractItem;
import com.bizcom.vo.VMessageFaceItem;
import com.bizcom.vo.VMessageImageItem;
import com.bizcom.vo.VMessageLinkTextItem;
import com.bizcom.vo.VMessageTextItem;
import com.v2tech.R;

public class ConferenceMessageBodyView extends LinearLayout {

	private View rootView;
	private TextView senderTV;
	private LinearLayout mMsgBodyContainer;
	private ConfChatTextView mMsgBodyTV;
	private OnClickListener mMsgBodyTVOnClickListener = new MsgBodyTVOnClickListener();

	private VMessage mVMessage;
	private ConferenceGroup conf;
	private Context mContext;

	public interface ClickListener {
		public void onMessageClicked(VMessage v);
	}

	public interface ActionListener {
		public void NotChangeTaskToBack();
	}

	public ConferenceMessageBodyView(Context context, VMessage m) {
		super(context);
		if(m == null)
			return ;
		this.mVMessage = m;
		this.mContext = context;
		initLayout();
		setViewContent();
	}

	private void initLayout() {
		rootView = LayoutInflater.from(this.mContext).inflate(
				R.layout.conference_message_body, null, false);
		senderTV = (TextView) rootView
				.findViewById(R.id.conference_message_sender);
		senderTV.setTextColor(Color.BLUE);
		mMsgBodyContainer = (LinearLayout) rootView
				.findViewById(R.id.conference_message_body_ly);
		mMsgBodyTV = new ConfChatTextView(this.getContext());
		mMsgBodyTV.setBackgroundColor(Color.TRANSPARENT);
		mMsgBodyTV.setTextColor(Color.BLACK);
		mMsgBodyTV.setSelected(false);
		mMsgBodyTV.setClickable(true);
		mMsgBodyTV.setOnClickListener(mMsgBodyTVOnClickListener);
		LinearLayout.LayoutParams ll1 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		mMsgBodyContainer.addView(mMsgBodyTV, ll1);
		LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		this.addView(rootView, ll);

	}

	private void setViewContent() {
		mMsgBodyContainer.setTag(this.mVMessage);
		mMsgBodyTV.setText("");
		senderTV.setText(this.mVMessage.getFromUser().getDisplayName() + "  "
				+ DateUtil.getStringDate(mVMessage.getDate().getTime()));

		List<VMessageAbstractItem> items = mVMessage.getItems();
		for (int i = 0; items != null && i < items.size(); i++) {
			VMessageAbstractItem item = items.get(i);
			// Add new layout for new line
			if (item.isNewLine() && mMsgBodyTV.length() != 0) {
				mMsgBodyTV.append("\n");
			}
			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_TEXT) {
				mMsgBodyTV.append(((VMessageTextItem) item).getText());
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FACE) {
				Drawable dr = this
						.getResources()
						.getDrawable(
								GlobalConfig.GLOBAL_FACE_ARRAY[((VMessageFaceItem) item)
										.getIndex()]);
				dr.setBounds(0, 0, dr.getIntrinsicWidth(),
						dr.getIntrinsicHeight());

				mMsgBodyTV.append(".");

				SpannableStringBuilder builder = new SpannableStringBuilder(
						mMsgBodyTV.getText());
				ImageSpan is = new ImageSpan(dr);
				builder.setSpan(is, mMsgBodyTV.getText().length() - 1,
						mMsgBodyTV.getText().length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				mMsgBodyTV.setText(builder);
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_IMAGE) {
				Drawable dr = new BitmapDrawable(this.getContext()
						.getResources(),
						((VMessageImageItem) item).getCompressedBitmap());
				dr.setBounds(0, 0, dr.getIntrinsicWidth(),
						dr.getIntrinsicHeight());
				mMsgBodyTV.append(".");
				SpannableStringBuilder builder = new SpannableStringBuilder(
						mMsgBodyTV.getText());
				ImageSpan is = new ImageSpan(dr);
				builder.setSpan(is, mMsgBodyTV.getText().length() - 1,
						mMsgBodyTV.getText().length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				mMsgBodyTV.setText(builder);

			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_LINK_TEXT) {
				String linkText = ((VMessageLinkTextItem) item).getText();
				SpannableStringBuilder style = new SpannableStringBuilder(
						((VMessageLinkTextItem) item).getText());
				style.setSpan(new ForegroundColorSpan(Color.BLUE), 0,
						linkText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				style.setSpan(new UnderlineSpan(), 0, linkText.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				mMsgBodyTV.append(style);
			}

		}
	}

	public void updateView(VMessage vm) {
		if (vm == null) {
			V2Log.e("Can't not update data vm is null");
			return;
		}
		
		boolean isAddView = false;
		if (mMsgBodyContainer != null) {
			mMsgBodyContainer.removeAllViews();
			isAddView = true;
		}
		this.mVMessage = vm;
		setViewContent();
		
		if(isAddView){
			LinearLayout.LayoutParams ll1 = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			mMsgBodyContainer.addView(mMsgBodyTV, ll1);
		}
	}

	public VMessage getVMessage() {
		return this.mVMessage;
	}

	public void recycle() {
		mVMessage.recycleAllImageMessage();
	}

	public void setConf(ConferenceGroup conf) {
		this.conf = conf;
	}

	private class MsgBodyTVOnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			List<VMessageImageItem> imageItems = mVMessage.getImageItems();
			if (imageItems != null && imageItems.size() > 0) {
				VMessageImageItem imageItem = mVMessage.getImageItems().get(0);
				// notify conferenceActivity chanage flag isMoveToBack
				Intent notify = new Intent();
				notify.addCategory(PublicIntent.DEFAULT_CATEGORY);
				notify.setAction(PublicIntent.NOTIFY_CONFERENCE_ACTIVITY);
				mContext.sendBroadcast(notify);

				Intent i = new Intent();
				i.addCategory(PublicIntent.DEFAULT_CATEGORY);
				i.setAction(PublicIntent.START_VIDEO_IMAGE_GALLERY);
				if (imageItem != null)
					i.putExtra("imageID", imageItem.getUuid());
				// type 0: is not group image view
				// type 1: group image view
				i.putExtra("cid", mVMessage.getId());
				i.putExtra("type", conf.getGroupType().intValue());
				i.putExtra("gid", conf.getmGId());
				mContext.startActivity(i);
				return;
			}

			List<VMessageLinkTextItem> linkItems = mVMessage.getLinkItems();
			if (linkItems != null && linkItems.size() > 0) {
				VMessageLinkTextItem linkItem = linkItems.get(0);
				Intent intent = new Intent();
				intent.setAction("android.intent.action.VIEW");
				Uri content_url = Uri.parse("http://" + linkItem.getUrl());
				intent.setData(content_url);
				mContext.startActivity(intent);
				return;
			}
		}
	}
}
