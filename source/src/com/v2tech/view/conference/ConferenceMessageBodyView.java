package com.v2tech.view.conference;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.v2tech.R;
import com.v2tech.util.V2Log;
import com.v2tech.vo.VImageMessage;
import com.v2tech.vo.VMessage;

public class ConferenceMessageBodyView extends LinearLayout {

	private VMessage mMsg;

	private LinearLayout mContentContainer;

	private View rootView;

	private TextView senderTV;
	private ImageView mImageIV;

	private ClickListener callback;

	public interface ClickListener {
		public void onMessageClicked(VMessage v);
	}

	public ConferenceMessageBodyView(Context context, VMessage m) {
		super(context);
		this.mMsg = m;

		rootView = LayoutInflater.from(context).inflate(
				R.layout.conference_message_body, null, false);
		initView();
		initData();
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
		senderTV.setText(this.mMsg.getUser().getName() + "  "
				+ mMsg.getDateTimeStr());

		if (mMsg.getType() == VMessage.MessageType.TEXT) {
			TextView contentTV = new TextView(this.getContext());
			contentTV.setText(mMsg.getText());
			mContentContainer.addView(contentTV, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));
		} else if (mMsg.getType() == VMessage.MessageType.IMAGE) {
			VImageMessage.Size si = ((VImageMessage) mMsg)
					.getCompressedBitmapSize();
			mImageIV = new ImageView(getContext());
			mContentContainer.addView(mImageIV, new LinearLayout.LayoutParams(
					si.width, si.height));

			new LoadTask()
					.execute(new VImageMessage[] { (VImageMessage) mMsg });
		}

		if (mMsg.getType() == VMessage.MessageType.IMAGE) {
			mContentContainer.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					if (callback != null) {
						callback.onMessageClicked(mMsg);
					}
				}

			});

		}

	}

	public VMessage getItem() {
		return this.mMsg;
	}

	public void setCallback(ClickListener cl) {
		this.callback = cl;
	}

	public void recycle() {
		if (this.mMsg.getType() == VMessage.MessageType.IMAGE) {
			((VImageMessage) this.mMsg).recycle();
		}
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

	class LoadTask extends AsyncTask<VImageMessage, Void, VImageMessage[]> {

		@Override
		protected VImageMessage[] doInBackground(VImageMessage... vms) {
			// Only has one element
			for (VImageMessage vm : vms) {
				vm.getCompressedBitmap();
			}
			return vms;
		}

		@Override
		protected void onPostExecute(VImageMessage[] result) {
			// Only has one element
			VImageMessage vm = result[0];
			// If loaded vm is not same member's, means current view has changed
			// message, igonre this result
			if (vm != mMsg) {
				return;
			}
			mImageIV = (ImageView) mContentContainer
					.getChildAt(mContentContainer.getChildCount() - 1);
			Bitmap bm = vm.getCompressedBitmap();
			mImageIV.setImageBitmap(bm);
		}

	}

}
