package com.bizcom.vc.activity.conference;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.V2.jni.util.V2Log;
import com.bizcom.util.DateUtil;
import com.bizcom.vc.application.GlobalConfig;
import com.bizcom.vc.application.PublicIntent;
import com.bizcom.vo.ConferenceGroup;
import com.bizcom.vo.VMessage;
import com.bizcom.vo.VMessageAbstractItem;
import com.bizcom.vo.VMessageFaceItem;
import com.bizcom.vo.VMessageImageItem;
import com.bizcom.vo.VMessageLinkTextItem;
import com.bizcom.vo.VMessageTextItem;
import com.v2tech.R;

public class ConferenceMessageBodyView extends LinearLayout{

	private VMessage mMsg;
	private ConferenceGroup conf;

	private LinearLayout mContentContainer;
	private View rootView;

	private TextView senderTV;
	private Context mContext;

	public interface ClickListener {
		public void onMessageClicked(VMessage v);
	}
	
	public interface ActionListener {
		public void NotChangeTaskToBack();
	}

	public ConferenceMessageBodyView(Context context, VMessage m) {
		super(context);
		this.mMsg = m;
		this.mContext = context;
		rootView = LayoutInflater.from(context).inflate(
				R.layout.conference_message_body, null, false);
		initView();
		initData();
		
	}

	public void setConf(ConferenceGroup conf) {
		this.conf = conf;
	}

	private void initView() {
		if (rootView == null) {
			V2Log.e(" root view is Null can not initialize");
			return;
		}

		senderTV = (TextView) rootView
				.findViewById(R.id.conference_message_sender);
		mContentContainer = (LinearLayout) rootView
				.findViewById(R.id.conference_message_body_ly);
		LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		this.addView(rootView, ll);
	}

	private void initData() {
		senderTV.setText(this.mMsg.getFromUser().getName() + "  "
				+ DateUtil.getStringDate(mMsg.getDate().getTime()));

		TextView et = new TextView(this.getContext());
		et.setBackgroundColor(Color.TRANSPARENT);
		et.setSelected(false);
		et.setClickable(true);
		et.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				List<VMessageImageItem> imageItems = mMsg.getImageItems();
				if (imageItems != null && imageItems.size() > 0) {
					VMessageImageItem imageItem = mMsg.getImageItems().get(0);
					//notify conferenceActivity chanage flag isMoveToBack
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
					i.putExtra("cid", mMsg.getId());
					i.putExtra("type", conf.getGroupType().intValue());
					i.putExtra("gid", conf.getmGId());
					mContext.startActivity(i);
				}
			}
		});
		
		LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		mContentContainer.addView(et, ll);

		List<VMessageAbstractItem> items = mMsg.getItems();
		for (int i = 0; items != null && i < items.size(); i++) {
			VMessageAbstractItem item = items.get(i);
			// Add new layout for new line
			if (item.isNewLine() && et.length() != 0) {
				et.append("\n");
			}
			if (item.getType() == VMessageAbstractItem.ITEM_TYPE_TEXT) {
				et.append(((VMessageTextItem) item).getText());
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_FACE) {
				Drawable dr = this
						.getResources()
						.getDrawable(
								GlobalConfig.GLOBAL_FACE_ARRAY[((VMessageFaceItem) item)
										.getIndex()]);
				dr.setBounds(0, 0, dr.getIntrinsicWidth(),
						dr.getIntrinsicHeight());

				et.append(".");

				SpannableStringBuilder builder = new SpannableStringBuilder(
						et.getText());
				ImageSpan is = new ImageSpan(dr);
				builder.setSpan(is, et.getText().length() - 1, et.getText()
						.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				et.setText(builder);
			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_IMAGE) {
				Drawable dr = new BitmapDrawable(this.getContext()
						.getResources(),
						((VMessageImageItem) item).getCompressedBitmap());
				dr.setBounds(0, 0, dr.getIntrinsicWidth(),
						dr.getIntrinsicHeight());
				et.append(".");
				SpannableStringBuilder builder = new SpannableStringBuilder(
						et.getText());
				ImageSpan is = new ImageSpan(dr);
				builder.setSpan(is, et.getText().length() - 1, et.getText()
						.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				et.setText(builder);

			} else if (item.getType() == VMessageAbstractItem.ITEM_TYPE_LINK_TEXT) {
				et.append(((VMessageLinkTextItem) item).getText());
			}

		}

		mContentContainer.setTag(this.mMsg);

	}

	public VMessage getItem() {
		return this.mMsg;
	}

	public void recycle() {
		mMsg.recycleAllImageMessage();
	}

	public void updateView(VMessage vm) {
		if (vm == null) {
			V2Log.e("Can't not update data vm is null");
			return;
		}
		if (this.mMsg == vm) {
			return;
		}
		if (mContentContainer != null) {
			mContentContainer.removeAllViews();
		}
		this.mMsg = vm;
		initData();
	}

	class LoadTask extends AsyncTask<ImageView, Void, ImageView[]> {

		@Override
		protected ImageView[] doInBackground(ImageView... vms) {
			// Only has one element
			for (ImageView vm : vms) {
				((VMessageImageItem) vm.getTag()).getCompressedBitmap();
			}
			return vms;
		}

		@Override
		protected void onPostExecute(ImageView[] result) {
			// Only has one element
			ImageView vm = result[0];
			// If loaded vm is not same member's, means current view has changed
			// message, ignore this result
			VMessageImageItem item = ((VMessageImageItem) vm.getTag());
			if (item.getVm() != mMsg) {
				return;
			}
			Bitmap bm = item.getCompressedBitmap();
			vm.setImageBitmap(bm);
		}

	}

}